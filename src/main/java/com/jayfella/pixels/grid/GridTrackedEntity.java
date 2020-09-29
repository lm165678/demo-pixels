package com.jayfella.pixels.grid;

import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.entity.Entity;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jme3.math.Vector3f;

public class GridTrackedEntity {

    private final Entity entity;
    private final GridSettings gridSettings;

    private final GridPos2i currentGridPosition;
    private final GridPos2i lastGridPosition;

    public GridTrackedEntity(Entity entity, GridSettings gridSettings) {
        this.entity = entity;
        this.gridSettings = gridSettings;

        currentGridPosition = new GridPos2i(Integer.MIN_VALUE, Integer.MIN_VALUE, gridSettings.getCellSize().getBitshift());
        lastGridPosition = new GridPos2i(Integer.MAX_VALUE, Integer.MAX_VALUE, gridSettings.getCellSize().getBitshift());
    }

    public Entity getEntity() {
        return entity;
    }

    public Vector3f getLocation() {
        return entity.getLocation();
    }

    public GridPos2i getCurrentGridPosition() {
        return currentGridPosition;
    }

    public GridPos2i getLastGridPosition() {
        return lastGridPosition;
    }

}
