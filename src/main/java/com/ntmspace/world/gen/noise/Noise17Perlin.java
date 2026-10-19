package com.ntmspace.world.gen.noise;

import java.util.Arrays;
import java.util.Random;

/** Minecraft 1.7.10's {@code NoiseGeneratorPerlin}: octaves of {@link Noise17Simplex} (the "stone noise"). */
public final class Noise17Perlin {

    private final Noise17Simplex[] levels;

    public Noise17Perlin(Random rand, int count) {
        levels = new Noise17Simplex[count];
        for (int i = 0; i < count; ++i) levels[i] = new Noise17Simplex(rand);
    }

    /** 1.7 {@code func_151601_a}: one point, octaves halving in frequency. */
    public double value(double x, double z) {
        double d2 = 0.0D;
        double d3 = 1.0D;
        for (Noise17Simplex level : levels) {
            d2 += level.value(x * d3, z * d3) / d3;
            d3 /= 2.0D;
        }
        return d2;
    }

    /** 1.7 {@code func_151599_a}: the full form with lacunarity 0.5. */
    public double[] generate(double[] noise, double x, double z, int xSize, int zSize, double xScale, double zScale,
                             double persistence) {
        return generate(noise, x, z, xSize, zSize, xScale, zScale, persistence, 0.5D);
    }

    /** 1.7 {@code func_151600_a}. */
    public double[] generate(double[] noise, double x, double z, int xSize, int zSize, double xScale, double zScale,
                             double persistence, double lacunarity) {
        if (noise != null && noise.length >= xSize * zSize) Arrays.fill(noise, 0.0D);
        else noise = new double[xSize * zSize];
        double frequency = 1.0D;
        double amplitude = 1.0D;
        for (Noise17Simplex level : levels) {
            level.add(noise, x, z, xSize, zSize, xScale * amplitude * frequency, zScale * amplitude * frequency,
                    0.55D / frequency);
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return noise;
    }
}
