package com.ntmspace.despaceimport;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.steps.SavedDataStep;
import com.hbm.delegacy.AtomicNbt;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.Tables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * DE: Space's saved data under NTM: Space's names. The solar system file splits in two (body traits, stations), Duna's
 * dust storm is renamed, and the chunk loader list is parked (the transporter claims its chunk again when it ticks).
 * Runs after DE's saved data step, so the satellites it reshaped carry their orbits already; DE's files that step
 * could not reach in a world imported before NTM: Space convert here.
 */
final class SpaceDataStep implements Step {
    private static final String[] STATION_STATES = {"ORBIT", "LEAVING", "TRANSFER", "ARRIVING"};

    @Override
    public String id() {
        return "spacesaveddata@1";
    }

    @Override
    public boolean applies(Ctx ctx) throws IOException {
        for (Path dim : Dimensions.withData(ctx.world).values()) if (!files(dim).isEmpty()) return true;
        return !maps(ctx.world).isEmpty() || Files.isDirectory(ctx.world.resolve("dimensions").resolve(SpaceAddon.TARGET));
    }

    /** Each file is a unit, parked and committed before its NTM: Space files are written and the original removed. */
    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        Map<String, Path> folders = Dimensions.without(Dimensions.withData(ctx.world), ctx.deferredAddons);
        SavedDataStep.convert(ctx, folders);
        for (Path dim : folders.values()) satellites(ctx, dim);
        restoreSatellites(ctx, folders);
        for (Path dim : folders.values()) ownSatellites(ctx, dim);
        for (Map.Entry<String, Path> dim : folders.entrySet()) {
            Path data = dim.getValue().resolve("data");
            for (Path file : files(dim.getValue())) {
                String name = file.getFileName().toString();
                name = name.substring(0, name.length() - ".dat".length());
                String where = dim.getKey() + " " + name;
                CompoundTag root = read(ctx, file, where);
                if (root == null) continue;
                Unit unit = ctx.begin("saved data " + where);
                ctx.parking.park("saved_data", where, name, root.copy());
                CompoundTag outer = root.getCompound("data");
                CompoundTag content = outer.contains("data", Tag.TAG_COMPOUND) ? outer.getCompound("data") : outer;
                switch (name) {
                    case "hbmspace_solar_system" -> {
                        CompoundTag bodies = new CompoundTag();
                        for (String key : content.getAllKeys()) {
                            if (key.startsWith("b_") && content.get(key) instanceof CompoundTag traits) bodies.put(key.substring(2), traits(traits));
                        }
                        ListTag stations = content.getList("stations", Tag.TAG_COMPOUND);
                        Set<String> squares = new HashSet<>();
                        for (Tag t : stations) {
                            CompoundTag station = (CompoundTag) t;
                            int state = station.getInt("state");
                            station.putString("state", STATION_STATES[state >= 0 && state < STATION_STATES.length ? state : 0]);
                            squares.add(station.getInt("x") + "," + station.getInt("z"));
                        }
                        // stations NTM: Space recorded in an earlier start, or a merge a stopped run wrote, on grid squares DE:
                        // Space's list leaves free; none when NTM: Space's orbit was set aside, as their blocks went with it
                        Path own = data.resolve("ntmspace_stations.dat");
                        boolean orbitKept = Files.exists(ctx.world.resolve(DimensionsStep.KEPT).resolve(SpaceAddon.TARGET).resolve("orbit"));
                        CompoundTag ownStations = Files.isRegularFile(own) && !orbitKept ? read(ctx, own, where) : null;
                        for (Tag t : ownStations == null ? new ListTag() : ownStations.getCompound("data").getList("stations", Tag.TAG_COMPOUND)) {
                            CompoundTag station = (CompoundTag) t;
                            if (squares.add(station.getInt("x") + "," + station.getInt("z"))) stations.add(station);
                        }
                        CompoundTag stationData = new CompoundTag();
                        stationData.put("stations", stations);
                        CompoundTag traitData = new CompoundTag();
                        traitData.put("bodies", bodies);
                        ctx.commit(unit);
                        write(root, traitData, data.resolve("ntmspace_solar_system.dat"), ctx, where);
                        write(root, stationData, data.resolve("ntmspace_stations.dat"), ctx, where);
                        ctx.report.count("space bodies converted", bodies.size());
                        ctx.report.count("space stations converted", stations.size());
                    }
                    case "hbmspace_duna_dust_storm" -> {
                        ctx.commit(unit);
                        write(root, content, data.resolve("ntmspace_dust_storm.dat"), ctx, where);
                    }
                    default -> ctx.commit(unit);
                }
                Files.delete(file);
                ctx.report.line("saved data converted: " + where);
            }
        }
        for (Path file : maps(ctx.world)) {
            CompoundTag root = read(ctx, file, "map " + file.getFileName());
            if (root == null) continue;
            CompoundTag data = root.getCompound("data");
            data.putString("dimension", DeAddons.rename(data.getString("dimension")));
            AtomicNbt.write(root, file);
            ctx.report.count("maps moved to NTM: Space's dimensions");
        }
        return true;
    }

    /**
     * DE: Space's satellites in a file DE's step already flattened without NTM: Space: their bare type names, which
     * Continuum cannot read, make the level's whole list empty.
     */
    private static void satellites(Ctx ctx, Path dim) throws IOException {
        Path file = dim.resolve("data/hbm_satellites.dat");
        if (!Files.isRegularFile(file)) return;
        String where = ctx.world.relativize(file).toString();
        CompoundTag root = read(ctx, file, where);
        if (root == null) return;
        CompoundTag before = root.copy();
        for (Tag t : root.getCompound("data").getList("sats", Tag.TAG_COMPOUND)) {
            CompoundTag sat = ((CompoundTag) t).getCompound("sat");
            String type = SpaceAddon.SATELLITES.get(sat.getString("type"));
            if (type != null) sat.putString("type", type);
        }
        if (root.equals(before)) return;
        Unit unit = ctx.begin("satellite types " + where);
        ctx.parking.park("saved_data", where, "hbm_satellites", before);
        ctx.commit(unit);
        AtomicNbt.write(root, file);
        ctx.report.line("satellites renamed to NTM: Space's types in " + where + "; their orbits stay in the leftovers");
    }

    /**
     * Satellites a first import without NTM: Space could not keep, from its leftovers: one parked for its DE: Space
     * type, or in a world 1.0.3 imported a level's whole list holding such a type, which Continuum could not read and
     * saved over (1.0.3 kept it flattened). One removed in Continuum since stays removed, a frequency in use stays.
     */
    private void restoreSatellites(Ctx ctx, Map<String, Path> folders) throws IOException {
        ImportState state = ImportState.read(ctx.world);
        String addons = state == null ? null : state.get("addons");
        if (state == null || addons != null && List.of(addons.split(",")).contains(SpaceAddon.SOURCE)) return;
        Map<Path, ListTag> lost = new LinkedHashMap<>();
        for (Tag t : List.copyOf(ctx.parking.records())) {
            CompoundTag record = (CompoundTag) t;
            String where = record.getString("where"), kind = record.getString("kind");
            int at = where.indexOf(" hbm_satellites");
            if (at < 0 || !(kind.equals("saved_data") && where.endsWith(" hbm_satellites") || kind.equals("satellite"))) continue;
            Path dim = folders.get(DeAddons.rename(where.substring(0, at)));
            if (dim == null) continue;
            if (kind.equals("satellite")) {
                if (spaceType(record.getCompound("nbt"))) lost.computeIfAbsent(dim.resolve("data/hbm_satellites.dat"), f -> new ListTag()).add(record.getCompound("nbt").copy());
                continue;
            }
            CompoundTag outer = record.getCompound("nbt").getCompound("data");
            ListTag list = (outer.contains("data", Tag.TAG_COMPOUND) ? outer.getCompound("data") : outer).getList("sats", Tag.TAG_COMPOUND);
            if (addons != null || list.stream().noneMatch(s -> spaceType((CompoundTag) s))) continue;
            ListTag entries = lost.computeIfAbsent(dim.resolve("data/hbm_satellites.dat"), f -> new ListTag());
            for (Tag s : list) entries.add(s.copy());
        }
        for (Map.Entry<Path, ListTag> each : lost.entrySet()) {
            Path file = each.getKey();
            String where = ctx.world.relativize(file).toString();
            if (!Files.isRegularFile(file) || ctx.journal.done(id(), ctx.world, file)) continue;
            CompoundTag root = read(ctx, file, where);
            if (root == null) continue;
            ListTag sats = root.getCompound("data").getList("sats", Tag.TAG_COMPOUND);
            Set<Integer> taken = new HashSet<>();
            for (Tag s : sats) taken.add(((CompoundTag) s).getInt("freq"));
            ListTag nested = new ListTag();
            int back = 0;
            for (Tag t : each.getValue()) {
                CompoundTag entry = (CompoundTag) t;
                if (!taken.add(entry.getInt("freq"))) continue;
                if (entry.getCompound("sat").contains("sat", Tag.TAG_COMPOUND)) {
                    nested.add(entry);
                    continue;
                }
                CompoundTag sat = entry.getCompound("sat");
                sat.putString("type", SpaceAddon.SATELLITES.getOrDefault(sat.getString("type"), sat.getString("type")));
                if (!ctx.target.hasSatelliteType(sat.getString("type"))) continue;
                sats.add(entry);
                back++;
            }
            Unit unit = ctx.begin("satellites again " + where);
            CompoundTag converted = new CompoundTag();
            converted.put("sats", nested);
            SavedDataStep.satellites(ctx, converted, where);
            ListTag kept = converted.getList("sats", Tag.TAG_COMPOUND);
            sats.addAll(kept);
            back += kept.size();
            root.getCompound("data").put("sats", sats);
            ctx.commit(unit);
            if (back > 0) AtomicNbt.write(root, file);
            ctx.journal.mark(id(), ctx.world, file);
            if (back > 0) ctx.report.count("satellites brought back", back);
        }
    }

    /** A saved satellite entry, nested as DE saves it or flattened, of a DE: Space type Continuum alone cannot read. */
    private static boolean spaceType(CompoundTag entry) {
        CompoundTag sat = entry.getCompound("sat");
        String type = (sat.contains("sat", Tag.TAG_COMPOUND) ? sat.getCompound("sat") : sat).getString("type");
        return SpaceAddon.SATELLITES.containsKey(type) || DeAddons.bySource(type) != null;
    }

    /** NTM: Space's own satellites of a body DE: Space's replaced, added by frequency; one whose frequency is taken is parked. */
    private static void ownSatellites(Ctx ctx, Path dim) throws IOException {
        Path own = dim.resolve(DimensionsStep.OWN_SATELLITES), file = dim.resolve("data/hbm_satellites.dat");
        if (!Files.isRegularFile(own)) return;
        if (!Files.isRegularFile(file)) {
            Files.move(own, file);
            return;
        }
        String where = ctx.world.relativize(own).toString();
        CompoundTag root = read(ctx, file, where), mine = read(ctx, own, where);
        if (root == null || mine == null) return;
        ListTag sats = root.getCompound("data").getList("sats", Tag.TAG_COMPOUND);
        Set<Integer> taken = new HashSet<>();
        for (Tag s : sats) taken.add(((CompoundTag) s).getInt("freq"));
        Unit unit = ctx.begin("satellites " + where);
        for (Tag t : mine.getCompound("data").getList("sats", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            // a run that stopped before the side file went added this one already
            if (sats.contains(entry)) continue;
            if (taken.add(entry.getInt("freq"))) sats.add(entry);
            else ctx.parking.park("satellite", where + " freq " + entry.getInt("freq"), entry.getCompound("sat").getString("type"), entry);
        }
        root.getCompound("data").put("sats", sats);
        ctx.commit(unit);
        AtomicNbt.write(root, file);
        Files.delete(own);
        ctx.report.count("satellites merged with NTM: Space's own");
    }

    /** Vanilla map data drawn in a DE: Space dimension, which names it. */
    private static List<Path> maps(Path world) throws IOException {
        Path data = world.resolve("data");
        if (!Files.isDirectory(data)) return List.of();
        List<Path> out = new ArrayList<>();
        try (Stream<Path> list = Files.list(data)) {
            for (Path file : list.filter(p -> p.getFileName().toString().matches("map_\\d+\\.dat")).toList()) {
                try {
                    String dim = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompound("data").getString("dimension");
                    if (dim.startsWith(SpaceAddon.SOURCE + ":")) out.add(file);
                } catch (IOException | RuntimeException e) {
                    // a map vanilla cannot read either is left to it
                }
            }
        }
        return out;
    }

    /** DE: Space's per-body traits; only the war, atmosphere and water traits save differently. */
    private static CompoundTag traits(CompoundTag traits) {
        if (traits.get("war") instanceof CompoundTag war && war.get("projectiles") instanceof CompoundTag numbered) {
            ListTag list = new ListTag();
            for (int i = 0; numbered.get("projectile" + i) instanceof CompoundTag projectile; i++) {
                projectile.putString("target", DeAddons.rename(projectile.getString("target")));
                list.add(projectile);
            }
            war.put("projectiles", list);
        }
        if (traits.get("atmosphere") instanceof CompoundTag atmosphere) {
            for (Tag t : atmosphere.getList("fluids", Tag.TAG_COMPOUND)) {
                CompoundTag fluid = (CompoundTag) t;
                fluid.putString("type", Tables.get().fluid(fluid.getString("type")));
            }
        }
        if (traits.get("water") instanceof CompoundTag water && water.contains("fluid", Tag.TAG_STRING))
            water.putString("fluid", Tables.get().fluid(water.getString("fluid")));
        return traits;
    }

    /**
     * NTM: Space's file beside DE's; one NTM: Space saved in an earlier start is set aside under {@code hbm_de_kept}.
     * Each written file is journaled, so a crash before DE's file goes writes it again and keeps nothing of its own.
     */
    private void write(CompoundTag deRoot, CompoundTag content, Path file, Ctx ctx, String where) throws IOException {
        if (Files.exists(file) && !ctx.journal.done(id(), ctx.world, file)) {
            Path aside = ctx.world.resolve(DimensionsStep.KEPT).resolve(ctx.world.relativize(file).toString());
            for (int i = 2; Files.exists(aside); i++) aside = aside.resolveSibling(file.getFileName() + "_" + i);
            Files.createDirectories(aside.getParent());
            Files.move(file, aside);
            ctx.report.lineNow(ctx.world, where + ": NTM: Space's own " + file.getFileName() + " moved to " + ctx.world.relativize(aside));
        }
        CompoundTag root = new CompoundTag();
        if (deRoot.contains("DataVersion")) root.put("DataVersion", deRoot.get("DataVersion"));
        root.put("data", content);
        AtomicNbt.write(root, file);
        ctx.journal.mark(id(), ctx.world, file);
    }

    /** Every {@code hbmspace_*} saved data file of a dimension. */
    private static List<Path> files(Path dim) throws IOException {
        Path data = dim.resolve("data");
        if (!Files.isDirectory(data)) return List.of();
        try (Stream<Path> list = Files.list(data)) {
            return list.filter(p -> p.getFileName().toString().startsWith(SpaceAddon.SOURCE + "_") && p.getFileName().toString().endsWith(".dat"))
                    .sorted().toList();
        }
    }

    private static @Nullable CompoundTag read(Ctx ctx, Path file, String where) throws IOException {
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            ctx.parking.park("saved_data", where, "unreadable: " + e, new ByteArrayTag(Files.readAllBytes(file)));
            ctx.report.line(where + " cannot be read and was left as it is");
            return null;
        }
    }
}
