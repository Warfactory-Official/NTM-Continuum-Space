package com.ntmspace.gametest.despaceimport;

import com.hbm.deimport.DeImport;
import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Journal;
import com.hbm.deimport.engine.Parking;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Report;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.gate.Signature;
import com.hbm.deimport.source.de7.Airships;
import com.hbm.deimport.source.de7.BlockEntities;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.steps.BackupStep;
import com.hbm.deimport.steps.PlayersStep;
import com.hbm.deimport.steps.RegionsStep;
import com.hbm.deimport.steps.Steps;
import com.hbm.deimport.target.ContinuumTarget;
import com.hbm.delegacy.ContinuumStacks;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.Entities;
import com.hbm.delegacy.de7.Walker;
import com.hbm.delegacy.legacy.DeLegacy;
import com.hbm.delegacy.legacy.LegacyStacks;
import com.hbm.gametest.deimport.DeFixtures;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import com.ntmspace.trait.SolarSystemSavedData;
import dev.golem.core.gametest.GameTests;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/** The DE: Space importer against DE: Space-shaped saves; NTM: Space's own readers load what it wrote. */
public final class DeSpaceImportGameTests {
    public static final GameTests.Suite TESTS = GameTests.suite(NtmSpace.MOD_ID);

    static {
        TESTS.function("despace_chunk_conversion", 20, () -> DeSpaceImportGameTests::chunkConversion);
        TESTS.function("despace_stacks_and_entities", 20, () -> DeSpaceImportGameTests::stacksAndEntities);
        TESTS.function("despace_saved_data", 20, () -> DeSpaceImportGameTests::savedData);
        TESTS.function("despace_satellite_orbits", 20, () -> DeSpaceImportGameTests::satelliteOrbits);
        TESTS.function("despace_players", 20, () -> DeSpaceImportGameTests::players);
        TESTS.function("despace_gate_and_aliases", 20, () -> DeSpaceImportGameTests::gateAndAliases);
        TESTS.function("despace_tables_resolve", 20, () -> DeSpaceImportGameTests::tablesResolve);
        TESTS.function("despace_links_ores_loot", 20, () -> DeSpaceImportGameTests::linksOresLoot);
        TESTS.function("despace_folders_and_maps", 20, () -> DeSpaceImportGameTests::foldersAndMaps);
        TESTS.function("despace_late_install", 20, () -> DeSpaceImportGameTests::lateInstall);
        TESTS.function("despace_data_only_bodies", 20, () -> DeSpaceImportGameTests::dataOnlyBodies);
        TESTS.function("despace_late_world_fixups", 20, () -> DeSpaceImportGameTests::lateWorldFixups);
        TESTS.function("despace_late_resume", 20, () -> DeSpaceImportGameTests::lateResume);
        TESTS.function("despace_components_and_legacy", 20, () -> DeSpaceImportGameTests::componentsAndLegacy);
        TESTS.function("despace_late_players_and_links", 20, () -> DeSpaceImportGameTests::latePlayersAndLinks);
        TESTS.function("despace_late_restore_resume", 20, () -> DeSpaceImportGameTests::lateRestoreResume);
        TESTS.function("despace_late_partial_import", 20, () -> DeSpaceImportGameTests::latePartialImport);
        TESTS.function("despace_station_orbit_kept", 20, () -> DeSpaceImportGameTests::stationOrbitKept);
        TESTS.function("despace_satellite_restore_gates", 20, () -> DeSpaceImportGameTests::satelliteRestoreGates);
        if (ModList.get().isLoaded("sable")) {
            TESTS.function("despace_airship_retry", 20, () -> DeSpaceSableGameTests::retry);
            TESTS.function("despace_airship_entities", 20, () -> DeSpaceSableGameTests::aboard);
            TESTS.function("despace_airship_gives_up", 20, () -> DeSpaceSableGameTests::givesUp);
        }
    }

    private DeSpaceImportGameTests() {}

    private static void chunkConversion(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            BlockPos origin = new BlockPos(2, 70, 2);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, origin, DeFixtures.block("hbmspace:ore_iron_duna"));
            DeFixtures.setBlock(chunk, origin.east(), DeFixtures.block("hbmspace:ore_copper_orbit"));
            DeFixtures.setBlock(chunk, origin.east(2), DeFixtures.block("hbmspace:moon_rock"));
            DeFixtures.setBlock(chunk, origin.east(3), DeFixtures.block("hbmspace:laythe_coral_tube", "facing", "up"));
            DeFixtures.setBlock(chunk, origin.east(4), DeFixtures.block("hbmspace:rubber_farmland", "moist", "true"));
            DeFixtures.setBlock(chunk, origin.east(5), DeFixtures.block("hbmspace:ore_cinnabar_mun"));
            DeFixtures.setBlock(chunk, origin.east(6), DeFixtures.block("hbmspace:geysir_volcanic"));
            BlockPos pad = origin.south(5);
            DeFixtures.setBlock(chunk, pad, DeFixtures.block("hbmspace:launch_pad_rocket", "cell", "core", "facing", "north"));
            DeFixtures.setBlock(chunk, pad.east(), DeFixtures.block("hbmspace:launch_pad_rocket", "cell", "forward", "facing", "north"));
            DeFixtures.setBlock(chunk, pad.west(), DeFixtures.block("hbmspace:multiblock_cell_h1_r1_m6"));
            BlockPos magma = origin.south(10);
            DeFixtures.setBlock(chunk, magma, DeFixtures.block("hbmspace:machine_magma", "facing", "north"));
            CompoundTag magmaBe = DeFixtures.blockEntity("hbmspace:machine_magma", magma);
            // DE's 1.7 material ids: iron is 2600, copper 2900
            magmaBe.put("liquids", new IntArrayTag(new int[]{2600, 144, 2900, 72}));
            DeFixtures.addBlockEntity(chunk, magmaBe);

            CompoundTag biomes = chunk.getList("sections", Tag.TAG_COMPOUND).getCompound(8).getCompound("biomes");
            ListTag biomePalette = new ListTag();
            biomePalette.add(StringTag.valueOf("hbmspace:space"));
            biomePalette.add(StringTag.valueOf("hbmspace:duna_polar"));
            biomes.put("palette", biomePalette);

            Ctx ctx = ctx(world);
            CompoundTag out = new ChunkConverter(ctx, "ntmspace:duna").convert(chunk);
            ListTag outBiomes = out.getList("sections", Tag.TAG_COMPOUND).getCompound(8).getCompound("biomes").getList("palette", Tag.TAG_STRING);
            helper.assertTrue(outBiomes.getString(0).equals("ntmspace:orbit") && outBiomes.getString(1).equals("ntmspace:duna_polar"),
                    "biomes by NTM: Space's names: " + outBiomes);
            helper.assertTrue(out != null, "the chunk converts");
            expect(helper, out, origin, "ntmspace:ore_iron", "body", "duna");
            expect(helper, out, origin.east(), "ntmspace:ore_copper", "body", "kerbin");
            expect(helper, out, origin.east(2), "ntmspace:moon_rock", null, null);
            expect(helper, out, origin.east(3), "ntmspace:laythe_coral", "type", "tube");
            helper.assertTrue("up".equals(ChunkBlocks.get(out, origin.east(3)).getCompound("Properties").getString("facing")),
                    "the coral keeps its facing");
            expect(helper, out, origin.east(4), "ntmspace:rubber_farmland", "moisture", "1");
            expect(helper, out, origin.east(5), "ntmspace:ore_cinnebar", "body", "mun");
            expect(helper, out, origin.east(6), "minecraft:air", null, null);
            expect(helper, out, pad, "ntmspace:launch_pad_rocket", "facing", "north");
            expect(helper, out, pad.east(), "minecraft:air", null, null);
            expect(helper, out, pad.west(), "minecraft:air", null, null);
            helper.assertTrue(ctx.cores.stream().anyMatch(c -> c.getLong("pos") == pad.asLong() && c.getString("dim").equals("ntmspace:duna")),
                    "the pad's core waits for the rebuild in NTM: Space's Duna: " + ctx.cores);
            helper.assertTrue(ctx.parking.counts().getOrDefault("refunded_block", 0) == 1, "the volcanic geysir is parked with its position");

            CompoundTag be = null;
            for (Tag t : out.getList("block_entities", Tag.TAG_COMPOUND))
                if (BlockPos.of(BlockPos.asLong(((CompoundTag) t).getInt("x"), ((CompoundTag) t).getInt("y"), ((CompoundTag) t).getInt("z"))).equals(magma)) be = (CompoundTag) t;
            helper.assertTrue(be != null && be.getString("id").startsWith("ntmspace:"), "the magma block entity is NTM: Space's: " + be);
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), ChunkBlocks.get(out, magma));
            BlockEntity loaded = BlockEntity.loadStatic(magma, state, be, helper.getLevel().registryAccess());
            helper.assertTrue(loaded != null, "NTM: Space loads the magma block entity");
            String saved = loaded.saveWithoutMetadata(helper.getLevel().registryAccess()).toString();
            helper.assertTrue(saved.contains("iron") && saved.contains("144") && saved.contains("72"),
                    "the molten metals survive by name: " + saved);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void stacksAndEntities(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world);
            Walker walker = new Walker(ctx);
            CompoundTag rocketItem = DeFixtures.stack("hbmspace:rp_c_20", 1);
            CompoundTag design = new CompoundTag();
            design.putString("capsule", "hbmspace:rp_c_20");
            CompoundTag stage = new CompoundTag();
            stage.putString("fuselage", "hbm:mp_fuselage_15_kerosene");
            stage.putString("thruster", "hbmspace:mp_thruster_20_methalox");
            ListTag stages = new ListTag();
            stages.add(stage);
            design.put("stages", stages);
            CompoundTag components = new CompoundTag();
            components.put("hbmspace:rocket", design);
            components.put("hbmspace:rocket_fueled", new CompoundTag());
            rocketItem.put("components", components);
            CompoundTag drive = DeFixtures.stack("hbmspace:hard_drive", 1);
            CompoundTag driveData = new CompoundTag();
            driveData.putInt("x", 10);
            driveData.putInt("z", 20);
            driveData.put("approx", new IntArrayTag(new int[]{11, 21}));
            CompoundTag driveComponents = new CompoundTag();
            driveComponents.put("hbmspace:drive", driveData);
            drive.put("components", driveComponents);

            CompoundTag chest = DeFixtures.inventory(
                    DeFixtures.slot(0, DeFixtures.stack("hbmspace:clump_nitric", 3)),
                    DeFixtures.slot(1, DeFixtures.stack("hbmspace:ore_iron_kerbin", 5)),
                    DeFixtures.slot(2, DeFixtures.stack("hbmspace:bedrock_ore_fragment_cobalt_60", 2)),
                    DeFixtures.slot(3, DeFixtures.stack("hbmspace:ingot_raw_iridium", 1)),
                    DeFixtures.slot(4, rocketItem),
                    DeFixtures.slot(5, drive),
                    DeFixtures.slot(6, DeFixtures.stack("hbmspace:moon_rock", 64)));
            walker.walk(chest, "test chest");
            String text = chest.toString();
            for (String id : List.of("ntmspace:mineral_fragment_nitric", "ntmspace:ore_iron\"", "ntmspace:bedrock_ore_fragment_co60", "ntmspace:moon_rock"))
                helper.assertTrue(text.contains(id), id + " in " + text);
            helper.assertTrue(!text.contains("ingot_raw_iridium") && ctx.parking.counts().getOrDefault("item", 0) == 1,
                    "DE: Space's raw iridium is parked: " + ctx.parking.counts());
            CompoundTag rocket = find(chest, "ntmspace:rp_c_20");
            helper.assertTrue(rocket != null, "the rocket item keeps its id in NTM: Space: " + text);
            CompoundTag rc = rocket.getCompound("components");
            helper.assertTrue(rc.getBoolean("ntmspace:rocket_fuelled"), "fueled is NTM: Space's boolean: " + rc);
            String parts = rc.getCompound("ntmspace:rocket").toString();
            helper.assertTrue(parts.contains("ntmspace:rp_c_20") && parts.contains("ntmspace:mp_thruster_20_methalox")
                    && parts.contains("hbm:mp_fuselage_15_kerosene"), "rocket parts by NTM: Space's ids: " + parts);
            helper.assertTrue(List.of().equals(ContinuumStacks.undecodable(helper.getLevel().registryAccess(), rocket)),
                    "NTM: Space decodes the rocket: " + rocket);
            CompoundTag driveOut = find(chest, "ntmspace:hard_drive");
            CompoundTag dd = driveOut == null ? new CompoundTag() : driveOut.getCompound("components").getCompound("ntmspace:drive");
            helper.assertTrue(dd.getInt("ax") == 11 && dd.getInt("az") == 21 && !dd.contains("approx"), "drive approx split: " + dd);

            CompoundTag rocketEntity = entity("hbmspace:entity_rideable_rocket");
            rocketEntity.putInt("state", 3);
            CompoundTag override = new CompoundTag();
            override.putString("body", "duna");
            override.putInt("x", 5);
            override.putInt("z", -7);
            rocketEntity.put("override", override);
            CompoundTag depress = entity("hbmspace:entity_depress");
            CompoundTag ufo = entity("hbmspace:entity_micro_ufo");
            ufo.putInt("siegeTier", 3);
            CompoundTag squid = entity("hbmspace:entity_depthsquid");
            squid.putByte("type", (byte) 6);
            ListTag entities = new ListTag();
            entities.add(rocketEntity);
            entities.add(depress);
            entities.add(ufo);
            entities.add(squid);
            walker.walkList(entities, "test entities");
            helper.assertTrue(entities.size() == 3, "the breach pull is parked: " + entities);
            CompoundTag r = entities.getCompound(0);
            helper.assertTrue(r.getString("id").equals("ntmspace:entity_rideable_rocket") && r.getString("state").equals("LANDED")
                    && r.getBoolean("hasOverride") && r.getString("overrideBody").equals("duna") && r.getInt("overrideZ") == -7,
                    "rocket state and destination: " + r);
            helper.assertTrue(entities.getCompound(1).getString("siegeTier").equals("iron"), "siege tier by name: " + entities.getCompound(1));
            helper.assertTrue(entities.getCompound(2).getString("variant").equals("VICIOUS") && !entities.getCompound(2).contains("type"),
                    "the depth squid's variant by name: " + entities.getCompound(2));
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void savedData(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Files.createDirectories(world.resolve("dimensions/hbmspace/duna/region"));
            CompoundTag solar = new CompoundTag();
            CompoundTag mun = new CompoundTag();
            CompoundTag temperature = new CompoundTag();
            temperature.putFloat("c", -80F);
            mun.put("temperature", temperature);
            CompoundTag atmosphere = new CompoundTag();
            ListTag fluids = new ListTag();
            CompoundTag air = new CompoundTag();
            air.putString("type", "hbmspace:earthair");
            air.putDouble("percentage", 0.5D);
            fluids.add(air);
            atmosphere.put("fluids", fluids);
            mun.put("atmosphere", atmosphere);
            CompoundTag war = new CompoundTag();
            war.putInt("health", 90);
            CompoundTag projectiles = new CompoundTag();
            CompoundTag projectile = new CompoundTag();
            projectile.putInt("damage", 5);
            projectile.putString("type", "SMALL");
            projectile.putString("target", "hbmspace:duna");
            projectiles.put("projectile0", projectile);
            war.put("projectiles", projectiles);
            mun.put("war", war);
            solar.put("b_mun", mun);
            ListTag stations = new ListTag();
            CompoundTag station = new CompoundTag();
            station.putInt("x", 3);
            station.putInt("z", 4);
            station.putString("orbiting", "duna");
            station.putString("target", "duna");
            station.putInt("state", 2);
            station.putInt("stateTimer", 40);
            station.putInt("maxStateTimer", 200);
            station.putBoolean("hasStation", true);
            station.putString("name", "Kerbal One");
            stations.add(station);
            solar.put("stations", stations);
            writeDe(world.resolve("data/hbmspace_solar_system.dat"), solar);
            CompoundTag storm = new CompoundTag();
            storm.putInt("dustStormTimer", 1200);
            storm.putFloat("dustStormIntensity", 0.75F);
            writeDe(world.resolve("dimensions/hbmspace/duna/data/hbmspace_duna_dust_storm.dat"), storm);
            writeDe(world.resolve("data/hbmspace_chunk_loader.dat"), new CompoundTag());

            Ctx ctx = ctx(world);
            List<Step> space = Steps.all().stream().filter(s -> s.id().startsWith("space")).toList();
            helper.assertTrue(space.size() == 4, "NTM: Space added its four steps: " + Steps.all().stream().map(Step::id).toList());
            for (Step step : space) if (step.applies(ctx)) step.run(ctx, null);

            helper.assertTrue(Files.isDirectory(world.resolve("dimensions/ntmspace/duna/region")) && !Files.exists(world.resolve("dimensions/hbmspace")),
                    "Duna moved under ntmspace");
            CompoundTag stormOut = read(world.resolve("dimensions/ntmspace/duna/data/ntmspace_dust_storm.dat")).getCompound("data");
            helper.assertTrue(stormOut.getInt("dustStormTimer") == 1200, "dust storm renamed: " + stormOut);
            helper.assertTrue(!Files.exists(world.resolve("data/hbmspace_chunk_loader.dat")) && ctx.parking.counts().getOrDefault("saved_data", 0) == 3,
                    "every DE: Space file is parked first: " + ctx.parking.counts());

            CompoundTag traitFile = read(world.resolve("data/ntmspace_solar_system.dat"));
            SolarSystemSavedData traits = SolarSystemSavedData.fromTag(traitFile.get("data"));
            helper.assertTrue(traits.getTraits("mun") != null && traits.getTraits("mun").size() == 3,
                    "NTM: Space reads Mun's three traits: " + traitFile);
            String munText = traitFile.toString();
            helper.assertTrue(munText.contains("ntmspace:earthair") && munText.contains("ntmspace:duna"),
                    "fluid and war target in NTM: Space's namespace: " + munText);
            CompoundTag stationFile = read(world.resolve("data/ntmspace_stations.dat"));
            StationData stationData = StationData.CODEC.parse(NbtOps.INSTANCE, stationFile.get("data")).getOrThrow();
            OrbitalStation one = stationData.getStations().stream().findFirst().orElse(null);
            helper.assertTrue(one != null && one.state == OrbitalStation.StationState.TRANSFER && "Kerbal One".equals(one.name),
                    "the station loads in transfer: " + stationFile);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void satelliteOrbits(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag sat = new CompoundTag();
            sat.putString("type", "war");
            CompoundTag orbit = new CompoundTag();
            orbit.putFloat("inclination", 12F);
            orbit.putFloat("altitude", 400F);
            orbit.putFloat("phase", 0.25F);
            orbit.putString("owner", "Jeb");
            orbit.putFloat("red", 1F);
            orbit.putFloat("green", 0.5F);
            orbit.putFloat("blue", 0F);
            CompoundTag components = new CompoundTag();
            components.put("hbmspace:satellite_orbit", orbit);
            CompoundTag state = new CompoundTag();
            state.putInt("targetX", 100);
            state.putString("driveInput", "disk_orbitdata");
            state.put("components", components);
            CompoundTag outer = new CompoundTag();
            outer.put("sat", sat);
            outer.put("state", state);
            CompoundTag entry = new CompoundTag();
            entry.putInt("freq", 77);
            entry.put("sat", outer);
            ListTag sats = new ListTag();
            sats.add(entry);
            CompoundTag relay = new CompoundTag();
            relay.putString("type", "dyson_relay");
            CompoundTag relayOuter = new CompoundTag();
            relayOuter.put("sat", relay);
            relayOuter.put("state", new CompoundTag());
            CompoundTag relayEntry = new CompoundTag();
            relayEntry.putInt("freq", 78);
            relayEntry.put("sat", relayOuter);
            sats.add(relayEntry);
            CompoundTag data = new CompoundTag();
            data.put("sats", sats);
            writeDe(world.resolve("data/hbm_satellites.dat"), data);
            CompoundTag objective = new CompoundTag();
            objective.putString("Name", "duna_iron");
            objective.putString("CriteriaName", "minecraft.mined:hbmspace.ore_iron_duna");
            ListTag objectives = new ListTag();
            objectives.add(objective);
            CompoundTag scores = new CompoundTag();
            scores.put("Objectives", objectives);
            CompoundTag scoreboard = new CompoundTag();
            scoreboard.put("data", scores);
            NbtIo.writeCompressed(scoreboard, world.resolve("data/scoreboard.dat"));

            Ctx ctx = ctx(world);
            Steps.all().stream().filter(s -> s.id().startsWith("saveddata@")).findFirst().orElseThrow().run(ctx, null);
            CompoundTag out = read(world.resolve("data/hbm_satellites.dat")).getCompound("data");
            SatelliteSavedData loaded = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, out).getOrThrow();
            Satellite railgun = loaded.getSatFromFreq(77);
            String saved = SatelliteSavedData.CODEC.encodeStart(NbtOps.INSTANCE, loaded).getOrThrow().toString();
            helper.assertTrue(railgun != null && saved.contains("ntmspace:railgun"), "DE: Space's war satellite is NTM: Space's railgun: " + saved);
            helper.assertTrue(loaded.getSatFromFreq(78) != null && saved.contains("ntmspace:dyson_relay"), "the Dyson relay too: " + saved);
            String criteria = read(world.resolve("data/scoreboard.dat")).getCompound("data").getList("Objectives", Tag.TAG_COMPOUND)
                    .getCompound(0).getString("CriteriaName");
            helper.assertTrue(criteria.equals("minecraft.mined:ntmspace.ore_iron"), "the objective counts NTM: Space's ore: " + criteria);
            CompoundTag extra = railgun.extra.getCompound("ntmspace:orbit");
            helper.assertTrue(extra.getFloat("phase_offset") == 0.25F && extra.getFloat("g") == 0.5F && extra.getString("owner").equals("Jeb"),
                    "orbit moved into the satellite's extra data: " + railgun.extra);
            helper.assertTrue(railgun.targetX == 100 && railgun.driveInput != null && railgun.driveInput.name().equals("DISK_ORBITDATA"),
                    "target and drive from DE's satellite state: " + out);
            helper.assertTrue(ctx.parking.counts().getOrDefault("satellite_state", 0) == 0, "nothing of the state is left over: " + ctx.parking.counts());
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void players(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world);
            CompoundTag player = new CompoundTag();
            player.putString("Dimension", "hbmspace:mun");
            player.putString("SpawnDimension", "hbmspace:orbit");
            CompoundTag attachments = new CompoundTag();
            CompoundTag nitan = new CompoundTag();
            nitan.putInt("nitan", 4);
            nitan.putFloat("nitan_health", 6F);
            attachments.put("hbmspace:nitan", nitan);
            CompoundTag breath = new CompoundTag();
            breath.putInt("oxygen", 120);
            attachments.put("hbmspace:breath", breath);
            CompoundTag warped = new CompoundTag();
            warped.putBoolean("hasWarped", true);
            attachments.put("hbmspace:has_warped", warped);
            CompoundTag living = new CompoundTag();
            living.putFloat("radiation", 50F);
            living.putFloat("activation", 3.5F);
            attachments.put("hbm:living_props", living);
            ListTag effects = new ListTag();
            CompoundTag nitanEffect = new CompoundTag();
            nitanEffect.putString("id", "hbmspace:nitan");
            nitanEffect.putInt("duration", 3600);
            effects.add(nitanEffect);
            player.put("active_effects", effects);
            player.put("neoforge:attachments", attachments);
            Entities.living(player, ctx, "test player");

            CompoundTag out = player.getCompound("neoforge:attachments");
            helper.assertTrue(player.getString("Dimension").equals("ntmspace:mun") && player.getString("SpawnDimension").equals("ntmspace:orbit"),
                    "the player stays on Mun: " + player);
            helper.assertTrue(out.getCompound("ntmspace:nitan").getInt("count") == 4 && out.getCompound("ntmspace:nitan").getFloat("health") == 6F,
                    "nitan count and health: " + out);
            helper.assertTrue(out.getCompound("ntmspace:breath").getInt("oxygen") == 120, "breath: " + out);
            helper.assertTrue(out.getCompound("hbm:living_props").get("neutrons") instanceof DoubleTag activation
                    && activation.getAsDouble() == 3.5D, "neutron activation, as the double Continuum reads: " + out);
            helper.assertTrue(player.getList("active_effects", Tag.TAG_COMPOUND).getCompound(0).getString("id").equals("ntmspace:nitan"),
                    "Ambrosia stays on: " + player.get("active_effects"));
            helper.assertTrue("ntmspace:run".equals(String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(
                    BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse("hbmspace:run"))))), "effect aliases");
            helper.assertTrue(!out.contains("hbmspace:has_warped") && ctx.parking.counts().getOrDefault("attachment", 0) == 1,
                    "the warp flag is parked: " + ctx.parking.counts());
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void gateAndAliases(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Files.createDirectories(world.resolve("dimensions/hbmspace/mun/region"));
            helper.assertTrue(Signature.addons(world, DeAddons.KNOWN.keySet()).equals(List.of("hbmspace")), "the world holds DE: Space's data");
            helper.assertTrue(DeAddons.has("hbmspace"), "NTM: Space converts it, so the importer does not refuse the world");
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
            state.put("addons", "hbmspace");
            state.put("deferred_addons", "hbmspace");
            helper.assertTrue(DeImport.missingAddon(state) == null, "an import begun with NTM: Space goes on with it");
            Ctx ctx = ctx(world);
            DeImport.deferred(state, ctx);
            helper.assertTrue(ctx.deferredAddons.isEmpty() && "".equals(state.get("deferred_addons")) && "hbmspace".equals(state.get("converting_addons")),
                    "DE: Space left for later converts now that NTM: Space is here, and a start without it is refused until done");
            Map<String, String> want = new HashMap<>(Map.of("hbmspace:moon_rock", "ntmspace:moon_rock",
                    "hbmspace:clump_nitric", "ntmspace:mineral_fragment_nitric"));
            want.forEach((from, to) -> helper.assertTrue(
                    BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.get(ResourceLocation.parse(from))).toString().equals(to),
                    from + " is an alias of " + to));
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(BuiltInRegistries.ENTITY_TYPE.get(
                    ResourceLocation.parse("hbmspace:entity_moon_cow"))).toString().equals("ntmspace:entity_moon_cow"), "entity alias");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** Every DE: Space id the tables or the namespace rule send somewhere names a registered NTM: Space id. */
    private static void tablesResolve(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        try (var in = new BufferedReader(new InputStreamReader(
                DeSpaceImportGameTests.class.getResourceAsStream("/ntmspace_deimport/aliases.tsv"), StandardCharsets.UTF_8))) {
            for (String line; (line = in.readLine()) != null; ) {
                if (line.startsWith("#")) continue;
                String[] cols = line.split("\t");
                ResourceLocation to = ResourceLocation.parse(cols[2]);
                boolean known = switch (cols[0]) {
                    case "item" -> BuiltInRegistries.ITEM.containsKey(to);
                    case "block" -> BuiltInRegistries.BLOCK.containsKey(to);
                    case "entity_type" -> BuiltInRegistries.ENTITY_TYPE.containsKey(to);
                    case "mob_effect" -> BuiltInRegistries.MOB_EFFECT.containsKey(to);
                    default -> BuiltInRegistries.FLUID.containsKey(to);
                };
                if (!known) bad.add(cols[0] + " " + cols[1] + " -> " + cols[2]);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (!bad.isEmpty()) NtmSpace.LOG.warn("DE: Space ids with no NTM: Space target ({}): {}", bad.size(), bad);
        helper.assertTrue(bad.isEmpty(), bad.size() + " DE: Space id(s) name nothing registered, first " + bad.stream().limit(5).toList());
        helper.succeed();
    }

    /** Dimensions in links and targets, DE: Space's materials, per-body bedrock ore, samples, and structure loot. */
    private static void linksOresLoot(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world);
            CompoundTag pad = DeFixtures.blockEntity("hbmspace:transporter_rocket", BlockPos.ZERO);
            pad.putString("dimension", "hbmspace:duna");
            BlockEntities.convert(pad, "ntmspace:transporter_rocket", ctx, null, "test pad");
            helper.assertTrue(pad.getString("dimension").equals("ntmspace:duna"), "the transporter's link: " + pad);
            CompoundTag teleporter = DeFixtures.blockEntity("hbm:machine_teleporter", BlockPos.ZERO);
            CompoundTag target = new CompoundTag();
            target.putString("dimension", "hbmspace:mun");
            target.put("pos", new IntArrayTag(new int[]{1, 2, 3}));
            teleporter.put("target", target);
            BlockEntities.convert(teleporter, "hbm:machine_teleporter", ctx, null, "test teleporter");
            helper.assertTrue(teleporter.getCompound("target").getString("dimension").equals("ntmspace:mun"), "the teleporter's target: " + teleporter);
            CompoundTag magma = DeFixtures.blockEntity("hbmspace:machine_magma", BlockPos.ZERO);
            magma.put("liquids", new IntArrayTag(new int[]{24002, 50, 2800, 9}));
            BlockEntities.convert(magma, "ntmspace:machine_magma", ctx, null, "test magma");
            String melt = magma.get("liquids").toString();
            helper.assertTrue(melt.contains("rich_magma") && melt.contains("nickel_pure"), "DE: Space's own materials by name: " + melt);

            CompoundTag ore = DeFixtures.stack("hbm:bedrock_ore", 4);
            CompoundTag oreData = new CompoundTag();
            oreData.putString("type", "duna_heavy");
            oreData.putString("grade", "primary");
            CompoundTag oreComponents = new CompoundTag();
            oreComponents.put("hbm:bedrock_ore", oreData);
            ore.put("components", oreComponents);
            CompoundTag sample = DeFixtures.stack("hbm:bedrock_ore_base", 1);
            CompoundTag sampleData = new CompoundTag();
            sampleData.putString("set", "duna");
            for (String kind : List.of("light", "heavy", "rare", "actinide", "nonmetal", "crystal")) sampleData.putDouble(kind, 0.5D);
            CompoundTag sampleComponents = new CompoundTag();
            sampleComponents.put("hbm:bedrock_ore_sample", sampleData);
            sample.put("components", sampleComponents);
            CompoundTag linker = DeFixtures.stack("hbm:linker", 1);
            CompoundTag linkTarget = new CompoundTag();
            linkTarget.putString("dimension", "hbmspace:duna");
            linkTarget.put("pos", new IntArrayTag(new int[]{4, 5, 6}));
            CompoundTag linkerComponents = new CompoundTag();
            linkerComponents.put("hbm:linker_target", linkTarget);
            linker.put("components", linkerComponents);
            CompoundTag chest = DeFixtures.inventory(DeFixtures.slot(0, ore), DeFixtures.slot(1, sample), DeFixtures.slot(2, linker));
            new Walker(ctx).walk(chest, "test chest");
            CompoundTag oreOut = find(chest, "ntmspace:bedrock_ore_primary_duna_heavy");
            helper.assertTrue(oreOut != null && List.of().equals(ContinuumStacks.undecodable(helper.getLevel().registryAccess(), oreOut)),
                    "Duna's heavy primary bedrock ore: " + chest);
            CompoundTag sampleOut = find(chest, "hbm:bedrock_ore_base");
            helper.assertTrue(sampleOut != null && sampleOut.getCompound("components").contains("hbm:bedrock_ore_sample")
                    && List.of().equals(ContinuumStacks.undecodable(helper.getLevel().registryAccess(), sampleOut)), "Duna's sample decodes: " + sampleOut);
            CompoundTag linkerOut = find(chest, "hbm:linker");
            helper.assertTrue(linkerOut != null && linkerOut.getCompound("components").getCompound("hbm:tele_link").getString("dim").equals("ntmspace:duna"),
                    "the linker's dimension: " + linkerOut);
            helper.assertTrue(ctx.parking.counts().isEmpty(), "nothing parked: " + ctx.parking.counts());

            BlockPos crate = new BlockPos(3, 70, 3);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, crate, DeFixtures.block("hbm:crate_iron"));
            CompoundTag crateBe = DeFixtures.blockEntity("hbm:crate_iron", crate);
            crateBe.putString("LootTable", "hbmspace:chests/submarine_6_11");
            crateBe.putLong("LootTableSeed", 42L);
            DeFixtures.addBlockEntity(chunk, crateBe);
            new ChunkConverter(ctx, "ntmspace:laythe").convert(chunk);
            helper.assertTrue(ctx.loot.stream().anyMatch(r -> r.getString("table").equals("ntmspace_de:chests/submarine_6_11")),
                    "the submarine crate rolls the served table: " + ctx.loot);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    DeSpaceImportGameTests.class.getResourceAsStream("/ntmspace_deimport/loot_tables.tsv"), StandardCharsets.UTF_8))) {
                for (String line; (line = in.readLine()) != null; ) {
                    if (line.startsWith("#")) continue;
                    ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(line.split("\t")[1]));
                    helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "served table " + key);
                }
            }
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /**
     * The step order, a body folder with saved data and no terrain joining NTM: Space's own, NTM: Space's solar system
     * from an earlier start (set aside, DE: Space's wins), and a map drawn on Duna.
     */
    private static void foldersAndMaps(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            List<String> order = Steps.all().stream().map(Step::id).toList();
            helper.assertTrue(order.indexOf("spacedimensions@1") < order.indexOf("sublevels@1"), "the folders move before the airships convert: " + order);
            CompoundTag storm = new CompoundTag();
            storm.putInt("dustStormTimer", 300);
            writeDe(world.resolve("dimensions/hbmspace/duna/data/hbmspace_duna_dust_storm.dat"), storm);
            Files.createDirectories(world.resolve("dimensions/ntmspace/duna/data"));
            Files.writeString(world.resolve("dimensions/ntmspace/duna/data/raids.dat"), "ntm: space");
            CompoundTag own = new CompoundTag();
            own.put("data", new CompoundTag());
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(own, world.resolve("data/ntmspace_solar_system.dat"));
            CompoundTag mun = new CompoundTag();
            mun.put("temperature", new CompoundTag());
            CompoundTag solar = new CompoundTag();
            solar.put("b_mun", mun);
            solar.put("stations", stations(1));
            writeDe(world.resolve("data/hbmspace_solar_system.dat"), solar);
            CompoundTag ownStations = new CompoundTag();
            ownStations.put("stations", ownStations(3));
            CompoundTag ownStationsRoot = new CompoundTag();
            ownStationsRoot.put("data", ownStations);
            NbtIo.writeCompressed(ownStationsRoot, world.resolve("data/ntmspace_stations.dat"));
            CompoundTag map = new CompoundTag();
            map.putString("dimension", "hbmspace:duna");
            CompoundTag mapRoot = new CompoundTag();
            mapRoot.put("data", map);
            NbtIo.writeCompressed(mapRoot, world.resolve("data/map_3.dat"));

            Ctx ctx = ctx(world);
            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            helper.assertTrue(Files.isRegularFile(world.resolve("dimensions/ntmspace/duna/data/ntmspace_dust_storm.dat")),
                    "Duna's storm converts though Duna has no terrain");
            helper.assertTrue(Files.readString(world.resolve("dimensions/ntmspace/duna/data/raids.dat")).equals("ntm: space"),
                    "NTM: Space's own Duna stays: DE: Space's only saved data");
            helper.assertTrue(Files.isRegularFile(world.resolve("hbm_de_kept/data/ntmspace_solar_system.dat")), "and its own solar system");
            helper.assertTrue(read(world.resolve("data/ntmspace_solar_system.dat")).getCompound("data").getCompound("bodies").contains("mun"),
                    "DE: Space's solar system is the one in use");
            helper.assertTrue(read(world.resolve("data/map_3.dat")).getCompound("data").getString("dimension").equals("ntmspace:duna"),
                    "the map follows Duna");
            helper.assertTrue(stationCount(world.resolve("data/ntmspace_stations.dat")) == 2, "NTM: Space's own station stays beside DE: Space's");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /**
     * A world imported before NTM: Space was installed: its terrain step took DE's stamps off the bodies' chunks, so
     * the bodies convert from the import's backup when NTM: Space arrives.
     */
    private static void lateInstall(GameTestHelper helper) {
        Path parent = tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-010000");
        try {
            BlockPos pos = new BlockPos(2, 100, 2);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, pos, DeFixtures.block("hbmspace:ore_iron_mun"));
            writeChunk(backup.resolve("dimensions/hbmspace/mun/region/r.0.0.mca"), chunk);
            CompoundTag decoy = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(decoy, pos, DeFixtures.block("hbmspace:ore_iron_duna"));
            writeChunk(parent.resolve("world_DE_backup_20261029-000000/dimensions/hbmspace/mun/region/r.0.0.mca"), decoy);
            Files.createDirectories(world);
            Files.writeString(world.resolve(BackupStep.RECORD), backup.getFileName().toString());
            CompoundTag unstamped = chunk.copy();
            unstamped.remove(ChunkConverter.STAMP);
            writeChunk(world.resolve("dimensions/hbmspace/mun/region/r.0.0.mca"), unstamped);
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
            state.markDone("regions@1");
            state.status("done");
            state.save();

            Ctx ctx = ctx(world);
            Step dims = Steps.all().stream().filter(s -> s.id().startsWith("spacedimensions")).findFirst().orElseThrow();
            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            CompoundTag out;
            try (RegionFile region = RegionIo.open(world.resolve("dimensions/ntmspace/mun/region/r.0.0.mca"))) {
                out = RegionIo.read(region, new ChunkPos(0, 0));
            }
            CompoundTag ore = out == null ? null : ChunkBlocks.get(out, pos);
            helper.assertTrue(ore != null && ore.getString("Name").equals("ntmspace:ore_iron")
                    && ore.getCompound("Properties").getString("body").equals("mun"), "Mun's ore converts from the backup: " + ore);
            helper.assertTrue(Files.isRegularFile(world.resolve("hbm_de_kept/hbmspace/mun/region/r.0.0.mca")), "the first import's terrain is kept aside");
            helper.assertTrue(!dims.applies(ctx), "a second run finds nothing to do");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(parent);
        }
    }

    /**
     * A body DE: Space saved data for but never a chunk: DE's satellites there load, and its files join a body NTM:
     * Space already built on instead of replacing it.
     */
    private static void dataOnlyBodies(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag data = new CompoundTag();
            ListTag sats = new ListTag();
            sats.add(deSatellite(5, "miner"));
            data.put("sats", sats);
            writeDe(world.resolve("dimensions/hbmspace/dres/data/hbm_satellites.dat"), data);
            writeDe(world.resolve("dimensions/hbmspace/eve/data/hbm_satellites.dat"), data.copy());
            Files.writeString(world.resolve("dimensions/hbmspace/eve/data/raids.dat"), "de: space");
            Files.createDirectories(world.resolve("dimensions/ntmspace/eve/region"));
            Files.createDirectories(world.resolve("dimensions/ntmspace/eve/data"));
            Files.writeString(world.resolve("dimensions/ntmspace/eve/data/raids.dat"), "ntm: space");
            ListTag owned = new ListTag();
            owned.add(flatSatellite(5, "radar"));
            owned.add(flatSatellite(6, "radar"));
            CompoundTag ownedData = new CompoundTag();
            ownedData.put("sats", owned);
            CompoundTag ownedRoot = new CompoundTag();
            ownedRoot.put("data", ownedData);
            NbtIo.writeCompressed(ownedRoot, world.resolve("dimensions/ntmspace/eve/data/hbm_satellites.dat"));

            Ctx ctx = ctx(world);
            for (Step step : Steps.all())
                if ((step.id().startsWith("space") || step.id().startsWith("saveddata@")) && step.applies(ctx)) step.run(ctx, null);
            SatelliteSavedData eve = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE,
                    read(world.resolve("dimensions/ntmspace/eve/data/hbm_satellites.dat")).getCompound("data")).getOrThrow();
            helper.assertTrue(eve.getSatFromFreq(6) != null && ctx.parking.counts().getOrDefault("satellite", 0) == 1
                    && !Files.exists(world.resolve("dimensions/ntmspace/eve/data/hbm_satellites_ntmspace.dat")),
                    "NTM: Space's own satellites join DE: Space's, the one on a taken frequency parked: " + ctx.parking.counts());
            for (String body : List.of("dres", "eve")) {
                CompoundTag saved = read(world.resolve("dimensions/ntmspace/" + body + "/data/hbm_satellites.dat")).getCompound("data");
                helper.assertTrue(SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().getSatFromFreq(5) != null,
                        "the miner in " + body + " loads: " + saved);
            }
            helper.assertTrue(Files.isDirectory(world.resolve("dimensions/ntmspace/eve/region"))
                    && Files.readString(world.resolve("dimensions/ntmspace/eve/data/raids.dat")).equals("ntm: space"), "NTM: Space's Eve stays in use");
            helper.assertTrue(Files.readString(world.resolve("hbm_de_kept/hbmspace/eve/data/raids.dat")).equals("de: space"),
                    "DE: Space's clashing file is kept aside");
            helper.assertTrue(!Files.exists(world.resolve("dimensions/hbmspace")), "nothing is left under DE: Space's namespace");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /**
     * A world imported without NTM: Space: a body left in DE's shape converts where it is, players come back to the
     * bodies they logged out on, and satellites DE's step flattened or never reached load.
     */
    private static void lateWorldFixups(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            BlockPos pos = new BlockPos(2, 100, 2);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, pos, DeFixtures.block("hbmspace:ore_iron_duna"));
            writeChunk(world.resolve("dimensions/hbmspace/duna/region/r.0.0.mca"), chunk);
            CompoundTag own = DeFixtures.emptyChunk(0, 0);
            own.remove(ChunkConverter.STAMP);
            writeChunk(world.resolve("dimensions/ntmspace/eve/region/r.0.0.mca"), own);
            CompoundTag relay = new CompoundTag();
            ListTag relays = new ListTag();
            relays.add(deSatellite(9, "relay"));
            relay.put("sats", relays);
            writeDe(world.resolve("dimensions/hbmspace/mun/data/hbm_satellites.dat"), relay);
            ListTag flat = new ListTag();
            flat.add(flatSatellite(7, "war"));
            flat.add(flatSatellite(8, "radar"));
            CompoundTag flatData = new CompoundTag();
            flatData.put("sats", flat);
            CompoundTag flatRoot = new CompoundTag();
            flatRoot.put("data", flatData);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(flatRoot, world.resolve("data/hbm_satellites.dat"));
            CompoundTag player = new CompoundTag();
            player.putString("Dimension", "hbmspace:mun");
            player.putString("SpawnDimension", "hbmspace:duna");
            CompoundTag attachments = new CompoundTag();
            CompoundTag breath = new CompoundTag();
            breath.putInt("oxygen", 80);
            attachments.put("hbmspace:breath", breath);
            player.put("neoforge:attachments", attachments);
            Files.createDirectories(world.resolve("playerdata"));
            NbtIo.writeCompressed(player, world.resolve("playerdata/00000000-0000-0000-0000-000000000001.dat"));
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
            for (String id : List.of("backup@1", "regions@1", "players@1", "saveddata@1")) state.markDone(id);
            state.status("done");
            state.save();

            Ctx ctx = ctx(world);
            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            CompoundTag out;
            try (RegionFile region = RegionIo.open(world.resolve("dimensions/ntmspace/duna/region/r.0.0.mca"))) {
                out = RegionIo.read(region, new ChunkPos(0, 0));
            }
            CompoundTag ore = out == null ? null : ChunkBlocks.get(out, pos);
            helper.assertTrue(ore != null && ore.getString("Name").equals("ntmspace:ore_iron"), "Duna converts where it was left: " + ore);
            CompoundTag moved = read(world.resolve("playerdata/00000000-0000-0000-0000-000000000001.dat"));
            helper.assertTrue(moved.getString("Dimension").equals("ntmspace:mun") && moved.getString("SpawnDimension").equals("ntmspace:duna")
                    && moved.getCompound("neoforge:attachments").contains("ntmspace:breath"), "the player stays on Mun: " + moved);
            SatelliteSavedData overworld = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, read(world.resolve("data/hbm_satellites.dat")).getCompound("data")).getOrThrow();
            helper.assertTrue(overworld.getSatFromFreq(7) != null && overworld.getSatFromFreq(8) != null, "the railgun and DE's radar load");
            SatelliteSavedData mun = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE,
                    read(world.resolve("dimensions/ntmspace/mun/data/hbm_satellites.dat")).getCompound("data")).getOrThrow();
            helper.assertTrue(mun.getSatFromFreq(9) != null, "the relay on Mun, which nobody landed on, loads");
            helper.assertTrue(ctx.report.get("dimensions not converted") == 0, "NTM: Space's own Eve is not reported: " + ctx.report.summary());
            String report = Files.readString(world.resolve(Report.FILE));
            helper.assertTrue(report.contains("no import backup was found"), "a world without its backup says what cannot come back: " + report);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** A late conversion resumed after a crash keeps what it converted: the copy from the backup is journaled. */
    private static void lateResume(GameTestHelper helper) {
        Path parent = tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-020000");
        try {
            CompoundTag raw = entity("hbmspace:entity_scrapfish");
            raw.putByte("type", (byte) 1);
            writeChunk(backup.resolve("dimensions/hbmspace/mun/entities/r.0.0.mca"), entityChunk(raw));
            writeChunk(backup.resolve("dimensions/hbmspace/mun/region/r.0.0.mca"), DeFixtures.emptyChunk(0, 0));
            Files.createDirectories(world);
            Files.writeString(world.resolve(BackupStep.RECORD), backup.getFileName().toString());
            CompoundTag converted = entity("ntmspace:entity_scrapfish");
            converted.putString("variant", "PINK");
            CompoundTag done = entityChunk(converted);
            done.putBoolean("hbm_de_converted", true);
            Path live = world.resolve("dimensions/ntmspace/mun");
            writeChunk(live.resolve("entities/r.0.0.mca"), done);
            CompoundTag terrain = DeFixtures.emptyChunk(0, 0);
            terrain.remove(ChunkConverter.STAMP);
            writeChunk(live.resolve("region/r.0.0.mca"), terrain);
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
            state.markDone("regions@1");
            state.status("done");
            state.save();
            Ctx ctx = ctx(world);
            ctx.journal.mark("spacedimensions@1", world, world.resolve("dimensions/ntmspace"));
            ctx.journal.mark("spacedimensions@1", world, live.resolve("moved"));
            ctx.journal.mark("spacedimensions@1", world, live.resolve("from_backup"));
            ctx.journal.mark("spacedimensions@1", world, live.resolve("region"));
            ctx.journal.mark("spacedimensions@1", world, live.resolve("entities"));
            ctx.journal.mark("regions@1", world, live.resolve("entities/r.0.0.mca"));
            Path duna = world.resolve("dimensions/ntmspace/duna");
            writeChunk(backup.resolve("dimensions/hbmspace/duna/entities/r.0.0.mca"), entityChunk(raw.copy()));
            writeChunk(backup.resolve("dimensions/hbmspace/duna/region/r.0.0.mca"), DeFixtures.emptyChunk(0, 0));
            writeChunk(duna.resolve("entities/r.0.0.mca"), done.copy());
            writeChunk(duna.resolve("region/r.0.0.mca"), terrain.copy());
            ctx.journal.mark("spacedimensions@1", world, duna.resolve("moved"));
            ctx.journal.mark("spacedimensions@1", world, duna.resolve("in_place"));
            ctx.journal.mark("regions@1", world, duna.resolve("entities/r.0.0.mca"));

            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            CompoundTag out;
            try (RegionFile region = RegionIo.open(live.resolve("entities/r.0.0.mca"))) {
                out = RegionIo.read(region, new ChunkPos(0, 0));
            }
            CompoundTag fish = out == null ? new CompoundTag() : out.getList("Entities", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(fish.getString("variant").equals("PINK") && !fish.contains("type"), "the converted scrapfish stays: " + fish);
            helper.assertTrue(!Files.exists(world.resolve("hbm_de_kept/hbmspace/mun")), "nothing was copied over it again");
            try (RegionFile region = RegionIo.open(duna.resolve("entities/r.0.0.mca"))) {
                out = RegionIo.read(region, new ChunkPos(0, 0));
            }
            CompoundTag dunaFish = out == null ? new CompoundTag() : out.getList("Entities", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(dunaFish.getString("variant").equals("PINK") && !Files.exists(world.resolve("hbm_de_kept/hbmspace/duna")),
                    "a body that began converting where it was is not taken from the backup: " + dunaFish);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(parent);
        }
    }

    /**
     * DE: Space's PLSS oxygen and complex capacitor charge, a DE: Space stack its alias loaded before the converter saw
     * it, and the ids registered so stacks in other mods' storage load.
     */
    private static void componentsAndLegacy(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world);
            CompoundTag plss = DeFixtures.stack("hbmspace:oxy_plss", 1);
            CompoundTag oxygen = new CompoundTag();
            oxygen.putString("type", "hbm:oxygen");
            oxygen.putLong("amount", 12000L);
            CompoundTag plssComponents = new CompoundTag();
            plssComponents.put("hbm:fluid_content", oxygen);
            plss.put("components", plssComponents);
            CompoundTag capacitor = DeFixtures.stack("hbmspace:capacitor_complex", 1);
            CompoundTag charge = new CompoundTag();
            charge.putLong("power", 5000L);
            charge.putLong("max_power", 9000L);
            CompoundTag capacitorComponents = new CompoundTag();
            capacitorComponents.put("hbm:capacitor_state", charge);
            capacitor.put("components", capacitorComponents);
            CompoundTag drive = DeFixtures.stack("ntmspace:hard_drive_full_duna", 1);
            CompoundTag driveData = new CompoundTag();
            driveData.put("approx", new IntArrayTag(new int[]{3, 4}));
            CompoundTag driveComponents = new CompoundTag();
            driveComponents.put("hbmspace:drive", driveData);
            drive.put("components", driveComponents);
            CompoundTag chest = DeFixtures.inventory(DeFixtures.slot(0, plss), DeFixtures.slot(1, capacitor), DeFixtures.slot(2, drive));
            new Walker(ctx).walk(chest, "test chest");

            CompoundTag plssOut = find(chest, "ntmspace:oxy_plss");
            helper.assertTrue(plssOut != null && plssOut.getCompound("components").getInt("ntmspace:plss_fuel") == 12000
                    && !plssOut.getCompound("components").contains("hbm:fluid_amount"), "the PLSS keeps its oxygen: " + plssOut);
            CompoundTag capacitorOut = find(chest, "ntmspace:capacitor_complex");
            CompoundTag state = capacitorOut == null ? new CompoundTag() : capacitorOut.getCompound("components").getCompound("ntmspace:capacitor_state");
            helper.assertTrue(state.getLong("power") == 5000L && state.getLong("max_power") == 9000L, "the capacitor keeps its charge: " + capacitorOut);
            CompoundTag driveOut = find(chest, "ntmspace:hard_drive_full_duna");
            CompoundTag driveState = driveOut == null ? new CompoundTag() : driveOut.getCompound("components").getCompound("ntmspace:drive");
            helper.assertTrue(driveState.getInt("ax") == 3 && driveState.getInt("az") == 4, "the aliased drive converts: " + driveOut);
            for (CompoundTag out : List.of(plssOut, capacitorOut, driveOut))
                helper.assertTrue(List.of().equals(ContinuumStacks.undecodable(helper.getLevel().registryAccess(), out)), "decodes: " + out);
            helper.assertTrue(DeLegacy.isRaw("hbmspace:drive") && DeLegacy.isRaw("hbmspace:transporter_links"),
                    "DE: Space's components load raw until their stack converts");
            helper.assertTrue("ntmspace:kmno4".equals(String.valueOf(BuiltInRegistries.FLUID.getKey(
                    BuiltInRegistries.FLUID.get(ResourceLocation.parse("hbmspace:kmno4"))))), "Eve's acid has a fluid alias");

            // a PLSS in a helmet's modules, in storage the importer never reached: it converts on touch, the helmet stays
            CompoundTag held = DeFixtures.stack("ntmspace:oxy_plss", 1);
            CompoundTag deOxygen = new CompoundTag();
            deOxygen.putString("type", "hbm:oxygen");
            deOxygen.putLong("amount", 12000L);
            CompoundTag heldComponents = new CompoundTag();
            heldComponents.put("hbm:fluid_content", deOxygen);
            held.put("components", heldComponents);
            ListTag modules = new ListTag();
            modules.add(held);
            CompoundTag helmetComponents = new CompoundTag();
            helmetComponents.put("hbm:armor_mods", modules);
            CompoundTag helmetTag = DeFixtures.stack("minecraft:iron_helmet", 1);
            helmetTag.put("components", helmetComponents);
            ItemStack helmet = ItemStack.parse(helper.getLevel().registryAccess(), helmetTag).orElseThrow();
            helper.assertTrue(LegacyStacks.isLegacy(helmet), "the helmet holds a stack still in DE's shape");
            ItemStack converted = LegacyStacks.convert(helmet, helper.getLevel().getServer(), "test helmet");
            CompoundTag saved = (CompoundTag) converted.save(helper.getLevel().registryAccess());
            CompoundTag module = saved.getCompound("components").getList("hbm:armor_mods", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(converted.is(Items.IRON_HELMET) && !LegacyStacks.isLegacy(converted)
                    && module.getCompound("components").getInt("ntmspace:plss_fuel") == 12000, "the PLSS in the helmet keeps its oxygen: " + saved);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /**
     * A world imported without NTM: Space: a player who has not played since is converted again from the backup with
     * DE: Space's gear, one who has keeps it and follows the renamed dimensions, links outside the bodies follow too,
     * satellites a list Continuum saved over had lost come back, and Continuum's own saved data stays.
     */
    private static void latePlayersAndLinks(GameTestHelper helper) {
        Path parent = tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-030000");
        try {
            String fresh = "00000000-0000-0000-0000-000000000001.dat", played = "00000000-0000-0000-0000-000000000002.dat";
            CompoundTag plss = DeFixtures.stack("hbmspace:oxy_plss", 1);
            CompoundTag oxygen = new CompoundTag();
            oxygen.putString("type", "hbm:oxygen");
            oxygen.putLong("amount", 9000L);
            CompoundTag plssComponents = new CompoundTag();
            plssComponents.put("hbm:fluid_content", oxygen);
            plss.put("components", plssComponents);
            CompoundTag original = new CompoundTag();
            original.putString("Dimension", "hbmspace:mun");
            ListTag inventory = new ListTag();
            inventory.add(DeFixtures.slot(0, plss));
            original.put("Inventory", inventory);
            Files.createDirectories(backup.resolve("playerdata"));
            Files.createDirectories(backup.resolve("dimensions/hbmspace/mun"));
            NbtIo.writeCompressed(original, backup.resolve("playerdata/" + fresh));
            Files.createDirectories(world.resolve("playerdata"));
            Files.writeString(world.resolve(BackupStep.RECORD), backup.getFileName().toString());
            CompoundTag stripped = new CompoundTag();
            stripped.putString("Dimension", "hbmspace:mun");
            stripped.putBoolean(PlayersStep.CONVERTED, true);
            NbtIo.writeCompressed(stripped, world.resolve("playerdata/" + fresh));
            CompoundTag linker = DeFixtures.stack("hbm:linker", 1);
            CompoundTag link = new CompoundTag();
            link.putString("dim", "hbmspace:duna");
            CompoundTag linkComponents = new CompoundTag();
            linkComponents.put("hbm:tele_link", link);
            linker.put("components", linkComponents);
            CompoundTag since = new CompoundTag();
            since.putString("Dimension", "hbmspace:mun");
            ListTag carried = new ListTag();
            carried.add(DeFixtures.slot(0, linker));
            since.put("Inventory", carried);
            NbtIo.writeCompressed(since, world.resolve("playerdata/" + played));
            CompoundTag before = new CompoundTag();
            before.putString("Dimension", "hbmspace:mun");
            ListTag gear = new ListTag();
            gear.add(DeFixtures.slot(0, plss.copy()));
            before.put("Inventory", gear);
            CompoundTag nitan = new CompoundTag();
            nitan.putInt("nitan", 2);
            nitan.putFloat("nitan_health", 20F);
            CompoundTag deAttachments = new CompoundTag();
            deAttachments.put("hbmspace:nitan", nitan);
            CompoundTag deProps = new CompoundTag();
            deProps.putFloat("activation", 2.5F);
            deAttachments.put("hbm:living_props", deProps);
            before.put("neoforge:attachments", deAttachments);
            NbtIo.writeCompressed(before, backup.resolve("playerdata/" + played));
            CompoundTag shifted = new CompoundTag();
            shifted.putInt("XpLevel", 7);
            shifted.putBoolean(PlayersStep.CONVERTED, true);
            NbtIo.writeCompressed(shifted, world.resolve("playerdata/" + played + "_old"));
            CompoundTag older = new CompoundTag();
            older.putInt("XpLevel", 42);
            NbtIo.writeCompressed(older, backup.resolve("playerdata/" + played + "_old"));
            Files.createDirectories(world.resolve("dimensions/hbmspace/mun/data"));
            Files.writeString(world.resolve("dimensions/hbmspace/mun/data/raids.dat"), "de: space");

            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            chunk.remove(ChunkConverter.STAMP);
            DeFixtures.setBlock(chunk, new BlockPos(5, 70, 5), DeFixtures.block("hbmspace:ore_iron_duna"));
            DeFixtures.setBlock(chunk, new BlockPos(6, 70, 6), DeFixtures.block("hbm:pwr_block"));
            CompoundTag teleporter = DeFixtures.blockEntity("hbm:machine_teleporter", new BlockPos(1, 70, 1));
            CompoundTag target = new CompoundTag();
            target.putString("dimension", "hbmspace:mun");
            teleporter.put("target", target);
            chunk.getList("block_entities", Tag.TAG_COMPOUND).add(teleporter);
            BlockPos chestPos = new BlockPos(8, 70, 8), built = new BlockPos(9, 70, 9), regrown = new BlockPos(20, 70, 4);
            DeFixtures.setBlock(chunk, chestPos, DeFixtures.block("minecraft:chest"));
            DeFixtures.setBlock(chunk, built, DeFixtures.block("minecraft:stone"));
            CompoundTag liveChest = DeFixtures.blockEntity("minecraft:chest", chestPos);
            ListTag liveItems = new ListTag();
            liveItems.add(DeFixtures.slot(1, DeFixtures.stack("minecraft:shulker_box", 1)));
            liveChest.put("Items", liveItems);
            chunk.getList("block_entities", Tag.TAG_COMPOUND).add(liveChest);
            writeChunk(world.resolve("region/r.0.0.mca"), chunk);
            // the same chunk as DE saved it: the game dropped its ore at 7, 70, 7 and the chest's moon rock when it loaded
            CompoundTag saved = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(saved, new BlockPos(5, 70, 5), DeFixtures.block("hbmspace:ore_iron_duna"));
            DeFixtures.setBlock(saved, new BlockPos(7, 70, 7), DeFixtures.block("hbmspace:ore_iron_duna"));
            DeFixtures.setBlock(saved, chestPos, DeFixtures.block("minecraft:chest"));
            CompoundTag savedChest = DeFixtures.blockEntity("minecraft:chest", chestPos);
            ListTag savedItems = new ListTag();
            savedItems.add(DeFixtures.slot(0, DeFixtures.stack("hbmspace:moon_rock", 7)));
            CompoundTag box = DeFixtures.stack("minecraft:shulker_box", 1), boxComponents = new CompoundTag(), inBox = new CompoundTag();
            ListTag boxContents = new ListTag();
            inBox.putInt("slot", 0);
            inBox.put("item", DeFixtures.stack("hbmspace:moon_rock", 4));
            boxContents.add(inBox);
            boxComponents.put("minecraft:container", boxContents);
            box.put("components", boxComponents);
            savedItems.add(DeFixtures.slot(1, box));
            savedChest.put("Items", savedItems);
            saved.getList("block_entities", Tag.TAG_COMPOUND).add(savedChest);
            DeFixtures.setBlock(saved, built, DeFixtures.block("hbmspace:ore_iron_duna"));
            writeChunk(backup.resolve("region/r.0.0.mca"), saved);
            // a chunk regenerated since: its inhabited time starts again at 0
            CompoundTag lived = DeFixtures.emptyChunk(1, 0);
            lived.putLong("InhabitedTime", 500L);
            DeFixtures.setBlock(lived, regrown, DeFixtures.block("hbmspace:ore_iron_duna"));
            writeChunk(backup.resolve("region/r.0.0.mca"), lived);
            CompoundTag reborn = DeFixtures.emptyChunk(1, 0);
            reborn.remove(ChunkConverter.STAMP);
            writeChunk(world.resolve("region/r.0.0.mca"), reborn);

            ListTag now = new ListTag();
            now.add(flatSatellite(1, "radar"));
            CompoundTag nowData = new CompoundTag();
            nowData.put("sats", now);
            CompoundTag nowRoot = new CompoundTag();
            nowRoot.put("data", nowData);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(nowRoot, world.resolve("data/hbm_satellites.dat"));
            ListTag lost = new ListTag();
            lost.add(deSatellite(2, "radar"));
            lost.add(deSatellite(3, "war"));
            CompoundTag lostData = new CompoundTag();
            lostData.put("sats", lost);
            CompoundTag lostInner = new CompoundTag();
            lostInner.put("data", lostData);
            CompoundTag lostRoot = new CompoundTag();
            lostRoot.put("data", lostInner);
            CompoundTag record = new CompoundTag();
            record.putString("kind", "saved_data");
            record.putString("where", "minecraft:overworld hbm_satellites");
            record.putString("id", "hbm_satellites");
            record.put("nbt", lostRoot);
            ListTag records = new ListTag();
            records.add(record);
            ListTag plain = new ListTag();
            plain.add(deSatellite(11, "radar"));
            CompoundTag plainData = new CompoundTag();
            plainData.put("sats", plain);
            CompoundTag plainInner = new CompoundTag();
            plainInner.put("data", plainData);
            CompoundTag plainRoot = new CompoundTag();
            plainRoot.put("data", plainInner);
            CompoundTag readable = new CompoundTag();
            readable.putString("kind", "saved_data");
            readable.putString("where", "minecraft:the_nether hbm_satellites");
            readable.putString("id", "hbm_satellites");
            readable.put("nbt", plainRoot);
            records.add(readable);
            CompoundTag netherRoot = new CompoundTag();
            netherRoot.put("data", new CompoundTag());
            Files.createDirectories(world.resolve("DIM-1/data"));
            NbtIo.writeCompressed(netherRoot, world.resolve("DIM-1/data/hbm_satellites.dat"));
            CompoundTag leftovers = new CompoundTag();
            leftovers.put("records", records);
            CompoundTag annihilator = new CompoundTag();
            annihilator.put("pools", new ListTag());
            CompoundTag ownRoot = new CompoundTag();
            ownRoot.put("data", annihilator);
            NbtIo.writeCompressed(ownRoot, world.resolve("data/hbm_annihilator.dat"));
            CompoundTag rocket = entity("hbmspace:entity_rideable_rocket");
            rocket.putInt("state", 0);
            rocket.putIntArray("UUID", new int[]{1, 2, 3, 4});
            CompoundTag rocketChunk = entityChunk(rocket);
            rocketChunk.putIntArray("Position", new int[]{0, 0});
            CompoundTag frame = entity("minecraft:item_frame");
            frame.putIntArray("UUID", new int[]{5, 6, 7, 8});
            CompoundTag framed = frame.copy();
            framed.put("Item", DeFixtures.stack("hbmspace:moon_rock", 1));
            rocketChunk.getList("Entities", Tag.TAG_COMPOUND).add(framed);
            writeChunk(backup.resolve("entities/r.0.0.mca"), rocketChunk);
            writeChunk(world.resolve("entities/r.0.0.mca"), entityChunk(frame));
            // a rocket of the chunk regenerated since, and one of a chunk the world no longer has
            CompoundTag stranded = entity("hbmspace:entity_rideable_rocket");
            stranded.putInt("state", 0);
            stranded.putIntArray("UUID", new int[]{9, 9, 9, 9});
            // aboard an airship: Sable keeps its entities in its plot grid's chunks, which wait for the airship step
            int yard = 10000 << 7;
            CompoundTag shipFrame = entity("minecraft:item_frame");
            shipFrame.putIntArray("UUID", new int[]{20, 20, 20, 20});
            CompoundTag shipFramed = shipFrame.copy();
            shipFramed.put("Item", DeFixtures.stack("hbmspace:moon_rock", 1));
            CompoundTag rider = stranded.copy();
            rider.putIntArray("UUID", new int[]{21, 21, 21, 21});
            CompoundTag lone = stranded.copy();
            lone.putIntArray("UUID", new int[]{22, 22, 22, 22});
            String plotFile = "entities/r." + (yard >> 5) + "." + (yard >> 5) + ".mca";
            for (int x : new int[]{0, 1}) {
                CompoundTag aboard = entityChunk(x == 0 ? shipFramed : lone);
                if (x == 0) aboard.getList("Entities", Tag.TAG_COMPOUND).add(rider);
                aboard.putInt("xPos", yard + x);
                aboard.putInt("zPos", yard);
                aboard.putIntArray("Position", new int[]{yard + x, yard});
                writeChunk(backup.resolve(plotFile), aboard);
            }
            CompoundTag liveAboard = entityChunk(shipFrame);
            liveAboard.putInt("xPos", yard);
            liveAboard.putInt("zPos", yard);
            liveAboard.putIntArray("Position", new int[]{yard, yard});
            writeChunk(world.resolve(plotFile), liveAboard);
            for (int x : new int[]{1, 32}) {
                CompoundTag at = entityChunk(stranded.copy());
                at.putInt("xPos", x);
                at.putIntArray("Position", new int[]{x, 0});
                writeChunk(backup.resolve("entities/r." + (x >> 5) + ".0.mca"), at);
            }
            CompoundTag single = new CompoundTag();
            single.putString("kind", "satellite");
            single.putString("where", "minecraft:overworld hbm_satellites freq 4");
            single.putString("id", "dyson_relay");
            single.put("nbt", deSatellite(4, "dyson_relay"));
            records.add(single);
            NbtIo.writeCompressed(leftovers, world.resolve(Parking.FILE));
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "1.0.3");
            for (String id : List.of("backup@1", "regions@1", "players@1", "saveddata@1")) state.markDone(id);
            state.status("done");
            state.save();

            Ctx ctx = ctx(world);
            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            CompoundTag again = read(world.resolve("playerdata/" + fresh));
            CompoundTag plssOut = find(items(again), "ntmspace:oxy_plss");
            helper.assertTrue(plssOut != null && plssOut.getCompound("components").getInt("ntmspace:plss_fuel") == 9000
                    && again.getString("Dimension").equals("ntmspace:mun"), "the player who has not played since gets the PLSS back: " + again);
            CompoundTag moved = read(world.resolve("playerdata/" + played));
            CompoundTag linkerOut = find(items(moved), "hbm:linker");
            helper.assertTrue(moved.getString("Dimension").equals("ntmspace:mun") && linkerOut != null
                    && linkerOut.getCompound("components").getCompound("hbm:tele_link").getString("dim").equals("ntmspace:duna"),
                    "the other keeps their inventory, and its linker follows Duna: " + moved);
            CompoundTag gearOut = find(items(moved), "ntmspace:oxy_plss");
            helper.assertTrue(gearOut != null && gearOut.getByte("Slot") == 1 && gearOut.getCompound("components").getInt("ntmspace:plss_fuel") == 9000
                    && moved.getCompound("neoforge:attachments").getCompound("ntmspace:nitan").getInt("count") == 2
                    && moved.getCompound("neoforge:attachments").getCompound("hbm:living_props").getDouble("neutrons") == 2.5,
                    "the PLSS, NITAN and neutron activation vanilla dropped come back from the backup, the PLSS into a free slot: " + moved);
            helper.assertTrue(read(world.resolve("playerdata/" + played + "_old")).getInt("XpLevel") == 7,
                    "the first import's file vanilla shifted to .dat_old is not taken for the backup's .dat_old");
            CompoundTag terrain;
            try (RegionFile region = RegionIo.open(world.resolve("region/r.0.0.mca"))) {
                terrain = RegionIo.read(region, new ChunkPos(0, 0));
            }
            String linked = terrain == null ? "" : terrain.getList("block_entities", Tag.TAG_COMPOUND).getCompound(0).getCompound("target").getString("dimension");
            helper.assertTrue(linked.equals("ntmspace:mun"), "the overworld teleporter targets NTM: Space's Mun: " + linked);
            expect(helper, terrain, new BlockPos(5, 70, 5), "ntmspace:ore_iron", "body", "duna");
            expect(helper, terrain, new BlockPos(7, 70, 7), "ntmspace:ore_iron", "body", "duna");
            expect(helper, terrain, built, "minecraft:stone", null, null);
            CompoundTag regenerated;
            try (RegionFile region = RegionIo.open(world.resolve("region/r.0.0.mca"))) {
                regenerated = RegionIo.read(region, new ChunkPos(1, 0));
            }
            expect(helper, regenerated, regrown, "minecraft:air", null, null);
            String chestBack = "";
            for (Tag t : terrain.getList("block_entities", Tag.TAG_COMPOUND)) if (((CompoundTag) t).getString("id").equals("minecraft:chest")) chestBack = t.toString();
            helper.assertTrue(chestBack.contains("ntmspace:moon_rock"), "the chest gets back the moon rock the game dropped: " + chestBack);
            helper.assertTrue(chestBack.contains("minecraft:container") && chestBack.contains("count:4"),
                    "and its shulker box what it held: " + chestBack);
            expect(helper, terrain, new BlockPos(6, 70, 6), "hbm:pwr_block", null, null);
            SatelliteSavedData sats = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, read(world.resolve("data/hbm_satellites.dat")).getCompound("data")).getOrThrow();
            helper.assertTrue(sats.getSatFromFreq(1) != null && sats.getSatFromFreq(2) != null && sats.getSatFromFreq(3) != null
                    && sats.getSatFromFreq(4) != null, "the satellite launched since stays and the lost ones come back, the one parked alone too");
            helper.assertTrue(read(world.resolve("DIM-1/data/hbm_satellites.dat")).getCompound("data").getList("sats", Tag.TAG_COMPOUND).isEmpty(),
                    "a list Continuum read stays as Continuum left it: a satellite missing there was removed since");
            CompoundTag back;
            try (RegionFile region = RegionIo.open(world.resolve("entities/r.0.0.mca"))) {
                back = RegionIo.read(region, new ChunkPos(0, 0));
            }
            String rocketBack = "", frameBack = "";
            for (Tag t : back == null ? new ListTag() : back.getList("Entities", Tag.TAG_COMPOUND)) {
                if (((CompoundTag) t).getString("id").equals("minecraft:item_frame")) frameBack = t.toString();
                else rocketBack = t.toString();
            }
            helper.assertTrue(frameBack.contains("ntmspace:moon_rock"), "the item frame gets back the moon rock the game dropped: " + frameBack);
            helper.assertTrue(rocketBack.contains("ntmspace:entity_rideable_rocket") && rocketBack.contains("AWAITING"),
                    "the rocket the first start dropped comes back from the backup: " + rocketBack);
            CompoundTag strandedBack;
            try (RegionFile region = RegionIo.open(world.resolve("entities/r.0.0.mca"))) {
                strandedBack = RegionIo.read(region, new ChunkPos(1, 0));
            }
            helper.assertTrue(strandedBack == null && !Files.exists(world.resolve("entities/r.1.0.mca")),
                    "a chunk regenerated since, or one the world no longer has, gets no entity back: " + strandedBack);
            CompoundTag aboardBack, loneBack;
            try (RegionFile region = RegionIo.open(world.resolve(plotFile))) {
                aboardBack = RegionIo.read(region, new ChunkPos(yard, yard));
                loneBack = RegionIo.read(region, new ChunkPos(yard + 1, yard));
            }
            String aboard = aboardBack == null ? "" : aboardBack.toString();
            helper.assertTrue(!aboard.contains("moon_rock") && !aboard.contains("[I;21,21,21,21]") && loneBack == null,
                    "entities aboard airships wait for the airship step, which runs with Sable: " + aboard);
            CompoundTag plot = new CompoundTag();
            plot.putString("biome", "hbmspace:duna_polar");
            plot.put("chunks", new CompoundTag());
            CompoundTag ship = new CompoundTag();
            ship.put("plot", plot);
            Airships.convert(ctx, "ntmspace:duna", ship, "test airship");
            helper.assertTrue(ship.getCompound("plot").getString("biome").startsWith("ntmspace:"), "Sable's plot biome follows: " + ship);
            helper.assertTrue(Files.isRegularFile(world.resolve("data/hbm_annihilator.dat")), "Continuum's own annihilator stays");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(parent);
        }
    }

    /**
     * A copy from the backup a crash interrupted is only the backup's: it is copied again, not converted as it stands.
     * The body counts as moved by its journaled choice alone, its airships come along, and a read-only backup works.
     */
    private static void lateRestoreResume(GameTestHelper helper) {
        Path parent = tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-040000");
        File locked = null;
        try {
            BlockPos near = new BlockPos(2, 100, 2), far = new BlockPos(642, 100, 2);
            CompoundTag first = DeFixtures.emptyChunk(0, 0), second = DeFixtures.emptyChunk(40, 0);
            DeFixtures.setBlock(first, near, DeFixtures.block("hbmspace:ore_iron_mun"));
            DeFixtures.setBlock(second, far, DeFixtures.block("hbmspace:ore_iron_mun"));
            writeChunk(backup.resolve("dimensions/hbmspace/mun/region/r.0.0.mca"), first);
            writeChunk(backup.resolve("dimensions/hbmspace/mun/region/r.1.0.mca"), second);
            locked = backup.resolve("dimensions/hbmspace/mun/region/r.1.0.mca").toFile();
            helper.assertTrue(locked.setReadOnly(), "the backup file is read-only");
            Files.createDirectories(backup.resolve("dimensions/hbmspace/mun/sublevels"));
            Files.writeString(backup.resolve("dimensions/hbmspace/mun/sublevels/r.0.0.0.slvls"), "de: space ships");
            Files.createDirectories(world);
            Files.writeString(world.resolve(BackupStep.RECORD), backup.getFileName().toString());
            Path live = world.resolve("dimensions/ntmspace/mun");
            writeChunk(live.resolve("region/r.0.0.mca"), first.copy());
            Files.createDirectories(world.resolve("hbm_de_kept/hbmspace/mun/region"));
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "1.0.3");
            for (String id : List.of("backup@1", "sublevels@1", "regions@1", "players@1", "saveddata@1")) state.markDone(id);
            state.status("done");
            state.save();
            Ctx ctx = ctx(world);
            ctx.journal.mark("spacedimensions@1", world, world.resolve("dimensions/ntmspace"));
            ctx.journal.mark("spacedimensions@1", world, live.resolve("from_backup"));
            ctx.journal.mark("spacedimensions@1", world, live.resolve("region.cleared"));

            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            CompoundTag out0, out1;
            try (RegionFile region = RegionIo.open(live.resolve("region/r.0.0.mca"))) {
                out0 = RegionIo.read(region, new ChunkPos(0, 0));
            }
            try (RegionFile region = RegionIo.open(live.resolve("region/r.1.0.mca"))) {
                out1 = RegionIo.read(region, new ChunkPos(40, 0));
            }
            helper.assertTrue(out0 != null && out1 != null, "both region files are back: " + out0 + " " + out1);
            expect(helper, out0, near, "ntmspace:ore_iron", "body", "mun");
            expect(helper, out1, far, "ntmspace:ore_iron", "body", "mun");
            helper.assertTrue(!Files.exists(world.resolve("hbm_de_kept/hbmspace/mun/region_2")), "the half copy is not kept aside");
            helper.assertTrue(ctx.report.get("dimensions converted from the backup") == 1, ctx.report.summary());
            helper.assertTrue(Files.isWritable(live.resolve("region/r.1.0.mca")), "the copy of a read-only backup file can be converted");
            Step airships = Steps.all().stream().filter(s -> s.id().startsWith("spaceairships")).findFirst().orElseThrow();
            helper.assertTrue(ModList.get().isLoaded("sable") ? Files.isRegularFile(live.resolve("sublevels/r.0.0.0.slvls"))
                    : Files.isRegularFile(live.resolve("sublevels.de-pending/r.0.0.0.slvls")) && airships.applies(ctx),
                    "the airships come back too, and without Sable wait for it where it cannot load them");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (locked != null) locked.setWritable(true);
            delete(parent);
        }
    }

    /**
     * An import 1.0.3 began and did not finish, resumed with NTM: Space: a body whose first chunk 1.0.3 had not reached
     * but whose second it converted comes back from the backup whole, before DE's terrain step goes on.
     */
    private static void latePartialImport(GameTestHelper helper) {
        Path parent = tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-050000");
        try {
            BlockPos near = new BlockPos(2, 100, 2), far = new BlockPos(18, 100, 2);
            CompoundTag first = DeFixtures.emptyChunk(0, 0), second = DeFixtures.emptyChunk(1, 0);
            DeFixtures.setBlock(first, near, DeFixtures.block("hbmspace:ore_iron_mun"));
            DeFixtures.setBlock(second, far, DeFixtures.block("hbmspace:ore_iron_mun"));
            Path backed = backup.resolve("dimensions/hbmspace/mun/region/r.0.0.mca");
            writeChunk(backed, first);
            writeChunk(backed, second);
            Files.createDirectories(world);
            Files.writeString(world.resolve(BackupStep.RECORD), backup.getFileName().toString());
            Path old = world.resolve("dimensions/hbmspace/mun/region/r.0.0.mca");
            writeChunk(old, first.copy());
            CompoundTag reached = second.copy();
            reached.remove(ChunkConverter.STAMP);
            writeChunk(old, reached);
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "1.0.3");
            state.markDone("backup@1");
            state.save();
            Ctx ctx = ctx(world);

            Step dims = Steps.all().stream().filter(s -> s.id().startsWith("spacedimensions")).findFirst().orElseThrow();
            dims.run(ctx, null);
            new RegionsStep().run(ctx, null);
            Step late = Steps.all().stream().filter(s -> s.id().startsWith("spacelate")).findFirst().orElseThrow();
            if (late.applies(ctx)) late.run(ctx, null);
            CompoundTag out;
            try (RegionFile region = RegionIo.open(world.resolve("dimensions/ntmspace/mun/region/r.0.0.mca"))) {
                out = RegionIo.read(region, new ChunkPos(1, 0));
            }
            helper.assertTrue(out != null, "the chunk 1.0.3 converted is there");
            expect(helper, out, far, "ntmspace:ore_iron", "body", "mun");
            helper.assertTrue(Files.isRegularFile(world.resolve("hbm_de_kept/hbmspace/mun/region/r.0.0.mca")), "what 1.0.3 left is kept aside");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(parent);
        }
    }

    /**
     * Where an import without NTM: Space kept satellite lists Continuum could read (left for later, here), a whole list
     * in the leftovers brings nothing back; a satellite parked alone comes back only if it is of a DE: Space type.
     */
    private static void satelliteRestoreGates(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag live = new CompoundTag();
            CompoundTag liveData = new CompoundTag();
            liveData.put("sats", new ListTag());
            live.put("data", liveData);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(live, world.resolve("data/hbm_satellites.dat"));
            Files.createDirectories(world.resolve("dimensions/ntmspace"));
            ListTag whole = new ListTag();
            whole.add(deSatellite(5, "war"));
            CompoundTag wholeData = new CompoundTag();
            wholeData.put("sats", whole);
            CompoundTag wholeInner = new CompoundTag();
            wholeInner.put("data", wholeData);
            CompoundTag wholeRoot = new CompoundTag();
            wholeRoot.put("data", wholeInner);
            ListTag records = new ListTag();
            records.add(record("saved_data", "minecraft:overworld hbm_satellites", "hbm_satellites", wholeRoot));
            records.add(record("satellite", "minecraft:overworld hbm_satellites freq 6", "radar", deSatellite(6, "radar")));
            records.add(record("satellite", "minecraft:overworld hbm_satellites freq 7", "dyson_relay", deSatellite(7, "dyson_relay")));
            CompoundTag leftovers = new CompoundTag();
            leftovers.put("records", records);
            NbtIo.writeCompressed(leftovers, world.resolve(Parking.FILE));
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
            state.put("addons", "");
            state.put("deferred_addons", "");
            state.status("done");
            state.save();

            Ctx ctx = ctx(world);
            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            SatelliteSavedData sats = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, read(world.resolve("data/hbm_satellites.dat")).getCompound("data")).getOrThrow();
            helper.assertTrue(sats.getSatFromFreq(5) == null && sats.getSatFromFreq(6) == null && sats.getSatFromFreq(7) != null,
                    "only the dyson relay parked for its type comes back");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static CompoundTag record(String kind, String where, String id, CompoundTag nbt) {
        CompoundTag record = new CompoundTag();
        record.putString("kind", kind);
        record.putString("where", where);
        record.putString("id", id);
        record.put("nbt", nbt);
        return record;
    }

    /** How many stations NTM: Space's own codec reads from a stations file; a bad entry fails the read. */
    private static int stationCount(Path file) throws IOException {
        StationData data = StationData.CODEC.parse(NbtOps.INSTANCE, read(file).getCompound("data")).getOrThrow();
        return ((CompoundTag) StationData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getList("stations", Tag.TAG_COMPOUND).size();
    }

    /** NTM: Space's own stations do not join DE: Space's list when NTM: Space's orbit, which holds their blocks, was set aside. */
    private static void stationOrbitKept(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag solar = new CompoundTag();
            solar.put("b_mun", new CompoundTag());
            solar.put("stations", stations(1));
            writeDe(world.resolve("data/hbmspace_solar_system.dat"), solar);
            CompoundTag ownStations = new CompoundTag();
            ownStations.put("stations", ownStations(3));
            CompoundTag ownStationsRoot = new CompoundTag();
            ownStationsRoot.put("data", ownStations);
            NbtIo.writeCompressed(ownStationsRoot, world.resolve("data/ntmspace_stations.dat"));
            writeChunk(world.resolve("dimensions/hbmspace/orbit/region/r.0.0.mca"), DeFixtures.emptyChunk(0, 0));
            CompoundTag built = DeFixtures.emptyChunk(0, 0);
            built.remove(ChunkConverter.STAMP);
            writeChunk(world.resolve("dimensions/ntmspace/orbit/region/r.0.0.mca"), built);

            Ctx ctx = ctx(world);
            for (Step step : Steps.all()) if (step.id().startsWith("space") && step.applies(ctx)) step.run(ctx, null);
            helper.assertTrue(Files.isRegularFile(world.resolve("hbm_de_kept/ntmspace/orbit/region/r.0.0.mca")), "NTM: Space's orbit was set aside");
            ListTag stations = read(world.resolve("data/ntmspace_stations.dat")).getCompound("data").getList("stations", Tag.TAG_COMPOUND);
            helper.assertTrue(stationCount(world.resolve("data/ntmspace_stations.dat")) == 1 && stations.getCompound(0).getInt("x") == 1,
                    "only DE: Space's station is listed: " + stations);
            helper.assertTrue(Files.isRegularFile(world.resolve("hbm_de_kept/data/ntmspace_stations.dat")), "NTM: Space's list is kept aside");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** A player's inventory in the shape {@link #find} reads. */
    private static CompoundTag items(CompoundTag player) {
        CompoundTag view = new CompoundTag();
        view.put("Items", player.getList("Inventory", Tag.TAG_COMPOUND));
        return view;
    }

    private static CompoundTag deSatellite(int freq, String type) {
        CompoundTag sat = new CompoundTag();
        sat.putString("type", type);
        CompoundTag outer = new CompoundTag();
        outer.put("sat", sat);
        outer.put("state", new CompoundTag());
        CompoundTag entry = new CompoundTag();
        entry.putInt("freq", freq);
        entry.put("sat", outer);
        return entry;
    }

    /** A satellite as DE's step left it when NTM: Space was not installed: flattened, its DE: Space type bare. */
    private static CompoundTag flatSatellite(int freq, String type) {
        CompoundTag sat = new CompoundTag();
        sat.putString("type", type);
        CompoundTag entry = new CompoundTag();
        entry.putInt("freq", freq);
        entry.put("sat", sat);
        return entry;
    }

    /** One station as NTM: Space saves it, its state by name. */
    private static ListTag ownStations(int square) {
        ListTag list = stations(square);
        list.getCompound(0).putString("state", "ORBIT");
        return list;
    }

    /** One station on the grid square (square, square), in DE: Space's shape. */
    private static ListTag stations(int square) {
        CompoundTag station = new CompoundTag();
        station.putInt("x", square);
        station.putInt("z", square);
        station.putString("orbiting", "kerbin");
        station.putString("target", "kerbin");
        station.putInt("state", 0);
        station.putInt("stateTimer", 0);
        station.putInt("maxStateTimer", 0);
        station.putBoolean("hasStation", true);
        ListTag list = new ListTag();
        list.add(station);
        return list;
    }

    static CompoundTag entityChunk(CompoundTag entity) {
        CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", 3955);
        chunk.putInt("xPos", 0);
        chunk.putInt("zPos", 0);
        chunk.putIntArray("Position", new int[]{0, 0});
        ListTag entities = new ListTag();
        entities.add(entity);
        chunk.put("Entities", entities);
        return chunk;
    }

    static void writeChunk(Path file, CompoundTag chunk) throws IOException {
        Files.createDirectories(file.getParent());
        ChunkPos pos = new ChunkPos(chunk.getInt("xPos"), chunk.getInt("zPos"));
        try (RegionFile region = RegionIo.open(file); DataOutputStream out = region.getChunkDataOutputStream(pos)) {
            NbtIo.write(chunk, out);
        }
    }


    private static void expect(GameTestHelper helper, CompoundTag chunk, BlockPos pos, String name, @Nullable String key, @Nullable String value) {
        CompoundTag state = ChunkBlocks.get(chunk, pos);
        String got = state == null ? "minecraft:air" : state.getString("Name");
        helper.assertTrue(got.equals(name), pos.toShortString() + " is " + state + ", not " + name);
        if (key != null) helper.assertTrue(value.equals(state.getCompound("Properties").getString(key)), pos.toShortString() + " " + state + " has no " + key + "=" + value);
    }

    private static @Nullable CompoundTag find(CompoundTag inventory, String id) {
        for (Tag t : inventory.getList("Items", Tag.TAG_COMPOUND)) if (((CompoundTag) t).getString("id").equals(id)) return (CompoundTag) t;
        return null;
    }

    static CompoundTag entity(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        ListTag pos = new ListTag();
        for (int i = 0; i < 3; i++) pos.add(DoubleTag.valueOf(0D));
        tag.put("Pos", pos);
        return tag;
    }

    /** A saved data file as DE wrote it: its content inside one more {@code data} level. */
    private static void writeDe(Path file, CompoundTag content) throws IOException {
        CompoundTag inner = new CompoundTag();
        inner.put("data", content);
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3955);
        root.put("data", inner);
        Files.createDirectories(file.getParent());
        NbtIo.writeCompressed(root, file);
    }

    private static CompoundTag read(Path file) throws IOException {
        return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
    }

    static Ctx ctx(Path world) throws IOException {
        return new Ctx(world, new ContinuumTarget(null), new Parking(world), new Report(), new Journal(world));
    }

    static Path tempWorld() {
        try {
            return Files.createTempDirectory("ntmspace-deimport");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void delete(Path world) {
        try (Stream<Path> files = Files.walk(world)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        } catch (IOException e) {
            // a temp folder left behind is harmless; the test result stands
        }
    }
}
