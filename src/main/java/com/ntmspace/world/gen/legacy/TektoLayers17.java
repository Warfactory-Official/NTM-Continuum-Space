package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.gen.legacy.layer.GenLayer17;
import com.ntmspace.world.gen.legacy.layer.GenLayerRiver17;
import com.ntmspace.world.gen.legacy.layer.GenLayerRiverMix17;
import com.ntmspace.world.gen.legacy.layer.GenLayerSmooth17;
import com.ntmspace.world.gen.legacy.layer.GenLayerVoronoiZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerZoom17;

/**
 * hbm-space's Tekto biome map: {@code WorldProviderTekto.createBiomeGenerators} with {@code GenlayerTektoBiomes} and
 * {@code GenLayerTektoRiverMix}, ported line for line. Ids are hbm-space's {@code SpaceConfig} defaults; vanilla's
 * {@code GenLayerRiver} reads their parity.
 *
 * <p>hbm-space also has {@code GenLayerDiversifyTekto}, {@code GenlayerTektoPlains} and
 * {@code GenlayerTetrachloricRiver}; Tekto's stack never uses them, so they are not ported.
 */
public final class TektoLayers17 {

    public static final int POLYVINYL_PLAINS = 92;
    public static final int HALOGEN_HILLS = 91;
    public static final int TETRACHLORIC_RIVER = 90;
    public static final int FOREST = 89;
    public static final int VINYL_SANDS = 88;

    private TektoLayers17() {}

    /** {@code WorldProviderTekto.createBiomeGenerators(seed)}. */
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

    private static final int[] BIOMES = {POLYVINYL_PLAINS, HALOGEN_HILLS, FOREST, VINYL_SANDS};

    /** {@code GenlayerTektoBiomes}: plains, hills, forest or vinyl sands at random. */
    static final class Biomes extends GenLayer17 {
        Biomes(long seed) {
            super(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] dest = new int[width * depth];
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    this.initChunkSeed(dx + x, dz + z);
                    dest[dx + dz * width] = BIOMES[this.nextInt(BIOMES.length)];
                }
            }
            return dest;
        }
    }

    /**
     * {@code GenLayerTektoRiverMix} (a {@code GenLayerRiverMix}): a river cell becomes Tetrachloride River except on
     * Polyvinyl Plains. Its other branches (a biome id 1.7 had not registered, mushroom islands) cannot happen with
     * Tekto's ids and are left out.
     */
    /** The same river compositor with explicit biome ids, used by configured biome-layer callers. */
    public static GenLayer17 riverMix(GenLayer17 biomes, GenLayer17 rivers, int river, int preservedBiome) {
        return new RiverMix(100L, biomes, rivers, river, preservedBiome);
    }

    static final class RiverMix extends GenLayerRiverMix17 {
        private final int river;
        private final int preservedBiome;

        RiverMix(long seed, GenLayer17 biomePatternGeneratorChain, GenLayer17 riverPatternGeneratorChain) {
            this(seed, biomePatternGeneratorChain, riverPatternGeneratorChain, TETRACHLORIC_RIVER, POLYVINYL_PLAINS);
        }

        RiverMix(long seed, GenLayer17 biomes, GenLayer17 rivers, int river, int preservedBiome) {
            super(seed, biomes, rivers);
            this.river = river;
            this.preservedBiome = preservedBiome;
        }

        @Override
        public int[] getInts(int x, int z, int width, int length) {
            int[] inputBiomeIds = this.biomes.getInts(x, z, width, length);
            int[] riverBiomeIds = this.rivers.getInts(x, z, width, length);
            int[] outputBiomeIds = new int[width * length];
            for (int i1 = 0; i1 < width * length; ++i1) {
                if (inputBiomeIds[i1] != preservedBiome && riverBiomeIds[i1] == GenLayerRiver17.VANILLA_RIVER) {
                    outputBiomeIds[i1] = river;
                } else {
                    outputBiomeIds[i1] = inputBiomeIds[i1];
                }
            }
            return outputBiomeIds;
        }
    }
}
