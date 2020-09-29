package com.jayfella.pixels.physics.shape;

import org.dyn4j.geometry.Convex;

public class ConvexCollisionShape extends CollisionShape {

    public ConvexCollisionShape(Convex convex) {
        cShape = convex;
    }
}
