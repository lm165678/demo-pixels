package com.jayfella.pixels.grid;

import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.core.WorldConstants;
import com.jayfella.pixels.grid.settings.GridSettings;
import com.jayfella.pixels.world.Chunk;
import com.jayfella.pixels.world.ChunkCell;
import com.jayfella.pixels.world.World;
import com.jme3.math.Vector3f;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class SceneGrid  {

    // Chunks that are currently being generated.
    private final List<Future<Chunk>> submittedTasks = new ArrayList<>();

    // Chunks that are currenty in-scene: loaded and visible to some extent (only the required cells are visible).
    private final Map<Integer, Chunk> loadedChunks = new HashMap<>();

    private final List<Integer> requiredChunks = new ArrayList<>(); // Chunks we require to view everything we need to view.
    private final Deque<Integer> chunkRemovals = new ArrayDeque<>(); // Chunks we need to remove because they are no longer visible.

    // temporary frame-by-frame lists
    private final Deque<Integer> chunkAdditions = new ArrayDeque<>(); // Chunks we need to add.
    private final List<Integer> unneededCells = new ArrayList<>(); // Chunks we need to remove.

    // Chunks that are currently being loaded.
    private final Set<Integer> loadingChunks = new HashSet<>();

    // keeps track of whether or not we moved grid positions.
    // Set them so they don't match initially so it forces a refresh.
    private final GridPos2i lastGridPos = new GridPos2i(Integer.MIN_VALUE, Integer.MIN_VALUE, WorldConstants.GRID_BITSHIFT);
    private final GridPos2i currentGridPos = new GridPos2i(Integer.MAX_VALUE, Integer.MAX_VALUE, WorldConstants.GRID_BITSHIFT);

    // keep a count of how many cells we've added and removed per-frame.
    private int removalIterations = 0;
    private int additionIterations = 0;

    private final World world;
    private final GridSettings gridSettings;

    public SceneGrid(World world, GridSettings gridSettings) {
        this.world = world;
        this.gridSettings = gridSettings;
    }

    public GridSettings getGridSettings() {
        return gridSettings;
    }

    public void setLocation(Vector3f location) {
        setLocation(location, false);
    }

    public void setLocation(Vector3f location, boolean forceUpdate) {

        currentGridPos.setFromWorldLocation(location);

        // if the last grid position equals the current grid position, nothing needs to be done unless we demand an update
        if (lastGridPos.equals(currentGridPos) && !forceUpdate) {
            return;
        }

        // only update the visible cells every time the user moves cells.
        updateChunkCellView();

        // we only need to update chunks if the x grid position has changed.
        if (lastGridPos.getX() == currentGridPos.getX() && !forceUpdate) {
            return;
        }

        // iterate over the view distance and add each grid position to a required list regardless of whether it's loaded or not..
        for (int x = currentGridPos.getX() - gridSettings.getViewDistance(); x <= currentGridPos.getX() + gridSettings.getViewDistance(); x++) {
            // for (int y = currentGridPos.getY() - gridSettings.getViewDistance(); y <= currentGridPos.getY() + gridSettings.getViewDistance(); y++) {
                requiredChunks.add(x);
            // }
        }

        // clear the list of current cell additions.
        // if we got this far, we need an entirely new set of cells than any other previous call.
        chunkAdditions.clear();

        // load cells we do need.
        // just blanket request all cells in our view distance.
        // the method that processes this list will not load any cells that already exist.
        chunkAdditions.addAll(requiredChunks);

        // if we remove the required cells from the loaded cells, we end up with a list of
        // cells we don't want anymore.
        unneededCells.addAll(loadedChunks.keySet().stream()
                .filter(key -> !requiredChunks.contains(key))
                .collect(Collectors.toList()));

        chunkRemovals.addAll(unneededCells);

        // tidy up after ourselves.
        requiredChunks.clear();
        unneededCells.clear();

        // set our last position to the set position.
        lastGridPos.set(currentGridPos);

    }

    private void updateThreadpool() {

        submittedTasks.removeIf(future -> {

            if (future.isCancelled()) {
                return true;
            }

            if (future.isDone()) {

                // ThreadedWorker worker;
                Chunk chunk;

                try {
                    chunk = future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }

                if (chunk != null) {
                    applyChunk(chunk);

                }

                return true;
            }

            return false;
        });

    }

    private void applyChunk(Chunk chunk) {
        world.getWorldNode().attachChild(chunk.getChunkNode());
        loadedChunks.put(chunk.getGridPosition(), chunk);
        loadingChunks.remove(chunk.getGridPosition());

        world.getChunkGenerator().initLightMap(chunk);

        // System.out.println("Attached chunk: " + chunk.getGridPosition() + " with children: " + chunk.getChunkNode().getChildren().size());
    }

    /**
     * Only displays chunk cells above and below the player that are within the view distance.
     * Removes cells from the scene that are outside it.
     */
    private void updateChunkCellView() {

        int yGrid = (int)world.getPlayer().getLocation().y >> WorldConstants.GRID_BITSHIFT;

        for (Chunk chunk : getLoadedChunks()) {

            for (int yCell = 0; yCell < WorldConstants.CELL_COUNT_Y; yCell++) {

                int cellMin = Math.max(0, yCell - gridSettings.getViewDistance());
                int cellMax = Math.min(WorldConstants.CELL_COUNT_Y, yCell + gridSettings.getViewDistance());

                ChunkCell chunkCell = chunk.getCell(yCell);

                if (chunkCell.getGeometry() != null) {
                    if (yGrid < cellMin || yGrid > cellMax) {
                        if (chunkCell.getGeometry().getParent() != null) {
                            chunkCell.getGeometry().removeFromParent();
                        }

                    } else {
                        if (chunkCell.getGeometry().getParent() == null) {
                            chunk.getChunkNode().attachChild(chunkCell.getGeometry());
                        }
                    }
                }
            }

        }

    }

    public void update(float tpf) {

        updateThreadpool();

        Integer chunkRemoval = chunkRemovals.poll();

        while (chunkRemoval != null) {

            Chunk chunk = loadedChunks.get(chunkRemoval);

            if (chunk != null) {
                loadedChunks.remove(chunkRemoval);
                chunk.destroy();

                // only iterate if we've actually removed a cell.
                removalIterations++;
            }

            // if we've removed the maximum amount this frame, wait until the next frame.
            if (removalIterations % gridSettings.getRemovalsPerFrame() == 0) {
                chunkRemoval = null;
            }

        }

        // reset the removal count.
        removalIterations = 0;

        Integer chunkAddition = chunkAdditions.poll();

        while (chunkAddition != null) {

            // if this position is loading or already loaded, ignore the cell load request.
            if (loadingChunks.contains(chunkAddition) || loadedChunks.containsKey(chunkAddition)) {
                chunkAddition = chunkAdditions.poll();
                continue;
            }

            loadingChunks.add(chunkAddition);

            ChunkLoadTask chunkLoadTask = new ChunkLoadTask(chunkAddition, world);
            submittedTasks.add(world.getThreadPool().submit(chunkLoadTask));

            additionIterations++;

            // if we've added the maximum amount this frame, wait until the next frame.
            if (additionIterations % gridSettings.getAdditionsPerFrame() == 0) {
                chunkAddition = null;
            }
            else {
                chunkAddition = chunkAdditions.poll();
            }
        }

        // reset the addition count.
        additionIterations = 0;

    }

    public Chunk getChunk(int xGrid) {
        return loadedChunks.get(xGrid);
    }

    public Collection<Chunk> getLoadedChunks() {
        return loadedChunks.values();
    }

}
