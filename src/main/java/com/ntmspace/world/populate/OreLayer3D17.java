package com.ntmspace.world.populate;

import com.ntmspace.world.gen.noise.Noise17Perlin;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code OreLayer3D} on a celestial body: blobs of a block where three 4-octave Perlin fields multiply past a
 * threshold, between y 6 and 64, replacing the body's stone. It ran on {@code DecorateBiomeEvent.Pre} and draws no
 * random numbers. Its noises are seeded {@code world seed + 101/102/103 + id}, where {@code id} counts the
 * {@code OreLayer3D}s in the order hbm-space's {@code MainRegistry} built them (hematite 0, bauxite 1, malachite 2, the
 * Mun's conglomerate 3, Ike's 4, Minmus's regolith 5, smooth rock 6 and conglomerate 7).
 *
 * <p>Ported oddity: the z field reads {@code cacheX} (1.7 wrote {@code double nz = cacheX[ox][y]}), so the Z noise is
 * never used and the pattern is symmetric in a way the author did not intend.
 */
public final class OreLayer3D17 {

    private final int id;
    private final Supplier<BlockState> block;
    private final double scaleH;
    private final double scaleV;
    private final double threshold;

    private long lastSeed;
    private Noise17Perlin noiseX;
    private Noise17Perlin noiseY;

    public OreLayer3D17(int id, Supplier<BlockState> block, double scaleH, double scaleV, double threshold) {
        this.id = id;
        this.block = block;
        this.scaleH = scaleH;
        this.scaleV = scaleV;
        this.threshold = threshold;
    }

    /** {@code onDecorate} for the chunk at block origin {@code (cx, cz)}, replacing {@code replace}. */
    public void generate(PopulateWorld world, long worldSeed, int cx, int cz, Block replace) {
        Noise17Perlin nX;
        Noise17Perlin nY;
        synchronized (this) {
            if (noiseX == null || worldSeed != lastSeed) {
                noiseX = new Noise17Perlin(new Random(worldSeed + 101 + id), 4);
                noiseY = new Noise17Perlin(new Random(worldSeed + 102 + id), 4);
                // noiseZ (seed + 103 + id) fed cacheZ, which nothing read
                lastSeed = worldSeed;
            }
            nX = noiseX;
            nY = noiseY;
        }

        double[][] cacheX = new double[16][65];
        for (int o = 0; o < 16; o++) {
            for (int y = 64; y > 5; y--) {
                cacheX[o][y] = nX.value(y * scaleV, (cz + 8 + o) * scaleH);
            }
        }

        BlockState state = block.get();
        for (int ox = 0; ox < 16; ox++) {
            int x = cx + 8 + ox;
            for (int oz = 0; oz < 16; oz++) {
                int z = cz + 8 + oz;
                double ny = nY.value(x * scaleH, z * scaleH);
                for (int y = 64; y > 5; y--) {
                    double nx = cacheX[oz][y];
                    double nz = cacheX[ox][y];
                    if (nx * ny * nz > threshold) {
                        BlockState target = world.get(x, y, z);
                        if (PopulateWorld.normalCube(target) && target.is(replace)) {
                            world.set(x, y, z, state);
                        }
                    }
                }
            }
        }
    }
}
