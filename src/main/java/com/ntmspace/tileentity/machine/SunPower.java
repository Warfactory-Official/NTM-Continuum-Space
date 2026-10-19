package com.ntmspace.tileentity.machine;

import com.hbm.api.environment.Environment;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;

/**
 * hbm-space's {@code WorldProviderCelestial.getSunPower(provider, x, z)} on a body's surface, as HBM's
 * {@link Environment#SUNLIGHT}: {@code body.getSunPower() * (1 - eclipse)}. A level that is no celestial body uses
 * Kerbin's sun without an eclipse, as 1.7's fallback {@code CelestialBody.getBody(world).getSunPower()} did; orbit is
 * {@code OrbitEnvironment}'s answer (the station's sun). Read by the solar panel and by HBM's solar boiler.
 */
public final class SunPower {

    private static boolean registered;

    private SunPower() {}

    public static synchronized void init() {
        if (registered) return;
        registered = true;
        Environment.SUNLIGHT.register((level, pos) -> {
            if (Orbit.inOrbit(level)) return Double.NaN;
            SolarSystem.Body body = SolarSystem.body(CelestialTraits.bodyOf(level));
            if (body == null) return Double.NaN;
            // SpaceSkies.body is null for a level that is no celestial provider (the Nether, the End): no eclipse there
            double eclipse = SpaceSkies.body(level) == null ? 0.0D : SpaceSkies.eclipse(level);
            return body.getSunPower() * (1 - (float) eclipse);
        });
    }
}
