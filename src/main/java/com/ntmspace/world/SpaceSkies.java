package com.ntmspace.world;

import com.ntmspace.orbit.Orbit;
import com.ntmspace.weather.DustStorm;
import dev.golem.core.world.TimeOfDay;
import com.hbm.handler.ImpactSky;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.Skies;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The time of day and the light on a celestial body, as hbm-space's {@code WorldProviderCelestial} gave them: the sun
 * follows the body's own solar day ({@link SolarSystem#solarAngle}; the Mun's lasts about 154,800 ticks), and the
 * daylight vanilla derives from the sun's angle (1.7 {@code getSunBrightnessFactor}, which also drives mob spawning) is
 * derived from that angle instead (Golem Core's {@link Skies.Modifier}). The same side-neutral {@link SpaceSkyColors} formulas
 * supply the renderer and common level queries; camera and fog integration stay client-scoped.
 *
 * <p>Kerbin is the overworld, as in hbm-space ({@code WorldProviderEarth}): its sky is the solar system seen from Kerbin,
 * but its time of day stays vanilla's. hbm-space's Kerbin day ({@code getDayLength}: Kerbin's rotation corrected by its
 * year) is 23,999.7 ticks against vanilla's 24,000 with the same angle formula, so the overworld keeps vanilla's angle
 * and with it every vanilla consumer of the time of day. Thatmo's level is Kerbin's sky too (hbm-space gave Thatmo's body
 * no dimension, so {@code CelestialBody.getBody} answered Kerbin there; see {@link CelestialBodies#THATMO}), on
 * Thatmo's own day.
 *
 * <p>Each body's level keeps its own time of day, as hbm-space's {@code CelestialBodyWorldSavedData} did (1.7
 * {@code WorldProviderCelestial.getWorldTime}, advanced by one each tick while the daylight cycle runs): Golem Core's
 * {@code TimeOfDay}: native per-body clocks and solar-day timelines on modern nodes, saved counters on older nodes.
 * The same modern clock drives the native sun angle, gameplay daylight and rendered sky; overworld time commands
 * leave the body clocks alone.
 * hbm-space also reset it to 0 when everyone on the body woke up ({@code resetRainAndThunder}); BodyWeather clears
 * the cycle and resets only a body's own advancing clock at that same moment.
 *
 * <p>NTM: Space's sky modifiers run in the phase {@link #PHASE}, before HBM's own (Tom's impact dust and fire, which
 * hbm-space applied last in the same formulas).
 */
public final class SpaceSkies {

    /**
     * The {@link Skies#MODIFIERS} / {@code Skyboxes} phase of NTM: Space's sky, ordered before the default phase.
     */
    public static final String PHASE = "ntmspace:celestial";

    private SpaceSkies() {
    }

    public static void init() {
        Skies.MODIFIERS.addPhaseOrdering(PHASE, Hook.DEFAULT_PHASE);
        Skies.MODIFIERS.register(PHASE, new Skies.Modifier() {
            @Override
            public boolean active(Level level) {
                return body(level) != null;
            }

            @Override
            public float daylight(Level level, float daylight) {
                if (isKerbin(level) || SolarSystem.timeMultiplier(level) == 1 || !TimeOfDay.isNative(level))
                    return daylight;
                return SpaceSkies.daylight(level, 0.0F);
            }

            @Override
            public float sunAngle(Level level, float angle) {
                if (isKerbin(level) || SolarSystem.timeMultiplier(level) == 1) return angle;
                // The Time Wand changes astronomy against raw clock ticks, including its immediate phase jump.
                return SolarSystem.solarAngle(body(level), TimeOfDay.get(level), 0.0F,
                        SolarSystem.timeMultiplier(level)) * 360.0F;
            }

            @Override
            public boolean brightOutside(Level level, boolean bright) {
                return bright && !DustStorm.obscuresDaylight(level);
            }

            @Override
            public Vec3 skyColor(Level level, Vec3 value, Vec3 position, float partialTick) {
                return SpaceSkyColors.skyColor(level, value, partialTick, position);
            }

            @Override
            public Vec3 fogColor(Level level, Vec3 value, Vec3 position, float partialTick) {
                return SpaceSkyColors.fogColor(level, angle(level, partialTick), position);
            }

            @Override
            public Vec3 cloudColor(Level level, Vec3 value) {
                return SpaceSkyColors.cloudColor(level, 0.0F);
            }

            @Override
            public int cloudColor(Level level, int value) {
                return SpaceSkyColors.cloudColor(level, value);
            }

            @Override
            public float starBrightness(Level level, float value) {
                return isKerbin(level) ? value : SpaceSkies.starBrightness(level, 0.0F);
            }

            @Override
            public float skyLight(Level level, float brightness) {
                return SpaceSkyColors.sunBrightness(level,
                        isKerbin(level) ? brightness : sunBrightness(level, 0.0F), 0.0F);
            }

            @Override
            public boolean rawSkyLightFactor(Level level) {
                return true;
            }

            @Override
            public float skyLightFactor(Level level, float factor) {
                return SpaceSkyColors.sunBrightness(level, factor, 0.0F);
            }

            @Override
            public float[] sunriseColor(Level level, float[] rgba) {
                return isKerbin(level) ? rgba : null;
            }
        });
        // Space's provider replaces these four impact components; fog, stars and sunrise retain their existing hooks.
        ImpactSky.OWNERS.register(level -> body(level) != null || Orbit.inOrbit(level)
                ? ImpactSky.SKY_COLOR | ImpactSky.SKY_LIGHT | ImpactSky.DAYLIGHT | ImpactSky.CLOUD_COLOR : 0);
        // MunSky draws every body's sky, Kerbin's (the overworld) included, and hbm-space drew none of HBM's overworld
        // sky objects (lode star, digamma star, Bobmazon satellite, impact sky) there: HBM's layers stay off.
        com.hbm.api.environment.SkyOwnership.OWNERS.register(level -> body(level) != null);
        // every WorldProviderCelestial but Kerbin's (dimension 0) kept its own time of day
        for (var dimension : java.util.List.of(SpaceDimensions.MUN, SpaceDimensions.MINMUS, SpaceDimensions.DUNA,
                SpaceDimensions.IKE, SpaceDimensions.DRES, SpaceDimensions.EVE, SpaceDimensions.MOHO,
                SpaceDimensions.LAYTHE, SpaceDimensions.TEKTO, SpaceDimensions.THATMO)) {
            TimeOfDay.autoClock(dimension);
        }
    }

    /**
     * 1.7 {@code getSunBrightnessFactor} (vanilla's sky darkening) at the body's angle, darkened by the level's rain and
     * thunder as 1.7 did (the body's weather, {@link com.ntmspace.weather.BodyWeather}); Kerbin's is vanilla's own.
     */
    public static float daylight(Level level, float partialTicks) {
        float angle = angle(level, partialTicks);
        float f = 0.5F + 2.0F * Mth.clamp(Mth.cos(angle * (float) Math.PI * 2.0F), -0.25F, 0.25F);
        return weather(level, partialTicks, f);
    }

    /**
     * 1.7's weather darkening: {@code f * (1 - rain * 5 / 16) * (1 - weightedThunder * 5 / 16)}, in doubles.
     */
    private static float weather(Level level, float partialTicks, float f) {
        f = (float) ((double) f * (1.0D - (double) (level.getRainLevel(partialTicks) * 5.0F) / 16.0D));
        f = (float) ((double) f * (1.0D - (double) (level.getThunderLevel(partialTicks) * 5.0F) / 16.0D));
        return f;
    }

    /**
     * 1.7 {@code World.getSunBrightness} before the provider's changes: {@code f * 0.8 + 0.2} at the body's angle, with
     * {@code f} darkened by rain and thunder ({@code getSunBrightnessBody}).
     */
    public static float sunBrightness(Level level, float partialTicks) {
        float angle = angle(level, partialTicks);
        float f = 1.0F - (Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.2F);
        f = 1.0F - Mth.clamp(f, 0.0F, 1.0F);
        f = weather(level, partialTicks, f);
        return f * 0.8F + 0.2F;
    }

    /**
     * {@code WorldProviderCelestial.getStarBrightness}: vanilla's curve at the body's angle; by day only beyond Duna.
     */
    public static float starBrightness(Level level, float partialTicks) {
        float angle = angle(level, partialTicks);
        float f = 1.0F - (Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.25F);
        f = Mth.clamp(f, 0.0F, 1.0F);
        float star = f * f * 0.5F;
        SolarSystem.Body body = body(level);
        float distanceFactor = Mth.clamp((body.getPlanet().semiMajorAxisKm - 20_000_000F) / (80_000_000F - 20_000_000F),
                0F, 1F);
        return Mth.clamp(star, distanceFactor, 1F);
    }

    /**
     * The sky's body for {@code level} (hbm-space's {@code CelestialBody.getBody(world)}): the overworld is Kerbin, and
     * so is Thatmo's level (see the class comment); null for a level that is no body.
     */
    public static SolarSystem.Body body(Level level) {
        if (level.dimension().equals(Level.OVERWORLD)) return SolarSystem.body("kerbin");
        if (level.dimension().equals(SpaceDimensions.MUN)) return SolarSystem.body("mun");
        if (level.dimension().equals(SpaceDimensions.EVE)) return SolarSystem.body("eve");
        if (level.dimension().equals(SpaceDimensions.MOHO)) return SolarSystem.body("moho");
        if (level.dimension().equals(SpaceDimensions.MINMUS)) return SolarSystem.body("minmus");
        if (level.dimension().equals(SpaceDimensions.DUNA)) return SolarSystem.body("duna");
        if (level.dimension().equals(SpaceDimensions.IKE)) return SolarSystem.body("ike");
        if (level.dimension().equals(SpaceDimensions.DRES)) return SolarSystem.body("dres");
        if (level.dimension().equals(SpaceDimensions.LAYTHE)) return SolarSystem.body("laythe");
        if (level.dimension().equals(SpaceDimensions.TEKTO)) return SolarSystem.body("tekto");
        if (level.dimension().equals(SpaceDimensions.THATMO)) return SolarSystem.body("kerbin");
        return null;
    }

    /**
     * Whether {@code level} is Kerbin's own surface, the overworld (vanilla's time of day).
     */
    public static boolean isKerbin(Level level) {
        return level.dimension().equals(Level.OVERWORLD);
    }

    /**
     * The name of the {@link Atmospheres} body whose air {@code level} has ({@code kerbin} on Thatmo), or null.
     */
    public static String airBody(Level level) {
        CelestialBodies.Body body = CelestialBodies.of(level);
        return body == null ? null : body.name();
    }

    /**
     * The body's solar angle now: 0 at noon, 0.5 at midnight. Kerbin's is vanilla's (see the class comment).
     */
    public static float angle(Level level, float partialTicks) {
        if (isKerbin(level) || TimeOfDay.isNative(level)) return Skies.celestialAngle(level, BlockPos.ZERO);
        return SolarSystem.solarAngle(body(level), TimeOfDay.get(level), partialTicks,
                SolarSystem.timeMultiplier(level));
    }

    // ---------------------------------------------------------------------------------------- eclipses

    private static Level eclipseLevel;
    private static long eclipseTick = Long.MIN_VALUE;
    private static long eclipseTimeMultiplier = Long.MIN_VALUE;
    private static double eclipseAmount;

    /**
     * {@code WorldProviderCelestial.updateSky} / {@code getEclipseAmount}: how far another body covers the sun, 0..1,
     * computed once per tick for the client's level.
     */
    /**
     * A harness test hook (query {@code ntmspace_sky} {@code pin_ticks}, with the sky's orbits in {@code MunSky}): the
     * total world time the client's eclipse is computed at, so every node's sun brightness and light map agree although
     * their saves' ages differ; {@link Long#MIN_VALUE} (the default) for the level's own time.
     */
    public static volatile long clientPinnedTicks = Long.MIN_VALUE;

    public static synchronized double eclipse(Level level) {
        long pinned = clientPinnedTicks;
        long tick = level.getGameTime();
        long multiplier = SolarSystem.timeMultiplier(level);
        if (level == eclipseLevel && tick == eclipseTick && multiplier == eclipseTimeMultiplier) return eclipseAmount;
        long orbitTick = level.isClientSide() && pinned != Long.MIN_VALUE ? pinned : tick;
        SolarSystem.Body body = body(level);
        if (body == null) return 0;
        float solarAngle = angle(level, 0.0F);
        List<SolarSystem.AstroMetric> metrics = SolarSystem.calculateMetricsFromBody(orbitTick, body, solarAngle,
                multiplier);
        eclipseAmount = eclipseFactor(metrics, SolarSystem.calculateSunSize(body),
                SolarSystem.MAX_APPARENT_SIZE_SURFACE);
        eclipseLevel = level;
        eclipseTick = tick;
        eclipseTimeMultiplier = multiplier;
        return eclipseAmount;
    }

    /**
     * {@code WorldProviderCelestial.getEclipseFactor}.
     */
    public static double eclipseFactor(List<SolarSystem.AstroMetric> metrics, double sunSize, double maxSize) {
        double factor = 0;
        double sunArc = arc(sunSize);
        for (SolarSystem.AstroMetric metric : metrics) {
            if (metric.apparentSize < 1) continue;
            double planetArc = arc(Mth.clamp(metric.apparentSize, 0, maxSize));
            double minPhase = 1 - (planetArc + sunArc);
            double maxPhase = 1 - (planetArc - sunArc);
            if (metric.phaseObscure < minPhase) continue;
            double thisFactor = 1 - (metric.phaseObscure - maxPhase) / (minPhase - maxPhase);
            factor = Math.min(Math.max(factor, thisFactor), 1.0);
        }
        return factor;
    }

    /**
     * {@code getArc}: apparent size to the 0..1 arc of the flat quads the sky draws.
     */
    private static double arc(double apparentSize) {
        return apparentSize * 0.0017D + Math.sqrt(apparentSize * 0.00003D);
    }
}
