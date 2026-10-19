package com.ntmspace.world.gen.legacy.layer;

/**
 * Minecraft 1.7.10's {@code GenLayer}: a biome-id layer seeded from its own base seed and the world seed, with the
 * per-cell linear-congruential random 1.7's biome stacks are built on. hbm-space's celestial bodies stack these
 * (vanilla's zoom, smooth, river and Voronoi layers plus their own) exactly as 1.7's overworld did.
 *
 * <p>Proven equal to the 1.7.10 jar's {@code GenLayer} subclasses, value for value, by the scratchpad oracle
 * ({@code mc17/cmp/LayerCompare.java}: the same stacks built from the obfuscated vanilla classes).
 *
 * <p>1.7 handed out its arrays from {@code IntCache}, a pool reset per query; every layer here allocates its own array
 * instead, which gives the same values (no 1.7 layer read an array after handing it on).
 */
public abstract class GenLayer17 {

    private long worldGenSeed;
    protected GenLayer17 parent;
    private long chunkSeed;
    protected long baseSeed;

    protected GenLayer17(long seed) {
        this.baseSeed = seed;
        this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
        this.baseSeed += seed;
        this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
        this.baseSeed += seed;
        this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
        this.baseSeed += seed;
    }

    /** 1.7 {@code initWorldGenSeed}: seeds this layer (and its parents) from the world seed. */
    public void initWorldGenSeed(long seed) {
        this.worldGenSeed = seed;
        if (this.parent != null) this.parent.initWorldGenSeed(seed);
        this.worldGenSeed *= this.worldGenSeed * 6364136223846793005L + 1442695040888963407L;
        this.worldGenSeed += this.baseSeed;
        this.worldGenSeed *= this.worldGenSeed * 6364136223846793005L + 1442695040888963407L;
        this.worldGenSeed += this.baseSeed;
        this.worldGenSeed *= this.worldGenSeed * 6364136223846793005L + 1442695040888963407L;
        this.worldGenSeed += this.baseSeed;
    }

    /** 1.7 {@code initChunkSeed}. */
    public void initChunkSeed(long x, long z) {
        this.chunkSeed = this.worldGenSeed;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += x;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += z;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += x;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += z;
    }

    /** 1.7 {@code nextInt}. */
    protected int nextInt(int bound) {
        int i = (int) ((this.chunkSeed >> 24) % (long) bound);
        if (i < 0) i += bound;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += this.worldGenSeed;
        return i;
    }

    /** 1.7 {@code getInts}: the ids of the {@code width} x {@code depth} cells from {@code (x, z)}, x fastest. */
    public abstract int[] getInts(int x, int z, int width, int depth);

    /** 1.7 {@code selectRandom}. */
    protected int selectRandom(int... choices) {
        return choices[this.nextInt(choices.length)];
    }

    /** 1.7 {@code selectModeOrRandom}. */
    protected int selectModeOrRandom(int a, int b, int c, int d) {
        if (b == c && c == d) return b;
        if (a == b && a == c) return a;
        if (a == b && a == d) return a;
        if (a == c && a == d) return a;
        if (a == b && c != d) return a;
        if (a == c && b != d) return a;
        if (a == d && b != c) return a;
        if (b == c && a != d) return b;
        if (b == d && a != c) return b;
        if (c == d && a != b) return c;
        return this.selectRandom(a, b, c, d);
    }
}
