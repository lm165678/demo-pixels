package com.jayfella.pixels.core;

import com.jme3.math.Vector2f;

/**
 * A generic noise evaluator that allows the user to use any noise generator.
 */
public interface NoiseEvaluator {
    float evaluate(float x, float y);
}
