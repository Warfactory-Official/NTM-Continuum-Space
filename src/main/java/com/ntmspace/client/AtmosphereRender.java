package com.ntmspace.client;

import com.ntmspace.trait.CBT_Weather;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.CloudLayers17;
import com.ntmspace.world.NukeShocks;
import com.ntmspace.world.SkyMath;
import com.ntmspace.world.SolarSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code AtmosphereRenderUtil}: the values its planet shaders are fed for a body seen in the sky (and on the
 * Stardar) - the air's pressure, density, style (clear, clouds, haze, gas bands), colours, the cloud tint, the storm
 * darkening eased over wall-clock time, the lightning strength and Eve's flash schedule, and the nuke-shock uniforms.
 * Pure maths except where 1.7 read the client's synced traits.
 */
public final class AtmosphereRender {

    private AtmosphereRender() {}

    static final float OPAQUE_ATMOSPHERE_PRESSURE = 5.0F;
    static final float CLOUDY_ATMOSPHERE_PRESSURE = 0.5F;
    static final float DENSE_HAZE_PRESSURE = 3.0F;
    static final float CLOUD_STORM_SMOOTHING_RATE = 6.0F;

    public static final int ATMOSPHERE_STYLE_CLEAR = 0;
    public static final int ATMOSPHERE_STYLE_CLOUDS = 1;
    public static final int ATMOSPHERE_STYLE_HAZE = 2;
    public static final int ATMOSPHERE_STYLE_GAS_BANDS = 3;
    public static final int LIGHTNING_MODE_STANDARD = 0;
    public static final int LIGHTNING_MODE_EVE = 1;
    static final float DEFAULT_NUKE_SHOCK_CENTER = 0.5F;

    /** {@code CloudStormFadeState} per body (1.7 keyed it by dimension id). */
    private static final Map<String, float[]> STORM_FADE = new HashMap<>();
    private static final Map<String, Long> STORM_FADE_TIME = new HashMap<>();

    /** 1.7 {@code body.gas != null}: the gas giants. */
    static boolean isGasGiant(SolarSystem.Body body) {
        return CelestialSkyObjects.isGasGiant(body);
    }

    /** {@code getAtmospherePressure}: 5 for a gas giant, the air's pressure, 0 without air. */
    public static float pressure(SolarSystem.Body body) {
        if (body == null) return 0.0F;
        if (isGasGiant(body)) return OPAQUE_ATMOSPHERE_PRESSURE;
        if (!Atmospheres.has(body.name)) return 0.0F;
        return Math.max(0.0F, (float) Atmospheres.pressure(body.name));
    }

    /** {@code getAtmosphereDensity} (and {@code getAtmosphereSurfaceAlpha}): the pressure over 5 atm, 0..1. */
    public static float density(SolarSystem.Body body) {
        return Mth.clamp(pressure(body) / OPAQUE_ATMOSPHERE_PRESSURE, 0.0F, 1.0F);
    }

    /** {@code getAtmosphereStyle}: gas bands for a gas giant, haze above 3 atm, clouds from 0.5 atm, else clear. */
    public static int style(SolarSystem.Body body) {
        if (body == null) return ATMOSPHERE_STYLE_CLEAR;
        if (isGasGiant(body)) return ATMOSPHERE_STYLE_GAS_BANDS;
        return style(pressure(body));
    }

    /** {@code getAtmosphereStyle} of a body that is not a gas giant, by its pressure ({@link SkyMath#atmosphereStyle}). */
    public static int style(float pressure) {
        return SkyMath.atmosphereStyle(pressure);
    }

    /** {@code getBodyAtmosphereColor}. */
    public static Vec3 atmosphereColor(SolarSystem.Body body) {
        if (body == null) return new Vec3(1, 1, 1);
        return CelestialSkyObjects.atmosphereColor(body);
    }

    /** {@code getBodyCloudColor}: a gas giant's gas colour scaled to its peak, else white pulled towards the air's tint. */
    public static Vec3 cloudColor(SolarSystem.Body body) {
        if (body == null) return new Vec3(1, 1, 1);
        if (isGasGiant(body)) {
            Vec3 gas = atmosphereColor(body);
            double peak = Math.max(gas.x, Math.max(gas.y, gas.z));
            if (peak <= 0.0D) return new Vec3(1, 1, 1);
            return new Vec3(Mth.clamp(gas.x / peak, 0, 1), Mth.clamp(gas.y / peak, 0, 1), Mth.clamp(gas.z / peak, 0, 1));
        }
        double[] tinted = CloudLayers17.tinted(Atmospheres.of(body.name), 1.0D, 1.0D, 1.0D);
        return new Vec3(tinted[0], tinted[1], tinted[2]);
    }

    /** {@code getBodyCloudTintStrength}: 1 for a gas giant, else {@code CloudProviderCelestial.getCloudTintStrength}. */
    public static float cloudTintStrength(SolarSystem.Body body) {
        if (body == null) return 0.0F;
        if (isGasGiant(body)) return 1.0F;
        double[] tint = CloudLayers17.tintData(Atmospheres.of(body.name));
        return tint != null ? (float) tint[0] : 0.0F;
    }

    /** {@code getBodyCloudStormDarkness}: the weather's darkening, eased at 6 per second of wall-clock time. */
    public static float cloudStormDarkness(SolarSystem.Body body, float partialTicks) {
        float target = stormDarknessTarget(body, partialTicks);
        if (body == null) return target;
        return smoothStormDarkness(body.name, target, System.currentTimeMillis());
    }

    /** The darkness the storm eases towards (1.7's {@code targetDarkness}). */
    static float stormDarknessTarget(SolarSystem.Body body, float partialTicks) {
        CBT_Weather weather = body != null ? CelestialTraits.getTrait(body.name, CBT_Weather.class, true) : null;
        if (weather == null || !CBT_Weather.supportsWeather(body.name, true)) return 0.0F;
        return stormDarkness(weather.raining, weather.thundering, weather.getRainStrength(partialTicks),
                weather.getThunderStrength(partialTicks));
    }

    /** 1.7's storm darkening from the weather state ({@link SkyMath#stormDarkness}). */
    public static float stormDarkness(boolean raining, boolean thundering, float rainStrength, float thunderStrength) {
        return SkyMath.stormDarkness(raining, thundering, rainStrength, thunderStrength);
    }

    /** {@code smoothCloudStormDarkness}: a per-body state eased towards the target by the wall-clock time between calls. */
    static float smoothStormDarkness(String body, float target, long now) {
        float[] state = STORM_FADE.get(body);
        if (state == null) {
            STORM_FADE.put(body, new float[] {target});
            STORM_FADE_TIME.put(body, now);
            return target;
        }
        float deltaSeconds = Mth.clamp((now - STORM_FADE_TIME.get(body)) / 1000.0F, 0.0F, 0.5F);
        float blend = Mth.clamp(deltaSeconds * CLOUD_STORM_SMOOTHING_RATE, 0.0F, 1.0F);
        state[0] += (target - state[0]) * blend;
        STORM_FADE_TIME.put(body, now);
        return state[0];
    }

    /** {@code getBodyCloudLightningStrength}: Eve's activity factor, elsewhere the thunder strength times it. */
    public static float cloudLightningStrength(SolarSystem.Body body, float partialTicks) {
        CBT_Weather weather = body != null ? CelestialTraits.getTrait(body.name, CBT_Weather.class, true) : null;
        if (weather == null || !CBT_Weather.supportsWeather(body.name, true) || !weather.canSpawnLightning) return 0.0F;
        float activity = CBT_Weather.getLightningActivityFactor(body.name, true);
        if (isEve(body)) return Mth.clamp(activity, 0.0F, 1.0F);
        return Mth.clamp(weather.getThunderStrength(partialTicks) * activity, 0.0F, 1.0F);
    }

    /** {@code getBodyLightningMode}. */
    public static int lightningMode(SolarSystem.Body body) {
        return isEve(body) ? LIGHTNING_MODE_EVE : LIGHTNING_MODE_STANDARD;
    }

    /** {@code getBodyEveFlashStrength}. */
    public static float eveFlashStrength(SolarSystem.Body body, float atmosphereTime) {
        return isEve(body) ? eveFlashStrength(atmosphereTime) : 0.0F;
    }

    private static boolean isEve(SolarSystem.Body body) {
        return body != null && "eve".equals(body.name);
    }

    /** {@code getEveFlashStrength} ({@link SkyMath#eveFlashStrength}). */
    public static float eveFlashStrength(float atmosphereTime) {
        return SkyMath.eveFlashStrength(atmosphereTime);
    }

    /**
     * {@code applyNukeShockUniforms}: up to 4 shocks into {@code params} from {@code offset} - the count, then 4 times,
     * 4 centres x, 4 centres y, 4 strengths; unused slots time -1, centre 0.5, strength 0.
     */
    public static void nukeShockParams(float[] params, int offset, List<NukeShocks.Shock> shocks, double currentTime) {
        int count = Math.min(shocks != null ? shocks.size() : 0, NukeShocks.MAX_ACTIVE_SHOCKS);
        params[offset] = count;
        for (int i = 0; i < NukeShocks.MAX_ACTIVE_SHOCKS; i++) {
            float time = -1.0F, cx = DEFAULT_NUKE_SHOCK_CENTER, cy = DEFAULT_NUKE_SHOCK_CENTER, strength = 0.0F;
            if (i < count) {
                NukeShocks.Shock shock = shocks.get(i);
                time = (float) (currentTime - shock.time());
                cx = shock.centerX();
                cy = shock.centerY();
                strength = shock.strength();
            }
            params[offset + 1 + i] = time;
            params[offset + 5 + i] = cx;
            params[offset + 9 + i] = cy;
            params[offset + 13 + i] = strength;
        }
    }
}
