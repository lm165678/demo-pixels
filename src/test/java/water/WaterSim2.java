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
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class WaterSim2 extends SimpleApplication {

    public static void main(String... args) {

        WaterSim2 main = new WaterSim2();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setResolution(1280, 720);
        appSettings.setAudioRenderer(null);

        main.setSettings(appSettings);
        main.setShowSettings(false);
        main.setPauseOnLostFocus(false);
        main.start();
    }

    //Block types
    private final int AIR = 0;
    private final int GROUND = 1;
    private final int WATER = 2;

    private final int size = 32;
    private final int[][] blocks = new int[size][size];
    private final float[][] mass = new float[size][size];
    private final Geometry[][] geometries = new Geometry[size][size];

    private final Label[][] labels = new Label[size][size];

    private final ColorRGBA[] block_colors = {
            ColorRGBA.Black.clone(),// color(255)  //air
            new ColorRGBA(0.7f, 0.7f, 0.35f, 1.0f), // color(200,200,100) //ground
            ColorRGBA.Blue.clone()
    };

    // the smallest amount to accept as a movement. To avoid tiny amounts transferring from one to another.
    private final float minFlow = 0.001f;

    private Label changesLabel;

    private WaterSim2() {
        // super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);

        cam.setLocation(new Vector3f(0, 0, 40));
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);

        Node blocksNode = new Node("Blocks");

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                Label label = new Label("1.0000");
                label.setFontSize(0.1f);
                label.setColor(ColorRGBA.Pink);
                label.setLocalTranslation(x - 2, y + label.getPreferredSize().y, 0.001f);
                blocksNode.attachChild(label);
                labels[x][y] = label;

                // set the perimeter to ground.
                if (x == 0 || x == size - 1 || y == 0 || y == size - 1) {
                    blocks[x][y] = GROUND;
                    continue;
                }

                // one in x chance of ground
                int groundChance = FastMath.nextRandomInt(0, 5);
                if (groundChance == 5) {
                    blocks[x][y] = GROUND;
                }
                else {
                    // one in x chance of water
                    int waterChance = FastMath.nextRandomInt(0, 10);
                    if (waterChance == 3) {
                        blocks[x][y] = WATER;
                    }
                    else {
                        blocks[x][y] = AIR;
                    }
                }

                if (blocks[x][y] == WATER) {
                    addChange(x, y, 1, true);
                }
                else {
                    //mass[x][y] = 0.0f;
                }

            }
        }

        Material unshaded = new Material(assetManager, Materials.UNSHADED);
        unshaded.setBoolean("VertexColor", true);

        for ( int x = 0; x < size; x++ ) {
            for (int y = 0; y < size; y++) {

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

        blocksNode.setLocalTranslation(
                -size * 0.5f,
                -size * 0.5f,
                0
        );
        rootNode.attachChild(blocksNode);

        changesLabel = new Label("");
        changesLabel.setLocalTranslation(10, cam.getHeight() - 10, 1);
        guiNode.attachChild(changesLabel);

    }

    // changes per frame.
    private final Map<GridPos, Float> changes = new HashMap<>();
    // private final Map<GridPos, Float> newChanges = new HashMap<>();

    private final float maxMass = 1.0f;

    private void setMeshColor(Mesh mesh, ColorRGBA color) {
        ColorRGBA[] colors = { color, color, color, color };
        mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));
    }

    private void setMeshSize(Mesh mesh, float size) {
        Quad quad = (Quad) mesh;
        quad.updateGeometry(1, size);
    }

    private void draw_block(int x, int y, ColorRGBA color, float filled) {

        Geometry geometry = geometries[x][y];
        setMeshColor(geometry.getMesh(), color);

        if (blocks[x][y] == WATER) {

//            // above
//            boolean waterAbove = false;
//
//            if (y + 1 < size - 1) {
//                waterAbove = blocks[x][y + 1] == WATER;
//            }
//
//            if (waterAbove) {
//                setMeshSize(geometry.getMesh(), 1);
//            }
//            else {
//                setMeshSize(geometry.getMesh(), FastMath.clamp(filled, 0.0f, 1.0f));
//            }
            setMeshSize(geometry.getMesh(), FastMath.clamp(filled, 0.0f, 1.0f));
        }
        else {
            setMeshSize(geometry.getMesh(), 1);
        }

    }

    private void addChange(int x, int y, float change, boolean add) {

        if (blocks[x][y] == GROUND) {
            System.out.println("ADDED A GROUND BLOCK");
            return;
        }

        //if (change <= 0) {
            // System.out.println("Ignored Zero Change: " + x + "," + y);
            //return;
        //}

        GridPos gp = new GridPos(x, y);

        Float newVal = changes.get(gp);

        if (newVal == null) {
            newVal = mass[x][y];
        }

        if (add) {
            newVal += change;
        }
        else {
            newVal -= change;
        }

//        if (newVal == mass[x][y]) {
//            System.out.println("dupe");
//            return;
//        }

        // if (newVal != 0) {
        changes.put(gp, newVal);
        // }

        // System.out.println("Change: " + x + "," + y + " " + change);
    }

    float time = 0;
    float frameTime = 1f / 10;

    private void setBlock(int x, int y, float val) {
        setBlock(x, y, val, true);
    }

    private void setBlock(int x, int y, float val, boolean addToChanges) {
        mass[x][y] = val;

        if (val <= 0) {
            blocks[x][y] = AIR;
            draw_block(x, y, ColorRGBA.Black, 1);
        }
        else {
            blocks[x][y] = WATER;
            draw_block(x, y, ColorRGBA.Blue, val);
        }

        if (addToChanges) {
            changes.put(new GridPos(x, y), mass[x][y]);
        }
    }

    Random random = new Random();
    private final String format = "%.4f";

    @Override
    public void simpleUpdate(float tpf) {

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                labels[x][y].setText(String.format(format, mass[x][y]));
            }
        }

//        time += tpf;
//
//        if (time < frameTime) {
//            return;
//        }
//
//        time -= frameTime;

        // for (Map.Entry<GridPos, Float> entry : changes.entrySet()) {

        Iterator<Map.Entry<GridPos, Float>> iterator = changes.entrySet().iterator();

        if (iterator.hasNext()) {

            Map.Entry<GridPos, Float> entry = iterator.next();
            iterator.remove();

            float flow, remaining_mass;

            int x = entry.getKey().x;
            int y = entry.getKey().y;

            remaining_mass = entry.getValue();
            setBlock(x, y, remaining_mass, false);
//
//            mass[x][y] = remaining_mass;
//
//            if (remaining_mass == 0) {
//                blocks[x][y] = AIR;
//                draw_block(x, y, ColorRGBA.Black, 1);
//            }
//            else {
//                blocks[x][y] = WATER;
//                draw_block(x, y, ColorRGBA.Blue, remaining_mass);
//            }

            if (remaining_mass <= 0) {
                return;
            }

            // below
            if (blocks[x][y - 1] != GROUND) {

                float block_mass = mass[x][y - 1];

                if (block_mass < maxMass) {

                    // the max this block can take.
                    float maxFlow = maxMass - block_mass;

                    // transfer the smallest amount,
                    flow = Math.min(remaining_mass, maxFlow);

                    if (flow > 0) {
                        setBlock(x, y, mass[x][y] - flow);

                        setBlock(x, y - 1, mass[x][y] + flow);

                        // addChange(x, y, flow, false);
                        // addChange(x, y - 1, flow, true);

                        remaining_mass -= flow;

                        return;
                    }
                }

            }

            if (remaining_mass <= 0) {
                return;
            }

            // Left
            if (blocks[x - 1][y] != GROUND) {

                if (mass[x - 1][y] < remaining_mass) {

                    // the most this block can take.
                    float maxFlow = (maxMass - mass[x - 1][y]) * 0.5f;

                    // subtract the remaining mass from the block on the left to get the difference.
                    // give the block half of the difference or maxFlow. Whichever is lowest.
                    float diff = (remaining_mass - mass[x - 1][y]) * 0.5f;

                    flow = Math.min(maxFlow, diff);

                    if (flow > minFlow) {
                        // addChange(x, y, flow, false);
                        // addChange(x - 1, y, flow, true);

                        setBlock(x, y, mass[x][y] - flow);

                        setBlock(x - 1, y, mass[x - 1][y] + flow);

                        remaining_mass -= flow;

                        System.out.println("flow left: " + flow);
                    }

                }

            }

            if (remaining_mass <= 0) {
                return;
            }

//            // Right
            if (blocks[x + 1][y] != GROUND) {

                if (mass[x + 1][y] < remaining_mass) {

                    float maxFlow = (maxMass - mass[x + 1][y]) * 0.5f;

                    // subtract the remaining mass from the block on the right to get the difference.
                    // give the block half of the difference or maxFlow. Whichever is lowest.
                    float diff = (remaining_mass - mass[x + 1][y]) * 0.5f;

                    flow = Math.min(maxFlow, diff);

                    if (flow > minFlow) {
                        // addChange(x, y, flow, false);
                        // addChange(x + 1, y, flow, true);

                        setBlock(x, y, mass[x][y] - flow);
                        changes.put(new GridPos(x, y), mass[x][y]);

                        setBlock(x + 1, y, mass[x + 1][y] + flow);
                        changes.put(new GridPos(x + 1, y), mass[x + 1][y]);

                        remaining_mass -= flow;

                        System.out.println("flow right: " + flow);
                    }

                }

            }

        }

//        for (Map.Entry<GridPos, Float> entry : changes.entrySet()) {
//            GridPos gp = entry.getKey();
//            Float val = entry.getValue();
//
//            if (blocks[gp.x][gp.y] != GROUND) {
//
//                mass[gp.x][gp.y] = val;
//
//                if (val > 0) {
//                    blocks[gp.x][gp.y] = WATER;
//                    draw_block(gp.x, gp.y, ColorRGBA.Blue, val);
//
//                } else {
//                    blocks[gp.x][gp.y] = AIR;
//                    draw_block(gp.x, gp.y, ColorRGBA.Black, 0);
//                }
//            } else {
//                System.out.println("Ground block in changes!");
//            }
//        }
        //changes.clear();



        // IteratorMap.Entry<GridPos, Float> iterator = newChanges.entrySet().iterator();

        // newChanges.entrySet().removeIf(entry -> {
//        for (Map.Entry<GridPos, Float> entry : newChanges.entrySet()) {
//            if (entry.getValue() > 0) {
//
//                blocks[entry.getKey().x][entry.getKey().y] = WATER;
//                mass[entry.getKey().x][entry.getKey().y] = entry.getValue();
//
//                draw_block(entry.getKey().x, entry.getKey().y, ColorRGBA.Blue, entry.getValue());
//
//                changes.put(entry.getKey(), entry.getValue());
//
//                // return false;
//
//            }
//            else {
//                blocks[entry.getKey().x][entry.getKey().y] = AIR;
//                mass[entry.getKey().x][entry.getKey().y] = 0;
//                draw_block(entry.getKey().x, entry.getKey().y, ColorRGBA.Black, 0);
//
//                // return true;
//            }
//        }//);
//        newChanges.clear();

        // finally
        changesLabel.setText("" + changes.size());

    }

}
