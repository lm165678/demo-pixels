package com.jayfella.pixels.item;

import com.jayfella.pixels.core.MathUtils;

public class Inventory {

    private final ItemStack[] content;
    private final int size;

    public Inventory(int size) {
        this.size = size;
        content = new ItemStack[size];
    }

    public int getSize() {
        return size;
    }

    public ItemStack get(int slot) {
        int inventorySlot = MathUtils.clamp(slot, 0, size - 1);
        return content[inventorySlot];
    }

    public void set(ItemStack itemStack, int slot) {
        int inventorySlot = MathUtils.clamp(slot, 0, size - 1);
        content[inventorySlot] = itemStack;
    }

}
