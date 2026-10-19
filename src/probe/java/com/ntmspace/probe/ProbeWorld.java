package com.ntmspace.probe;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * The probe's world-level extension points, the ones NTM: Space declares too: a mod dimension
 * ({@code Dimensions.dimension}, {@code data/ntmprobe/dimension/probe.json}) with its own time of day, a world type
 * ({@code WorldTypes.declare} + its world preset and parameter list), a furnace fuel ({@code Fuels.register} and the
 * NeoForge {@code furnace_fuels} data map), Golem's biome feature installer called a second time, and HBM's switches
 * (the vein miner's stone predicate widened again, NTM: Space's other switches left as NTM: Space set them).
 */
public final class ProbeWorld {

    public static final String DIMENSION = "probe";
    public static final String WORLD_TYPE = "probe";
    /** Ticks a feather burns while the probe is installed (NTM: Space's flora fuels are its own). */
    public static final int FEATHER_BURN = 77;

    public static ResourceKey<Level> LEVEL;

    private ProbeWorld() {}

    public static void init() {
        LEVEL = dev.golem.core.world.Dimensions.dimension(ProbeEntry.MOD_ID, DIMENSION);
        // NTM: Space already gave the Mun its own clock: a second call for the same dimension does nothing (documented)
        dev.golem.core.world.TimeOfDay.ownClock(com.ntmspace.world.SpaceDimensions.MUN);
        dev.golem.core.world.TimeOfDay.ownClock(LEVEL);
        dev.golem.core.world.WorldTypes.declare(ProbeEntry.MOD_ID, WORLD_TYPE);
        // idempotent installer of every mod's Dejson biome features (NTM: Space calls it too)
        dev.golem.core.world.BiomeFeatures.install();
        // the vein miner's stop predicate: widened on top of HBM's and NTM: Space's, never replaced
        com.hbm.handler.ability.IToolAreaAbility.RECURSION_STONE.updateAndGet(previous ->
                previous.or(state -> state.is(Blocks.CALCITE)));
    }

    /** Golem's furnace fuels, after every mod's registers are filled (NeoForge: the probe's data map file instead). */
    public static void registered() {
        Map<Supplier<? extends Item>, Integer> fuels = Map.of(() -> Items.FEATHER, FEATHER_BURN);
        dev.golem.core.registry.Fuels.register(fuels);
    }
}
