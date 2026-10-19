package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.noise.Noise17Perlin;
import java.util.Arrays;
import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderDuna} with Duna's five biomes ({@code dim/duna/biome}), ported line for line: Duna rock
 * through the biome map of {@link DunaLayers17}; the hills' stepped plateaus ({@link MapGenPlateau17}) before the surface
 * pass, then basalt-floored caves under the lowlands and small caves elsewhere ({@link ExperimentalCaves17}), and oil
 * bubbles ({@link MapGenBubble}, a hundredth of the chunks).
 *
 * <p><b>Ported oddities:</b> 1.7 built the plateau noise from the unseeded {@code world.rand}, and the plains, polar and
 * polar-hills surfaces choose between two blocks with {@code Math.random()}; both draw from the session {@code Random}
 * the port is given, so (as in 1.7) the hills' plateau steps and those block choices change between game sessions while
 * everything else follows the world seed. The hills surface is vanilla's mesa surface with its clay band table built from
 * the world seed; its band lookup reads the noise at {@code (x, x)}, a 1.7 slip, kept.
 */
public class DunaTerrain17 extends LayeredBodyTerrain17 {

    /** hbm-space's {@code WorldConfig.dunaOilSpawn} default. */
    public static final int OIL_SPAWN = 100;

    private final Random session;

    private final ExperimentalCaves17 caveGenSmall = new ExperimentalCaves17(2, 12, 0.12F);
    private final ExperimentalCaves17 caveGenV2 = new ExperimentalCaves17(2, 40, 3.0F);
    private final MapGenPlateau17 genPlateau;
    private final MapGenBubble oil = new MapGenBubble(OIL_SPAWN);

    public final Biome17 plains = new Plains();
    public final Biome17 lowlands = new Lowlands();
    public final Biome17 polar = new Polar(0.425F, 0.05F);
    public final Biome17 hills = new Hills();
    public final Biome17 polarHills = new Polar(0.725F, 0.8F);
    /** Vanilla's {@code river} (id 7), which Duna's river mix lays into the terrain noise's biome map. */
    public final Biome17 vanillaRiver = new VanillaRiver();

    public DunaTerrain17(long seed, Random session) {
        super(seed, DunaLayers17::create);
        this.session = session;
        register(DunaLayers17.PLAINS, plains);
        register(DunaLayers17.LOWLANDS, lowlands);
        register(DunaLayers17.POLAR, polar);
        register(DunaLayers17.HILLS, hills);
        register(DunaLayers17.POLAR_HILLS, polarHills);
        register(com.ntmspace.world.gen.legacy.layer.GenLayerRiver17.VANILLA_RIVER, vanillaRiver);

        genPlateau = new MapGenPlateau17(session);

        stoneBlock = Block17.DUNA_ROCK;

        caveGenV2.lavaBlock = Block17.BASALT;
        caveGenV2.stoneBlock = DunaIkeDresBlocks17.DUNA_COBBLE;
        caveGenSmall.lavaBlock = Block17.DUNA_SANDS;
        caveGenSmall.stoneBlock = Block17.DUNA_ROCK;

        caveGenSmall.smallCaveSize = 0.1F;

        caveGenV2.onlyBiome = lowlands;
        caveGenSmall.ignoreBiome = lowlands;

        genPlateau.surfrock = Block17.DUNA_SANDS;
        genPlateau.stoneBlock = Block17.DUNA_ROCK;
        genPlateau.fillblock = Block17.DUNA_SANDS;
        genPlateau.applyToBiome = hills;

        oil.block = Block17.ORE_OIL;
        oil.meta = BodyOres17.DUNA;
        oil.replace = Block17.DUNA_ROCK;
        oil.setSize(8, 16);
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        // Instead of calling super, we want to do some generation before _and_ after block replacement, for shaping!
        Primer buffer = new Primer();
        generateBlocks(x, z, buffer.blocks);
        int[] biomesForGeneration = chunkBiomeIds(x, z);

        boolean hasLowlands = false;
        boolean hasNotLowlands = false;
        boolean hasPlateau = false;

        for (int i = 0; i < biomesForGeneration.length; i++) {
            if (biomesForGeneration[i] == DunaLayers17.LOWLANDS) hasLowlands = true; else hasNotLowlands = true;
            if (biomesForGeneration[i] == DunaLayers17.HILLS) hasPlateau = true;
            if (hasLowlands && hasNotLowlands && hasPlateau) break;
        }

        // Pre-biome blocks
        if (hasPlateau) genPlateau.generate(this, x, z, buffer.blocks);

        replaceBlocksForBiome(x, z, buffer.blocks, buffer.metas);

        // Post-biome blocks
        if (hasLowlands) caveGenV2.generate(this, x, z, buffer.blocks);
        if (hasNotLowlands) caveGenSmall.generate(this, x, z, buffer.blocks);

        oil.setMetas(buffer.metas);
        oil.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /** hbm-space's {@code WorldConfig.ironSpawn} and {@code zincSpawn} defaults. */
    public static final int IRON_SPAWN = 9;
    public static final int ZINC_SPAWN = 8;

    /**
     * {@code WorldGeneratorDuna.generateDuna}'s ore veins for the chunk at block origin {@code (i, j)}, drawn from the
     * FML generator {@code Random} ({@link Ores17#generatorRandom}): Duna iron and zinc in Duna rock, then the five basalt
     * ores in the lowland caves' basalt. {@code ores} gives the placer of an ore by its HBM id ({@code ore_iron},
     * {@code ore_zinc}, {@code ore_basalt_<type>}), or null for an ore not ported yet (its veins are still drawn).
     * The rest of {@code generateDuna} (the martian world preset's base, the broken behemoth lanterns) is not terrain.
     */
    public static void populateOres(Random rand, int i, int j, java.util.function.Function<String, Ores17.Placer> ores) {
        Ores17.generateOre(rand, i, j, IRON_SPAWN, 8, 32, 64, ores.apply("ore_iron"));
        Ores17.generateOre(rand, i, j, ZINC_SPAWN, 9, 4, 27, ores.apply("ore_zinc"));

        // Basalt rich in minerals, but only in basaltic caves!
        Ores17.generateOre(rand, i, j, 12, 6, 0, 16, ores.apply("ore_basalt_sulfur"));
        Ores17.generateOre(rand, i, j, 8, 8, 0, 16, ores.apply("ore_basalt_fluorite"));
        Ores17.generateOre(rand, i, j, 8, 9, 0, 16, ores.apply("ore_basalt_asbestos"));
        Ores17.generateOre(rand, i, j, 2, 4, 0, 16, ores.apply("ore_basalt_gem"));
        Ores17.generateOre(rand, i, j, 6, 10, 0, 16, ores.apply("ore_basalt_molysite"));
    }

    /** {@code BiomeGenBaseDuna}: no rain, temperature -1, Duna sand over Duna rock. */
    private abstract static class Duna extends Biome17 {

        Duna(float rootHeight, float heightVariation) {
            this.temperature = -1.0F;
            this.rainfall = 0.0F;
            this.topBlock = Block17.DUNA_SANDS;
            this.fillerBlock = Block17.DUNA_ROCK;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
        }
    }

    /** {@code BiomeGenDunaPlains}, "Dunaian Plains". */
    private final class Plains extends Duna {

        Plains() {
            super(0.125F, 0.05F);
            this.topBlock = Block17.DUNA_SANDS;
            this.fillerBlock = Block17.DUNA_SANDS;
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
                        if (block2 == Block17.DUNA_ROCK) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.DUNA_ROCK;
                                } else if (l1 >= 59 && l1 <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    // 1.7 tested getFloatTemperature(x, l1, z) < 0.15 here; both branches set the same
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (l1 >= 62) {
                                    blocks[i2] = block;
                                    meta[i2] = b0;
                                } else if (l1 < 62) {
                                    block = null;
                                    block1 = Block17.DUNA_ROCK;
                                    if (session.nextDouble() > 0.4) { // 1.7: Math.random()
                                        blocks[i2] = Block17.DUNA_ROCK;
                                    } else {
                                        blocks[i2] = Block17.DUNA_SANDS;
                                    }
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

    /** {@code BiomeGenDunaLowlands}, "Dunaian Lowland Plains". */
    private static final class Lowlands extends Duna {

        Lowlands() {
            super(-0.6F, 0.01F);
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
                        if (block2 == Block17.DUNA_ROCK) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.DUNA_ROCK;
                                } else if (l1 >= 59 && l1 <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (l1 >= 62) {
                                    blocks[i2] = block;
                                    meta[i2] = b0;
                                } else if (l1 < 62) {
                                    block = null;
                                    block1 = topBlock;
                                    float randValue = rand.nextFloat();
                                    if (l1 < 55 && randValue > 0.75) {
                                        blocks[i2] = DunaIkeDresBlocks17.DUNA_COBBLE;
                                    } else if (l1 < 54 && randValue > 0.5) {
                                        blocks[i2] = DunaIkeDresBlocks17.DUNA_COBBLE;
                                    } else if (randValue > 0.4) {
                                        blocks[i2] = Block17.DUNA_ROCK;
                                    } else {
                                        blocks[i2] = Block17.DUNA_SANDS;
                                    }
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

    /** {@code BiomeGenDunaPolar} ("Dunaian Ice Sheet") and {@code BiomeGenDunaPolarHills} ("Dunaian Polar Mountains"). */
    private final class Polar extends Duna {

        Polar(float rootHeight, float heightVariation) {
            super(rootHeight, heightVariation);
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
                        if (block2 == Block17.DUNA_ROCK) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.DUNA_ROCK;
                                } else if (l1 >= 59 && l1 <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (l1 >= 62) {
                                    if (session.nextDouble() > 0.4) { // 1.7: Math.random()
                                        blocks[i2] = block;
                                    } else {
                                        blocks[i2] = DunaIkeDresBlocks17.DRY_ICE;
                                    }
                                    meta[i2] = b0;
                                } else if (l1 < 56 - l) {
                                    block = null;
                                    block1 = Block17.DUNA_ROCK;
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

    /**
     * {@code BiomeGenDunaHills}, "Weathered Dunaian Hills": vanilla's mesa surface ({@code BiomeGenMesa}, "Ripped from"),
     * ferric clay and red terracotta bands over Duna rock. The mesa's two optional features (bryce spires, the high
     * plateau surface) are off, as they were in hbm-space.
     */
    private final class Hills extends Duna {

        private byte[] field_150621_aC;
        private long seed;
        private Noise17Perlin perlin1;
        private Noise17Perlin perlin2;
        private Noise17Perlin perlin3;
        private boolean field_150626_aH;
        private boolean field_150620_aI;

        Hills() {
            super(0.525F, 0.51F);
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] metas, int x, int z, double noise) {
            long worldSeed = seed();
            if (this.field_150621_aC == null || this.seed != worldSeed) {
                this.generateBuffers(worldSeed);
            }

            if (this.perlin1 == null || this.perlin2 == null || this.seed != worldSeed) {
                Random random1 = new Random(this.seed);
                this.perlin1 = new Noise17Perlin(random1, 4);
                this.perlin2 = new Noise17Perlin(random1, 1);
            }

            this.seed = worldSeed;
            double d5 = 0.0D;
            int k;
            int l;

            if (this.field_150626_aH) {
                k = (x & -16) + (z & 15);
                l = (z & -16) + (x & 15);
                double d1 = Math.min(Math.abs(noise), this.perlin1.value((double) k * 0.25D, (double) l * 0.25D));

                if (d1 > 0.0D) {
                    double d2 = 0.001953125D;
                    double d3 = Math.abs(this.perlin2.value((double) k * d2, (double) l * d2));
                    d5 = d1 * d1 * 2.5D;
                    double d4 = Math.ceil(d3 * 50.0D) + 14.0D;

                    if (d5 > d4) {
                        d5 = d4;
                    }

                    d5 += 64.0D;
                }
            }

            k = x & 15;
            l = z & 15;
            Block17 block = DunaIkeDresBlocks17.FERRIC_CLAY;
            Block17 block2 = this.fillerBlock;
            int i1 = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
            boolean flag1 = Math.cos(noise / 3.0D * Math.PI) > 0.0D;
            int j1 = -1;
            boolean flag2 = false;
            int k1 = blocks.length / 256;

            for (int l1 = 255; l1 >= 0; --l1) {
                int i2 = (l * 16 + k) * k1 + l1;

                if (blocks[i2] == null && l1 < (int) d5) {
                    blocks[i2] = Block17.DUNA_ROCK;
                }

                if (l1 <= 0 + rand.nextInt(5)) {
                    blocks[i2] = Block17.BEDROCK;
                } else {
                    Block17 block1 = blocks[i2];

                    if (block1 != null) {
                        if (block1 == Block17.DUNA_ROCK) {
                            byte b0;

                            if (j1 == -1) {
                                flag2 = false;

                                if (i1 <= 0) {
                                    block = null;
                                    block2 = Block17.DUNA_ROCK;
                                } else if (l1 >= 59 && l1 <= 64) {
                                    block = DunaIkeDresBlocks17.FERRIC_CLAY;
                                    block2 = this.fillerBlock;
                                }

                                if (l1 < 63 && block == null) {
                                    block = Block17.WATER; // assigned and never placed, as in 1.7
                                }

                                j1 = i1 + Math.max(0, l1 - 63);

                                if (l1 >= 62) {
                                    if (this.field_150620_aI && l1 > 86 + i1 * 2) {
                                        if (flag1) {
                                            blocks[i2] = Block17.DUNA_SANDS;
                                        } else {
                                            blocks[i2] = Block17.DUNA_SANDS;
                                        }
                                    } else if (l1 > 66 + i1) {
                                        b0 = 16;

                                        if (l1 >= 64 && l1 <= 127) {
                                            if (!flag1) {
                                                b0 = this.func_150618_d(x, l1, z);
                                            }
                                        } else {
                                            b0 = 1;
                                        }

                                        if (b0 < 16) {
                                            blocks[i2] = DunaIkeDresBlocks17.STAINED_HARDENED_CLAY;
                                            metas[i2] = 14;
                                        } else {
                                            blocks[i2] = Block17.DUNA_ROCK;
                                        }
                                    } else {
                                        blocks[i2] = this.topBlock;
                                        flag2 = true;
                                    }
                                } else {
                                    blocks[i2] = block2;
                                }
                            } else if (j1 > 0) {
                                --j1;

                                if (flag2) {
                                    blocks[i2] = DunaIkeDresBlocks17.FERRIC_CLAY;
                                } else {
                                    b0 = this.func_150618_d(x, l1, z);

                                    if (b0 == 4) { // yellow is ugly
                                        blocks[i2] = DunaIkeDresBlocks17.FERRIC_CLAY;
                                    } else if (b0 == 0) { // white is good for yer teeth
                                        blocks[i2] = DunaIkeDresBlocks17.STONE_RESOURCE;
                                        metas[i2] = 7; // EnumStoneType.CALCIUM.ordinal()
                                    } else if (b0 < 16) {
                                        blocks[i2] = DunaIkeDresBlocks17.STAINED_HARDENED_CLAY;
                                        metas[i2] = b0;
                                    } else {
                                        blocks[i2] = Block17.DUNA_ROCK;
                                    }
                                }
                            }
                        }
                    } else {
                        j1 = -1;
                    }
                }
            }
        }

        private void generateBuffers(long seed) {
            this.field_150621_aC = new byte[64];
            Arrays.fill(this.field_150621_aC, (byte) 16);
            Random random = new Random(seed);
            this.perlin3 = new Noise17Perlin(random, 1);
            int j;

            for (j = 0; j < 64; ++j) {
                j += random.nextInt(5) + 1;

                if (j < 64) {
                    this.field_150621_aC[j] = 1;
                }
            }

            j = random.nextInt(4) + 2;
            int k;
            int l;
            int i1;
            int j1;

            for (k = 0; k < j; ++k) {
                l = random.nextInt(3) + 1;
                i1 = random.nextInt(64);

                for (j1 = 0; i1 + j1 < 64 && j1 < l; ++j1) {
                    this.field_150621_aC[i1 + j1] = 4;
                }
            }

            k = random.nextInt(4) + 2;
            int k1;

            for (l = 0; l < k; ++l) {
                i1 = random.nextInt(3) + 2;
                j1 = random.nextInt(64);

                for (k1 = 0; j1 + k1 < 64 && k1 < i1; ++k1) {
                    this.field_150621_aC[j1 + k1] = 12;
                }
            }

            l = random.nextInt(4) + 2;

            for (i1 = 0; i1 < l; ++i1) {
                j1 = random.nextInt(3) + 1;
                k1 = random.nextInt(64);

                for (int l1 = 0; k1 + l1 < 64 && l1 < j1; ++l1) {
                    this.field_150621_aC[k1 + l1] = 14;
                }
            }

            i1 = random.nextInt(3) + 3;
            j1 = 0;

            for (k1 = 0; k1 < i1; ++k1) {
                byte b0 = 1;
                j1 += random.nextInt(16) + 4;

                for (int i2 = 0; j1 + i2 < 64 && i2 < b0; ++i2) {
                    this.field_150621_aC[j1 + i2] = 0;

                    if (j1 + i2 > 1 && random.nextBoolean()) {
                        this.field_150621_aC[j1 + i2 - 1] = 8;
                    }

                    if (j1 + i2 < 63 && random.nextBoolean()) {
                        this.field_150621_aC[j1 + i2 + 1] = 8;
                    }
                }
            }
        }

        private byte func_150618_d(int p_150618_1_, int p_150618_2_, int p_150618_3_) {
            int l = (int) Math.round(this.perlin3.value((double) p_150618_1_ * 1.0D / 512.0D, (double) p_150618_1_ * 1.0D / 512.0D) * 2.0D);
            return this.field_150621_aC[(p_150618_2_ + l + 64) % 64];
        }
    }

    /** 1.7's {@code BiomeGenRiver} (height -0.5, 0), with vanilla's default surface pass. */
    private static final class VanillaRiver extends Biome17 {

        VanillaRiver() {
            this.rootHeight = -0.5F;
            this.heightVariation = 0.0F;
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] metas, int x, int z, double noise) {
            VanillaSurface17.genBiomeTerrain(this, rand, blocks, metas, x, z, noise);
        }
    }
}
