package water;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.*;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.style.BaseStyles;

import java.util.HashMap;
import java.util.Map;

public class WaterSim5 extends SimpleApplication {

    public static void main(String... args) {

        WaterSim5 main = new WaterSim5();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setResolution(1280, 720);
        appSettings.setAudioRenderer(null);

        main.setSettings(appSettings);
        main.setShowSettings(false);
        main.setPauseOnLostFocus(false);
        main.start();
    }

    private final int xSize = 56;
    private final int ySize = 32;

    private final Node sceneNode = new Node("Scene");
    private final Node blocksNode = new Node("Blocks");
    private final Node labelsNode = new Node("Labels");

    private Geometry[][] geometries = new Geometry[xSize][ySize];
    private int[][] blocks = new int[xSize][ySize];
    private int[][] water = new int[xSize][ySize];

    private final Map<GridPos, Integer> changes = new HashMap<>();
    private final Map<GridPos, Integer> newChanges = new HashMap<>();

    // block types
    final int AIR = 0;
    final int GROUND = 1;
    final int WATER = 2;

    // water configuration
    int maxWater = 1000;


    ColorRGBA[] block_colors = {
            ColorRGBA.Black.clone(),// color(255)  //air
            new ColorRGBA(0.7f, 0.7f, 0.35f, 1.0f), // color(200,200,100) //ground
            ColorRGBA.Blue.clone()
    };

    // scene configuration
    private int groundLikelihood = 5;
    private int waterLikelihood = 3;
    private VersionedReference<Double> groundLikelihoodRef;
    private VersionedReference<Double> waterLikelihoodRef;

    // debug
    private VersionedReference<Boolean> showLabelsRef;
    private Label changesLabel;
    private final Label[][] labels = new Label[xSize][ySize];

    private Container container;

    private void setCamera(float fov, float near, float far) {
        float aspect = (float)cam.getWidth() / (float)cam.getHeight();
        cam.setFrustumPerspective(fov, aspect, near, far);
    }

    @Override
    public void simpleInitApp() {

        setCamera(45f, 0.1f, 100f);

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);

        cam.setLocation(new Vector3f(0, 0, 40));
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);

        Material unshaded = new Material(assetManager, Materials.UNSHADED);
        unshaded.setBoolean("VertexColor", true);
        // unshaded.getAdditionalRenderState().setWireframe(true);

        // GUI
        container = new Container();
        container.setLocalTranslation(10, cam.getHeight() - 10, 0);

        int row = 0;

        Slider slider;

        container.addChild(new Label("Ground"), row, 0);
        slider = container.addChild(new Slider(), row, 1);
        slider.setPreferredSize(new Vector3f(200, slider.getPreferredSize().y, 1));
        slider.getModel().setMinimum(1);
        slider.getModel().setMaximum(50);
        slider.setDelta(1);
        slider.getModel().setValue(groundLikelihood);
        groundLikelihoodRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("Water"), row, 0);
        slider = container.addChild(new Slider(), row, 1);
        slider.getModel().setMinimum(1);
        slider.getModel().setMaximum(50);
        slider.setDelta(1);
        slider.getModel().setValue(waterLikelihood);
        waterLikelihoodRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("Labels"), row, 0);
        Checkbox checkbox = container.addChild(new Checkbox(""), row, 1);
        checkbox.setChecked(true);
        showLabelsRef = checkbox.getModel().createReference();

        row++;

        container.addChild(new Label("Changes"), row, 0);
        changesLabel = container.addChild(new Label(""), row, 1);

        guiNode.attachChild(container);

        Button startSimButton = new Button("Restart Simulation");
        startSimButton.addClickCommands(source -> startSimulation());
        startSimButton.setLocalTranslation(10, container.getLocalTranslation().y - container.getPreferredSize().y - 10, 0);

        for ( int x = 0; x < xSize; x++ ) {
            for (int y = 0; y < ySize; y++) {

                Label label = new Label("1.0000");
                label.setFontSize(0.1f);
                label.setColor(ColorRGBA.Pink);
                label.setLocalTranslation(x - 2, y + label.getPreferredSize().y, 0.001f);
                labelsNode.attachChild(label);
                labels[x][y] = label;

                Quad quad = new Quad(1,1);
                Geometry geometry = new Geometry("Geom: " + x + "," + y, quad);
                geometry.setMaterial(unshaded);
                geometry.setLocalTranslation(x, y, 0);
                blocksNode.attachChild(geometry);

                int block = blocks[x][y];

                if (block == AIR) {
                    setMeshColor(quad, block_colors[0]);
                }
                else if (block == GROUND) {
                    setMeshColor(quad, block_colors[1]);
                }
                else {
                    setMeshColor(quad, ColorRGBA.Blue.clone());
                }

                geometries[x][y] = geometry;

            }
        }


        // attach the scene.
        guiNode.attachChild(container);
        guiNode.attachChild(startSimButton);

        sceneNode.attachChild(blocksNode);
        sceneNode.attachChild(labelsNode);

        sceneNode.setLocalTranslation(
                -xSize * 0.5f,
                -ySize * 0.5f,
                0
        );
        rootNode.attachChild(sceneNode);

        startSimulation();
    }

    private void startSimulation() {

        changes.clear();
        newChanges.clear();

        //Fill the map with random blocks
        for ( int x = 1; x < xSize - 1; x++ ){
            for ( int y = 1; y < ySize - 1; y++ ){

                // blocks[x][y] = FastMath.nextRandomInt(0, 2);

                // one in x chance of ground
                int groundChance = FastMath.nextRandomInt(0, groundLikelihood);
                if (groundChance == 1) {
                    blocks[x][y] = GROUND;
                }
                else {
                    int waterChance = FastMath.nextRandomInt(0, waterLikelihood);
                    if (waterChance == 1) {
                        blocks[x][y] = WATER;
                    }
                    else {
                        blocks[x][y] = AIR;
                    }
                }

                water[x][y] = 0;

                if (blocks[x][y] == WATER) {
                    addChange(x, y, FastMath.nextRandomInt(10, 1000), true);
                    setMeshSize(geometries[x][y].getMesh(), water[x][y]);
                }
                else {
                    setMeshSize(geometries[x][y].getMesh(), maxWater);
                }


            }
        }

        // top and right-side = air
        for (int x =0; x < xSize; x++){
            blocks[x][0] = GROUND;
            blocks[x][ySize - 1] = GROUND;
        }

        // bottom and left = air
        for (int y = 1; y < ySize - 1; y++){
            blocks[0][y] = GROUND;
            blocks[xSize - 1][y] = GROUND;
        }

        for ( int x = 0; x < xSize; x++ ) {
            for (int y = 0; y < ySize; y++) {

                int block = blocks[x][y];

                if (block == AIR) {
                    setMeshColor(geometries[x][y].getMesh(), block_colors[0]);
                }
                else if (block == GROUND) {
                    setMeshColor(geometries[x][y].getMesh(), block_colors[1]);
                }
                else {
                    setMeshColor(geometries[x][y].getMesh(), block_colors[2]);
                }

            }
        }

    }

    private void setMeshColor(Mesh mesh, ColorRGBA color) {
        ColorRGBA[] colors = { color, color, color, color };
        mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));
    }

    private void setMeshSize(Mesh mesh, int size) {
        Quad quad = (Quad) mesh;
        quad.updateGeometry(1, (float)size / maxWater);
    }

    private void drawBlock(int x, int y) {

        Geometry geometry = geometries[x][y];

        if (blocks[x][y] == WATER) {

            boolean aboveWater = false;

            if (y < ySize -1) {
                aboveWater =blocks[x][y + 1] == WATER;
            }

            if (aboveWater) {
                setMeshSize(geometry.getMesh(), maxWater);
            }
            else {
                setMeshSize(geometry.getMesh(), water[x][y]);
            }

            setMeshColor(geometry.getMesh(), block_colors[2]);

        } else {
            setMeshSize(geometry.getMesh(), maxWater);
            setMeshColor(geometry.getMesh(), block_colors[0]);
        }

    }

    public int getWaterContent(int x, int y) {
        GridPos gp = new GridPos(x, y);

        Integer val = newChanges.get(gp);

        if (val == null) {
            val = water[x][y];
        }

        return val;
    }

    private void addChange(int x, int y, int change, boolean add) {

        GridPos gp = new GridPos(x, y);

        int newVal = getWaterContent(x, y);

        if (add) {
            newVal += change;
        }
        else {
            newVal -= change;
        }

        if (newVal > maxWater) {
            System.out.println("WATER EXCEEDED MAX WATER");
        }

        newChanges.put(gp, newVal);
    }

    private final String format = "[ %d | %d ] %d";

    private void updateGui() {
        if (groundLikelihoodRef.update()) {
            groundLikelihood = 49 - groundLikelihoodRef.get().intValue();
        }
        if (waterLikelihoodRef.update()) {
            waterLikelihood = 49 - waterLikelihoodRef.get().intValue();
        }

        if (showLabelsRef.get()) {
            for (int x = 0; x < xSize; x++) {
                for (int y = 0; y < ySize; y++) {
                    labels[x][y].setText(String.format(format, x, y, water[x][y]));
                }
            }
        }
    }

    float maxTime = 1f / 60;
    float time = 0;

    @Override
    public void simpleUpdate(float tpf) {

        if (tpf > 1) {
            return;
        }

        time += tpf;

        if (time < maxTime) {
            return;
        }

        time -= maxTime;

        updateGui();
        updateSimulation(tpf);
    }

    private void updateSimulation(float tpf) {

        // draw changes
        for (Map.Entry<GridPos, Integer> entry : changes.entrySet()) {

            int x = entry.getKey().x;
            int y = entry.getKey().y;
            int newMass = entry.getValue();

            water[x][y] = newMass;

            if (newMass == 0) {
                blocks[x][y] = AIR;
            }
            else {
                blocks[x][y] = WATER;
            }

            drawBlock(x,y);
        }

        // add new changes
        for (Map.Entry<GridPos, Integer> entry : changes.entrySet()) {

            int x = entry.getKey().x;
            int y = entry.getKey().y;

            int remainingMass = entry.getValue();

            // if we're empty, we're empty.
            if (remainingMass == 0) {
                blocks[x][y] = AIR;
                drawBlock(x,y);
                continue;
            }

            // if a block changes its water value, we also need to ask its neighbors if they want to change
            // as a result.

            // below
            if (blocks[x][y-1] != GROUND) {

                // how water mass the block has.
                int bWater = getWaterContent(x, y-1); // water[x][y-1];

                // how much water it can take before it's full.
                int availableSpace = maxWater - bWater;

                // get the most flow we can push down.
                int flow = Math.min(remainingMass, availableSpace);

                if (flow > 0) {
                    addChange(x, y, flow, false);
                    addChange(x, y-1, flow, true);

                    remainingMass -= flow;

                    // ask our neighbors if they want to change.
                    if (blocks[x-1][y] != GROUND) addChange(x-1,y,0, false);
                    if (blocks[x+1][y] != GROUND) addChange(x+1,y,0, false);

                    if (remainingMass == 0) {
                        continue;
                    }
                }
            }

            // if we have any remaining mass, continually iterate left and right until we meet a block of air or water
            // that will take *some* mass. If true, add it to changes. If both left and right fail, we've met our match.

            int n = x - 1;

            while (blocks[n][y] != GROUND) {


                // how much water the neighbor block has.
                int nWater = getWaterContent(n, y); // water[n][y];

                // only push water to a block if it has less water than this block.
                if (nWater < remainingMass) {

                    // get the available space of the neighbor block.
                    int availableSpace = maxWater - nWater;

                    // get difference between this block and the neighbor.
                    int diff = remainingMass - nWater;

                    // get the most flow we can push.
                    int flow = Math.min(availableSpace, Math.min(diff, remainingMass));

                    if (flow > 1) {
                        flow /= 2; // give half because we want to offer the other half to the right if it can take it.

                        addChange(x, y, flow, false);
                        addChange(n, y, flow, true);

                        remainingMass -= flow;
                        break;
                    }

                }

                n--;
            }

            if (remainingMass == 0) {
                continue;
            }

            n = x + 1;
            while (blocks[n][y] != GROUND) {

                // how much water the neighbor block has.
                int nWater = getWaterContent(n, y); // water[n][y];

                // only push water to a block if it has less water than this block.
                if (nWater < remainingMass) {

                    // get the available space of the neighbor block.
                    int availableSpace = maxWater - nWater;

                    // get difference between this block and the neighbor.
                    int diff = remainingMass - nWater;

                    // get the most flow we can push.
                    int flow = Math.min(availableSpace, Math.min(diff, remainingMass));

                    if (flow > 0) {

                        if (flow > 1) {
                            flow /= 2;
                        }

                        addChange(x, y, flow, false);
                        addChange(n, y, flow, true);

                        break;
                    }

                }

                n++;
            }

        }

        // draw changes
        for (Map.Entry<GridPos, Integer> entry : changes.entrySet()) {

            int x = entry.getKey().x;
            int y = entry.getKey().y;
            int newMass = entry.getValue();

            water[x][y] = newMass;

            if (newMass == 0) {
                blocks[x][y] = AIR;
            }
            else {
                blocks[x][y] = WATER;
            }

            drawBlock(x,y);
        }

        changes.clear();

        // put new changes in changes
        for (Map.Entry<GridPos, Integer> entry : newChanges.entrySet()) {
            changes.put(entry.getKey(), entry.getValue());
        }
        newChanges.clear();

        changesLabel.setText("" + changes.size());
    }

}
