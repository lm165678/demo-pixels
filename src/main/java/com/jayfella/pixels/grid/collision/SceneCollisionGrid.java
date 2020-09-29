package com.jayfella.pixels.grid.collision;

import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.entity.Entity;
import com.jayfella.pixels.grid.GridTrackedEntity;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jayfella.pixels.physics.PhysicsSpace;
import com.jayfella.pixels.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class SceneCollisionGrid {

    private static final Logger log = LoggerFactory.getLogger(SceneCollisionGrid.class);

    // we have our own executor here because collisions don't want to be swamped behind any scene generation tasks.
    // private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // cells that are currently being generated.
    private final List<Future<CollisionCell>> submittedTasks = new ArrayList<>();

    // cells that are currenty in-scene: loaded and visible to some extent (only the required cells are visible).
    private final Map<GridPos2i, GreedyCollisionCell> loadedCells = new HashMap<>();

    private final List<GridPos2i> requiredCells = new ArrayList<>(); // cells we require to collide everything we need to.
    private final Deque<GridPos2i> cellRemovals = new ArrayDeque<>(); // cells we need to remove because they are no longer required.

    // temporary frame-by-frame lists
    private final Deque<GridPos2i> cellAdditions = new ArrayDeque<>(); // Cells we need to add.
    private final List<GridPos2i> unneededCells = new ArrayList<>(); // Cells we need to remove.

    // Cells that are currently being loaded.
    private final Set<GridPos2i> loadingCells = new HashSet<>();

    // keeps track of whether or not we moved grid positions.
    // Set them so they don't match initially so it forces a refresh.
    // private final GridPos2i lastGridPos = new GridPos2i(Integer.MIN_VALUE, Integer.MIN_VALUE, WorldConstants.GRID_BITSHIFT);
    // private final GridPos2i currentGridPos = new GridPos2i(Integer.MAX_VALUE, Integer.MAX_VALUE, WorldConstants.GRID_BITSHIFT);

    // keep a count of how many cells we've added and removed per-frame.
    private int removalIterations = 0;
    private int additionIterations = 0;

    private final PhysicsSpace physicsSpace;
    private final World world;
    private final GridSettings gridSettings;

    private final List<GridTrackedEntity> entities = new ArrayList<>();

    public SceneCollisionGrid(World world, PhysicsSpace physicsSpace, GridSettings gridSettings) {
        this.world = world;
        this.physicsSpace = physicsSpace;
        this.gridSettings = gridSettings;
    }

    public World getWorld() {
        return world;
    }

    public GridSettings getGridSettings() {
        return gridSettings;
    }

    public void addEntity(Entity entity) {
        entities.add(new GridTrackedEntity(entity, gridSettings));
    }

    public void removeEntity(Entity entity) {
        entities.removeIf(e -> e.getEntity() == entity);
    }

    public void updateGrid(float tpf) {

        for (GridTrackedEntity entity : entities) {

            entity.getCurrentGridPosition().setFromWorldLocation(entity.getLocation());

            if (entity.getLastGridPosition().equals(entity.getCurrentGridPosition())) {
                continue;
            }

            GridPos2i currentGridPos = entity.getCurrentGridPosition();

            for (int x = currentGridPos.getX() - gridSettings.getViewDistance(); x < currentGridPos.getX() + gridSettings.getViewDistance(); x++) {
                for (int y = currentGridPos.getY() - gridSettings.getViewDistance(); y < currentGridPos.getY() + gridSettings.getViewDistance(); y++) {
                    requiredCells.add(new GridPos2i(x, y, gridSettings.getCellSize().getBitshift()));
                }
            }

            // clear the list of current cell additions.
            // if we got this far, we need an entirely new set of cells than any other previous call.
            cellAdditions.clear();

            // load cells we do need.
            // just blanket request all cells in our view distance.
            // the method that processes this list will not load any cells that already exist.
            cellAdditions.addAll(requiredCells);

            // if we remove the required cells from the loaded cells, we end up with a list of
            // cells we don't want anymore.
            unneededCells.addAll(loadedCells.keySet().stream()
                    .filter(key -> !requiredCells.contains(key))
                    .collect(Collectors.toList()));

            cellRemovals.addAll(unneededCells);

            // tidy up after ourselves.
            requiredCells.clear();
            unneededCells.clear();

            // set our last position to the set position.
            entity.getLastGridPosition().set(currentGridPos);

        }

    }

    private void applyapplyCell(GreedyCollisionCell collisionCell) {
        world.getWorldNode().attachChild(collisionCell.getCellNode());
        loadedCells.put(collisionCell.getGridPosition(), collisionCell);
        loadingCells.remove(collisionCell.getGridPosition());

        physicsSpace.add(collisionCell.getRigidBodyControl());

        // System.out.println("Attached cell: " + collisionCell.getGridPosition() + " with children: " + collisionCell.getCellNode().getChildren().size());
    }

    public void update(float tpf) {

        updateGrid(tpf);

        GridPos2i cellRemoval = cellRemovals.poll();

        while (cellRemoval != null) {

            GreedyCollisionCell cell = loadedCells.get(cellRemoval);

            if (cell != null) {
                loadedCells.remove(cellRemoval);
                cell.destroy();

                // only iterate if we've actually removed a cell.
                removalIterations++;
            }

            // if we've removed the maximum amount this frame, wait until the next frame.
            if (removalIterations % gridSettings.getRemovalsPerFrame() == 0) {
                cellRemoval = null;
            }

        }

        if (log.isDebugEnabled() && removalIterations > 0) {
            log.debug("Collision Cells Removed This Frame: " + removalIterations);
        }

        // reset the removal count.
        removalIterations = 0;

        GridPos2i cellAddition = cellAdditions.poll();

        while (cellAddition != null) {

            // if this position is loading or already loaded, ignore the cell load request.
            if (loadingCells.contains(cellAddition) || loadedCells.containsKey(cellAddition)) {
                cellAddition = cellAdditions.poll();
                continue;
            }

            loadingCells.add(cellAddition);

            CollisionCellLoadTask collisionCellLoadTask = new CollisionCellLoadTask(cellAddition, this);

            try {
                GreedyCollisionCell cell = collisionCellLoadTask.call();
                applyapplyCell(cell);
            }catch (Exception e) {
                e.printStackTrace();;
            }

            // submittedTasks.add(executor.submit(collisionCellLoadTask));

            additionIterations++;

            // if we've added the maximum amount this frame, wait until the next frame.
            if (additionIterations % gridSettings.getAdditionsPerFrame() == 0) {
                cellAddition = null;
            }
            else {
                cellAddition = cellAdditions.poll();
            }
        }

        if (log.isDebugEnabled() && additionIterations > 0) {
            log.debug("Collision Cells Added This Frame: " + additionIterations);
        }

        // reset the addition count.
        additionIterations = 0;

    }

    /**
     * Re-generates a collision cell *if* the cell is loaded.
     * @param gridPos the grid position of the cell. Grid Position is relative to the CollisionGrid GridSettings.
     */
    public void refreshLoadedCell(GridPos2i gridPos) {

        if (loadedCells.containsKey(gridPos)) {
            cellRemovals.add(gridPos);
            cellAdditions.add(gridPos);
        }

    }

    public GreedyCollisionCell getCollisionCell(GridPos2i gridPos2i) {
        return loadedCells.get(gridPos2i);
    }

    public Collection<GreedyCollisionCell> getLoadedCollisionCells() {
        return loadedCells.values();
    }

    public void destroy() {
        // executor.shutdown();
    }

}
