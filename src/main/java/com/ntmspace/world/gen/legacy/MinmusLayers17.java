package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.gen.legacy.layer.GenLayer17;
import com.ntmspace.world.gen.legacy.layer.GenLayerDiversify17;
import com.ntmspace.world.gen.legacy.layer.GenLayerFuzzyZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerSmooth17;
import com.ntmspace.world.gen.legacy.layer.GenLayerVoronoiZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerZoom17;

/**
 * Minmus's biome map: hbm-space's {@code WorldProviderMinmus.createBiomeGenerators} with {@code GenLayerMinmusBiomes},
 * {@code GenLayerMinmusBasins} and {@code GenLayerMinmusPlains}. Ids are hbm-space's {@code SpaceConfig} defaults
 * ({@code minmusPlains} is the hills biome, {@code minmusCanyon} the basins).
 */
public final class MinmusLayers17 {

    public static final int HILLS = 124;
    public static final int BASINS = 123;

    private MinmusLayers17() {}

    public static BiomeLayers17 create(long seed) {
        GenLayer17 biomes = new Biomes(seed);

        biomes = new GenLayerFuzzyZoom17(2000L, biomes);
        biomes = new GenLayerZoom17(2001L, biomes);
        biomes = new GenLayerDiversify17(1000L, biomes, 2, BASINS, HILLS);
        biomes = new GenLayerZoom17(1000L, biomes);
        biomes = new GenLayerDiversify17(1001L, biomes, 2, BASINS, HILLS);
        biomes = new GenLayerZoom17(1001L, biomes);
        biomes = new Basins(3000L, biomes);
        biomes = new GenLayerZoom17(1003L, biomes);
        biomes = new GenLayerSmooth17(700L, biomes);
        biomes = new Plains(200L, biomes);
        biomes = new GenLayerZoom17(1006L, biomes);

        GenLayer17 genLayerVoronoiZoom = new GenLayerVoronoiZoom17(10L, biomes);

        return new BiomeLayers17(biomes, genLayerVoronoiZoom, seed);
    }

    /** hbm-space's {@code GenLayerMinmusBiomes}: basins or hills, evenly. */
    static final class Biomes extends GenLayer17 {

        private static final int[] BIOMES = {BASINS, HILLS};

        Biomes(long seed) {
            super(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] dest = new int[width * depth];
            for (int k = 0; k < depth; ++k) {
                for (int i = 0; i < width; ++i) {
                    initChunkSeed(x + i, z + k);
                    dest[i + k * width] = BIOMES[nextInt(BIOMES.length)];
                }
            }
            return dest;
        }
    }

    /**
     * hbm-space's {@code GenLayerMinmusBasins} and {@code GenLayerMinmusPlains}: a cell of {@code self} next to
     * {@code other} becomes {@code self}, which it already is (both 1.7 layers are identities that only advance the
     * cell seed). Ported as written.
     */
    abstract static class Grow extends GenLayer17 {

        private final int self;
        private final int other;

        Grow(long seed, GenLayer17 parent, int self, int other) {
            super(seed);
            this.parent = parent;
            this.self = self;
            this.other = other;
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int i = areaX - 1;
            int j = areaY - 1;
            int k = 1 + areaWidth + 1;
            int l = 1 + areaHeight + 1;
            int[] aint = this.parent.getInts(i, j, k, l);
            int[] aint1 = new int[areaWidth * areaHeight];

            for (int i1 = 0; i1 < areaHeight; ++i1) {
                for (int j1 = 0; j1 < areaWidth; ++j1) {
                    this.initChunkSeed((long) (j1 + areaX), (long) (i1 + areaY));
                    int k1 = aint[j1 + 1 + (i1 + 1) * k];

                    if (k1 == self) {
                        int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
                        int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
                        int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
                        int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
                        boolean flag = l1 == other || i2 == other || j2 == other || k2 == other;
                        if (flag) k1 = self;
                    }

                    aint1[j1 + i1 * areaWidth] = k1;
                }
            }
            return aint1;
        }
    }

    static final class Basins extends Grow {
        Basins(long seed, GenLayer17 parent) {
            super(seed, parent, BASINS, HILLS);
        }
    }

    static final class Plains extends Grow {
        Plains(long seed, GenLayer17 parent) {
            super(seed, parent, HILLS, BASINS);
        }
    }
}
