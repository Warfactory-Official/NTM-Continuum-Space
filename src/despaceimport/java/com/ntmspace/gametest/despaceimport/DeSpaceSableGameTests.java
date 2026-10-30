package com.ntmspace.gametest.despaceimport;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.PackedSection;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Pipeline;
import com.hbm.deimport.engine.Report;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.steps.BackupStep;
import com.hbm.deimport.steps.Steps;
import com.hbm.gametest.deimport.DeFixtures;
import com.hbm.gametest.deimport.DeSableGameTests;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.neoforged.fml.ModList;

/** Airships in Sable's storage, written through HBM's test helpers; only loaded while Sable is installed. */
final class DeSpaceSableGameTests {
    private DeSpaceSableGameTests() {}

    /** Airships outside the bodies Sable could not write keep the world waiting, and a later start gives them DE: Space's blocks. */
    static void retry(GameTestHelper helper) {
        Path parent = DeSpaceImportGameTests.tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-050000");
        try {
            UUID id = new UUID(7, 7);
            BlockPos lost = new BlockPos(1, 70, 1);
            CompoundTag before = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(before, lost, DeFixtures.block("hbmspace:ore_iron_duna"));
            CompoundTag de = DeFixtures.ship(before);
            de.putUUID("uuid", id);
            DeSableGameTests.write(backup.resolve("sublevels"), de);
            // a storage file Sable cannot open: a folder in its place
            Files.createDirectories(world.resolve("sublevels/r.0.0.0.slvls"));
            ImportState state = late(world, backup);
            Pipeline pipeline = new Pipeline(Steps.all().stream().filter(s -> s.id().startsWith("space")).toList(), ModList.get()::isLoaded);
            Ctx ctx = DeSpaceImportGameTests.ctx(world);
            for (Step.Phase phase : Step.Phase.values()) pipeline.run(phase, state, ctx, null);
            String waits = state.deferredOn("spaceairships@1");
            helper.assertTrue(waits != null && waits.contains("could not be read or written") && !state.stepDone("spaceairships@1"),
                    "the airship step waits for another start: " + waits);

            Files.delete(world.resolve("sublevels/r.0.0.0.slvls"));
            CompoundTag now = DeFixtures.ship(DeFixtures.emptyChunk(0, 0));
            now.putUUID("uuid", id);
            Path liveFile = DeSableGameTests.write(world.resolve("sublevels"), now);
            Ctx next = DeSpaceImportGameTests.ctx(world);
            for (Step.Phase phase : Step.Phase.values()) pipeline.run(phase, state, next, null);
            CompoundTag out = DeSableGameTests.read(liveFile, 0);
            CompoundTag section = out.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0)))
                    .getCompound("sections").getCompound(Integer.toString((lost.getY() >> 4) + 4));
            helper.assertTrue(state.stepDone("spaceairships@1") && new PackedSection(section.getCompound("block_states")).palette.stream()
                    .anyMatch(t -> ((CompoundTag) t).getString("Name").equals("ntmspace:ore_iron")),
                    "the next start merges the ship and is done: " + state.deferredOn("spaceairships@1"));
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeSpaceImportGameTests.delete(parent);
        }
    }

    /**
     * A DE: Space rocket alone in an airship's plot chunk comes back when Sable still has that ship in that plot; one in
     * a plot that holds no ship now does not, unless another entity of its chunk is still there (an item frame, which
     * gets its moon rock back too).
     */
    static void aboard(GameTestHelper helper) {
        Path parent = DeSpaceImportGameTests.tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-060000");
        try {
            UUID id = new UUID(8, 8);
            CompoundTag de = DeFixtures.ship(DeFixtures.emptyChunk(0, 0)), now = DeFixtures.ship(DeFixtures.emptyChunk(0, 0));
            de.putUUID("uuid", id);
            now.putUUID("uuid", id);
            DeSableGameTests.write(backup.resolve("sublevels"), de);
            DeSableGameTests.write(world.resolve("sublevels"), now);
            int yard = 10000 << 7;
            for (int plot : new int[]{0, 1}) {
                CompoundTag rocket = DeSpaceImportGameTests.entity("hbmspace:entity_rideable_rocket");
                rocket.putInt("state", 0);
                rocket.putIntArray("UUID", new int[]{30 + plot, 30, 30, 30});
                CompoundTag chunk = DeSpaceImportGameTests.entityChunk(rocket);
                int x = yard + (plot << 7);
                chunk.putInt("xPos", x);
                chunk.putInt("zPos", yard);
                chunk.putIntArray("Position", new int[]{x, yard});
                DeSpaceImportGameTests.writeChunk(backup.resolve("entities/r." + (x >> 5) + "." + (yard >> 5) + ".mca"), chunk);
            }
            int framed = yard + (2 << 7);
            CompoundTag frame = DeSpaceImportGameTests.entity("minecraft:item_frame");
            frame.putIntArray("UUID", new int[]{40, 40, 40, 40});
            CompoundTag full = frame.copy(), rider = DeSpaceImportGameTests.entity("hbmspace:entity_rideable_rocket");
            full.put("Item", DeFixtures.stack("hbmspace:moon_rock", 1));
            rider.putInt("state", 0);
            rider.putIntArray("UUID", new int[]{41, 41, 41, 41});
            for (boolean then : new boolean[]{true, false}) {
                CompoundTag chunk = DeSpaceImportGameTests.entityChunk(then ? full : frame);
                if (then) chunk.getList("Entities", Tag.TAG_COMPOUND).add(rider);
                chunk.putInt("xPos", framed);
                chunk.putInt("zPos", yard);
                chunk.putIntArray("Position", new int[]{framed, yard});
                DeSpaceImportGameTests.writeChunk((then ? backup : world).resolve("entities/r." + (framed >> 5) + "." + (yard >> 5) + ".mca"), chunk);
            }
            ImportState state = late(world, backup);
            Pipeline pipeline = new Pipeline(Steps.all().stream().filter(s -> s.id().startsWith("space")).toList(), ModList.get()::isLoaded);
            Ctx ctx = DeSpaceImportGameTests.ctx(world);
            for (Step.Phase phase : Step.Phase.values()) pipeline.run(phase, state, ctx, null);
            CompoundTag back, beside;
            try (RegionFile region = RegionIo.open(world.resolve("entities/r." + (framed >> 5) + "." + (yard >> 5) + ".mca"))) {
                beside = RegionIo.read(region, new ChunkPos(framed, yard));
            }
            helper.assertTrue(beside != null && beside.toString().contains("ntmspace:moon_rock") && beside.toString().contains("[I;41,41,41,41]"),
                    "the frame gets its moon rock back, and the rocket beside it comes back: " + beside);
            try (RegionFile region = RegionIo.open(world.resolve("entities/r." + (yard >> 5) + "." + (yard >> 5) + ".mca"))) {
                back = RegionIo.read(region, new ChunkPos(yard, yard));
            }
            helper.assertTrue(back != null && back.toString().contains("[I;30,30,30,30]"), "the rocket aboard the ship Sable still has comes back: " + back);
            helper.assertTrue(!Files.exists(world.resolve("entities/r." + ((yard + 128) >> 5) + "." + (yard >> 5) + ".mca"))
                            && ctx.report.get("addon entities kept in the leftovers as their place is gone") == 1,
                    "the one in a plot without its ship does not, and is kept in the leftovers: " + ctx.report.summary());
            helper.assertTrue(!Files.exists(world.resolve("sublevels.de-plots")), "the copy Sable's files were read through is gone");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeSpaceImportGameTests.delete(parent);
        }
    }

    /** Airships Sable cannot write on three starts are left as they are on the fourth, so the import finishes. */
    static void givesUp(GameTestHelper helper) {
        Path parent = DeSpaceImportGameTests.tempWorld();
        Path world = parent.resolve("world"), backup = parent.resolve("world_DE_backup_20261030-070000");
        try {
            CompoundTag before = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(before, new BlockPos(1, 70, 1), DeFixtures.block("hbmspace:ore_iron_duna"));
            CompoundTag de = DeFixtures.ship(before);
            de.putUUID("uuid", new UUID(7, 8));
            DeSableGameTests.write(backup.resolve("sublevels"), de);
            Files.createDirectories(world.resolve("sublevels/r.0.0.0.slvls"));
            ImportState state = late(world, backup);
            Pipeline pipeline = new Pipeline(Steps.all().stream().filter(s -> s.id().startsWith("space")).toList(), ModList.get()::isLoaded);
            for (int start = 1; start <= 3; start++) {
                Ctx ctx = DeSpaceImportGameTests.ctx(world);
                for (Step.Phase phase : Step.Phase.values()) pipeline.run(phase, state, ctx, null);
                helper.assertTrue(state.deferredOn("spaceairships@1") != null, "start " + start + " waits for the next");
            }
            Ctx last = DeSpaceImportGameTests.ctx(world);
            for (Step.Phase phase : Step.Phase.values()) pipeline.run(phase, state, last, null);
            last.report.append(world, "test");
            String report = Files.readString(world.resolve(Report.FILE));
            helper.assertTrue(state.stepDone("spaceairships@1") && report.contains("could not be done in three starts"),
                    "the fourth start leaves them and finishes: " + state.deferredOn("spaceairships@1"));
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeSpaceImportGameTests.delete(parent);
        }
    }

    /** A world imported by 1.0.3 without NTM: Space, with one DE: Space body and its backup. */
    private static ImportState late(Path world, Path backup) throws IOException {
        Files.createDirectories(backup.resolve("dimensions/hbmspace/mun"));
        Files.createDirectories(world.resolve("dimensions/hbmspace/mun/data"));
        Files.writeString(world.resolve("dimensions/hbmspace/mun/data/raids.dat"), "de: space");
        Files.writeString(world.resolve(BackupStep.RECORD), backup.getFileName().toString());
        ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "1.0.3");
        for (String step : List.of("backup@1", "regions@1", "players@1", "saveddata@1")) state.markDone(step);
        state.status("done");
        state.save();
        return state;
    }
}
