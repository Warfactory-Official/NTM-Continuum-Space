package com.ntmspace.world;

import net.minecraft.util.Mth;

/**
 * The pure formulas of hbm-space's sky, fog and weather rendering, side-neutral so GameTests can pin them: the fog
 * densities ({@code WorldProviderCelestial/Duna/Laythe.fogDensity}) and the exponential fog they make ({@code GL_EXP}) and the port's linear stand-in for it,
 * the weather colour ({@code WorldProviderCelestial.getWeatherColor}), the atmosphere style and the storm darkening
 * ({@code AtmosphereRenderUtil}) and Eve's lightning schedule ({@code getEveFlashStrength}).
 */
public final class SkyMath {

    private SkyMath() {}

    /** {@code WorldProviderCelestial.fogDensity}: {@code p * p * 0.002} above 2 atm, else no fog change (0). */
    public static float fogDensity(float pressure) {
        if (pressure <= 2F) return 0;
        return pressure * pressure * 0.002F;
    }

    /** {@code WorldProviderDuna.fogDensity}: a dust storm of at least 0.25 by the square of its eased strength, else -1. */
    public static float dustStormFogDensity(float stormSmoothed) {
        return stormSmoothed >= 0.25F ? stormSmoothed * stormSmoothed * 0.075F : -1.0F;
    }

    /** {@code WorldProviderLaythe.fogDensity}: 0.03 with the camera in water. */
    public static final float LAYTHE_WATER_FOG_DENSITY = 0.03F;

    /** 1.7 {@code GL_EXP}: the fog's share at {@code distance} blocks from the eye, {@code 1 - e^(-density * distance)}. */
    public static double exponentialFog(double density, double distance) {
        return 1.0D - Math.exp(-density * distance);
    }

    /**
     * The port's linear stand-in for 1.7's {@code GL_EXP} fog of {@code density}: a linear fog from the eye ending where
     * the exponential one is 95 % opaque, {@code ln 20 / density} (59.9 blocks for Eve's 0.05). Modern shaders know only
     * linear fog and the vanilla fog include is not overridden (user decision 2026-10-11).
     */
    public static float linearFogEnd(float density) {
        return (float) (Math.log(20.0D) / density);
    }

    /** The linear fog's share at {@code distance} blocks from the eye with its end at {@code end}: {@code distance / end}, at most 1. */
    public static double linearFog(double end, double distance) {
        return Math.min(Math.max(distance / end, 0.0D), 1.0D);
    }

    /**
     * {@code getWeatherColor} for a liquid of colour {@code 0xRRGGBB}: desaturated to 35 % around its luminance
     * ({@code 0.299 r + 0.587 g + 0.114 b}), each channel clamped to 0..1.
     */
    public static float[] weatherColor(int fluidColor) {
        double r = ((fluidColor >> 16) & 0xFF) / 255.0D;
        double g = ((fluidColor >> 8) & 0xFF) / 255.0D;
        double b = (fluidColor & 0xFF) / 255.0D;
        double luminance = r * 0.299D + g * 0.587D + b * 0.114D;
        double saturation = 0.35D;
        return new float[] {
                (float) Mth.clamp(luminance + (r - luminance) * saturation, 0.0D, 1.0D),
                (float) Mth.clamp(luminance + (g - luminance) * saturation, 0.0D, 1.0D),
                (float) Mth.clamp(luminance + (b - luminance) * saturation, 0.0D, 1.0D)};
    }

    /** {@code getAtmosphereStyle} of a body that is no gas giant: 2 (haze) above 3 atm, 1 (clouds) from 0.5, else 0. */
    public static int atmosphereStyle(float pressure) {
        if (pressure > 3.0F) return 2;
        if (pressure >= 0.5F) return 1;
        return 0;
    }

    /** {@code getBodyCloudStormDarkness}'s target: raining 0.22, thundering 0.48, or the animated strengths, at most 0.9. */
    public static float stormDarkness(boolean raining, boolean thundering, float rainStrength, float thunderStrength) {
        float rainStateDarkness = raining ? 0.22F : 0.0F;
        float thunderStateDarkness = thundering ? 0.48F : 0.0F;
        float animatedDarkness = rainStrength * 0.35F + thunderStrength * 0.22F;
        return Mth.clamp(Math.max(rainStateDarkness + thunderStateDarkness, animatedDarkness), 0.0F, 0.9F);
    }

    private static final float EVE_STRIKE_WINDOW = 8.0F;
    private static final float EVE_STRIKE_FADE_TIME = 5.0F;
    private static final float EVE_STRIKE_DELAY_RANGE = 2.4F;
    private static final float EVE_STRIKE_GATE_THRESHOLD = 0.82F;
    private static final double EVE_HASH_MULTIPLIER = 43758.5453123D;

    /** {@code getEveFlashStrength}: one possible strike every 8 s, delayed and gated by a hash of the window. */
    public static float eveFlashStrength(float atmosphereTime) {
        float eventIndex = Mth.floor(atmosphereTime / EVE_STRIKE_WINDOW);
        float eventTime = atmosphereTime - eventIndex * EVE_STRIKE_WINDOW;
        float strikeDelay = eveHash(eventIndex, 41.9F) * EVE_STRIKE_DELAY_RANGE;
        if (eventTime < strikeDelay) return 0.0F;
        if (eveHash(eventIndex, 313.7F) < EVE_STRIKE_GATE_THRESHOLD) return 0.0F;
        float t = Math.max(eventTime - strikeDelay, 0.0F);
        float strikeIntro = smoothstep(0.0F, 0.14F, t);
        float strikeFade = 1.0F - smoothstep(0.16F, EVE_STRIKE_FADE_TIME, t);
        return Mth.clamp(strikeIntro * strikeFade, 0.0F, 1.0F);
    }

    private static float eveHash(float x, float y) {
        double value = Math.sin(x * 127.1D + y * 311.7D) * EVE_HASH_MULTIPLIER;
        return (float) (value - Math.floor(value));
    }

    /** {@code AtmosphereRenderUtil.smoothstep}. */
    public static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) return value < edge0 ? 0.0F : 1.0F;
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
