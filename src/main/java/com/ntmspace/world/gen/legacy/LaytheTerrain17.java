package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderLaythe} with Laythe's four biomes ({@code dim/laythe/biome}), ported line for line:
 * stone under laythe silt with a sea of water below y 63, through the biome map of {@link LaytheLayers17}; then tilted
 * spires (of packed ice and snow when the chunk's origin column is polar, of stone and silt otherwise), Laythe root wood,
 * Greg caves, wide underwater ravines and desulfurized-oil bubbles, in that order.
 *
 * <p>The polar surface picked snow or packed ice with {@code Math.random()}. As in {@link EveTerrain17}, the port draws
 * that choice from a {@code Random} seeded per chunk from the session: the same chunk built twice in one session is the
 * same (1.7 built each chunk once), and the pattern is as random as 1.7's.
 */
public class LaytheTerrain17 extends LayeredBodyTerrain17 {

    /** {@code SolarSystem.Body.LAYTHE.ordinal()}: Laythe's ore metadata in hbm-space. */
    public static final byte LAYTHE_META = 9;
    /** hbm-space's {@code WorldConfig.laytheOilSpawn} default. */
    public static final int OIL_SPAWN = 100;

    private final MapGenGreg caveGenV3 = new MapGenGreg();
    private final MapGenRavine rgen = new MapGenRavine();
    private final MapGenRoots17 roots = new MapGenRoots17();

    private final MapGenTiltedSpires17 spires = new MapGenTiltedSpires17(2, 14, 0.75F);
    private final MapGenTiltedSpires17 snowires = new MapGenTiltedSpires17(2, 14, 0.75F);

    private final MapGenBubble oil = new MapGenBubble(OIL_SPAWN);

    private final long sessionSeed;
    /** Stands in for {@code Math.random()} in the polar surface pass; reseeded per chunk. */
    private final Random mathRandom = new Random();

    /** {@code laytheIsland}, "Laythe Islands". */
    public final Biome17 islands = new Laythe(0.256F, 0.05F);
    /** {@code laytheOcean}, "Sagan Sea". */
    public final Biome17 ocean = new Laythe(-1.8F, 0.24F);
    /** {@code laythePolar}, "Laythe Poles". */
    public final Biome17 polar = new Polar(-0.1F, 0.05F);
    /** {@code laytheCoast}, "Laythe Reef". */
    public final Biome17 coast = new Laythe(-0.4F, 0.01F);

    public LaytheTerrain17(long seed, Random session) {
        super(seed, LaytheLayers17::create);
        register(LaytheLayers17.ISLANDS, islands);
        register(LaytheLayers17.OCEAN, ocean);
        register(LaytheLayers17.POLAR, polar);
        register(LaytheLayers17.COAST, coast);
        // BiomeGenLaytheIslands overrides its base tint after construction.
        islands.waterColorMultiplier = 0x5B209A;
        this.sessionSeed = session.nextLong();

        rgen.yMin = 32;
        rgen.frequency = 20;
        rgen.strataFreq = 3;
        rgen.strataScale = 8.0F;
        rgen.width = 16.0D;
        rgen.taper = 8.0D;
        rgen.allowUnderwater = true;
        rgen.height = 5.0D;

        snowires.rock = Block17.PACKED_ICE;
        snowires.regolith = Block17.SNOW;
        spires.rock = Block17.STONE;
        spires.regolith = Block17.LAYTHE_SILT;

        spires.curve = snowires.curve = true;
        spires.maxPoint = snowires.maxPoint = 6.0F;
        spires.maxTilt = snowires.maxTilt = 3.5F;

        oil.block = Block17.ORE_OIL;
        oil.meta = LAYTHE_META;
        oil.replace = Block17.STONE;
        oil.setSize(8, 16);

        seaBlock = Block17.WATER;
        // seaLevel = 96;
    }

    @Override
    protected void prepareSurface(int x, int z) {
        mathRandom.setSeed(sessionSeed ^ ((long) x * 341873128712L + (long) z * 132897987541L));
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);
        oil.setMetas(buffer.metas);

        if (biomeAt(x * 16, z * 16) == polar) { // biomesForGeneration[0] == laythePolar
            snowires.generate(this, x, z, buffer.blocks);
        } else {
            spires.generate(this, x, z, buffer.blocks);
        }
        roots.generate(this, x, z, buffer.blocks);
        caveGenV3.generate(this, x, z, buffer.blocks);
        rgen.generate(this, x, z, buffer.blocks);
        oil.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /** hbm-space's {@code BiomeGenBaseLaythe}: vanilla's surface pass, but silt where vanilla laid gravel. */
    private static class Laythe extends Biome17 {

        Laythe(float rootHeight, float heightVariation) {
            this.temperature = 0.2F;
            this.rainfall = 0.2F;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
            this.topBlock = Block17.LAYTHE_SILT;
            this.waterColorMultiplier = PURPLE_WATER_MULTIPLIER;
            this.fillerBlock = Block17.LAYTHE_SILT;
        }

        // Same as vanilla but without the gravel
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
                        if (block2 == Block17.STONE) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.STONE;
                                } else if (l1 >= 59 && l1 <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    if (this.getFloatTemperature(x, l1, z) < 0.15F) {
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
                                    meta[i2] = b0;
                                } else if (l1 < 56 - l) {
                                    block = null;
                                    block1 = Block17.STONE;
                                    blocks[i2] = Block17.LAYTHE_SILT;
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

    /** hbm-space's {@code BiomeGenLaythePolar}: snow over ice; the first stone below y 65 becomes snow or packed ice. */
    private final class Polar extends Laythe {

        Polar(float rootHeight, float heightVariation) {
            super(rootHeight, heightVariation);
            this.topBlock = Block17.SNOW;
            // BiomeGenLaythePolar.
            this.waterColorMultiplier = 0xC1F4FF;
            this.fillerBlock = Block17.ICE;
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
                        if (block2 == Block17.STONE) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.STONE;
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

                                if (l1 >= 65) {
                                    blocks[i2] = block;
                                    meta[i2] = b0;
                                } else {
                                    // 1.7: else if (l1 < 65), so its final else (the filler) never ran
                                    block = null;
                                    block1 = Block17.STONE;
                                    if (mathRandom.nextDouble() > 0.4) { // Math.random()
                                        blocks[i2] = Block17.SNOW;
                                    } else {
                                        blocks[i2] = Block17.PACKED_ICE;
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
