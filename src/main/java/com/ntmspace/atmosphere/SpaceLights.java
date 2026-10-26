package com.ntmspace.atmosphere;

import net.minecraft.world.level.levelgen.FlatLevelSource;
import com.hbm.blocks.machine.FloodlightBeam;
import com.hbm.blocks.machine.SpotlightBeam;
import com.ntmspace.trait.CBT_Lights;
import com.ntmspace.trait.CelestialTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's lights on a body's night side ({@code CBT_Lights}), counted where 1.7 counted them: a block brighter than
 * 10 placed above the horizon adds its light value, broken removes it (except beams, whose placement counts as a break
 * without having added), and a repaired spotlight adds its own ({@code Environment.LIGHT_ON}).
 */
public final class SpaceLights {

    private SpaceLights() {}

    static void placed(ServerLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() < horizon(level)) return;
        int light = state.getLightEmission();
        if (light <= 10) return;
        add(level, light);
    }

    static void broken(ServerLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() <= horizon(level)) return;
        int light = state.getLightEmission();
        if (light <= 10) return;
        // Placing blocks onto beams counts as a break, but doesn't reduce light count (1.7)
        if (state.getBlock() instanceof SpotlightBeam || state.getBlock() instanceof FloodlightBeam) return; // 1.7 BlockBeamBase
        CBT_Lights lights = CelestialTraits.getTrait(level, CBT_Lights.class);
        lights = lights == null ? new CBT_Lights() : lights.copy();
        lights.removeLight(light);
        CelestialTraits.modifyTraits(level, lights);
    }

    /** A light value added to the body of {@code level} (1.7 {@code addLight} + {@code modifyTraits}). */
    public static void add(ServerLevel level, int light) {
        CBT_Lights lights = CelestialTraits.getTrait(level, CBT_Lights.class);
        lights = lights == null ? new CBT_Lights() : lights.copy();
        lights.addLight(light);
        CelestialTraits.modifyTraits(level, lights);
    }

    /**
     * 1.7 {@code WorldProvider.getHorizon}: the flat generator's minimum Y (the GameTest arena's case),
     * else {@link AtmosphereHandler#HORIZON}.
     */
    static int horizon(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof FlatLevelSource
                ? level.getMinY()
                : AtmosphereHandler.HORIZON;
    }
}
