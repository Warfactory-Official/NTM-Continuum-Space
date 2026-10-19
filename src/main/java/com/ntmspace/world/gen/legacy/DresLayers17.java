package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.gen.legacy.layer.GenLayer17;
import com.ntmspace.world.gen.legacy.layer.GenLayerDiversify17;
import com.ntmspace.world.gen.legacy.layer.GenLayerFuzzyZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerSmooth17;
import com.ntmspace.world.gen.legacy.layer.GenLayerVoronoiZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerZoom17;

/**
 * Dres's biome map: hbm-space's {@code WorldProviderDres.createBiomeGenerators} with {@code GenLayerDresBiomes},
 * {@code GenLayerDiversifyDres}, {@code GenLayerDresBasins} and {@code GenLayerDresPlains} (both no-ops, see
 * {@link DunaLayers17.Spread}). Ids are hbm-space's {@code SpaceConfig} defaults. No rivers: the generation chain is the
 * biome chain itself. Checked value for value against the 1.7.10 jar's vanilla stages (scratchpad
 * {@code m3did/oracle/LayerOracle}).
 */
public final class DresLayers17 {

    public static final int PLAINS = 112;
    /** {@code dresCanyon}, "Dres Large Basins" ({@code SpaceConfig.dresBasinBiome}). */
    public static final int CANYON = 111;

    /** {@code GenLayerDresBiomes.biomes} and {@code GenLayerDiversifyDres.biomes}, in their order. */
    static final int[] BIOMES = {CANYON, PLAINS};

    private DresLayers17() {}

    public static BiomeLayers17 create(long seed) {
        GenLayer17 biomes = new DunaLayers17.Pick(seed, BIOMES);

        biomes = new GenLayerFuzzyZoom17(2000L, biomes);
        biomes = new GenLayerZoom17(2001L, biomes);
        biomes = new GenLayerDiversify17(1000L, biomes, 2, BIOMES);
        biomes = new GenLayerZoom17(1000L, biomes);
        biomes = new GenLayerDiversify17(1001L, biomes, 2, BIOMES);
        biomes = new GenLayerZoom17(1001L, biomes);
        biomes = new DunaLayers17.Spread(3000L, biomes, CANYON, PLAINS);
        biomes = new GenLayerZoom17(1003L, biomes);
        biomes = new GenLayerSmooth17(700L, biomes);
        biomes = new DunaLayers17.Spread(200L, biomes, PLAINS, CANYON);

        biomes = new GenLayerZoom17(1006L, biomes);

        GenLayer17 genLayerVeronoiZoom = new GenLayerVoronoiZoom17(10L, biomes);

        return new BiomeLayers17(biomes, genLayerVeronoiZoom, seed);
    }
}
