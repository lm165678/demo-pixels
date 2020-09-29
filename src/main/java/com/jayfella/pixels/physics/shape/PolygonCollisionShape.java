package com.jayfella.pixels.physics.shape;

import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;

public class PolygonCollisionShape extends CollisionShape {

    public PolygonCollisionShape(Vector2... vertices) {
        createShape(vertices);
    }

    protected void createShape(Vector2... vertices) {
        cShape = new Polygon(vertices);
    }
}
