package com.ntmspace.tileentity.machine;

import com.hbm.api.computer.ComputerComponents;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.world.SolarSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * hbm-space's StarDar OpenComputers component ({@code TileEntityMachineStardar.getComponentName} {@code "ntm_stardar"},
 * "This one is COOL"), through HBM's {@link ComputerComponents} (a CC: Tweaked peripheral wherever CC is loaded):
 * <ul>
 *   <li>{@code getPlanetStats(name)}: name, parent's name (or nil), star's name, the body it is tidally locked to,
 *       axial tilt, whether it can be landed on, mass in kg, the drive processing level from here, radius in km,
 *       semi-major axis in km, sun power, surface gravity in m/s^2, rotational period in ticks and orbital period in
 *       KSP days (0 for the star) - or {@code nil, "No body with that name found."};</li>
 *   <li>{@code getCurrentPlanet()}: the name of the body the StarDar stands on (Kerbin in orbit or the overworld, as
 *       1.7's {@code CelestialBody.getBody} fell back);</li>
 *   <li>{@code getSatellites(name)}: the names of the body's moons (or planets of the star), or the not-found pair.</li>
 * </ul>
 * 1.7's arguments were {@code args.checkString(0)}: a missing or non-string name is a Lua error.
 */
public final class StardarComputer {

    public static final String NAME = "ntm_stardar";
    private static final Object[] NOT_FOUND = {null, "No body with that name found."};

    private StardarComputer() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        Map<String, ComputerComponents.Method<TileEntityMachineStardar>> methods = new LinkedHashMap<>();
        methods.put("getPlanetStats", (stardar, args) -> planetStats(Orbit.bodyOf(stardar.getLevel()), checkString(args, 0)));
        methods.put("getCurrentPlanet", (stardar, args) -> new Object[] {Orbit.bodyOf(stardar.getLevel()).name});
        methods.put("getSatellites", (stardar, args) -> satellites(checkString(args, 0)));
        ComputerComponents.register(TileEntityMachineStardar.class, NAME, methods, SpaceMachinesD.MACHINE_STARDAR);
    }

    /** OpenComputers' {@code Arguments.checkString(index)}. */
    static String checkString(Object[] args, int index) {
        if (index >= args.length || !(args[index] instanceof String s)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected)");
        }
        return s;
    }

    /** 1.7 {@code getPlanetStats}, as seen from {@code here}. */
    public static Object[] planetStats(SolarSystem.Body here, String name) {
        SolarSystem.Body body = SolarSystem.body(name);
        if (body == null) return NOT_FOUND.clone();
        String parentName = null;
        double orbitalPeriod = 0.0;
        if (body.parent != null) {
            parentName = body.parent.name;
            orbitalPeriod = body.getOrbitalPeriod();
        }
        return new Object[] {
                body.name,
                parentName,
                body.getStar().name,
                body.tidallyLockedTo,
                body.axialTilt,
                Orbit.isLandable(body),
                body.massKg,
                DriveBody.processingLevel(body, here),
                body.radiusKm,
                body.semiMajorAxisKm,
                body.getSunPower(),
                body.getSurfaceGravity(),
                body.getRotationalPeriod(),
                orbitalPeriod
        };
    }

    /** 1.7 {@code getSatellites}. */
    public static Object[] satellites(String name) {
        SolarSystem.Body body = SolarSystem.body(name);
        if (body == null) return NOT_FOUND.clone();
        List<String> names = new ArrayList<>();
        for (SolarSystem.Body satellite : body.satellites) names.add(satellite.name);
        return names.toArray();
    }
}
