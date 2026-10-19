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
 * Duna's biome map: hbm-space's {@code WorldProviderDuna.createBiomeGenerators} with {@code GenLayerDunaBiomes},
 * {@code GenLayerDiversifyDuna} and {@code GenLayerDunaLowlands}. Ids are hbm-space's {@code SpaceConfig} defaults.
 * Checked value for value against the 1.7.10 jar's vanilla stages (scratchpad {@code m3did/oracle/LayerOracle}).
 *
 * <p><b>Ported oddities:</b> Duna mixes in vanilla rivers ({@code GenLayerRiverMix}), so the terrain noise sees
 * vanilla's {@code river} (id 7, height -0.5) where the zoomed biome map changes id parity; the detail chain ignores the
 * rivers, so no column's surface is a river. The river chain zooms the biome map once more before {@code GenLayerRiver}
 * and once after, so the rivers are four times finer than the biome cells they cut. {@code GenLayerDunaHills} and
 * {@code GenLayerDunaPolarHills} exist in hbm-space but are in no stack; they are not ported.
 */
public final class DunaLayers17 {

    public static final int PLAINS = 122;
    public static final int LOWLANDS = 121;
    public static final int POLAR = 120;
    public static final int HILLS = 119;
    public static final int POLAR_HILLS = 118;

    /** {@code GenLayerDunaBiomes.biomes} and {@code GenLayerDiversifyDuna.biomes}, in their order. */
    static final int[] BIOMES = {PLAINS, LOWLANDS, POLAR, HILLS, POLAR_HILLS};

    private DunaLayers17() {}

    public static BiomeLayers17 create(long seed) {
        GenLayer17 biomes = new Pick(seed, BIOMES);

        biomes = new GenLayerFuzzyZoom17(2000L, biomes);
        biomes = new GenLayerZoom17(2001L, biomes);
        biomes = new GenLayerDiversify17(1000L, biomes, 2, BIOMES);
        biomes = new GenLayerZoom17(1000L, biomes);
        biomes = new GenLayerDiversify17(1001L, biomes, 2, BIOMES);
        biomes = new GenLayerZoom17(1001L, biomes);
        biomes = new Spread(1300L, biomes, LOWLANDS, PLAINS);
        biomes = new GenLayerZoom17(1003L, biomes);
        biomes = new GenLayerSmooth17(700L, biomes);
        biomes = new GenLayerZoom17(1005L, biomes);
        biomes = new GenLayerSmooth17(703L, biomes);
        biomes = new GenLayerFuzzyZoom17(1000L, biomes);
        biomes = new GenLayerSmooth17(705L, biomes);
        biomes = new GenLayerFuzzyZoom17(1001L, biomes);
        biomes = new GenLayerZoom17(1006L, biomes);

        GenLayer17 genlayerVoronoiZoom = new GenLayerVoronoiZoom17(10L, biomes);

        GenLayer17 genlayerRiverZoom = new GenLayerZoom17(1000L, biomes);
        GenLayer17 genlayerRiver = new GenLayerRiver17(1004L, genlayerRiverZoom); // Your custom river layer
        genlayerRiver = new GenLayerZoom17(105L, genlayerRiver);

        GenLayer17 genlayerRiverMix = new GenLayerRiverMix17(100L, biomes, genlayerRiver);

        return new BiomeLayers17(genlayerRiverMix, genlayerVoronoiZoom, seed);
    }

    /**
     * {@code GenLayerDunaBiomes} / {@code GenLayerDresBiomes}: the root stage, one of {@code biomes} per cell, chosen with
     * the cell's chunk random. The root's layer seed is the world seed itself, as in 1.7.
     */
    static final class Pick extends GenLayer17 {

        private final int[] biomes;

        Pick(long seed, int... biomes) {
            super(seed);
            this.biomes = biomes;
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] dest = new int[width * depth];
            for (int k = 0; k < depth; ++k) {
                for (int i = 0; i < width; ++i) {
                    initChunkSeed(x + i, z + k);
                    dest[i + k * width] = biomes[nextInt(biomes.length)];
                }
            }
            return dest;
        }
    }

    /**
     * {@code GenLayerDunaLowlands} / {@code GenLayerDresBasins} / {@code GenLayerDresPlains}: "a {@code target} cell next
     * to a {@code neighbour} cell becomes {@code target}".
     *
     * <p><b>Ported oddity:</b> the cell already is {@code target}, so the stage changes nothing; it reads its parent one
     * cell wider on every side and seeds (but never draws from) the chunk random. 1.7 ran it, so it runs here.
     */
    static final class Spread extends GenLayer17 {

        private final int target;
        private final int neighbour;

        Spread(long seed, GenLayer17 parent, int target, int neighbour) {
            super(seed);
            this.parent = parent;
            this.target = target;
            this.neighbour = neighbour;
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

                    if (k1 == target) {
                        int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
                        int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
                        int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
                        int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
                        boolean flag = l1 == neighbour || i2 == neighbour || j2 == neighbour || k2 == neighbour;
                        if (flag) k1 = target;
                    }

                    aint1[j1 + i1 * areaWidth] = k1;
                }
            }
            return aint1;
        }
    }
}
