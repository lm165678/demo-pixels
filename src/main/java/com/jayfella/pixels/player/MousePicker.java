package com.jayfella.pixels.player;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

public class MousePicker {

    private final InputManager inputManager;
    private final Camera camera;

    private final CollisionResults collisionResults = new CollisionResults();
    private final Ray ray = new Ray();

    public MousePicker(InputManager inputManager, Camera camera) {
        this.inputManager = inputManager;
        this.camera = camera;
    }

    public Vector3f getClickLocation() {

        float projectionZ = camera.getViewToProjectionZ(camera.getLocation().z);

        Vector2f click2d = inputManager.getCursorPosition().clone();
        Vector3f click3d = camera.getWorldCoordinates(click2d, projectionZ).clone();

        System.out.println("HitPoint: " + click3d.x + ", " + click3d.y);

        return click3d;
    }

//    public Vector3f getClickLocation() {
//
//        Vector2f click2d = inputManager.getCursorPosition().clone();
//        Vector3f click3d0 = camera.getWorldCoordinates(click2d, 0f).clone();
//        Vector3f click3d1 = camera.getWorldCoordinates(click2d, 1f).clone();
//
//        Vector3f distance = click3d1.subtract(click3d0);
//        Vector3f dir = distance.normalize();
//
//        Vector3f loc = dir.mult(camera.getLocation().z);
//        Vector3f position = distance.add(loc);
//
//        // System.out.println(camera.getLocation().add(loc));
//        return camera.getLocation().add(loc);
//    }

    public CollisionResult pickClosestCollisionFromCursorPosition(Spatial collidables) {

        Vector2f click2d = inputManager.getCursorPosition().clone();
        Vector3f click3d = camera.getWorldCoordinates(click2d, 0f).clone();
        Vector3f dir = camera.getWorldCoordinates(click2d, 1f).subtractLocal(click3d).normalizeLocal();

        ray.setOrigin(click3d);
        ray.setDirection(dir);

        collidables.collideWith(ray, collisionResults);

        CollisionResult collisionResult = null;

        if (collisionResults.size() > 0) {
            collisionResult = collisionResults.getClosestCollision();
            collisionResults.clear();
        }

        return collisionResult;
    }

    public Vector3f getClosestContactPointFromCursorPosition(Spatial collidables) {
        CollisionResult collisionResult = pickClosestCollisionFromCursorPosition(collidables);

        if (collisionResult != null) {
            return collisionResult.getContactPoint();
        }

        return null;
    }

}
