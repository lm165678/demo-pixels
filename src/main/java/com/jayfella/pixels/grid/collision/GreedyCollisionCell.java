package com.jayfella.pixels.grid.collision;

import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.physics.RigidBodyControl2D;
import com.jayfella.pixels.physics.shape.PolygonCollisionShape;
import com.jayfella.pixels.tile.Block;
import com.jme3.scene.Node;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GreedyCollisionCell {

    private final Node node;
    private final RigidBodyControl2D rigidBodyControl;
    private final GridPos2i gridPos;

    // two-dimensional greedy meshing for collision-shapes.
    public GreedyCollisionCell(GridPos2i gridPos, SceneCollisionGrid collisionGrid) {

        node = new Node("Collision Cell: " + gridPos);
        this.gridPos = gridPos;

        rigidBodyControl = new RigidBodyControl2D(MassType.INFINITE);
        rigidBodyControl.setPhysicsLocation(gridPos.toWorldTranslation());

        int xWorld = gridPos.getWorldTranslationX();
        int yWorld = gridPos.getWorldTranslationY();

        int min, max;

        Set<BlockPosition> visitedBlocks = new HashSet<>();

        for (int y = 0; y < collisionGrid.getGridSettings().getCellSize().getSize(); y++) {

            min = -1;
            max = -1;

            for (int x = 0; x < collisionGrid.getGridSettings().getCellSize().getSize(); x++) {

                int xPosWorld = xWorld + x;
                int yPosWorld = yWorld + y;

                // try to draw rectangles of blocks that are next to each other instead of individual squares for each block.

                Block block = collisionGrid.getWorld().getBlock(xPosWorld, yPosWorld);

                if (block == null) {
                    continue;
                }

                BlockPosition bp = new BlockPosition(x,y);

                // if block is solid
                if (block.getType() > 0 ) {

                    if (!visitedBlocks.contains(bp)) {

                        // if the minimum has not been set, set it.
                        if (min < 0) {
                            min = x;
                        }

                        // this solves single-block issues. If min has been set and max has not, there's only a single block.
                        if (max < 0 || max < x) {
                            max = x;
                        }
                    }

                }

                // if we hit an air block OR we've reached the end of the chunk, create the row.
                if (block.getType() < 1 || x == collisionGrid.getGridSettings().getCellSize().getSize() - 1 || visitedBlocks.contains(bp)) {

                    // check if a min has been set. if not, there are no blocks in this row.
                    if (min > -1) {

                        // traverse up the grid row by row until we hit a row that doesn't contain the same amount.
                        // of solid blocks in our row.

                        int maxY = y;

                        boolean giveUp = false;
                        while (!giveUp) {

                            maxY++;

                            if (maxY > collisionGrid.getGridSettings().getCellSize().getSize() - 1) {
                                giveUp = true;
                            }

                            // iterate horizontally.
                            for (int r = min; r <= max; r++) {

                                Block n = collisionGrid.getWorld().getBlock(xWorld + r, yWorld + maxY);

                                // if the block is not solid or it's already been visited, stop going up.
                                if (n.getType() < 1 || visitedBlocks.contains(new BlockPosition(r, maxY))) {
                                    maxY--;
                                    giveUp = true;
                                    break;
                                }
                            }

                            // if we haven't given up, add this row to the visited blocks.
                            if (!giveUp) {
                                for (int r = min; r <= max; r++) {
                                    visitedBlocks.add(new BlockPosition(r, maxY));
                                }
                            }

                        }

                        // we're done. We should have min/max X and Y to create our polygon.

                        // this works but I shouldn't have to do this. I need to investigate why maxY would be +1 the height.
                        if (maxY == collisionGrid.getGridSettings().getCellSize().getSize()) {
                            maxY -= 1;
                        }

                        // draw the rectangle.
                        Vector2[] verts = {
                                // bl, br, tr, tl
                                new Vector2(min, y),
                                new Vector2(max + 1, y),
                                new Vector2(max + 1, maxY + 1),
                                new Vector2(min, maxY + 1),
                        };

                        PolygonCollisionShape polygonCollisionShape = new PolygonCollisionShape(verts);
                        rigidBodyControl.addCollisionShape(polygonCollisionShape);

                        // reset the min/max so we can continue.
                        min = -1;
                        max = -1;

                    }

                }

                // only add solid blocks to our visited list because we already check if it's not solid - which is
                // cheaper than checking this collection.
                if (block.getType() > 0 ) {
                    visitedBlocks.add(bp);
                }

            }
        }

        // clean up after ourselves.
        visitedBlocks.clear();

    }

    public GridPos2i getGridPosition() {
        return gridPos;
    }

    public RigidBodyControl2D getRigidBodyControl() {
        return rigidBodyControl;
    }

    public Node getCellNode() {
        return node;
    }

    public void destroy() {

        if (rigidBodyControl.getPhysicsSpace() != null) {
            rigidBodyControl.getPhysicsSpace().remove(rigidBodyControl);
        }

        if (node.getParent() != null) {
            node.removeFromParent();
        }

    }

    private static class BlockPosition {

        private final int x, y;

        public BlockPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockPosition that = (BlockPosition) o;
            return x == that.x &&
                    y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

}
