package com.ntmspace.client;

import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.client.Fogs;
import dev.golem.core.world.Skies;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The colours of a celestial body's sky, ported from hbm-space's {@code WorldProviderCelestial} ({@code getSkyColor},
 * {@code getFogColor}, {@code calcSunriseSunsetColors}, {@code getSunBrightness}, {@code getSunColor},
 * {@code fogDensity}) and its per-body overrides ({@code WorldProviderEve} lightning flashes, {@code WorldProviderTekto}'s
 * dimmer sun, {@code WorldProviderThatmo}'s). The air comes from {@link Atmospheres}: each fluid adds its colour weighted
 * by its share of the pressure, and the total pressure thins the sky and the fog. Applied through Golem Core's
 * {@link Skies.Modifier} (phase {@link SpaceSkies#PHASE}, before HBM's impact dust) and {@link Fogs.Modifier}.
 *
 * <p>Differences, each because there is no seam for the 1.7 behaviour:
 * <ul>
 *   <li>The fog densities are 1.7's {@code fogDensity}: thick air (above 2 atm, Eve), Duna's dust storm
 *       ({@link SkyWeatherClient#dustStorm}) and Laythe's clear water (0.03 with the camera in water).</li>
 *   <li>1.7's {@code fogDensity} switched GL to exponential fog ({@code GL_EXP}, radial with {@code GL_NV_fog_distance}).
 *       Modern shaders know only linear fog, and Golem does not override vanilla's fog include (user decision
 *       2026-10-11: it would clash with other mods and shader packs), so the port draws a linear fog from the eye to
 *       where the exponential fog is 95 % opaque ({@link com.ntmspace.world.SkyMath#linearFogEnd}, {@code ln 20 / density},
 *       60 blocks on Eve). Nearer than that the linear fog is thinner than 1.7's (Eve at 20 blocks: 33 % against
 *       63 %).</li>
 *   <li>The sunset fan of a body other than Kerbin is drawn by {@link MunSky} at the body's own sun angle, so vanilla's
 *       (which follows the dimension's vanilla angle) is switched off; vanilla's fog therefore no longer blends that
 *       sunset colour in when looking at the sun.</li>
 *   <li>Earth-air skies off Kerbin (Laythe, Thatmo's level) are 1.7's {@code getSkyColorBody} at the body's angle from the
 *       camera's biome base temperature, without 1.7's height correction of the temperature.</li>
 * </ul>
 * hbm-space's {@code 1.44_hardcoreDarkness} option is its default, off.
 */
public final class CelestialSkyColors {

    /** hbm-space {@code GeneralConfig.enableHardcoreDarkness} (config key {@code 1.44_hardcoreDarkness}), its default. */
    static final boolean HARDCORE_DARKNESS = false;

    private CelestialSkyColors() {}

    static void init() {
        Skies.MODIFIERS.addPhaseOrdering(SpaceSkies.PHASE, dev.golem.core.hook.Hook.DEFAULT_PHASE);
        Skies.MODIFIERS.register(SpaceSkies.PHASE, new Skies.Modifier() {
            @Override
            public boolean active(Level level) {
                return level.isClientSide() && SpaceSkies.body(level) != null;
            }

            @Override
            public Vec3 skyColor(Level level, Vec3 color) {
                return CelestialSkyColors.skyColor(level, color, 0.0F);
            }

            @Override
            public Vec3 fogColor(Level level, Vec3 color) {
                return CelestialSkyColors.fogColor(level, SpaceSkies.angle(level, 0.0F));
            }

            @Override
            public float starBrightness(Level level, float brightness) {
                return SpaceSkies.isKerbin(level) ? brightness : SpaceSkies.starBrightness(level, 0.0F);
            }

            @Override
            public float skyLight(Level level, float factor) {
                return sunBrightness(level, SpaceSkies.isKerbin(level) ? factor : SpaceSkies.sunBrightness(level, 0.0F), 0.0F);
            }

            @Override
            public float[] sunriseColor(Level level, float[] rgba) {
                // Kerbin: vanilla's fan at vanilla's angle, earth air leaves its colours alone. Elsewhere MunSky draws it.
                return SpaceSkies.isKerbin(level) ? rgba : null;
            }
        });
        Fogs.addModifier(new Fogs.Modifier() {
            @Override
            public boolean active(Entity camera) {
                return camera.level().isClientSide() && SpaceSkies.body(camera.level()) != null && fogDensity(camera.level()) > 0;
            }

            @Override
            public float far(Entity camera, float renderDistanceBlocks) {
                return com.ntmspace.world.SkyMath.linearFogEnd(fogDensity(camera.level()));
            }
        });
    }

    // ---------------------------------------------------------------------------------------- 1.7 formulas

    /** {@code WorldProviderCelestial.fogDensity}: {@code p * p * 0.002} above 2 atm, else none (0). */
    static float fogDensity(Level level) {
        // WorldProviderLaythe.fogDensity: Laythe's clear water, 0.03 with the camera in water
        if (level.dimension().equals(SpaceDimensions.LAYTHE)) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera != null && camera.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) return com.ntmspace.world.SkyMath.LAYTHE_WATER_FOG_DENSITY;
        }
        // WorldProviderDuna.fogDensity: a dust storm of 0.25 or more, by the square of its eased strength
        if (level.dimension().equals(SpaceDimensions.DUNA)) {
            float storm = com.ntmspace.world.SkyMath.dustStormFogDensity(SkyWeatherClient.dustStorm());
            if (storm >= 0.0F) return storm;
        }
        String air = SpaceSkies.airBody(level);
        if (air == null || !Atmospheres.has(air)) return 0;
        return com.ntmspace.world.SkyMath.fogDensity((float) Atmospheres.pressure(air));
    }

    /**
     * {@code WorldProviderCelestial.getSkyColor} (with {@code WorldProviderEve}'s lightning flash), then
     * {@code SkyProviderCelestial}'s thinning of the sky colour above y 300. {@code vanilla} is vanilla's sky colour,
     * which Kerbin's earth air keeps (1.7 {@code super.getSkyColor}).
     */
    static Vec3 skyColor(Level level, Vec3 vanilla, float partialTicks) {
        String air = SpaceSkies.airBody(level);
        // The cold hard vacuum of space
        if (air == null || !Atmospheres.has(air)) return Vec3.ZERO;

        // 1.7 getSunBrightnessFactor; Kerbin's air is earth air only, which keeps vanilla's colour and never reads it
        float sun = SpaceSkies.isKerbin(level) ? 1.0F : SpaceSkies.daylight(level, partialTicks);
        float totalPressure = (float) Atmospheres.pressure(air);
        double r = 0, g = 0, b = 0;
        for (Atmospheres.Entry entry : Atmospheres.of(air)) {
            double fr, fg, fb;
            String fluid = entry.fluid();
            if (fluid.equals(Atmospheres.EVEAIR)) {
                fr = 53F / 255F * sun; fg = 32F / 255F * sun; fb = 74F / 255F * sun;
            } else if (fluid.equals(Atmospheres.DUNAAIR) || fluid.equals(Atmospheres.CARBONDIOXIDE)) {
                fr = 212F / 255F * sun; fg = 112F / 255F * sun; fb = 78F / 255F * sun;
            } else if (fluid.equals(Atmospheres.EARTHAIR) || fluid.equals(Atmospheres.OXYGEN) || fluid.equals(Atmospheres.NITROGEN)) {
                // Default to regular ol' overworld
                Vec3 earth = SpaceSkies.isKerbin(level) ? vanilla : skyColorBody(level, partialTicks);
                fr = earth.x; fg = earth.y; fb = earth.z;
            } else {
                int hex = Atmospheres.color(fluid);
                fr = ((hex >> 16) & 0xFF) / 255.0F * sun;
                fg = ((hex >> 8) & 0xFF) / 255.0F * sun;
                fb = (hex & 0xFF) / 255.0F * sun;
            }
            float percentage = (float) entry.pressure() / totalPressure;
            r += fr * percentage;
            g += fg * percentage;
            b += fb * percentage;
        }

        // war weapons light up the sky (1.7 CBT_War: (100 - flash) * 0.5 per running impact; see WarSky)
        float war = WarSky.skyFlash(level);
        r += war;
        g += war;
        b += war;

        // Lower pressure sky renders thinner
        float pressureFactor = Mth.clamp(totalPressure, 0.0F, 1.0F);
        r *= pressureFactor;
        g *= pressureFactor;
        b *= pressureFactor;

        double eclipse = SpaceSkies.eclipse(level);
        if (eclipse > 0) {
            r *= 1 - eclipse * 0.6;
            g *= 1 - eclipse * 0.6;
            b *= 1 - eclipse * 0.5;
        }

        if (level.dimension().equals(SpaceDimensions.EVE)) {
            float flash = eveFlash(level, partialTicks);
            r += flash;
            g += flash;
            b += flash;
        }

        // WorldProviderThatmo.getSkyColor: the railgun's flash and the shield's glow (ThatmoSky)
        float thatmo = ThatmoSky.lightAdd(level);
        r += thatmo;
        g += thatmo;
        b += thatmo;

        // SkyProviderCelestial: diminish the sky colour when leaving the atmosphere
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null && camera.getEyeY() > 300) {
            double curvature = Mth.clamp((800.0F - (float) camera.getEyeY()) / 500.0F, 0.0F, 1.0F);
            r *= curvature;
            g *= curvature;
            b *= curvature;
        }
        return new Vec3(r, g, b);
    }

    /** 1.7 {@code World.getSkyColorBody} without weather: the camera biome's {@code getSkyColorByTemp}, by the sun's height. */
    static Vec3 skyColorBody(Level level, float partialTicks) {
        float angle = SpaceSkies.angle(level, partialTicks);
        float f2 = Mth.clamp(Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0F, 1.0F);
        Entity camera = Minecraft.getInstance().getCameraEntity();
        float temperature = camera == null ? 0.5F : level.getBiome(camera.blockPosition()).value().getBaseTemperature();
        int rgb = skyColorByTemp(temperature);
        return new Vec3(((rgb >> 16) & 255) / 255.0F * f2, ((rgb >> 8) & 255) / 255.0F * f2, (rgb & 255) / 255.0F * f2);
    }

    /** 1.7 {@code BiomeGenBase.getSkyColorByTemp}: {@code Color.getHSBColor} of the temperature. */
    static int skyColorByTemp(float temperature) {
        temperature /= 3.0F;
        if (temperature < -1.0F) temperature = -1.0F;
        if (temperature > 1.0F) temperature = 1.0F;
        return java.awt.Color.HSBtoRGB(0.62222224F - temperature * 0.05F, 0.5F + temperature * 0.1F, 1.0F);
    }

    /** {@code WorldProviderCelestial.getFogColor(solarAngle)}, before Tom's dust (HBM applies it after). */
    static Vec3 fogColor(Level level, float solarAngle) {
        String air = SpaceSkies.airBody(level);
        // The cold hard vacuum of space
        if (air == null || !Atmospheres.has(air)) return Vec3.ZERO;

        float sun = Mth.clamp(Mth.cos(solarAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0F, 1.0F);
        float sunR = sun, sunG = sun, sunB = sun;
        if (!HARDCORE_DARKNESS) {
            sunR *= 0.94F;
            sunG *= 0.94F;
            sunB *= 0.91F;
        }

        float totalPressure = (float) Atmospheres.pressure(air);
        double r = 0, g = 0, b = 0;
        for (Atmospheres.Entry entry : Atmospheres.of(air)) {
            double fr, fg, fb;
            String fluid = entry.fluid();
            if (fluid.equals(Atmospheres.EVEAIR)) {
                fr = 53F / 255F * sunR; fg = 32F / 255F * sunG; fb = 74F / 255F * sunB;
            } else if (fluid.equals(Atmospheres.DUNAAIR) || fluid.equals(Atmospheres.CARBONDIOXIDE)) {
                fr = 212F / 255F * sunR; fg = 112F / 255F * sunG; fb = 78F / 255F * sunB;
            } else if (fluid.equals(Atmospheres.EARTHAIR) || fluid.equals(Atmospheres.OXYGEN) || fluid.equals(Atmospheres.NITROGEN)) {
                fr = 0.7529412F * sunR; fg = 0.84705883F * sunG; fb = 1.0F * sunB;
            } else {
                int hex = Atmospheres.color(fluid);
                fr = ((hex >> 16) & 0xFF) / 255.0F * (sunR * 1.4F);
                fg = ((hex >> 8) & 0xFF) / 255.0F * (sunG * 1.4F);
                fb = (hex & 0xFF) / 255.0F * (sunB * 1.4F);
            }
            float percentage = (float) entry.pressure() / totalPressure;
            r += fr * percentage;
            g += fg * percentage;
            b += fb * percentage;
        }

        // Add minimum fog colour, for night-time glow
        if (!HARDCORE_DARKNESS) {
            float nightDensity = Mth.clamp(totalPressure, 0.0F, 1.0F);
            r += 0.06F * nightDensity;
            g += 0.06F * nightDensity;
            b += 0.09F * nightDensity;
        }

        // Fog intensity remains high to simulate a thin looking atmosphere on low pressure planets
        float pressureFactor = Mth.clamp(totalPressure * 10.0F, 0.0F, 1.0F);
        r *= pressureFactor;
        g *= pressureFactor;
        b *= pressureFactor;

        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null && camera.getEyeY() > 600) {
            double curvature = Mth.clamp((1000.0F - (float) camera.getEyeY()) / 400.0F, 0.0F, 1.0F);
            r *= curvature;
            g *= curvature;
            b *= curvature;
        }

        double eclipse = SpaceSkies.eclipse(level);
        if (eclipse > 0) {
            r *= 1 - eclipse * 0.3;
            g *= 1 - eclipse * 0.3;
            b *= 1 - eclipse * 0.3;

            float[] sunsetFog = providerSunset(level, 0.25F);
            if (sunsetFog != null) {
                double sunsetAmount = Mth.clamp(eclipse * 0.5 - (1 - sun), 0.0, 1.0);
                r = r * (1.0F - sunsetAmount) + sunsetFog[0] * sunsetAmount;
                g = g * (1.0F - sunsetAmount) + sunsetFog[1] * sunsetAmount;
                b = b * (1.0F - sunsetAmount) + sunsetFog[2] * sunsetAmount;
            }
        }
        // WorldProviderThatmo.getFogColor: the railgun's flash and the shield's glow (ThatmoSky)
        float thatmo = ThatmoSky.lightAdd(level);
        return new Vec3(r + thatmo, g + thatmo, b + thatmo);
    }

    /** Vanilla 1.7 {@code WorldProvider.calcSunriseSunsetColors} at {@code solarAngle}, or null away from the horizon. */
    static float[] vanillaSunset(float solarAngle) {
        float f2 = 0.4F;
        float f3 = Mth.cos(solarAngle * (float) Math.PI * 2.0F) - 0.0F;
        float f4 = -0.0F;
        if (f3 >= f4 - f2 && f3 <= f4 + f2) {
            float f5 = (f3 - f4) / f2 * 0.5F + 0.5F;
            float f6 = 1.0F - (1.0F - Mth.sin(f5 * (float) Math.PI)) * 0.99F;
            f6 *= f6;
            return new float[] {f5 * 0.3F + 0.7F, f5 * f5 * 0.7F + 0.2F, f5 * f5 * 0.0F + 0.2F, f6};
        }
        return null;
    }

    /**
     * {@code WorldProviderCelestial.calcSunriseSunsetColors}: none below 0.05 atm; carbon dioxide airs swap red and blue
     * (Mars' blue sunsets), Eve's is its own green, chlorine airs swap green and blue. Tom's dust is HBM's.
     */
    static float[] providerSunset(Level level, float solarAngle) {
        String air = SpaceSkies.airBody(level);
        if (air == null || !Atmospheres.has(air) || Atmospheres.pressure(air) < 0.05F) return null;
        float[] colors = vanillaSunset(solarAngle);
        if (colors == null) return null;

        if (Atmospheres.hasFluid(air, Atmospheres.DUNAAIR) || Atmospheres.hasFluid(air, Atmospheres.CARBONDIOXIDE)) {
            float tmp = colors[0];
            colors[0] = colors[2];
            colors[2] = tmp;
        } else if (Atmospheres.hasFluid(air, Atmospheres.EVEAIR)) {
            float f2 = 0.4F;
            float f3 = Mth.cos(solarAngle * (float) Math.PI * 2.0F) - 0.0F;
            float f4 = -0.0F;
            if (f3 >= f4 - f2 && f3 <= f4 + f2) {
                float f5 = (f3 - f4) / f2 * 0.5F + 0.5F;
                float f6 = 1.0F - (1.0F - Mth.sin(f5 * (float) Math.PI)) * 0.99F;
                f6 *= f6;
                colors[0] = f5 * 0.01F;
                colors[1] = f5 * f5 * 0.9F + 0.3F;
                colors[2] = f5 * f5;
                colors[3] = f6;
            }
        } else if (Atmospheres.hasFluid(air, Atmospheres.TEKTOAIR) || Atmospheres.hasFluid(air, Atmospheres.JOOLGAS)
                || Atmospheres.hasFluid(air, Atmospheres.CHLORINE)) {
            float tmp = colors[1];
            colors[1] = colors[2];
            colors[2] = tmp;
        }
        return colors;
    }

    /**
     * {@code SkyProviderCelestial.calcSunriseSunsetColors}: an airless body's faint white sunrise glow (which 1.7 kept out
     * of the fog), else the provider's.
     */
    static float[] skySunset(Level level, float solarAngle) {
        String air = SpaceSkies.airBody(level);
        float pressure = air == null ? 0 : (float) Atmospheres.pressure(air);
        if (pressure < 0.05F) {
            float cutoff = 0.4F;
            float angle = Mth.cos(solarAngle * (float) Math.PI * 2.0F) - 0.0F;
            if (angle < -cutoff || angle > cutoff) return null;
            float colorIntensity = angle / cutoff * 0.5F + 0.5F;
            float alpha = 1.0F - (1.0F - Mth.sin(colorIntensity * (float) Math.PI)) * 0.99F;
            alpha *= alpha;
            return new float[] {0.9F, 1.0F, 1.0F, alpha * 0.2F};
        }
        return providerSunset(level, solarAngle);
    }

    /**
     * {@code WorldProviderCelestial.getSunBrightness} (the light map's sky light, 0.2 at night): {@code base} is 1.7's
     * {@code World.getSunBrightness}; less during an eclipse, less in air thicker than 1.5 atm; Tekto's 70 %, Thatmo's
     * level 10 %, Eve's lightning adds. A destroyed sun ({@code CBT_Destroyed} on the star) gives none.
     */
    static float sunBrightness(Level level, float base, float partialTicks) {
        float sunBrightness = 0;
        // a destroyed star gives no light at all (1.7 returned 0 before anything else; Eve's flash still adds)
        com.ntmspace.world.SolarSystem.Body body = SpaceSkies.body(level);
        if (body == null || !CelestialSkyObjects.starDestroyed(body)) {
            sunBrightness = base;
            sunBrightness *= (float) (1 - SpaceSkies.eclipse(level) * 0.6);
            String air = SpaceSkies.airBody(level);
            if (air != null && Atmospheres.has(air)) {
                sunBrightness *= Mth.clamp(1.0F - ((float) Atmospheres.pressure(air) - 1.5F) * 0.2F, 0.25F, 1.0F);
            }
            // brightness _inside_ of the atmosphere, from effects like lightning or war weapons (1.7; WarSky)
            sunBrightness += WarSky.insideBrightness(level);
        }
        if (level.dimension().equals(SpaceDimensions.TEKTO)) sunBrightness *= 0.7F;
        // WorldProviderThatmo.getSunBrightness: a tenth, plus the railgun's flash and the shield's glow (ThatmoSky)
        if (level.dimension().equals(SpaceDimensions.THATMO)) sunBrightness = sunBrightness * 0.1F + ThatmoSky.lightAdd(level);
        if (level.dimension().equals(SpaceDimensions.EVE)) sunBrightness += eveFlash(level, partialTicks) * 0.7F;
        return sunBrightness;
    }

    /**
     * {@code WorldProviderCelestial.getSunColor}: chlorine airs redden the sun by absorbing green and blue (Tekto's red
     * sun).
     */
    static float[] sunColor(Level level) {
        float[] sunColor = {1.0F, 1.0F, 1.0F};
        String air = SpaceSkies.airBody(level);
        if (air == null) return sunColor;
        for (Atmospheres.Entry entry : Atmospheres.of(air)) {
            String fluid = entry.fluid();
            if (fluid.equals(Atmospheres.TEKTOAIR) || fluid.equals(Atmospheres.CHLORINE) || fluid.equals(Atmospheres.CHLOROMETHANE)
                    || fluid.equals(Atmospheres.RADIOSOLVENT) || fluid.equals(Atmospheres.CCL)) {
                float absorption = Mth.clamp(1.0F - (float) entry.pressure() * 0.5F, 0.0F, 1.0F);
                sunColor[1] *= absorption;
                sunColor[2] *= absorption;
            }
        }
        return sunColor;
    }

    // ---------------------------------------------------------------------------------------- Eve's lightning

    private static final float EVE_STRIKE_WINDOW = 8.0F;
    private static final float EVE_STRIKE_FADE_TIME = 5.0F;
    private static final float EVE_STRIKE_DELAY_RANGE = 2.4F;
    private static final float EVE_STRIKE_GATE_THRESHOLD = 0.82F;
    private static final double EVE_HASH_MULTIPLIER = 43758.5453123D;

    /** {@code AtmosphereRenderUtil.getBodyEveFlashStrength} at {@code WorldProviderEve.getAtmosphereTime}. */
    static float eveFlash(Level level, float partialTicks) {
        float atmosphereTime = ((float) level.getGameTime() + partialTicks) / 20.0F;
        float eventIndex = Mth.floor(atmosphereTime / EVE_STRIKE_WINDOW);
        float eventTime = atmosphereTime - eventIndex * EVE_STRIKE_WINDOW;
        float strikeDelay = eveHash(eventIndex, 41.9F) * EVE_STRIKE_DELAY_RANGE;
        if (eventTime < strikeDelay) return 0.0F;
        if (eveHash(eventIndex, 313.7F) < EVE_STRIKE_GATE_THRESHOLD) return 0.0F;
        float t = Math.max(eventTime - strikeDelay, 0.0F);
        float fade = smoothstep(0.0F, 0.14F, t) * (1.0F - smoothstep(0.16F, EVE_STRIKE_FADE_TIME, t));
        return Mth.clamp(fade, 0.0F, 1.0F);
    }

    private static float eveHash(float x, float y) {
        double value = Math.sin(x * 127.1D + y * 311.7D) * EVE_HASH_MULTIPLIER;
        return (float) (value - Math.floor(value));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) return value < edge0 ? 0.0F : 1.0F;
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** For the sky renderer: the fog's share of the planets' tint ({@code ModEventHandlerRenderer.lastFogDensity * 30}). */
    static float fogIntensity(Level level) {
        return fogDensity(level) * 30;
    }
}
