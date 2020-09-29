package com.jayfella.pixels.tile;

import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.world.Chunk;
import com.jayfella.pixels.world.ChunkCell;

import java.util.Objects;

public class Block {

    private final ChunkCell chunkCell;
    private int type;
    private byte configuration;
    private final int x, y;

    private int lightValue;

    public Block(ChunkCell chunkCell, int type, byte configuration, int x, int y) {
        this.chunkCell = chunkCell;
        this.type = type;
        this.configuration = configuration;
        this.x = x;
        this.y = y;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte getConfiguration() {
        return configuration;
    }

    public void setConfiguration(byte configuration) {
        this.configuration = configuration;
    }

    public ChunkCell getChunkCell() {
        return chunkCell;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWorldY() {
        int cellWorld = chunkCell.getCellIndex() << WorldConstants.GRID_BITSHIFT;
        return cellWorld + y;
    }

    public int getSunlight() {
        return (lightValue >> 4) & 0xF;
    }

    public void setSunlight(int val) {
        lightValue = (lightValue & 0xF) | (val << 4);
    }

    public int getTorchlight() {
        return lightValue & 0xF;
    }

    public void setTorchlight(int val) {
        lightValue = (lightValue & 0xF0) | val;
    }

    public int getRedLight() {

        return (lightValue >> 8) & 0xF;

    }

    public void setRedLight(int val) {

        lightValue = (lightValue & 0xF0FF) | (val << 8);
    }

    public int getGreenLight(int x, int y, int z) {

        return (lightValue >> 4) & 0xF;

    }

    public void setGreenLight(int x, int y, int z, int val) {

        lightValue = (lightValue & 0xFF0F) | (val << 4);
    }

    public int getBlueLight(int x, int y, int z) {

        return lightValue & 0xF;

    }

    public void setBlueLight(int x, int y, int z, int val) {

        lightValue = (lightValue & 0xFFF0) | (val);
    }

    public Block getNeighbor(BlockFace blockFace) {

        switch (blockFace) {

            case North: {
                // if the block is in this cell, return the block.
                if (y < WorldConstants.CELL_SIZE - 1) {
                    return chunkCell.getBlockLocal(x, y + 1);
                }
                else {
                    // if this is the highest cell in the chunk, there is no block.
                    if (chunkCell.getCellIndex() == WorldConstants.CELL_COUNT_Y - 1) {
                        return null;
                    }
                    else {
                        // get the cell above and return the block at y = 0
                        ChunkCell cell = chunkCell.getChunk().getCell(chunkCell.getCellIndex() + 1);
                        return cell.getBlockLocal(x, 0);
                    }
                }

                // break;
            }

            case East: {

                // if the block is in this cell, return the block.
                if (x < WorldConstants.CELL_SIZE - 1) {
                    return chunkCell.getBlockLocal(x + 1, y);
                }
                else {
                    // check if the next chunk to the right is loaded.
                    Chunk chunk = chunkCell.getChunk().getWorld().getChunk(chunkCell.getChunk().getGridPosition() + 1);

                    if (chunk != null) {
                        return chunk.getCell(chunkCell.getCellIndex()).getBlockLocal(0, y);
                    }
                    else {
                        // the chunk is not loaded.
                        return null;
                    }

                }

                // break;
            }

            case South: {

                // if the block is in this cell, return the block.
                if ( y > 0) {
                    return chunkCell.getBlockLocal(x, y - 1);
                }
                else {
                    // if this is the lowest cell in the chunk, there is no block.
                    if (chunkCell.getCellIndex() == 0) {
                        return null;
                    }
                    else {
                        // get the cell below and return the block at y = WorldConstants.CELL_SIZE - 1
                        ChunkCell cell = chunkCell.getChunk().getCell(chunkCell.getCellIndex() - 1);
                        return cell.getBlockLocal(x, WorldConstants.CELL_SIZE - 1);
                    }
                }

                // break;
            }

            case West: {

                // if the block is in this cell, return the block.
                if (x > 0) {
                    return chunkCell.getBlockLocal(x - 1, y);
                }
                else {
                    // check if the next chunk to the left is loaded.
                    Chunk chunk = chunkCell.getChunk().getWorld().getChunk(chunkCell.getChunk().getGridPosition() - 1);

                    if (chunk != null) {
                        return chunk.getCell(chunkCell.getCellIndex()).getBlockLocal(WorldConstants.CELL_SIZE - 1, y);
                    }
                    else {
                        // the chunk is not loaded.
                        return null;
                    }

                }

                // break;
            }

            default: throw new RuntimeException("Unhandled BlockFace: " + blockFace);
        }


    }



    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        return type == block.type &&
                chunkCell.getCellIndex() == block.chunkCell.getCellIndex() &&
                chunkCell.getChunk().getGridPosition() == block.chunkCell.getChunk().getGridPosition() &&
                configuration == block.configuration &&
                x == block.x &&
                y == block.y;
    }

    // DO NOT hash the light value!
    // If the hashCode() value of an object has changed since it was added to the HashSet, it seems to render the object unremovable.
    // https://stackoverflow.com/a/256247

    @Override
    public int hashCode() {
        return Objects.hash(type, configuration, chunkCell.getCellIndex(), chunkCell.getChunk().getGridPosition(), x, y);
    }

}
