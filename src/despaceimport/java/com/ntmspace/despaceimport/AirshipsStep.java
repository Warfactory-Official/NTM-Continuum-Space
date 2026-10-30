package com.ntmspace.despaceimport;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.steps.SubLevelsStep;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Airships of a body {@link LateStep} converted while Sable was missing: they wait aside and convert once it is
 * installed. Airships outside the bodies that an import without NTM: Space converted get back DE: Space's blocks and
 * the entities aboard from the backup, which Sable dropped when it loaded them, or which that import left unconverted.
 */
final class AirshipsStep implements Step {
    /** Folders whose airships Sable could not write this start. */
    private final Set<String> unwritten = new LinkedHashSet<>();

    @Override
    public String id() {
        return "spaceairships@1";
    }

    @Override
    public Set<String> requiresMods() {
        return Set.of("sable");
    }

    @Override
    public boolean applies(Ctx ctx) throws IOException {
        for (Path body : DimensionsStep.moved(ctx)) if (Files.isDirectory(body.resolve(SubLevelsStep.PENDING))) return true;
        return !outside(ctx).isEmpty();
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        for (Path body : DimensionsStep.moved(ctx)) {
            if (Files.isDirectory(body.resolve(SubLevelsStep.PENDING))) LateStep.airships(ctx, SpaceAddon.TARGET + ":" + body.getFileName(), body);
        }
        Path backup = DimensionsStep.backup(ctx.world);
        unwritten.clear();
        Map<String, Path> outside = outside(ctx);
        List<Path> merged = new ArrayList<>();
        for (Map.Entry<String, Path> dim : outside.entrySet()) {
            Path folder = dim.getValue().resolve("sublevels");
            int ships = SubLevelsStep.merge(ctx, dim.getKey(), folder, backup.resolve(ctx.world.relativize(folder).toString()), LateStep.ADDON);
            if (ships < 0 && SubLevelsStep.retry(ctx, folder.resolveSibling("sublevels.merge"), "giving back DE: Space's blocks to the airships in " + ctx.world.relativize(folder))) {
                unwritten.add(ctx.world.relativize(folder).toString());
                continue;
            }
            ctx.report.count("airships given back DE: Space's blocks", Math.max(ships, 0));
            merged.add(folder);
        }
        // the entities aboard sit in Sable's plot grid, where only its storage tells one ship from another; a folder
        // is done once they are back too
        if (!outside.isEmpty() && !LateStep.dropped(ctx, DimensionsStep.target(ctx.world), DimensionsStep.source(ctx.world), backup, true)
                && SubLevelsStep.retry(ctx, ctx.world.resolve("sublevels.de-entities"), "bringing back the DE: Space entities aboard airships")) {
            unwritten.add("the entities aboard");
            return true;
        }
        for (Path folder : merged) ctx.journal.mark(id(), ctx.world, folder);
        return true;
    }

    @Override
    public @Nullable String waitsFor(Ctx ctx) {
        return unwritten.isEmpty() ? null : "airships that could not be read or written now: " + String.join(", ", unwritten);
    }

    /** Dimensions outside the bodies whose airships a world imported without NTM: Space has in the backup too. */
    private Map<String, Path> outside(Ctx ctx) throws IOException {
        Map<String, Path> out = new LinkedHashMap<>();
        ImportState state = ImportState.read(ctx.world);
        Path backup = DimensionsStep.backup(ctx.world);
        if (state == null || !LateStep.withoutAddon(state) || backup == null
                || !ctx.journal.done("spacedimensions@1", ctx.world, DimensionsStep.target(ctx.world))) return out;
        Path target = DimensionsStep.target(ctx.world), source = DimensionsStep.source(ctx.world);
        for (Map.Entry<String, Path> dim : Dimensions.all(ctx.world).entrySet()) {
            Path folder = dim.getValue().resolve("sublevels");
            if (dim.getValue().startsWith(target) || dim.getValue().startsWith(source) || !Files.isDirectory(folder)
                    || !Files.isDirectory(backup.resolve(ctx.world.relativize(folder).toString())) || ctx.journal.done(id(), ctx.world, folder)) continue;
            out.put(dim.getKey(), dim.getValue());
        }
        return out;
    }
}
