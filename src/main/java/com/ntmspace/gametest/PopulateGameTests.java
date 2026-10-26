package com.ntmspace.gametest;

import com.hbm.config.GeneralConfig;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.DresChunkGenerator;
import com.ntmspace.world.DunaChunkGenerator;
import com.ntmspace.world.EveChunkGenerator;
import com.ntmspace.world.IkeChunkGenerator;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.LaytheChunkGenerator;
import com.ntmspace.world.MinmusChunkGenerator;
import com.ntmspace.world.MohoChunkGenerator;
import com.ntmspace.world.MunChunkGenerator;
import com.ntmspace.world.TektoChunkGenerator;
import com.ntmspace.world.ThatmoChunkGenerator;
import com.ntmspace.world.gen.legacy.BasePrimers17;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import com.ntmspace.world.gen.legacy.IkeTerrain17;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.MunTerrain17;
import com.ntmspace.world.populate.BodyGenerators17;
import com.ntmspace.world.populate.Populate17;
import com.ntmspace.world.populate.Populate17.Body;
import com.ntmspace.world.populate.PrimerPopulateWorld;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.CompatChunkGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's populate ({@link Populate17}) of each celestial body, off the level (a GameTest world has no space
 * dimensions): a chunk's decorator and FML generators run over a world made of the body's 1.7 primers
 * ({@link PrimerPopulateWorld}) for a fixed world and session seed.
 *
 * <ul>
 *   <li>{@code populate_<body>}: what the populate of two chunks places on the body's base primers
 *       ({@code BasePrimers17}: terrain and surface, without the map generators, whose Greg caves were seeded per game
 *       session in 1.7 and are in the port), block by block (counts per block id and a hash of every changed position),
 *       pinned from a GameTest run of this code; a change that moves one ore block fails.
 *       Laythe runs its {@code DecorateBiomeEvent.Pre} part and its generators ({@code LaytheTektoDecoration} does the rest
 *       of its decorator); Tekto runs its decorator and {@code WorldGeneratorCelestial} (its own generator is in
 *       {@code LaytheTektoDecoration}).</li>
 *   <li>{@code populate_draw_sequence}: hbm-space's random draws, independent of the terrain: each body's decorator,
 *       own generator and {@code WorldGeneratorCelestial} over an all-air world (where every draw is still made but
 *       nothing is placed), fingerprinted by the next {@code long} of each {@code Random} afterwards. Pinned from a
 *       plain-JVM program that re-states hbm-space's generator methods line by line over the port's {@code Ores17} and
 *       {@code Noise17Perlin} (scratchpad {@code m3ore/DrawOracle.java}), so a port whose call order, counts, sizes or
 *       heights drift from hbm-space's fails here.</li>
 * </ul>
 */
public final class PopulateGameTests {

    static final long SEED = 4815162342L;
    static final long SESSION = 1L;
    static final int[][] CHUNKS = {{0, 0}, {3, -2}};
    static final int[][] DRAW_CHUNKS = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}};

    /** Pinned per body: the block counts the populate of {@link #CHUNKS} placed, and the hash of every placed block. */
    static final Map<Body, String> PINNED_COUNTS = new HashMap<>();
    static final Map<Body, Long> PINNED_HASH = new HashMap<>();
    static {
        pin(Body.MUN, -5503125155400877527L,
                "{hbm:ore_bedrock_block=4, hbm:stone_depth=103, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=4469, ntmspace:cluster_aluminium=18, ntmspace:cluster_copper=28, ntmspace:cluster_iron=35, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=106, ntmspace:ore_beryllium=22, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=13, ntmspace:ore_copper=77, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=43, ntmspace:ore_gold=20, ntmspace:ore_iron=185, ntmspace:ore_lead=65, ntmspace:ore_lithium=56, ntmspace:ore_nickel=90, ntmspace:ore_niter=53, ntmspace:ore_quartz=229, ntmspace:ore_rare=33, ntmspace:ore_redstone=46, ntmspace:ore_shale=16, ntmspace:ore_sulfur=39, ntmspace:ore_thorium=48, ntmspace:ore_titanium=60, ntmspace:ore_tungsten=101, ntmspace:ore_uranium=40, ntmspace:ore_zinc=60, ntmspace:stone_resource_calcium=27}");
        pin(Body.MINMUS, -3350046285583065044L,
                "{hbm:ore_bedrock_block=4, hbm:stone_depth=103, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=4436, hbm:stone_resource_malachite=39, ntmspace:cluster_aluminium=17, ntmspace:cluster_copper=23, ntmspace:cluster_iron=33, ntmspace:cluster_titanium=11, ntmspace:minmus_regolith=1823, ntmspace:minmus_smooth=1273, ntmspace:ore_aluminium=59, ntmspace:ore_beryllium=24, ntmspace:ore_cobalt=9, ntmspace:ore_copper=730, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=18, ntmspace:ore_gold=18, ntmspace:ore_iron=152, ntmspace:ore_lead=64, ntmspace:ore_nickel=164, ntmspace:ore_niter=48, ntmspace:ore_rare=30, ntmspace:ore_redstone=45, ntmspace:ore_sulfur=37, ntmspace:ore_thorium=48, ntmspace:ore_titanium=215, ntmspace:ore_tungsten=92, ntmspace:ore_uranium=38, ntmspace:ore_zinc=52, ntmspace:stone_resource_calcium=27, ntmspace:stone_resource_conglomerate=78}");
        pin(Body.DUNA, 4329496627435051229L,
                "{hbm:ore_bedrock_block=4, hbm:stone_depth=88, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=3487, ntmspace:cluster_aluminium=15, ntmspace:cluster_copper=28, ntmspace:cluster_iron=32, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=66, ntmspace:ore_beryllium=26, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=13, ntmspace:ore_copper=80, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=19, ntmspace:ore_gold=21, ntmspace:ore_iron=196, ntmspace:ore_lead=65, ntmspace:ore_nickel=87, ntmspace:ore_niter=52, ntmspace:ore_rare=36, ntmspace:ore_redstone=52, ntmspace:ore_sulfur=37, ntmspace:ore_thorium=48, ntmspace:ore_titanium=60, ntmspace:ore_tungsten=102, ntmspace:ore_uranium=44, ntmspace:ore_zinc=162, ntmspace:stone_resource_calcium=27}");
        pin(Body.IKE, -4683054245050110578L,
                "{hbm:ore_bedrock_block=4, hbm:stone_depth=103, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=4302, ntmspace:cluster_aluminium=18, ntmspace:cluster_copper=28, ntmspace:cluster_iron=34, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=59, ntmspace:ore_asbestos=21, ntmspace:ore_beryllium=25, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=13, ntmspace:ore_coltan=1, ntmspace:ore_copper=204, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=20, ntmspace:ore_gold=18, ntmspace:ore_iron=268, ntmspace:ore_lead=62, ntmspace:ore_lithium=51, ntmspace:ore_mineral=70, ntmspace:ore_nickel=86, ntmspace:ore_niter=53, ntmspace:ore_rare=34, ntmspace:ore_redstone=38, ntmspace:ore_sulfur=36, ntmspace:ore_thorium=49, ntmspace:ore_titanium=56, ntmspace:ore_tungsten=99, ntmspace:ore_uranium=42, ntmspace:ore_zinc=59, ntmspace:stone_resource_calcium=27, ntmspace:stone_resource_conglomerate=764}");
        pin(Body.DRES, 1964165391104599260L,
                "{hbm:ore_bedrock_block=4, hbm:stone_depth=103, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=4436, ntmspace:cluster_aluminium=18, ntmspace:cluster_copper=28, ntmspace:cluster_iron=34, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=65, ntmspace:ore_beryllium=25, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=22, ntmspace:ore_coltan=12, ntmspace:ore_copper=81, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=19, ntmspace:ore_gold=21, ntmspace:ore_iron=334, ntmspace:ore_lanthanium=9, ntmspace:ore_lead=65, ntmspace:ore_nickel=90, ntmspace:ore_niobium=107, ntmspace:ore_niter=48, ntmspace:ore_rare=32, ntmspace:ore_redstone=46, ntmspace:ore_shale=26, ntmspace:ore_sulfur=38, ntmspace:ore_thorium=49, ntmspace:ore_titanium=59, ntmspace:ore_tungsten=101, ntmspace:ore_uranium=43, ntmspace:ore_zinc=59, ntmspace:stone_resource_calcium=27}");
        pin(Body.EVE, -8641025773579039144L,
                "{hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=4469, ntmspace:cluster_aluminium=18, ntmspace:cluster_copper=28, ntmspace:cluster_iron=35, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=65, ntmspace:ore_beryllium=25, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=13, ntmspace:ore_copper=80, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=20, ntmspace:ore_gold=21, ntmspace:ore_iodine=35, ntmspace:ore_iron=179, ntmspace:ore_lead=65, ntmspace:ore_nickel=91, ntmspace:ore_niobium=86, ntmspace:ore_niter=52, ntmspace:ore_rare=35, ntmspace:ore_redstone=47, ntmspace:ore_schrabidium=6, ntmspace:ore_sulfur=38, ntmspace:ore_thorium=49, ntmspace:ore_titanium=60, ntmspace:ore_tungsten=102, ntmspace:ore_uranium=43, ntmspace:ore_zinc=59, ntmspace:stone_resource_calcium=27}");
        pin(Body.MOHO, 2949115211161428687L,
                "{hbm:basalt=688, hbm:ore_basalt_fluorite=1, hbm:ore_basalt_molysite=8, hbm:ore_basalt_sulfur=16, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=4328, ntmspace:cluster_aluminium=15, ntmspace:cluster_copper=28, ntmspace:cluster_iron=29, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=64, ntmspace:ore_australium=36, ntmspace:ore_beryllium=24, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=13, ntmspace:ore_copper=62, ntmspace:ore_diamond=3, ntmspace:ore_fire=95, ntmspace:ore_fluorite=20, ntmspace:ore_glowstone=294, ntmspace:ore_gold=20, ntmspace:ore_iron=168, ntmspace:ore_lead=55, ntmspace:ore_mineral=63, ntmspace:ore_nickel=80, ntmspace:ore_niter=53, ntmspace:ore_rare=36, ntmspace:ore_redstone=48, ntmspace:ore_shale=20, ntmspace:ore_sulfur=39, ntmspace:ore_thorium=48, ntmspace:ore_titanium=60, ntmspace:ore_tungsten=101, ntmspace:ore_uranium=42, ntmspace:ore_zinc=59, ntmspace:stone_resource_calcium=25}");
        pin(Body.LAYTHE, -568718117742332150L,
                "{hbm:ore_depth_cinnabar=114, hbm:ore_depth_zirconium=57, hbm:stone_depth=205, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=2320, ntmspace:cluster_aluminium=17, ntmspace:cluster_copper=35, ntmspace:cluster_iron=23, ntmspace:cluster_titanium=11, ntmspace:ore_aluminium=48, ntmspace:ore_asbestos=8, ntmspace:ore_beryllium=43, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=10, ntmspace:ore_copper=82, ntmspace:ore_fluorite=10, ntmspace:ore_lead=77, ntmspace:ore_nickel=87, ntmspace:ore_niter=34, ntmspace:ore_rare=68, ntmspace:ore_sulfur=60, ntmspace:ore_thorium=37, ntmspace:ore_titanium=56, ntmspace:ore_tungsten=98, ntmspace:ore_uranium=51, ntmspace:ore_zinc=54, ntmspace:stone_resource_calcium=17}");
        // This base-primer pass reseeds Tekto's surface random, matching its actual per-chunk surface choices.
        pin(Body.TEKTO, -2593641461929070627L,
                "{hbm:ore_bedrock_block=4, hbm:stone_depth=103, hbm:stone_keyhole=1, hbm:stone_resource_bauxite=398, hbm:stone_resource_hematite=3689, ntmspace:cluster_aluminium=18, ntmspace:cluster_copper=28, ntmspace:cluster_iron=35, ntmspace:cluster_titanium=15, ntmspace:ore_aluminium=65, ntmspace:ore_beryllium=25, ntmspace:ore_cinnebar=3, ntmspace:ore_cobalt=13, ntmspace:ore_copper=81, ntmspace:ore_diamond=4, ntmspace:ore_fluorite=20, ntmspace:ore_gold=21, ntmspace:ore_iron=181, ntmspace:ore_lead=65, ntmspace:ore_nickel=91, ntmspace:ore_niter=53, ntmspace:ore_rare=35, ntmspace:ore_redstone=47, ntmspace:ore_sulfur=39, ntmspace:ore_thorium=49, ntmspace:ore_titanium=60, ntmspace:ore_tungsten=102, ntmspace:ore_uranium=43, ntmspace:ore_zinc=60, ntmspace:stone_resource_calcium=27}");
        pin(Body.THATMO, 1L,
                "{}");
    }

    private static void pin(Body body, long hash, String counts) {
        PINNED_HASH.put(body, hash);
        PINNED_COUNTS.put(body, counts);
    }

    /** Pinned from the plain-JVM oracle: the draw fingerprint of all bodies. */
    static final long PINNED_DRAWS = -8647435089504848248L;

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function MUN = body(Body.MUN, MunChunkGenerator::new);
    public static final GameTests.Function MINMUS = body(Body.MINMUS, MinmusChunkGenerator::new);
    public static final GameTests.Function DUNA = body(Body.DUNA, DunaChunkGenerator::new);
    public static final GameTests.Function IKE = body(Body.IKE, IkeChunkGenerator::new);
    public static final GameTests.Function DRES = body(Body.DRES, DresChunkGenerator::new);
    public static final GameTests.Function EVE = body(Body.EVE, EveChunkGenerator::new);
    public static final GameTests.Function MOHO = body(Body.MOHO, MohoChunkGenerator::new);
    public static final GameTests.Function LAYTHE = body(Body.LAYTHE, LaytheChunkGenerator::new);
    public static final GameTests.Function TEKTO = body(Body.TEKTO, TektoChunkGenerator::new);
    public static final GameTests.Function THATMO = body(Body.THATMO, ThatmoChunkGenerator::new);

    public static final GameTests.Function DRAWS = TESTS.function("populate_draw_sequence", 200,
            () -> PopulateGameTests::draws);

    public static final GameTests.Function ORE_DROPS = TESTS.function("body_ore_drops", 20,
            () -> PopulateGameTests::oreDrops);

    private PopulateGameTests() {}

    public static void init() {}

    private static GameTests.Function body(Body body, Function<BiomeSource, ? extends CompatChunkGenerator> factory) {
        return TESTS.function("populate_" + body.name().toLowerCase(java.util.Locale.ROOT), 600,
                () -> helper -> populate(helper, body, factory));
    }

    private static void populate(GameTestHelper helper, Body body, Function<BiomeSource, ? extends CompatChunkGenerator> factory) {
        CompatChunkGenerator generator = factory.apply(helper.getLevel().getChunkSource().getGenerator().getBiomeSource());
        generator.useSeed(SEED);
        LayeredBodyTerrain17 terrain = null;
        CelestialTerrain17 base17;
        if (generator instanceof LayeredChunkGenerator layered) {
            layered.useSessionSeed(SESSION);
            terrain = layered.terrain();
            base17 = terrain;
        } else if (generator instanceof ThatmoChunkGenerator thatmo) {
            thatmo.useSessionSeed(SESSION);
            base17 = thatmo.terrain();
        } else if (body == Body.MUN) {
            base17 = new MunTerrain17(SEED, 100);
        } else {
            base17 = new IkeTerrain17(SEED);
        }

        // the base primers (no map generators): Greg's caves were seeded per game session in 1.7 and are in the port
        Map<Long, CelestialTerrain17.Primer> primers = new HashMap<>();
        PrimerPopulateWorld.Base base = (x, y, z) -> {
            if (y < 0 || y > 255) return Blocks.AIR.defaultBlockState();
            CelestialTerrain17.Primer primer = primers.computeIfAbsent(((long) (x >> 4) << 32) ^ ((z >> 4) & 0xFFFFFFFFL), key -> {
                synchronized (generator) {
                    // the full primer first: it reseeds the surface pass's Math.random() stand-in for this chunk, which
                    // BasePrimers17 alone leaves wherever the last chunk left it
                    base17.provide(x >> 4, z >> 4);
                    return BasePrimers17.of(base17, x >> 4, z >> 4);
                }
            });
            int index = ((x & 15) * 16 + (z & 15)) * 256 + y;
            Block17 block = primer.blocks[index];
            return block == null ? Blocks.AIR.defaultBlockState() : LayeredChunkGenerator.state(block, primer.metas[index]);
        };

        Map<String, Integer> counts = new TreeMap<>();
        long hash = 1;
        for (int[] c : CHUNKS) {
            PrimerPopulateWorld world = new PrimerPopulateWorld(base, c[0], c[1], 0L);
            if (body == Body.LAYTHE) {
                Populate17.pre(body, world, SEED, c[0] * 16, c[1] * 16, Populate17.populateRandom(SEED, c[0], c[1]));
            } else {
                Populate17.decorator(body, world, SEED, c[0], c[1]);
            }
            Populate17.generators(body, world, SEED, c[0], c[1], terrain, generator, body != Body.TEKTO);

            TreeMap<Long, BlockState> placed = new TreeMap<>();
            for (Map.Entry<Long, BlockState> entry : world.written().entrySet()) {
                long key = entry.getKey();
                BlockState was = base.at(PrimerPopulateWorld.x(key), PrimerPopulateWorld.y(key), PrimerPopulateWorld.z(key));
                if (entry.getValue() != was) placed.put(key, entry.getValue());
            }
            for (Map.Entry<Long, BlockState> entry : placed.entrySet()) {
                String id = BuiltInRegistries.BLOCK.getKey(entry.getValue().getBlock()).toString();
                counts.merge(id, 1, Integer::sum);
                hash = hash * 31 + entry.getKey();
                hash = hash * 31 + entry.getValue().toString().hashCode();
            }
        }

        String actual = counts.toString();
        NtmSpace.LOG.info("populate_{}: {} hash {}L", body.name().toLowerCase(java.util.Locale.ROOT), actual, hash);
        String pinnedCounts = PINNED_COUNTS.get(body);
        Long pinnedHash = PINNED_HASH.get(body);
        if (!actual.equals(pinnedCounts)) {
            fail(helper, body + ": populate placed " + actual + ", pinned " + pinnedCounts);
            return;
        }
        if (pinnedHash == null || hash != pinnedHash) {
            fail(helper, body + ": populate hash " + hash + "L, pinned " + pinnedHash);
            return;
        }
        helper.succeed();
    }

    /** The draw fingerprint of every body over an all-air world (see the class comment). */
    public static long drawFingerprint() {
        BlockState air = Blocks.AIR.defaultBlockState();
        long hash = 1;
        for (Body body : Body.values()) {
            for (int[] c : DRAW_CHUNKS) {
                int k = c[0] * 16;
                int l = c[1] * 16;
                PrimerPopulateWorld world = new PrimerPopulateWorld((x, y, z) -> air, c[0], c[1], 0L);

                Random decorator = Populate17.populateRandom(SEED, c[0], c[1]);
                if (body == Body.LAYTHE) {
                    Populate17.pre(body, world, SEED, k, l, decorator);
                } else {
                    Populate17.decorator(body, world, SEED, c[0], c[1], decorator);
                }
                hash = hash * 31 + decorator.nextLong();

                long chunkSeed = Populate17.generatorSeed(SEED, c[0], c[1]);
                if (body != Body.TEKTO && body != Body.THATMO) {
                    Random own = new Random(chunkSeed);
                    BodyGenerators17.generate(body, world, own, k, l, null, world);
                    hash = hash * 31 + own.nextLong();
                }
                Random celestial = new Random(chunkSeed);
                Populate17.celestial(body, world, SEED, celestial, k, l);
                hash = hash * 31 + celestial.nextLong();
            }
        }
        return hash;
    }

    private static void draws(GameTestHelper helper) {
        long hash = drawFingerprint();
        NtmSpace.LOG.info("populate_draw_sequence: {}L", hash);
        if (hash != PINNED_DRAWS) {
            fail(helper, "draw fingerprint " + hash + "L, hbm-space's generators (plain-JVM oracle) give " + PINNED_DRAWS + "L");
            return;
        }
        helper.succeed();
    }

    /**
     * The per-body ore family: a body state drops its own body's item where 1.7's {@code damageDropped} kept the body
     * (an ore that drops itself), the material otherwise; each body item places its state; the Kerbin form of an ore HBM
     * has is HBM's own block for the world generators.
     */
    private static void oreDrops(GameTestHelper helper) {
        java.util.List<String> wrong = new java.util.ArrayList<>();
        net.minecraft.core.BlockPos rel = new net.minecraft.core.BlockPos(1, 1, 1);
        net.minecraft.core.BlockPos pos = helper.absolutePos(rel);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();

        String[][] cases = {
                {"ore_uranium", "duna", GeneralConfig.enableRawOres ? "hbm:raw_uranium" : "ntmspace:ore_uranium_duna"},
                {"ore_nickel", "minmus", GeneralConfig.enableRawOres ? "ntmspace:raw_nickel" : "ntmspace:ore_nickel_minmus"},
                {"ore_iron", "mun", "minecraft:raw_iron"},
                {"ore_sulfur", "eve", "hbm:sulfur"},
                {"ore_glowstone", "moho", "minecraft:glowstone_dust"},
                {"ore_gas", "eve", "ntmspace:ore_gas_empty"},
                {"ore_brine", "minmus", "ntmspace:ore_brine_empty"},
        };
        for (String[] c : cases) {
            BlockState state = com.ntmspace.registry.BodyOreBlocks.state(c[0],
                    com.ntmspace.block.BlockBodyOre.Body.valueOf(c[1].toUpperCase(java.util.Locale.ROOT)));
            if (state == null) {
                wrong.add(c[0] + " is not registered");
                continue;
            }
            helper.setBlock(rel, state);
            java.util.List<net.minecraft.world.item.ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, level, pos, null);
            boolean found = drops.stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(c[2]));
            if (!found) wrong.add(c[0] + "[body=" + c[1] + "] drops " + drops + ", not " + c[2]);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.parse("ntmspace:" + c[0] + "_" + c[1])).orElse(null);
            if (!(item instanceof com.ntmspace.block.BodyOreItem bodyItem) || bodyItem.body() != state.getValue(com.ntmspace.block.BlockBodyOre.BODY)) {
                wrong.add("ntmspace:" + c[0] + "_" + c[1] + " is not that body's item");
            }
        }
        helper.setBlock(rel, Blocks.AIR.defaultBlockState());
        BlockState kerbin = com.ntmspace.registry.BodyOreBlocks.state("ore_uranium", com.ntmspace.block.BlockBodyOre.Body.KERBIN);
        if (kerbin == null || !BuiltInRegistries.BLOCK.getKey(kerbin.getBlock()).toString().equals("hbm:ore_uranium")) {
            wrong.add("the Kerbin form of ore_uranium is " + kerbin + ", not hbm:ore_uranium");
        }
        if (!wrong.isEmpty()) {
            fail(helper, String.join("; ", wrong));
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
