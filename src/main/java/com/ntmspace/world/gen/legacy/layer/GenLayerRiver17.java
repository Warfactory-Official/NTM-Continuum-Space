package com.ntmspace.world.gen.legacy.layer;

/**
 * Minecraft 1.7.10's {@code GenLayerRiver}: a river wherever a cell differs from a neighbour after folding every id of
 * 2 or more onto 2 or 3 by parity, -1 elsewhere. The river id is vanilla's {@code river} (7).
 *
 * <p>Fed biome ids rather than vanilla's river-init noise, as hbm-space's bodies do, the rivers trace the borders
 * between biomes of different id parity: so a body's 1.7 biome ids are load-bearing for its terrain and are kept.
 */
public class GenLayerRiver17 extends GenLayer17 {

    /** 1.7 {@code BiomeGenBase.river.biomeID}. */
    public static final int VANILLA_RIVER = 7;

    public GenLayerRiver17(long seed, GenLayer17 parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        int i1 = x - 1;
        int j1 = z - 1;
        int k1 = width + 2;
        int l1 = depth + 2;
        int[] aint = this.parent.getInts(i1, j1, k1, l1);
        int[] aint1 = new int[width * depth];

        for (int i2 = 0; i2 < depth; ++i2) {
            for (int j2 = 0; j2 < width; ++j2) {
                int k2 = fold(aint[j2 + 0 + (i2 + 1) * k1]);
                int l2 = fold(aint[j2 + 2 + (i2 + 1) * k1]);
                int i3 = fold(aint[j2 + 1 + (i2 + 0) * k1]);
                int j3 = fold(aint[j2 + 1 + (i2 + 2) * k1]);
                int k3 = fold(aint[j2 + 1 + (i2 + 1) * k1]);

                if (k3 == k2 && k3 == i3 && k3 == l2 && k3 == j3) {
                    aint1[j2 + i2 * width] = -1;
                } else {
                    aint1[j2 + i2 * width] = VANILLA_RIVER;
                }
            }
        }
        return aint1;
    }

    /** 1.7 {@code func_151630_c}. */
    private static int fold(int id) {
        return id >= 2 ? 2 + (id & 1) : id;
    }
}
