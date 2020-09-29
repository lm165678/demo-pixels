package noise;

import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.core.WorldConstants;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.props.PropertyPanel;
import com.simsilica.lemur.style.BaseStyles;

import java.nio.ByteBuffer;

// use a texture to draw some some so we can see what it's generating.
public class TestWorldNoise extends SimpleApplication {

    public static void main(String... args) {

        TestWorldNoise main = new TestWorldNoise();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setResolution(1280, 720);
        appSettings.setAudioRenderer(null);
        appSettings.setFrameRate(90);

        main.setSettings(appSettings);
        main.setShowSettings(false);
        main.setPauseOnLostFocus(false);
        main.start();
    }

    // texture size
    int width, height;

    private ImageRaster imageRaster;
    private NoiseEvaluator noiseEvaluator;
    private NoiseEvaluator oreEvaluator;

    // gui

//    private TestWorldNoise() {
//        super(new AppState[0]);
//    }

    @Override
    public void simpleInitApp() {

        width = cam.getWidth();
        height = WorldConstants.MAX_HEIGHT;

        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.6f, 0.7f, 1.0f));

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);
        cam.setLocation(new Vector3f(width * 0.5f, height * 0.5f, 375));

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);

        // noise generator;
        noiseEvaluator = new TestWorldNoiseGenerator();
        oreEvaluator = new TestWorldOresNoiseGenerator();

        // noisy texture


        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        Image result = new Image(Image.Format.RGBA8, width, height, buffer, ColorSpace.sRGB);
        imageRaster = ImageRaster.create(result);

        Material material = new Material(assetManager, Materials.UNSHADED);
        Mesh mesh = new Quad(width, height);

        Geometry geometry = new Geometry("Geometry", mesh);
        geometry.setMaterial(material);

        Texture texture = new Texture2D(result);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        material.setTexture("ColorMap", texture);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        rootNode.attachChild(geometry);

        // GUI
        Container container = new Container();
        TabbedPanel tabbedPanel = container.addChild(new TabbedPanel());

        // world noise
        PropertyPanel noiseProps = new PropertyPanel(null);
        noiseProps.addIntProperty("Seed", noiseEvaluator, "seed", 0, Integer.MAX_VALUE, 1);
        noiseProps.addFloatProperty("Surface Height", noiseEvaluator, "surfaceHeight", 0, 720, 1);
        noiseProps.addFloatProperty("Turbulence X", noiseEvaluator, "turbulenceX", 0, 500, 1);
        noiseProps.addFloatProperty("Turbulence Y", noiseEvaluator, "turbulenceY", 0, 500, 1);
        noiseProps.addFloatProperty("Cave Height", noiseEvaluator, "caveHeight", 0, 1, 0.01f);
        noiseProps.addFloatProperty("Cave Size", noiseEvaluator, "caveSize", 0, 1, 0.1f);

        tabbedPanel.addTab("World Noise", noiseProps);

        // world ores
        PropertyPanel oresProps = new PropertyPanel(null);
        oresProps.addIntProperty("Seed", oreEvaluator, "seed", 0, Integer.MAX_VALUE, 1);
        oresProps.addFloatProperty("Dirt Height", oreEvaluator, "dirtHeight", 0, 720, 1);
        oresProps.addFloatProperty("Dirt Turbulence", oreEvaluator, "dirtTurbulence", 0, 500, 1);

        tabbedPanel.addTab("Ores Noise", oresProps);


        Button generateButton = container.addChild(new Button("Generate"));
        generateButton.addClickCommands(source -> generateTexture());

        container.setLocalTranslation(10, cam.getHeight() - 10, 1);
        guiNode.attachChild(container);

        generateTexture();
    }

    private void generateTexture() {

        // for now
        // ((TestWorldNoiseGenerator)noiseEvaluator).setSeed(System.currentTimeMillis());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                float noise = noiseEvaluator.evaluate(x, y);

                // ColorRGBA color = new ColorRGBA(noise, noise, noise, 1.0f);
                int blockType = (int) oreEvaluator.evaluate(x, y);

                ColorRGBA pixelColor = TestWorldOresNoiseGenerator.blockTypes.get(blockType).clone();
                pixelColor.multLocal(noise);

                if (pixelColor.r > 0 || pixelColor.g > 0 || pixelColor.b > 0) {
                    pixelColor.a = 1;
                }
                imageRaster.setPixel(x,y, pixelColor);

            }
        }

    }

}
