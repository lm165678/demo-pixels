package com.jayfella.pixels.physics;

import com.jayfella.pixels.mesh.CenteredQuad;
import com.jayfella.pixels.mesh.JmeMesh;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import org.dyn4j.collision.Bounds;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Capacity;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nickidebruyn
 */
public class Dyn4jAppState extends BaseAppState implements PhysicsSpaceListener {

    private static final long TIME_STEP_IN_MICROSECONDS = (long) (Settings.DEFAULT_STEP_FREQUENCY * 1000);
    /**
     * See {@link Application} for details.
     */
    protected Capacity initialCapacity;
    protected Bounds bounds;
    protected PhysicsSpace physicsSpace = null;
    protected float tpf = 0;
    protected float tpfSum = 0;

    // MultiTreading Fields
    protected ThreadingType threadingType;
    protected ScheduledThreadPoolExecutor executor;

    private final Runnable parallelPhysicsUpdate = () -> {
        if (!isEnabled()) {
            return;
        }

        Dyn4jAppState.this.physicsSpace.updateFixed(Dyn4jAppState.this.tpfSum);
        Dyn4jAppState.this.tpfSum = 0;
    };

    private boolean debugEnabled = false;

    public Dyn4jAppState() {
        this(null, null, ThreadingType.SEQUENTIAL);
    }

    public Dyn4jAppState(final Bounds bounds) {
        this(null, bounds, ThreadingType.SEQUENTIAL);
    }

    public Dyn4jAppState(final Capacity initialCapacity) {
        this(initialCapacity, null, ThreadingType.SEQUENTIAL);
    }

    public Dyn4jAppState(final Capacity initialCapacity, final Bounds bounds) {
        this(initialCapacity, bounds, ThreadingType.SEQUENTIAL);
    }

    public Dyn4jAppState(final ThreadingType threadingType) {
        this(null, null, threadingType);
    }

    public Dyn4jAppState(final Bounds bounds, final ThreadingType threadingType) {
        this(null, bounds, threadingType);
    }

    public Dyn4jAppState(final Capacity initialCapacity, final ThreadingType threadingType) {
        this(initialCapacity, null, threadingType);
    }

    public Dyn4jAppState(final Capacity initialCapacity, final Bounds bounds, final ThreadingType threadingType) {
        this.threadingType = threadingType;
        this.initialCapacity = initialCapacity;
        this.bounds = bounds;
        startPhysics();
    }

    @Override
    public void initialize( final Application app) {
        // Start physic related objects.
        // startPhysics();
        if (debugShapeMaterial == null) {
            debugShapeMaterial = new Material(getApplication().getAssetManager(), Materials.UNSHADED);
            debugShapeMaterial.getAdditionalRenderState().setWireframe(true);
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }

        this.physicsSpace.clear();
    }

    @Override
    protected void onEnable() {
        schedulePhysicsCalculationTask();
    }

    @Override
    protected void onDisable() {
        if (this.executor != null) {
            this.executor.remove(this.parallelPhysicsUpdate);
        }
    }

    private void startPhysics() {

        if (this.threadingType == ThreadingType.PARALLEL) {
            startPhysicsOnExecutor();
        } else {
            physicsSpace = new PhysicsSpace(initialCapacity, bounds);
            physicsSpace.addPhysicsSpaceListener(this);
        }

    }

    private void startPhysicsOnExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
        executor = new ScheduledThreadPoolExecutor(1);

        final Callable<Boolean> call = () -> {
            physicsSpace = new PhysicsSpace(initialCapacity, bounds);

            // hmmmmmm.. this is going to be called on another thread....
            physicsSpace.addPhysicsSpaceListener(this);
            return true;
        };

        try {
            this.executor.submit(call).get();
        } catch (final Exception ex) {
            Logger.getLogger(Dyn4jAppState.class.getName()).log(Level.SEVERE, null, ex);
        }

        schedulePhysicsCalculationTask();
    }

    private void schedulePhysicsCalculationTask() {
        if (this.executor != null) {
            this.executor.scheduleAtFixedRate(this.parallelPhysicsUpdate, 0L, TIME_STEP_IN_MICROSECONDS, TimeUnit.MICROSECONDS);
        }
    }

    /**
     * See {@link AppStateManager#update(float)}. Note: update method is not
     * called if enabled = false.
     */
    @Override
    public void update(final float tpf) {
        if (!isEnabled()) {
            return;
        }

        this.tpf = tpf;
        this.tpfSum += tpf;

        if (debugEnabled) {
            renderDebugs();
        }
    }

    /**
     * See {@link AppStateManager#render(RenderManager)}. Note: render method is
     * not called if enabled = false.
     */
    @Override
    public void render(final RenderManager rm) {

        if (threadingType == ThreadingType.PARALLEL) {
            executor.submit(parallelPhysicsUpdate);

        } else if (threadingType == ThreadingType.SEQUENTIAL) {
            final float timeStep = isEnabled() ? this.tpf * this.physicsSpace.getSpeed() : 0;
            this.physicsSpace.updateFixed(timeStep);

        }
    }

    public PhysicsSpace getPhysicsSpace() {
        return this.physicsSpace;
    }



    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;

        if (enabled) {

            // first add all the ones that don't exist yet (query the world)
            // then rely on the add/remove to mediate the visuals.

            List<Body> bodies = physicsSpace.getPhysicsWorld().getBodies();

            for (Body body : bodies) {

                List<BodyFixture> fixtures = body.getFixtures();

                for (BodyFixture fixture : fixtures) {

                    Convex shape = fixture.getShape();

                    Geometry geometry = debugShapes.get(shape);

                    if (geometry == null) {

                        if (shape instanceof Rectangle) {

                            Rectangle rectangle = (Rectangle) shape;

                            Mesh mesh = new Quad( (float) rectangle.getWidth(), (float) rectangle.getHeight() );
                            geometry = new Geometry("Box Collision Shape", mesh);
                            geometry.setMaterial(debugShapeMaterial);
                            debugShapes.put(shape, geometry);
                            debugNode.attachChild(geometry);

                        }
                    }

                    if (geometry != null) {
                        geometry.setLocalTranslation(
                                (float) shape.getCenter().x,
                                (float) shape.getCenter().y,
                                0.1f);
                    }

                }

            }

        }
        else {
            debugShapes.clear();
            debugNode.detachAllChildren();
            debugNode.removeFromParent();

            debugShapeMaterial = null;
        }
    }

    private Material debugShapeMaterial;
    private final Map<Convex, Geometry> debugShapes = new HashMap<>();
    private final Node debugNode = new Node("Dyn4J Debug Node");

    private void renderDebugs() {

        if (debugNode.getParent() == null) {
            ((SimpleApplication) getApplication()).getRootNode().attachChild(debugNode);
        }

        List<Body> bodies = physicsSpace.getPhysicsWorld().getBodies();

        for (Body body : bodies) {

            List<BodyFixture> fixtures = body.getFixtures();

            for (BodyFixture fixture : fixtures) {

                Convex shape = fixture.getShape();

                Geometry geometry = debugShapes.get(shape);

                if (geometry != null) {

                    geometry.setLocalTranslation(
                            (float) body.getTransform().getTranslation().x,
                            (float) body.getTransform().getTranslation().y,
                            0.001f // juuuust slightly in front of the terrain.
                    );

                    // one-dimensional rotation in 2D
                    Quaternion quaternion = new Quaternion()
                            .fromAngles(
                                    0,
                                    0,
                                    (float)body.getTransform().getRotationAngle());

                    geometry.setLocalRotation(quaternion);

                }
            }
        }


    }

    @Override
    public void bodyAdded(Body body) {

        List<BodyFixture> fixtures = body.getFixtures();

        for (BodyFixture fixture : fixtures) {

            Convex shape = fixture.getShape();

            Geometry geometry = debugShapes.get(shape);

            if (geometry == null) {

                // shapes are created from the center in Box2D.
                // JME Quads are not, and probably some other stuff. Just be aware.

                if (shape instanceof Rectangle) {

                    Rectangle rectangle = (Rectangle) shape;

//                    float halfWidth = (float) rectangle.getWidth() * 0.5f;
//                    float halfHeight = (float) rectangle.getHeight() * 0.5f;
//
//                    float z = 0.001f; // juuust slightly in front of 0
//
//                    JmeMesh mesh = new JmeMesh();
//
//                    Vector3f[] verts = { // bl, br, tl, tr
//                            new Vector3f(-halfWidth, -halfHeight, z),
//                            new Vector3f(halfWidth, -halfHeight, z),
//                            new Vector3f(-halfWidth, halfHeight, z),
//                            new Vector3f(halfWidth, halfHeight, z)
//                    };
//
//                    mesh.set(VertexBuffer.Type.Position, verts);

                    Mesh mesh = new CenteredQuad((float)rectangle.getWidth(), (float)rectangle.getHeight());

                    geometry = new Geometry("Box Collision Shape", mesh);
                    geometry.setMaterial(debugShapeMaterial);
                    debugShapes.put(shape, geometry);
                    debugNode.attachChild(geometry);

                }

                else if (shape instanceof Polygon) {

                    Polygon polygon = (Polygon) shape;

                    Vector2[] vertices = polygon.getVertices();
                    Vector3f[] meshVerts = new Vector3f[vertices.length];

                    for (int i = 0; i < meshVerts.length; i++) {
                        meshVerts[i] = new Vector3f((float)vertices[i].x, (float)vertices[i].y, 0.01f);
                    }

                    Integer[] indices = { 0,1,2, 0,2,3 };

                    JmeMesh mesh = new JmeMesh();
                    mesh.set(VertexBuffer.Type.Position, meshVerts);
                    mesh.set(VertexBuffer.Type.Index, indices);

                    geometry = new Geometry("Polygon Shape", mesh);
                    geometry.setMaterial(debugShapeMaterial);

                    geometry.setLocalTranslation(
                            (float) body.getTransform().getTranslationX(),
                            (float) body.getTransform().getTranslationY(),
                            0);

                    debugShapes.put(shape, geometry);
                    debugNode.attachChild(geometry);


                }

            }

        }

    }

    @Override
    public void bodyRemoved(Body body) {

        List<BodyFixture> fixtures = body.getFixtures();

        for (BodyFixture fixture : fixtures) {

            Convex shape = fixture.getShape();

            Geometry geometry = debugShapes.get(shape);

            if (geometry != null) {
                debugShapes.remove(shape);
                geometry.removeFromParent();
            }

        }

    }

}
