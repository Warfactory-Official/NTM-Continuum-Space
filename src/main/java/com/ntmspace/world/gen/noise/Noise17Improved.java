package com.ntmspace.world.gen.noise;

import java.util.Random;

/**
 * Minecraft 1.7.10's {@code NoiseGeneratorImproved}, the improved-Perlin generator under every 1.7 terrain octave,
 * reproduced exactly: hbm-space's planet terrain ({@code ChunkProviderCelestial}) is built from it, and modern
 * {@code ImprovedNoise} is not the same function.
 *
 * <p>Two 1.7 behaviours are deliberate and must stay:
 * <ul>
 *   <li>In the 3D bulk fill the four x-lerped corner pairs are cached per integer y cell, keyed by the cell only. A
 *       sample later in the same cell reuses gradients dotted with the y fraction of the cell's FIRST sample. The
 *       low-frequency octaves (which dominate the sum) step less than one unit in y, so this shapes all 1.7 terrain.
 *       The cache key also survives from one (x, z) column to the next; only the first y sample of a column forces a
 *       refill.</li>
 *   <li>The 2D fill ({@code ySize == 1}) takes its first corner from the 2D gradient table and its other three from
 *       the 3D table with y = 0.</li>
 * </ul>
 * The gradient tables and the permutation shuffle are the ones modern {@code ImprovedNoise} still uses (checked by
 * {@code Noise17Test}).
 */
public final class Noise17Improved {

    private static final double[] GRAD_X = {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0};
    private static final double[] GRAD_Y = {1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1};
    private static final double[] GRAD_Z = {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1};
    private static final double[] GRAD2_X = {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0};
    private static final double[] GRAD2_Z = {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1};

    private final int[] permutations = new int[512];
    public final double xCoord;
    public final double yCoord;
    public final double zCoord;

    public Noise17Improved(Random rand) {
        this.xCoord = rand.nextDouble() * 256.0D;
        this.yCoord = rand.nextDouble() * 256.0D;
        this.zCoord = rand.nextDouble() * 256.0D;
        for (int i = 0; i < 256; i++) permutations[i] = i;
        for (int i = 0; i < 256; ++i) {
            int j = rand.nextInt(256 - i) + i;
            int k = permutations[i];
            permutations[i] = permutations[j];
            permutations[j] = k;
            permutations[i + 256] = permutations[i];
        }
    }

    static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    static double grad2(int hash, double x, double z) {
        int j = hash & 15;
        return GRAD2_X[j] * x + GRAD2_Z[j] * z;
    }

    static double grad(int hash, double x, double y, double z) {
        int j = hash & 15;
        return GRAD_X[j] * x + GRAD_Y[j] * y + GRAD_Z[j] * z;
    }

    /** One point without the cache: the per-sample function the bulk fill caches (used by tests). */
    public double sample(double x, double y, double z) {
        x += xCoord;
        y += yCoord;
        z += zCoord;
        int xi = floor(x), yi = floor(y), zi = floor(z);
        int px = xi & 255, py = yi & 255, pz = zi & 255;
        x -= xi;
        y -= yi;
        z -= zi;
        double fx = fade(x), fy = fade(y), fz = fade(z);
        int k5 = permutations[px] + py;
        int l5 = permutations[k5] + pz;
        int i6 = permutations[k5 + 1] + pz;
        int j6 = permutations[px + 1] + py;
        int k1 = permutations[j6] + pz;
        int i2 = permutations[j6 + 1] + pz;
        double d8 = lerp(fx, grad(permutations[l5], x, y, z), grad(permutations[k1], x - 1, y, z));
        double d9 = lerp(fx, grad(permutations[i6], x, y - 1, z), grad(permutations[i2], x - 1, y - 1, z));
        double d10 = lerp(fx, grad(permutations[l5 + 1], x, y, z - 1), grad(permutations[k1 + 1], x - 1, y, z - 1));
        double d11 = lerp(fx, grad(permutations[i6 + 1], x, y - 1, z - 1), grad(permutations[i2 + 1], x - 1, y - 1, z - 1));
        return lerp(fz, lerp(fy, d8, d9), lerp(fy, d10, d11));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    /** 1.7 {@code populateNoiseArray}: adds this octave into {@code noise}, x outermost, then z, then y. */
    public void populate(double[] noise, double xOffset, double yOffset, double zOffset, int xSize, int ySize,
                         int zSize, double xScale, double yScale, double zScale, double noiseScale) {
        double amplitude = 1.0D / noiseScale;
        int index = 0;
        if (ySize == 1) {
            for (int ix = 0; ix < xSize; ++ix) {
                double x = xOffset + ix * xScale + xCoord;
                int xi = floor(x);
                int px = xi & 255;
                x -= xi;
                double fx = fade(x);
                for (int iz = 0; iz < zSize; ++iz) {
                    double z = zOffset + iz * zScale + zCoord;
                    int zi = floor(z);
                    int pz = zi & 255;
                    z -= zi;
                    double fz = fade(z);
                    int l = permutations[px];
                    int j3 = permutations[l] + pz;
                    int k3 = permutations[px + 1];
                    int i1 = permutations[k3] + pz;
                    double near = lerp(fx, grad2(permutations[j3], x, z), grad(permutations[i1], x - 1.0D, 0.0D, z));
                    double far = lerp(fx, grad(permutations[j3 + 1], x, 0.0D, z - 1.0D),
                            grad(permutations[i1 + 1], x - 1.0D, 0.0D, z - 1.0D));
                    noise[index++] += lerp(fz, near, far) * amplitude;
                }
            }
            return;
        }
        int cachedCell = -1;
        double d8 = 0, d9 = 0, d10 = 0, d11 = 0;
        for (int ix = 0; ix < xSize; ++ix) {
            double x = xOffset + ix * xScale + xCoord;
            int xi = floor(x);
            int px = xi & 255;
            x -= xi;
            double fx = fade(x);
            for (int iz = 0; iz < zSize; ++iz) {
                double z = zOffset + iz * zScale + zCoord;
                int zi = floor(z);
                int pz = zi & 255;
                z -= zi;
                double fz = fade(z);
                for (int iy = 0; iy < ySize; ++iy) {
                    double y = yOffset + iy * yScale + yCoord;
                    int yi = floor(y);
                    int py = yi & 255;
                    y -= yi;
                    double fy = fade(y);
                    if (iy == 0 || py != cachedCell) {
                        cachedCell = py;
                        int k5 = permutations[px] + py;
                        int l5 = permutations[k5] + pz;
                        int i6 = permutations[k5 + 1] + pz;
                        int j6 = permutations[px + 1] + py;
                        int k1 = permutations[j6] + pz;
                        int i2 = permutations[j6 + 1] + pz;
                        d8 = lerp(fx, grad(permutations[l5], x, y, z), grad(permutations[k1], x - 1.0D, y, z));
                        d9 = lerp(fx, grad(permutations[i6], x, y - 1.0D, z), grad(permutations[i2], x - 1.0D, y - 1.0D, z));
                        d10 = lerp(fx, grad(permutations[l5 + 1], x, y, z - 1.0D),
                                grad(permutations[k1 + 1], x - 1.0D, y, z - 1.0D));
                        d11 = lerp(fx, grad(permutations[i6 + 1], x, y - 1.0D, z - 1.0D),
                                grad(permutations[i2 + 1], x - 1.0D, y - 1.0D, z - 1.0D));
                    }
                    double low = lerp(fy, d8, d9);
                    double high = lerp(fy, d10, d11);
                    noise[index++] += lerp(fz, low, high) * amplitude;
                }
            }
        }
    }
}
