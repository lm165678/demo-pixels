package com.jayfella.pixels.item;


import com.jayfella.pixels.core.MathUtils;

public class ItemStack {

    private final Class<? extends Item> itemClass;
    private final int maxAmount;
    private int amount;

    public ItemStack(Class<? extends Item> itemClass, int maxAmount) {
        this(itemClass, maxAmount, 1);
    }

    public ItemStack(Class<? extends Item> itemClass, int maxAmount, int amount) {
        this.itemClass = itemClass;
        this.maxAmount = maxAmount;
        setAmount(amount);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = MathUtils.clamp(amount, 0, maxAmount);
    }

    public void addAmount(int amount) {
        this.amount = MathUtils.clamp(this.amount + amount, 0, maxAmount);
    }

    public void removeAmount(int amount) {
        this.amount = MathUtils.clamp(this.amount - amount, 0, maxAmount);
    }

}


