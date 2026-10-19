package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderMoon} with {@code BiomeGenMoon}, ported line for line: one biome everywhere
 * ({@code WorldChunkManagerHell}), moon rock under moon turf, basalt seas below y 64, then Greg caves, ravines, small
 * and large craters, and brine bubbles, in that order.
 *
 * <p>{@code brineSpawn} is hbm-space's {@code WorldConfig.munBrineSpawn}; {@code munMeta} is the Mun's
 * {@code CelestialBody.getMeta}, the metadata of its ores.
 */
public class MunTerrain17 extends CelestialTerrain17 {

    /** {@code SolarSystem.Body.MUN.ordinal()}: the Mun's ore metadata in hbm-space (KERBIN, MUN, ...). */
    public static final byte MUN_META = 2;

    private final Biome17 biome = new BiomeMun();
    private final MapGenGreg caveGenV3 = new MapGenGreg();
    private final MapGenRavine rgen = new MapGenRavine();
    private final MapGenCrater smallCrater = new MapGenCrater(6);
    private final MapGenCrater largeCrater = new MapGenCrater(64);
    private final MapGenBubble brine;

    public MunTerrain17(long seed, int brineSpawn) {
        super(seed);
        brine = new MapGenBubble(brineSpawn);
        caveGenV3.stoneBlock = Block17.MOON_ROCK;
        rgen.stoneBlock = Block17.MOON_ROCK;

        smallCrater.setSize(8, 32);
        largeCrater.setSize(96, 128);

        smallCrater.regolith = largeCrater.regolith = Block17.BASALT;
        smallCrater.rock = largeCrater.rock = Block17.MOON_ROCK;

        brine.block = Block17.ORE_BRINE;
        brine.meta = MUN_META;
        brine.replace = Block17.MOON_ROCK;
        brine.setSize(8, 16);

        stoneBlock = Block17.MOON_ROCK;
        seaBlock = Block17.BASALT;
        seaLevel = 64;
    }

    @Override
    public Biome17 biomeAt(int x, int z) {
        return biome;
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);
        brine.setMetas(buffer.metas);

        // NEW CAVES
        caveGenV3.generate(this, x, z, buffer.blocks);
        rgen.generate(this, x, z, buffer.blocks);
        smallCrater.generate(this, x, z, buffer.blocks);
        largeCrater.generate(this, x, z, buffer.blocks);
        brine.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /** hbm-space's {@code BiomeGenMoon}. */
    public static final class BiomeMun extends Biome17 {

        BiomeMun() {
            this.temperature = -1.0F;
            this.rainfall = 0.0F;
            this.rootHeight = 0.125F;
            this.heightVariation = 0.05F;
            this.topBlock = Block17.MOON_TURF;
            this.fillerBlock = Block17.MOON_ROCK;
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] meta, int x, int z, double noise) {
            Block17 block = this.topBlock;
            byte b0 = (byte) (this.topMeta & 255);
            Block17 block1 = this.fillerBlock;
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
                        if (block2 == Block17.MOON_ROCK) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.MOON_ROCK;
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
                                } else if (l1 < 56 - l) {
                                    block = null;
                                    block1 = Block17.MOON_ROCK;
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
}
