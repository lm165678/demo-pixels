package com.jayfella.pixels.core;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import java.util.Objects;
import java.util.logging.Logger;

public class GridPos2i implements Cloneable {
    private static final Logger log = Logger.getLogger(GridPos2i.class.getName());
    private int x;
    private int y;
    private int bitshift;

    private GridPos2i() {
    }

    public GridPos2i(int bitshift) {
        this.x = this.y = 0;
        this.bitshift = bitshift;
    }

    public GridPos2i(int x, int y, int bitshift) {
        this.x = x;
        this.y = y;
        this.bitshift = bitshift;
    }

    public static GridPos2i fromWorldLocation(float x, float y, int bitshift) {
        return new GridPos2i((int)x >> bitshift, (int)y >> bitshift, bitshift);
    }

    public static GridPos2i fromWorldLocation(Vector2f worldLocation, int bitshift) {
        return new GridPos2i((int)worldLocation.x >> bitshift, (int)worldLocation.y >> bitshift, bitshift);
    }

    public static GridPos2i fromWorldLocation(Vector3f worldLocation, int bitshift) {
        return new GridPos2i((int)worldLocation.x >> bitshift, (int)worldLocation.y >> bitshift, bitshift);
    }

    public int getBitshift() {
        return bitshift;
    }

    public void setBitshift(int bitshift) {
        this.bitshift = bitshift;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public GridPos2i set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public GridPos2i setFromWorldLocation(Vector3f in) {
        this.x = (int)in.x >> this.bitshift;
        this.y = (int)in.y >> this.bitshift;
        return this;
    }

    public GridPos2i set(GridPos2i in) {
        this.x = in.x;
        this.y = in.y;
        this.bitshift = in.bitshift;

        return this;
    }

    public Vector3f toWorldTranslation() {
        return new Vector3f((float)(this.x << this.bitshift), (float)(this.y << this.bitshift), 0);
    }

    public Vector2f toWorldTranslation2D() {
        return new Vector2f((float)(this.x << this.bitshift), (float)(this.y << this.bitshift));
    }

    public int getWorldTranslationX() {
        return this.x << this.bitshift;
    }

    public int getWorldTranslationY() {
        return this.y << this.bitshift;
    }

    public GridPos2i subtractLocal(int x, int y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public GridPos2i subtract(int x, int y) {
        return new GridPos2i(this.x - x, this.y - y, this.bitshift);
    }

    public GridPos2i addLocal(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public GridPos2i add(int x, int y) {
        return new GridPos2i(this.x + x, this.y + y, this.bitshift);
    }

    /**
     * Get the largest distance from this cell to another cell.
     * This check assumes both GridPositions have an equal bitshift.
     * @param other the other grid position to check.
     * @return the largest distance of the x,y planes in grid cells
     */
    public int farthestDist(GridPos2i other) {

        if (this.bitshift != other.bitshift) {
            throw new IllegalArgumentException("GridPosition bitshift must be equal!");
        }

        int x = Math.abs(other.getX() - this.x);
        int y = Math.abs(other.getY() - this.y);
        return Math.max(x, y); // get the larger of the two planes
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridPos2i gridPos2i = (GridPos2i) o;
        return x == gridPos2i.x &&
                y == gridPos2i.y &&
                bitshift == gridPos2i.bitshift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, bitshift);
    }

    @Override
    public String toString() {
        return String.format("%d,%d", this.x, this.y);
    }

    @Override
    public GridPos2i clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException e) {
            log.warning("Clone not supported.");
        }

        return new GridPos2i(this.x, this.y, this.bitshift);
    }
}
