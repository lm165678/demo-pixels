package com.jayfella.pixels.core;

public class Range {

    private float min, max;

    public Range(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public boolean isInRange(float value) {
        return value >= min && value <= max;
    }

    public boolean isInRange(float value, float mult) {
        return value >= min && value <=  max * mult;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

}
