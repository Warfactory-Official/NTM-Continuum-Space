package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.EveChunkGenerator;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.MinmusChunkGenerator;
import com.ntmspace.world.MohoChunkGenerator;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import dev.golem.core.gametest.GameTests;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * Eve, Moho and Minmus generators on their own (a GameTest world has no datapack dimensions; in a world,
 * {@code /ntmspace check <body>}). For a fixed world seed and session seed, five chunks' primers hold only the body's
 * blocks, have bedrock at y 0, agree with the generator's base column, and their column heights hash to the value the
 * 1.7 code path gives (pinned from {@code scratchpad/mc17/layers}'s plain-JVM run of the same terrain classes, whose
 * biome maps are proven equal to the 1.7.10 jar).
 */
public final class PlanetGameTests {

    static final long SEED = 4815162342L;
    static final long SESSION = 1L;
    static final int[][] CHUNKS = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}};

    /** Pinned column-height hashes (see {@link #heightHash}). */
    static final long EVE_HASH = -622044020059025378L;
    static final long MOHO_HASH = -5947557171282599261L;
    static final long MINMUS_HASH = -6705813515984441108L;

    public static final GameTests.Suite TESTS = GameTests.suite(NtmSpace.MOD_ID);

    public static final GameTests.Function EVE = TESTS.function("eve_generator_primer", 400,
            () -> helper -> body(helper, "eve", EveChunkGenerator::new, EVE_HASH, Set.of("minecraft:bedrock",
                    "minecraft:lava", "ntmspace:eve_rock", "ntmspace:eve_silt", "hbm:basalt_smooth", "hbm:basalt",
                    "ntmspace:ore_gas", "ntmspace:mercury_block", "hbm:oil_spill", "hbm:stone_cracked")));

    public static final GameTests.Function MOHO = TESTS.function("moho_generator_primer", 400,
            () -> helper -> body(helper, "moho", MohoChunkGenerator::new, MOHO_HASH, Set.of("minecraft:bedrock",
                    "minecraft:lava", "ntmspace:moho_stone", "ntmspace:moho_regolith", "hbm:basalt", "hbm:volcano_core")));

    public static final GameTests.Function MINMUS = TESTS.function("minmus_generator_primer", 400,
            () -> helper -> body(helper, "minmus", MinmusChunkGenerator::new, MINMUS_HASH, Set.of("minecraft:bedrock",
                    "minecraft:gravel", "ntmspace:minmus_stone", "ntmspace:minmus_regolith", "ntmspace:minmus_smooth",
                    "ntmspace:ore_brine", "hbm:oil_spill", "hbm:stone_cracked")));

    private PlanetGameTests() {}

    public static void init() {}

    /** The column heights of the test chunks (the topmost opaque block, -1 for none), hashed in order. */
    public static long heightHash(Function<int[], CelestialTerrain17.Primer> primers) {
        long hash = 1;
        for (int[] c : CHUNKS) {
            CelestialTerrain17.Primer primer = primers.apply(c);
            for (int column = 0; column < 256; column++) {
                int top = -1;
                for (int y = 0; y < 256; y++) {
                    Block17 block = primer.blocks[column * 256 + y];
                    if (block != null && block.isOpaqueCube()) top = y;
                }
                hash = hash * 31 + top;
            }
        }
        return hash;
    }

    private static void body(GameTestHelper helper, String name, Function<BiomeSource, LayeredChunkGenerator> factory,
                             long pinned, Set<String> allowed) {
        LayeredChunkGenerator generator = factory.apply(helper.getLevel().getChunkSource().getGenerator().getBiomeSource());
        generator.useSeed(SEED);
        generator.useSessionSeed(SESSION);
        for (int[] c : CHUNKS) {
            CelestialTerrain17.Primer primer = generator.primer(c[0], c[1]);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int column = (x * 16 + z) * 256;
                    if (primer.blocks[column] != Block17.BEDROCK) {
                        fail(helper, name + ": no bedrock at y 0 in chunk " + c[0] + "," + c[1]);
                        return;
                    }
                    int top = -1;
                    for (int y = 0; y < 256; y++) {
                        Block17 block = primer.blocks[column + y];
                        if (block == null) continue;
                        if (!allowed.contains(block.id)) {
                            fail(helper, name + ": chunk " + c[0] + "," + c[1] + " has " + block.id + " at y " + y);
                            return;
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
                        return;
                    }
                }
            }
        }
        long hash = heightHash(c -> generator.primer(c[0], c[1]));
        if (hash != pinned) {
            fail(helper, name + ": column-height hash " + hash + ", the 1.7 code path gives " + pinned);
            return;
        }
        helper.succeed();
    }

    /** Fails the test, logging the reason too: 1.20.1 Fabric's runner prints only the failed test's name. */
    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
