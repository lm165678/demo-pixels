package noise;

import com.jayfella.fastnoise.FastNoise;
import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.item.Material;
import com.jayfella.pixels.item.generation.WorldGeneration;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestWorldOresNoiseGenerator implements NoiseEvaluator {

    // Block Types
    public static final Map<Integer, ColorRGBA> blockTypes = new HashMap<>();

    static {
        blockTypes.put(Material.Dirt.getId(), ColorRGBA.Brown.mult(1.5f));
        blockTypes.put(Material.Stone.getId(), ColorRGBA.White.mult(0.3f));
        blockTypes.put(Material.Gold.getId(), ColorRGBA.Yellow.clone());
        blockTypes.put(Material.Copper.getId(), ColorRGBA.Orange.clone());
        blockTypes.put(Material.Iron.getId(), ColorRGBA.White.mult(0.6f));
        blockTypes.put(Material.Silver.getId(), ColorRGBA.White.mult(0.9f));
    }


    private int seed = FastMath.nextRandomInt();
    private final Random random = new Random();

    // turbulence for the layer of dirt on the top of the world.
    // creates a sort of gradient randomness to the depth of the dirt layer.
    private FastNoise dirtNoise;

    // cellular noise for ore/material placement.
    private FastNoise oreNoise;

    // configurables
    private float dirtHeight = 250;
    private float dirtTurbulence = 227;

    public TestWorldOresNoiseGenerator() {
        dirtNoise = new FastNoise();
        dirtNoise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        dirtNoise.SetFractalOctaves(6);
        dirtNoise.SetFrequency(0.005f);

        oreNoise = new FastNoise();
        oreNoise.SetNoiseType(FastNoise.NoiseType.Cellular);
        oreNoise.SetFractalOctaves(6);
        oreNoise.SetFrequency(0.3f);

        setSeed(FastMath.nextRandomInt());
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
        random.setSeed(seed);

        dirtNoise.SetSeed(random.nextInt());
        oreNoise.SetSeed(random.nextInt());
    }

    public float getDirtHeight() { return dirtHeight; }
    public void setDirtHeight(float dirtHeight) { this.dirtHeight = dirtHeight; }

    public float getDirtTurbulence() { return dirtTurbulence; }
    public void setDirtTurbulence(float dirtTurbulence) { this.dirtTurbulence = dirtTurbulence; }

    // turn the value into a 0-1 range instead of -1,1
    private float normalize(float input) {
        input += 1;
        input *= 0.5;
        return input;
    }

    private float getDirtBinaryMaskValue(float x, float y) {

        if (y > dirtHeight) {
            return 0;
        }

        return 1;
    }

    private float addDirtTurbulenceToY(float x, float y) {

        // turn the noise into a heightmap by only evaluating it in one dimension.
        float noise = dirtNoise.GetNoise(x, 0);

        float normalized = normalize(noise);

        // how strong we move the y coordinate.
        // this is kind of how much we want the surface to deviate.
        float turbForce = dirtTurbulence;

        float result = y + (normalized * turbForce);

        return result;
    }

    @Override
    public float evaluate(float x, float y) {

        float oreNoise = normalize(this.oreNoise.GetNoise(x, y));

        // check for ores first.
        for (Material material : Material.values()) {

            float likelihood = WorldGeneration.getLikelihoodFromHeight(material, y);

            if (likelihood > 0) {

                if (WorldGeneration.isInRange(material, oreNoise, likelihood)) {
                    return material.getId();
                }
            }
        }

        // else draw dirt and rock.
        // dirt is in the upper portion of the map.
        // rock is in the lower.
        float turbulenceY = addDirtTurbulenceToY(x, y);

        if (turbulenceY > dirtHeight) {
            return 1;
        }

        return 2;
    }

}
