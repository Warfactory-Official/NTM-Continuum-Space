package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderTekto} with Tekto's five biomes ({@code dim/tekto/biome}), ported line for line:
 * basalt under rubber grass, rubber silt and vinyl sand with a carbon tetrachloride sea below y 63, through the biome map
 * of {@link TektoLayers17} and without the reclamped noise; then a vinyl volcano with a chloric geyser when the chunk's
 * origin column is Vinyl Desert, Greg caves, Tekto oil bubbles and bedrock oil, in that order.
 *
 * <p>The surface picked basalt or smooth basalt with {@code Math.random()}; as in {@link EveTerrain17}, the port draws
 * that choice from a {@code Random} seeded per chunk from the session.
 *
 * <p><b>Ported oddity:</b> the Tetrachloride River's surface tests for Duna's rock (a copy-paste in hbm-space), which
 * Tekto never has, so a river only gets its bedrock: its bed is bare basalt.
 */
public class TektoTerrain17 extends LayeredBodyTerrain17 {

    /** {@code SolarSystem.Body.TEKTO.ordinal()}: Tekto's ore metadata in hbm-space. */
    public static final byte TEKTO_META = 10;
    /** hbm-space's {@code WorldConfig.tektoOilSpawn} default. */
    public static final int OIL_SPAWN = 100;
    /** hbm-space's {@code WorldConfig.tektoBedrockOilSpawn} default. */
    public static final int BEDROCK_OIL_SPAWN = 200;

    private final MapGenGreg caveGenV3 = new MapGenGreg();
    private final MapGenVolcano17 volcano = new MapGenVolcano17(12);

    private final MapGenBubble tektonic = new MapGenBubble(OIL_SPAWN);
    public static final MapGenBedrockOil17.Config BEDROCK_OIL = new MapGenBedrockOil17.Config(
            Block17.ORE_BEDROCK_OIL, TEKTO_META, Block17.BASALT, MapGenBedrockOil17.TEKTO_SCARS);
    private final MapGenBedrockOil17 bedtonic = BEDROCK_OIL.create(BEDROCK_OIL_SPAWN);

    private final long sessionSeed;
    /** Stands in for {@code Math.random()} in the surface pass; reseeded per chunk. */
    private final Random mathRandom = new Random();

    /** {@code polyvinylPlains}, "Polyvinyl Plains". */
    public final Biome17 polyvinylPlains = new Tekto(0.125F, 0.05F, Block17.RUBBER_GRASS, Block17.RUBBER_SILT, false);
    /** {@code halogenHills}, "Halogen Hills". */
    public final Biome17 halogenHills = new Tekto(0.525F, 0.4F, Block17.RUBBER_SILT, Block17.RUBBER_SILT, false);
    /** {@code tetrachloricRiver}, "Tetrachloride River". */
    public final Biome17 tetrachloricRiver = new Tekto(-0.7F, 0.0F, Block17.VINYL_SAND, Block17.VINYL_SAND, true);
    /** {@code forest}, "Tekto Forest". */
    public final Biome17 forest = new Tekto(0.425F, 0.3F, Block17.RUBBER_GRASS, Block17.RUBBER_SILT, false);
    /** {@code vinylsands}, "Vinyl Desert". */
    public final Biome17 vinylsands = new Tekto(0.425F, 0.3F, Block17.VINYL_SAND, Block17.VINYL_SAND, false);

    public TektoTerrain17(long seed, Random session) {
        super(seed, TektoLayers17::create);
        register(TektoLayers17.POLYVINYL_PLAINS, polyvinylPlains);
        register(TektoLayers17.HALOGEN_HILLS, halogenHills);
        register(TektoLayers17.TETRACHLORIC_RIVER, tetrachloricRiver);
        register(TektoLayers17.FOREST, forest);
        register(TektoLayers17.VINYL_SANDS, vinylsands);
        this.sessionSeed = session.nextLong();

        reclamp = false; // please kill this, we spend more time calculating perlin noise than anything then skip two whole octaves of it!
        caveGenV3.stoneBlock = Block17.BASALT;

        volcano.setSize(8, 16);
        volcano.setMaterial(Block17.GEYSIR_CHLORIC, Block17.VINYL_SAND);

        tektonic.block = Block17.ORE_TEKTO;
        tektonic.meta = TEKTO_META;
        tektonic.replace = Block17.BASALT;
        tektonic.setSize(8, 16);

        stoneBlock = Block17.BASALT;
        seaBlock = Block17.CCL_BLOCK;
    }

    @Override
    protected void prepareSurface(int x, int z) {
        mathRandom.setSeed(sessionSeed ^ ((long) x * 341873128712L + (long) z * 132897987541L));
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        Primer buffer = super.getChunkPrimer(x, z);
        tektonic.setMetas(buffer.metas);
        bedtonic.setMetas(buffer.metas);

        if (biomeAt(x * 16, z * 16) == vinylsands) { // biomesForGeneration[0] == BiomeGenBaseTekto.vinylsands
            volcano.generate(this, x, z, buffer.blocks);
        }

        caveGenV3.generate(this, x, z, buffer.blocks);

        tektonic.generate(this, x, z, buffer.blocks);
        bedtonic.generate(this, x, z, buffer.blocks);

        // how many times do I gotta say BEEEEG
        return buffer;
    }

    /**
     * hbm-space's {@code BiomeGenBaseTekto} subclasses; all five share one surface pass (Forest's), the river's testing
     * Duna's rock and laying Duna's sands instead.
     */
    private final class Tekto extends Biome17 {

        private final Block17 rock;
        private final Block17 smooth;

        Tekto(float rootHeight, float heightVariation, Block17 top, Block17 filler, boolean river) {
            this.temperature = 1.0F;
            this.rainfall = 0.5F;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
            this.topBlock = top;
            this.waterColorMultiplier = PURPLE_WATER_MULTIPLIER;
            this.fillerBlock = filler;
            this.rock = river ? Block17.DUNA_ROCK : Block17.BASALT;
            this.smooth = river ? Block17.DUNA_SANDS : Block17.BASALT_SMOOTH;
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
                        if (block2 == rock) {
                            if (k == -1) {
                                if (l <= 0) {
                                    block = null;
                                    b0 = 0;
                                    block1 = rock;
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
                                    block1 = rock;
                                    if (mathRandom.nextDouble() > 0.4) { // Math.random()
                                        blocks[i2] = rock;
                                    } else {
                                        blocks[i2] = smooth;
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
