package com.jayfella.pixels.world;

import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.tile.BlobTile;
import com.jayfella.pixels.tile.Block;
import com.jayfella.pixels.tile.RotatedBlobTile;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;

public class ChunkCell {

    private final Chunk chunk;
    private final int cellIndex;
    private final Block[][] blocks;

    private Geometry geometry;

    public ChunkCell(Chunk chunk, int cellIndex) {
        this.chunk = chunk;
        this.cellIndex = cellIndex;

        blocks = generateCell();
    }


    // Wang rotating tilesets (15 tiles, rotated).
    public Block[][] generateCellWang() {

        Block[][] blocks = new Block[WorldConstants.CELL_SIZE][WorldConstants.CELL_SIZE];

        // re-used in the loops.
        int worldX = chunk.getGridPosition() << WorldConstants.GRID_BITSHIFT;
        int worldY = cellIndex << WorldConstants.GRID_BITSHIFT;

        NoiseEvaluator noiseEvaluator = chunk.getWorld().getWorldNoiseEvaluator();

        for (int y = 0; y < WorldConstants.CELL_SIZE; y++) {
            for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {

                int posX = worldX + x;
                int posY = worldY + y;

                // currently either visible or not.
                float fVal = noiseEvaluator.evaluate(posX, posY);
                int type = fVal > 0.5 ? 1 : 0;

                // determine if there are any blocks on any sides.
                boolean n = noiseEvaluator.evaluate(posX + 0, posY + 1) > 0.5;
                boolean e = noiseEvaluator.evaluate(posX + 1, posY + 0) > 0.5;
                boolean s = noiseEvaluator.evaluate(posX + 0, posY - 1) > 0.5;
                boolean w = noiseEvaluator.evaluate(posX - 1, posY + 0) > 0.5;

                boolean ne = noiseEvaluator.evaluate(posX + 1, posY + 1) > 0.5;
                boolean se = noiseEvaluator.evaluate(posX + 1, posY - 1) > 0.5;
                boolean sw = noiseEvaluator.evaluate(posX - 1, posY - 1) > 0.5;
                boolean nw = noiseEvaluator.evaluate(posX- 1, posY + 1) > 0.5;

                byte configuration = RotatedBlobTile.getConfiguration(n, e, s, w, nw, ne, sw, se);

                blocks[x][y] = new Block(this, type, configuration, x, y);
            }
        }

        return blocks;
    }



    // traditional blob tileset (48 tiles)
    public Block[][] generateCell() {

        Block[][] blocks = new Block[WorldConstants.CELL_SIZE][WorldConstants.CELL_SIZE];

        // re-used in the loops.
        int worldX = chunk.getGridPosition() << WorldConstants.GRID_BITSHIFT;
        int worldY = cellIndex << WorldConstants.GRID_BITSHIFT;

        NoiseEvaluator noiseEvaluator = chunk.getWorld().getWorldNoiseEvaluator();

        for (int y = 0; y < WorldConstants.CELL_SIZE; y++) {
            for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {

                int posX = worldX + x;
                int posY = worldY + y;

                // currently either visible or not.
                float fVal = noiseEvaluator.evaluate(posX, posY);
                int type = fVal > 0.5 ? 1 : 0;

                // determine if there are any blocks on any sides.
                boolean n = noiseEvaluator.evaluate(posX + 0, posY + 1) > 0.5;
                boolean e = noiseEvaluator.evaluate(posX + 1, posY + 0) > 0.5;
                boolean s = noiseEvaluator.evaluate(posX + 0, posY - 1) > 0.5;
                boolean w = noiseEvaluator.evaluate(posX - 1, posY + 0) > 0.5;

                boolean ne = noiseEvaluator.evaluate(posX + 1, posY + 1) > 0.5;
                boolean se = noiseEvaluator.evaluate(posX + 1, posY - 1) > 0.5;
                boolean sw = noiseEvaluator.evaluate(posX - 1, posY - 1) > 0.5;
                boolean nw = noiseEvaluator.evaluate(posX- 1, posY + 1) > 0.5;

                byte configuration = BlobTile.getConfiguration(n, e, s, w, nw, ne, sw, se);

                blocks[x][y] = new Block(this, type, configuration, x, y);
            }
        }

        return blocks;
    }


    public Chunk getChunk() {
        return chunk;
    }

    public int getCellIndex() {
        return cellIndex;
    }

    Block[][] getBlocks() {
        return blocks;
    }

    public Block getBlockLocal(int x, int y) {
        return blocks[x][y];
    }

    public void removeBlock(int x, int y) {
        blocks[x][y].setType(0); // air
    }

    public void setSunlight(int x, int y, int val) {

        blocks[x][y].setSunlight(val);

        // we can't update an AIR block because they aren't rendered.
        if (blocks[x][y].getType() == 0) {
            return;
        }

        if (geometry != null) {

            // iterate over each block, and if a block is NOT air, add 4 to the vertex count;
            int index = 0;
            boolean foundItself = false;

            // iterate over each block until it hits itself so we can determine which vertex to set the light value to.
            for (int j = 0; j < WorldConstants.CELL_SIZE; j++) {
                for (int i = 0; i < WorldConstants.CELL_SIZE; i++) {

                    if (x == i && y == j) {
                        foundItself = true;
                        break;
                    }

                    // if the block is NOT air...
                    if (blocks[i][j].getType() > 0) {
                        index++;
                    }
                }

                if (foundItself) {
                    break;
                }
            }

            index *= 4;

            FloatBuffer floatBuffer = geometry.getMesh().getFloatBuffer(VertexBuffer.Type.TexCoord2);
            Vector3f[] blockData = BufferUtils.getVector3Array(floatBuffer);

            for (int i = 0; i < 4; i++) {

                try {
                    Vector3f data = blockData[index + i];
                    data.z = blocks[x][y].getSunlight() / 15f;
                    BufferUtils.setInBuffer(data, floatBuffer, index + i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            geometry.getMesh().getBuffer(VertexBuffer.Type.TexCoord2).setUpdateNeeded();

        }
    }

    public void setTorchlight(int x, int y, int val) {
        blocks[x][y].setTorchlight(val);
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
