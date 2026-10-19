package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.gen.legacy.layer.GenLayer17;
import com.ntmspace.world.gen.legacy.layer.GenLayerDiversify17;
import com.ntmspace.world.gen.legacy.layer.GenLayerFuzzyZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerRiver17;
import com.ntmspace.world.gen.legacy.layer.GenLayerRiverMix17;
import com.ntmspace.world.gen.legacy.layer.GenLayerSmooth17;
import com.ntmspace.world.gen.legacy.layer.GenLayerVoronoiZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerZoom17;

/**
 * Moho's biome map: hbm-space's {@code WorldProviderMoho.createBiomeGenerators} with {@code GenLayerMohoBiomes} and
 * {@code GenLayerMohoPlateauExtend}. Ids are hbm-space's {@code SpaceConfig} defaults.
 *
 * <p><b>Ported oddity:</b> Moho mixes its rivers in with vanilla's {@code GenLayerRiverMix}, so the terrain noise sees
 * vanilla's {@code river} biome (id 7, height -0.5) along the borders of biomes of different id parity. The detail
 * chain ignores the rivers, so no column's surface is a river.
 */
public final class MohoLayers17 {

    public static final int CRAG = 101;
    public static final int BASALT = 100;
    public static final int LAVA_SEA = 99;
    public static final int PLATEAU = 98;

    private MohoLayers17() {}

    public static BiomeLayers17 create(long seed) {
        GenLayer17 biomes = new Biomes(seed);

        biomes = new GenLayerFuzzyZoom17(2000L, biomes);
        biomes = new GenLayerDiversify17(1234L, biomes, 6, BASALT);
        biomes = new GenLayerZoom17(2001L, biomes);
        biomes = new PlateauExtend(9944L, biomes);
        biomes = new GenLayerZoom17(1006L, biomes);
        biomes = new GenLayerSmooth17(700L, biomes);
        biomes = new GenLayerZoom17(1000L, biomes);
        biomes = new GenLayerZoom17(107L, biomes);

        // biome detail layer should ignore rivers (for monster spawns and biome block replacement)
        GenLayer17 genlayerVoronoiZoom = new GenLayerVoronoiZoom17(10L, biomes);

        GenLayer17 genlayerRiver = new GenLayerRiver17(1004L, biomes);
        genlayerRiver = new GenLayerZoom17(105L, genlayerRiver);
        genlayerRiver = new GenLayerZoom17(106L, genlayerRiver); // Added extra zoom for more frequent rivers

        // apply the rivers to the biome map
        GenLayer17 genlayerRiverMix = new GenLayerRiverMix17(100L, biomes, genlayerRiver);

        return new BiomeLayers17(genlayerRiverMix, genlayerVoronoiZoom, seed);
    }

    /** hbm-space's {@code GenLayerMohoBiomes}: crag twice as likely as lava sea. */
    static final class Biomes extends GenLayer17 {

        private static final int[] BIOMES = {CRAG, CRAG, LAVA_SEA};

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

    /** hbm-space's {@code GenLayerMohoPlateauExtend}: crag surrounded by crag or plateau becomes plateau. */
    static final class PlateauExtend extends GenLayer17 {

        PlateauExtend(long seed, GenLayer17 parent) {
            super(seed);
            this.parent = parent;
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

                    if (k1 == CRAG) {
                        int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
                        int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
                        int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
                        int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
                        boolean flag = ((l1 == CRAG || l1 == PLATEAU)
                                && (i2 == CRAG || i2 == PLATEAU)
                                && (j2 == CRAG || j2 == PLATEAU)
                                && (k2 == CRAG || k2 == PLATEAU));
                        if (flag) k1 = PLATEAU;
                    }

                    aint1[j1 + i1 * areaWidth] = k1;
                }
            }
            return aint1;
        }
    }
}
