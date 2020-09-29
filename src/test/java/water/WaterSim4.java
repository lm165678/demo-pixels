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

public class WaterSim4 extends SimpleApplication {

    public static void main(String... args) {

        WaterSim4 main = new WaterSim4();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setResolution(1280, 720);
        appSettings.setAudioRenderer(null);

        main.setSettings(appSettings);
        main.setShowSettings(false);
        main.setPauseOnLostFocus(false);
        main.start();
    }

    //Block types
    final int AIR = 0;
    final int GROUND = 1;
    final int WATER = 2;

    private boolean stepping = false;

    private final int xSize = 56;
    private final int ySize = 32;

    private final Node sceneNode = new Node("Scene");
    private final Node blocksNode = new Node("Blocks");
    private final Node labelsNode = new Node("Labels");

    private Geometry[][] geometries = new Geometry[xSize][ySize];
    private int[][] blocks = new int[xSize][ySize];
    private float[][] mass = new float[xSize][ySize];

    //Water properties
    private final float MaxMass = 1.0f; // 1.0f
    private float MaxCompress = 0.02f; // 0.02f
    private float MinMass = 0.0001f; // 0.0001f

    // final float minflow = 0.01f;
    // float maxflow = 1;
    private float minflow = 0.01f;
    private float maxflow = 1;   //max units of water moved out of one block to another, per timestep
    private float smoothFlow = 0.5f;

    ColorRGBA[] block_colors = {
            ColorRGBA.Black.clone(),// color(255)  //air
            new ColorRGBA(0.7f, 0.7f, 0.35f, 1.0f), // color(200,200,100) //ground
            ColorRGBA.Blue.clone()
    };

    // debug
    private VersionedReference<Boolean> showLabelsRef;
    private Label changesLabel;
    private final Label[][] labels = new Label[xSize][ySize];

    @Override
    public void simpleInitApp() {

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);

        cam.setLocation(new Vector3f(0, 0, 40));
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);

        Material unshaded = new Material(assetManager, Materials.UNSHADED);
        unshaded.setBoolean("VertexColor", true);

        Container container = new Container();
        container.setLocalTranslation(10,
                cam.getHeight() - 10,
                0);

        int row = 0;

        Slider slider;

//        container.addChild(new Label("Min Flow"), row, 0);
//        slider = container.addChild(new Slider(),row, 1);
//        slider.setPreferredSize(new Vector3f(200, slider.getPreferredSize().y, 1));
//        slider.getModel().setMinimum(0.001f);
//        slider.getModel().setMaximum(100);
//        slider.getModel().setValue(minflow);
//        minFlowRef = slider.getModel().createReference();
//
//        row++;
//
//        container.addChild(new Label("Max Flow"), row, 0);
//        slider = container.addChild(new Slider(), row, 1);
//        slider.getModel().setMaximum(100);
//        slider.getModel().setMinimum(0.1);
//        slider.getModel().setValue(maxflow);
//        maxFlowRef = slider.getModel().createReference();
//
//        row++;
//
//        container.addChild(new Label("Min Mass"), row, 0);
//        slider = container.addChild(new Slider(), row, 1);
//        slider.getModel().setMaximum(MaxMass);
//        slider.getModel().setMinimum(0.0001f);
//        slider.getModel().setValue(MinMass);
//        minMassRef = slider.getModel().createReference();
//
//        row++;
//
//        container.addChild(new Label("Max Compress"), row, 0);
//        slider = container.addChild(new Slider(), row, 1);
//        slider.getModel().setMaximum(0.02f);
//        slider.getModel().setMinimum(0.0001f);
//        slider.getModel().setValue(MaxCompress);
//        maxCompressRef = slider.getModel().createReference();
//
//        row++;
//
//        container.addChild(new Label("SmoothFlow"), row, 0);
//        slider = container.addChild(new Slider(), row, 1);
//        slider.getModel().setMaximum(0.9f);
//        slider.getModel().setMinimum(0.1f);
//        slider.getModel().setValue(smoothFlow);
//        smoothFlowRef = slider.getModel().createReference();

//        container.addChild(new Label("Viscosity"), row, 0);
//        slider = container.addChild(new Slider(), row, 1);
//        slider.setPreferredSize(new Vector3f(200, slider.getPreferredSize().y, 1));
//        slider.getModel().setMinimum(minViscosity);
//        slider.getModel().setMaximum(maxViscosity);
//        slider.getModel().setValue(viscosity);
//        viscosityRef = slider.getModel().createReference();
//
//        row++;

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


        guiNode.attachChild(startSimButton);


        sceneNode.attachChild(blocksNode);
        sceneNode.attachChild(labelsNode);

        rootNode.attachChild(sceneNode);
        sceneNode.setLocalTranslation(
                -xSize * 0.5f,
                -ySize * 0.5f,
                0
        );

    }

    private void startSimulation() {

        changes.clear();
        newChanges.clear();

        //Fill the map with random blocks
        for ( int x = 1; x < xSize - 1; x++ ){
            for ( int y = 1; y < ySize - 1; y++ ){

                // blocks[x][y] = FastMath.nextRandomInt(0, 2);

                // one in x chance of ground
                int groundChance = FastMath.nextRandomInt(0, 5);
                if (groundChance == 5) {
                    blocks[x][y] = GROUND;
                }
                else {
                    int waterChance = FastMath.nextRandomInt(0, 3);
                    if (waterChance == 3) {
                        blocks[x][y] = WATER;
                    }
                    else {
                        blocks[x][y] = AIR;
                    }
                }

                // one in 3 chance of water.

                // mass[x][y] = blocks[x][y] == WATER ? MaxMass : 0.0f;

                // new_mass[x][y] = blocks[x][y] == WATER ? MaxMass : 0.0f;
                if (blocks[x][y] == WATER) {
                    addChange(x, y, 1, true);
                }
                else {
                    mass[x][y] = 0.0f;
                }

                setMeshSize(geometries[x][y].getMesh(), 1);
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

    private void setMeshSize(Mesh mesh, float size) {
        Quad quad = (Quad) mesh;
        quad.updateGeometry(1, size);
    }

    private void draw_block(int x, int y, float filled) {

        Geometry geometry = geometries[x][y];


        if (blocks[x][y] == WATER) {

            // if there is a block of water above, just set it to full height.
            // this makes blocks below water and falling water look nice.
            // without this there are gaps and it looks really ugly.

            boolean waterAbove = false;

            if (y + 1 < ySize - 1) {
                waterAbove = blocks[x][y + 1] == WATER;
            }

            if (waterAbove) {
                setMeshSize(geometry.getMesh(), 1);
            }
            else {
                setMeshSize(geometry.getMesh(), FastMath.clamp(filled, 0.1f, 1.0f));
            }

            setMeshColor(geometry.getMesh(), block_colors[2]);

        }
        else {
            setMeshSize(geometry.getMesh(), 1);
            setMeshColor(geometry.getMesh(), block_colors[0]);
        }

    }

    private void updateGui(float tpf) {

//        if (minFlowRef.update()) {
//            minflow = minFlowRef.get().floatValue();
//        }
//
//        if (maxFlowRef.update()) {
//            maxflow = maxFlowRef.get().floatValue();
//        }
//
//        if (minMassRef.update()) {
//            MinMass = minMassRef.get().floatValue();
//        }
//
//        if (maxCompressRef.update()) {
//            MaxCompress = maxCompressRef.get().floatValue();
//        }
//
//        if (smoothFlowRef.update()) {
//            smoothFlow = smoothFlowRef.get().floatValue();
//        }

//        if (viscosityRef.update()) {
//            viscosity = minViscosity + (maxViscosity - viscosityRef.get().floatValue());
//        }

        if (showLabelsRef.update()) {
            Boolean val = showLabelsRef.get();

            if (val) {
                sceneNode.attachChild(labelsNode);
            }
            else {
                labelsNode.removeFromParent();
            }
        }

    }

    float time = 0;
    float frameTime = 1f / 60;
    private final String format = "%d,%d:%.5f";

    @Override
    public void simpleUpdate(float tpf) {

        updateGui(tpf);

        //Run the water simulation (unless we're in the step-by-step mode)
        if (!stepping) {
            simulate_compression(tpf);
        }

        if (showLabelsRef.get()) {
            for (int x = 0; x < xSize; x++) {
                for (int y = 0; y < ySize; y++) {
                    labels[x][y].setText(String.format(format, x, y, mass[x][y]));
                }
            }
        }

    }

    private final Map<GridPos, Float> changes = new HashMap<>();
    private final Map<GridPos, Float> newChanges = new HashMap<>();

    private void addChange(int x, int y, float change, boolean add) {

        GridPos gp = new GridPos(x, y);

        Float newVal = newChanges.get(gp);

        if (newVal == null) {
            newVal = mass[x][y];
        }

        if (add) {
            newVal += change;
        }
        else {
            newVal -= change;
        }

        newChanges.put(gp, newVal);
    }

    // if the flow is smaller than this number, don't bother moving the water.
    // this stops it from continually moving tiny amounts.
    private final float ignoreMovement = 0.00001f; // 0.0000001f;

    void simulate_compression(float tpf) {

        float flow;
        float remaining_mass;

        int x, y;

        //Calculate and apply flow for each block
        for (Map.Entry<GridPos, Float> entry : changes.entrySet()) {

            x = entry.getKey().x;
            y = entry.getKey().y;

            //Custom push-only flow

            remaining_mass = entry.getValue();

//            if (remaining_mass <= MinMass) {
//                mass[x][y] = 0;
//                blocks[x][y] = AIR;
//                draw_block(x,y,1);
//                continue;
//            }

//            if (remaining_mass < ignoreMovement) {
//                blocks[x][y] = AIR;
//                mass[x][y] = 0;
//
//                System.out.println(x + "," + y + " Skipped because mass too low: " + remaining_mass);
//                continue;
//            }

            //The block below this one
            if (blocks[x][y - 1] != GROUND) {

                flow = get_stable_state_b(remaining_mass + mass[x][y - 1]) - mass[x][y - 1];

//                if (flow > minflow) {
//                    flow *= 0.5f; // leads to smoother flow
//                }

                flow = FastMath.clamp(flow, 0, Math.min(maxflow, remaining_mass));

                //if (flow > ignoreMovement) {
                    addChange(x, y, flow, false);
                    addChange(x, y - 1, flow, true);
                    remaining_mass -= flow;
                //}

            }

            if (remaining_mass <= 0) {
                continue;
            }

            //Left
            if (blocks[x - 1][y] != GROUND) {

                //Equalize the amount of water in this block and it's neighbour
                flow = (mass[x][y] - mass[x - 1][y]) / 4f;

//                if (flow > minflow) {
//                    flow *= 0.5f; // leads to smoother flow
//                }

                flow = FastMath.clamp(flow, 0, remaining_mass);



                //if (flow > ignoreMovement) {
                    addChange(x, y, flow, false);
                    addChange(x - 1, y, flow, true);
                    remaining_mass -= flow;
                //}


            }

            if (remaining_mass <= 0) {
                continue;
            }

            //Right
            if (blocks[x + 1][y] != GROUND) {

                //Equalize the amount of water in this block and it's neighbour
                flow = (mass[x][y] - mass[x + 1][y]) / 4f;
//                if (flow > minflow) {
//                    flow *= 0.5f; // leads to smoother flow
//                }

                flow = FastMath.clamp(flow, 0, remaining_mass);


                //if (flow > ignoreMovement) {
                    addChange(x, y, flow, false);
                    addChange(x + 1, y, flow, true);
                    remaining_mass -= flow;
                //}

            }

            if (remaining_mass <= MinMass) {
                mass[x][y] = 0;
                blocks[x][y] = AIR;
                draw_block(x,y,1);
            }



        }

        changes.clear();

        for (Map.Entry<GridPos, Float> entry : newChanges.entrySet()) {

            x = entry.getKey().x;
            y = entry.getKey().y;
            float val = entry.getValue();

            // mass[x][y] = val;
            // blocks[x][y] = val == 0 ? AIR : WATER;

            if (val > ignoreMovement) {
                blocks[x][y] = WATER;
                mass[x][y] = val;
                changes.put(entry.getKey(), val);
            }

            else {
                blocks[x][y] = AIR;
                mass[x][y] = 0;
            }

            if (blocks[x][y] == AIR) {
                draw_block(x, y, 1);
            }
            else {
                draw_block(x, y, val);
            }
        }
        newChanges.clear();

        changesLabel.setText("" + changes.size());
    }

    // Take an amount of water and calculate how it should be split among two
    // vertically adjacent cells. Returns the amount of water that should be in
    // the bottom cell.
    float get_stable_state_b ( float total_mass ){
        if ( total_mass <= 1 ){
            return 1.0f;
        } else if ( total_mass < 3.0f * MaxMass + MaxCompress ){
            return (MaxMass * MaxMass + total_mass * MaxCompress) / (MaxMass + MaxCompress);
        } else {
            return (total_mass + MaxCompress) / 2.0f;
        }
    }


}
