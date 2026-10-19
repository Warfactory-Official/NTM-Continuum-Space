package com.ntmspace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.render.Sky;
import dev.golem.core.render.Skyboxes;
import dev.golem.core.world.Skies;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The orbit's sky, hbm-space's {@code SkyProviderOrbit} with {@code WorldProviderOrbit}'s sky values: the stars tilted
 * 80 degrees, the sun (its corona widening near the terminator), and every body as seen from a station on a one-hour
 * orbit around {@link OrbitalStation#clientStation}'s body - the orbited body itself drawn huge below (up to
 * {@link SolarSystem#MAX_APPARENT_SIZE_ORBIT}), untilted, its surface scrolling past. During a transfer the vantage
 * point moves between the two orbits and the sun turns to the travel direction.
 *
 * <p>The light: the time of day is the angle of the sun around the orbited body ({@code WorldProviderOrbit.updateSky},
 * memoised once per frame here as there), the sun's brightness is 1 minus the eclipse, the stars show in an eclipse and
 * by day beyond Duna; no sunsets, no clouds, black sky and fog (the dimension type). The server keeps 1.7's angle 0
 * (noon): {@code updateSky} only ever ran on the client.
 *
 * <p>The drawing reuses {@link MunSky}'s 1.7 helpers (the sun with the star's Dyson swarms included); what they leave
 * out (atmosphere glows, rings, other stations, impact and war effects) is left out here too.
 */
public final class OrbitSky implements Skyboxes.Skybox {

    private static final float ORBITAL_TILT = 80;

    /** {@code WorldProviderOrbit}'s memo: the last frame's sky. */
    private static float celestialAngle;
    private static double eclipseAmount;
    private static List<SolarSystem.AstroMetric> metrics = List.of();
    private static SolarSystem.Body lastBody;

    private OrbitSky() {}

    static void init() {
        Skyboxes.SKYBOXES.register(SpaceSkies.PHASE, new OrbitSky());
        Skies.MODIFIERS.register(SpaceSkies.PHASE, new Skies.Modifier() {
            @Override
            public boolean active(Level level) {
                return Orbit.inOrbit(level);
            }

            @Override
            public float daylight(Level level, float daylight) {
                float angle = level.isClientSide() ? celestialAngle : 0;
                return 0.5F + 2.0F * Mth.clamp(Mth.cos(angle * (float) Math.PI * 2.0F), -0.25F, 0.25F);
            }

            @Override
            public float starBrightness(Level level, float brightness) {
                return OrbitSky.starBrightness(0);
            }

            @Override
            public float skyLight(Level level, float factor) {
                return 1.0F - (float) eclipseAmount;
            }

            @Override
            public Vec3 skyColor(Level level, Vec3 color) {
                return Vec3.ZERO;
            }

            @Override
            public Vec3 fogColor(Level level, Vec3 color) {
                return Vec3.ZERO;
            }

            @Override
            public float[] sunriseColor(Level level, float[] rgba) {
                return null;
            }
        });
    }

    /** {@code WorldProviderOrbit.updateSky}: the metrics, the sun's angle and the eclipse for this frame. */
    static void updateSky(ClientLevel level, float partialTicks) {
        OrbitalStation station = OrbitalStation.clientStation;
        double ticks = MunSky.skyTicks(level, partialTicks);

        // First fetch the suns true size
        double sunSize = SolarSystem.calculateSunSize(station.orbiting);
        double progress = station.getTransferProgress(partialTicks);

        if (station.state == OrbitalStation.StationState.ORBIT) {
            double altitude = Orbit.getOrbitalAltitude(station.orbiting);
            metrics = SolarSystem.calculateMetricsFromSatellite(ticks, station.orbiting, altitude);
        } else {
            double fromAlt = Orbit.getOrbitalAltitude(station.orbiting);
            double toAlt = Orbit.getOrbitalAltitude(station.target);
            metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(ticks, station.orbiting, station.target, fromAlt, toAlt, progress);

            double sunTargetSize = SolarSystem.calculateSunSize(station.target);
            sunSize = lerp(progress, sunSize, sunTargetSize);
        }

        // Get our sun angle
        float angle = (float) SolarSystem.calculateSingleAngle(ticks, metrics, station.orbiting, Orbit.getOrbitalAltitude(station.orbiting));
        if (progress > 0) {
            angle = (float) clerp(progress, angle,
                    (float) SolarSystem.calculateSingleAngle(ticks, metrics, station.target, Orbit.getOrbitalAltitude(station.target)));
        }

        celestialAngle = 0.5F - (angle / 360.0F);
        eclipseAmount = SpaceSkies.eclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
    }

    /**
     * {@code WorldProviderOrbit.getStarBrightness}: the eclipse, but at least the distance factor - stars by day from just
     * before Kerbin's orbit (9 Gm), fully beyond Duna's (30 Gm).
     */
    static float starBrightness(float partialTicks) {
        float distanceStart = 9_000_000;
        float distanceEnd = 30_000_000;

        OrbitalStation station = OrbitalStation.clientStation;
        double progress = station.getTransferProgress(partialTicks);
        float semiMajorAxisKm = station.orbiting.getPlanet().semiMajorAxisKm;
        if (progress > 0) {
            semiMajorAxisKm = (float) lerp(progress, semiMajorAxisKm, station.target.getPlanet().semiMajorAxisKm);
        }

        float distanceFactor = Mth.clamp((semiMajorAxisKm - distanceStart) / (distanceEnd - distanceStart), 0F, 1F);
        float starBrightness = (float) eclipseAmount;
        return Mth.clamp(starBrightness, distanceFactor, 1F);
    }

    @Override
    public boolean replacesCelestial(ClientLevel level) {
        return Orbit.inOrbit(level);
    }

    @Override
    public void celestial(Sky sky) {
        ClientLevel level = sky.level();
        float partialTicks = sky.partialTick();
        // 1.7: getSkyColor ran updateSky once at the start of every frame
        updateSky(level, partialTicks);

        OrbitalStation station = OrbitalStation.clientStation;
        double progress = station.getTransferProgress(partialTicks);
        double ticks = MunSky.skyTicks(level, partialTicks);

        float solarAngle = getCelestialAngle(partialTicks, station);
        float siderealAngle = (float) SolarSystem.calculateSiderealAngle(station.orbiting, ticks);
        float celestialPhase = (1 - (solarAngle + 0.5F) % 1) * 2 - 1;

        float starBrightness = starBrightness(partialTicks);

        MunSky.renderStars(sky, starBrightness, solarAngle + siderealAngle, ORBITAL_TILT);

        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(ORBITAL_TILT));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(solarAngle * 360.0F));

        double sunSize = SolarSystem.calculateSunSize(station.orbiting) * SolarSystem.SUN_RENDER_SCALE;
        if (station.state != OrbitalStation.StationState.ORBIT) {
            double sunTargetSize = SolarSystem.calculateSunSize(station.target) * SolarSystem.SUN_RENDER_SCALE;
            sunSize = lerp(progress, sunSize, sunTargetSize);
        }
        double coronaSize = sunSize * (3 - smoothstep(Math.abs(celestialPhase), 0.7, 0.8));

        MunSky.renderSun(sky, station.orbiting.getStar(), new float[] {1.0F, 1.0F, 1.0F}, (float) sunSize, (float) coronaSize, 1, 0);

        SolarSystem.Body orbiting = station.orbiting;
        if (station.state != OrbitalStation.StationState.ORBIT && progress > 0.5) orbiting = station.target;

        // 1.7 scrolled the orbited body by getWorldTime(), the time of day (in orbit the overworld's, vanilla's day time)
        double worldTime = dev.golem.core.world.TimeOfDay.get(level) + (double) partialTicks;
        MunSky.renderCelestials(sky, metrics, Vec3.ZERO, 1, 1, SolarSystem.MAX_APPARENT_SIZE_ORBIT, orbiting, worldTime);
        pose.popPose();
    }

    /** {@code SkyProviderOrbit.getCelestialAngle}: all angles normalised to -180/180 while the station travels. */
    private static float getCelestialAngle(float partialTicks, OrbitalStation station) {
        float solarAngle = celestialAngle;
        if (station.state == OrbitalStation.StationState.ORBIT) return solarAngle;

        solarAngle = solarAngle * 360.0F - 180.0F;

        if (station.state != OrbitalStation.StationState.ARRIVING) lastBody = station.orbiting;
        if (lastBody == null) lastBody = station.orbiting;

        double progress = station.getUnscaledProgress(partialTicks);
        float travelAngle = -(float) SolarSystem.calculateSingleAngle(metrics, lastBody, station.target);
        travelAngle = Mth.wrapDegrees(travelAngle + 90.0F);

        if (station.state == OrbitalStation.StationState.TRANSFER) {
            return (travelAngle + 180.0F) / 360.0F;
        } else if (station.state == OrbitalStation.StationState.LEAVING) {
            return ((float) clerp(progress, solarAngle, travelAngle) + 180.0F) / 360.0F;
        } else {
            return ((float) clerp(progress, travelAngle, solarAngle) + 180.0F) / 360.0F;
        }
    }

    /** {@code BobMathUtil.lerp}. */
    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    /** {@code BobMathUtil.clerp}: the short way round, in degrees. */
    private static double clerp(double delta, double start, double end) {
        double angle = ((((end - start) % 360) + 540) % 360) - 180;
        return start + angle * delta;
    }

    /** {@code Library.smoothstep}. */
    private static double smoothstep(double t, double edge0, double edge1) {
        t = Mth.clamp((t - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
