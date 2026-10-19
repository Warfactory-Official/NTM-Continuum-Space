package com.ntmspace.world.gen.legacy.layer;

/**
 * hbm-space's {@code GenLayerDiversify} ({@code com.hbm.dim}): one cell in {@code chance} becomes one of
 * {@code biomes} (1.7 biome ids), the rest keep the parent's id.
 */
public class GenLayerDiversify17 extends GenLayer17 {

    private final int[] biomes;
    private final int chance;

    public GenLayerDiversify17(long seed, GenLayer17 parent, int chance, int... biomes) {
        super(seed);
        this.parent = parent;
        this.biomes = biomes;
        this.chance = chance;
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
                if (nextInt(chance) == 0) {
                    output[i] = biomes[nextInt(biomes.length)];
                } else {
                    output[i] = center;
                }
            }
        }
        return output;
    }
}
