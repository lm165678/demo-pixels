package com.jayfella.pixels.world.settings;

public class WorldSettings {

    private String name = "New World";
    private int nThreads = 2;
    private int seed = 0;

    public WorldSettings() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumThreads() {
        return nThreads;
    }

    public void setNumThreads(int nThreads) {
        this.nThreads = nThreads;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

}
