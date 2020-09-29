package com.jayfella.pixels.physics;

/**
 * @author nickidebruyn
 */
public interface PhysicsTickListener {

    public void prePhysicsTick(PhysicsSpace space, float tpf);

    public void physicsTick(PhysicsSpace space, float tpf);

}
