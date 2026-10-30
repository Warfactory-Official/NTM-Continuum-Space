package com.ntmspace.despaceimport;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Report;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.steps.BackupStep;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * DE: Space's bodies are {@code hbmspace:<body>}, NTM: Space's the same bodies as {@code ntmspace:<body>}: each folder
 * moves under {@code dimensions/ntmspace} before any step records a dimension. A folder NTM: Space already saved there
 * is set aside under {@code hbm_de_kept} when DE: Space's has terrain, the body players built on; a DE: Space body
 * that only saved data moves its files into NTM: Space's. In a world imported without the addon, a body that import
 * converted comes back from its backup here, before DE's terrain step, and {@link LateStep} goes on.
 */
final class DimensionsStep implements Step {
    static final String KEPT = "hbm_de_kept";
    /** NTM: Space's own satellites of a body DE: Space's replaces, merged by {@link SpaceDataStep}. */
    static final String OWN_SATELLITES = "data/hbm_satellites_ntmspace.dat";
    /** Journaled under each body moved from DE: Space's folder, and on the target folder once any was moved or merged. */
    static final String MOVED = "moved";
    /** How a body moved into a world imported without the addon converts, journaled before it moves. */
    static final String IN_PLACE = "in_place", FROM_BACKUP = "from_backup", RESTORED = "restored";
    static final List<String> TERRAIN = List.of("region", "entities");

    @Override
    public String id() {
        return "spacedimensions@1";
    }

    @Override
    public boolean applies(Ctx ctx) throws IOException {
        return Files.isDirectory(source(ctx.world)) || moved(ctx).stream().anyMatch(body -> unrestored(ctx, body));
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        Path target = target(ctx.world);
        List<Path> bodies = List.of();
        if (Files.isDirectory(source(ctx.world))) {
            try (Stream<Path> list = Files.list(source(ctx.world))) {
                bodies = list.filter(Files::isDirectory).toList();
            }
        }
        ImportState state = ImportState.read(ctx.world);
        boolean late = state != null && LateStep.withoutAddon(state);
        Path backup = late ? backup(ctx.world) : null;
        // 1.0.3 knew no addons and may have converted any store of a body, airships first; an import that left the
        // addon for later touched none
        boolean earlier = late && state.get("addons") == null;
        if (!bodies.isEmpty()) ctx.journal.mark(id(), ctx.world, target);
        if (late) checkSpace(ctx, bodies, backup, earlier);
        for (Path body : bodies) {
            String name = body.getFileName().toString();
            Path to = target.resolve(name);
            if (Files.exists(to) && !hasTerrain(body)) {
                merge(ctx, body, to, name);
                continue;
            }
            if (late && fromBackup(body, backup, earlier)) ctx.journal.mark(id(), ctx.world, to.resolve(FROM_BACKUP));
            else if (late && stamped(body)) ctx.journal.mark(id(), ctx.world, to.resolve(IN_PLACE));
            if (Files.exists(to)) {
                keepSatellites(to, body);
                Path aside = setAside(ctx.world, to, SpaceAddon.TARGET + "/" + name);
                ctx.report.lineNow(ctx.world, "NTM: Space's own " + SpaceAddon.TARGET + ":" + name + " moved to " + ctx.world.relativize(aside));
                ctx.report.count("dimensions set aside");
            }
            Files.createDirectories(target);
            Files.move(body, to);
            ctx.journal.mark(id(), ctx.world, to.resolve(MOVED));
            ctx.report.count("dimensions moved");
        }
        if (Files.isDirectory(source(ctx.world))) {
            try (Stream<Path> left = Files.list(source(ctx.world))) {
                if (left.findAny().isEmpty()) Files.delete(source(ctx.world));
            }
        }
        for (Path body : moved(ctx)) {
            if (!unrestored(ctx, body)) continue;
            Path from = backed(backup(ctx.world), body);
            for (String sub : TERRAIN) restore(ctx, from, body, sub);
            // that import converted the airships under DE: Space's name and without its tables
            if (state != null && (state.stepDone("sublevels@1") || state.ranEarlier("sublevels@1"))) restore(ctx, from, body, "sublevels");
            ctx.journal.mark(id(), ctx.world, body.resolve(RESTORED));
            ctx.report.count("dimensions brought back from the backup");
        }
        return true;
    }

    /** Bodies an import without the addon converted come back from its backup: refused before anything moves. */
    private static void checkSpace(Ctx ctx, List<Path> bodies, @Nullable Path backup, boolean earlier) throws IOException {
        long needed = 0;
        for (Path body : bodies) if (fromBackup(body, backup, earlier)) needed += size(backed(backup, body));
        if (needed > 0 && Files.getFileStore(ctx.world).getUsableSpace() < needed + (64L << 20))
            throw new IOException("not enough disk space to bring NTM: Space's dimensions back from " + backup.getFileName() + " ("
                    + (needed >> 20) + " MB)");
    }

    private static boolean fromBackup(Path body, @Nullable Path backup, boolean earlier) throws IOException {
        return backup != null && hasTerrain(body) && (earlier || !stamped(body)) && stamped(backed(backup, body));
    }

    /** The bodies moved from DE: Space's folder, in this start or an earlier one; the choice is journaled before the move. */
    static List<Path> moved(Ctx ctx) throws IOException {
        Path target = target(ctx.world);
        if (!Files.isDirectory(target)) return List.of();
        try (Stream<Path> list = Files.list(target)) {
            return list.filter(Files::isDirectory).filter(p -> Stream.of(MOVED, IN_PLACE, FROM_BACKUP)
                    .anyMatch(mark -> ctx.journal.done("spacedimensions@1", ctx.world, p.resolve(mark)))).sorted().toList();
        }
    }

    private static boolean unrestored(Ctx ctx, Path body) {
        return ctx.journal.done("spacedimensions@1", ctx.world, body.resolve(FROM_BACKUP))
                && !ctx.journal.done("spacedimensions@1", ctx.world, body.resolve(RESTORED));
    }

    /**
     * Brings one folder of a body back from the backup, once. What the first import left is set aside first; a copy
     * a crash interrupted is only the backup's, and goes.
     */
    private void restore(Ctx ctx, Path from, Path live, String sub) throws IOException {
        Path to = live.resolve(sub), cleared = live.resolve(sub + ".cleared");
        if (!Files.isDirectory(from.resolve(sub)) || ctx.journal.done(id(), ctx.world, to)) return;
        if (ctx.journal.done(id(), ctx.world, cleared)) {
            delete(to);
        } else {
            if (Files.exists(to)) {
                Path aside = setAside(ctx.world, to, SpaceAddon.SOURCE + "/" + live.getFileName() + "/" + sub);
                ctx.report.lineNow(ctx.world, sub + " of " + SpaceAddon.TARGET + ":" + live.getFileName() + " as the first import left it moved to "
                        + ctx.world.relativize(aside));
            }
            ctx.journal.mark(id(), ctx.world, cleared);
        }
        copy(from.resolve(sub), to);
        ctx.journal.mark(id(), ctx.world, to);
    }

    /** Copies a backup folder, each file forced to disk before the journal says the folder is back. */
    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path p : files.toList()) {
                Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                    continue;
                }
                // from a stream: copying the file would carry a read-only backup's attribute over to the copy
                try (InputStream in = Files.newInputStream(p)) {
                    Files.copy(in, dest);
                }
                try (FileChannel channel = FileChannel.open(dest, StandardOpenOption.WRITE)) {
                    channel.force(true);
                }
            }
        }
    }

    private static void delete(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    /** NTM: Space's satellites of a body that is set aside or merged, kept for {@link SpaceDataStep} to merge. */
    private static void keepSatellites(Path own, Path into) throws IOException {
        Path file = own.resolve("data/hbm_satellites.dat");
        if (!Files.isRegularFile(file) || Files.exists(own.resolve(OWN_SATELLITES)) || Files.exists(into.resolve(OWN_SATELLITES))) return;
        Files.createDirectories(into.resolve("data"));
        Files.move(file, into.resolve(OWN_SATELLITES));
    }

    /** Where both have a file, NTM: Space's stays in use and DE: Space's is set aside; satellites merge. */
    private void merge(Ctx ctx, Path body, Path to, String name) throws IOException {
        // once DE: Space's file has moved in, the one there is DE: Space's
        if (Files.isRegularFile(body.resolve("data/hbm_satellites.dat"))) keepSatellites(to, to);
        try (Stream<Path> files = Files.walk(body)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String rel = body.relativize(file).toString().replace('\\', '/');
                Path dest = to.resolve(rel);
                if (!Files.exists(dest)) {
                    Files.createDirectories(dest.getParent());
                    Files.move(file, dest);
                    continue;
                }
                Path aside = setAside(ctx.world, file, SpaceAddon.SOURCE + "/" + name + "/" + rel);
                ctx.report.lineNow(ctx.world, "DE: Space's " + name + " " + rel + " moved to " + ctx.world.relativize(aside) + "; NTM: Space's own stays");
            }
        }
        try (Stream<Path> dirs = Files.walk(body)) {
            for (Path dir : dirs.sorted(Comparator.reverseOrder()).toList()) Files.delete(dir);
        }
        ctx.journal.mark(id(), ctx.world, to);
        ctx.report.count("dimensions merged");
    }

    /**
     * The import's finished backup: the one it recorded, else the one an earlier importer's report names, else the
     * earliest beside the world.
     */
    static @Nullable Path backup(Path world) throws IOException {
        Path parent = world.getParent();
        if (parent == null) return null;
        String name = null;
        if (Files.isRegularFile(world.resolve(BackupStep.RECORD))) {
            name = Files.readString(world.resolve(BackupStep.RECORD), StandardCharsets.UTF_8).trim();
        } else if (Files.isRegularFile(world.resolve(Report.FILE))) {
            for (String line : Files.readAllLines(world.resolve(Report.FILE), StandardCharsets.UTF_8))
                if (line.startsWith("backup: ")) name = Path.of(line.substring("backup: ".length()).trim()).getFileName().toString();
        }
        if (name != null) {
            Path named = parent.resolve(name);
            return Files.isDirectory(named.resolve("dimensions").resolve(SpaceAddon.SOURCE)) ? named : null;
        }
        String prefix = world.getFileName() + "_DE_backup_";
        try (Stream<Path> list = Files.list(parent)) {
            return list.filter(p -> p.getFileName().toString().startsWith(prefix))
                    .filter(p -> Files.isDirectory(p.resolve("dimensions").resolve(SpaceAddon.SOURCE)))
                    .min(Comparator.comparing(p -> p.getFileName().toString())).orElse(null);
        }
    }

    static Path backed(Path backup, Path body) {
        return backup.resolve("dimensions").resolve(SpaceAddon.SOURCE).resolve(body.getFileName().toString());
    }

    static boolean hasTerrain(Path body) {
        return Files.isDirectory(body.resolve("region")) || Files.isDirectory(body.resolve("entities"));
    }

    /** The first chunk of a body's terrain still carries DE's stamp: no terrain step converted it. Only reads. */
    static boolean stamped(Path body) throws IOException {
        for (Path file : RegionIo.regionFiles(body.resolve("region"))) {
            boolean[] stamp = {false, false};
            try {
                RegionIo.readOnly(file, (pos, chunk) -> {
                    stamp[0] = true;
                    stamp[1] = chunk.contains(ChunkConverter.STAMP);
                    return false;
                });
            } catch (IOException e) {
                // a file that cannot be opened says nothing either way; the next one may
                continue;
            }
            if (stamp[0]) return stamp[1];
        }
        return false;
    }

    /** Moves {@code path} under {@code hbm_de_kept/<name>}, numbered if an earlier run kept one there. */
    static Path setAside(Path world, Path path, String name) throws IOException {
        Path aside = world.resolve(KEPT).resolve(name);
        for (int i = 2; Files.exists(aside); i++) aside = world.resolve(KEPT).resolve(name + "_" + i);
        Files.createDirectories(aside.getParent());
        Files.move(path, aside);
        return aside;
    }

    static long size(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return 0;
        long[] total = new long[1];
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                total[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });
        return total[0];
    }

    static Path source(Path world) {
        return world.resolve("dimensions").resolve(SpaceAddon.SOURCE);
    }

    static Path target(Path world) {
        return world.resolve("dimensions").resolve(SpaceAddon.TARGET);
    }
}
