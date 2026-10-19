package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.noise.Noise17Perlin;
import java.util.Random;

/**
 * hbm-space's {@code ChunkProviderEve} with Eve's five biomes ({@code dim/eve/biome}), ported line for line: eve rock
 * under eve silt over a sea of mercury, through the biome map of {@link EveLayers17}; seismic cracks of lava in the
 * seismic plains, Voronoi rock plates on the mercury oceans ({@link MapGenVNoise17}), then natural gas bubbles.
 *
 * <p>Two 1.7 sources of randomness had no seed, and their behaviour is kept with a session {@code Random} in their place:
 * <ul>
 *   <li>the crack noise was built from {@code world.rand} (unseeded), so the cracks move from one game session to the
 *       next, as in 1.7;</li>
 *   <li>the plains, mountains and seismic plains surfaces picked eve rock or smooth basalt with {@code Math.random()}.
 *       The port draws those choices from a {@code Random} seeded per chunk from the session, so the same chunk built
 *       twice in one session is the same (1.7 built each chunk once); the pattern is as random as 1.7's.</li>
 * </ul>
 *
 * <p><b>Ported oddity:</b> the ocean and river surfaces test for Duna's rock (a copy-paste in hbm-space), which Eve never
 * has, so those biomes only get their bedrock: their sea floor is bare eve rock.
 */
public class EveTerrain17 extends LayeredBodyTerrain17 {

    /** {@code SolarSystem.Body.EVE.ordinal()}: Eve's ore metadata in hbm-space. */
    public static final byte EVE_META = 7;
    /** hbm-space's {@code WorldConfig.eveGasSpawn} default. */
    public static final int GAS_SPAWN = 100;

    private final Noise17Perlin crackNoise;
    private final MapGenVNoise17 noise = new MapGenVNoise17();
    private final MapGenBubble oil = new MapGenBubble(GAS_SPAWN);
    private final long sessionSeed;
    /** Stands in for {@code Math.random()} in the surface pass; reseeded per chunk. */
    private final Random mathRandom = new Random();

    public final Biome17 plains = new Eve(0.125F, 0.05F, Surface.PLAINS);
    public final Biome17 ocean = new Eve(-0.6F, 0.01F, Surface.OCEAN);
    public final Biome17 mountains = new Eve(0.525F, 0.4F, Surface.PLAINS);
    public final Biome17 seismicPlains = new Eve(0.270F, 0.3F, Surface.SEISMIC);
    public final Biome17 river = new Eve(-0.7F, 0.0F, Surface.OCEAN);

    public EveTerrain17(long seed, Random session) {
        super(seed, EveLayers17::create);
        register(EveLayers17.PLAINS, plains);
        register(EveLayers17.OCEAN, ocean);
        register(EveLayers17.MOUNTAINS, mountains);
        register(EveLayers17.SEISMIC_PLAINS, seismicPlains);
        register(EveLayers17.RIVER, river);
        ocean.topBlock = Block17.EVE_SILT;
        ocean.fillerBlock = Block17.EVE_SILT;

        reclamp = false;
        stoneBlock = Block17.EVE_ROCK;
        seaBlock = Block17.MERCURY;
        this.crackNoise = new Noise17Perlin(session, 4);
        this.sessionSeed = session.nextLong();

        noise.fluidBlock = Block17.MERCURY;
        noise.rockBlock = Block17.EVE_ROCK;
        noise.surfBlock = Block17.EVE_SILT;
        noise.cellSize = 72;
        noise.crackSize = 2.0;
        noise.plateThickness = 35;
        noise.shapeExponent = 2.0;
        noise.plateStartY = 57;

        noise.applyToBiome = ocean;

        oil.block = Block17.ORE_GAS;
        oil.meta = EVE_META;
        oil.replace = Block17.EVE_ROCK;
        oil.setSize(8, 16);
    }

    @Override
    protected Primer getChunkPrimer(int x, int z) {
        mathRandom.setSeed(sessionSeed ^ ((long) x * 341873128712L + (long) z * 132897987541L));
        Primer buffer = super.getChunkPrimer(x, z);
        oil.setMetas(buffer.metas);

        boolean hasOcean = chunkHas(x, z, EveLayers17.OCEAN);
        boolean hasSeismic = chunkHas(x, z, EveLayers17.SEISMIC_PLAINS);

        if (hasSeismic) generateCracks(x, z, buffer);
        if (hasOcean) noise.generate(this, x, z, buffer.blocks);

        oil.generate(this, x, z, buffer.blocks);

        return buffer;
    }

    private void generateCracks(int chunkX, int chunkZ, Primer buffer) {
        int[] biomes = chunkBiomeIds(chunkX, chunkZ);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (biomes[x + z * 16] != EveLayers17.SEISMIC_PLAINS) continue;

                double crackValue = crackNoise.value((chunkX * 16 + x) * 0.3, (chunkZ * 16 + z) * 0.3); // Lower scale value for more spread-out cracks

                if (crackValue > 0.9) {
                    int bedrockY = -1;
                    for (int y = 0; y < 256; y++) {
                        int index = (x * 16 + z) * 256 + y;
                        if (buffer.blocks[index] == Block17.BEDROCK) {
                            if (bedrockY == -1) bedrockY = y;
                        } else {
                            buffer.blocks[index] = null;
                        }
                    }
                    if (bedrockY != -1) {
                        for (int y = bedrockY + 1; y < Math.min(bedrockY + 10, 256); y++) {
                            int index = (x * 16 + z) * 256 + y;
                            buffer.blocks[index] = Block17.LAVA;
                        }
                    }
                }
            }
        }
    }

    private enum Surface { PLAINS, OCEAN, SEISMIC }

    /** hbm-space's {@code BiomeGenBaseEve} and its five subclasses' {@code genTerrainBlocks}. */
    private final class Eve extends Biome17 {

        private final Surface surface;

        Eve(float rootHeight, float heightVariation, Surface surface) {
            this.surface = surface;
            this.temperature = 1.0F;
            this.rainfall = 0.5F;
            this.rootHeight = rootHeight;
            this.heightVariation = heightVariation;
            this.topBlock = Block17.EVE_SILT;
            this.fillerBlock = Block17.EVE_ROCK;
        }

        @Override
        public void genTerrainBlocks(Random rand, Block17[] blocks, byte[] meta, int x, int z, double noise) {
            // the ocean and the river test Duna's rock and fall back on it (hbm-space copied them from Duna)
            Block17 rock = surface == Surface.OCEAN ? Block17.DUNA_ROCK : Block17.EVE_ROCK;
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

                                if (surface == Surface.SEISMIC) {
                                    if (l1 >= 62) {
                                        if (mathRandom.nextDouble() > 0.4) {
                                            blocks[i2] = block;
                                        } else {
                                            blocks[i2] = Block17.EVE_ROCK;
                                        }
                                        meta[i2] = b0;
                                    } else if (l1 < 56 - l) {
                                        block = null;
                                        block1 = Block17.EVE_ROCK;
                                        blocks[i2] = Block17.BASALT;
                                    } else {
                                        blocks[i2] = block1;
                                    }
                                } else {
                                    if (l1 >= 62) {
                                        blocks[i2] = block;
                                        meta[i2] = b0;
                                    } else {
                                        // 1.7: "else if (l1 < 62)", always true here
                                        block = null;
                                        block1 = rock;
                                        if (mathRandom.nextDouble() > 0.4) {
                                            blocks[i2] = rock;
                                        } else {
                                            blocks[i2] = surface == Surface.OCEAN ? Block17.DUNA_SANDS : Block17.BASALT_SMOOTH;
                                        }
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
