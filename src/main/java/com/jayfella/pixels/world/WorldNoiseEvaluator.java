package com.jayfella.pixels.world;

import com.jayfella.fastnoise.FastNoise;
import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.core.WorldConstants;

public class WorldNoiseEvaluator implements NoiseEvaluator {

    private final FastNoise fastNoise;

    public WorldNoiseEvaluator(int seed) {
        fastNoise = new FastNoise(seed);
        fastNoise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        fastNoise.SetFractalOctaves(8);
        fastNoise.SetFrequency(0.05f);
    }

    // turn the value into a 0-1 range instead of -1,1
    private float normalize(float input) {
        input += 1;
        input *= 0.5;
        return input;
    }

    /*
        1) Create a binary region to determine when we start creating land.
        This allows us to not create land above a certain point.

        2) Add turbulence. Effectively moving the line created by the binary
        region up and down with noise.

        3)

     */

    @Override
    public float evaluate(float x, float y) {

        if (y > WorldConstants.MAX_HEIGHT - 1) {
            return 0;
        }

        float noise = normalize(fastNoise.GetNoise(x, y));

        float heightGradient = heightGradient(y);

        return noise * heightGradient;

    }

    // create a gradient map that has a "gap" at the very top.
    // this stops the world from generating at the top of the world.

    private float heightGradient(float y) {

        if (y > 200) {
            return 0;
        }

        else if (y < 180) {
            return 1;
        }

        float result = 1.0f - ( ( y - 180f ) / 20f );

        return result;
    }

}
