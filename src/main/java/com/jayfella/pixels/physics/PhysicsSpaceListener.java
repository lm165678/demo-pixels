package com.jayfella.pixels.physics;

import org.dyn4j.dynamics.Body;

public interface PhysicsSpaceListener {

    void bodyAdded(Body body);
    void bodyRemoved(Body body);

}
