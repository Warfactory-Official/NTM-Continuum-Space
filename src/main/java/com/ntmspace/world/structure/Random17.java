package com.ntmspace.world.structure;

import java.util.Random;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * A {@link java.util.Random} (1.7's generator random) whose state can be read: the same linear congruential generator,
 * drawn through {@link #next}, with the state kept here. {@link #source} hands the stream on, at its current state, as a
 * vanilla {@link LegacyRandomSource} (the same generator: HBM's 1.7 builders draw from a {@code RandomSource}), so a
 * builder 1.7 called with the generator's random continues exactly where the generator stood.
 */
public final class Random17 extends Random {

    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    /** The scrambled 48-bit state ({@code java.util.Random}'s {@code seed} field). Assigned by the constructor's setSeed. */
    private long state;

    public Random17(long seed) {
        super(seed);
    }

    @Override
    public synchronized void setSeed(long seed) {
        state = (seed ^ MULTIPLIER) & MASK;
        super.setSeed(seed); // clears the cached gaussian
    }

    @Override
    protected int next(int bits) {
        state = (state * MULTIPLIER + ADDEND) & MASK;
        return (int) (state >>> (48 - bits));
    }

    /** The scrambled state. */
    public long state() {
        return state;
    }

    /** A vanilla random at this state (it does not advance this one). */
    public RandomSource source() {
        return source(state);
    }

    /** A vanilla random at a scrambled state from {@link #state}. */
    public static RandomSource source(long state) {
        return new LegacyRandomSource(state ^ MULTIPLIER);
    }
}
