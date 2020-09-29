package com.jayfella.pixels.player;

import com.jayfella.pixels.entity.Entity;
import com.jayfella.pixels.item.Inventory;
import com.jayfella.pixels.mesh.CenteredQuad;
import com.jayfella.pixels.physics.RigidBodyControl2D;
import com.jayfella.pixels.physics.shape.BoxCollisionShape;
import com.jayfella.pixels.world.World;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import org.dyn4j.geometry.MassType;

public class Player implements Entity {

    private final Geometry geometry;
    private final RigidBodyControl2D rigidBodyControl2D;
    private float zoom = 30.0f;

    private World world;

    private Inventory inventory;

    public Player(AssetManager assetManager, Vector3f startLocation) {

        Mesh mesh = new CenteredQuad(1.1f, 1.8f);
        geometry = new Geometry("Player", mesh);

        Material material = new Material(assetManager, Materials.UNSHADED);
        geometry.setMaterial(material);

        BoxCollisionShape boxCollisionShape = new BoxCollisionShape(1.1f, 1.8f);
        rigidBodyControl2D = new RigidBodyControl2D(boxCollisionShape, MassType.FIXED_ANGULAR_VELOCITY);
        rigidBodyControl2D.setAngularVelocity(0);
        rigidBodyControl2D.setDensity(600);

        geometry.addControl(rigidBodyControl2D);
        setLocation(startLocation);

        // Inventory
        inventory = new Inventory(32);
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public Spatial getModel() {
        return geometry;
    }

    public RigidBodyControl2D getRigidBodyControl2D() {
        return rigidBodyControl2D;
    }

    @Override
    public Vector3f getLocation() {
        return rigidBodyControl2D.getPhysicsLocation();
    }

    public void setLocation(Vector3f location) {
        rigidBodyControl2D.setPhysicsLocation(location.x, location.y);
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = FastMath.clamp(zoom, 10, 60);
    }

    public Inventory getInventory() {
        return inventory;
    }

}
