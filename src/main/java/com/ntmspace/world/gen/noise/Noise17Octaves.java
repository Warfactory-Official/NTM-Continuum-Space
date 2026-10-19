package com.ntmspace.world.gen.noise;

import java.util.Arrays;
import java.util.Random;

/** Minecraft 1.7.10's {@code NoiseGeneratorOctaves}: octaves of {@link Noise17Improved}, frequency halving each. */
public final class Noise17Octaves {

    private final Noise17Improved[] octaves;

    public Noise17Octaves(Random rand, int count) {
        octaves = new Noise17Improved[count];
        for (int i = 0; i < count; ++i) octaves[i] = new Noise17Improved(rand);
    }

    /** 3D fill of {@code xSize * ySize * zSize} values at integer start {@code (x, y, z)}. */
    public double[] generate(double[] noise, int x, int y, int z, int xSize, int ySize, int zSize,
                             double xScale, double yScale, double zScale) {
        if (noise == null) noise = new double[xSize * ySize * zSize];
        else Arrays.fill(noise, 0.0D);
        double scale = 1.0D;
        for (Noise17Improved octave : octaves) {
            double dx = x * scale * xScale;
            double dy = y * scale * yScale;
            double dz = z * scale * zScale;
            long ix = floorLong(dx);
            long iz = floorLong(dz);
            dx -= ix;
            dz -= iz;
            ix %= 16777216L;
            iz %= 16777216L;
            dx += ix;
            dz += iz;
            octave.populate(noise, dx, dy, dz, xSize, ySize, zSize, xScale * scale, yScale * scale, zScale * scale, scale);
            scale /= 2.0D;
        }
        return noise;
    }

    /** 1.7's 2D form: a 3D fill one sample high at y = 10 with y scale 1 (the last argument is unused, as in 1.7). */
    public double[] generate(double[] noise, int x, int z, int xSize, int zSize, double xScale, double zScale,
                             double unused) {
        return generate(noise, x, 10, z, xSize, 1, zSize, xScale, 1.0D, zScale);
    }

    private static long floorLong(double v) {
        long i = (long) v;
        return v < i ? i - 1L : i;
    }
}
