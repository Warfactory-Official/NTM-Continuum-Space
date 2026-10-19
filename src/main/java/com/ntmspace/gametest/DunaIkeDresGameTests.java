package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.DresChunkGenerator;
import com.ntmspace.world.DunaChunkGenerator;
import com.ntmspace.world.IkeChunkGenerator;
import com.ntmspace.world.LayeredChunkGenerator;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Duna, Ike and Dres generators on their own (a GameTest world has no datapack dimensions; in a world,
 * {@code /ntmspace check <body>}). For a fixed world seed and session seed, five chunks' primers hold only the body's
 * blocks, have bedrock at y 0, agree with the generator's base column, and their column heights (and, for Duna and Dres,
 * their 1.7 detail biome ids) hash to the values the 1.7 code path gives: pinned from a plain-JVM run of the same 1.7
 * terrain classes (scratchpad {@code m3did/terrain/Pins.java}), whose biome maps are proven equal to the 1.7.10 jar
 * ({@code m3did/oracle/LayerOracle.java}). A change to the terrain code that moves one block of height fails here.
 */
public final class DunaIkeDresGameTests {

    static final long SEED = 4815162342L;
    static final long SESSION = 1L;
    static final int[][] CHUNKS = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}};

    /** Pinned column-height hashes ({@link #heightHash}) and detail-biome hashes ({@link #biomeHash}). */
    static final long DUNA_HEIGHTS = 1242985143435258862L;
    static final long DUNA_BIOMES = 8345122696892135294L;
    static final long IKE_HEIGHTS = 6258748169365973958L;
    static final long DRES_HEIGHTS = -640628364821338370L;
    static final long DRES_BIOMES = -2090638199153602434L;

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function DUNA = TESTS.function("duna_generator_primer", 400,
            () -> helper -> {
                DunaChunkGenerator generator = new DunaChunkGenerator(source(helper));
                generator.useSeed(SEED);
                generator.useSessionSeed(SESSION);
                body(helper, "duna", generator, generator::primer, DUNA_HEIGHTS, DUNA_BIOMES, generator.terrain(),
                        Set.of("minecraft:bedrock", "minecraft:gravel", "minecraft:snow_block", "minecraft:orange_terracotta",
                                "minecraft:light_gray_terracotta", "minecraft:brown_terracotta", "minecraft:red_terracotta",
                                "ntmspace:duna_rock", "ntmspace:duna_sands", "ntmspace:duna_cobble", "ntmspace:ferric_clay",
                                "ntmspace:dry_ice", "ntmspace:stone_resource_calcium", "ntmspace:ore_oil", "hbm:basalt",
                                "hbm:oil_spill", "hbm:stone_cracked"));
            });

    public static final GameTests.Function IKE = TESTS.function("ike_generator_primer", 400,
            () -> helper -> {
                IkeChunkGenerator generator = new IkeChunkGenerator(source(helper));
                generator.useSeed(SEED);
                body(helper, "ike", generator, generator::primer, IKE_HEIGHTS, 0L, null,
                        Set.of("minecraft:bedrock", "minecraft:gravel", "minecraft:lava", "ntmspace:ike_stone",
                                "ntmspace:ike_regolith", "ntmspace:ore_brine", "hbm:sellafield_0", "hbm:sellafield_1",
                                "hbm:sellafield_2", "hbm:sellafield_3", "hbm:sellafield_4", "hbm:sellafield_5",
                                "hbm:sellafield_slaked", "hbm:oil_spill", "hbm:stone_cracked"));
            });

    public static final GameTests.Function DRES = TESTS.function("dres_generator_primer", 400,
            () -> helper -> {
                DresChunkGenerator generator = new DresChunkGenerator(source(helper));
                generator.useSeed(SEED);
                generator.useSessionSeed(SESSION);
                body(helper, "dres", generator, generator::primer, DRES_HEIGHTS, DRES_BIOMES, generator.terrain(),
                        Set.of("minecraft:bedrock", "minecraft:lava", "ntmspace:dres_rock", "hbm:sellafield_slaked"));
            });

    private DunaIkeDresGameTests() {}

    public static void init() {}

    private static BiomeSource source(GameTestHelper helper) {
        return helper.getLevel().getChunkSource().getGenerator().getBiomeSource();
    }

    /** The column heights of the test chunks (the topmost opaque block, -1 for none), hashed in order. */
    public static long heightHash(BiFunction<Integer, Integer, CelestialTerrain17.Primer> primers) {
        long hash = 1;
        for (int[] c : CHUNKS) {
            CelestialTerrain17.Primer primer = primers.apply(c[0], c[1]);
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

    /** The 1.7 detail biome ids of the test chunks, hashed in order. */
    public static long biomeHash(LayeredBodyTerrain17 terrain) {
        long hash = 1;
        for (int[] c : CHUNKS) {
            for (int id : terrain.chunkBiomeIds(c[0], c[1])) hash = hash * 31 + id;
        }
        return hash;
    }

    private static void body(GameTestHelper helper, String name, CompatChunkGenerator generator,
                             BiFunction<Integer, Integer, CelestialTerrain17.Primer> primers, long pinnedHeights,
                             long pinnedBiomes, LayeredBodyTerrain17 layered, Set<String> allowed) {
        for (int[] c : CHUNKS) {
            CelestialTerrain17.Primer primer = primers.apply(c[0], c[1]);
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
                        BlockState state = LayeredChunkGenerator.state(block, primer.metas[column + y]);
                        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                        if (!allowed.contains(id)) {
                            fail(helper, name + ": chunk " + c[0] + "," + c[1] + " has " + id + " at y " + y);
                            return;
                        }
                        if (block.isOpaqueCube()) top = y;
                    }
                    if (top < 0) continue;
                    NoiseColumn noise = generator.getBaseColumn(c[0] * 16 + x, c[1] * 16 + z, helper.getLevel(), null);
                    BlockState expected = LayeredChunkGenerator.state(primer.blocks[column + top], primer.metas[column + top]);
                    if (noise.getBlock(top) != expected) {
                        fail(helper, name + ": base column says " + noise.getBlock(top) + " where the primer has " + expected);
                        return;
                    }
                }
            }
        }
        long heights = heightHash(primers);
        if (heights != pinnedHeights) {
            fail(helper, name + ": column-height hash " + heights + ", the 1.7 code path gives " + pinnedHeights);
            return;
        }
        if (layered != null) {
            long biomes = biomeHash(layered);
            if (biomes != pinnedBiomes) {
                fail(helper, name + ": detail-biome hash " + biomes + ", the 1.7 code path gives " + pinnedBiomes);
                return;
            }
        }
        helper.succeed();
    }

    /** Fails the test, logging the reason too: 1.20.1 Fabric's runner prints only the failed test's name. */
    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
