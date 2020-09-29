package com.jayfella.pixels.grid.collision;

import com.jayfella.pixels.core.GridPos2i;

import java.util.concurrent.Callable;

public class CollisionCellLoadTask implements Callable<GreedyCollisionCell> {

    private final GridPos2i gridPos;
    private final SceneCollisionGrid collisionGrid;

    public CollisionCellLoadTask(GridPos2i gridPos, SceneCollisionGrid collisionGrid) {
        this.gridPos = gridPos;
        this.collisionGrid = collisionGrid;
    }

    @Override
    public GreedyCollisionCell call() throws Exception {
        return new GreedyCollisionCell(gridPos, collisionGrid);

    }
}
