package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.noise.BobMath;
import com.ntmspace.world.gen.noise.Noise17Octaves;
import com.ntmspace.world.gen.noise.NoiseCaveGenerator;

/**
 * hbm-space's {@code MapGenGreg} (com.hbm.dim.mapgen), ported line for line: 1.7 tunnel caves plus "noise caves", a
 * cave density field built from the same noise pipeline as the terrain.
 *
 * <p><b>Ported oddity:</b> the noise generators are created from this generator's own {@code Random} the first time
 * it runs for a world, BEFORE that Random is seeded from the world seed. In 1.7 that Random was {@code new Random()},
 * so the noise caves differed from one game session to the next and could tear at the border between chunks generated
 * in different sessions. That is what hbm-space did; it is kept.
 */
public class MapGenGreg extends Caves17 {

    public Block17 stoneBlock = Block17.STONE;

    private double[] caveNoise;
    private float[] biomeWeightTable;
    private NoiseCaveGenerator noiseCaves;
    public Noise17Octaves noiseGen6;
    private Noise17Octaves lowerNoise;
    private Noise17Octaves upperNoise;
    private Noise17Octaves interpolationNoise;
    private double[] interpolationNoises;
    private double[] lowerInterpolatedNoises;
    private double[] upperInterpolatedNoises;
    private double[] depthNoises;

    @Override
    public void generate(Legacy17World w, int chunkX, int chunkZ, Block17[] blocks) {
        if (world != w) {
            this.caveNoise = new double[825];
            this.biomeWeightTable = new float[25];
            this.lowerNoise = new Noise17Octaves(this.rand, 16);
            this.upperNoise = new Noise17Octaves(this.rand, 16);
            this.interpolationNoise = new Noise17Octaves(this.rand, 8);
            this.noiseGen6 = new Noise17Octaves(this.rand, 16);
            this.noiseCaves = new NoiseCaveGenerator(this.rand);
            for (int j = -2; j <= 2; ++j) {
                for (int k = -2; k <= 2; ++k) {
                    float f = 10.0F / Math17.sqrt_float((float) (j * j + k * k) + 0.2F);
                    this.biomeWeightTable[j + 2 + (k + 2) * 5] = f;
                }
            }
        }
        this.world = w;
        this.rand.setSeed(w.seed());
        int k = this.range;
        long l = this.rand.nextLong();
        long i1 = this.rand.nextLong();
        for (int j1 = chunkX - k; j1 <= chunkX + k; ++j1) {
            for (int k1 = chunkZ - k; k1 <= chunkZ + k; ++k1) {
                long l1 = (long) j1 * l;
                long i2 = (long) k1 * i1;
                this.rand.setSeed(l1 ^ i2 ^ w.seed());
                this.recursiveGenerate(j1, k1, chunkX, chunkZ, blocks);
            }
        }
        this.generateNoiseCaves(chunkX, chunkZ, blocks);
    }

    private void generateNoiseCaves(int chunkX, int chunkZ, Block17[] blocks) {
        generateNoiseCavesNoise(chunkX, chunkZ);

        for (int noiseX = 0; noiseX < 4; ++noiseX) {
            int ix0 = noiseX * 5;
            int ix1 = (noiseX + 1) * 5;

            for (int noiseZ = 0; noiseZ < 4; ++noiseZ) {
                int ix0z0 = (ix0 + noiseZ) * 33;
                int ix0z1 = (ix0 + noiseZ + 1) * 33;
                int ix1z0 = (ix1 + noiseZ) * 33;
                int ix1z1 = (ix1 + noiseZ + 1) * 33;

                for (int noiseY = 0; noiseY < 32; ++noiseY) {
                    double x0z0 = this.caveNoise[ix0z0 + noiseY];
                    double x0z1 = this.caveNoise[ix0z1 + noiseY];
                    double x1z0 = this.caveNoise[ix1z0 + noiseY];
                    double x1z1 = this.caveNoise[ix1z1 + noiseY];
                    double x0z0Add = (this.caveNoise[ix0z0 + noiseY + 1] - x0z0) * 0.125D;
                    double x0z1Add = (this.caveNoise[ix0z1 + noiseY + 1] - x0z1) * 0.125D;
                    double x1z0Add = (this.caveNoise[ix1z0 + noiseY + 1] - x1z0) * 0.125D;
                    double x1z1Add = (this.caveNoise[ix1z1 + noiseY + 1] - x1z1) * 0.125D;

                    for (int pieceY = 0; pieceY < 8; ++pieceY) {
                        double z0 = x0z0;
                        double z1 = x0z1;
                        double z0Add = (x1z0 - x0z0) * 0.25D;
                        double z1Add = (x1z1 - x0z1) * 0.25D;

                        for (int pieceX = 0; pieceX < 4; ++pieceX) {
                            int index = pieceX + noiseX * 4 << 12 | noiseZ * 4 << 8 | noiseY * 8 + pieceY;
                            short idAdd = 256;
                            index -= idAdd;
                            double densityAdd = (z1 - z0) * 0.25D;
                            double density = z0 - densityAdd;

                            for (int pieceZ = 0; pieceZ < 4; ++pieceZ) {
                                index += idAdd;
                                if ((density += densityAdd) < 0) {
                                    int y = noiseY * 8 + pieceY;
                                    if (y > 0) {
                                        if (blocks[index] != Block17.BEDROCK) blocks[index] = null;
                                    }
                                }
                            }

                            z0 += z0Add;
                            z1 += z1Add;
                        }

                        x0z0 += x0z0Add;
                        x0z1 += x0z1Add;
                        x1z0 += x1z0Add;
                        x1z1 += x1z1Add;
                    }
                }
            }
        }
    }

    private void generateNoiseCavesNoise(int chunkX, int chunkZ) {
        int cx = chunkX * 4, cz = chunkZ * 4;
        this.depthNoises = this.noiseGen6.generate(this.depthNoises, cx, cz, 5, 5, 200.0D, 200.0D, 0.5D);
        this.interpolationNoises = this.interpolationNoise.generate(this.interpolationNoises, cx, 0, cz, 5, 33, 5,
                8.555150000000001D, 4.277575000000001D, 8.555150000000001D);
        this.lowerInterpolatedNoises = this.lowerNoise.generate(this.lowerInterpolatedNoises, cx, 0, cz, 5, 33, 5,
                684.412D, 684.412D, 684.412D);
        this.upperInterpolatedNoises = this.upperNoise.generate(this.upperInterpolatedNoises, cx, 0, cz, 5, 33, 5,
                684.412D, 684.412D, 684.412D);
        int i = 0, j = 0;

        for (int x = 0; x < 5; ++x) {
            for (int z = 0; z < 5; ++z) {
                float scale = 0.0F;
                float depth = 0.0F;
                float weight = 0.0F;
                double lowestScaledDepth = 0;

                Biome17 biome0 = generationBiome(cx - 2 + x + 2, cz - 2 + z + 2);
                // We iterate the entire area to ensure we're not anywhere even near an ocean
                for (int x1 = -2; x1 <= 2; ++x1) {
                    for (int z1 = -2; z1 <= 2; ++z1) {
                        Biome17 biome = generationBiome(cx - 2 + x + x1 + 2, cz - 2 + z + z1 + 2);
                        float depthHere = biome.rootHeight;
                        float scaleHere = biome.heightVariation;

                        float weightHere = this.biomeWeightTable[x1 + 2 + (z1 + 2) * 5] / (depthHere + 2.0F);

                        if (biome.rootHeight > biome0.rootHeight) weightHere /= 2.0F;

                        scale += scaleHere * weightHere;
                        depth += depthHere * weightHere;
                        weight += weightHere;
                        // Disable in oceans
                        lowestScaledDepth = Math.min(lowestScaledDepth, biome.rootHeight);
                    }
                }
                scale /= weight;
                depth /= weight;
                scale = scale * 0.9F + 0.1F;
                depth = (depth * 4.0F - 1.0F) / 8.0F;
                double depthNoise = this.depthNoises[j] / 8000;

                if (depthNoise < 0.0D) depthNoise = -depthNoise * 0.3D;

                depthNoise = depthNoise * 3.0D - 2.0D;

                if (depthNoise < 0.0D) {
                    depthNoise /= 2.0D;
                    if (depthNoise < -1.0D) depthNoise = -1.0D;
                    depthNoise /= 1.4D;
                    depthNoise /= 2.0D;
                } else {
                    if (depthNoise > 1.0D) depthNoise = 1.0D;
                    depthNoise /= 8.0D;
                }

                ++j;
                double scaledDepth = depth;
                double scaledScale = scale;
                scaledDepth += depthNoise * 0.2D;
                scaledDepth = scaledDepth * 8.5D / 8.0D;
                double terrainHeight = 8.5D + scaledDepth * 4.0D;

                // Each unit of depth roughly corresponds to 16 blocks, but we use 20 for good measure
                // We start reduction at 56 instead of 64, the sea level, to give ourselves some more room.
                double startLevel = 56 + (lowestScaledDepth * 20);
                int sub = (int) (startLevel / 8);

                for (int y = 0; y < 33; y++) {
                    double falloff = ((double) y - terrainHeight) * 12.0D * 128.0D / 256.0D / scaledScale;

                    if (falloff < 0.0D) falloff *= 4.0D;

                    double lower = this.lowerInterpolatedNoises[i] / 512.0D;
                    double upper = this.upperInterpolatedNoises[i] / 512.0D;
                    double interpolation = (this.interpolationNoises[i] / 10.0D + 1.0D) / 2.0D;
                    double noise = Math17.denormalizeClamp(lower, upper, interpolation) - falloff;

                    // Scale down the last 3 layers
                    if (y > 29) {
                        double lerp = (float) (y - 29) / 3.0F;
                        noise = noise * (1.0D - lerp) + -10.0D * lerp;
                    }

                    double caveSample = this.noiseCaves.sample(noise, y * 8, chunkZ * 16 + (z * 4), chunkX * 16 + (x * 4));

                    // Reduce so we don't break the surface
                    caveSample = BobMath.clampedLerp(caveSample, (lowestScaledDepth * -30) + 20, (y - sub + 2) / 2.0);

                    this.caveNoise[i] = caveSample;
                    i++;
                }
            }
        }
    }

    /**
     * 1.7 {@code getBiomesForGeneration(cx - 2, cz - 2, 10, 10)}: the coarse (4 x 4 block cell) biome layer, the same
     * window the terrain noise read for this chunk; one biome everywhere on a body without layers.
     */
    private Biome17 generationBiome(int cellX, int cellZ) {
        return world instanceof CelestialTerrain17 terrain ? terrain.generationBiome(cellX, cellZ) : world.biomeAt(cellX * 4, cellZ * 4);
    }

    @Override
    protected void digBlock(Block17[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        Biome17 biome = world.biomeAt(x + chunkX * 16, z + chunkZ * 16);
        Block17 top = biome.topBlock;
        Block17 filler = biome.fillerBlock;
        Block17 block = data[index];

        if (block == stoneBlock || block == filler || block == top) {
            if (y < 2) {
                data[index] = Block17.FLOWING_LAVA;
            } else {
                data[index] = null;
                if (foundTop && data[index - 1] == filler) data[index - 1] = top;
            }
        }
    }
}
