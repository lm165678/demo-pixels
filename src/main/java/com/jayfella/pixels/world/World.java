package com.jayfella.pixels.world;

import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.grid.SceneGrid;
import com.jayfella.pixels.player.Player;
import com.jayfella.pixels.tile.Block;
import com.jayfella.pixels.world.settings.WorldSettings;
import com.jme3.app.Application;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;

import java.util.concurrent.ExecutorService;

public interface World {

    WorldSettings getWorldSettings();

    Node getWorldNode();

    SceneGrid getSceneGrid();

    Material getWorldMaterial();

    ExecutorService getThreadPool();
    NoiseEvaluator getWorldNoiseEvaluator();

    Application getApplication();
    Player getPlayer();

    ChunkGenerator getChunkGenerator();

    void addBlock(int type, Vector2f... worldLocations);
    void deleteBlock(Vector2f... worldLocations);

    Block getBlock(int x, int y);
    Chunk getChunk(int xGrid);
}
