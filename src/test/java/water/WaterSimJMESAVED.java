package water;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
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

public class WaterSimJMESAVED extends SimpleApplication {

    public static void main(String... args) {

        WaterSimJMESAVED main = new WaterSimJMESAVED();

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

    // how full a block is visually.
    final float MinDraw = 0.01f;
    final float MaxDraw = 1.1f;

    private boolean stepping = false;

    private final int size = 32;

    private final Node blocksNode = new Node("Blocks");
    private Geometry[][] geometries = new Geometry[size][size];
    private int[][] blocks = new int[size][size];
    private float[][] mass = new float[size][size];
    // private float[][] new_mass = new float[size][size];


    boolean draw_depth = true; //draw partially filled cells differently, simulating more
    //fine-grained water depth display (imperfect)

    final int SHOW_NOTHING = 0;
    final int SHOW_MASS = 1;
    int show_state = SHOW_NOTHING; //whether to show the mass of each block

    // Configurable Properties

    //Water properties
    private final float MaxMass = 1.0f; // 1.0f
    private float MaxCompress = 0.02f; // 0.02f
    private float MinMass = 0.0001f; // 0.0001f

    // final float minflow = 0.01f;
    // float maxflow = 1;
    private float minflow = 0.01f;
    private float maxflow = 1;   //max units of water moved out of one block to another, per timestep
    private float smoothFlow = 0.5f;

    private VersionedReference<Double> maxCompressRef;
    private VersionedReference<Double> minMassRef;
    private VersionedReference<Double> minFlowRef;
    private VersionedReference<Double> maxFlowRef;
    private VersionedReference<Double> smoothFlowRef;
    private Label changesLabel;

    private WaterSimJMESAVED() {
        super(new StatsAppState());
    }



    ColorRGBA[] block_colors = {
            ColorRGBA.Black.clone(),// color(255)  //air
            new ColorRGBA(0.7f, 0.7f, 0.35f, 1.0f), // color(200,200,100) //ground
            ColorRGBA.Blue.clone()
    };

    @Override
    public void simpleInitApp() {

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);

        cam.setLocation(new Vector3f(0, 0, 40));

        Material unshaded = new Material(assetManager, Materials.UNSHADED);
        unshaded.setBoolean("VertexColor", true);

        Container container = new Container();
        container.setLocalTranslation(10,
                cam.getHeight() - 10,
                0);

        int row = 0;

        Slider slider;

        container.addChild(new Label("Min Flow"), row, 0);
        slider = container.addChild(new Slider(),row, 1);
        slider.setPreferredSize(new Vector3f(200, slider.getPreferredSize().y, 1));
        slider.getModel().setMinimum(0.001f);
        slider.getModel().setMaximum(100);
        slider.getModel().setValue(minflow);
        minFlowRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("Max Flow"), row, 0);
        slider = container.addChild(new Slider(), row, 1);
        slider.getModel().setMaximum(100);
        slider.getModel().setMinimum(0.1);
        slider.getModel().setValue(maxflow);
        maxFlowRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("Min Mass"), row, 0);
        slider = container.addChild(new Slider(), row, 1);
        slider.getModel().setMaximum(MaxMass);
        slider.getModel().setMinimum(0.0001f);
        slider.getModel().setValue(MinMass);
        minMassRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("Max Compress"), row, 0);
        slider = container.addChild(new Slider(), row, 1);
        slider.getModel().setMaximum(0.02f);
        slider.getModel().setMinimum(0.0001f);
        slider.getModel().setValue(MaxCompress);
        maxCompressRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("SmoothFlow"), row, 0);
        slider = container.addChild(new Slider(), row, 1);
        slider.getModel().setMaximum(0.9f);
        slider.getModel().setMinimum(0.1f);
        slider.getModel().setValue(smoothFlow);
        smoothFlowRef = slider.getModel().createReference();

        row++;

        container.addChild(new Label("Changes"), row, 0);
        changesLabel = container.addChild(new Label(""), row, 1);

        guiNode.attachChild(container);

        Button startSimButton = new Button("Restart Simulation");
        startSimButton.addClickCommands(source -> startSimulation());
        startSimButton.setLocalTranslation(10, container.getLocalTranslation().y - container.getPreferredSize().y - 10, 0);



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


        guiNode.attachChild(startSimButton);



        rootNode.attachChild(blocksNode);
        blocksNode.setLocalTranslation(
                -size * 0.5f,
                -size * 0.5f,
                0
        );

    }

    private void startSimulation() {

        changes.clear();
        newChanges.clear();

        //Fill the map with random blocks
        for ( int x = 1; x < size - 1; x++ ){
            for ( int y = 1; y < size - 1; y++ ){

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
        for (int x =0; x < size; x++){
            blocks[x][0] = GROUND;
            blocks[x][size - 1] = GROUND;
        }

        // bottom and left = air
        for (int y = 1; y <size - 1; y++){
            blocks[0][y] = GROUND;
            blocks[size - 1][y] = GROUND;
        }

        for ( int x = 0; x < size; x++ ) {
            for (int y = 0; y < size; y++) {

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

    private void draw_block(int x, int y, ColorRGBA color, float filled) {

        Geometry geometry = geometries[x][y];
        setMeshColor(geometry.getMesh(), color);

        if (blocks[x][y] == WATER) {

            // boolean airSurrounding = false;

//            // above
//            if (y + 1 < size - 1) {
//                airSurrounding = blocks[x][y + 1] == AIR;
//            }
//
//            // right
//            if (!airSurrounding) {
//                if (x + 1 < size - 1) {
//                    airSurrounding = blocks[x + 1][y] == AIR;
//                }
//            }
//
//            // below
//            if (!airSurrounding) {
//                if (y - 1 >= 0) {
//                    airSurrounding = blocks[x][y - 1] == AIR;
//                }
//            }
//
//            // left
//            if (!airSurrounding) {
//                if (x - 1 >= 0) {
//                    airSurrounding = blocks[x - 1][y] == AIR;
//                }
//            }
//
//            if (airSurrounding) {
//                setMeshSize(geometry.getMesh(), FastMath.clamp(filled, 0.0f, 1.0f));
//            }
//            else {
//                setMeshSize(geometry.getMesh(), 1);
//            }

            // above

            boolean waterAbove = false;

            if (y + 1 < size - 1) {
                waterAbove = blocks[x][y + 1] == WATER;
            }

            if (waterAbove) {
                setMeshSize(geometry.getMesh(), 1);
            }
            else {
                setMeshSize(geometry.getMesh(), FastMath.clamp(filled, 0.1f, 1.0f));
            }

        }
        else {
            setMeshSize(geometry.getMesh(), 1);
        }

    }

    private ColorRGBA waterColor(float m) {

        // m = constrain( m, MinDraw, MaxDraw );
//        if (m < MinDraw) m = MinDraw;
//        else if (m > MaxDraw) m = MaxDraw;

//        int r = 50, g = 50;
//        int b;

//        if (m < 1){
//            b = int(map(m, 0.01, 1, 255, 200));
//            r = int(map(m, 0.01, 1, 240, 50));
//            r = constrain(r, 50, 240);
//            g = r;
//        } else {
//            b = int(map(m, 1, 1.1, 190, 140));
//        }
//
//        b = constrain(b, 140, 255);

//        return color( r, g, b );

        return new ColorRGBA(0, 0, m, 1.0f);

    }

    private void updateGui(float tpf) {

        if (minFlowRef.update()) {
            minflow = minFlowRef.get().floatValue();
        }

        if (maxFlowRef.update()) {
            maxflow = maxFlowRef.get().floatValue();
        }

        if (minMassRef.update()) {
            MinMass = minMassRef.get().floatValue();
        }

        if (maxCompressRef.update()) {
            MaxCompress = maxCompressRef.get().floatValue();
        }

        if (smoothFlowRef.update()) {
            smoothFlow = smoothFlowRef.get().floatValue();
        }

    }

    float time = 0;
    float frameTime = 1f / 30;

    @Override
    public void simpleUpdate(float tpf) {

        time += tpf;

        if (time < frameTime) {
            return;
        }

        time -= frameTime;

        updateGui(tpf);

        //Run the water simulation (unless we're in the step-by-step mode)
        if (!stepping) {
            simulate_compression(tpf);
        }

        //Draw the map
        // float h;
        // ColorRGBA c;

//        for ( int x = 1; x < size - 1; x++ ){
//            for ( int y = 1; y < size - 1; y++ ){
//                if ( blocks[x][y] == WATER ){
//
//                    //Skip cells that contain very little water
//                    //if (mass[x][y] < MinDraw) continue;
//
//                    //Draw water
//                    if ( draw_depth && ( mass[x][y] < MaxMass ) ){
//                        //Draw a half-full block. Block size is dependent on the amount of water in it.
//                        // if ( mass[x][y+1] >= MinDraw ){
//                            // draw_block( x, y, waterColor(mass[x][y+1]), 1);
//                            // draw_block( x, y, ColorRGBA.Blue.clone(), 1);
//                        // }
//                        // draw_block( x, y, waterColor(mass[x][y]), mass[x][y]);
//                        draw_block( x, y, block_colors[2], mass[x][y]);
//
//                    } else {
//                        //Draw a full block
//                        // h = 1;
//                        draw_block( x, y, block_colors[2], 1);
//                    }
//
//                } else {
//                    //Draw any other block
//                    draw_block( x, y, block_colors[ blocks[x][y] ], 1 );
//                    //setMeshSize(geometries[x][y].getMesh(), 1);
//                }
//
//            }
//        }
    }



    private final Map<GridPos, Float> changes = new HashMap<>();
    private final Map<GridPos, Float> newChanges = new HashMap<>();

    private void addChange(int x, int y, float change, boolean add) {

        if (blocks[x][y] == GROUND) {
            System.out.println("ADDED A GROUND BLOCK");
        }

        if (change <= 0) {
            // System.out.println("Ignored Zero Change: " + x + "," + y);
            return;
        }

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

        if (newVal == mass[x][y]) {
            //System.out.println("dupe");
            return;
        }

        // if (newVal != 0) {
        newChanges.put(gp, newVal);
        // }

        // System.out.println("Change: " + x + "," + y + " " + change);
    }


    // Ideas:
    // If there is ANY air in the 4 cardinal directions then it's moving.
    // If there is ALL WATER in each cardinal direction then it's a full square.
    // Feed the direction of flow to the block to determine which way to scale the block.
    // sideward movement = scale the top down (gets shorter with mass).
    // vertical movement = scale it horizontally (gets thinner with mass).
    // vertical movement scales from the center(?). Horizontal movement scales from the bottom.

    void simulate_compression(float tpf) {

        float flow;
        float remaining_mass;

        //Calculate and apply flow for each block
        for (Map.Entry<GridPos, Float> entry : changes.entrySet()) {

            int x = entry.getKey().x;
            int y = entry.getKey().y;

            //Custom push-only flow

            remaining_mass = entry.getValue();

            if (remaining_mass <= 0) {
                continue;
            }

            //The block below this one
            if (blocks[x][y - 1] != GROUND) {

                flow = get_stable_state_b(remaining_mass + mass[x][y - 1]) - mass[x][y - 1];

                if (flow > minflow) {
                    flow *= smoothFlow; //leads to smoother flow
                }

                // Flow = constrain( Flow, 0, min(MaxSpeed, remaining_mass) );
                flow = FastMath.clamp(flow, 0, Math.min(maxflow, remaining_mass));

                // new_mass[x][y] -= flow;
                // new_mass[x][y-1] += flow;
                addChange(x, y, flow, false);
                addChange(x, y - 1, flow, true);

                remaining_mass -= flow;
            }

            if (remaining_mass <= 0) {
                continue;
            }

            //Left
            if (blocks[x - 1][y] != GROUND) {

                //Equalize the amount of water in this block and it's neighbour
                flow = (mass[x][y] - mass[x - 1][y]) / 4f;

                if (flow > minflow) {
                    flow *= smoothFlow; //leads to smoother flow
                }

                // Flow = constrain(Flow, 0, remaining_mass);
                flow = FastMath.clamp(flow, 0, remaining_mass);

                // new_mass[x][y] -= flow;
                // new_mass[x-1][y] += flow;
                addChange(x, y, flow, false);
                addChange(x - 1, y, flow, true);

                remaining_mass -= flow;
            }

            if (remaining_mass <= 0) {
                continue;
            }

            //Right
            if (blocks[x + 1][y] != GROUND) {

                //Equalize the amount of water in this block and it's neighbour
                flow = (mass[x][y] - mass[x + 1][y]) / 4f;
                if (flow > minflow) {
                    flow *= smoothFlow; //leads to smoother flow
                }

                // Flow = constrain(Flow, 0, remaining_mass);
                flow = FastMath.clamp(flow, 0, remaining_mass);

                // new_mass[x][y] -= flow;
                // new_mass[x+1][y] += flow;
                addChange(x, y, flow, false);
                addChange(x + 1, y, flow, true);

                remaining_mass -= flow;
            }

            if (remaining_mass <= 0) {
                continue;
            }

            //Up. Only compressed water flows upwards.
            if (blocks[x][y + 1] != GROUND) {

                flow = remaining_mass - get_stable_state_b(remaining_mass + mass[x][y + 1]);

                if (flow > minflow) {
                    flow *= smoothFlow; //leads to smoother flow
                }

                // Flow = constrain( Flow, 0, min(MaxSpeed, remaining_mass) );
                flow = FastMath.clamp(flow, 0, Math.min(maxflow, remaining_mass));

                // new_mass[x][y] -= flow;
                // new_mass[x][y+1] += flow;
                addChange(x, y, flow, false);
                addChange(x, y + 1, flow, true);
                //remaining_mass -= flow;
            }

            // }
        }

        for (Map.Entry<GridPos, Float> entry : changes.entrySet()) {
            GridPos gp = entry.getKey();
            Float val = entry.getValue();

            mass[gp.x][gp.y] = val;

            if (blocks[gp.x][gp.y] != GROUND) {

                if (val > MinMass) {
                    blocks[gp.x][gp.y] = WATER;
                    draw_block(gp.x, gp.y, ColorRGBA.Blue, val);
                } else {
                    blocks[gp.x][gp.y] = AIR;
                    draw_block(gp.x, gp.y, ColorRGBA.Black, 0);
                }
            } else {
                System.out.println("Ground block in changes!");
            }
        }
        changes.clear();

        for (Map.Entry<GridPos, Float> entry : newChanges.entrySet()) {
            if (entry.getValue() > 0) {
                changes.put(entry.getKey(), entry.getValue());
            }
            else {
                blocks[entry.getKey().x][entry.getKey().y] = AIR;
                draw_block(entry.getKey().x, entry.getKey().y, ColorRGBA.Black, 0);
            }
        }

        //float end = System.currentTimeMillis();

        //System.out.println("Took: " + (end - start) + "ms");

        //Copy the new mass values to the mass array
//        for (int x = 0; x < size; x++){
//            for (int y = 0; y < size; y++){
//                mass[x][y] = new_mass[x][y];
//            }
//        }

//        for (int x = 1; x < size - 1; x++){
//            for(int y = 1; y < size - 1; y++){
//
//                //Skip ground blocks
//                if(blocks[x][y] == GROUND) {
//                    continue;
//                }
//
//                //Flag/unflag water blocks
//                if (mass[x][y] > MinMass){
//                    blocks[x][y] = WATER;
//                } else {
//                    blocks[x][y] = AIR;
//                }
//            }
//        }

        //Remove any water that has left the map
//        for (int x =0; x < size; x++){
//            mass[x][0] = 0;
//            mass[x][size - 1] = 0;
//        }
//        for (int y = 1; y < size - 1; y++){
//            mass[0][y] = 0;
//            mass[size - 1][y] = 0;
//        }

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
