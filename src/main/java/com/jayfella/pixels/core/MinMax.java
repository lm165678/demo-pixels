package com.jayfella.pixels.core;

public class MinMax {

    private int minX, minY;
    private int maxX, maxY;

    public MinMax() {
        clear();
    }

    public void clear() {
        minX = minY = maxX = maxY = -1;
    }

    public void evaluate(int x, int y) {

        if (x < minX) minX = x;
        else if (x > maxX) maxX = x;

        if (y < minY) minY = y;
        else if (y > maxY) maxY = y;

    }

    public int getMinX() { return minX; }
    public void setMinX(int minX) { this.minX = minX; }

    public int getMinY() { return minY; }
    public void setMinY(int minY) { this.minY = minY; }

    public int getMaxX() { return maxX; }
    public void setMaxX(int maxX) { this.maxX = maxX; }

    public int getMaxY() { return maxY; }
    public void setMaxY(int maxY) { this.maxY = maxY; }

}
