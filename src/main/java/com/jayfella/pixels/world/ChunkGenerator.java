package com.jayfella.pixels.world;

import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.mesh.JmeMesh;
import com.jayfella.pixels.tile.BlobTile;
import com.jayfella.pixels.tile.Block;
import com.jayfella.pixels.tile.BlockFace;
import com.jayfella.pixels.tile.RotatedBlobTile;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ChunkGenerator  {

    private final World world;
    private final NoiseEvaluator noiseEvaluator;

    public ChunkGenerator(World world) {
        this.world = world;
        this.noiseEvaluator = world.getWorldNoiseEvaluator();
    }

    // finds the highest block in each column of the chunk.
    public void initHeightMap(Chunk chunk) {

        for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {
            for (int y = WorldConstants.MAX_HEIGHT - 1; y >= 0; y--) {

                // keep going down until we hit a block, then set the lightmap to that value.

                int cellY = y >> WorldConstants.GRID_BITSHIFT;
                ChunkCell chunkCell = chunk.getCell(cellY);

                int cellLocalY = y - (cellY << WorldConstants.GRID_BITSHIFT);

                Block block = chunkCell.getBlockLocal(x, cellLocalY);

                // if the block is not air and it's higher than the current value
                // set the new highest value to this block.
                if (block.getType() > 0 && y > chunk.getHeightMapValue(x)) {
                    chunk.setHeightMapValue(x, y);

                    // move to the next column.
                    break;
                }

                // if this height is greater or equal to the heightmap value
                // set this light value to MAX.
                // if (y >= chunk.getHeightMapValue(x)) {
                    // chunk.setSunlight(x, y, 15);
                // }

            }
        }

    }

    // sets all air and the highest block to sunlight 15
    public void initLightMap(Chunk chunk) {

        for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {

            int heightmapVal = chunk.getHeightMapValue(x);

            for (int y = WorldConstants.MAX_HEIGHT - 1; y >= heightmapVal; y--) {
                chunk.setSunlight(x, y, 15);
            }
        }

    }

    public boolean applyLightAdditions(Chunk chunk) {

        Iterator<Block> iterator = chunk.getLightBfsQueue().iterator();

        if (iterator.hasNext()) {

            Block block = iterator.next();
            iterator.remove();

            doLight(block, block.getSunlight());

            return true;
        }

        return false;
    }

    private void doLight(Block block, int light) {

        if (light > 0) {

            // this just sets the value of the property and sets it on the mesh.
            block.getChunkCell().setSunlight(block.getX(), block.getY(), light);

            light--;

            // LightNode neighbor;
            Block neighbor;

            for (BlockFace face : BlockFace.values()) {

                neighbor = block.getNeighbor(face);

                if (neighbor != null && neighbor.getSunlight() < light) {

                    // this will add it to the BFS queue.
                    neighbor.getChunkCell()
                            .getChunk()
                            .setSunlight(neighbor.getX(), neighbor.getWorldY(),
                                    neighbor.getType() > 0 ? light - 1 : light
                            );

                }

            }

        }

    }

    public boolean applyLightRemovals(Chunk chunk) {

        Iterator<Block> iterator = chunk.getLightRemovalBfsQueue().iterator();

        if (iterator.hasNext()) {

            Block block = iterator.next();
            iterator.remove();

            doLightRemoval(block, block.getSunlight());

            return true;
        }

        return false;

    }

    public void doLightRemoval(Block block, int light) {

        // this just sets the value of the property and sets it on the mesh.
        block.getChunkCell().setSunlight(block.getX(), block.getY(), light);

        // Block neighbor;
        Block neighbor;

        for (BlockFace face : BlockFace.values()) {

            neighbor = block.getNeighbor(face);

            if (neighbor != null) {

                if (neighbor.getSunlight() > 0 && neighbor.getSunlight() < light) {

                    // neighbor.getChunk().getLightManager().lightRemovalBfsQue.add(neighbor);

                    neighbor.setSunlight(0);

                    neighbor.getChunkCell()
                            .getChunk()
                            .getLightRemovalBfsQueue()
                            .add(neighbor);


                }
                else if (neighbor.getSunlight() >= light) {

                    // neighbor.getChunk().getLightManager().lightBfsQueue.add(neighbor);

                    // this will add it to the BFS queue.
                    neighbor.getChunkCell()
                            .getChunk()
                            .setSunlight(neighbor.getX(), neighbor.getWorldY(), neighbor.getSunlight());
                }
            }
        }

    }

    public Geometry generateGeometry(ChunkCell chunkCell) {

        Mesh mesh = generateSpriteSheetMesh(chunkCell);

        if (mesh.getVertexCount() > 0) {

            String nameFormat = "Chunk %d | Cell: %d";

            Geometry geometry = new Geometry(String.format(nameFormat, chunkCell.getChunk().getGridPosition(), chunkCell.getCellIndex()), mesh);

            geometry.setMaterial(world.getWorldMaterial());
            geometry.setLocalTranslation(0, chunkCell.getCellIndex() << WorldConstants.GRID_BITSHIFT, 0);

            return geometry;
        }

        return null;
    }

    public Mesh generateMeshWang(ChunkCell chunkCell) {

        List<Vector3f> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();

        // add the lookup int to "voxel data".
        // x = which texture variant to show (top-left, etc.. one of the 48 variants.).
        // y = which texture to show (grass, dirt, etc)
        // z = lightValue (for now)
        // right now we only have one type, so it's just zero
        List<Vector3f> blockData = new ArrayList<>();

        int indexStart = 0;

        for (int y = 0; y < WorldConstants.CELL_SIZE; y++) {
            for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {

                Block block = chunkCell.getBlockLocal(x, y);

                if (block.getType() > 0) {



                    Collections.addAll(vertices,
                            new Vector3f(x + 0, y + 0, 0),
                            new Vector3f(x + 1, y + 0, 0),
                            new Vector3f(x + 1, y + 1, 0),
                            new Vector3f(x + 0, y + 1, 0)
                    );

                    Collections.addAll(indices,
                            indexStart + 0,
                            indexStart + 1,
                            indexStart + 2,
                            indexStart + 0,
                            indexStart + 2,
                            indexStart + 3);

                    int rotations = RotatedBlobTile.getTextureRotationCount(block.getConfiguration());
                    Vector2f[] coords = getTextureCoords(rotations);
                    Collections.addAll(texCoords, coords);

                    if (rotations > 0) {
                        String a = "b";
                    }

                    float lightVal = block.getSunlight() / 15f;
                    int textureId = RotatedBlobTile.getTextureIndex(block.getConfiguration());

                    Collections.addAll(blockData,
                            new Vector3f(textureId, 0, lightVal),
                            new Vector3f(textureId, 0, lightVal),
                            new Vector3f(textureId, 0, lightVal),
                            new Vector3f(textureId, 0, lightVal));

                    indexStart += 4;
                }
            }
        }

        JmeMesh mesh = new JmeMesh();
        mesh.set(VertexBuffer.Type.Position, vertices);
        mesh.set(VertexBuffer.Type.Index, indices);
        mesh.set(VertexBuffer.Type.TexCoord, texCoords);
        mesh.set(VertexBuffer.Type.TexCoord2, blockData);

        return mesh;
    }

    private Vector2f[] getTextureCoords(int rotations) {

        switch (rotations) {

            case 0: {
                return new Vector2f[] {
                        new Vector2f(0, 0),
                        new Vector2f(1, 0),
                        new Vector2f(1, 1),
                        new Vector2f(0,1)
                };
            }

            case 1: {
                return new Vector2f[] {
                        new Vector2f(1, 0),
                        new Vector2f(1, 1),
                        new Vector2f(0,1),
                        new Vector2f(0, 0),
                };
            }

            case 2: {
                return new Vector2f[] {
                        new Vector2f(1, 1),
                        new Vector2f(0,1),
                        new Vector2f(0, 0),
                        new Vector2f(1, 0),
                };
            }

            case 3: {
                return new Vector2f[] {
                        new Vector2f(0,1),
                        new Vector2f(0, 0),
                        new Vector2f(1, 0),
                        new Vector2f(1, 1),

                };
            }

            default: throw new RuntimeException("Invalid rotator: " + rotations);
        }

    }

    public Mesh generateMesh(ChunkCell chunkCell) {

        List<Vector3f> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();

        // add the lookup int to "voxel data".
        // x = which texture variant to show (top-left, etc.. one of the 48 variants.).
        // y = which texture to show (grass, dirt, etc)
        // z = lightValue (for now)
        // right now we only have one type, so it's just zero
        List<Vector3f> blockData = new ArrayList<>();

        int indexStart = 0;

            for (int y = 0; y < WorldConstants.CELL_SIZE; y++) {
                for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {

                Block block = chunkCell.getBlockLocal(x, y);

                if (block.getType() > 0) {

                    Collections.addAll(vertices,
                            new Vector3f(x + 0, y + 0, 0),
                            new Vector3f(x + 1, y + 0, 0),
                            new Vector3f(x + 0, y + 1, 0),
                            new Vector3f(x + 1, y + 1, 0));

                    Collections.addAll(indices,
                            indexStart + 0,
                            indexStart + 1,
                            indexStart + 2,
                            indexStart + 2,
                            indexStart + 1,
                            indexStart + 3);

                    Collections.addAll(texCoords,
                            new Vector2f(0, 0),
                            new Vector2f(1, 0),
                            new Vector2f(0, 1),
                            new Vector2f(1,1));


                    int lookupId = BlobTile.getTextureIndex(block.getConfiguration());

                    float lightVal = block.getSunlight() / 15f;

                    Collections.addAll(blockData,
                            new Vector3f(lookupId, 0, lightVal),
                            new Vector3f(lookupId, 0, lightVal),
                            new Vector3f(lookupId, 0, lightVal),
                            new Vector3f(lookupId, 0, lightVal));

                    indexStart += 4;
                }
            }
        }

        JmeMesh mesh = new JmeMesh();
        mesh.set(VertexBuffer.Type.Position, vertices);
        mesh.set(VertexBuffer.Type.Index, indices);
        mesh.set(VertexBuffer.Type.TexCoord, texCoords);
        mesh.set(VertexBuffer.Type.TexCoord2, blockData);

        return mesh;
    }

    public Mesh generateSpriteSheetMesh(ChunkCell chunkCell) {

        List<Vector3f> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();

        // add the lookup int to "voxel data".
        // x = which texture variant to show (top-left, etc.. one of the 48 variants.).
        // y = which texture to show (grass, dirt, etc)
        // z = lightValue (for now)
        // right now we only have one type, so it's just zero
        List<Vector3f> blockData = new ArrayList<>();

        int indexStart = 0;

        for (int y = 0; y < WorldConstants.CELL_SIZE; y++) {
            for (int x = 0; x < WorldConstants.CELL_SIZE; x++) {

                Block block = chunkCell.getBlockLocal(x, y);

                if (block.getType() > 0) {

                    Collections.addAll(vertices,
                            new Vector3f(x + 0, y + 0, 0),
                            new Vector3f(x + 1, y + 0, 0),
                            new Vector3f(x + 1, y + 1, 0),
                            new Vector3f(x + 0, y + 1, 0));

                    Collections.addAll(indices,
                            indexStart + 0,
                            indexStart + 1,
                            indexStart + 2,
                            indexStart + 0,
                            indexStart + 2,
                            indexStart + 3);


                    // calculate the row and column from the texture index.
                    int textureId = BlobTile.getTextureIndex(block.getConfiguration());
                    int lookupId = BlobTile.toArtistsLayout(textureId);

                    // row    = (int)(index / width)
                    // column = index % width
                    int row = lookupId / 7;
                    int col = lookupId % 7;

                    float size = 1.0f / 7.0f;

                    float bl_x = size * col;
                    float bl_y = size * row;

                    // bl, br, tr, tl
                    Collections.addAll(texCoords,
                            new Vector2f(bl_x, bl_y),
                            new Vector2f(bl_x + size, bl_y),
                            new Vector2f(bl_x + size, bl_y + size),
                            new Vector2f(bl_x, bl_y + size)
                    );

//                    Collections.addAll(texCoords,
//                            new Vector2f(0, 0),
//                            new Vector2f(1, 0),
//                            new Vector2f(0, 1),
//                            new Vector2f(1,1));

                    float lightVal = block.getSunlight() / 15f;

                    Collections.addAll(blockData,
                            new Vector3f(lookupId, 0, lightVal),
                            new Vector3f(lookupId, 0, lightVal),
                            new Vector3f(lookupId, 0, lightVal),
                            new Vector3f(lookupId, 0, lightVal));

                    indexStart += 4;
                }
            }
        }

        JmeMesh mesh = new JmeMesh();
        mesh.set(VertexBuffer.Type.Position, vertices);
        mesh.set(VertexBuffer.Type.Index, indices);
        mesh.set(VertexBuffer.Type.TexCoord, texCoords);
        mesh.set(VertexBuffer.Type.TexCoord2, blockData);

        return mesh;
    }

}
