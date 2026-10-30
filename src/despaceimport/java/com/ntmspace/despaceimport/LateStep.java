package com.ntmspace.despaceimport;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.source.de7.Airships;
import com.hbm.deimport.source.de7.BackupMerge;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.source.de7.Players;
import com.hbm.deimport.steps.PlayersStep;
import com.hbm.deimport.steps.RegionsStep;
import com.hbm.deimport.steps.SubLevelsStep;
import com.hbm.delegacy.AtomicNbt;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.Entities;
import com.hbm.delegacy.de7.Walker;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * A world imported without NTM: Space's converter, by an importer that knew none or with the addon left for later.
 * After DE's terrain and player steps, the bodies {@link DimensionsStep} moved convert, where that import left them or
 * as its backup has them, and the players and blocks outside them follow the renamed dimensions.
 */
final class LateStep implements Step {
    private static final String ATTACHMENTS = "neoforge:attachments";
    static final Predicate<String> ADDON = id -> DeAddons.bySource(id) != null;

    static final String ID = "spacelate@1";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean applies(Ctx ctx) throws IOException {
        ImportState state = ImportState.read(ctx.world);
        return state != null && withoutAddon(state) && ctx.journal.done("spacedimensions@1", ctx.world, DimensionsStep.target(ctx.world));
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        ImportState state = ImportState.read(ctx.world);
        if (state != null && state.get("addons") == null) staleAirships(ctx);
        players(ctx, server);
        links(ctx);
        Map<String, Path> late = late(ctx);
        if (!late.isEmpty()) convertLate(ctx, late);
        unconverted(ctx);
        return true;
    }

    /** The import began without this addon's converter: by an importer that knew none, or left for later. */
    static boolean withoutAddon(ImportState state) {
        String addons = state.get("addons");
        return addons == null || !List.of(addons.split(",")).contains(SpaceAddon.SOURCE);
    }

    /**
     * 1.0.3 converted airships on the bodies under DE: Space's names, where a missing dimension gave them a floor of -64:
     * its waiting records place cells and cables 64 blocks too low. The ships convert again from the backup, or stay
     * unconverted; either way those records go to the leftovers.
     */
    private static void staleAirships(Ctx ctx) throws IOException {
        for (Path body : DimensionsStep.moved(ctx)) {
            String dim = SpaceAddon.SOURCE + ":" + body.getFileName();
            int stale = ctx.dropEarlier((kind, t) -> t.getString("dim").equals(dim)
                    && (kind.equals("plotcells") || kind.equals("conductors") && t.getBoolean("plot")));
            if (stale > 0) ctx.report.line(stale + " airship record(s) 1.0.3 left for " + dim + " were moved to the leftovers");
        }
    }

    /** Moved bodies {@link DimensionsStep} found in DE's shape or brought back from the backup, not converted yet. */
    private Map<String, Path> late(Ctx ctx) throws IOException {
        Map<String, Path> out = new LinkedHashMap<>();
        for (Path live : DimensionsStep.moved(ctx)) {
            if (ctx.journal.done(id(), ctx.world, live)) continue;
            if (moving(ctx, live, DimensionsStep.IN_PLACE) || moving(ctx, live, DimensionsStep.RESTORED)) out.put(SpaceAddon.TARGET + ":" + live.getFileName(), live);
        }
        return out;
    }

    private static boolean moving(Ctx ctx, Path body, String mark) {
        return ctx.journal.done("spacedimensions@1", ctx.world, body.resolve(mark));
    }

    /**
     * Terrain and airships, each once. DE's terrain step may have converted a body already, in a start that resumed an
     * import; its stamps and journal keep this from doing it twice.
     */
    private void convertLate(Ctx ctx, Map<String, Path> late) throws IOException {
        for (Map.Entry<String, Path> body : late.entrySet()) {
            Path live = body.getValue();
            if (moving(ctx, live, DimensionsStep.RESTORED)) {
                ctx.report.count("dimensions converted from the backup");
            } else {
                ctx.report.count("dimensions converted where they were left");
            }
            new RegionsStep().convert(ctx, Map.of(body.getKey(), live));
            airships(ctx, body.getKey(), live);
            ctx.journal.mark(id(), ctx.world, live);
        }
    }

    /** A body's DE: Space airships: converted now, or while Sable is missing kept where it cannot load them. */
    static void airships(Ctx ctx, String dim, Path live) throws IOException {
        Path folder = live.resolve("sublevels"), pending = live.resolve(SubLevelsStep.PENDING);
        if (!ModList.get().isLoaded("sable")) {
            if (Files.isDirectory(folder) && !Files.exists(pending)) Files.move(folder, pending);
            if (Files.isDirectory(pending)) ctx.report.line("airships on " + dim + " wait in " + ctx.world.relativize(pending) + " until Sable is installed");
            return;
        }
        if (Files.isDirectory(pending) && !Files.exists(folder)) Files.move(pending, folder);
        if (Files.isDirectory(folder)) SubLevelsStep.convert(ctx, dim, folder);
        if (Files.isDirectory(pending)) {
            SubLevelsStep.convert(ctx, dim, pending);
            ctx.report.line("airships in " + ctx.world.relativize(pending) + " are converted but stay aside: " + ctx.world.relativize(folder)
                    + " was created since; move one of them away to bring them back");
        }
    }

    /** Moved bodies with terrain that nothing could convert: no backup has them in DE's shape. */
    private void unconverted(Ctx ctx) throws IOException {
        int count = 0;
        for (Path live : DimensionsStep.moved(ctx)) {
            if (ctx.journal.done(id(), ctx.world, live) || !DimensionsStep.hasTerrain(live)) continue;
            ctx.report.lineNow(ctx.world, SpaceAddon.TARGET + ":" + live.getFileName() + " was imported before NTM: Space was installed and no "
                    + "import backup has it: its DE: Space ids stay unconverted");
            ctx.journal.mark(id(), ctx.world, live);
            count++;
        }
        if (count > 0) ctx.report.count("dimensions not converted", count);
    }

    /**
     * Players saved before NTM: Space was installed. One who has not played since still carries the first import's
     * mark and is converted again from the backup; anyone else keeps what Continuum saved and follows the renamed
     * dimensions, with what vanilla dropped of DE: Space's back from the backup.
     */
    private void players(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        Path backup = DimensionsStep.backup(ctx.world);
        Walker walker = new Walker(ctx);
        Path folder = ctx.world.resolve("playerdata");
        if (Files.isDirectory(folder)) {
            List<Path> files;
            try (Stream<Path> list = Files.list(folder)) {
                files = list.filter(p -> p.getFileName().toString().endsWith(".dat") || p.getFileName().toString().endsWith(".dat_old")).toList();
            }
            for (Path file : files) {
                if (ctx.journal.done(id(), ctx.world, file)) continue;
                String name = file.getFileName().toString(), where = "player " + name;
                boolean old = name.endsWith(".dat_old");
                CompoundTag player = read(file), original = backup == null ? null : read(backup.resolve("playerdata").resolve(name));
                boolean untouched = player != null && player.getBoolean(PlayersStep.CONVERTED);
                // vanilla's save shifts the first import's file to .dat_old: it is that import's only while the .dat is too
                if (untouched && old) {
                    CompoundTag current = read(file.resolveSibling(name.substring(0, name.length() - "_old".length())));
                    untouched = current != null && current.getBoolean(PlayersStep.CONVERTED);
                }
                if (untouched && original != null && !original.getBoolean(PlayersStep.CONVERTED)) {
                    Unit unit = ctx.begin("player again " + name);
                    Players.convert(original, walker, ctx, where);
                    original.putBoolean(PlayersStep.CONVERTED, true);
                    ctx.commit(unit);
                    AtomicNbt.write(original, file);
                    ctx.report.count("players converted again from the backup");
                } else if (player != null && player(ctx, player, old ? null : original, walker, where)) {
                    AtomicNbt.write(player, file);
                }
                ctx.journal.mark(id(), ctx.world, file);
            }
        }
        Path level = ctx.world.resolve("level.dat");
        CompoundTag host = server == null ? null : server.getWorldData().getLoadedPlayerTag();
        if (host == null || ctx.journal.done(id(), ctx.world, level)) return;
        CompoundTag original = backup == null ? null : read(backup.resolve("level.dat"));
        CompoundTag originalHost = original == null ? null : original.getCompound("Data").getCompound("Player");
        // the server loads the owner from this tag, so it changes in place
        if (host.getBoolean(PlayersStep.CONVERTED) && originalHost != null && !originalHost.isEmpty() && !originalHost.getBoolean(PlayersStep.CONVERTED)) {
            for (String key : List.copyOf(host.getAllKeys())) host.remove(key);
            host.merge(originalHost);
            PlayersStep.host(ctx, walker, host);
            ctx.report.count("players converted again from the backup");
        } else if (player(ctx, host, originalHost, walker, "level.dat player")) {
            CompoundTag root = NbtIo.readCompressed(level, NbtAccounter.unlimitedHeap());
            root.getCompound("Data").put("Player", host.copy());
            AtomicNbt.write(root, level);
        }
        ctx.journal.mark(id(), ctx.world, level);
    }

    /**
     * Only the dimensions, links to them and the addon's own data: the rest is Continuum's own by now. The addon's
     * attachments and stacks vanilla dropped on that player's last load come back from {@code original}, the backup's.
     */
    private static boolean player(Ctx ctx, CompoundTag player, @Nullable CompoundTag original, Walker walker, String where) throws IOException {
        CompoundTag before = player.copy();
        Unit unit = ctx.begin("player dimensions " + where);
        if (original != null) {
            CompoundTag kept = original.getCompound(ATTACHMENTS), live = player.getCompound(ATTACHMENTS);
            for (String key : kept.getAllKeys()) {
                if (ADDON.test(key) && !live.contains(key) && !live.contains(DeAddons.rename(key))) live.put(key, kept.get(key).copy());
            }
            // DE: Space's activation; the first import kept it aside, and nothing but NTM: Space changes it
            CompoundTag props = live.getCompound("hbm:living_props"), saved = kept.getCompound("hbm:living_props");
            if (saved.contains("activation", Tag.TAG_ANY_NUMERIC) && props.getDouble("neutrons") == 0) {
                props.putDouble("neutrons", saved.getFloat("activation"));
                live.put("hbm:living_props", props);
            }
            if (!live.isEmpty()) player.put(ATTACHMENTS, live);
            stacks(ctx, walker, original, player, "Inventory", 36, where);
            stacks(ctx, walker, original, player, "EnderItems", 27, where);
        }
        Entities.dimensions(player);
        links(player);
        for (DeAddons.Addon addon : DeAddons.all()) addon.living(player, ctx, where);
        ctx.commit(unit);
        if (player.equals(before)) return false;
        ctx.report.count("players moved to NTM: Space's dimensions");
        return true;
    }

    /** The addon's stacks in the backup's list, which vanilla dropped as unknown: back in their slot, else the first free one. */
    private static void stacks(Ctx ctx, Walker walker, CompoundTag original, CompoundTag player, String key, int size, String where) {
        ListTag live = player.getList(key, Tag.TAG_COMPOUND);
        Set<Integer> taken = new HashSet<>();
        Set<String> held = new HashSet<>();
        for (Tag t : live) {
            taken.add((int) ((CompoundTag) t).getByte("Slot"));
            held.add(((CompoundTag) t).getString("id"));
        }
        for (Tag t : original.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag stack = ((CompoundTag) t).copy();
            if (!ADDON.test(stack.getString("id"))) continue;
            int slot = stack.getByte("Slot");
            for (int i = 0; taken.contains(slot) && i < size; i++) slot = i;
            // one already held, raw or converted (a start that stopped after writing this player), is kept aside instead
            if (taken.contains(slot) || held.contains(stack.getString("id"))) {
                ctx.parking.park("item", where + " " + key, stack.getString("id"), stack);
                continue;
            }
            if (!walker.stacks().convert(stack, where + " " + key)) continue;
            if (held.contains(stack.getString("id"))) {
                ctx.parking.park("item", where + " " + key, stack.getString("id"), stack);
                continue;
            }
            taken.add(slot);
            stack.putByte("Slot", (byte) slot);
            live.add(stack);
            ctx.report.count("addon stacks brought back from the backup");
        }
        if (!live.isEmpty()) player.put(key, live);
    }

    /**
     * Outside the bodies: links to them (a teleporter's target, a linker, a lodestone compass), the addon's blocks and
     * entities the first import left in DE's shape, and from the backup what the game dropped of them since: blocks,
     * block entities, stacks in containers and entities, and whole entities.
     */
    private void links(Ctx ctx) throws IOException {
        Path backup = DimensionsStep.backup(ctx.world), target = DimensionsStep.target(ctx.world), source = DimensionsStep.source(ctx.world);
        Map<Path, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, Path> dim : Dimensions.all(ctx.world).entrySet()) {
            if (dim.getValue().startsWith(target) || dim.getValue().startsWith(source)) continue;
            for (String sub : DimensionsStep.TERRAIN) for (Path file : RegionIo.regionFiles(dim.getValue().resolve(sub))) files.put(file, dim.getKey());
        }
        try {
            files.entrySet().parallelStream().forEach(e -> {
                Path file = e.getKey();
                String where = ctx.world.relativize(file).toString();
                boolean entities = file.getParent().getFileName().toString().equals("entities");
                try {
                    if (ctx.journal.done(id(), ctx.world, file)) return;
                    ChunkConverter converter = new ChunkConverter(ctx, e.getValue(), ADDON);
                    Map<ChunkPos, CompoundTag> lost = backup == null || entities ? Map.of() : lost(backup.resolve(ctx.world.relativize(file).toString()));
                    RegionsStep.rewrite(ctx, "dimension links " + where, where, file, chunk -> {
                        boolean changed = false;
                        if (entities) {
                            changed = addonEntities(ctx, chunk, where);
                        } else if (addonBlocks(chunk)) {
                            converter.convert(chunk);
                            changed = true;
                        }
                        CompoundTag before = lost.get(new ChunkPos(chunk.getInt("xPos"), chunk.getInt("zPos")));
                        if (before != null && chunk.getLong("InhabitedTime") >= before.getLong("InhabitedTime"))
                            changed |= restore(ctx, e.getValue(), chunk, before, where);
                        changed |= links(chunk.getList("block_entities", Tag.TAG_COMPOUND)) | links(chunk.getList("Entities", Tag.TAG_COMPOUND));
                        return changed ? chunk : null;
                    });
                    ctx.journal.mark(id(), ctx.world, file);
                } catch (IOException x) {
                    throw new UncheckedIOException(x);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        if (backup == null) {
            ctx.report.lineNow(ctx.world, "no import backup was found, so DE: Space content outside its own dimensions that the game "
                    + "dropped before NTM: Space was installed cannot come back");
            return;
        }
        dropped(ctx, target, source, backup, false);
        ctx.report.line("DE: Space content outside its own dimensions that the game dropped before NTM: Space was installed came back from "
                + backup.getFileName() + " wherever nothing was built since; anything else stays there");
    }

    private static boolean inPlotYard(Path region) {
        String[] name = region.getFileName().toString().split("[.]");
        return Airships.inPlotYard(new ChunkPos(Integer.parseInt(name[1]) << 5, Integer.parseInt(name[2]) << 5));
    }

    /** The backup's chunks of one region file that hold the addon's ids. */
    private static Map<ChunkPos, CompoundTag> lost(Path backed) throws IOException {
        if (!Files.isRegularFile(backed)) return Map.of();
        Map<ChunkPos, CompoundTag> out = new HashMap<>();
        RegionIo.readOnly(backed, (pos, chunk) -> {
            if (BackupMerge.holds(chunk, ADDON)) out.put(pos, chunk);
            return true;
        });
        return out;
    }

    /** What the game dropped of the addon's in a chunk it loaded since the first import: stacks in kept containers, then blocks. */
    private static boolean restore(Ctx ctx, String dim, CompoundTag chunk, CompoundTag before, String where) {
        int stacks = BackupMerge.containers(ctx, chunk, before, new Walker(ctx), ADDON, where);
        int blocks = BackupMerge.blocks(ctx, dim, chunk, before, ADDON);
        ctx.report.count("addon stacks brought back from the backup", stacks);
        ctx.report.count("addon blocks brought back from the backup", blocks);
        return stacks + blocks > 0;
    }

    /**
     * How long players spent in each terrain chunk beside an entity file. The game never lowers it and a regenerated
     * chunk starts at 0, so a live chunk at least as old as DE's grew from it.
     */
    private static Map<ChunkPos, Long> inhabited(Path entities) throws IOException {
        Path file = entities.getParent().resolveSibling("region").resolve(entities.getFileName().toString());
        Map<ChunkPos, Long> out = new HashMap<>();
        if (Files.isRegularFile(file)) RegionIo.readOnly(file, (pos, chunk) -> {
            out.put(pos, chunk.getLong("InhabitedTime"));
            return true;
        });
        return out;
    }

    /** DE: Space entities outside the bodies that the first import left in DE's shape. */
    private static boolean addonEntities(Ctx ctx, CompoundTag chunk, String where) {
        Walker walker = new Walker(ctx, ADDON);
        ListTag entities = chunk.getList("Entities", Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = entities.size() - 1; i >= 0; i--) {
            if (!ADDON.test(entities.getCompound(i).getString("id"))) continue;
            if (!walker.walk(entities.getCompound(i), where)) entities.remove(i);
            ctx.report.count("addon entities outside its dimensions converted");
            changed = true;
        }
        return changed;
    }

    /** A chunk the first import left with the addon's blocks or block entities in DE's shape. */
    private static boolean addonBlocks(CompoundTag chunk) {
        for (Tag be : chunk.getList("block_entities", Tag.TAG_COMPOUND))
            if (ADDON.test(((CompoundTag) be).getString("id"))) return true;
        for (Tag s : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            for (Tag p : ((CompoundTag) s).getCompound("block_states").getList("palette", Tag.TAG_COMPOUND))
                if (ADDON.test(((CompoundTag) p).getString("Name"))) return true;
        }
        return false;
    }

    /**
     * DE: Space entities outside the bodies that the first start dropped as unknown, as vanilla does without the
     * addon: each comes back from the backup unless an entity of its UUID is there. One that is there gets back the
     * addon's stacks it dropped (an item frame's item, a minecart's cargo). A chunk whose entities all went is no longer
     * in the file, so this goes by the backup's chunks. Once per file.
     */
    /**
     * What the game dropped of the addon's entities and their stacks, from the backup's entity files: those of Sable's
     * plot grid {@code aboard} only, which wait for Sable to tell its ships apart, else the others. False when Sable's
     * storage could not be read now, so that a later start does those files.
     */
    static boolean dropped(Ctx ctx, Path target, Path source, Path backup, boolean aboard) throws IOException {
        Walker walker = new Walker(ctx);
        boolean complete = true;
        for (Path dim : Dimensions.all(ctx.world).values()) {
            if (dim.startsWith(target) || dim.startsWith(source)) continue;
            Path folder = backup.resolve(ctx.world.relativize(dim).toString()).resolve("entities");
            List<Path> files = RegionIo.regionFiles(folder).stream().filter(f -> aboard == inPlotYard(f)).toList();
            if (files.isEmpty()) continue;
            Map<Long, UUID> shipsNow = null, shipsThen = null;
            if (aboard) {
                Path scratch = dim.resolve("sublevels.de-plots");
                try {
                    shipsNow = SubLevelsStep.plots(dim.resolve("sublevels"), scratch);
                    shipsThen = SubLevelsStep.plots(folder.resolveSibling("sublevels"), scratch);
                } catch (IOException e) {
                    ctx.report.line(ctx.world.relativize(dim) + " airships cannot be read now (" + e + "); the entities aboard wait for the next start");
                    complete = false;
                    continue;
                }
            }
            for (Path original : files) {
                Path file = dim.resolve("entities").resolve(original.getFileName().toString());
                Path mark = file.resolveSibling(file.getFileName() + ".restored");
                if (ctx.journal.done(ID, ctx.world, mark)) continue;
                String where = ctx.world.relativize(file).toString();
                Map<ChunkPos, CompoundTag> backed = new LinkedHashMap<>();
                RegionIo.readOnly(original, (pos, chunk) -> {
                    backed.put(pos, chunk);
                    return true;
                });
                if (backed.values().stream().noneMatch(chunk -> BackupMerge.holds(chunk, ADDON))) {
                    ctx.journal.mark(ID, ctx.world, mark);
                    continue;
                }
                Map<ChunkPos, Long> now = inhabited(file), then = inhabited(original);
                // an entity can have moved to another chunk of the file since
                Map<ChunkPos, CompoundTag> live = new HashMap<>();
                Map<String, CompoundTag> byUuid = new HashMap<>();
                Map<String, ChunkPos> chunkOf = new HashMap<>();
                if (Files.isRegularFile(file)) RegionIo.readOnly(file, (pos, chunk) -> {
                    live.put(pos, chunk);
                    for (Tag t : chunk.getList("Entities", Tag.TAG_COMPOUND)) {
                        String uuid = Arrays.toString(((CompoundTag) t).getIntArray("UUID"));
                        byUuid.put(uuid, (CompoundTag) t);
                        chunkOf.put(uuid, pos);
                    }
                    return true;
                });
                Unit unit = ctx.begin("entities from the backup " + where);
                Set<ChunkPos> changed = new LinkedHashSet<>();
                for (Map.Entry<ChunkPos, CompoundTag> chunk : backed.entrySet()) {
                    ListTag de = chunk.getValue().getList("Entities", Tag.TAG_COMPOUND);
                    // a chunk regenerated since starts its inhabited time again
                    boolean gone = now.getOrDefault(chunk.getKey(), -1L) < then.getOrDefault(chunk.getKey(), 0L);
                    if (Airships.inPlotYard(chunk.getKey())) {
                        // Sable's plots have no terrain: the same ship in the plot, or another entity of the chunk
                        // still standing, shows it is the ship the backup saw
                        UUID ship = shipsThen == null ? null : shipsThen.get(Airships.plotOf(chunk.getKey()));
                        gone = !(ship != null && ship.equals(shipsNow.get(Airships.plotOf(chunk.getKey()))))
                                && de.stream().noneMatch(t -> byUuid.containsKey(Arrays.toString(((CompoundTag) t).getIntArray("UUID"))));
                    }
                    ListTag back = new ListTag();
                    for (Tag t : de) {
                        CompoundTag entity = (CompoundTag) t;
                        String uuid = Arrays.toString(entity.getIntArray("UUID"));
                        CompoundTag there = byUuid.get(uuid);
                        if (there != null) {
                            int stacks = BackupMerge.holds(entity, ADDON) ? BackupMerge.stacks(there, entity, walker, ADDON, where) : 0;
                            if (stacks > 0) changed.add(chunkOf.get(uuid));
                            ctx.report.count("addon stacks brought back from the backup", stacks);
                            continue;
                        }
                        CompoundTag copy = entity.copy();
                        if (!ADDON.test(copy.getString("id"))) continue;
                        if (gone) {
                            // its chunk was made again, or its airship no longer holds its plot: kept as DE saved it
                            ctx.parking.park("entity", where + " " + chunk.getKey(), copy.getString("id"), copy);
                            ctx.report.count("addon entities kept in the leftovers as their place is gone");
                        } else if (walker.walk(copy, where)) {
                            back.add(copy);
                        }
                    }
                    if (back.isEmpty()) continue;
                    CompoundTag out = live.computeIfAbsent(chunk.getKey(), pos -> {
                        CompoundTag fresh = chunk.getValue().copy();
                        fresh.put("Entities", new ListTag());
                        return fresh;
                    });
                    ListTag entities = out.getList("Entities", Tag.TAG_COMPOUND);
                    entities.addAll(back);
                    out.put("Entities", entities);
                    out.putBoolean("hbm_de_converted", true);
                    changed.add(chunk.getKey());
                    ctx.report.count("addon entities brought back from the backup", back.size());
                }
                // what was parked is kept before any chunk changes; a crash in between brings them back again
                ctx.commit(unit);
                if (!changed.isEmpty()) {
                    Files.createDirectories(file.getParent());
                    try (RegionFile to = RegionIo.open(file)) {
                        for (ChunkPos pos : changed) {
                            try (DataOutputStream out = to.getChunkDataOutputStream(pos)) {
                                NbtIo.write(live.get(pos), out);
                            }
                        }
                    }
                }
                ctx.journal.mark(ID, ctx.world, mark);
            }
        }
        return complete;
    }

    /** Renames every {@code dimension} or {@code dim} string naming an addon's DE dimension, at any depth. */
    private static boolean links(Tag tag) {
        boolean changed = false;
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                if ((key.equals("dimension") || key.equals("dim")) && value instanceof StringTag text) {
                    String renamed = DeAddons.rename(text.getAsString());
                    if (renamed.equals(text.getAsString())) continue;
                    compound.putString(key, renamed);
                    changed = true;
                } else {
                    changed |= links(value);
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag t : list) changed |= links(t);
        }
        return changed;
    }

    /** A saved NBT file, or null when it is missing or cannot be read; the players step kept a copy of any it could not. */
    private static @Nullable CompoundTag read(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
