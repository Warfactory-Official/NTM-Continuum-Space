package com.ntmspace.orbit;

import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Teleports;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code CelestialTeleporter}: moving an entity between bodies and the orbit level. The move is queued and
 * run from the server tick, one per level per tick (1.7 polled the queue once per world tick end), never from inside
 * an entity's own update. Rockets, the orbit fall and {@code /ntmstations} call {@link #teleport}.
 *
 * <ul>
 *   <li>{@code grounded}: the y is replaced by 5 above the topmost non-air block of the target column (1.7
 *       {@code placeInPortal}; the column's chunk is loaded or generated for it). Otherwise the target chunk is just
 *       loaded and the y kept (a fall from orbit starts at y 800).</li>
 *   <li>A vehicle carrying a player is moved by moving the player: the player is sent first, the vehicle after it
 *       (vanilla recreates it in the target level with the same UUID), then the player is put back in it and the
 *       vehicle told ({@link StationDock#teleportedWith}, 1.7's "rocket stickiness").</li>
 * </ul>
 * 1.7's extra {@code setPositionAndUpdate(x, 900, z)} after remounting was a client refresh hack and is not ported.
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
     * Queues moving {@code entity} to {@code x, y, z} of {@code target}. Ignored when the entity is in that level
     * already (1.7: "ignore if we're teleporting to the same place"), and on the client.
     */
    public static synchronized void teleport(Entity entity, ResourceKey<Level> target, double x, double y, double z, boolean grounded) {
        if (entity.level().dimension().equals(target)) return;
        if (entity.level().isClientSide()) return;
        QUEUE.add(new Pending(entity, target, x, y, z, grounded));
    }

    /** How many moves wait (GameTests). */
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
        ServerLevel targetLevel = server.getLevel(pending.target);
        if (targetLevel == null) return;
        Entity entity = pending.entity;

        // If this entity got teleported with a player rider, switch to the rider!
        if (entity.getFirstPassenger() instanceof ServerPlayer rider) entity = rider;
        if (entity.isRemoved()) return;
        // queued twice before the first ran (the orbit fall asks every tick): the second finds it there already
        if (entity.level().dimension().equals(pending.target)) return;

        double x = pending.x, y = pending.y, z = pending.z;
        y = placeY(targetLevel, x, y, z, pending.grounded);

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
        int ix = (int) x;
        int iz = (int) z;
        if (grounded) {
            for (int i = target.getMaxY() + 1; i > target.getMinY(); i--) {
                if (!target.getBlockState(new BlockPos(ix, i, iz)).isAir()) return i + 5;
            }
            return y;
        }
        target.getBlockState(new BlockPos(ix, Mth.clamp((int) y, 1, 255), iz)); // dummy load to maybe gen chunk
        return y;
    }
}
