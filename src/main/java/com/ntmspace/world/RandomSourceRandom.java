package com.ntmspace.world;

import java.util.Random;
import net.minecraft.util.RandomSource;

/** Presents the caller's random source to shared legacy generators without reseeding or making any eager draws. */
public final class RandomSourceRandom extends Random {
    private final RandomSource source;
    public RandomSourceRandom(RandomSource source) { super(0L); this.source = source; }
    @Override protected int next(int bits) { return bits == 0 ? 0 : source.nextInt() >>> (32 - bits); }
    @Override public synchronized void setSeed(long seed) {
        // Random's constructor calls this before the delegate is assigned; constructing a view must not reseed it.
        if (source != null) source.setSeed(seed);
    }
    @Override public int nextInt() { return source.nextInt(); }
    @Override public int nextInt(int bound) { return source.nextInt(bound); }
    @Override public boolean nextBoolean() { return source.nextBoolean(); }
    @Override public long nextLong() { return source.nextLong(); }
    @Override public float nextFloat() { return source.nextFloat(); }
    @Override public double nextDouble() { return source.nextDouble(); }
    @Override public double nextGaussian() { return source.nextGaussian(); }
}
