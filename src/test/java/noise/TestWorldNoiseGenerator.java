package noise;

import com.jayfella.fastnoise.FastNoise;
import com.jayfella.pixels.core.NoiseEvaluator;
import com.jayfella.pixels.core.WorldConstants;
import com.jme3.math.FastMath;

import java.util.Random;

public class TestWorldNoiseGenerator implements NoiseEvaluator {

    private final Random random = new Random();
    private int seed;

    private final FastNoise heightTurbulenceNoise;
    private final FastNoise xTurbulentNoise;

    private final FastNoise lowlands;
    private final FastNoise highlands;
    private final FastNoise mountains;

    private final FastNoise terrainSelector;

    private final FastNoise caveNoise; // creates a step from RidgedMulti
    private final FastNoise caveFractal; // adds some noise to the smooth snakes that are caves.

    // private final float worldHeight = 720;

    // configurables
    private float surfaceHeight = 317;

    private float turbulenceX = 285;
    private float turbulenceY = 227;

    private float caveHeight = 1.0f;
    private float caveSize = 0.134f;

    public TestWorldNoiseGenerator() {

        heightTurbulenceNoise = new FastNoise();
        heightTurbulenceNoise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        heightTurbulenceNoise.SetFractalType(FastNoise.FractalType.FBM);
        heightTurbulenceNoise.SetInterp(FastNoise.Interp.Quintic);
        heightTurbulenceNoise.SetFractalOctaves(8);
        heightTurbulenceNoise.SetFrequency(0.008f);

        xTurbulentNoise = new FastNoise();
        xTurbulentNoise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        xTurbulentNoise.SetFractalType(FastNoise.FractalType.FBM);
        xTurbulentNoise.SetInterp(FastNoise.Interp.Quintic);
        xTurbulentNoise.SetFractalOctaves(8);
        xTurbulentNoise.SetFrequency(0.008f);

        lowlands = new FastNoise();
        lowlands.SetNoiseType(FastNoise.NoiseType.Perlin);
        lowlands.SetFrequency(0.008f);

        highlands = new FastNoise();
        highlands.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        highlands.SetFractalType(FastNoise.FractalType.RigidMulti);
        highlands.SetFractalOctaves(2);
        highlands.SetFrequency(0.008f);

        mountains = new FastNoise();
        mountains.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        mountains.SetFractalType(FastNoise.FractalType.Billow);
        mountains.SetFractalOctaves(4);
        mountains.SetFrequency(0.008f);

        terrainSelector = new FastNoise();
        terrainSelector.SetFrequency(0.01f); // how fast we go from lowland/highland/mountain

        caveNoise = new FastNoise();
        caveNoise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        caveNoise.SetFractalType(FastNoise.FractalType.RigidMulti);
        caveNoise.SetFractalOctaves(3);
        caveNoise.SetFrequency(0.03f);

        caveFractal = new FastNoise();
        caveFractal.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
        caveFractal.SetFractalType(FastNoise.FractalType.FBM);
        caveFractal.SetFractalOctaves(1);
        caveFractal.SetFrequency(0.008f);

        setSeed(FastMath.nextRandomInt());
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
        random.setSeed(seed);

        xTurbulentNoise.SetSeed(random.nextInt());
        heightTurbulenceNoise.SetSeed(random.nextInt());

        caveNoise.SetSeed(random.nextInt());
        caveFractal.SetSeed(random.nextInt());
    }

    public float getSurfaceHeight() { return surfaceHeight; }
    public void setSurfaceHeight(float surfaceHeight) { this.surfaceHeight = surfaceHeight; }

    public float getTurbulenceY() { return turbulenceY; }
    public void setTurbulenceY(float turbulenceY) { this.turbulenceY = turbulenceY; }

    public float getTurbulenceX() { return turbulenceX; }
    public void setTurbulenceX(float turbulenceX) { this.turbulenceX = turbulenceX; }

    public float getCaveHeight() { return caveHeight; }
    public void setCaveHeight(float caveHeight) { this.caveHeight = caveHeight; }

    public float getCaveSize() { return caveSize; }
    public void setCaveSize(float caveSize) { this.caveSize = caveSize; }


    // turn the value into a 0-1 range instead of -1,1
    private float normalize(float input) {
        input += 1;
        input *= 0.5;
        return input;
    }

    // create a binary mask so we can have some space above the land mass.
    private float getBinaryMaskValue(float x, float y) {

        // in this test we have a height of 720. This would normally be our
        // maximum world height.

        if (y > surfaceHeight) {
            return 0;
        }

        return 1;
    }



    // move the binary mask up/down using noise.
    private float addTurbulenceToY(float x, float y) {

        // turn the noise into a heightmap by only evaluating it in one dimension.
        float noise = heightTurbulenceNoise.GetNoise(x, 0);

        float normalized = normalize(noise);

        // how strong we move the y coordinate.
        // this is kind of how much we want the surface to deviate.
        float turbForce = turbulenceY;

        float result = y + (normalized * turbForce);

        return result;
    }

    private float addTurbulenceToX(float x, float y) {

        // turn the noise into a heightmap by only evaluating it in one dimension.
        float noise = xTurbulentNoise.GetNoise(x, y);

        // float normalized = normalize(noise);

        // how strong we move the y coordinate.
        // this is kind of how much we want the surface to deviate.
        float turbForce = turbulenceX;

        float result = x + (noise * turbForce);

        return result;
    }

    private float getBomeNoise(float x, float y) {
        // get the biome noise.

        float variance = 1.0f / 3; // three biomes..

        float biomeNoise = terrainSelector.GetNoise(x, 0);
        biomeNoise = normalize(biomeNoise);

        if (biomeNoise < variance) {
            return lowlands.GetNoise(x,y) * 0.2f;
        }
        else if (biomeNoise < variance * 2) {
            return highlands.GetNoise(x, y) * 0.45f;
        }
        else {
            return mountains.GetNoise(x, y) * 0.75f;
        }

    }



    private float getCaveNoise(float x, float y) {

        float cave = caveNoise.GetNoise(x, y);
        float normalizedY = caveHeight - (y / WorldConstants.MAX_HEIGHT);
        cave *= normalizedY;

        if (cave > caveSize)
            cave = 0;
        else
            cave = 1;

        // float fractal = normalize(caveFractal.GetNoise(x, y));

        return cave;// * fractal;

    }

    @Override
    public float evaluate(float x, float y) {

        float result;

        // float biomeNoise = normalize(getBomeNoise(x, y));
        //x += biomeNoise;

        float turbulentX = addTurbulenceToX(x, y);
        float turbulentY = addTurbulenceToY(turbulentX, y);
        float binaryMask = getBinaryMaskValue(x, turbulentY);

        float cave = getCaveNoise(x, y);

        result = binaryMask * cave;

        return result;
    }

}
