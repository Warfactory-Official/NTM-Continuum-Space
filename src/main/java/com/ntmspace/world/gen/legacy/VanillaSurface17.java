package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * Minecraft 1.7.10's {@code BiomeGenBase.genBiomeTerrain}, the default surface pass of a biome that does not override
 * {@code genTerrainBlocks} (checked against the 1.7.10 jar's bytecode). It only reshapes vanilla stone, so on a body of
 * other rock it lays the bedrock and draws from the chunk {@code Random} exactly as 1.7 did, and nothing else: that is
 * what hbm-space's Minmus basins and Moho's vanilla rivers got.
 */
public final class VanillaSurface17 {

    private VanillaSurface17() {}

    public static void genBiomeTerrain(Biome17 biome, Random rand, Block17[] blocks, byte[] metas, int x, int z, double noise) {
        Block17 block = biome.topBlock;
        byte b0 = (byte) (biome.topMeta & 255);
        Block17 block1 = biome.fillerBlock;
        int k = -1;
        int l = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int i1 = x & 15;
        int j1 = z & 15;
        int k1 = blocks.length / 256;

        for (int l1 = 255; l1 >= 0; --l1) {
            int i2 = (j1 * 16 + i1) * k1 + l1;

            if (l1 <= 0 + rand.nextInt(5)) {
                blocks[i2] = Block17.BEDROCK;
            } else {
                Block17 block2 = blocks[i2];

                if (block2 != null) {
                    if (block2 == Block17.STONE) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                b0 = 0;
                                block1 = Block17.STONE;
                            } else if (l1 >= 59 && l1 <= 64) {
                                block = biome.topBlock;
                                b0 = (byte) (biome.topMeta & 255);
                                block1 = biome.fillerBlock;
                            }

                            if (l1 < 63 && block == null) {
                                if (biome.getFloatTemperature(x, l1, z) < 0.15F) {
                                    block = Block17.ICE;
                                    b0 = 0;
                                } else {
                                    block = Block17.WATER;
                                    b0 = 0;
                                }
                            }

                            k = l;

                            if (l1 >= 62) {
                                blocks[i2] = block;
                                metas[i2] = b0;
                            } else if (l1 < 56 - l) {
                                block = null;
                                block1 = Block17.STONE;
                                blocks[i2] = Block17.GRAVEL;
                            } else {
                                blocks[i2] = block1;
                            }
                        } else if (k > 0) {
                            --k;
                            blocks[i2] = block1;

                            if (k == 0 && block1 == Block17.SAND) {
                                k = rand.nextInt(4) + Math.max(0, l1 - 63);
                                block1 = Block17.SANDSTONE;
                            }
                        }
                    }
                } else {
                    k = -1;
                }
            }
        }
    }
}
