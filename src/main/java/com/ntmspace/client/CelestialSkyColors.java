package com.ntmspace.client;

import com.hbm.api.environment.Environment;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.SpaceSkies;
import com.ntmspace.world.SkyMath;
import com.ntmspace.world.SpaceSkyColors;
import dev.golem.core.client.Fogs;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Celestial sky and fog formulas from WorldProviderCelestial and its body overrides. */
public final class CelestialSkyColors {

    /** hbm-space {@code GeneralConfig.enableHardcoreDarkness} (config key {@code 1.44_hardcoreDarkness}), its default. */
    static final boolean HARDCORE_DARKNESS = SpaceSkyColors.HARDCORE_DARKNESS;

    private CelestialSkyColors() {}

    static void init() {
        Environment.TERRAIN_FOG_RANGE.register(level -> fogDensity(level) > 0F);
        // port: Linear fog replaces 1.7 EXP fog to keep shader-pack compatibility.
        // port: Positive atmosphere and dust fog retain Space 1.7 priority over status and medium fog.
        Fogs.addModifier(new Fogs.Modifier() {
            @Override
            public boolean active(Entity camera) {
                return camera.level().isClientSide() && SpaceSkies.body(camera.level()) != null && fogDensity(camera.level()) > 0F;
            }

            @Override
            public float far(Entity camera, float renderDistanceBlocks) {
                return SkyMath.linearFogEnd(fogDensity(camera.level()));
            }
        });
    }

    /** {@code WorldProviderCelestial.fogDensity}: {@code p * p * 0.002} above 2 atm, else none (0). */
    public static float fogDensity(Level level) {
        return fogDensity(level, Fogs.cameraInWater());
    }

    private static float fogDensity(Level level, boolean inWater) {
        // WorldProviderLaythe.fogDensity: Laythe's clear water, 0.03 with the camera in water
        if (level.dimension().equals(SpaceDimensions.LAYTHE) && inWater) {
            return SkyMath.LAYTHE_WATER_FOG_DENSITY;
        }
        // WorldProviderDuna.fogDensity: a dust storm of 0.25 or more, by the square of its eased strength
        if (level.dimension().equals(SpaceDimensions.DUNA)) {
            float storm = SkyMath.dustStormFogDensity(SkyWeatherClient.dustStorm());
            if (storm >= 0.0F) return storm;
        }
        String air = SpaceSkies.airBody(level);
        if (air == null || !Atmospheres.has(air)) return 0;
        return SkyMath.fogDensity((float) Atmospheres.pressure(air));
    }

    private static Vec3 viewPosition(float partialTicks) {
        var camera = Minecraft.getInstance().getCameraEntity();
        return camera == null ? Vec3.ZERO : camera.getEyePosition(partialTicks);
    }

    static Vec3 skyColor(Level level, Vec3 vanilla, float partialTicks) {
        return SpaceSkyColors.skyColor(level, vanilla, partialTicks, viewPosition(partialTicks));
    }

    static Vec3 skyColorBody(Level level, float partialTicks) {
        return SpaceSkyColors.skyColorBody(level, partialTicks, viewPosition(partialTicks));
    }

    static int skyColorByTemp(float temperature) { return SpaceSkyColors.skyColorByTemp(temperature); }

    static Vec3 fogColor(Level level, float solarAngle) {
        return SpaceSkyColors.fogColor(level, solarAngle, viewPosition(0.0F));
    }

    static float[] vanillaSunset(float angle) { return SpaceSkyColors.vanillaSunset(angle); }

    public static float[] providerSunset(Level level, float angle) { return SpaceSkyColors.providerSunset(level, angle); }

    static float[] skySunset(Level level, float angle) { return SpaceSkyColors.skySunset(level, angle); }

    static float sunBrightness(Level level, float base, float partialTicks) {
        return SpaceSkyColors.sunBrightness(level, base, partialTicks);
    }

    static float[] sunColor(Level level) { return SpaceSkyColors.sunColor(level); }

    static float eveFlash(Level level, float partialTicks) { return SpaceSkyColors.eveFlash(level, partialTicks); }

    /** For the sky renderer: the fog's share of the planets' tint ({@code ModEventHandlerRenderer.lastFogDensity * 30}). */
    static float fogIntensity(Level level) {
        return fogDensity(level) * 30;
    }
}
