package com.jayfella.pixels.item.generation;

import com.jayfella.pixels.core.Range;
import com.jayfella.pixels.core.SplineInterpolator;
import com.jayfella.pixels.item.Material;

import java.util.HashMap;
import java.util.Map;

// our world will have a height of 256
public class WorldGeneration {


    private static final Map<Material, SplineInterpolator> materialGenerationLikelihoods = new HashMap<>();
    private static final Map<Material, Range> materialNoiseRanges = new HashMap<>();

    static {

        createLikelihoods();
        createRanges();
    }

    private static void createLikelihoods() {

        // determines the likelihood of the material being generated based on its height in the world.
        // x-axis determines the world height
        // y-axis determines the likelihood of the ore spawning at that level.

        // so when we evaluate/interpolate we pass the height.

        SplineInterpolator interpolator;

        // copper is a cheap ore. Available near the top.
        interpolator = SplineInterpolator.createMonotoneCubicSpline(
                new Float[]{0f, 130f, 150f, 250f},
                new Float[]{0f, 0f, 1.0f, 1.0f}
        );
        materialGenerationLikelihoods.put(Material.Copper, interpolator);

        // iron is semi-easy. Harder than copper.
        interpolator = SplineInterpolator.createMonotoneCubicSpline(
                new Float[]{0f, 40f, 150f, 210f, 250f},
                new Float[]{0f, 0.5f, 1.0f, 0.4f, 0.0f}
        );
        materialGenerationLikelihoods.put(Material.Iron, interpolator);

        // silver is harder than iron to find, but easier than gold.
        interpolator = SplineInterpolator.createMonotoneCubicSpline(
                new Float[]{0f, 30f, 150f, 190f, 250f},
                new Float[]{0f, 0.8f, 1.0f, 0.4f, 0.0f}
        );
        materialGenerationLikelihoods.put(Material.Silver, interpolator);

        // gold is primarily used for electronics and jewelry. Rare.
        interpolator = SplineInterpolator.createMonotoneCubicSpline(
                new Float[]{0f, 30f, 100f, 150f, 250f},
                new Float[]{0f, 0.7f, 1.0f, 0.2f, 0.0f}
        );
        materialGenerationLikelihoods.put(Material.Gold, interpolator);

    }

    private static void createRanges() {

        // determines the range of noise that will be used to place the material in the world.
        // the distance between min and max determines the size of the vein.
        Range range;

        range = new Range(0.0f, 0.04f);
        materialNoiseRanges.put(Material.Copper, range);

        range = new Range(0.15f, 0.2f);
        materialNoiseRanges.put(Material.Iron, range);

        range = new Range(0.21f, 0.23f);
        materialNoiseRanges.put(Material.Silver, range);

        range = new Range(0.24f, 0.26f);
        materialNoiseRanges.put(Material.Gold, range);


    }

    public static float getLikelihoodFromHeight(Material material, float height) {

        SplineInterpolator interpolator = materialGenerationLikelihoods.get(material);

        if (interpolator != null) {
            return interpolator.interpolate(height);
        }

        return 0;
    }


    public static boolean isInRange(Material material, float value, float mult) {
        Range range = materialNoiseRanges.get(material);

        if (range == null) {
            return false;
        }

        return range.isInRange(value, mult);
    }

}
