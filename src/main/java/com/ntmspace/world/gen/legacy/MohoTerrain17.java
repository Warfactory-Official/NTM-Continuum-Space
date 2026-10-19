package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderMoho} with Moho's four biomes ({@code dim/moho/biome}), ported line for line: moho
 * stone over a lava sea, through the biome map of {@link MohoLayers17}; Voronoi plates on the lava seas
 * ({@link MapGenVNoise17}), stepped plateaus ({@link MapGenPlateau17}), then caves, ravines, small and large craters and
 * volcanoes, in that order.
 *
 * <p>The plateau noise was built from 1.7's unseeded {@code world.rand}; the port builds it from the session
 * {@code Random} it is given, so plateaus change from one game session to the next as they did in 1.7.
 */
public class MohoTerrain17 extends LayeredBodyTerrain17 {

    private final ExperimentalCaves17 caveGenV2 = new ExperimentalCaves17(1, 52, 5.0F);
    private final MapGenRavine rgen = new MapGenRavine();

    private final MapGenVNoise17 noise = new MapGenVNoise17();
    private final MapGenCrater smallCrater = new MapGenCrater(6);
    private final MapGenCrater largeCrater = new MapGenCrater(64);
    private final MapGenVolcano17 volcano = new MapGenVolcano17(72);
    private final MapGenPlateau17 plateau;

    public final Biome17 crag = new Moho(0.275F, 0.666F, Block17.MOHO_REGOLITH);
    public final Biome17 basalt = new Basalt();
    public final Biome17 lavaSea = new Moho(-0.8F, 0.01F, Block17.MOHO_REGOLITH);
    public final Biome17 plateauBiome = new Moho(0.255F, 0.432F, Block17.MOHO_REGOLITH);
    /** Vanilla's {@code river} (id 7), which Moho's river mix lays into the terrain noise's biome map. */
    public final Biome17 vanillaRiver = new VanillaRiver();

    public MohoTerrain17(long seed, Random session) {
        super(seed, MohoLayers17::create);
        register(MohoLayers17.CRAG, crag);
        register(MohoLayers17.BASALT, basalt);
        register(MohoLayers17.LAVA_SEA, lavaSea);
        register(MohoLayers17.PLATEAU, plateauBiome);
        register(com.ntmspace.world.gen.legacy.layer.GenLayerRiver17.VANILLA_RIVER, vanillaRiver);

        plateau = new MapGenPlateau17(session);

        smallCrater.setSize(8, 32);
        largeCrater.setSize(96, 128);
        volcano.setSize(64, 128);
        volcano.setMaterial(Block17.VOLCANO_CORE, Block17.BASALT);

        smallCrater.regolith = largeCrater.regolith = Block17.MOHO_REGOLITH;
        smallCrater.rock = largeCrater.rock = Block17.MOHO_STONE;

        caveGenV2.stoneBlock = Block17.MOHO_STONE;
        rgen.stoneBlock = Block17.MOHO_STONE;
        stoneBlock = Block17.MOHO_STONE;
        seaBlock = Block17.LAVA;

        noise.surfBlock = Block17.MOHO_STONE;
        noise.rockBlock = Block17.MOHO_STONE;
        noise.fluidBlock = Block17.LAVA;
        noise.crackSize = 0.5;
        noise.cellSize = 27;
        noise.plateStartY = 62;
        noise.plateThickness = 25;
        noise.applyToBiome = lavaSea;

        plateau.maxPlateauAddition = 6;
        plateau.surfrock = Block17.MOHO_REGOLITH;
        plateau.stoneBlock = Block17.MOHO_STONE;
        plateau.fillblock = Block17.LAVA;
        plateau.maxPlateauAddition = 6;
        plateau.stepHeight = 2;
        plateau.noiseScale = 0.03;
        plateau.applyToBiome = plateauBiome;
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);

        boolean hasLavaSea = chunkHas(x, z, MohoLayers17.LAVA_SEA);
        boolean hasPlateau = chunkHas(x, z, MohoLayers17.PLATEAU);

        if (hasLavaSea) noise.generate(this, x, z, buffer.blocks);

        if (hasPlateau) plateau.generate(this, x, z, buffer.blocks);

        caveGenV2.generate(this, x, z, buffer.blocks);
        rgen.generate(this, x, z, buffer.blocks);
        smallCrater.generate(this, x, z, buffer.blocks);
        largeCrater.generate(this, x, z, buffer.blocks);
        volcano.generate(this, x, z, buffer.blocks);
        return buffer;
    }

    /** hbm-space's {@code BiomeGenBaseMoho} (crag, lava sea and plateau use its surface pass). */
    private static class Moho extends Biome17 {

        Moho(float rootHeight, float heightVariation, Block17 surface) {
            this.temperature = 1.0F;
            this.rainfall = 0.0F;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
            this.topBlock = surface;
            this.fillerBlock = surface; // thiccer regolith due to uhhhhhh...................
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] meta, int x, int z, double noise) {
            Block17 block = this.topBlock;
            byte b0 = (byte) (this.topMeta & 255);
            Block17 block1 = this.fillerBlock;
            int k = -1;
            int l = (int) (noise / 8.0D + 8.0D + rand.nextDouble() * 0.50D);
            int bx = x & 15;
            int bz = z & 15;
            int s = blocks.length / 256;

            for (int by = 255; by >= 0; --by) {
                int i = (bz * 16 + bx) * s + by;

                if (by <= 0 + rand.nextInt(5)) {
                    blocks[i] = Block17.BEDROCK;
                } else {
                    Block17 block2 = blocks[i];

                    if (block2 != null) {
                        if (block2 == Block17.MOHO_STONE) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.MOHO_STONE;
                                } else if (by >= 59 && by <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (by < 63 && block == null) {
                                    // 1.7 checked getFloatTemperature < 0.15 here; both branches set the same
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (by >= 62) {
                                    blocks[i] = block;
                                    meta[i] = b0;
                                } else if (by < 56 - l) {
                                    block = null;
                                    block1 = Block17.MOHO_STONE;
                                    blocks[i] = Block17.BASALT;
                                } else {
                                    blocks[i] = block1;
                                }
                            } else if (k > 0) {
                                --k;
                                blocks[i] = block1;

                                if (k == 0 && block1 == Block17.SAND) {
                                    k = rand.nextInt(4) + Math.max(0, by - 63);
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
     * hbm-space's {@code BiomeGenMohoBasalt} ("Moho Basalt Deltas"): basalt surface, with random basalt spires raised
     * off the column's top except on the stone-noise contour bands.
     */
    private static final class Basalt extends Moho {

        Basalt() {
            super(0.0F, 0.224F, Block17.BASALT);
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] meta, int x, int z, double noise) {
            Block17 block = this.topBlock;
            byte b0 = (byte) (this.topMeta & 255);
            Block17 block1 = this.fillerBlock;
            int k = -1;
            int l = (int) (noise / 8.0D + 8.0D + rand.nextDouble() * 0.50D);
            int bx = x & 15;
            int bz = z & 15;
            int s = blocks.length / 256;

            for (int by = 255; by >= 0; --by) {
                int i = (bz * 16 + bx) * s + by;

                boolean contour = (noise > -0.25D && noise < 1.0D) || (noise > 1.5D && noise < 2.0D) || (noise > 2.5D && noise < 3.0D);
                double offset = contour ? 0 : Math.pow(rand.nextDouble(), 4) * 8;

                if (by <= 0 + rand.nextInt(5)) {
                    blocks[i] = Block17.BEDROCK;
                } else {
                    Block17 block2 = blocks[i];

                    if (block2 != null) {
                        if (block2 == Block17.MOHO_STONE) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.MOHO_STONE;
                                } else if (by >= 59 && by <= 64) {
                                    block = this.topBlock;
                                    b0 = (byte) (this.topMeta & 255);
                                    block1 = this.fillerBlock;
                                }

                                if (by < 63 && block == null) {
                                    block = this.topBlock;
                                    b0 = 0;
                                }

                                k = l;

                                if (by >= 62) {
                                    blocks[i] = block;
                                    meta[i] = b0;
                                } else if (by < 56 - l) {
                                    block = null;
                                    block1 = Block17.MOHO_STONE;
                                    blocks[i] = Block17.BASALT;
                                } else {
                                    blocks[i] = block1;
                                }

                                for (int oy = 0; oy < offset; oy++) {
                                    blocks[i + oy] = this.topBlock;
                                }
                            } else if (k > 0) {
                                --k;
                                blocks[i] = block1;
                            }
                        }
                    } else {
                        k = -1;
                    }
                }
            }
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
