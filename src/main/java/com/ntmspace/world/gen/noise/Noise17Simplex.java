package com.ntmspace.world.gen.noise;

import java.util.Random;

/**
 * Minecraft 1.7.10's {@code NoiseGeneratorSimplex} 2D bulk fill. Modern {@code SimplexNoise.getValue(x, y)} is the same
 * function of a point (checked by {@code Noise17Test}); this keeps 1.7's fill order: the second coordinate outermost,
 * values written in that order.
 */
public final class Noise17Simplex {

    private static final int[][] GRAD3 = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1},
            {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    private static final double SQRT_3 = Math.sqrt(3.0D);
    private static final double F2 = 0.5D * (SQRT_3 - 1.0D);
    private static final double G2 = (3.0D - SQRT_3) / 6.0D;

    private final int[] perm = new int[512];
    public final double xo;
    public final double yo;
    public final double zo;

    public Noise17Simplex(Random rand) {
        xo = rand.nextDouble() * 256.0D;
        yo = rand.nextDouble() * 256.0D;
        zo = rand.nextDouble() * 256.0D;
        for (int i = 0; i < 256; i++) perm[i] = i;
        for (int i = 0; i < 256; ++i) {
            int j = rand.nextInt(256 - i) + i;
            int k = perm[i];
            perm[i] = perm[j];
            perm[j] = k;
            perm[i + 256] = perm[i];
        }
    }

    private static int fastFloor(double v) {
        return v > 0.0D ? (int) v : (int) v - 1;
    }

    private static double corner(int[] g, double x, double y) {
        double t = 0.5D - x * x - y * y;
        if (t < 0.0D) return 0.0D;
        t *= t;
        return t * t * (g[0] * x + g[1] * y);
    }

    /** One point; the caller has already added this generator's offsets. */
    public double value(double xin, double yin) {
        double s = (xin + yin) * F2;
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        double t = (i + j) * G2;
        double x0 = xin - (i - t);
        double y0 = yin - (j - t);
        int i1 = x0 > y0 ? 1 : 0;
        int j1 = x0 > y0 ? 0 : 1;
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2;
        double y2 = y0 - 1.0D + 2.0D * G2;
        int ii = i & 255;
        int jj = j & 255;
        int gi0 = perm[ii + perm[jj]] % 12;
        int gi1 = perm[ii + i1 + perm[jj + j1]] % 12;
        int gi2 = perm[ii + 1 + perm[jj + 1]] % 12;
        return 70.0D * (corner(GRAD3[gi0], x0, y0) + corner(GRAD3[gi1], x1, y1) + corner(GRAD3[gi2], x2, y2));
    }

    /** 1.7 {@code func_151606_a}: adds {@code amplitude * value} over a {@code xSize * zSize} grid, z outermost. */
    public void add(double[] noise, double x, double z, int xSize, int zSize, double xScale, double zScale,
                    double amplitude) {
        int k = 0;
        for (int l = 0; l < zSize; ++l) {
            double zz = (z + l) * zScale + yo;
            for (int i1 = 0; i1 < xSize; ++i1) {
                double xx = (x + i1) * xScale + xo;
                noise[k++] += value(xx, zz) * amplitude;
            }
        }
    }
}
