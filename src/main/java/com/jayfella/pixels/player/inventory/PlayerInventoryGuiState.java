package com.jayfella.pixels.player.inventory;

import com.jayfella.pixels.item.Inventory;
import com.jayfella.pixels.player.Player;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Container;

public class PlayerInventoryGuiState extends BaseAppState {

    private final Player player;
    private final Inventory inventory;

    private Container container;

    public PlayerInventoryGuiState(Player player) {
        this.player = player;
        this.inventory = player.getInventory();
    }


    @Override
    protected void initialize(Application app) {

        this.container = new Container();

        // show the first 8 items in the player inventory
        for (int i = 0; i < 8; i++) {
            InventorySlotContainer inventorySlotContainer = new InventorySlotContainer();
            container.addChild(inventorySlotContainer, 0, i);
        }

        container.setLocalTranslation(
                app.getCamera().getWidth() * 0.5f - container.getPreferredSize().x * 0.5f,
                10 + container.getPreferredSize().y,
                0
        );

    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(container);
    }

    @Override
    protected void onDisable() {
        container.removeFromParent();
    }


}
