package com.jayfella.pixels.player;

import com.jayfella.pixels.core.CellSize;
import com.jayfella.pixels.grid.OldSceneCollisionGrid;
import com.jayfella.pixels.grid.collision.GridCollider;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jayfella.pixels.physics.RigidBodyControl2D;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Responsible for loading physics meshes around itself.
 */
public class VelocityBasedPhysicsControl extends AbstractControl {

    private final RigidBodyControl2D rigidBodyControl;
    private final GridCollider gridCollider;

    private final float velocityDistMult = 0.3f;

    public VelocityBasedPhysicsControl(RigidBodyControl2D rigidBodyControl, OldSceneCollisionGrid collisionGrid) {

        this.rigidBodyControl = rigidBodyControl;

        GridSettings gridSettings = new GridSettings();
        gridSettings.setCellSize(CellSize.Size_8);
        gridSettings.setViewDistance(2);

        gridCollider = new GridCollider(gridSettings, collisionGrid);
    }

    public void invalidate() {
        gridCollider.invalidatePosition();
    }

    @Override
    protected void controlUpdate(float tpf) {

        Vector2f linearVelocity = rigidBodyControl.getLinearVelocity();


        if (linearVelocity.x > 0 ) {
            this.gridCollider.setVd_x_r(Math.round(Math.max(this.gridCollider.getGridSettings().getViewDistance(), linearVelocity.x * velocityDistMult)));
            this.gridCollider.setVd_x_l(this.gridCollider.getGridSettings().getViewDistance());
        }
        else if (linearVelocity.x < 0) {
            this.gridCollider.setVd_x_l(Math.round(Math.max(this.gridCollider.getGridSettings().getViewDistance(), -linearVelocity.x * velocityDistMult)));
            this.gridCollider.setVd_x_r(this.gridCollider.getGridSettings().getViewDistance());
        }

        if (linearVelocity.y > 0) {
            this.gridCollider.setVd_z_b(Math.round(Math.max(this.gridCollider.getGridSettings().getViewDistance(), linearVelocity.y * velocityDistMult)));
            this.gridCollider.setVd_z_f(this.gridCollider.getGridSettings().getViewDistance());
        }
        else if (linearVelocity.y < 0) {
            this.gridCollider.setVd_z_f(Math.round(Math.max(this.gridCollider.getGridSettings().getViewDistance(), -linearVelocity.y * velocityDistMult)));
            this.gridCollider.setVd_z_b(this.gridCollider.getGridSettings().getViewDistance());
        }

        this.gridCollider.setLocation(getSpatial().getLocalTranslation());
        this.gridCollider.update();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}

