package com.jayfella.pixels.player.inventory;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;

public class InventorySlotContainer extends Container {

    public InventorySlotContainer() {
        super();

        TbtQuadBackgroundComponent backgroundComponent = (TbtQuadBackgroundComponent) getBackground();
        backgroundComponent.setMargin(5,5);

        setPreferredSize(new Vector3f(32,32,1));
    }

}
