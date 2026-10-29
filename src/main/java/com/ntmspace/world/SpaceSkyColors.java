package com.ntmspace.world;

import java.awt.Color;
import com.hbm.handler.ImpactSky;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CelestialTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** WorldProviderCelestial's live colour and illumination formulas, shared by rendering and level queries. */
public final class SpaceSkyColors {

    public static final boolean HARDCORE_DARKNESS = false;

    private SpaceSkyColors() {}

    /**
     * {@code WorldProviderCelestial.getSkyColor} (with {@code WorldProviderEve}'s lightning flash), then
     * {@code SkyProviderCelestial}'s thinning of the sky colour above y 300. {@code vanilla} is vanilla's sky colour,
     * which Kerbin's earth air keeps (1.7 {@code super.getSkyColor}).
     */
    public static Vec3 skyColor(Level level, Vec3 vanilla, float partialTicks, Vec3 position) {
        String air = SpaceSkies.airBody(level);
        // The cold hard vacuum of space
        if (air == null || !Atmospheres.has(air, level.isClientSide())) return Vec3.ZERO;

        // Colored atmospheres follow daylight on Kerbin too; the earth-air branch keeps vanilla's lit color.
        float sun = SpaceSkies.daylight(level, partialTicks);
        float totalPressure = (float) Atmospheres.pressure(air, level.isClientSide());
        if (totalPressure <= 0.0F) return Vec3.ZERO;
        double r = 0, g = 0, b = 0;
        for (Atmospheres.Entry entry : Atmospheres.of(air, level.isClientSide())) {
            double fr, fg, fb;
            String fluid = entry.fluid();
            if (fluid.equals(Atmospheres.EVEAIR)) {
                fr = 53F / 255F * sun; fg = 32F / 255F * sun; fb = 74F / 255F * sun;
            } else if (fluid.equals(Atmospheres.DUNAAIR) || fluid.equals(Atmospheres.CARBONDIOXIDE)) {
                fr = 212F / 255F * sun; fg = 112F / 255F * sun; fb = 78F / 255F * sun;
            } else if (fluid.equals(Atmospheres.EARTHAIR) || fluid.equals(Atmospheres.OXYGEN) || fluid.equals(Atmospheres.NITROGEN)) {
                // Default to regular ol' overworld
                Vec3 earth = SpaceSkies.isKerbin(level) ? vanilla : skyColorBody(level, partialTicks, position);
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
        float war = SkyMath.insideBrightness(CelestialTraits.getTrait(level, CBT_War.class)) * 0.5F;
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

        // WorldProviderCelestial.getSkyColor: Space's dust removes blue four times as quickly, even without fire.
        var dust = ImpactSky.dust(level);
        var fire = ImpactSky.fire(level);
        if (dust > 0.0F) {
            if (fire > 0.0F) {
                r *= 1.3D;
                g *= Math.max(1 - dust * 1.4F, 0);
                b *= Math.max(1 - dust * 4, 0);
            } else {
                g *= 1 - dust * 0.5F;
                b *= Math.max(1 - dust * 4, 0);
            }
            var strength = fire + (1 - dust);
            r *= strength;
            g *= strength;
            b *= strength;
        }

        if (level.dimension().equals(SpaceDimensions.EVE)) {
            float flash = eveFlash(level, partialTicks);
            r = SkyMath.eveSkyChannel(r, flash);
            g = SkyMath.eveSkyChannel(g, flash);
            b = SkyMath.eveSkyChannel(b, flash);
        }

        // WorldProviderThatmo.getSkyColor: the railgun's flash and the shield's glow (ThatmoSky)
        float thatmo = ThatmoBattlefield.lightAdd(level);
        r += thatmo;
        g += thatmo;
        b += thatmo;

        // SkyProviderCelestial: diminish the sky colour when leaving the atmosphere
        if (position.y > 300) {
            double curvature = Mth.clamp((800.0F - (float) position.y) / 500.0F, 0.0F, 1.0F);
            r *= curvature;
            g *= curvature;
            b *= curvature;
        }
        return new Vec3(r, g, b);
    }

    /** 1.7 {@code World.getSkyColorBody} without weather: the camera biome's {@code getSkyColorByTemp}, by the sun's height. */
    public static Vec3 skyColorBody(Level level, float partialTicks, Vec3 position) {
        float angle = SpaceSkies.angle(level, partialTicks);
        float f2 = Mth.clamp(Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0F, 1.0F);
        var temperature = level.getBiome(BlockPos.containing(position)).value().getBaseTemperature();
        int rgb = skyColorByTemp(temperature);
        return new Vec3(((rgb >> 16) & 255) / 255.0F * f2, ((rgb >> 8) & 255) / 255.0F * f2, (rgb & 255) / 255.0F * f2);
    }

    /** 1.7 {@code BiomeGenBase.getSkyColorByTemp}: {@code Color.getHSBColor} of the temperature. */
    public static int skyColorByTemp(float temperature) {
        temperature /= 3.0F;
        if (temperature < -1.0F) temperature = -1.0F;
        if (temperature > 1.0F) temperature = 1.0F;
        return Color.HSBtoRGB(0.62222224F - temperature * 0.05F, 0.5F + temperature * 0.1F, 1.0F);
    }

    /** {@code WorldProviderCelestial.getFogColor(solarAngle)}, before the shared impact-fog modifier. */
    public static Vec3 fogColor(Level level, float solarAngle, Vec3 position) {
        String air = SpaceSkies.airBody(level);
        // The cold hard vacuum of space
        if (air == null || !Atmospheres.has(air, level.isClientSide())) return Vec3.ZERO;

        float sun = Mth.clamp(Mth.cos(solarAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0F, 1.0F);
        float sunR = sun, sunG = sun, sunB = sun;
        if (!HARDCORE_DARKNESS) {
            sunR *= 0.94F;
            sunG *= 0.94F;
            sunB *= 0.91F;
        }

        float totalPressure = (float) Atmospheres.pressure(air, level.isClientSide());
        if (totalPressure <= 0.0F) return Vec3.ZERO;
        double r = 0, g = 0, b = 0;
        for (Atmospheres.Entry entry : Atmospheres.of(air, level.isClientSide())) {
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

        if (position.y > 600) {
            double curvature = Mth.clamp((1000.0F - (float) position.y) / 400.0F, 0.0F, 1.0F);
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
        float thatmo = ThatmoBattlefield.lightAdd(level);
        return new Vec3(r + thatmo, g + thatmo, b + thatmo);
    }

    /** Vanilla 1.7 {@code WorldProvider.calcSunriseSunsetColors} at {@code solarAngle}, or null away from the horizon. */
    public static float[] vanillaSunset(float solarAngle) {
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
    public static float[] providerSunset(Level level, float solarAngle) {
        String air = SpaceSkies.airBody(level);
        if (air == null || !Atmospheres.has(air, level.isClientSide()) || Atmospheres.pressure(air, level.isClientSide()) < 0.05F) return null;
        float[] colors = vanillaSunset(solarAngle);
        if (colors == null) return null;

        if (Atmospheres.hasFluid(air, Atmospheres.DUNAAIR, level.isClientSide()) || Atmospheres.hasFluid(air, Atmospheres.CARBONDIOXIDE, level.isClientSide())) {
            float tmp = colors[0];
            colors[0] = colors[2];
            colors[2] = tmp;
        } else if (Atmospheres.hasFluid(air, Atmospheres.EVEAIR, level.isClientSide())) {
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
        } else if (Atmospheres.hasFluid(air, Atmospheres.TEKTOAIR, level.isClientSide()) || Atmospheres.hasFluid(air, Atmospheres.JOOLGAS, level.isClientSide())
                || Atmospheres.hasFluid(air, Atmospheres.CHLORINE, level.isClientSide())) {
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
    public static float[] skySunset(Level level, float solarAngle) {
        String air = SpaceSkies.airBody(level);
        float pressure = air == null ? 0 : (float) Atmospheres.pressure(air, level.isClientSide());
        if (pressure < 0.05F) return chargedDustSunset(solarAngle);
        return providerSunset(level, solarAngle);
    }

    /** {@code SkyProviderCelestial.calcSunriseSunsetColors}'s airless glow, shared by its fan and charged dust. */
    public static float[] chargedDustSunset(float solarAngle) {
        float cutoff = 0.4F;
        float angle = Mth.cos(solarAngle * (float) Math.PI * 2.0F) - 0.0F;
        if (angle < -cutoff || angle > cutoff) return null;
        float colorIntensity = angle / cutoff * 0.5F + 0.5F;
        float alpha = 1.0F - (1.0F - Mth.sin(colorIntensity * (float) Math.PI)) * 0.99F;
        alpha *= alpha;
        return new float[]{0.9F, 1.0F, 1.0F, alpha * 0.2F};
    }

    /**
     * {@code WorldProviderCelestial.getSunBrightness} (the light map's sky light, 0.2 at night): {@code base} is 1.7's
     * {@code World.getSunBrightness}; less during an eclipse, less in air thicker than 1.5 atm; Tekto's 70 %, Thatmo's
     * level 10 %, Eve's lightning adds. A destroyed sun ({@code CBT_Destroyed} on the star) gives none.
     */
    public static float sunBrightness(Level level, float base, float partialTicks) {
        float sunBrightness = 0;
        // a destroyed star gives no light at all (1.7 returned 0 before anything else; Eve's flash still adds)
        SolarSystem.Body body = SpaceSkies.body(level);
        if (body == null || !SkyMath.starDestroyed(body, level.isClientSide())) {
            var air = SpaceSkies.airBody(level);
            var hasAtmosphere = air != null && Atmospheres.has(air, level.isClientSide());
            sunBrightness = SkyMath.sunBrightness(base, SpaceSkies.eclipse(level), hasAtmosphere,
                    hasAtmosphere ? (float) Atmospheres.pressure(air, level.isClientSide()) : 0.0F,
                    SkyMath.insideBrightness(CelestialTraits.getTrait(level, CBT_War.class)), ImpactSky.dust(level));
        }
        if (level.dimension().equals(SpaceDimensions.TEKTO)) sunBrightness *= 0.7F;
        // WorldProviderThatmo.getSunBrightness: a tenth, plus the railgun's flash and the shield's glow (ThatmoSky)
        if (level.dimension().equals(SpaceDimensions.THATMO)) sunBrightness = sunBrightness * SkyMath.THATMO_SUN_MULTIPLIER + ThatmoBattlefield.lightAdd(level);
        if (level.dimension().equals(SpaceDimensions.EVE)) sunBrightness = SkyMath.eveSunBrightness(sunBrightness, eveFlash(level, partialTicks));
        return sunBrightness;
    }

    /**
     * {@code WorldProviderCelestial.getSunColor}: chlorine airs redden the sun by absorbing green and blue (Tekto's red
     * sun).
     */
    public static float[] sunColor(Level level) {
        float[] sunColor = {1.0F, 1.0F, 1.0F};
        String air = SpaceSkies.airBody(level);
        if (air == null) return sunColor;
        for (Atmospheres.Entry entry : Atmospheres.of(air, level.isClientSide())) {
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

    // Eve's lightning

    /** The same live body gate and authored flash curve exposed by the common sky rules. */
    public static float eveFlash(Level level, float partialTicks) {
        return SkyMath.eveFlashStrength(SpaceSkies.body(level), ((float) level.getGameTime() + partialTicks) / 20.0F);
    }

    /** The same base colour and tint used by the actual cloud-layer renderer. */
    public static Vec3 cloudColor(Level level, float partialTick) {
        return CloudLayers17.color(level, partialTick);
    }

    public static int cloudColor(Level level, int vanilla) {
        var body = SpaceSkies.airBody(level);
        var air = Atmospheres.of(body, level.isClientSide());
        if (CloudLayers17.layerCount(air) == 0) return vanilla & 0xFFFFFF;
        var color = cloudColor(level, 0.0F);
        return (vanilla & 0xFF000000) | Math.round((float) color.x * 255.0F) << 16
                | Math.round((float) color.y * 255.0F) << 8 | Math.round((float) color.z * 255.0F);
    }
}
