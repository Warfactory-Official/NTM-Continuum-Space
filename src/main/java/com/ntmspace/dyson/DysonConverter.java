package com.ntmspace.dyson;

/**
 * hbm-space's {@code IDysonConverter}: a block entity a Dyson receiver's beam can feed. The receiver hands the energy of
 * one tick to the first converter its beam meets; a converter that refuses (the beam hit it anywhere but its intake)
 * makes the beam detonate there instead.
 */
public interface DysonConverter {

    /** Attempt to provide power at a given position on the converter; false if power can't be received at this location. */
    boolean provideEnergy(int x, int y, int z, long energy);

    /** Tell Dyson receivers our energy conversion limit (1.7 declared it; nothing reads it). */
    long maximumEnergy();
}
