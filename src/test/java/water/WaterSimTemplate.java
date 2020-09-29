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
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;

public abstract class WaterSimTemplate extends SimpleApplication {

    //Block types
    protected final int AIR = 0;
    protected final int GROUND = 1;
    protected final int WATER = 2;

    protected final int size = 32;

    protected final int[][] blocks = new int[size][size];
    protected final float[][] mass = new float[size][size];
    private final Geometry[][] geometries = new Geometry[size][size];

    private final Label[][] labels = new Label[size][size];
    protected Label changesLabel;

    private final ColorRGBA[] block_colors = {
            ColorRGBA.Black.clone(),// color(255)  //air
            new ColorRGBA(0.7f, 0.7f, 0.35f, 1.0f), // color(200,200,100) //ground
            ColorRGBA.Blue.clone()
    };

    public abstract void addChange(int x, int y);

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
                    int waterChance = FastMath.nextRandomInt(0, 30);
                    if (waterChance == 3) {
                        blocks[x][y] = WATER;
                    }
                    else {
                        blocks[x][y] = AIR;
                    }
                }

                if (blocks[x][y] == WATER) {
                    mass[x][y] = 1;
                    addChange(x, y);
                }
                else {
                    mass[x][y] = 0.0f;
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

    private void setMeshColor(Mesh mesh, ColorRGBA color) {
        ColorRGBA[] colors = { color, color, color, color };
        mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));
    }

    private void setMeshSize(Mesh mesh, float size) {
        Quad quad = (Quad) mesh;
        quad.updateGeometry(1, size);
    }

    protected void draw_block(int x, int y, float filled) {

        Geometry geometry = geometries[x][y];


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
            setMeshColor(geometry.getMesh(), ColorRGBA.Blue);
        }
        else {
            setMeshSize(geometry.getMesh(), 1);
            setMeshColor(geometry.getMesh(), ColorRGBA.Black);
        }

    }

    private final String format = "%.4f";

    @Override
    public void simpleUpdate(float tpf) {

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                labels[x][y].setText(String.format(format, mass[x][y]));
            }
        }

    }
}
