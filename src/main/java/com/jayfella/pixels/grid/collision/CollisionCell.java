package com.jayfella.pixels.grid.collision;

import com.jayfella.pixels.core.GridPos2i;
import com.jayfella.pixels.physics.RigidBodyControl2D;
import com.jayfella.pixels.physics.shape.PolygonCollisionShape;
import com.jayfella.pixels.tile.Block;
import com.jme3.scene.Node;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

public class CollisionCell {

    private final Node node;
    private final RigidBodyControl2D rigidBodyControl;
    private final GridPos2i gridPos;


    // one-dimensional greedy meshing for collision-shapes.
    public CollisionCell(GridPos2i gridPos, SceneCollisionGrid collisionGrid) {
        node = new Node("Collision Cell: " + gridPos);
        this.gridPos = gridPos;

        rigidBodyControl = new RigidBodyControl2D(MassType.INFINITE);
        rigidBodyControl.setPhysicsLocation(gridPos.toWorldTranslation());

        int xWorld = gridPos.getWorldTranslationX();
        int yWorld = gridPos.getWorldTranslationY();

        int min, max;

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

                // if block is solid
                if (block.getType() > 0) {
                    // if the minimum has not been set, set it.
                    if (min < 0) {
                        min = x;
                    }

                    // this solves single-block issues because if min has been set and max has not, there's only a single block.
                    if (max < 0 || max < x) {
                        max = x;
                    }

                }

                // if we hit an air block OR we've reached the end of the chunk, create the row.
                if (block.getType() < 1 || x == collisionGrid.getGridSettings().getCellSize().getSize() - 1) {

                    // check if a min has been set. if not, there are no blocks in this row.
                    if (min > -1) {

                        // draw the rectangle.
                        Vector2[] verts = {
                                // bl, br, tr, tl
                                new Vector2(min, y),
                                new Vector2(max + 1, y),
                                new Vector2(max + 1, y + 1),
                                new Vector2(min, y + 1),
                        };

                        PolygonCollisionShape polygonCollisionShape = new PolygonCollisionShape(verts);
                        rigidBodyControl.addCollisionShape(polygonCollisionShape);

                        // reset the min/max so we can continue.
                        min = -1;
                        max = -1;

                    }

                }


            }
        }

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

}
