package com.ntmspace.rocket;

import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.world.SolarSystem;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemVOTVdrive.Target}: where a craft is or goes, as orbital mechanics sees it: the body, whether in
 * orbit of it (at a station) and whether the target exists (a station that has been launched).
 */
public record RocketTarget(SolarSystem.@Nullable Body body, boolean inOrbit, boolean isValid) {

    /**
     * 1.7 {@code CelestialBody.getTarget(world, x, z)}: in orbit, the station at {@code x, z} (the client's synced
     * station on the client); on a surface, its body.
     */
    public static RocketTarget of(Level level, int x, int z) {
        if (Orbit.inOrbit(level)) {
            OrbitalStation station = !level.isClientSide() && level.getServer() != null
                    ? OrbitalStation.getStationFromPosition(level.getServer(), x, z) : OrbitalStation.clientStation;
            return new RocketTarget(station.orbiting, true, station.hasStation);
        }

        return new RocketTarget(Orbit.bodyOf(level), false, true);
    }
}
