package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderDres} with Dres's two biomes ({@code dim/dres/biome}), ported line for line: Dres rock
 * under slaked sellafite through the biome map of {@link DresLayers17}, then vanilla caves digging Dres rock
 * ({@link VanillaCaves17}) and small and large craters ({@link MapGenCrater}), in that order.
 *
 * <p><b>Ported oddity:</b> both surfaces choose the sellafite's shade below the top layer with {@code Math.random()}; the
 * port draws it from the session {@code Random} it is given, so (as in 1.7) those shades change between game sessions.
 */
public class DresTerrain17 extends LayeredBodyTerrain17 {

    private final Random session;

    private final VanillaCaves17 caveGenerator = new VanillaCaves17(DunaIkeDresBlocks17.DRES_ROCK);
    private final MapGenCrater smallCrater = new MapGenCrater(6);
    private final MapGenCrater largeCrater = new MapGenCrater(64);

    public final Biome17 plains = new Plains();
    public final Biome17 canyon = new Canyon();

    public DresTerrain17(long seed, Random session) {
        super(seed, DresLayers17::create);
        this.session = session;
        register(DresLayers17.PLAINS, plains);
        register(DresLayers17.CANYON, canyon);

        smallCrater.setSize(8, 32);
        largeCrater.setSize(96, 128);

        smallCrater.regolith = largeCrater.regolith = DunaIkeDresBlocks17.DRES_ROCK;
        smallCrater.rock = largeCrater.rock = DunaIkeDresBlocks17.DRES_ROCK;

        stoneBlock = DunaIkeDresBlocks17.DRES_ROCK;
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);

        caveGenerator.generate(this, x, z, buffer.blocks);
        smallCrater.generate(this, x, z, buffer.blocks);
        largeCrater.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /** {@code BiomeGenBaseDres}: no rain, temperature -1, slaked sellafite as top and filler. */
    private abstract static class Dres extends Biome17 {

        Dres(float rootHeight, float heightVariation) {
            this.temperature = -1.0F;
            this.rainfall = 0.0F;
            this.topBlock = Block17.SELLAFIELD_SLAKED;
            this.fillerBlock = Block17.SELLAFIELD_SLAKED;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
        }
    }

    /** {@code BiomeGenDresPlains}, "Dresian Plains". */
    private final class Plains extends Dres {

        Plains() {
            super(0.625F, 0.04F);
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] meta, int x, int z, double noise) {
            Block17 block = this.topBlock;
            byte b0 = (byte) (this.topMeta & 255);
            Block17 block1 = this.fillerBlock;
            int k = -1;
            int l = (int) (noise / 8.0D + 8.0D + rand.nextDouble() * 0.50D);
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
                        if (block2 == DunaIkeDresBlocks17.DRES_ROCK) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.SELLAFIELD_SLAKED;
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

                                if (l1 >= 82) {
                                    blocks[i2] = block;
                                    meta[i2] = b0;
                                } else if (l1 < 80) {
                                    block = null;
                                    block1 = Block17.SELLAFIELD_SLAKED;
                                    if (session.nextDouble() > 0.4) { // 1.7: Math.random()
                                        blocks[i2] = Block17.SELLAFIELD_SLAKED;
                                        meta[i2] = 8;
                                    } else {
                                        blocks[i2] = Block17.SELLAFIELD_SLAKED;
                                        meta[i2] = 9;
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

    /** {@code BiomeGenDresCanyon}, "Dres Large Basins". */
    private final class Canyon extends Dres {

        Canyon() {
            super(-1F, 0.34F);
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
                        if (block2 == DunaIkeDresBlocks17.DRES_ROCK) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = Block17.SELLAFIELD_SLAKED;
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
                                } else if (l1 < 68) {
                                    block = null;
                                    block1 = Block17.SELLAFIELD_SLAKED;
                                    if (session.nextDouble() > 0.4) { // 1.7: Math.random()
                                        blocks[i2] = Block17.SELLAFIELD_SLAKED;
                                        meta[i2] = 8;
                                    } else {
                                        blocks[i2] = Block17.SELLAFIELD_SLAKED;
                                        meta[i2] = 7;
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
}
