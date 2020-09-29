package com.jayfella.pixels.entity;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public interface Entity {



    Vector3f getLocation();
    void setLocation(Vector3f location);

    Spatial getModel();

}
