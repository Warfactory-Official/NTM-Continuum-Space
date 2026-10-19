package com.ntmspace.world.gen.legacy.layer;

/**
 * Minecraft 1.7.10's {@code GenLayerRiverMix}: lays the river layer over the biome layer, except on vanilla's two ocean
 * ids; a river over vanilla's ice plains freezes and over its mushroom islands becomes their shore. Subclasses
 * (hbm-space's Eve) override {@link #getInts} and read the two chains through {@link #biomes} and {@link #rivers}.
 */
public class GenLayerRiverMix17 extends GenLayer17 {

    // 1.7 BiomeGenBase ids
    public static final int OCEAN = 0;
    public static final int RIVER = 7;
    public static final int FROZEN_RIVER = 11;
    public static final int ICE_PLAINS = 12;
    public static final int MUSHROOM_ISLAND = 14;
    public static final int MUSHROOM_ISLAND_SHORE = 15;
    public static final int DEEP_OCEAN = 24;

    protected final GenLayer17 biomes;
    protected final GenLayer17 rivers;

    public GenLayerRiverMix17(long seed, GenLayer17 biomes, GenLayer17 rivers) {
        super(seed);
        this.biomes = biomes;
        this.rivers = rivers;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        this.biomes.initWorldGenSeed(seed);
        this.rivers.initWorldGenSeed(seed);
        super.initWorldGenSeed(seed);
    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        int[] aint = this.biomes.getInts(x, z, width, depth);
        int[] aint1 = this.rivers.getInts(x, z, width, depth);
        int[] aint2 = new int[width * depth];

        for (int i1 = 0; i1 < width * depth; ++i1) {
            if (aint[i1] != OCEAN && aint[i1] != DEEP_OCEAN) {
                if (aint1[i1] == RIVER) {
                    if (aint[i1] == ICE_PLAINS) {
                        aint2[i1] = FROZEN_RIVER;
                    } else if (aint[i1] != MUSHROOM_ISLAND && aint[i1] != MUSHROOM_ISLAND_SHORE) {
                        aint2[i1] = aint1[i1] & 255;
                    } else {
                        aint2[i1] = MUSHROOM_ISLAND_SHORE;
                    }
                } else {
                    aint2[i1] = aint[i1];
                }
            } else {
                aint2[i1] = aint[i1];
            }
        }
        return aint2;
    }
}
