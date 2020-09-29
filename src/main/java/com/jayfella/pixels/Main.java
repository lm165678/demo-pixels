package com.jayfella.pixels;

import com.jayfella.pixels.core.LogUtils;
import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.physics.Dyn4jAppState;
import com.jayfella.pixels.player.Player;
import com.jayfella.pixels.player.PlayerInputState;
import com.jayfella.pixels.world.WorldNoiseEvaluator;
import com.jayfella.pixels.world.WorldState;
import com.jayfella.pixels.world.settings.WorldSettings;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Main extends SimpleApplication {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {

        initializeLogger(Level.INFO);
        log.info("Engine Version: " + JmeSystem.getFullName());
        log.info("Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));

        Main main = new Main();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setResolution(1280, 720);
        appSettings.setTitle("Pixels");
        appSettings.setAudioRenderer(null);

        main.setSettings(appSettings);
        main.setShowSettings(false);
        // main.setPauseOnLostFocus(false);
        main.start();
    }

    private static void initializeLogger(Level logLevel) {

        LogUtils.initializeLogger(logLevel, true);

        Arrays.stream(new String[] {
                "com.simsilica.lemur.GuiGlobals",
                "com.simsilica.lemur.style.BaseStyles",
        }).forEach(p -> LogManager.getLogger(p).setLevel(Level.WARN));

        Arrays.stream(new String[] {
                "com.jme3.audio.openal.ALAudioRenderer",
                "com.jme3.asset.AssetConfig",
                "com.jme3.material.plugins.J3MLoader",

                // startup information
                "com.jme3.system.JmeSystem",
                "com.jme3.system.lwjgl.LwjglContext",
                // "com.jme3.renderer.opengl.GLRenderer"
        }).forEach(p -> LogManager.getLogger(p).setLevel(Level.ERROR));

    }

    private Player player;

    private Main() {
        super(new StatsAppState());
        // super(new AppState[0]);
    }

    private Label label;

    @Override
    public void simpleInitApp() {

        setCamera(45f, 0.1f, 100f);

        log.info("Initializing game...");

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);

        viewPort.setBackgroundColor(new ColorRGBA(0.2f, 0.3f, 0.4f, 1.0f));

        Dyn4jAppState dyn4jAppState = new Dyn4jAppState();
        dyn4jAppState.setDebugEnabled(false);
        stateManager.attach(dyn4jAppState);

        cam.setLocation(new Vector3f(0, 0, 60));

        player = new Player(assetManager, new Vector3f(0, 205, 0));

        WorldSettings worldSettings = new WorldSettings();
        worldSettings.setName("My World");
        worldSettings.setNumThreads(3);
        worldSettings.setSeed(654);


        NoiseEvaluator worldNoise = new WorldNoiseEvaluator(worldSettings.getSeed());
        WorldState worldState = new WorldState(worldSettings, worldNoise, player);

        stateManager.attach(worldState);
        stateManager.attach(new PlayerInputState(player));

        Container container = new Container();
        label = container.addChild(new Label("123"));
        container.setLocalTranslation(10, cam.getHeight() - 10, 0);
        guiNode.attachChild(container);

        log.info("Ready.");

    }

    private void setCamera(float fov, float near, float far) {

        float aspect = (float)cam.getWidth() / (float)cam.getHeight();
        cam.setFrustumPerspective(fov, aspect, near, far);
    }

    private static final String locFormat = "Location: [ %.2f | %.2f | %.2f ]";

    @Override
    public void simpleUpdate(float tpf) {

        cam.setLocation(new Vector3f(
                player.getLocation().x,
                player.getLocation().y,
                player.getZoom())
        );

        label.setText(String.format(locFormat, player.getLocation().x, player.getLocation().y, player.getLocation().z));

    }





}
