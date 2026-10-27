package com.ntmspace.world;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The celestial bodies NTM: Space has dimensions for, with the values hbm-space's {@code SolarSystem} gives them. Kerbin
 * is the overworld, as in hbm-space (it replaced the overworld's provider); a level that is no body has no entry.
 *
 * <p>Only what the ported systems read so far is here (the Mun's slice: gravity and whether air is breathable); the
 * rest of {@code CelestialBody} (orbits, traits, the sky) arrives with the systems that use it.
 */
public final class CelestialBodies {

    /** hbm-space's {@code AstronomyUtil.GRAVITATIONAL_CONSTANT}. */
    public static final float GRAVITATIONAL_CONSTANT = 6.6743015e-11F;
    /** hbm-space's {@code AstronomyUtil.STANDARD_GRAVITY}: 1.6 blocks per second squared is vanilla's 0.08 per tick. */
    public static final float STANDARD_GRAVITY = 1.6F;
    /** hbm-space's {@code AstronomyUtil.PLAYER_GRAVITY_MODIFIER}. */
    public static final float PLAYER_GRAVITY_MODIFIER = STANDARD_GRAVITY / 9.81F;

    /**
     * A body: mass in kg, radius in km, its surface air pressure in atmospheres (hbm-space's
     * {@code CBT_Atmosphere.getPressure()}, 0 without an atmosphere) and whether that air lets a fire breathe
     * ({@code CBT_Atmosphere.hasFluid(EARTHAIR, 0.19) || hasFluid(OXYGEN, 0.09)}); {@code atmosphere} is the registry
     * name of its air's fluid where one is stated (hbm-space's {@code CBT_Atmosphere} fluid), else null.
     */
    public record Body(String name, float massKg, float radiusKm, double pressure, boolean breathable, String atmosphere) {

        /** A body whose air (if any) is not named here ({@link #atmosphere} null). */
        public Body(String name, float massKg, float radiusKm, double pressure, boolean breathable) {
            this(name, massKg, radiusKm, pressure, breathable, null);
        }

        /** hbm-space's {@code CelestialBody.getSurfaceGravity}, in m/s². */
        public float surfaceGravity() {
            float radius = radiusKm * 1000;
            return GRAVITATIONAL_CONSTANT * massKg / (radius * radius);
        }

        /** hbm-space's {@code CelestialBody.getGravity(entity)} for a body's surface, in blocks/s² (1.6 is normal). */
        public float playerGravity() {
            return surfaceGravity() * PLAYER_GRAVITY_MODIFIER;
        }
    }

    /** hbm-space: Kerbin, {@code withMassRadius(5.292e22F, 600)}, earth air at 1 atm. */
    public static final Body KERBIN = new Body("kerbin", 5.292e22F, 600, 1.0D, true);
    /** hbm-space: Mun, {@code withMassRadius(9.76e20F, 200)}, no atmosphere. */
    public static final Body MUN = new Body("mun", 9.76e20F, 200, 0.0D, false);

    /**
     * hbm-space: Eve, {@code withMassRadius(1.224e23F, 700)}, {@code CBT_Atmosphere(Fluids.EVEAIR, 5D)}: thick, not
     * breathable. Its fluid {@code ntmspace:eveair} belongs to the fluids port (not registered yet).
     */
    public static final Body EVE = new Body("eve", 1.224e23F, 700, 5.0D, false, "ntmspace:eveair");
    /** hbm-space: Moho, {@code withMassRadius(2.526e21F, 250)}, no atmosphere. */
    public static final Body MOHO = new Body("moho", 2.526e21F, 250, 0.0D, false);
    /** hbm-space: Minmus, {@code withMassRadius(2.646e19F, 60)}, no atmosphere. */
    public static final Body MINMUS = new Body("minmus", 2.646e19F, 60, 0.0D, false);

    /**
     * hbm-space: Duna, {@code withMassRadius(4.515e21F, 320)}, {@code CBT_Atmosphere(Fluids.DUNAAIR, 0.1D)}: a thin
     * carbon-dioxide air, not breathable. Its fluid {@code ntmspace:dunaair} belongs to the fluids port.
     */
    public static final Body DUNA = new Body("duna", 4.515e21F, 320, 0.1D, false, "ntmspace:dunaair");
    /** hbm-space: Ike, {@code withMassRadius(2.782e20F, 130)}, no atmosphere (its bromine is {@code CBT_Water}). */
    public static final Body IKE = new Body("ike", 2.782e20F, 130, 0.0D, false);
    /** hbm-space: Dres, {@code withMassRadius(3.219e20F, 138)}, no atmosphere. */
    public static final Body DRES = new Body("dres", 3.219e20F, 138, 0.0D, false);

    /**
     * hbm-space: Laythe, {@code withMassRadius(2.94e22F, 500)},
     * {@code CBT_Atmosphere(Fluids.EARTHAIR, 0.45D).and(Fluids.XENON, 0.15D)}: 0.6 atm, breathable (earth air above 0.19).
     * Its main fluid {@code ntmspace:earthair} belongs to the fluids port; the xenon is HBM's {@code hbm:xenon}.
     */
    public static final Body LAYTHE = new Body("laythe", 2.94e22F, 500, 0.6D, true, "ntmspace:earthair");
    /**
     * hbm-space: Tekto, {@code withMassRadius(2.883e21F, 480)}, {@code CBT_Atmosphere(Fluids.TEKTOAIR, 1.5F)}: thick,
     * poisonous, not breathable. Its fluid {@code ntmspace:tektoair} belongs to the fluids port.
     */
    public static final Body TEKTO = new Body("tekto", 2.883e21F, 480, 1.5D, false, "ntmspace:tektoair");
    /**
     * hbm-space: Thatmo, {@code withMassRadius(2.788e21F, 286)}, {@code CBT_Atmosphere(Fluids.NITROGEN, 0.005F)}. This
     * body is NOT what the Thatmo level is: hbm-space gave Thatmo's {@code CelestialBody} no dimension id, so
     * {@code CelestialBody.getBody(world)} on the Thatmo dimension fell back to Kerbin (gravity, air, time of day).
     * The port keeps that: {@link #of} answers {@link #KERBIN} there. Its fluid {@code ntmspace:nitrogen} belongs to the
     * fluids port.
     */
    public static final Body THATMO = new Body("thatmo", 2.788e21F, 286, 0.005D, false, "ntmspace:nitrogen");

    private static final Map<ResourceKey<Level>, Body> BY_LEVEL = Map.ofEntries(
            Map.entry(Level.OVERWORLD, KERBIN),
            Map.entry(SpaceDimensions.MUN, MUN),
            Map.entry(SpaceDimensions.EVE, EVE),
            Map.entry(SpaceDimensions.MOHO, MOHO),
            Map.entry(SpaceDimensions.MINMUS, MINMUS),
            Map.entry(SpaceDimensions.DUNA, DUNA),
            Map.entry(SpaceDimensions.IKE, IKE),
            Map.entry(SpaceDimensions.DRES, DRES),
            Map.entry(SpaceDimensions.LAYTHE, LAYTHE),
            Map.entry(SpaceDimensions.TEKTO, TEKTO),
            // 1.7: getBody fell back to Kerbin on the Thatmo dimension, see THATMO
            Map.entry(SpaceDimensions.THATMO, KERBIN));

    private CelestialBodies() {}

    /** The body {@code level} is the surface of, or null. */
    public static Body of(Level level) {
        return of(level.dimension());
    }

    /** The same surface lookup without requiring the target level to be loaded. */
    public static Body of(ResourceKey<Level> dimension) {
        return BY_LEVEL.get(dimension);
    }
}
