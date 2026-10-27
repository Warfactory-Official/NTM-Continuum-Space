package com.ntmspace.orbit;

import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Teleports;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Moves an entity, its root vehicle and its passengers between bodies and the orbit level. On versions with vanilla
 * passenger transitions the move runs immediately; a caller iterating the departing level's players iterates a copy.
 * Older versions retain the end-of-tick transfer and remount worker.
 */
public final class SpaceTeleporter {

    private record Pending(Entity entity, ResourceKey<Level> target, double x, double y, double z, boolean grounded) {}

    private static final Queue<Pending> QUEUE = new ArrayDeque<>();

    /** A player waiting to be put back in its vehicle once the vehicle is visible in the target level. */
    private record Remount(ServerPlayer player, UUID vehicle, ResourceKey<Level> level, int deadline) {}

    private static final java.util.List<Remount> REMOUNTS = new java.util.ArrayList<>();

    private SpaceTeleporter() {}

    static void init() {
        Lifecycle.onServerTick(SpaceTeleporter::tick);
    }

    /**
     * Moves {@code entity} to {@code x, y, z} of {@code target}. Ignored when the entity is in that level already,
     * and on the client. Refuses a destination whose server level does not exist.
     */
    public static synchronized void teleport(Entity entity, ResourceKey<Level> target, double x, double y, double z, boolean grounded) {
        if (entity.level().dimension().equals(target)) return;
        if (entity.level().isClientSide()) return;
        var server = ((ServerLevel) entity.level()).getServer();
        if (Teleports.defersTransfers()) {
            targetLevel(server, target);
            QUEUE.add(new Pending(entity, target, x, y, z, grounded));
        } else {
            run(server, new Pending(entity, target, x, y, z, grounded));
        }
    }

    /** Defers a move until the player-copy callback has finished placing the new player in its respawn level. */
    public static synchronized void teleportAfterRespawn(Entity entity, ResourceKey<Level> target,
                                                          double x, double y, double z, boolean grounded) {
        if (entity.level().dimension().equals(target) || entity.level().isClientSide()) return;
        targetLevel(((ServerLevel) entity.level()).getServer(), target);
        QUEUE.add(new Pending(entity, target, x, y, z, grounded));
    }

    private static ServerLevel targetLevel(MinecraftServer server, ResourceKey<Level> target) {
        var level = server.getLevel(target);
        if (level == null) throw new IllegalArgumentException("no level is declared for " + target);
        return level;
    }

    /** How many deferred moves wait. */
    public static synchronized int pending() {
        return QUEUE.size();
    }

    private static void tick(MinecraftServer server) {
        remounts(server);
        int levels = 0;
        for (ServerLevel ignored : server.getAllLevels()) levels++;
        for (int i = 0; i < levels; i++) {
            Pending next;
            synchronized (SpaceTeleporter.class) {
                next = QUEUE.poll();
            }
            if (next == null) return;
            run(server, next);
        }
    }

    /** Runs one move now (the queue's worker; GameTests call it directly). */
    static void run(MinecraftServer server, Pending pending) {
        var targetLevel = targetLevel(server, pending.target);
        Entity entity = pending.entity;

        if (entity.isRemoved()) return;
        // queued twice before the first ran (the orbit fall asks every tick): the second finds it there already
        if (entity.level().dimension().equals(pending.target)) return;

        double x = pending.x, y = pending.y, z = pending.z;
        y = placeY(targetLevel, x, y, z, pending.grounded);

        var position = new Vec3(x, y, z);
        // Modern vanilla moves the root with its passengers at once; legacy moves player and craft separately below.
        if (Teleports.transferRoot(entity.getRootVehicle(), targetLevel, position, SpaceTeleporter::teleported)) return;

        // A player drives the older transfer path so it can remount the recreated craft.
        if (entity.getFirstPassenger() instanceof ServerPlayer rider) entity = rider;
        if (entity instanceof ServerPlayer player) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.isAlive()) {
                UUID vehicleId = vehicle.getUUID();
                player.stopRiding();
                Teleports.toLevel(player, targetLevel, x, y, z);
                Teleports.toLevel(vehicle, targetLevel, x, y, z);
                // The recreated vehicle is only visible to a UUID lookup once its chunk's entity section is tracked,
                // which can take the target chunk's ticket a tick or more: retry from the server tick until then.
                if (!remount(player, targetLevel, vehicleId)) {
                    synchronized (SpaceTeleporter.class) {
                        REMOUNTS.add(new Remount(player, vehicleId, pending.target, server.getTickCount() + 100));
                    }
                }
            } else {
                Teleports.toLevel(player, targetLevel, x, y, z);
            }
        } else {
            Teleports.toLevel(entity, targetLevel, x, y, z);
        }
    }

    // Vanilla invokes this after the transferred entity's passengers have been put back aboard.
    private static void teleported(Entity moved) {
        if (moved instanceof StationDock dock) {
            for (var passenger : moved.getPassengers()) {
                if (passenger instanceof ServerPlayer player) dock.teleportedWith(player);
            }
        }
    }

    /** Puts the player back in its vehicle in {@code level}; false while the vehicle is not visible there yet. */
    private static boolean remount(ServerPlayer player, ServerLevel level, UUID vehicleId) {
        Entity moved = level.getEntity(vehicleId);
        if (moved == null) return false;
        // forced: stopRiding gave the player vanilla's boarding cooldown (1.7 mountEntity had none)
        Teleports.remount(player, moved);
        // Ensure rocket stickiness
        if (moved instanceof StationDock dock) dock.teleportedWith(player);
        return true;
    }

    private static void remounts(MinecraftServer server) {
        java.util.List<Remount> due;
        synchronized (SpaceTeleporter.class) {
            if (REMOUNTS.isEmpty()) return;
            due = new java.util.ArrayList<>(REMOUNTS);
            REMOUNTS.clear();
        }
        for (Remount remount : due) {
            ServerLevel level = server.getLevel(remount.level());
            if (level == null || remount.player().isRemoved() || !remount.player().level().dimension().equals(remount.level())) continue;
            if (remount(remount.player(), level, remount.vehicle())) continue;
            if (server.getTickCount() < remount.deadline()) {
                synchronized (SpaceTeleporter.class) {
                    REMOUNTS.add(remount);
                }
            }
        }
    }

    /** 1.7 {@code placeInPortal}: the y the entity arrives at. */
    static double placeY(ServerLevel target, double x, double y, double z, boolean grounded) {
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        // Loads, or generates, the arrival chunk before consulting its surface height.
        var chunk = target.getChunk(ix >> 4, iz >> 4);
        if (!grounded) return y;
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, ix, iz);
        return top > target.getMinY() ? top + 5 : y;
    }
}
