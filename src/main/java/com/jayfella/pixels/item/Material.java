package com.jayfella.pixels.item;

public enum Material {

    AIR(0),
    Dirt(1),
    Stone(2),
    Copper(3),
    Iron(4),
    Gold(5),
    Silver(6),
    ;

    private final int id;

    private Material(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
