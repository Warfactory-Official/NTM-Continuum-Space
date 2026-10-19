package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.noise.Noise17Octaves;
import com.ntmspace.world.gen.noise.Noise17Perlin;
import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderCelestial} terrain, ported line for line onto a 1.7 chunk primer: the 1.7 overworld
 * density field (three octave noises blended by a fourth, shaped by the biome height parameters and a depth noise),
 * filled with the body's stone and sea, then each column's biome surface. Each body subclasses it and adds its map
 * generators, as hbm-space's per-body chunk providers did.
 *
 * <p>One instance per level: like the 1.7 provider it keeps one {@code Random}, seeded from the world seed, which
 * built the noise generators and is reseeded per chunk before the surface pass.
 */
public abstract class CelestialTerrain17 implements Legacy17World {

    // Default block fills
    protected Block17 stoneBlock = Block17.STONE;
    protected Block17 seaBlock = null; // Doesn't have to be liquid, if you want like, basalt seas
    protected int seaLevel = 63;

    // Noise frequency
    protected double[] firstOrderFreq = {684.412D, 684.412D, 684.412D};
    protected double[] secondOrderFreq = {684.412D, 684.412D, 684.412D};
    protected double[] thirdOrderFreq = {8.555150000000001D, 4.277575000000001D, 8.555150000000001D};
    protected double[] heightOrderFreq = {200.0D, 200.0D, 0.5D};

    // Embiggenify
    protected boolean amplified = false;

    //round
    protected boolean reclamp = true;

    private final long seed;
    protected final Random rand;

    private final Noise17Octaves firstOrder;
    private final Noise17Octaves secondOrder;
    private final Noise17Octaves thirdOrder;
    private final Noise17Perlin perlin;
    private final Noise17Octaves heightOrder;

    private final double[] terrainBuffer = new double[825];
    private final float[] parabolicField = new float[25];
    private double[] stoneNoise = new double[256];

    private double[] firstOrderBuffer;
    private double[] secondOrderBuffer;
    private double[] thirdOrderBuffer;
    private double[] heightOrderBuffer;

    protected CelestialTerrain17(long seed) {
        this.seed = seed;
        this.rand = new Random(seed);
        this.firstOrder = new Noise17Octaves(this.rand, 16);
        this.secondOrder = new Noise17Octaves(this.rand, 16);
        this.thirdOrder = new Noise17Octaves(this.rand, 8);
        this.perlin = new Noise17Perlin(this.rand, 4);
        this.heightOrder = new Noise17Octaves(this.rand, 16);

        for (int j = -2; j <= 2; ++j) {
            for (int k = -2; k <= 2; ++k) {
                float f = 10.0F / Math17.sqrt_float((float) (j * j + k * k) + 0.2F);
                this.parabolicField[j + 2 + (k + 2) * 5] = f;
            }
        }
        // 1.7 then offered the generators to TerrainGen.getModdedNoiseGenerators, which hands them back unchanged
        // unless another mod replaces them.
    }

    @Override
    public long seed() {
        return seed;
    }

    /** A finished 1.7 chunk primer: 16 x 16 x 256 blocks, index {@code (x * 16 + z) * 256 + y}, and their metas. */
    public static final class Primer {
        public final Block17[] blocks = new Block17[65536];
        public final byte[] metas = new byte[65536];
    }

    /** 1.7 {@code provideChunk}'s part that builds the primer (biome bytes and skylight are the modern chunk's job). */
    public Primer provide(int x, int z) {
        rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
        return getChunkPrimer(x, z);
    }

    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = new Primer();
        generateBlocks(x, z, buffer.blocks);
        replaceBlocksForBiome(x, z, buffer.blocks, buffer.metas);
        return buffer;
    }

    // Fills in the chunk with stone, water, and air
    protected void generateBlocks(int x, int z, Block17[] blocks) {
        generateNoiseField(x * 4, 0, z * 4);

        for (int k = 0; k < 4; ++k) {
            int l = k * 5;
            int i1 = (k + 1) * 5;

            for (int j1 = 0; j1 < 4; ++j1) {
                int k1 = (l + j1) * 33;
                int l1 = (l + j1 + 1) * 33;
                int i2 = (i1 + j1) * 33;
                int j2 = (i1 + j1 + 1) * 33;

                for (int k2 = 0; k2 < 32; ++k2) {
                    double d0 = 0.125D;
                    double d1 = terrainBuffer[k1 + k2];
                    double d2 = terrainBuffer[l1 + k2];
                    double d3 = terrainBuffer[i2 + k2];
                    double d4 = terrainBuffer[j2 + k2];
                    double d5 = (terrainBuffer[k1 + k2 + 1] - d1) * d0;
                    double d6 = (terrainBuffer[l1 + k2 + 1] - d2) * d0;
                    double d7 = (terrainBuffer[i2 + k2 + 1] - d3) * d0;
                    double d8 = (terrainBuffer[j2 + k2 + 1] - d4) * d0;

                    for (int l2 = 0; l2 < 8; ++l2) {
                        double d9 = 0.25D;
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * d9;
                        double d13 = (d4 - d2) * d9;

                        for (int i3 = 0; i3 < 4; ++i3) {
                            int j3 = i3 + k * 4 << 12 | 0 + j1 * 4 << 8 | k2 * 8 + l2;
                            short short1 = 256;
                            j3 -= short1;
                            double d14 = 0.25D;
                            double d16 = (d11 - d10) * d14;
                            double d15 = d10 - d16;

                            for (int k3 = 0; k3 < 4; ++k3) {
                                if ((d15 += d16) > 0.0D) {
                                    blocks[j3 += short1] = stoneBlock;
                                } else if (k2 * 8 + l2 < seaLevel) {
                                    blocks[j3 += short1] = seaBlock;
                                } else {
                                    blocks[j3 += short1] = null;
                                }
                            }

                            d10 += d12;
                            d11 += d13;
                        }

                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    /** The biome a 1.7 generation-layer cell (4 x 4 blocks) holds; {@code biomeAt} of the cell's corner. */
    protected Biome17 generationBiome(int cellX, int cellZ) {
        return biomeAt(cellX * 4, cellZ * 4);
    }

    // Fills up a buffer with "chances for this block to be stone" using 3D noise and biome specific information
    protected void generateNoiseField(int x, int y, int z) {
        firstOrderBuffer = firstOrder.generate(firstOrderBuffer, x, y, z, 5, 33, 5, firstOrderFreq[0], firstOrderFreq[1], firstOrderFreq[2]);
        secondOrderBuffer = secondOrder.generate(secondOrderBuffer, x, y, z, 5, 33, 5, secondOrderFreq[0], secondOrderFreq[1], secondOrderFreq[2]);
        thirdOrderBuffer = thirdOrder.generate(thirdOrderBuffer, x, y, z, 5, 33, 5, thirdOrderFreq[0], thirdOrderFreq[1], thirdOrderFreq[2]);
        heightOrderBuffer = heightOrder.generate(heightOrderBuffer, x, z, 5, 5, heightOrderFreq[0], heightOrderFreq[1], heightOrderFreq[2]);

        int l = 0;
        int i1 = 0;

        for (int j1 = 0; j1 < 5; ++j1) {
            for (int k1 = 0; k1 < 5; ++k1) {
                float f = 0.0F;
                float f1 = 0.0F;
                float f2 = 0.0F;
                byte b0 = 2;
                // biomesForGeneration[j1 + 2 + (k1 + 2) * 10], a 10 x 10 cell window starting at (x - 2, z - 2)
                Biome17 biomegenbase = generationBiome(x - 2 + j1 + 2, z - 2 + k1 + 2);

                for (int l1 = -b0; l1 <= b0; ++l1) {
                    for (int i2 = -b0; i2 <= b0; ++i2) {
                        Biome17 biomegenbase1 = generationBiome(x - 2 + j1 + l1 + 2, z - 2 + k1 + i2 + 2);
                        float f3 = biomegenbase1.rootHeight;
                        float f4 = biomegenbase1.heightVariation;

                        if (amplified && f3 > 0.0F) {
                            f3 = 1.0F + f3 * 2.0F;
                            f4 = 1.0F + f4 * 4.0F;
                        }

                        float f5 = parabolicField[l1 + 2 + (i2 + 2) * 5] / (f3 + 2.0F);

                        if (biomegenbase1.rootHeight > biomegenbase.rootHeight) f5 /= 2.0F;

                        f += f4 * f5;
                        f1 += f3 * f5;
                        f2 += f5;
                    }
                }

                f /= f2;
                f1 /= f2;
                f = f * 0.9F + 0.1F;
                f1 = (f1 * 4.0F - 1.0F) / 8.0F;
                double d12 = heightOrderBuffer[i1] / 8000.0D;

                if (d12 < 0.0D) d12 = -d12 * 0.3D;

                d12 = d12 * 3.0D - 2.0D;

                if (d12 < 0.0D) {
                    d12 /= 2.0D;
                    if (d12 < -1.0D) d12 = -1.0D;
                    d12 /= 1.4D;
                    d12 /= 2.0D;
                } else {
                    if (d12 > 1.0D) d12 = 1.0D;
                    d12 /= 8.0D;
                }

                ++i1;
                double d13 = (double) f1;
                double d14 = (double) f;
                d13 += d12 * 0.2D;
                d13 = d13 * 8.5D / 8.0D;
                double d5 = 8.5D + d13 * 4.0D;

                for (int j2 = 0; j2 < 33; ++j2) {
                    double d6 = ((double) j2 - d5) * 12.0D * 128.0D / 256.0D / d14;

                    if (d6 < 0.0D) d6 *= 4.0D;

                    double d7 = firstOrderBuffer[l] / 512.0D;
                    double d8 = secondOrderBuffer[l] / 512.0D;
                    double d9 = (thirdOrderBuffer[l] / 10.0D + 1.0D) / 2.0D;
                    double d10 = reclamp ? Math17.denormalizeClamp(d7, d8, d9) - d6 : d8 - d6;
                    if (j2 > 29) {
                        double d11 = (double) ((float) (j2 - 29) / 3.0F);
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }

                    terrainBuffer[l] = d10;
                    ++l;
                }
            }
        }
    }

    protected void replaceBlocksForBiome(int x, int z, Block17[] blocks, byte[] metas) {
        double d0 = 0.03125D;
        stoneNoise = perlin.generate(stoneNoise, (double) (x * 16), (double) (z * 16), 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);

        for (int k = 0; k < 16; ++k) {
            for (int l = 0; l < 16; ++l) {
                // 1.7: biomes[l + k * 16] of loadBlockGeneratorData (index x + z * 16), i.e. the biome of column
                // (x * 16 + l, z * 16 + k), for the column (x * 16 + k, z * 16 + l): vanilla 1.7's transposition, kept.
                // It only shows on bodies with more than one biome.
                Biome17 biomegenbase = biomeAt(x * 16 + l, z * 16 + k);
                biomegenbase.genTerrainBlocks(rand, blocks, metas, x * 16 + k, z * 16 + l, stoneNoise[l + k * 16]);
            }
        }
    }
}
