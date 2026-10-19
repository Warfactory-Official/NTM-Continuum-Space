package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderThatmo} with {@code BiomeGenThatmo}, ported line for line: one biome everywhere
 * ({@code WorldChunkManagerHell}), basalt under snow with basalt "seas" below y 64, then Greg caves, ravines and small
 * craters, all three working in slaked sellafite.
 *
 * <p><b>Ported oddity:</b> the tunnel caves and the ravines dig only their stone block, slaked sellafite, and the biome's
 * snow: they leave the basalt Thatmo is made of alone and open only where the surface pass laid sellafite or snow (the
 * noise caves of {@link MapGenGreg} dig anything but bedrock, as everywhere).
 *
 * <p>The surface picked basalt or slaked sellafite with {@code Math.random()}; as in {@link EveTerrain17}, the port
 * draws that choice from a {@code Random} seeded per chunk from the session.
 */
public class ThatmoTerrain17 extends CelestialTerrain17 {

    private final MapGenGreg caveGenV3 = new MapGenGreg();
    private final MapGenRavine rgen = new MapGenRavine();
    private final MapGenCrater smallCrater = new MapGenCrater(6);

    private final long sessionSeed;
    /** Stands in for {@code Math.random()} in the surface pass; reseeded per chunk. */
    private final Random mathRandom = new Random();

    private final Biome17 biome = new BiomeThatmo();

    public ThatmoTerrain17(long seed, Random session) {
        super(seed);
        this.sessionSeed = session.nextLong();
        caveGenV3.stoneBlock = Block17.SELLAFIELD_SLAKED;
        rgen.stoneBlock = Block17.SELLAFIELD_SLAKED;

        smallCrater.setSize(8, 32);

        smallCrater.regolith = Block17.SELLAFIELD_SLAKED;
        smallCrater.rock = Block17.SELLAFIELD_SLAKED;

        stoneBlock = Block17.BASALT;
        seaBlock = Block17.BASALT;
        seaLevel = 64;
    }

    @Override
    public Biome17 biomeAt(int x, int z) {
        return biome;
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        mathRandom.setSeed(sessionSeed ^ ((long) x * 341873128712L + (long) z * 132897987541L));
        Primer buffer = super.getChunkPrimer(x, z);

        // NEW CAVES
        caveGenV3.generate(this, x, z, buffer.blocks);
        rgen.generate(this, x, z, buffer.blocks);
        smallCrater.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /** hbm-space's {@code BiomeGenThatmo}. */
    private final class BiomeThatmo extends Biome17 {

        BiomeThatmo() {
            this.temperature = -1.0F;
            this.rainfall = 0.0F;
            this.rootHeight = 0.125F;
            this.heightVariation = 0.05F;
            this.topBlock = Block17.SNOW;
            this.fillerBlock = Block17.SNOW;
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] meta, int x, int z, double noise) {
            Block17 block = this.topBlock;
            byte b0 = (byte) (this.topMeta & 255);
            Block17 block1 = this.fillerBlock;
            int k = -1;
            int l = (int) (noise / 6.0D + 6.0D + rand.nextDouble() * 0.85D);
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
                        if (block2 == Block17.BASALT) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.BASALT;
                                } else if (l1 >= 59 && l1 <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    // 1.7 checked getFloatTemperature(x, l1, z) < 0.15 here; both branches set the same
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (l1 >= 62) {
                                    blocks[i2] = block;
                                    meta[i2] = b0;
                                } else {
                                    // 1.7: else if (l1 < 62), so its final else (the filler) never ran
                                    block = null;
                                    block1 = Block17.BASALT;
                                    if (mathRandom.nextDouble() > 0.4) { // Math.random()
                                        blocks[i2] = Block17.BASALT;
                                    } else {
                                        blocks[i2] = Block17.SELLAFIELD_SLAKED;
                                    }
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
}
