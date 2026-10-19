package com.ntmspace.orbit;

import dev.golem.core.hook.Hook;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A craft docked at a station port: what hbm-space's {@code TileEntityOrbitalStation} asked of its
 * {@code EntityRideableRocket}. The rideable rocket (a later part of the port) implements this on its entity; the port
 * ({@code TileEntityOrbitalStation}) holds at most one, refuels it from its tank and stows it on a sneak-click.
 *
 * <p>Docking: a rocket arriving at a station takes a port from {@link OrbitalStation#getPort(Level, int, int)},
 * {@code reservePort()}s it, and on landing calls {@code port.dockRocket(this)}. A port spawns a craft from a held item
 * through {@link #SPAWNERS} (1.7: a fuelled {@code rocket_custom}, or a {@code rp_capsule_20} / {@code rp_pod_20}).
 */
public interface StationDock {

    /** The entity that is the craft. */
    Entity entity();

    /** 1.7 {@code isReusable()}: a pod or capsule that refuels at the port (rockets with stages do not). */
    boolean isReusable();

    /** 1.7 {@code riddenByEntity != null}. */
    boolean hasRider();

    /** 1.7 {@code getState() == RocketState.UNDOCKING}: the craft is leaving the port. */
    boolean isUndocking();

    /** 1.7 {@code getTarget().inOrbit}: whether the craft's destination is another station (fuel needed is 500 mB). */
    boolean targetInOrbit();

    /** 1.7 {@code getRocket().getLaunchMass()}, kg. */
    int launchMass();

    /**
     * 1.7's per-tick fuel check at the port: with enough fuel a {@code NEEDSFUEL} craft becomes {@code AWAITING} (with a
     * nav drive) or {@code LANDED}; without, any other state becomes {@code NEEDSFUEL}. The craft keeps its own state.
     */
    void fuelled(boolean hasFuel);

    /** 1.7 {@code interactFirst(player)}: the player boards. */
    void board(Player player);

    /**
     * 1.7 {@code despawnRocket}: removes the craft and returns what goes into the port's slots, in the order 1.7 filled
     * them (its stack popped the nav drive first, then the rocket item or capsule).
     */
    List<ItemStack> stow();

    /** 1.7 {@code dropNDie(null)}: the port was removed under the craft. */
    void dropAndDie();

    /** 1.7 {@code state == AWAITING || state == LANDED}: ready to be recalled to a surface. */
    boolean canRecall();

    /** 1.7 {@code recallPod(destination)}: fly down to {@code x, z} on {@code level}'s surface. */
    void recall(ResourceKey<Level> level, int x, int z);

    /** After {@link SpaceTeleporter} moved a rider together with this craft (1.7 "rocket stickiness": setThrower). */
    default void teleportedWith(ServerPlayer rider) {}

    /** Makes a craft from a stack held against a free port, or null for a stack that is no craft. */
    @FunctionalInterface
    interface Spawner {
        @Nullable StationDock spawn(com.ntmspace.tileentity.station.TileEntityOrbitalStation port, ItemStack held);
    }

    /**
     * Whether a stack is a craft a free port would take (the look overlay's "place held rocket" hint, on the client
     * too). 1.7: a fuelled {@code rocket_custom}, {@code rp_capsule_20} or {@code rp_pod_20}.
     */
    Hook<java.util.function.Predicate<ItemStack>> CRAFT_ITEMS = Hook.of(listeners -> stack -> {
        for (java.util.function.Predicate<ItemStack> test : listeners) {
            if (test.test(stack)) return true;
        }
        return false;
    });

    /** Craft makers, in order; the first non-null answer is docked. Plain NTM: Space has none until rockets exist. */
    Hook<Spawner> SPAWNERS = Hook.of(listeners -> (port, held) -> {
        for (Spawner spawner : listeners) {
            StationDock dock = spawner.spawn(port, held);
            if (dock != null) return dock;
        }
        return null;
    });
}
