package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.gen.legacy.layer.GenLayer17;
import com.ntmspace.world.gen.legacy.layer.GenLayerSmooth17;
import com.ntmspace.world.gen.legacy.layer.GenLayerVoronoiZoom17;
import com.ntmspace.world.gen.legacy.layer.GenLayerZoom17;

/**
 * hbm-space's Laythe biome map: {@code WorldProviderLaythe.createBiomeGenerators} and its {@code dim/laythe/genlayer}
 * stages, ported line for line. Ids are hbm-space's {@code SpaceConfig} defaults.
 */
public final class LaytheLayers17 {

    public static final int ISLANDS = 97;
    public static final int OCEAN = 96;
    public static final int POLAR = 95;
    public static final int COAST = 94;

    private LaytheLayers17() {}

    /** {@code WorldProviderLaythe.createBiomeGenerators(seed)}. */
    public static BiomeLayers17 create(long seed) {
        GenLayer17 biomes = new Biomes(seed);
        biomes = new Polar(1000L, biomes);

        biomes = new GenLayerZoom17(1040L, biomes);
        biomes = new GenLayerZoom17(1000L, biomes); // GenLayerZoom.magnify(1000L, biomes, 1)
        biomes = new Polar(1300L, biomes);

        biomes = new Diversify(1000L, biomes);
        biomes = new Oceans(56000L, biomes);
        biomes = new Islands(200L, biomes);

        biomes = new GenLayerZoom17(1200L, biomes);
        biomes = new GenLayerZoom17(1001L, biomes);

        biomes = new Oceans(4000L, biomes);
        biomes = new Oceans(4001L, biomes, COAST);

        biomes = new GenLayerZoom17(1003L, biomes);
        biomes = new Oceans(4342L, biomes, COAST);
        biomes = new GenLayerSmooth17(700L, biomes);

        biomes = new GenLayerZoom17(1006L, biomes);

        GenLayer17 voronoi = new GenLayerVoronoiZoom17(10L, biomes);

        return new BiomeLayers17(biomes, voronoi, seed);
    }

    private static final int[] BIOMES = {ISLANDS, OCEAN, COAST};

    /** {@code GenLayerLaytheBiomes}: islands, ocean or coast at random. */
    static final class Biomes extends GenLayer17 {
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

    /** {@code GenLayerDiversifyLaythe}: half the cells become islands, ocean or coast at random. */
    static final class Diversify extends GenLayer17 {
        Diversify(long seed, GenLayer17 parent) {
            super(seed);
            this.parent = parent;
        }

        @Override
        public int[] getInts(int x, int z, int width, int height) {
            int[] input = this.parent.getInts(x, z, width, height);
            int[] output = new int[width * height];
            for (int zOut = 0; zOut < height; zOut++) {
                for (int xOut = 0; xOut < width; xOut++) {
                    int i = xOut + zOut * width;
                    int center = input[i];
                    initChunkSeed(xOut + x, zOut + z);
                    if (nextInt(2) == 0) {
                        output[i] = BIOMES[nextInt(BIOMES.length)];
                    } else output[i] = center;
                }
            }
            return output;
        }
    }

    /** {@code GenLayerLaythePolar}: an ocean cell whose four diagonal neighbours are ocean becomes polar. */
    static final class Polar extends GenLayer17 {
        Polar(long seed, GenLayer17 parent) {
            super(seed);
            this.parent = parent;
        }

        @Override
        public int[] getInts(int p_75904_1_, int p_75904_2_, int p_75904_3_, int p_75904_4_) {
            int i1 = p_75904_1_ - 1;
            int j1 = p_75904_2_ - 1;
            int k1 = p_75904_3_ + 2;
            int l1 = p_75904_4_ + 2;
            int[] aint = this.parent.getInts(i1, j1, k1, l1);
            int[] aint1 = new int[p_75904_3_ * p_75904_4_];

            for (int i2 = 0; i2 < p_75904_4_; ++i2) {
                for (int j2 = 0; j2 < p_75904_3_; ++j2) {
                    int k2 = aint[j2 + 0 + (i2 + 0) * k1];
                    int l2 = aint[j2 + 2 + (i2 + 0) * k1];
                    int i3 = aint[j2 + 0 + (i2 + 2) * k1];
                    int j3 = aint[j2 + 2 + (i2 + 2) * k1];
                    int k3 = aint[j2 + 1 + (i2 + 1) * k1];
                    this.initChunkSeed((long) (j2 + p_75904_1_), (long) (i2 + p_75904_2_));
                    if (k3 == OCEAN && k2 == OCEAN && l2 == OCEAN && i3 == OCEAN && j3 == OCEAN) {
                        aint1[j2 + i2 * p_75904_3_] = POLAR;
                    } else {
                        aint1[j2 + i2 * p_75904_3_] = k3;
                    }
                }
            }

            return aint1;
        }
    }

    /** {@code GenLayerLaytheIslands}: coast cells keep only next to islands (1 in 50), islands turn coast 1 in 3. */
    static final class Islands extends GenLayer17 {
        Islands(long seed, GenLayer17 parent) {
            super(seed);
            this.parent = parent;
        }

        @Override
        public int[] getInts(int p_151626_1_, int p_151626_2_, int p_151626_3_, int p_151626_4_) {
            int i = p_151626_1_ - 1;
            int j = p_151626_2_ - 1;
            int k = 1 + p_151626_3_ + 1;
            int l = 1 + p_151626_4_ + 1;
            int[] aint = this.parent.getInts(i, j, k, l);
            int[] aint1 = new int[p_151626_3_ * p_151626_4_];

            for (int i1 = 0; i1 < p_151626_4_; ++i1) {
                for (int j1 = 0; j1 < p_151626_3_; ++j1) {
                    this.initChunkSeed((long) (j1 + p_151626_1_), (long) (i1 + p_151626_2_));
                    int k1 = aint[j1 + 1 + (i1 + 1) * k];

                    if (k1 == COAST) {
                        int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
                        int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
                        int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
                        int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
                        boolean flag = l1 == ISLANDS || i2 == ISLANDS || j2 == ISLANDS || k2 == ISLANDS;

                        boolean islandThreshold = flag && this.nextInt(50) == 0;

                        if (!islandThreshold) k1 = OCEAN;
                    } else {
                        if (k1 == ISLANDS) k1 = this.nextInt(3) == 0 ? COAST : k1;
                    }

                    aint1[j1 + i1 * p_151626_3_] = k1;
                }
            }

            return aint1;
        }
    }

    /** {@code GenLayerLaytheOceans}: an ocean cell next to {@code targetBiome} (islands by default) becomes coast. */
    static final class Oceans extends GenLayer17 {
        private final int targetBiome;

        Oceans(long seed, GenLayer17 parent) {
            this(seed, parent, ISLANDS);
        }

        Oceans(long seed, GenLayer17 parent, int targetBiome) {
            super(seed);
            this.parent = parent;
            this.targetBiome = targetBiome;
        }

        @Override
        public int[] getInts(int p_151626_1_, int p_151626_2_, int p_151626_3_, int p_151626_4_) {
            int i = p_151626_1_ - 1;
            int j = p_151626_2_ - 1;
            int k = 1 + p_151626_3_ + 1;
            int l = 1 + p_151626_4_ + 1;
            int[] aint = this.parent.getInts(i, j, k, l);
            int[] aint1 = new int[p_151626_3_ * p_151626_4_];

            for (int i1 = 0; i1 < p_151626_4_; ++i1) {
                for (int j1 = 0; j1 < p_151626_3_; ++j1) {
                    this.initChunkSeed((long) (j1 + p_151626_1_), (long) (i1 + p_151626_2_));
                    int k1 = aint[j1 + 1 + (i1 + 1) * k];

                    if (k1 == OCEAN) {
                        int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
                        int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
                        int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
                        int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
                        boolean flag = l1 == targetBiome || i2 == targetBiome || j2 == targetBiome || k2 == targetBiome;
                        if (flag) k1 = COAST;
                    }

                    aint1[j1 + i1 * p_151626_3_] = k1;
                }
            }

            return aint1;
        }
    }
}
