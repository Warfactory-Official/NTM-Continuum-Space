package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.LaytheChunkGenerator;
import com.ntmspace.world.TektoChunkGenerator;
import com.ntmspace.world.ThatmoChunkGenerator;
import com.ntmspace.world.gen.legacy.BasePrimers17;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.CompatChunkGenerator;
import java.util.Set;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * Laythe, Tekto and Thatmo generators on their own (a GameTest world has no datapack dimensions; in a world,
 * {@code /ntmspace check <body>}). For a fixed world seed and session seed:
 *
 * <ul>
 *   <li>five chunks' full primers hold only the body's blocks, have bedrock at y 0 (or Tekto's bedrock oil there) and
 *       agree with the generator's base column, which also proves every primer block maps to a registered block;</li>
 *   <li>six chunks' base primers (terrain noise, sea and surface pass, before the map generators;
 *       {@link BasePrimers17}) hash to the values the 1.7 code path gives: pinned from a plain-JVM run of the same
 *       terrain classes ({@code scratchpad/m3gl/Pin.java}), whose biome layers are proven equal to the 1.7.10 jar
 *       ({@code scratchpad/m3gl/Oracle.java}). The full primers are not pinned: hbm-space seeded its Greg noise caves per
 *       game session;</li>
 *   <li>Laythe's and Tekto's biome maps at four chunks hash to the values the 1.7.10 jar's own {@code GenLayer}
 *       classes give with hbm-space's stages ({@code Oracle.java}).</li>
 * </ul>
 */
public final class LaytheTektoThatmoGameTests {

    static final long SEED = 4815162342L;
    static final long SESSION = 1L;
    static final int[][] CHUNKS = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}};
    static final int[][] PIN_CHUNKS = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}, {250, -310}};

    /** Pinned base-primer height checksums ({@link BasePrimers17#heightChecksum}, folded {@code h = h * 31 + c}). */
    static final long LAYTHE_BASE = -8453705269825847567L;
    static final long TEKTO_BASE = -5670524702402162120L;
    static final long THATMO_BASE = 4397450043429754224L;
    /** Pinned detail-layer biome-id hashes over {@link #PIN_CHUNKS} from the 1.7.10 jar. */
    static final long LAYTHE_BIOMES = 790767696659383221L;
    static final long TEKTO_BIOMES = 2419218436554245185L;

    /** {@link SpaceGameTests#TESTS}: the suite every loader registers (a suite of its own would run on 26.1 only). */
    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function LAYTHE = TESTS.function("laythe_generator_primer", 600,
            () -> helper -> layered(helper, "laythe", LaytheChunkGenerator::new, LAYTHE_BASE, LAYTHE_BIOMES, false,
                    Set.of("minecraft:bedrock", "minecraft:stone", "minecraft:water", "minecraft:lava", "minecraft:obsidian",
                            "minecraft:ice", "minecraft:snow_block", "minecraft:packed_ice", "ntmspace:laythe_root",
                            "minecraft:grass_block", "ntmspace:laythe_silt", "hbm:ore_oil", "hbm:stone_cracked",
                            "hbm:oil_spill", "hbm:dirt_oily", "hbm:dirt_dead", "hbm:plant_dead")));

    public static final GameTests.Function TEKTO = TESTS.function("tekto_generator_primer", 600,
            () -> helper -> layered(helper, "tekto", TektoChunkGenerator::new, TEKTO_BASE, TEKTO_BIOMES, true,
                    Set.of("minecraft:bedrock", "minecraft:lava", "hbm:basalt", "hbm:basalt_smooth",
                            "ntmspace:ccl_block", "ntmspace:rubber_grass", "ntmspace:rubber_silt", "ntmspace:vinyl_sand",
                            "ntmspace:geysir_chloric", "ntmspace:ore_tekto", "hbm:ore_bedrock_oil", "ntmspace:ore_bedrock_oil", "hbm:sellafield_slaked",
                            "hbm:stone_cracked", "hbm:oil_spill")));

    public static final GameTests.Function THATMO = TESTS.function("thatmo_generator_primer", 600,
            () -> LaytheTektoThatmoGameTests::thatmo);

    private LaytheTektoThatmoGameTests() {}

    public static void init() {}

    private static void layered(GameTestHelper helper, String name, java.util.function.Function<BiomeSource, LayeredChunkGenerator> factory,
                                long pinnedBase, long pinnedBiomes, boolean bedrockOil, Set<String> allowed) {
        LayeredChunkGenerator generator = factory.apply(helper.getLevel().getChunkSource().getGenerator().getBiomeSource());
        generator.useSeed(SEED);
        generator.useSessionSeed(SESSION);
        if (!primers(helper, name, generator, generator::primer, bedrockOil, allowed)) return;
        long base;
        long biomes = 0;
        synchronized (generator) {
            LayeredBodyTerrain17 terrain = generator.terrain();
            base = baseChecksum(terrain);
            for (int[] c : PIN_CHUNKS) {
                for (int id : terrain.chunkBiomeIds(c[0], c[1])) biomes = biomes * 31 + id;
            }
        }
        if (base != pinnedBase) {
            fail(helper, name + ": base-primer height checksum " + base + ", the 1.7 code path gives " + pinnedBase);
            return;
        }
        if (biomes != pinnedBiomes) {
            fail(helper, name + ": biome map hash " + biomes + ", the 1.7.10 GenLayers give " + pinnedBiomes);
            return;
        }
        helper.succeed();
    }

    private static void thatmo(GameTestHelper helper) {
        ThatmoChunkGenerator generator = new ThatmoChunkGenerator(helper.getLevel().getChunkSource().getGenerator().getBiomeSource());
        generator.useSeed(SEED);
        generator.useSessionSeed(SESSION);
        if (!primers(helper, "thatmo", generator, generator::primer, false, Set.of("minecraft:bedrock", "minecraft:lava",
                "hbm:basalt", "minecraft:snow_block", "hbm:sellafield_slaked"))) return;
        long base;
        synchronized (generator) {
            base = baseChecksum(generator.terrain());
        }
        if (base != THATMO_BASE) {
            fail(helper, "thatmo: base-primer height checksum " + base + ", the 1.7 code path gives " + THATMO_BASE);
            return;
        }
        helper.succeed();
    }

    /** The folded base-primer checksums of {@link #PIN_CHUNKS} (call holding the generator's lock). */
    static long baseChecksum(CelestialTerrain17 terrain) {
        long all = 0;
        for (int[] c : PIN_CHUNKS) all = all * 31 + BasePrimers17.heightChecksum(BasePrimers17.of(terrain, c[0], c[1]));
        return all;
    }

    private static boolean primers(GameTestHelper helper, String name, CompatChunkGenerator generator,
                                   BiFunction<Integer, Integer, CelestialTerrain17.Primer> primers, boolean bedrockOil,
                                   Set<String> allowed) {
        for (int[] c : CHUNKS) {
            CelestialTerrain17.Primer primer = primers.apply(c[0], c[1]);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int column = (x * 16 + z) * 256;
                    Block17 floor = primer.blocks[column];
                    if (floor != Block17.BEDROCK && !(bedrockOil && floor == Block17.ORE_BEDROCK_OIL)) {
                        fail(helper, name + ": no bedrock at y 0 in chunk " + c[0] + "," + c[1] + " (" + floor + ")");
                        return false;
                    }
                    int top = -1;
                    for (int y = 0; y < 256; y++) {
                        Block17 block = primer.blocks[column + y];
                        if (block == null) continue;
                        if (!allowed.contains(block.id)) {
                            fail(helper, name + ": chunk " + c[0] + "," + c[1] + " has " + block.id + " at y " + y);
                            return false;
                        }
                        if (block.isOpaqueCube()) top = y;
                    }
                    if (top < 0) continue;
                    NoiseColumn noise = generator.getBaseColumn(c[0] * 16 + x, c[1] * 16 + z, helper.getLevel(), null);
                    String at = BuiltInRegistries.BLOCK.getKey(noise.getBlock(top).getBlock()).toString();
                    String expected = BuiltInRegistries.BLOCK.getKey(
                            LayeredChunkGenerator.state(primer.blocks[column + top], primer.metas[column + top]).getBlock()).toString();
                    if (!at.equals(expected)) {
                        fail(helper, name + ": base column says " + at + " where the primer has " + expected);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Fails the test, logging the reason too: 1.20.1 Fabric's runner prints only the failed test's name. */
    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
