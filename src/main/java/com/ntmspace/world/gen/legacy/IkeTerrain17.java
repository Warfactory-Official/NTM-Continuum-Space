package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderIke} with {@code BiomeGenIke}, ported line for line: one biome everywhere
 * ({@code WorldChunkManagerHell}), thick Ike regolith over Ike stone, then tilted spires ({@link MapGenTiltedSpires17}),
 * vanilla caves ({@link Caves17}), brine bubbles ({@link MapGenBubble}) and rare "Sellafield" hot-spot craters
 * ({@link MapGenTerrainCrater17}), in that order.
 *
 * <p><b>Ported oddities:</b> the caves are Forge's plain {@code MapGenCaves}, which digs only vanilla stone and the
 * biome's top and filler blocks: on Ike they carve the regolith and stop at Ike stone. The hot spots start in one origin
 * chunk in {@code WorldConfig.radfreq / 10} (500), and a crater laid over a brine bubble keeps the bubble's metadata, so
 * its sellafite there reads as level 5 (1.7 had the same metadata in its primer).
 */
public class IkeTerrain17 extends CelestialTerrain17 {

    /** hbm-space's {@code WorldConfig.ikeBrineSpawn} default. */
    public static final int BRINE_SPAWN = 100;
    /** hbm-space's {@code WorldConfig.radfreq} default (5000), divided by ten as {@code ChunkProviderIke} did. */
    public static final int SELLAFIELD_FREQUENCY = 5000 / 10;

    public final Biome17 biome = new BiomeIke();

    private final Caves17 caveGenerator = new Caves17();
    private final MapGenTiltedSpires17 spires = new MapGenTiltedSpires17(6, 6, 0F);
    private final MapGenBubble brine = new MapGenBubble(BRINE_SPAWN);
    private final MapGenTerrainCrater17 sellafield = new MapGenTerrainCrater17(SELLAFIELD_FREQUENCY);

    public IkeTerrain17(long seed) {
        super(seed);

        spires.rock = DunaIkeDresBlocks17.IKE_STONE;
        spires.regolith = DunaIkeDresBlocks17.IKE_REGOLITH;
        spires.mid = 86;

        brine.block = Block17.ORE_BRINE;
        brine.meta = BodyOres17.IKE;
        brine.replace = DunaIkeDresBlocks17.IKE_STONE;
        brine.setSize(8, 16);

        sellafield.regolith = DunaIkeDresBlocks17.SELLAFIELD;
        sellafield.rock = Block17.SELLAFIELD_SLAKED;

        stoneBlock = DunaIkeDresBlocks17.IKE_STONE;
    }

    @Override
    public Biome17 biomeAt(int x, int z) {
        return biome;
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);
        brine.setMetas(buffer.metas);

        spires.generate(this, x, z, buffer.blocks);
        caveGenerator.generate(this, x, z, buffer.blocks);
        brine.generate(this, x, z, buffer.blocks);
        sellafield.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    /**
     * hbm-space's {@code BiomeGenIke}, "Ike": height (0.325, 0.05), Ike regolith as top and filler ("thiccer regolith").
     * Its decorator's bromine lakes run at decoration time, not in the primer.
     */
    public static final class BiomeIke extends Biome17 {

        BiomeIke() {
            this.rootHeight = 0.325F;
            this.heightVariation = 0.05F;
            this.topBlock = DunaIkeDresBlocks17.IKE_REGOLITH;
            this.fillerBlock = DunaIkeDresBlocks17.IKE_REGOLITH; // thiccer regolith
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
                        if (block2 == DunaIkeDresBlocks17.IKE_STONE) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = DunaIkeDresBlocks17.IKE_STONE;
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
                                } else if (l1 < 56 - l) {
                                    block = null;
                                    block1 = DunaIkeDresBlocks17.IKE_STONE;
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
