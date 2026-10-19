package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.gen.legacy.layer.GenLayer17;
import com.ntmspace.world.gen.legacy.layer.GenLayerRiver17;
import com.ntmspace.world.gen.legacy.layer.GenLayerRiverMix17;
import com.ntmspace.world.gen.legacy.layer.GenLayerSmooth17;
import com.ntmspace.world.gen.legacy.layer.GenLayerVoronoiZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerZoom17;

/**
 * Eve's biome map: hbm-space's {@code WorldProviderEve.createBiomeGenerators} with {@code GenLayerEveBiomes} and
 * {@code GenLayerEveRiverMix}. The ids are hbm-space's {@code SpaceConfig} defaults; they decide where rivers run (see
 * {@link GenLayerRiver17}), so they are kept. hbm-space's other Eve layers ({@code GenLayerDiversifyEve},
 * {@code GenLayerEveMountains}, {@code GenLayerEveOceans}, {@code GenLayerEveRiver}, {@code GenLayerEveSeismicPlains})
 * are in no chain and are not ported.
 */
public final class EveLayers17 {

    public static final int PLAINS = 117;
    public static final int MOUNTAINS = 116;
    public static final int OCEAN = 115;
    public static final int SEISMIC_PLAINS = 114;
    public static final int RIVER = 113;

    private EveLayers17() {}

    public static BiomeLayers17 create(long seed) {
        GenLayer17 genlayerBiomes = new Biomes(seed);

        genlayerBiomes = new GenLayerZoom17(1000L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom17(1001L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom17(1002L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom17(1003L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom17(1004L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom17(1005L, genlayerBiomes);

        GenLayer17 genlayerRiverZoom = new GenLayerZoom17(1000L, genlayerBiomes);
        GenLayer17 genlayerRiver = new GenLayerRiver17(1001L, genlayerRiverZoom);
        GenLayer17 genlayersmooth = new GenLayerSmooth17(1000L, genlayerRiver);

        GenLayer17 genlayersmooth1 = new GenLayerSmooth17(1000L, genlayerBiomes);
        GenLayer17 genlayerrivermix = new RiverMix(100L, genlayersmooth1, genlayersmooth);
        GenLayer17 genlayervoronoizoom = new GenLayerVoronoiZoom17(10L, genlayerrivermix);

        return new BiomeLayers17(genlayerrivermix, genlayervoronoizoom, seed);
    }

    /** hbm-space's {@code GenLayerEveBiomes}: plains, ocean or mountains, one cell in 20 seismic plains. */
    static final class Biomes extends GenLayer17 {

        private static final int[] BIOMES = {PLAINS, OCEAN, MOUNTAINS};
        private static final int[] RARE_BIOMES = {SEISMIC_PLAINS};

        Biomes(long seed) {
            super(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] dest = new int[width * depth];

            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    this.initChunkSeed(dx + x, dz + z);

                    if (this.nextInt(20) == 0) {
                        dest[dx + dz * width] = RARE_BIOMES[this.nextInt(RARE_BIOMES.length)];
                    } else {
                        dest[dx + dz * width] = BIOMES[this.nextInt(BIOMES.length)];
                    }
                }
            }
            return dest;
        }
    }

    /**
     * hbm-space's {@code GenLayerEveRiverMix}: rivers become Eve's river except on Eve's ocean. Its 1.7 test
     * "{@code getBiomeGenArray()[id] != null}" holds for every id the Eve chain produces, so that branch is always
     * taken. Like the 1.7 class it re-seeds both chains, then its superclass seeds them again (harmless, kept).
     */
    static final class RiverMix extends GenLayerRiverMix17 {

        RiverMix(long seed, GenLayer17 biomes, GenLayer17 rivers) {
            super(seed, biomes, rivers);
        }

        @Override
        public void initWorldGenSeed(long seed) {
            this.biomes.initWorldGenSeed(seed);
            this.rivers.initWorldGenSeed(seed);
            super.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int length) {
            int[] inputBiomeIds = this.biomes.getInts(x, z, width, length);
            int[] riverBiomeIds = this.rivers.getInts(x, z, width, length);
            int[] outputBiomeIds = new int[width * length];

            for (int i1 = 0; i1 < width * length; ++i1) {
                if (inputBiomeIds[i1] != EveLayers17.OCEAN) {
                    if (riverBiomeIds[i1] == GenLayerRiverMix17.RIVER) {
                        outputBiomeIds[i1] = EveLayers17.RIVER; // (GenLayerRiverMix17.RIVER is vanilla's 7)
                    } else {
                        outputBiomeIds[i1] = inputBiomeIds[i1];
                    }
                } else {
                    outputBiomeIds[i1] = inputBiomeIds[i1];
                }
            }
            return outputBiomeIds;
        }
    }
}
