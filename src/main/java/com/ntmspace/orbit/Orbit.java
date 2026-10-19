package com.ntmspace.orbit;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.CelestialBodies;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceDimensions;
import dev.golem.core.world.Dimensions;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * hbm-space's orbit dimension ({@code dim/orbit}, {@code SpaceConfig.orbitDimension}): one void level shared by every
 * orbital station, each station in its own {@link OrbitalStation#STATION_SIZE} square of the grid. The level is authored
 * JSON ({@code data/ntmspace/dimension/orbit.json}, its type and the {@code ntmspace:orbit} biome, 1.7's
 * {@code BiomeGenOrbit} "Space"); the generator is {@link OrbitChunkGenerator}.
 *
 * <p>{@link #inOrbit} is hbm-space's {@code CelestialBody.inOrbit(world)}, answered to HBM through
 * {@code Environment.ORBIT} ({@link OrbitEnvironment}).
 */
public final class Orbit {

    /** {@code ntmspace:orbit}, the orbit level (1.7 dimension 413023 by default). */
    public static final ResourceKey<Level> ORBIT = Dimensions.dimension(NtmSpace.MOD_ID, "orbit");

    /** {@code ntmspace:orbit}: hbm-space's {@code ChunkProviderOrbit}. */
    public static final Dimensions.GeneratorType GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "orbit", () -> OrbitChunkGenerator.CODEC);

    /** hbm-space {@code SpaceConfig.maxProbeDistance} default: how far from 0, 0 a fall from orbit lands. */
    public static final int MAX_PROBE_DISTANCE = 32_000;
    /** hbm-space {@code SpaceConfig.maxStationDistance} default: how far from the centre new stations are placed. */
    public static final int MAX_STATION_DISTANCE = 32_000;

    /**
     * The bodies with a surface level, by body name (1.7 {@code CelestialBody.dimensionId}; Kerbin is the overworld).
     * Thatmo's body has no level in hbm-space (see {@link CelestialBodies#THATMO}).
     */
    private static final Map<String, ResourceKey<Level>> LEVELS = new LinkedHashMap<>();

    static {
        LEVELS.put("kerbin", Level.OVERWORLD);
        LEVELS.put("mun", SpaceDimensions.MUN);
        LEVELS.put("minmus", SpaceDimensions.MINMUS);
        LEVELS.put("duna", SpaceDimensions.DUNA);
        LEVELS.put("moho", SpaceDimensions.MOHO);
        LEVELS.put("dres", SpaceDimensions.DRES);
        LEVELS.put("eve", SpaceDimensions.EVE);
        LEVELS.put("ike", SpaceDimensions.IKE);
        LEVELS.put("laythe", SpaceDimensions.LAYTHE);
        LEVELS.put("tekto", SpaceDimensions.TEKTO);
    }

    private Orbit() {}

    public static void init() {
        OrbitEnvironment.init();
        SpaceTeleporter.init();
    }

    /** hbm-space's {@code CelestialBody.inOrbit(world)}. */
    public static boolean inOrbit(Level level) {
        return level.dimension().equals(ORBIT);
    }

    /**
     * The surface level of {@code body} (1.7 {@code body.dimensionId}); a body without one answers the overworld, as
     * 1.7's {@code CelestialBody.getBody} fell back to Kerbin.
     */
    public static ResourceKey<Level> levelOf(SolarSystem.Body body) {
        ResourceKey<Level> level = body == null ? null : LEVELS.get(body.name);
        return level == null ? Level.OVERWORLD : level;
    }

    /** Whether {@code body} has a surface level of its own (1.7 {@code CelestialBody.getLandableBodies}). */
    public static boolean isLandable(SolarSystem.Body body) {
        return body != null && LEVELS.containsKey(body.name);
    }

    /** The landable bodies in hbm-space's {@code SolarSystem.Body} order (ORBIT excluded). */
    public static Iterable<String> landableNames() {
        return LEVELS.keySet();
    }

    /**
     * The body whose surface {@code level} is, for orbit sums (1.7 {@code CelestialBody.getBody(world)}): Kerbin for the
     * overworld, a level that is no body, and the orbit level itself (1.7 had no body for the orbit dimension).
     */
    public static SolarSystem.Body bodyOf(Level level) {
        for (Map.Entry<String, ResourceKey<Level>> entry : LEVELS.entrySet()) {
            if (entry.getValue().equals(level.dimension())) return SolarSystem.body(entry.getKey());
        }
        return SolarSystem.body("kerbin");
    }

    /** The surface air pressure of a body in atm (1.7 {@code CBT_Atmosphere.getPressure()}), 0 without air. */
    public static double pressureOf(SolarSystem.Body body) {
        if (body == null) return 0;
        for (CelestialBodies.Body known : new CelestialBodies.Body[] {CelestialBodies.KERBIN, CelestialBodies.MUN,
                CelestialBodies.EVE, CelestialBodies.MOHO, CelestialBodies.MINMUS, CelestialBodies.DUNA, CelestialBodies.IKE,
                CelestialBodies.DRES, CelestialBodies.LAYTHE, CelestialBodies.TEKTO, CelestialBodies.THATMO}) {
            if (known.name().equals(body.name)) return known.pressure();
        }
        return 0;
    }

    /**
     * {@code WorldProviderOrbit.getOrbitalAltitude}: the radius (m) of the orbit whose period is 7200 s around
     * {@code body}, so every station circles its body in an hour of real time. Float arithmetic as 1.7.
     */
    public static float getOrbitalAltitude(SolarSystem.Body body) {
        float period = ORBITAL_PERIOD;
        return (float) Math.cbrt((SolarSystem.G_FLOAT * body.massKg * (period * period)) / (4 * Math.PI * Math.PI));
    }

    /** {@code WorldProviderOrbit.ORBITAL_PERIOD}. */
    public static final float ORBITAL_PERIOD = 7200;
}
