package com.jayfella.pixels.grid;

import com.jayfella.pixels.core.CellSize;
import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jayfella.pixels.physics.PhysicsSpace;
import com.jayfella.pixels.physics.RigidBodyControl2D;
import com.jayfella.pixels.physics.shape.PolygonCollisionShape;
import com.jayfella.pixels.tile.Block;
import com.jayfella.pixels.world.Chunk;
import com.jayfella.pixels.world.ChunkCell;
import com.jayfella.pixels.world.World;
import com.jme3.math.Vector3f;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class OldSceneCollisionGrid {

    private final World world;
    private final PhysicsSpace physicsSpace;
    private final CellSize cellSize;

    // we have our own executor here because collisions don't want to be swamped
    // behind any scene generation tasks.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Map<GridPos2i, RigidBodyControl2D> pooledRigidBodies = new HashMap<>();
    private final Set<GridPos2i> requiredPositions = new HashSet<>();

    private final BiConsumer<CollidableGridCell, Throwable> consumer;

    // a list of things the collision grid is going to keep track of.
    private final Map<Vector3f, GridSettings> entities = new HashMap<>();

    public OldSceneCollisionGrid(World world, PhysicsSpace physicsSpace, CellSize cellSize) {
        this.world = world;
        this.physicsSpace = physicsSpace;
        this.cellSize = cellSize;

        this.consumer = createConsumer();
    }

    private BiConsumer<CollidableGridCell, Throwable> createConsumer() {
        return (gridSection, throwable) -> {

            if (throwable == null) {
                world.getApplication().enqueue(() -> {

                    RigidBodyControl2D rigidBodyControl = gridSection.getRigidBodyControl();

                    if (rigidBodyControl != null) {

                        GridPos2i gridPos = gridSection.getGridPosition();

                        rigidBodyControl.setPhysicsLocation(gridPos.toWorldTranslation());
                        this.pooledRigidBodies.put(gridPos, rigidBodyControl);
                        this.physicsSpace.add(rigidBodyControl);
                    }

                });
            }
            else {
                throwable.printStackTrace();
            }
        };
    }

    public void addRequiredPositions(Collection<GridPos2i> requiredPositions) {
        this.requiredPositions.addAll(requiredPositions);
    }

    public void positionRequested(GridPos2i gridPos) {

        if (!pooledRigidBodies.containsKey(gridPos)) {

            pooledRigidBodies.put(gridPos, null);

            CompletableFuture
                    .supplyAsync(new CollidableGridCell(gridPos, this), executor)
                    .whenComplete(consumer);

        }

    }

    public RigidBodyControl2D positionRequestedAsync(GridPos2i gridPos) {

        // Vector2f worldPos = gridPos.toWorldTranslation2D();

        RigidBodyControl2D rbControl = new RigidBodyControl2D(MassType.INFINITE);

        SceneGrid sceneGrid = world.getSceneGrid();

        // convert this gridPos to a worldPos, then to the scene grids gridpos.
        int xWorld = gridPos.getWorldTranslationX();
        int yWorld = gridPos.getWorldTranslationY();

        for (int x = 0; x < cellSize.getSize() + 1; x++) {
            for (int y = 0; y < cellSize.getSize() + 1; y++) {

                int xPosWorld = xWorld + x;
                int yPosWorld = yWorld + y;

                int xGrid = xPosWorld >> WorldConstants.GRID_BITSHIFT;
                int yGrid = yPosWorld >> WorldConstants.GRID_BITSHIFT;

                int xLocal = xPosWorld - (xGrid << WorldConstants.GRID_BITSHIFT);
                int yLocal = yPosWorld - (yGrid << WorldConstants.GRID_BITSHIFT);

                Chunk chunk = sceneGrid.getChunk(xGrid);
                if (chunk != null) {
                    ChunkCell cell = chunk.getCell(yGrid);

                    Block block = cell.getBlockLocal(xLocal, yLocal);

                    if (block.getType() > 0) {

                        Vector2[] verts = {
                                new Vector2(x + 0, y + 0),
                                new Vector2(x + 1, y + 0),
                                new Vector2(x + 1, y + 1),
                                new Vector2(x + 0, y + 1),
                        };

                        PolygonCollisionShape polygonCollisionShape = new PolygonCollisionShape(verts);
                        rbControl.addCollisionShape(polygonCollisionShape);

                    }
                }

            }
        }

        return rbControl;
    }

    public void update(float tpf) {

        pooledRigidBodies.entrySet().removeIf(entry -> {

            if (requiredPositions.size() > 0 && !requiredPositions.contains(entry.getKey())) {
                this.physicsSpace.remove(entry.getValue());
                return true;
            }

            return false;

        });

        this.requiredPositions.clear();

    }

    public void destroy() {
        executor.shutdown();
    }



}
