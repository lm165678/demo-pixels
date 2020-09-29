package com.jayfella.pixels.grid;


import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.physics.RigidBodyControl2D;

import java.util.function.Supplier;

public class CollidableGridCell implements Supplier<CollidableGridCell> {

    private final GridPos2i gridPos;
    private final OldSceneCollisionGrid collisionGrid;

    private RigidBodyControl2D result;

    CollidableGridCell(GridPos2i gridPos, OldSceneCollisionGrid collisionGrid) {
        this.gridPos = gridPos;
        this.collisionGrid = collisionGrid;
    }

    GridPos2i getGridPosition() {
        return gridPos;
    }

    RigidBodyControl2D getRigidBodyControl() {
        return result;
    }

    @Override
    public CollidableGridCell get() {
        this.result = collisionGrid.positionRequestedAsync(gridPos);
        return this;
    }

}