package com.jayfella.pixels.world;

import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.tile.BlobTile;
import com.jayfella.pixels.tile.Block;
import com.jayfella.pixels.tile.BlockFace;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import java.util.HashSet;
import java.util.Set;

public class Chunk {

    private final int gridPos;
    private final World world;
    private final ChunkGenerator chunkGenerator;

    private final ChunkCell[] cells = new ChunkCell[WorldConstants.CELL_COUNT_Y];

    // we can probably use a byte array here since our max height is 256;
    private final int[] heightmap;

    // private final Deque<LightNode> lightBfsQueue = new ArrayDeque<>();
    private final Set<Block> lightBfsQueue = new HashSet<>(); //Sets.newConcurrentHashSet();
    private final Set<Block> lightRemovalBfsQueue = new HashSet<>();

    private final Node chunkNode;

    public Chunk(int gridPosition, World world) {
        gridPos = gridPosition;
        this.world = world;

        chunkGenerator = world.getChunkGenerator();

        heightmap = new int[WorldConstants.CELL_SIZE];


        chunkNode = new Node("Chunk: " + gridPos);
        chunkNode.setLocalTranslation(gridPosition << WorldConstants.GRID_BITSHIFT, 0, 0);
        generate();
    }

    public int getGridPosition() {
        return gridPos;
    }

    int getHeightMapValue(int x) {
        return heightmap[x];
    }

    void setHeightMapValue(int x, int value) {
        heightmap[x] = value;
    }

    public void generate() {

        // generate each chunk cell.
        for (int y = 0; y < WorldConstants.CELL_COUNT_Y; y++) {
            cells[y] = new ChunkCell(this, y);
        }

        // generate the heightmap from the cells.
        // iterate down each column, setting the light value to FULL until we hit the heightmap value.
        chunkGenerator.initHeightMap(this);

        for (int y = 0; y < WorldConstants.CELL_COUNT_Y; y++) {

            ChunkCell chunkCell = cells[y];

            Geometry geometry = chunkGenerator.generateGeometry(chunkCell);

            if (geometry != null) {
                chunkCell.setGeometry(geometry);
                chunkNode.attachChild(geometry);
            }
        }
    }

    public World getWorld() {
        return world;
    }

    public ChunkCell getCell(int y) {
        return cells[y];
    }

    private int worldToLocalGridY(int y) {
        return y >> WorldConstants.GRID_BITSHIFT;
    }

    private int worldToLocalY(int y) {
        int cellIndex = worldToLocalGridY(y);
        return y - (cellIndex << WorldConstants.GRID_BITSHIFT);
    }

    public Node getChunkNode() {
        return chunkNode;
    }

    /**
     * Gets the block from the given coordinates.
     * @param x a value between 0 and WorldConstants.CELL_SIZE - 1
     * @param y a value between 0 and WorldConstants.MAX_HEIGHT - 1
     * @return the block at the given coordinates.
     */
    public Block getBlockLocal(int x, int y) {
        int cellIndex = y >> WorldConstants.GRID_BITSHIFT;
        int localY = y - (cellIndex << WorldConstants.GRID_BITSHIFT);
        return cells[cellIndex].getBlockLocal(x, localY);
    }

    /**
     * Gets the block from the given coordinates.
     * @param x         a value between 0 and WorldConstants.CELL_SIZE - 1
     * @param y         a value between 0 and WorldConstants.CELL_SIZE - 1
     * @param cellIndex a value between 0 and WorldConstants.CELL_COUNT_Y - 1
     * @returnthe block at the given coordinates.
     */
    public Block getBlock(int x, int y, int cellIndex) {
        return cells[cellIndex].getBlockLocal(x, y);
    }

    /**
     * Sets the sunlight value of the block matching the given coordinates.
     * @param x a value between 0 and WorldConstants.CELL_SIZE - 1
     * @param y a value between 0 and WorldConstants.MAX_HEIGHT - 1
     */
    public void setSunlight(int x, int y, int val) {
        int cellIndex = y >> WorldConstants.GRID_BITSHIFT;
        int localY = y - (cellIndex << WorldConstants.GRID_BITSHIFT);

        Block block = cells[cellIndex].getBlockLocal(x, localY);
        block.setSunlight(val);

        lightBfsQueue.add(block);
    }

    public void setTorchlight(int x, int y, int val) {
        int cellIndex = y >> WorldConstants.GRID_BITSHIFT;
        int localY = y - (cellIndex << WorldConstants.GRID_BITSHIFT);

        Block block = cells[cellIndex].getBlockLocal(x, localY);
        block.setTorchlight(val);

        lightBfsQueue.add(block);
    }

    public Set<Block> getLightBfsQueue() {
        return lightBfsQueue;
    }

    public Set<Block> getLightRemovalBfsQueue() {
        return lightRemovalBfsQueue;
    }

    private byte calculateConfiguration(Block block) {

        Block nw = block.getNeighbor(BlockFace.North).getNeighbor(BlockFace.West);
        Block n = block.getNeighbor(BlockFace.North);
        Block ne = block.getNeighbor(BlockFace.North).getNeighbor(BlockFace.East);

        Block e = block.getNeighbor(BlockFace.East);
        Block w = block.getNeighbor(BlockFace.West);

        Block sw = block.getNeighbor(BlockFace.South).getNeighbor(BlockFace.West);
        Block s = block.getNeighbor(BlockFace.South);
        Block se = block.getNeighbor(BlockFace.South).getNeighbor(BlockFace.East);

        // clockwise beginning from north

        return BlobTile.getConfiguration(
                n.getType() > 0,
                e.getType() > 0,
                s.getType() > 0,
                w.getType() > 0,

                nw.getType() > 0,
                ne.getType() > 0,

                sw.getType() > 0,
                se.getType() > 0);

    }

    private void updateNeighborConfigurations(Block block) {

        Block[] neighbors = {
                block.getNeighbor(BlockFace.North).getNeighbor(BlockFace.West),
                block.getNeighbor(BlockFace.North),
                block.getNeighbor(BlockFace.North).getNeighbor(BlockFace.East),

                block.getNeighbor(BlockFace.West),
                block.getNeighbor(BlockFace.East),

                block.getNeighbor(BlockFace.South).getNeighbor(BlockFace.West),
                block.getNeighbor(BlockFace.South),
                block.getNeighbor(BlockFace.South).getNeighbor(BlockFace.East),
        };

        for (Block b : neighbors) {
            byte config = calculateConfiguration(b);
            b.setConfiguration(config);
        }

    }

    /**
     * Removes the block matching the given coordinates.
     * @param x a value between 0 and WorldConstants.CELL_SIZE - 1
     * @param y a value between 0 and WorldConstants.MAX_HEIGHT - 1
     */
    public void removeBlock(int x, int y) {

        int cellIndex = y >> WorldConstants.GRID_BITSHIFT;
        int localY = y - (cellIndex << WorldConstants.GRID_BITSHIFT);

        Block block = cells[cellIndex].getBlockLocal(x, localY);

        // don't bother setting a block to "air" if it's already "air".
        if (block.getType() == 0) {
            return;
        }

        // System.out.println("Block[" + block.getX() + "][" + block.getY() + "] | Chunk[" + block.getChunkCell().getChunk().getGridPosition() + "][" + block.getChunkCell().getCellIndex() + "]");

        block.setType(0);

        // we need to tell each block in the 8 directions that their configuration may have changed.
        updateNeighborConfigurations(block);

        // if this was the highest block, it's been removed, so find the next highest block.
        if (heightmap[x] == y) {
            for (int newHighestBlock = y; newHighestBlock >= 0; newHighestBlock--) {

                Block seekBlock = getBlockLocal(x, newHighestBlock);

                setSunlight(x, newHighestBlock, 15);

                if (seekBlock.getType() > 0) {
                    heightmap[x] = newHighestBlock;
                    break;
                }

            }
        }
        else {
            lightRemovalBfsQueue.add(block);
        }

    }

    public void addBlock(int x, int y, int type) {

        if (type == 0) {
            removeBlock(x, y);
        }

        int cellIndex = y >> WorldConstants.GRID_BITSHIFT;
        int localY = y - (cellIndex << WorldConstants.GRID_BITSHIFT);

        Block block = cells[cellIndex].getBlockLocal(x, localY);

        // don't bother setting a block to the same thing.
        if (block.getType() == type) {
            return;
        }

        // System.out.println("Block[" + block.getX() + "][" + block.getY() + "] | Chunk[" + block.getChunkCell().getChunk().getGridPosition() + "][" + block.getChunkCell().getCellIndex() + "]");

        block.setType(type);

        // we need to tell each block in the 8 directions that their configuration may have changed.
        updateNeighborConfigurations(block);

        // if this is now the highest block, set it in the heightmap
        if (y > heightmap[x]) {
            heightmap[x] = y;
            lightRemovalBfsQueue.add(block);
        }

    }

    public void destroy() {
        chunkNode.removeFromParent();
    }

}
