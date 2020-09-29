package com.jayfella.pixels.player;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerInputState extends BaseAppState implements ActionListener, AnalogListener {

    private static final Logger log = LoggerFactory.getLogger(PlayerInputState.class);

    private final Player player;

    private static final String MOVE_FORWARD = "MOVE_FORWARD";
    private static final String MOVE_BACKWARD = "MOVE_BACKWARD";
    private static final String JUMP = "JUMP";

    private static final String ZOOM_IN = "ZOOM_IN";
    private static final String ZOOM_OUT = "ZOOM_OUT";

    private static final String ADD_BLOCK = "ADD_BLOCK";
    private static final String REMOVE_BLOCK = "REMOVE_BLOCK";

    private MousePicker mousePicker;

    public PlayerInputState(Player player) {
        this.player = player;
    }

    @Override
    protected void initialize(Application app) {

        this.mousePicker = new MousePicker(app.getInputManager(), app.getCamera());

        app.getInputManager().addMapping(MOVE_FORWARD, new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping(MOVE_BACKWARD, new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping(JUMP, new KeyTrigger(KeyInput.KEY_SPACE));

        app.getInputManager().addMapping(ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        app.getInputManager().addMapping(ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        app.getInputManager().addMapping(ADD_BLOCK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping(REMOVE_BLOCK, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        app.getInputManager().addListener(this,
                MOVE_FORWARD, MOVE_BACKWARD, JUMP,
                ZOOM_IN, ZOOM_OUT,
                ADD_BLOCK, REMOVE_BLOCK);

    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    private final float force = 100.0f;
    private final float jumpForce = 10.0f;

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {

        switch (name) {

            case MOVE_FORWARD: {
                // player.getRigidBodyControl2D().applyImpulse(force * tpf, 0);
                forward = isPressed;
                break;
            }

            case MOVE_BACKWARD: {
                // player.getRigidBodyControl2D().applyImpulse(-force * tpf, 0);
                backward = isPressed;
                break;
            }

            case JUMP: {
                if (isPressed) {
                    player.getRigidBodyControl2D().applyImpulse(0, jumpForce);
                }

                break;
            }

            case ADD_BLOCK: {

                if (isPressed) {

                    Vector3f hitpoint = mousePicker.getClickLocation();

                    if (hitpoint != null) {

                        float distance = player.getLocation().distance(hitpoint);

                        if (distance < 10) {
                            player.getWorld().addBlock(1, new Vector2f((int) hitpoint.x, (int) hitpoint.y));
                        }
                    }

                }

                break;
            }

            case REMOVE_BLOCK: {

                if (isPressed) {

                    Vector3f hitpoint = mousePicker.getClickLocation();

                    if (hitpoint != null) {

                        float distance = player.getLocation().distance(hitpoint);

                        if (distance < 10) {
                            player.getWorld().addBlock(0, new Vector2f((int) hitpoint.x, (int) hitpoint.y));
                        }
                    }

                }

                break;
            }

        }

    }

    private boolean forward, backward;
    private final float maxVelocity = 4;

    @Override
    public void update(float tpf) {

        if (forward) {
            player.getRigidBodyControl2D().applyImpulse(force * tpf, 0);

            Vector2f linearVelocity = player.getRigidBodyControl2D().getLinearVelocity();

            if (linearVelocity.x > maxVelocity) {
                player.getRigidBodyControl2D().setLinearVelocity(maxVelocity, linearVelocity.y);
            }

        }
        else if (backward) {
            player.getRigidBodyControl2D().applyImpulse(-force * tpf, 0);

            Vector2f linearVelocity = player.getRigidBodyControl2D().getLinearVelocity();

            if (linearVelocity.x < -maxVelocity) {
                player.getRigidBodyControl2D().setLinearVelocity(-maxVelocity, linearVelocity.y);
            }
        }

        // test highlighting a block we're hovering over.
        // highlightBlock();
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {

        switch (name) {

            case ZOOM_IN: {
                player.setZoom(player.getZoom() - value);
                break;
            }

            case ZOOM_OUT: {
                player.setZoom(player.getZoom() + value);
                break;
            }

        }

    }


}
