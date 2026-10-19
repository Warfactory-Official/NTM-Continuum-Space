package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderMinmus} with Minmus's two biomes ({@code dim/minmus/biome}), ported line for line:
 * minmus stone under regolith, its basins flooded with smooth minmus rock up to y 63 (the "sea"), through the biome
 * map of {@link MinmusLayers17}; then caves (smooth rock instead of lava below y 10), small and large craters and
 * brine bubbles, in that order.
 *
 * <p><b>Ported oddity:</b> the basins keep vanilla's surface pass, which only reshapes vanilla stone, so they get bedrock
 * and nothing else; their floor is the smooth-rock sea and the bare minmus stone under it.
 */
public class MinmusTerrain17 extends LayeredBodyTerrain17 {

    /** {@code SolarSystem.Body.MINMUS.ordinal()}: Minmus's ore metadata in hbm-space. */
    public static final byte MINMUS_META = 3;
    /** hbm-space's {@code WorldConfig.minmusBrineSpawn} default. */
    public static final int BRINE_SPAWN = 100;

    private final Caves17 caveGenerator = new VanillaCaves17(Block17.MINMUS_STONE).withLava(Block17.MINMUS_SMOOTH);

    private final MapGenCrater smallCrater = new MapGenCrater(5);
    private final MapGenCrater largeCrater = new MapGenCrater(96);

    private final MapGenBubble brine = new MapGenBubble(BRINE_SPAWN);

    public final Biome17 hills = new Hills();
    public final Biome17 basins = new Basins();

    public MinmusTerrain17(long seed) {
        super(seed, MinmusLayers17::create);
        register(MinmusLayers17.HILLS, hills);
        register(MinmusLayers17.BASINS, basins);

        smallCrater.setSize(6, 24);
        largeCrater.setSize(64, 128);

        smallCrater.regolith = largeCrater.regolith = Block17.MINMUS_REGOLITH;
        smallCrater.rock = largeCrater.rock = Block17.MINMUS_STONE;

        brine.block = Block17.ORE_BRINE;
        brine.meta = MINMUS_META;
        brine.replace = Block17.MINMUS_STONE;
        brine.setSize(8, 16);

        stoneBlock = Block17.MINMUS_STONE;
        seaBlock = Block17.MINMUS_SMOOTH;
        seaLevel = 63;
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);
        brine.setMetas(buffer.metas);

        caveGenerator.generate(this, x, z, buffer.blocks);
        smallCrater.generate(this, x, z, buffer.blocks);
        largeCrater.generate(this, x, z, buffer.blocks);
        brine.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /** hbm-space's {@code BiomeGenBaseMinmus}. */
    private abstract static class Minmus extends Biome17 {

        Minmus(float rootHeight, float heightVariation) {
            this.temperature = -1.0F;
            this.rainfall = 0.0F;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
            this.topBlock = Block17.MINMUS_REGOLITH; //remind me to send roadhog his daily modmail
            this.fillerBlock = Block17.MINMUS_REGOLITH;
        }
    }

    /** hbm-space's {@code BiomeGenMinmusBasin} ("Minmus Basins"): smooth rock on top, vanilla's surface pass. */
    private static final class Basins extends Minmus {

        Basins() {
            super(-1.0F, 0.02F);
            this.topBlock = Block17.MINMUS_SMOOTH;
            this.fillerBlock = Block17.MINMUS_REGOLITH;
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] metas, int x, int z, double noise) {
            VanillaSurface17.genBiomeTerrain(this, rand, blocks, metas, x, z, noise);
        }
    }

    /** hbm-space's {@code BiomeGenMinmusHills} ("Minmus Hills"). */
    private static final class Hills extends Minmus {

        Hills() {
            super(0.325F, 0.08F);
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
                        if (block2 == Block17.MINMUS_STONE) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.MINMUS_STONE;
                                } else if (l1 >= 59 && l1 <= 74) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    // 1.7 checked getFloatTemperature < 0.15 here; both branches set the same
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (l1 >= 62) {
                                    blocks[i2] = block;
                                    meta[i2] = b0;
                                } else if (l1 < 56 - l) {
                                    block = null;
                                    block1 = Block17.MINMUS_STONE;
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
