package com.jayfella.pixels.grid;

import com.jayfella.pixels.world.Chunk;
import com.jayfella.pixels.world.World;

import java.util.concurrent.Callable;

public class ChunkLoadTask implements Callable<Chunk> {

    private int gridPosition;
    private final World world;

    public ChunkLoadTask(int gridPosition, World world) {
        this.gridPosition = gridPosition;
        this.world = world;
    }

    @Override
    public Chunk call() throws Exception {
        return new Chunk(gridPosition, world);
    }

}
