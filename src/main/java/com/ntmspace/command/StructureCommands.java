package com.ntmspace.command;

import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.nbt.NBTStructure;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.populate.Populate17;
import com.ntmspace.world.structure.BodyDungeonStructure;
import com.ntmspace.world.structure.BodyStructures;
import dev.golem.core.Ids;
import dev.golem.core.command.CompatCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

/**
 * {@code /ntmspace structure find|goto <spawn>} and {@code /ntmspace structure here}: the planet structures by 1.7's own
 * placement ({@link BodyStructures#pick}, {@link BodyDungeonStructure#site}), for the harness and for players. Operators
 * only. {@code find} logs {@code structure_find <spawn> <x> <z>} (the nearest start chunk's centre, region by region
 * outwards from the player; for the two dungeons the centre of the nearest built one) or {@code structure_find <spawn> NONE}; {@code goto} also teleports there, above the ground;
 * {@code here} logs {@code structure_here <names>} for the structure pieces that reach the player's chunk. A spawn is a
 * 1.7 name ({@code duna_comms}, {@code meteor_dungeon}, ...) or {@code spaceship} / {@code ancient_tomb}.
 */
public final class StructureCommands {

    /** Regions searched outwards (a region is {@code structureMaxChunks} chunks). */
    private static final int REGION_RADIUS = 48;
    /** Chunks searched outwards for the per-chunk dungeons. */
    private static final int CHUNK_RADIUS = 160;

    private StructureCommands() {}

    public static void init() {
        CompatCommands.server(StructureCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("structure")
                        .then(Commands.literal("find").then(Commands.argument("spawn", StringArgumentType.word())
                                .executes(c -> find(c.getSource(), StringArgumentType.getString(c, "spawn"), false))))
                        .then(Commands.literal("goto").then(Commands.argument("spawn", StringArgumentType.word())
                                .executes(c -> find(c.getSource(), StringArgumentType.getString(c, "spawn"), true))))
                        .then(Commands.literal("here").executes(c -> here(c.getSource())))));
    }

    private static String bodyId(ServerLevel level) {
        String id = Ids.keyId(level.dimension()).toString();
        return id.startsWith(NtmSpace.MOD_ID + ":") ? id.substring(NtmSpace.MOD_ID.length() + 1) : id;
    }

    private static int find(CommandSourceStack source, String spawn, boolean go) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        int[] found = switch (spawn) {
            case "spaceship", "ancient_tomb" -> findDungeon(level, spawn, player.getBlockX() >> 4, player.getBlockZ() >> 4);
            default -> findNbt(level, spawn, player.getBlockX() >> 4, player.getBlockZ() >> 4);
        };
        if (found == null) {
            String message = "structure_find " + spawn + " NONE on " + bodyId(level);
            NtmSpace.LOG.info(message);
            source.sendFailure(Component.literal(message));
            return 0;
        }
        String message = "structure_find " + spawn + " " + found[0] + " " + found[1];
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        if (go) {
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            int y = generator.getBaseHeight(found[0], found[1], Heightmap.Types.WORLD_SURFACE_WG, level,
                    level.getChunkSource().randomState()) + 12;
            dev.golem.core.world.Teleports.toLevel(player, level, found[0] + 0.5, y, found[1] + 0.5);
        }
        return 1;
    }

    /** The nearest region (ring by ring) whose 1.7 pick is {@code spawn}: its start chunk's centre, or null. */
    private static int @Nullable [] findNbt(ServerLevel level, String spawn, int chunkX, int chunkZ) {
        BodyStructures.Body body = BodyStructures.Body.byId(bodyId(level));
        if (body == null) return null;
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        int spacing = BodyStructures.spacing();
        int rx0 = Math.floorDiv(chunkX, spacing);
        int rz0 = Math.floorDiv(chunkZ, spacing);
        for (int r = 0; r <= REGION_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int[] chunk = regionChunk(level.getSeed(), body, rx0 + dx, rz0 + dz);
                    BodyStructures.Biome biome = BodyStructures.biomeAt(body, source, chunk[0], chunk[1]);
                    if (biome == null) continue;
                    BodyStructures.Pick pick = BodyStructures.pick(body, level.getSeed(), chunk[0], chunk[1], biome);
                    if (pick != null && spawn.equals(pick.member().spawn())) return new int[] {chunk[0] * 16 + 8, chunk[1] * 16 + 8};
                }
            }
        }
        return null;
    }

    /** The chunk 1.7 picked in region {@code (rx, rz)} (the first two draws of the region random). */
    private static int[] regionChunk(long seed, BodyStructures.Body body, int rx, int rz) {
        int spacing = BodyStructures.spacing();
        var rand = new net.minecraft.world.level.levelgen.WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(0L));
        rand.setLargeFeatureWithSalt(seed, rx, rz, body.salt());
        int x = rx * spacing + rand.nextInt(spacing - BodyStructures.separation());
        int z = rz * spacing + rand.nextInt(spacing - BodyStructures.separation());
        return new int[] {x, z};
    }

    /**
     * The nearest chunk (ring by ring) whose generator built the dungeon (its roll hit and its site test passed): the
     * centre of what it built.
     */
    private static int @Nullable [] findDungeon(ServerLevel level, String id, int chunkX, int chunkZ) {
        BodyDungeonStructure.Kind kind = BodyDungeonStructure.Kind.byId(id);
        Populate17.Body body = null;
        for (Populate17.Body b : Populate17.Body.values()) if (b.name().toLowerCase(Locale.ROOT).equals(bodyId(level))) body = b;
        if (kind == null || body == null) return null;
        LegacyDungeonWorld.Terrain terrain = BodyDungeonStructure.terrain(kind, level.getChunkSource().getGenerator(),
                level.getChunkSource().randomState(), level);
        for (int r = 0; r <= CHUNK_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BodyDungeonStructure.Site site = BodyDungeonStructure.site(kind, body, level.getSeed(), chunkX + dx, chunkZ + dz, terrain);
                    if (site == null) continue;
                    LegacyDungeonWorld.Result built = BodyDungeonStructure.run(kind, body, site, terrain.height(site.x(), site.z()), terrain);
                    if (built == null) continue;
                    // the built dungeon's centre, not the builder's column: a column on a chunk's last row can leave the
                    // whole build in the next chunk (the spaceship writes nothing on its own column), and `here` looks
                    // at the player's chunk
                    var box = built.bounds();
                    return new int[] {(box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2};
                }
            }
        }
        return null;
    }

    /** Logs the structures whose pieces reach the player's chunk. */
    private static int here(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        int cx = player.getBlockX() >> 4;
        int cz = player.getBlockZ() >> 4;
        List<String> names = new ArrayList<>();
        // the starts this chunk references (a start lives in its own chunk, the others hold references to it)
        ChunkAccess here = level.getChunk(cx, cz);
        java.util.Set<Long> starts = new java.util.TreeSet<>();
        starts.add((cx & 0xFFFFFFFFL) | ((cz & 0xFFFFFFFFL) << 32)); // ChunkPos's long form, as the references are kept
        for (it.unimi.dsi.fastutil.longs.LongSet refs : here.getAllReferences().values()) starts.addAll(refs);
        for (long key : starts) {
            ChunkAccess chunk = level.getChunk((int) key, (int) (key >>> 32));
            for (StructureStart start : chunk.getAllStarts().values()) {
                if (!start.isValid()) continue;
                for (StructurePiece piece : start.getPieces()) {
                    if (!piece.getBoundingBox().intersects(cx * 16, cz * 16, cx * 16 + 15, cz * 16 + 15)) continue;
                    if (piece instanceof NBTStructure.Component component) names.add(component.getStructureName() + "/"
                            + (component.getPiece() == null ? "?" : component.getPiece().name));
                    else if (piece instanceof BodyDungeonStructure.Piece dungeon) names.add(dungeon.kind().id);
                }
            }
        }
        String message = "structure_here " + bodyId(level) + " " + cx + "," + cz + " " + (names.isEmpty() ? "NONE" : String.join(" ", names));
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return names.isEmpty() ? 0 : 1;
    }
}
