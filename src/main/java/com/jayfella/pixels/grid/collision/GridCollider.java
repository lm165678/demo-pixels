package com.jayfella.pixels.grid.collision;

import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.grid.OldSceneCollisionGrid;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jme3.math.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class GridCollider {

    private final GridSettings gridSettings;
    private final OldSceneCollisionGrid collisionGrid;

    // view distance changes with velocity
    private int vd_x_l,vd_x_r, vd_z_f, vd_z_b;

    private final Set<GridPos2i> requiredPositions = new HashSet<>();

    private final GridPos2i currentGridPos, lastGridPos;

    public GridCollider(GridSettings gridSettings, OldSceneCollisionGrid collisionGrid) {
        this.gridSettings = gridSettings;
        this.collisionGrid = collisionGrid;

        this.vd_x_l = vd_x_r = vd_z_f = vd_z_b = gridSettings.getViewDistance();

        this.currentGridPos = new GridPos2i(0, 0, gridSettings.getCellSize().getBitshift());
        this.lastGridPos = new GridPos2i(-1, 0, gridSettings.getCellSize().getBitshift());
    }

    public GridSettings getGridSettings() {
        return gridSettings;
    }

    public void invalidatePosition() {
        this.lastGridPos.set(-currentGridPos.getX(), -currentGridPos.getY());
        setLocation(currentGridPos.toWorldTranslation());
    }

    public void setVd_x_l(int vd_x_l) {

        if (this.vd_x_l == vd_x_l) {
            return;
        }

        this.vd_x_l = vd_x_l;
        invalidatePosition();
    }

    public void setVd_x_r(int vd_x_r) {

        if (this.vd_x_r == vd_x_r) {
            return;
        }

        this.vd_x_r = vd_x_r;
        invalidatePosition();
    }

    public void setVd_z_f(int vd_z_f) {

        if (this.vd_z_f == vd_z_f) {
            return;
        }

        this.vd_z_f = vd_z_f;
        invalidatePosition();
    }

    public void setVd_z_b(int vd_z_b) {

        if (this.vd_z_b == vd_z_b) {
            return;
        }

        this.vd_z_b = vd_z_b;
        invalidatePosition();
    }

    public void setLocation(Vector3f location) {

        currentGridPos.setFromWorldLocation(location);

        if (currentGridPos.equals(lastGridPos)) {
            return;
        }

        requiredPositions.clear();

        for (int x = currentGridPos.getX() - vd_x_l; x <= currentGridPos.getX() + vd_x_r; x++) {
            for (int y = currentGridPos.getY() - vd_z_f; y <= currentGridPos.getY() + vd_z_b; y++) {

                GridPos2i newGridPosition = new GridPos2i(x, y, gridSettings.getCellSize().getBitshift());

                collisionGrid.positionRequested(newGridPosition);
                requiredPositions.add(newGridPosition);
            }
        }

        lastGridPos.set(currentGridPos);
    }

    public void update() {
        collisionGrid.addRequiredPositions(requiredPositions);
    }

}
