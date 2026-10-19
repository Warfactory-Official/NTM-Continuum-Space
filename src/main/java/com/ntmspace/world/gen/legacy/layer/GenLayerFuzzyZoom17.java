package com.ntmspace.world.gen.legacy.layer;

/** Minecraft 1.7.10's {@code GenLayerFuzzyZoom}: a zoom whose diagonal cell is any of the four, never the mode. */
public class GenLayerFuzzyZoom17 extends GenLayerZoom17 {

    public GenLayerFuzzyZoom17(long seed, GenLayer17 parent) {
        super(seed, parent);
    }

    @Override
    protected int selectModeOrRandom(int a, int b, int c, int d) {
        return this.selectRandom(a, b, c, d);
    }
}
