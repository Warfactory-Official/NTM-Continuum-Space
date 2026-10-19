package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * 1.7.10's {@code MapGenBase}: every chunk within {@link #range} of the chunk being generated may start a feature,
 * each seeded from the world seed and its own position, and each writes only its share of the buffer of the chunk
 * being generated.
 */
public abstract class MapGen17 {

    protected int range = 8;
    protected Random rand = new Random();
    protected Legacy17World world;
    /** The primer's meta buffer, for generators that write metadata (hbm-space's {@code MapGenBaseMeta}). */
    protected byte[] metas;

    public void setMetas(byte[] metas) {
        this.metas = metas;
    }

    /** 1.7 {@code func_151539_a}. */
    public void generate(Legacy17World world, int chunkX, int chunkZ, Block17[] blocks) {
        int k = this.range;
        this.world = world;
        this.rand.setSeed(world.seed());
        long l = this.rand.nextLong();
        long i1 = this.rand.nextLong();
        for (int j1 = chunkX - k; j1 <= chunkX + k; ++j1) {
            for (int k1 = chunkZ - k; k1 <= chunkZ + k; ++k1) {
                long l1 = (long) j1 * l;
                long i2 = (long) k1 * i1;
                this.rand.setSeed(l1 ^ i2 ^ world.seed());
                this.recursiveGenerate(j1, k1, chunkX, chunkZ, blocks);
            }
        }
    }

    /** 1.7 {@code func_151538_a}: one origin chunk {@code (offsetX, offsetZ)} against the chunk being generated. */
    protected abstract void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks);
}
