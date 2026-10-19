package com.ntmspace.gametest;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.config.DungeonConfig;
import com.hbm.config.StructureConfig;
import com.hbm.config.WorldConfig;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.world.gen.ConfigSpreadPlacement;
import com.hbm.world.gen.LegacyDungeonBlocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.hbm.world.gen.nbt.NBTStructure;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.StructureBlocks;
import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import com.ntmspace.world.populate.BedrockDeposit;
import com.ntmspace.world.populate.Populate17;
import com.ntmspace.world.populate.PopulateWorld;
import com.ntmspace.world.structure.BodyDungeonStructure;
import com.ntmspace.world.structure.BodyStructures;
import com.ntmspace.world.structure.PlanetFeatures17;
import com.ntmspace.world.structure.Random17;
import com.ntmspace.world.structure.SpaceStructures;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTests;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * hbm-space's planet structures (M3 structures family):
 * <ul>
 *   <li>{@code structure_pick_matches_17}: for a fixed seed, every region of a 25 x 25 window on each body, the chunk the
 *       structure set's placement ({@code hbm:config_spread}, the body's salt) offers and the spawn
 *       {@link BodyStructures#pick} lets win there, against a verbatim 1.7 {@code GenStructure.getSpawnAtCoords} +
 *       {@code findSpawn} on a {@code java.util.Random} over hbm-space's spawn lists re-stated here (weights, null
 *       weights, biome rules, registration order), plus the start random's next draw. The biome is the body's 1.7 detail
 *       biome at the chunk centre. Pinned: the winners per body (counts) and the first start chunk of each structure.</li>
 *   <li>{@code structure_files_and_palettes}: every planet file loaded, every spawn of the lists registered, and every
 *       block of every palette resolves (HBM's legacy palette, {@code ntmspace:} names for hbm-space's blocks).</li>
 *   <li>{@code structure_dungeon_sites}: the tomb and spaceship sites the structure replays equal the populate's own
 *       draws ({@code Populate17.generators}) and a verbatim 1.7 {@code HbmWorldGen} roll; {@link Random17} is
 *       {@code java.util.Random}; the tomb builds, the spaceship builds on accepted ground and not on other ground.</li>
 *   <li>{@code structure_ike_pedestal} and {@code structure_duna_lantern}: the artifact's pedestal holds a divine shard
 *       after Ike's re-stocking; the broken lantern stands five tall, broken, with its loot as the coin says.</li>
 * </ul>
 */
public final class StructureGameTests {

    static final long SEED = 4815162342L;
    static final int WINDOW = 12;

    /** Pinned per body from this test's verbatim 1.7 path: winner counts in the window, then each structure's first start. */
    static final Map<String, String> PINNED = new HashMap<>();
    static {
        // from this test's verbatim 1.7 path (structureMaxChunks 16, structureMinChunks 4: the hbm.cfg defaults; seed 4815162342)
        PINNED.put("mun", "{meteor_dungeon=17, mun_base=159, null=449} {meteor_dungeon=-182,122, mun_base=-181,-191}");
        PINNED.put("duna", "{duna_comms=95, meteor_dungeon=21, null=509} {duna_comms=-184,-166, meteor_dungeon=-159,73}");
        PINNED.put("ike", "{ike_artifact=149, meteor_dungeon=41, null=435} {ike_artifact=-190,-107, meteor_dungeon=-149,-74}");
        PINNED.put("eve", "{meteor_dungeon=17, null=608} {meteor_dungeon=-165,176}");
        PINNED.put("dres", "{dres_rbmk=101, meteor_dungeon=9, null=515} {dres_rbmk=-184,-191, meteor_dungeon=-167,-44}");
        PINNED.put("moho", "{meteor_dungeon=16, moho_base=94, null=515} {meteor_dungeon=-136,7, moho_base=-186,-170}");
        PINNED.put("minmus", "{meteor_dungeon=13, minmus_base=162, null=450} {meteor_dungeon=-169,-10, minmus_base=-191,-153}");
        PINNED.put("laythe", "{-=306, laythe_crashed_vertibird=53, laythe_nuke_sub=199, laythe_vertibird=60, meteor_dungeon=7} {laythe_crashed_vertibird=-181,-117, laythe_nuke_sub=-186,-183, laythe_vertibird=-191,-174, meteor_dungeon=-166,-133}");
        PINNED.put("tekto", "{meteor_dungeon=24, null=601} {meteor_dungeon=-182,59}");
        PINNED.put("thatmo", "{meteor_dungeon=625} {meteor_dungeon=-185,-184}");
    }

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function PICK = TESTS.function("structure_pick_matches_17", 400,
            () -> StructureGameTests::pick);
    public static final GameTests.Function FILES = TESTS.function("structure_files_and_palettes", 100,
            () -> StructureGameTests::files);
    public static final GameTests.Function DUNGEONS = TESTS.function("structure_dungeon_sites", 400,
            () -> StructureGameTests::dungeons);
    public static final GameTests.Function PEDESTAL = TESTS.function("structure_ike_pedestal", 40,
            () -> StructureGameTests::pedestal);
    public static final GameTests.Function LANTERN = TESTS.function("structure_duna_lantern", 40,
            () -> StructureGameTests::lantern);

    private StructureGameTests() {}

    public static void init() {}

    // ------------------------------------------------------------------------------------------------ the pick

    /** hbm-space's spawn lists, re-stated from its generator constructors (name, weight, rule on (biome, root, variation)). */
    private record Spawn17(String name, int weight, Predicate<BodyStructures.Biome> canSpawn) {}

    private static List<Spawn17> list17(BodyStructures.Body body) {
        List<Spawn17> list = new ArrayList<>();
        Predicate<BodyStructures.Biome> any = b -> true;
        // WorldGeneratorCelestial: canSpawn = biome -> biome.rootHeight >= 0, spawnWeight default 1
        list.add(new Spawn17("meteor_dungeon", 1, b -> b.rootHeight() >= 0));
        switch (body) {
            case MUN -> { list.add(new Spawn17("mun_base", 6, any)); list.add(new Spawn17("null", 18, any)); }
            case DUNA -> { list.add(new Spawn17("duna_comms", 6, b -> b.heightVariation() < 0.1F)); list.add(new Spawn17("null", 18, any)); }
            case IKE -> { list.add(new Spawn17("ike_artifact", 4, any)); list.add(new Spawn17("null", 12, any)); }
            case EVE, TEKTO -> list.add(new Spawn17("null", 24, any));
            case DRES -> { list.add(new Spawn17("dres_rbmk", 8, b -> b.name().equals("plains"))); list.add(new Spawn17("null", 16, any)); }
            case MOHO -> { list.add(new Spawn17("moho_base", 4, any)); list.add(new Spawn17("null", 20, any)); }
            case MINMUS -> { list.add(new Spawn17("minmus_base", 6, any)); list.add(new Spawn17("null", 18, any)); }
            case LAYTHE -> {
                list.add(new Spawn17("laythe_nuke_sub", 6, b -> b.name().equals("ocean")));
                list.add(new Spawn17("laythe_vertibird", 6, b -> b.rootHeight() >= 0));
                list.add(new Spawn17("laythe_crashed_vertibird", 6, b -> b.rootHeight() >= 0));
            }
            case THATMO -> { }
        }
        return list;
    }

    /** 1.7 {@code GenStructure.getSpawnAtCoords} (no custom-coordinate spawns) and {@code findSpawn}, verbatim; "-" for none. */
    private static String verbatim17(BodyStructures.Body body, long seed, int chunkX, int chunkZ, BodyStructures.Biome biome, long[] next) {
        int max = StructureConfig.structureMaxChunks;
        int min = StructureConfig.structureMinChunks;
        Random rand = new Random();
        int x = chunkX;
        int z = chunkZ;

        if (x < 0) x -= max - 1;
        if (z < 0) z -= max - 1;

        x /= max;
        z /= max;
        rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L + seed + (long) 996996996 - body.dimension);
        x *= max;
        z *= max;
        x += rand.nextInt(max - min);
        z += rand.nextInt(max - min);

        if (chunkX == x && chunkZ == z) {
            int total = 0;
            List<Spawn17> filtered = new ArrayList<>();
            for (Spawn17 spawn : list17(body)) {
                if (spawn.canSpawn().test(biome)) {
                    filtered.add(spawn);
                    total += spawn.weight();
                }
            }
            if (total == 0) return "-";
            int weight = rand.nextInt(total);
            for (Spawn17 spawn : filtered) {
                weight -= spawn.weight();
                if (weight < 0) {
                    next[0] = rand.nextLong();
                    return spawn.name();
                }
            }
        }
        return "-";
    }

    private static BodyStructures.Biome biome(BodyStructures.Body body, BiomeLayers17 layers, int chunkX, int chunkZ) {
        if (layers == null) return body.biomes().get(0);
        int[] ids = layers.detail.getInts(chunkX * 16, chunkZ * 16, 16, 16);
        return body.biomeOfLegacyId(ids[8 + 8 * 16]);
    }

    private static void pick(GameTestHelper helper) {
        int max = StructureConfig.structureMaxChunks;
        int min = StructureConfig.structureMinChunks;
        StructureConfig.structureMaxChunks = 16; // the pins' config: hbm.cfg 5.01/5.02 defaults
        StructureConfig.structureMinChunks = 4;
        try {
            pickWindow(helper);
        } finally {
            StructureConfig.structureMaxChunks = max;
            StructureConfig.structureMinChunks = min;
        }
    }

    private static void pickWindow(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        Map<String, String> summaries = new LinkedHashMap<>();
        int spacing = BodyStructures.spacing();
        for (BodyStructures.Body body : BodyStructures.Body.values()) {
            BiomeLayers17 layers = body.layered == null ? null : body.layered.layers().apply(SEED);
            ConfigSpreadPlacement placement = new ConfigSpreadPlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT,
                    1.0F, body.salt(), Optional.empty(), "ntm");
            Map<String, Integer> counts = new TreeMap<>();
            Map<String, String> first = new TreeMap<>();
            for (int rx = -WINDOW; rx <= WINDOW; rx++) {
                for (int rz = -WINDOW; rz <= WINDOW; rz++) {
                    ChunkPos candidate = placement.getPotentialStructureChunk(SEED, rx * spacing, rz * spacing);
                    int cx = candidate.getMinBlockX() >> 4;
                    int cz = candidate.getMinBlockZ() >> 4;
                    BodyStructures.Biome biome = biome(body, layers, cx, cz);
                    if (biome == null) {
                        wrong.add(body.id() + " " + cx + "," + cz + ": no 1.7 biome");
                        continue;
                    }
                    long[] next17 = {0L};
                    String old = verbatim17(body, SEED, cx, cz, biome, next17);
                    BodyStructures.Pick p = BodyStructures.pick(body, SEED, cx, cz, biome);
                    String now = p == null ? "-" : String.valueOf(p.member().spawn());
                    if (!old.equals(now)) {
                        wrong.add(body.id() + " " + cx + "," + cz + " (" + biome.name() + "): port " + now + ", 1.7 " + old);
                        continue;
                    }
                    if (p != null && p.random().nextLong() != next17[0]) wrong.add(body.id() + " " + cx + "," + cz + ": the start random differs from 1.7's");
                    counts.merge(now, 1, Integer::sum);
                    if (!now.equals("-") && !now.equals("null")) first.putIfAbsent(now, cx + "," + cz);
                }
            }
            summaries.put(body.id(), counts + " " + first);
        }
        for (Map.Entry<String, String> e : summaries.entrySet()) {
            NtmSpace.LOG.info("structure_pick {} {}", e.getKey(), e.getValue());
            String pinned = PINNED.get(e.getKey());
            if (pinned != null && !pinned.equals(e.getValue())) wrong.add(e.getKey() + ": " + e.getValue() + ", pinned " + pinned);
        }
        if (PINNED.isEmpty()) wrong.add("no pins yet (see the structure_pick lines)");
        finish(helper, wrong);
    }

    // ------------------------------------------------------------------------------------------------ files

    private static void files(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (NBTStructure file : SpaceStructures.Files.ALL) {
            if (!file.isLoaded()) {
                wrong.add(file.getName() + " did not load");
                continue;
            }
            String path = "/assets/" + NtmSpace.MOD_ID + "/" + file.getName() + ".nbt";
            try (InputStream in = StructureGameTests.class.getResourceAsStream(path)) {
                if (in == null) {
                    wrong.add(path + " missing");
                    continue;
                }
                CompoundTag data = NbtFiles.readCompressed(in);
                ListTag palette = NbtFiles.list(data, "palette");
                for (int i = 0; i < palette.size(); i++) {
                    CompoundTag entry = Tags.getCompoundOrEmpty(palette, i);
                    String name = Tags.getStringOr(entry, "Name", "minecraft:air");
                    if (name.equals("minecraft:air") || name.contains("wand_")) continue; // air; structure-wand placeholders
                    CompoundTag props = NbtFiles.compoundOrNull(entry, "Properties");
                    int meta = props == null ? 0 : Integer.parseInt(Tags.getStringOr(props, "meta", "0"));
                    LegacyStructurePalette.Resolved r = LegacyStructurePalette.resolve(name, meta);
                    if (r.kind() == LegacyStructurePalette.Kind.STATE && r.state().isAir()) {
                        wrong.add(file.getName() + ": " + name + ":" + meta + " has no port block");
                    }
                }
            } catch (Exception e) {
                wrong.add(path + ": " + e);
            }
        }
        for (BodyStructures.Body body : BodyStructures.Body.values()) {
            for (BodyStructures.Member member : body.members()) {
                if (member.spawn() != null && NBTStructure.getStructure(member.spawn()) == null) {
                    wrong.add(body.id() + ": spawn " + member.spawn() + " is not registered");
                }
            }
        }
        finish(helper, wrong);
    }

    // ------------------------------------------------------------------------------------------------ dungeons

    /** A populate world placing nothing that records the populate's tomb call and the next draw of its random. */
    private static final class Recording extends PopulateWorld {
        int x = Integer.MIN_VALUE, z;
        long next;

        Recording(int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
        }

        @Override public boolean exists(int x, int y, int z) { return false; }
        @Override protected BlockState read(int x, int y, int z) { return Blocks.AIR.defaultBlockState(); }
        @Override protected void write(int x, int y, int z, BlockState state) {}
        @Override public void bedrockOre(int x, int y, int z, BedrockDeposit deposit, int shape) {}
        @Override public Random worldRandom() { return new Random(0L); }

        @Override
        public void dungeonSite(String kind, int x, int z, Random rand) {
            this.x = x;
            this.z = z;
            this.next = rand.nextLong();
        }
    }

    /** A flat test terrain: {@code ground} up to y 63, air above. */
    private static LegacyDungeonWorld.Terrain flat(LegacyBlock ground) {
        return new LegacyDungeonWorld.Terrain() {
            @Override public LegacyBlock block(int x, int y, int z) { return y < 64 ? ground : LegacyDungeonBlocks.Blocks.air; }
            @Override public int height(int x, int z) { return 64; }
            @Override public int minY() { return 0; }
        };
    }

    private static void dungeons(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();

        // Random17 is java.util.Random, and its vanilla hand-over continues the stream
        Random a = new Random(SEED);
        Random17 b = new Random17(SEED);
        for (int i = 0; i < 2000; i++) {
            if (a.nextInt(1000) != b.nextInt(1000) || a.nextLong() != b.nextLong() || a.nextFloat() != b.nextFloat()
                    || a.nextDouble() != b.nextDouble() || a.nextBoolean() != b.nextBoolean()) {
                wrong.add("Random17 diverges from java.util.Random at draw " + i);
                break;
            }
        }
        RandomSource handed = b.source();
        if (handed.nextInt(4000) != a.nextInt(4000) || handed.nextLong() != a.nextLong() || handed.nextFloat() != a.nextFloat()) {
            wrong.add("Random17.source() does not continue the stream");
        }

        int pyramid = DungeonConfig.pyramidStructure;
        int spaceship = DungeonConfig.spaceshipStructure;
        int meteorite = WorldConfig.meteoriteSpawn;
        try {
            // every Ike chunk rolls a tomb: the structure's replay against the populate's own call
            DungeonConfig.pyramidStructure = 1;
            int[][] chunks = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}};
            for (int[] c : chunks) {
                BodyDungeonStructure.Site site = BodyDungeonStructure.site(BodyDungeonStructure.Kind.ANCIENT_TOMB,
                        Populate17.Body.IKE, SEED, c[0], c[1], null);
                Recording populate = new Recording(c[0], c[1]);
                Populate17.generators(Populate17.Body.IKE, populate, SEED, c[0], c[1], null, populate, true);
                if (site == null || populate.x == Integer.MIN_VALUE) {
                    wrong.add("tomb chunk " + c[0] + "," + c[1] + ": replay " + site + ", populate called " + (populate.x != Integer.MIN_VALUE));
                    continue;
                }
                if (site.x() != populate.x || site.z() != populate.z || Random17.source(site.state()).nextLong() != populate.next) {
                    wrong.add("tomb chunk " + c[0] + "," + c[1] + ": replay " + site + ", populate " + populate.x + "," + populate.z);
                }
            }

            // the spaceship roll against a verbatim 1.7 HbmWorldGen.generateSurface (no meteorite, then a meteorite in the air)
            DungeonConfig.spaceshipStructure = 1;
            LegacyDungeonWorld.Terrain air = flat(LegacyDungeonBlocks.Blocks.air);
            for (int m : new int[] {0, 1}) {
                WorldConfig.meteoriteSpawn = m;
                for (int[] c : chunks) {
                    BodyDungeonStructure.Site site = BodyDungeonStructure.site(BodyDungeonStructure.Kind.SPACESHIP,
                            Populate17.Body.MUN, SEED, c[0], c[1], air);
                    Random rand = new Random(Populate17.generatorSeed(SEED, c[0], c[1]));
                    int i = c[0] * 16, j = c[1] * 16;
                    if (m > 0 && rand.nextInt(m) == 0) {
                        rand.nextInt(16);
                        rand.nextInt(16);
                        rand.nextInt(10); // 64 - r, block two below is air: no meteorite
                    }
                    rand.nextInt(1);
                    int x = i + rand.nextInt(16);
                    int z = j + rand.nextInt(16);
                    long next = rand.nextLong();
                    if (site == null || site.x() != x || site.z() != z || Random17.source(site.state()).nextLong() != next) {
                        wrong.add("spaceship chunk " + c[0] + "," + c[1] + " meteorites " + m + ": replay " + site + ", 1.7 " + x + "," + z);
                    }
                }
            }
        } finally {
            DungeonConfig.pyramidStructure = pyramid;
            DungeonConfig.spaceshipStructure = spaceship;
            WorldConfig.meteoriteSpawn = meteorite;
        }

        // the builders on test terrain
        BodyDungeonStructure.Site tomb = new BodyDungeonStructure.Site(1000, 1000, 12345L);
        LegacyDungeonWorld.Result tombRun = BodyDungeonStructure.run(BodyDungeonStructure.Kind.ANCIENT_TOMB, Populate17.Body.IKE,
                tomb, 64, flat(LegacyDungeonBlocks.Blocks.stone));
        if (tombRun == null || tombRun.size() < 1000) wrong.add("the tomb built " + (tombRun == null ? 0 : tombRun.size()) + " blocks");
        LegacyDungeonWorld.Result shipOk = BodyDungeonStructure.run(BodyDungeonStructure.Kind.SPACESHIP, Populate17.Body.MUN,
                new BodyDungeonStructure.Site(2000, 2000, 777L), 64, flat(LegacyDungeonBlocks.Blocks.stone));
        LegacyDungeonWorld.Result shipNo = BodyDungeonStructure.run(BodyDungeonStructure.Kind.SPACESHIP, Populate17.Body.MUN,
                new BodyDungeonStructure.Site(3000, 3000, 777L), 64, flat(LegacyDungeonBlocks.Blocks.gravel));
        if (shipOk == null || shipOk.size() < 100) wrong.add("the spaceship did not build on accepted ground");
        if (shipNo != null) wrong.add("the spaceship built on ground hbm-space did not accept");
        finish(helper, wrong);
    }

    // ------------------------------------------------------------------------------------------------ in the world

    private static void pedestal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos corner = helper.absolutePos(new BlockPos(2, 1, 2));
        SpaceStructures.Files.IKE_ARTIFACT.build(level, corner.getX(), corner.getY(), corner.getZ(), 0, false, false);
        BlockPos found = null;
        for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(4, 11, 4))) {
            if (level.getBlockState(pos).is(ModDungeonBlocks.pedestal.get())) {
                found = pos.immutable();
                break;
            }
        }
        if (found == null) {
            fail(helper, "the Ike artifact has no pedestal");
            return;
        }
        PlanetFeatures17.restockPedestals(level, found.getX() & ~15, found.getZ() & ~15);
        if (!(level.getBlockEntity(found) instanceof BlockPedestal.TileEntityPedestal pedestal)
                || !pedestal.getItem().is(StructureBlocks.DIVINE_SHARD.get())) {
            fail(helper, "the artifact's pedestal holds no divine shard after Ike's re-stocking");
            return;
        }
        helper.succeed();
    }

    private static void lantern(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
            }
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 4));
        long seed = 3L;
        boolean loot = new Random(seed).nextInt(2) == 0;
        if (!PlanetFeatures17.placeLantern(level, pos, new Random(seed))) {
            fail(helper, "the lantern refused a stone floor");
            return;
        }
        List<String> wrong = new ArrayList<>();
        if (!(level.getBlockEntity(pos) instanceof TileEntityLanternBehemoth lantern) || !lantern.isBroken) wrong.add("no broken lantern core");
        if (!level.getBlockState(pos.above(4)).is(ModDungeonBlocks.lantern_behemoth.get())) wrong.add("the lantern is not five tall");
        if (level.getBlockState(pos.north(2)).is(ModDungeonBlocks.deco_loot.get()) != loot) wrong.add("the loot booklet does not follow the coin (" + loot + ")");
        finish(helper, wrong);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static void finish(GameTestHelper helper, List<String> wrong) {
        if (!wrong.isEmpty()) {
            fail(helper, wrong.size() + " problem(s): " + String.join("; ", wrong.subList(0, Math.min(12, wrong.size()))));
            return;
        }
        helper.succeed();
    }

    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
