package com.ntmspace.world;

import com.hbm.api.environment.Environment;
import com.ntmspace.data.SpaceData;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;

/** 1.7 EntityEffectHandler's unfiltered sunlight floor, before equipment shielding and contamination handling. */
public final class SolarRadiation {
    private SolarRadiation() {}

    public static void init() {
        Environment.AMBIENT_RADIATION.register((level, pos, entity) -> level instanceof ServerLevel server
                ? skyRadiation(server, pos, 0.0D) : 0.0D);
    }

    /** Retains a higher existing dose and uses the body's unfiltered sun, not panel power after an eclipse. */
    public static double skyRadiation(ServerLevel level, BlockPos pos, double existing) {
        var orbital = Orbit.inOrbit(level);
        var body = SpaceSkies.body(level);
        if (body == null && !orbital) return existing;
        if (level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() < 14) return existing;
        if (orbital) body = OrbitalStation.getStationFromPosition(level.getServer(), pos.getX(), pos.getZ()).orbiting;
        var exposure = body.getSunPower();
        var atmosphere = orbital ? null : CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere != null) exposure -= (float) atmosphere.getPressure() * 4.0F;
        exposure *= SpaceData.CELESTIAL_RAD_MULTIPLIER.get().floatValue();
        return Math.max(existing, exposure);
    }
}
