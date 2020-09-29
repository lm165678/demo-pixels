package com.jayfella.pixels.physics;

import com.jayfella.pixels.physics.shape.BoxCollisionShape;
import com.jayfella.pixels.physics.shape.CollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.TempVars;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nickidebruyn
 */
public class RigidBodyControl2D extends AbstractControl implements PhysicsControl {

    protected boolean spatialAdded = false;
    private Body body;
    // protected BodyFixture bodyFixture;
    // protected CollisionShape collisionShape;
    protected PhysicsSpace physicsSpace;
//    protected float restitution = 0.0f;
//    protected float friction = 1f;
//    protected float density = 1f;
//    protected float xOffset = 0f;
//    protected float yOffset = 0f;

    public RigidBodyControl2D(MassType massType) {
        this(null, massType);
    }

    public RigidBodyControl2D(CollisionShape collisionShape, MassType massType) {
        // this.collisionShape = collisionShape;
        this.body = new Body();

        if (collisionShape != null) {
            addCollisionShape(collisionShape);
//            this.bodyFixture = new BodyFixture(collisionShape.getCShape());
//            this.bodyFixture.setRestitution(restitution);
//            this.bodyFixture.setFriction(friction);
//            this.bodyFixture.setDensity(density);
//            this.bodyFixture.setUserData(collisionShape);
//            this.bodyFixture.setFilter(new DefaultFilter());
//            this.body.addFixture(bodyFixture);
        }

        body.setMass(massType);
        /*
        if (mass != 0) {
            body.setAutoSleepingEnabled(false);

        } else {
            body.setAutoSleepingEnabled(true);
        }
         */

    }

    public void setAutoSleepingEnabled(boolean enabled) {
        this.body.setAutoSleepingEnabled(enabled);
    }

    /**
     * Add an extra collision shape to the rigid body. This will help with
     * performace.
     *
     * @param collisionShape
     */
    public BodyFixture addCollisionShape(CollisionShape collisionShape) {

//        this.bodyFixture = new BodyFixture(collisionShape.getCShape());
//        this.bodyFixture.setRestitution(restitution);
//        this.bodyFixture.setFriction(friction);
//        this.bodyFixture.setDensity(density);
//        this.bodyFixture.setUserData(collisionShape);
//        this.bodyFixture.setFilter(new DefaultFilter());

        BodyFixture bodyFixture = new BodyFixture(collisionShape.getCShape());
//        bodyFixture.setRestitution(restitution);
//        bodyFixture.setFriction(friction);
//        bodyFixture.setDensity(density);
        // bodyFixture.setUserData(collisionShape);
        // bodyFixture.setFilter(new DefaultFilter());

        this.body.addFixture(bodyFixture);

        return bodyFixture;
    }

    public void setCollisionShape(CollisionShape collisionShape) {
//        this.collisionShape = collisionShape;
//        this.body.removeFixture(bodyFixture);
//        this.bodyFixture = new BodyFixture(collisionShape.getCShape());
//        this.bodyFixture.setRestitution(restitution);
//        this.bodyFixture.setFriction(friction);
//        this.bodyFixture.setDensity(density);
//        this.bodyFixture.setFilter(new DefaultFilter());
//        this.body.addFixture(bodyFixture);
        body.removeAllFixtures();
        addCollisionShape(collisionShape);
    }

    public void setLinearVelocity(float x, float y) {
        this.body.setLinearVelocity(x, y);
    }

    public Vector2f getLinearVelocity() {
        return new Vector2f((float) this.body.getLinearVelocity().x, (float) this.body.getLinearVelocity().y);
    }

    public void setAngularVelocity(float angle) {
        this.body.setAngularVelocity(angle);
    }

    public float getAngularVelocity() {
        return (float) this.body.getAngularVelocity();
    }

    public Body getBody() {
        return this.body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    @Override
    protected void controlUpdate(final float tpf) {
        // Update spatial location and rotation
        if (physicsSpace != null && this.body != null) {

            if (!spatialAdded) {
                body.setUserData(spatial);
                spatialAdded = true;
            }

            if (!hasMultipleBodies()) {
//                System.out.println("one body");
                setPhysicsLocation(this.body);
                setPhysicsRotation(this.body);

            } else if (this.body.getMass().getType().equals(MassType.NORMAL)) {
//                    System.out.println("multiple bodies and mass");
                //TODO: We do not want to update the physics location of the spatial because it will be set by the
                //Collision shape location
                setPhysicsLocation(this.body);
                setPhysicsRotation(this.body);
            }

        }

    }

    public boolean hasMultipleBodies() {
        return this.body.getFixtureCount() > 1;
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {
    }

    public void setPhysicsRotation(final Body physicBody) {
        final Transform transform = physicBody.getTransform();

        final float rotation = Converter.toFloat(transform.getRotationAngle());

        final TempVars tempVars = TempVars.get();
        final Quaternion quaternion = tempVars.quat1;
        quaternion.fromAngleAxis(rotation, new Vector3f(0, 0, 1));

        this.spatial.setLocalRotation(quaternion);

        tempVars.release();
    }

    public void setPhysicsLocation(final Body physicBody) {
        final Transform transform = physicBody.getTransform();

        final float posX = Converter.toFloat(transform.getTranslationX());
        final float posY = Converter.toFloat(transform.getTranslationY());

        this.spatial.setLocalTranslation(posX, posY, this.spatial.getLocalTranslation().z);
    }

    public void setPhysicsLocation(Vector3f location) {
        clearForces();
        this.body.getTransform().setTranslationX(location.x);
        this.body.getTransform().setTranslationY(location.y);
    }

    public Vector3f getPhysicsLocation() {
        return new Vector3f(Converter.toFloat(body.getTransform().getTranslationX()), Converter.toFloat(body.getTransform().getTranslationY()), this.spatial.getLocalTranslation().z);
    }

    public Vector3f getWorldLocation() {
        return new Vector3f(Converter.toFloat(body.getWorldCenter().x), Converter.toFloat(body.getWorldCenter().y), this.spatial.getWorldTranslation().z);
    }

    public void clearForces() {
        this.body.clearForce();
        this.body.clearAccumulatedForce();
        this.body.clearTorque();
        this.body.clearAccumulatedTorque();
        this.body.setAngularVelocity(0);
        this.body.setLinearVelocity(0, 0);
    }

    public void setPhysicsLocation(float locationX, float locationY) {
//        clearForces();

        this.body.getTransform().setTranslationX(locationX);
        this.body.getTransform().setTranslationY(locationY);
    }

    public void move(float locationX, float locationY) {
//        clearForces();

        this.body.translate(locationX, locationY);
    }

    public void setPhysicsRotation(float radians) {
//        clearForces();
        this.body.getTransform().setRotation(radians);
    }

    public float getPhysicsRotation() {
//        clearForces();
        return (float) this.body.getTransform().getRotationAngle();
    }

    public void rotate(float radians) {
//        clearForces();
        this.body.rotateAboutCenter(radians);
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        this.physicsSpace = space;
    }

    public PhysicsSpace getPhysicsSpace() {
        return this.physicsSpace;
    }

//    public float getRestitution() {
//        return restitution;
//    }

    public void setRestitution(float restitution) {
        // this.restitution = restitution;

        if (body.getFixtures() != null && !body.getFixtures().isEmpty()) {
            for (BodyFixture fixture : body.getFixtures()) {
                fixture.setRestitution(restitution);
            }
        }

    }

//    public float getFriction() {
//        return friction;
//    }

    public void setFriction(float friction) {
        // this.friction = friction;
        // this.bodyFixture.setFriction(friction);

        if (body.getFixtures() != null && !body.getFixtures().isEmpty()) {
            for (BodyFixture fixture : body.getFixtures()) {
                fixture.setFriction(friction);
            }
        }

    }

//    public float getMass() {
//        return mass;
//    }

    public void setGravityScale(float scale) {
        this.body.setGravityScale(scale);
    }

//    public void setMass(float mass) {
//        this.mass = mass;
//        if (mass != 0) {
//            body.setMass(MassType.NORMAL);
////            body.setAutoSleepingEnabled(false);clscls
//
//        } else {
////            body.setAutoSleepingEnabled(true);
//        }
//    }

//    public float getDensity() {
//        return density;
//    }

    public void setDensity(float density) {
        // this.density = density;

        if (body.getFixtures() != null && !body.getFixtures().isEmpty()) {

            for (BodyFixture fixture : body.getFixtures()) {
                fixture.setDensity(density);
                fixture.createMass();
            }

//            for (int i = 0; i < this.body.getFixtures().size(); i++) {
//                BodyFixture bodyFixture1 = this.body.getFixtures().get(i);
//                bodyFixture1.setDensity((double) density);
//                bodyFixture1.createMass();
//            }

        }
//        else {
//            this.bodyFixture.setDensity((double) density);
//            this.bodyFixture.createMass();
//        }

    }

    public void setActive(boolean active) {
        this.body.setActive(active);
    }

    public boolean isActive() {
        return this.body.isActive();
    }

    public void setAsleep(boolean asleep) {
        this.body.setAsleep(asleep);
    }

    public boolean isAsleep() {
        return this.body.isAsleep();
    }

    public void applyForce(float x, float y) {
        this.body.applyForce(new Vector2(x, y));

    }

    public void applyImpulse(float x, float y) {
        this.body.applyImpulse(new Vector2(x, y));

    }

//    public float getxOffset() {
//        return xOffset;
//    }
//
//    public void setxOffset(float xOffset) {
//        this.xOffset = xOffset;
//    }
//
//    public float getyOffset() {
//        return yOffset;
//    }
//
//    public void setyOffset(float yOffset) {
//        this.yOffset = yOffset;
//    }

    /**
     * This method will make a rigidbody detect collision but whose collisions
     * will not be resolved.
     *
     * @param isSensor
     */
    public void setSensor(boolean isSensor) {
        if (body.getFixtures() != null && !body.getFixtures().isEmpty()) {
            for (BodyFixture fixture : body.getFixtures()) {
                fixture.setSensor(isSensor);
            }
        }
    }

//    public boolean isSensor() {
//        if (this.body.getFixtures() != null && this.body.getFixtures().size() > 0) {
//            for (int i = 0; i < this.body.getFixtures().size(); i++) {
//                BodyFixture bodyFixture1 = this.body.getFixtures().get(i);
//                return bodyFixture1.isSensor();
//            }
//        }
//        return false;
//    }

    /**
     * This method will make a rigidbody detect collision but whose collisions
     * will not be resolved.
     *
     * @param
     */
    public void setFilter(Filter filter) {
        if (body.getFixtures() != null && !body.getFixtures().isEmpty()) {
            for (BodyFixture fixture : body.getFixtures()) {
                fixture.setFilter(filter);
            }
        }
    }

    public void setAngularDamping(float damping) {
        this.body.setAngularDamping(damping);
    }

    public void setLinearDamping(float damping) {
        this.body.setLinearDamping(damping);
    }

    public List<List<BodyFixture>> getGroupedInSameSizeBodyFixtures() {

        List<List<BodyFixture>> groupedList = new ArrayList<>();
        List<BodyFixture> lastList = new ArrayList<>();

        if (body.getFixtures().size() > 0) {
            for (int i = 0; i < body.getFixtures().size(); i++) {
                BodyFixture bf = body.getFixtures().get(i);

                if (bf.getUserData() instanceof BoxCollisionShape) {
                    if (!lastList.contains(bf)) {
                        lastList = getSamesizeBodyFixtures(bf, body.getFixtures());
                        if (lastList.size() > 1) {
                            groupedList.add(lastList);
                        }
                    }
                }
            }
        }
        return groupedList;
    }

    /**
     * This will optimize the rigid body event more. All of the same size box
     * collision shaped will be merged into one box collision shape if they have
     * no gaps inbetween. This will optimize horizontally.
     */
    public void optimize() {

        //First we group all of the same size box collisions shapes together
        //Horizontal batching
        List<List<BodyFixture>> groupedList = getGroupedInSameSizeBodyFixtures();

        if (groupedList.size() > 0) {
            for (int i = 0; i < groupedList.size(); i++) {
                List<BodyFixture> arrayList = groupedList.get(i);
                batchHorizontallyBodiesOfSameSize(arrayList);
            }
        }

        //TODO: Need to improve and test
        //Vertical batching
//        groupedList = getGroupedInSameSizeBodyFixtures();
//        Debug.log("\n\nVERTICAL BATCHING :" + groupedList.size());
//        Debug.log("-----------------------------");
//        Debug.log("Before body fixtures: " + this.body.getFixtures().size());
//
//        if (groupedList.size() > 0) {
//            for (int i = 0; i < groupedList.size(); i++) {
//                ArrayList<BodyFixture> arrayList = groupedList.get(i);
//                batchVerticallyBodiesOfSameSize(arrayList);
//            }
//        }
//        Debug.log("After body fixtures: " + this.body.getFixtures().size());
    }

    /**
     * Group all box collision shaped bodies into a list.
     *
     * @param requiredBodyFixture
     * @param fixtures
     * @return
     */
    private ArrayList<BodyFixture> getSamesizeBodyFixtures(BodyFixture requiredBodyFixture, List<BodyFixture> fixtures) {
        ArrayList<BodyFixture> batchedFixtures = new ArrayList<>();
        batchedFixtures.add(requiredBodyFixture);

        for (int i = 0; i < fixtures.size(); i++) {
            BodyFixture bf = fixtures.get(i);
            if (!bf.equals(requiredBodyFixture)) {
                if (bf.getUserData() instanceof BoxCollisionShape) {
                    BoxCollisionShape bcs = (BoxCollisionShape) bf.getUserData();
                    BoxCollisionShape rbcs = (BoxCollisionShape) requiredBodyFixture.getUserData();

                    //Add same size bodies to the list
                    if (bcs.getHeight() == rbcs.getHeight() && bcs.getWidth() == rbcs.getWidth()) {
                        batchedFixtures.add(bf);
                    }

                }
            }

        }

        return batchedFixtures;
    }

    /**
     * Loop over this list of bodies with the same size and first batch them
     * together with no spacing and then after all was batched we merge them
     * into a bigger body.
     *
     * @param bodyFixtureList
     */
    private void batchHorizontallyBodiesOfSameSize(List<BodyFixture> bodyFixtureList) {
        //First we have to sort the list by position
        bodyFixtureList.sort(new BoxCollisionShapeSorter());

        //First we group all of the same size box collisions shapes together
        List<List<BodyFixture>> groupedList = new ArrayList<>();

        if (bodyFixtureList.size() > 0) {
            for (int i = 0; i < bodyFixtureList.size(); i++) {
                BodyFixture bf = bodyFixtureList.get(i);

                if (bf.getUserData() instanceof BoxCollisionShape) {
                    BoxCollisionShape boxCollisionShape = (BoxCollisionShape) bf.getUserData();
                    boolean contains = groupListContainsBody(groupedList, bf);

                    if (!contains) {
                        ArrayList<BodyFixture> sameList = getSameHorizontalSpacingBodyFixtures(
                                new Vector2f(boxCollisionShape.getLocation().x, boxCollisionShape.getLocation().y),
                                boxCollisionShape.getWidth(),
                                bodyFixtureList);

                        if (sameList.size() > 1) {
                            groupedList.add(sameList);

                        }
                    }
                }
            }
        }

        //Merge Horizontally
        mergeHorizontally(groupedList);

    }

    /**
     * Merge the list of horizontally grouped bodies
     *
     * @param groupedList
     */
    private void mergeHorizontally(List<List<BodyFixture>> groupedList) {

        if (groupedList.size() > 0) {
            for (List<BodyFixture> listOfMergableBodies : groupedList) {
                BoxCollisionShape firstBoxCollisionShape = (BoxCollisionShape) listOfMergableBodies.get(0).getUserData();
                float boxWidth = firstBoxCollisionShape.getWidth();
                float boxHeight = firstBoxCollisionShape.getHeight();
                float totalWidth = boxWidth * listOfMergableBodies.size();
                float xPos = firstBoxCollisionShape.getLocation().x + (totalWidth * 0.5f) - (boxWidth * 0.5f);
                float yPos = firstBoxCollisionShape.getLocation().y;

                //Remove all body fixtures
                for (BodyFixture bodyFixture1 : listOfMergableBodies) {
                    this.body.removeFixture(bodyFixture1);
                }

                //Now we create the new body
                BoxCollisionShape boxCollisionShape = new BoxCollisionShape(totalWidth, boxHeight);
                boxCollisionShape.setLocation(xPos, yPos);
                addCollisionShape(boxCollisionShape);

            }
        }
    }

    /**
     * Loop over this list of bodies with the same size and first batch them
     * together with no spacing and then after all was batched we merge them
     * into a bigger body.
     *
     * @param bodyFixtureList
     */
    private void batchVerticallyBodiesOfSameSize(List<BodyFixture> bodyFixtureList) {
        //First we have to sort the list by position
        bodyFixtureList.sort(new BoxCollisionShapeSorter());
//        printBodyFixturesList(bodyFixtureList, "");

        //First we group all of the same size box collisions shapes together
        List<List<BodyFixture>> groupedList = new ArrayList<>();

        if (bodyFixtureList.size() > 0) {
            for (int i = 0; i < bodyFixtureList.size(); i++) {
                BodyFixture bf = bodyFixtureList.get(i);

                if (bf.getUserData() instanceof BoxCollisionShape) {
                    BoxCollisionShape boxCollisionShape = (BoxCollisionShape) bf.getUserData();
                    boolean contains = groupListContainsBody(groupedList, bf);

                    if (!contains) {
                        List<BodyFixture> sameList = getSameVerticalSpacingBodyFixtures(
                                new Vector2f(boxCollisionShape.getLocation().x, boxCollisionShape.getLocation().y),
                                boxCollisionShape.getHeight(),
                                bodyFixtureList);

                        if (sameList.size() > 1) {
                            groupedList.add(sameList);

                        }
                    }
                }
            }
        }

        //Merge Horizontally
        mergeVertically(groupedList);

    }

    /**
     * Merge the list of vertically grouped bodies
     *
     * @param groupedList
     */
    private void mergeVertically(List<List<BodyFixture>> groupedList) {

        if (groupedList.size() > 0) {
            for (List<BodyFixture> listOfMergableBodies : groupedList) {
                BoxCollisionShape firstBoxCollisionShape = (BoxCollisionShape) listOfMergableBodies.get(0).getUserData();
                float boxWidth = firstBoxCollisionShape.getWidth();
                float boxHeight = firstBoxCollisionShape.getHeight();
                float totalHeight = boxHeight * listOfMergableBodies.size();
                float xPos = firstBoxCollisionShape.getLocation().x;
                float yPos = firstBoxCollisionShape.getLocation().y + (totalHeight * 0.5f) - (boxHeight * 0.5f);

                //Remove all body fixtures
                for (BodyFixture bodyFixture1 : listOfMergableBodies) {
                    this.body.removeFixture(bodyFixture1);
                }

                //Now we create the new body
                BoxCollisionShape boxCollisionShape = new BoxCollisionShape(boxWidth, totalHeight);
                boxCollisionShape.setLocation(xPos, yPos);
                addCollisionShape(boxCollisionShape);

            }
        }
    }

    /**
     * Determine if a group list already contains a body fixture. Shared call
     *
     * @param groupedList
     * @param bf
     * @return
     */
    private boolean groupListContainsBody(List<List<BodyFixture>> groupedList, BodyFixture bf) {
        BoxCollisionShape bcs = (BoxCollisionShape) bf.getUserData();

        for (int i = 0; i < groupedList.size(); i++) {
            List<BodyFixture> fixtureList = groupedList.get(i);
            for (int j = 0; j < fixtureList.size(); j++) {
                BodyFixture bodyFixture1 = fixtureList.get(j);
                BoxCollisionShape bcs1 = (BoxCollisionShape) bodyFixture1.getUserData();

                if (bcs.getLocation().x == bcs1.getLocation().x && bcs.getLocation().y == bcs1.getLocation().y) {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * Get body fitures horizontally with the same spacing
     *
     * @param startPosition
     * @param spacing
     * @param fixtures
     * @return
     */
    private ArrayList<BodyFixture> getSameHorizontalSpacingBodyFixtures(Vector2f startPosition, float spacing, List<BodyFixture> fixtures) {
        ArrayList<BodyFixture> batchedFixtures = new ArrayList<>();

        for (int i = 0; i < fixtures.size(); i++) {
            BodyFixture bfAtPos = getBodyFixtureAtPosition(fixtures, startPosition);
            if (bfAtPos != null) {
                batchedFixtures.add(bfAtPos);
                startPosition = startPosition.add(new Vector2f(spacing, 0)); //Move horizontally
            } else {
                break;
            }
        }
        return batchedFixtures;
    }

    /**
     * Get body fitures vertical with the same spacing
     *
     * @param startPosition
     * @param spacing
     * @param fixtures
     * @return
     */
    private ArrayList<BodyFixture> getSameVerticalSpacingBodyFixtures(Vector2f startPosition, float spacing, List<BodyFixture> fixtures) {
        ArrayList<BodyFixture> batchedFixtures = new ArrayList<>();

        for (int i = 0; i < fixtures.size(); i++) {
            BodyFixture bfAtPos = getBodyFixtureAtPosition(fixtures, startPosition);
            if (bfAtPos != null) {
                batchedFixtures.add(bfAtPos);
                startPosition = startPosition.subtract(new Vector2f(0, spacing)); //Move vertically
            } else {
                break;
            }
        }
        return batchedFixtures;
    }

    /**
     * This method will try to get a body fixture a specified position.
     *
     * @param fixtures
     * @param position
     * @return
     */
    private BodyFixture getBodyFixtureAtPosition(List<BodyFixture> fixtures, Vector2f position) {
        BodyFixture bodyFixture = null;
        for (int i = 0; i < fixtures.size(); i++) {
            BodyFixture bodyFixture1 = fixtures.get(i);
            BoxCollisionShape bcs = (BoxCollisionShape) bodyFixture1.getUserData();
            if (bcs.getLocation().x == position.x && bcs.getLocation().y == position.y) {
                bodyFixture = bodyFixture1;
                break;
            }
        }

        return bodyFixture;
    }

//    private void printBodyFixturesList(List<BodyFixture> list, String heading) {
//        for (int i = 0; i < list.size(); i++) {
//            BodyFixture bodyFixture1 = list.get(i);
//            Debug.log(heading + "\t: " + ((BoxCollisionShape) bodyFixture1.getUserData()).getLocation());
//        }
//    }
}
