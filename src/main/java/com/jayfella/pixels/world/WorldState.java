package com.jayfella.pixels.world;

import com.jayfella.pixels.core.CellSize;
import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.grid.SceneGrid;
import com.jayfella.pixels.grid.collision.SceneCollisionGrid;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jayfella.pixels.physics.Dyn4jAppState;
import com.jayfella.pixels.player.Player;
import com.jayfella.pixels.player.inventory.PlayerInventoryGuiState;
import com.jayfella.pixels.tile.Block;
import com.jayfella.pixels.world.settings.WorldSettings;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldState extends BaseAppState implements World {


    private final WorldSettings worldSettings;
    private final Node worldNode;
    private final Player player;

    private final ExecutorService threadPool;
    private final NoiseEvaluator worldNoiseEvaluator;

    private final SceneGrid sceneGrid;
    private SceneCollisionGrid collisionGrid;

    private ChunkGenerator chunkGenerator;
    private Material worldMaterial;

    private Container debugContainer;
    private Label lightAdditionsLabel;
    private Label lightRemovalsLabel;

    public WorldState(WorldSettings worldSettings, NoiseEvaluator worldNoiseEvaluator, Player player) {
        this.worldSettings = worldSettings;
        this.worldNoiseEvaluator = worldNoiseEvaluator;
        this.player = player;

        player.setWorld(this);

        worldNode = new Node("World: " + worldSettings.getName());
        threadPool = Executors.newFixedThreadPool(worldSettings.getNumThreads());

        GridSettings terrainSettings = new GridSettings();
        terrainSettings.setViewDistance(3);
        this.sceneGrid = new SceneGrid(this, terrainSettings);
    }

    @Override
    public WorldSettings getWorldSettings() {
        return worldSettings;
    }

    @Override
    public Node getWorldNode() {
        return worldNode;
    }

    @Override
    public SceneGrid getSceneGrid() {
        return sceneGrid;
    }

    @Override
    public Material getWorldMaterial() {
        return worldMaterial;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    @Override
    public NoiseEvaluator getWorldNoiseEvaluator() {
        return worldNoiseEvaluator;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    @Override
    protected void initialize(Application app) {

        // worldMaterial = createMaterial(app.getAssetManager());
        // worldMaterial = createWangMaterial(app.getAssetManager());
        worldMaterial = createSpriteMaterial(app.getAssetManager());

        chunkGenerator = new ChunkGenerator(this);

        Dyn4jAppState dyn4jAppState = getState(Dyn4jAppState.class);

        GridSettings gridSettings = new GridSettings();
        gridSettings.setCellSize(CellSize.Size_8);
        gridSettings.setViewDistance(2);

        collisionGrid = new SceneCollisionGrid(this, dyn4jAppState.getPhysicsSpace(), gridSettings);

        worldNode.attachChild(player.getModel());

        debugContainer = new Container();
        lightAdditionsLabel = debugContainer.addChild(new Label(""));
        lightRemovalsLabel = debugContainer.addChild(new Label(""));



        ((SimpleApplication)app).getGuiNode().attachChild(debugContainer);
    }

    @Override protected void cleanup(Application app) {
        collisionGrid.destroy();
    }

    @Override protected void onEnable() {

        ((SimpleApplication)getApplication()).getRootNode().attachChild(worldNode);

        Dyn4jAppState dyn4jAppState = getState(Dyn4jAppState.class);
        dyn4jAppState.getPhysicsSpace().add(player.getRigidBodyControl2D());
        collisionGrid.addEntity(player);

        // player inventory
        PlayerInventoryGuiState inventoryGuiState = new PlayerInventoryGuiState(player);
        getStateManager().attach(inventoryGuiState);
    }

    @Override protected void onDisable() {
        worldNode.removeFromParent();
    }

    @Override
    public void update(float tpf) {

        sceneGrid.setLocation(player.getLocation());
        sceneGrid.update(tpf);

        collisionGrid.update(tpf);

        int lightRemovalsTotal = 0;
        int lightAdditionsTotal = 0;

        for (Chunk chunk : sceneGrid.getLoadedChunks()) {

            // The lighting system flood fills light. Sometimes that spreads to neighboring chunks.
            // It will however, only spread to its neighboring chunk, so only begin the flood-fill IF
            // both neighbors are loaded.

            // If neighboring chunks on either side are loaded, apply lighting.
            // We're doing it frame-by-frame. One lighting calculation per frame.
            // We could limit the calculations per-frame to speed it up vs "jittering" due to the time it takes.
            // But for now, one calc per frame lets us "see" that it's working properly.
            if (sceneGrid.getChunk(chunk.getGridPosition() -1) != null && sceneGrid.getChunk(chunk.getGridPosition() +1) != null) {

                chunkGenerator.applyLightRemovals(chunk);

                int lightRemovals = chunk.getLightRemovalBfsQueue().size();
                lightRemovalsTotal += lightRemovals;

                if (lightRemovals == 0) {
                    chunkGenerator.applyLightAdditions(chunk);
                }

                int lightAdditions = chunk.getLightBfsQueue().size();
                lightAdditionsTotal += lightAdditions;
            }
        }

        // debug info
        debugContainer.setLocalTranslation(
                getApplication().getCamera().getWidth() - 10 - debugContainer.getPreferredSize().x,
                getApplication().getCamera().getHeight() - 10,
                0
        );

        lightAdditionsLabel.setText("Light Additions: " + lightAdditionsTotal);
        lightRemovalsLabel.setText("Light Removals: " + lightRemovalsTotal);

    }

    private void regenMesh(ChunkCell cell) {


        if (cell.getGeometry() != null) {
            Mesh mesh = chunkGenerator.generateSpriteSheetMesh(cell);

            if (mesh.getVertexCount() > 0) {
                cell.getGeometry().setMesh(mesh);
            }
            else {
                cell.getGeometry().removeFromParent();
                cell.setGeometry(null);
            }
        }
        else {
            Geometry geometry = chunkGenerator.generateGeometry(cell);
            cell.setGeometry(geometry);

            if (geometry != null) {
                cell.getChunk().getChunkNode().attachChild(geometry);
            }
        }

    }

    private void regenNeighborMeshesIfRequired(ChunkCell cell, int localX, int localY) {

        // update the cell below, same chunk.
        if (localY == 0) {

            if (cell.getCellIndex() > 0) {
                ChunkCell lowerCell = cell.getChunk().getCell(cell.getCellIndex() - 1);
                regenMesh(lowerCell);
            }

        }

        // update the cell able, same chunk.
        else if (localY == WorldConstants.CELL_SIZE - 1) {

            if (cell.getCellIndex() < WorldConstants.CELL_COUNT_Y - 1) {
                ChunkCell upperCell = cell.getChunk().getCell(cell.getCellIndex() + 1);
                regenMesh(upperCell);
            }
        }

        // update the chunk to the left, same cell index.
        if (localX == 0) {

            Chunk chunkLeft = sceneGrid.getChunk(cell.getChunk().getGridPosition() - 1);

            if (chunkLeft != null) {

                ChunkCell leftCell = chunkLeft.getCell(cell.getCellIndex());
                regenMesh(leftCell);

                // update the chunk to the left, lower cell.
                if (localY == 0) {

                    if (cell.getCellIndex() > 0) {
                        ChunkCell leftLowerCell = chunkLeft.getCell(cell.getCellIndex() - 1);
                        regenMesh(leftLowerCell);
                    }

                }
                // update the chunk to the left, upper cell.
                else if (localY == WorldConstants.CELL_SIZE - 1) {

                    if (cell.getCellIndex() < WorldConstants.CELL_COUNT_Y - 1) {
                        ChunkCell leftUpperCell = chunkLeft.getCell(cell.getCellIndex() + 1);
                        regenMesh(leftUpperCell);
                    }

                }
            }

        }

        // update the chunk to the right, same cell index.
        else if (localX == WorldConstants.CELL_SIZE - 1) {

            Chunk chunkRight = sceneGrid.getChunk(cell.getChunk().getGridPosition() + 1);

            if (chunkRight != null) {
                ChunkCell rightCell = chunkRight.getCell(cell.getCellIndex());
                regenMesh(rightCell);

                // update the chunk to the right, lower cell.
                if (localY == 0) {

                    if (cell.getCellIndex() > 0) {
                        ChunkCell rightLowerCell = chunkRight.getCell(cell.getCellIndex() - 1);
                        regenMesh(rightLowerCell);
                    }

                }
                // update the chunk to the right, upper cell.
                else if (localY == WorldConstants.CELL_SIZE - 1) {

                    if (cell.getCellIndex() < WorldConstants.CELL_COUNT_Y - 1) {
                        ChunkCell rightUpperCell = chunkRight.getCell(cell.getCellIndex() + 1);
                        regenMesh(rightUpperCell);
                    }

                }

            }

        }

    }

    @Override
    public void addBlock(int type, Vector2f... worldLocations) {

        for (Vector2f loc : worldLocations) {

            int xGrid = (int)loc.x >> WorldConstants.GRID_BITSHIFT;
            int localX = (int)loc.x - (xGrid << WorldConstants.GRID_BITSHIFT);

            int cellY = (int)loc.y >> WorldConstants.GRID_BITSHIFT;
            int localY = (int)loc.y - (cellY << WorldConstants.GRID_BITSHIFT);

            Chunk chunk = sceneGrid.getChunk(xGrid);
            ChunkCell cell = chunk.getCell(cellY);

            chunk.addBlock(localX, (int)loc.y, type);

            regenMesh(cell);
            // depending on the block we've changed, we might need to also regen the neighbor cell(s).
            regenNeighborMeshesIfRequired(cell, localX, localY);

            // collision grid
            refreshLoadedCollisionCell(loc);
        }

    }

    private void refreshLoadedCollisionCell(Vector2f worldLocation) {

        // int xGridCollisionCell = (int)worldLocation.x >> collisionGrid.getGridSettings().getCellSize().getBitshift();
        // int yGridCollisionCell = (int)worldLocation.y >> collisionGrid.getGridSettings().getCellSize().getBitshift();

        // GridPos2i collisionCellGridPos = new GridPos2i(xGridCollisionCell, yGridCollisionCell, collisionGrid.getGridSettings().getCellSize().getBitshift());

        GridPos2i collisionCellGridPos = GridPos2i.fromWorldLocation(worldLocation, collisionGrid.getGridSettings().getCellSize().getBitshift());
        collisionGrid.refreshLoadedCell(collisionCellGridPos);
    }

    @Override
    public void deleteBlock(Vector2f... worldLocations) {

        for (Vector2f loc : worldLocations) {

            int gridX = (int)loc.x >> WorldConstants.GRID_BITSHIFT;
            int localX = (int)loc.x - (gridX << WorldConstants.GRID_BITSHIFT);

            int cellY = (int)loc.y >> WorldConstants.GRID_BITSHIFT;
            int localY = (int)loc.y - (cellY << WorldConstants.GRID_BITSHIFT);

            Chunk chunk = sceneGrid.getChunk(gridX);
            ChunkCell cell = chunk.getCell(cellY);

            chunk.removeBlock(localX, (int)loc.y);

            regenMesh(cell);
            // depending on the block we've changed, we might need to also regen the neighbor cell(s).
            regenNeighborMeshesIfRequired(cell, localX, localY);

            // collision grid
            refreshLoadedCollisionCell(loc);
        }

    }

    private Material createWangMaterial(AssetManager assetManager) {

        Material material = new Material(assetManager, "Materials/blocks.j3md");
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        List<Image> images = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            images.add(assetManager.loadTexture("Textures/Rotated/Dirt/" + i + ".gif").getImage());
        }

        TextureArray textureArray = new TextureArray(images);
        textureArray.setMagFilter(Texture.MagFilter.Nearest);
        textureArray.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        material.setTexture("DirtTextures", textureArray);

        return material;

    }

    private Material createMaterial(AssetManager assetManager) {

        Material material = new Material(assetManager, "Materials/Blocks.j3md");
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        List<Image> images = new ArrayList<>();

        for (int i = 0; i < 48; i++) {
            images.add(assetManager.loadTexture("Textures/Grass/" + i + ".png").getImage());
        }

        TextureArray textureArray = new TextureArray(images);
        textureArray.setMagFilter(Texture.MagFilter.Nearest);
        textureArray.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        material.setTexture("DirtTextures", textureArray);

        return material;
    }

    private Material createSpriteMaterial(AssetManager assetManager) {

        Material material = new Material(assetManager, "Materials/BlocksSpriteSheet.j3md");
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Texture dirtTexture = assetManager.loadTexture("Textures/Grass/dirt.png");
        TextureKey textureKey = new TextureKey("Textures/Grass/dirt.png");
        // textureKey.setGenerateMips(false);
        textureKey.setAnisotropy(16);
        Texture dirtTexture = assetManager.loadTexture(textureKey);
        dirtTexture.setMagFilter(Texture.MagFilter.Nearest);
        dirtTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        material.setTexture("DirtTexture", dirtTexture);

        return material;
    }


    /**
     * Gets the block at the given world coordinates.
     * returns NULL if the chunk is not loaded.
     * @param x the world x coordinate.
     * @param y the world y coordinate.
     * @return the block at the given coordinate.
     */
    @Override
    public Block getBlock(int x, int y) {

        if (y < 0) {
            return null;
        }

        int xGrid = x >> WorldConstants.GRID_BITSHIFT;
        // int yGrid = y >> WorldConstants.GRID_BITSHIFT;

        int xLocal = x - (xGrid << WorldConstants.GRID_BITSHIFT);
        // int yLocal = y - (yGrid << WorldConstants.GRID_BITSHIFT);

        Chunk chunk = sceneGrid.getChunk(xGrid);

        if (chunk != null) {
            return chunk.getBlockLocal(xLocal, y);
        }

        return null;
    }

    /**
     * Gets the chunk at the given grid location.
     * @param xGrid the grid location of the chunk.
     * @return the chunk at the given grid location.
     */
    @Override
    public Chunk getChunk(int xGrid) {
        return sceneGrid.getChunk(xGrid);
    }




}
