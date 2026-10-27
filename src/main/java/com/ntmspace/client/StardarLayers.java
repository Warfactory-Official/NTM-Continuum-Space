package com.ntmspace.client;

import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CBT_Impact;
import com.ntmspace.trait.CBT_Lights;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.NukeShocks;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.StardarMath;
import dev.golem.core.render.GuiCanvas;
import dev.golem.core.render.Sky;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.phys.Vec3;

/**
 * The planet layers of hbm-space's StarDar map ({@code GUIMachineStardar.drawBody} / {@code drawBodyOverlays}), drawn on
 * a Golem {@link GuiCanvas} in 1.7's order and blends:
 *
 * <ol>
 *   <li>before the body's disc, the atmosphere glow band ({@code AtmosphereRenderUtil.renderAtmosphereGlow2D}: gradient
 *       quads, alpha blended; {@link StardarMath#glowBand});</li>
 *   <li>after it: the atmosphere surface ({@code atmosphere.frag}: clouds, storm darkening, haze, gas bands; alpha),
 *       the crescent shadow ({@code crescent.frag}; alpha), the emissive air glow over city lights
 *       ({@code atmosphere_emissive.frag}), the city lights ({@code nightlights.frag}), lightning in the clouds
 *       ({@code lightning.frag}), the impact's lava ({@code impact.png}, fading in over 10 ticks) and the nuke flashes
 *       and rings ({@code nuke.frag}), all additive.</li>
 * </ol>
 *
 * Every shader layer samples the body's own texture as {@code bodyTex} with {@code useBodyAlphaMask} 1 (the sky draws
 * them without the mask), so the emissive and night-light layers bind three textures and have their own files
 * ({@code stardar_*.fsh}); the atmosphere, lightning and nuke layers are the sky's files. Parameters are
 * {@link PlanetShaders}' layout, with 1.7's StarDar values: the times are the world's total time plus the partial tick
 * ({@code dayTicks}), the weather's partial tick is {@code dayTicks}' fraction, and an impact's time counts from
 * {@code dayTicks}.
 */
public final class StardarLayers {

    private StardarLayers() {}

    public static final Sky.Shader CRESCENT = Sky.Shader.of(NtmSpace.MOD_ID, "stardar_crescent", 1);
    public static final Sky.Shader EMISSIVE = Sky.Shader.of(NtmSpace.MOD_ID, "stardar_atmosphere_emissive", 3);
    public static final Sky.Shader NIGHT_LIGHTS = Sky.Shader.of(NtmSpace.MOD_ID, "stardar_nightlights", 3);

    /** Every StarDar shader layer in drawing order, for the harness query. */
    public static final List<Sky.Shader> ALL = List.of(PlanetShaders.ATMOSPHERE, CRESCENT, EMISSIVE, NIGHT_LIGHTS,
            PlanetShaders.LIGHTNING, PlanetShaders.NUKE);

    private static final Sky.Texture IMPACT = Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/impact.png");

    private static final AtomicLong GLOWS = new AtomicLong();

    /** Client start-up: the harness query {@code ntmspace_stardar} ({@link #draws}). */
    public static void init() {
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmspace_stardar", (mc, params) -> draws());
    }

    private static final AtomicLong LAVA = new AtomicLong();

    /** {@code renderAtmosphereGlow2D(.., visibility 1)} around a body square of {@code size} at x, y. */
    public static void glow(GuiCanvas canvas, SolarSystem.Body body, float x, float y, float size) {
        Vec3 atmo = AtmosphereRender.atmosphereColor(body);
        float[] band = StardarMath.glowBand(x, y, size, CelestialSkyObjects.glowAlpha(body), atmo.x, atmo.y, atmo.z);
        if (band == null) return;
        canvas.coloredQuads(Sky.Blend.ALPHA, band);
        GLOWS.incrementAndGet();
    }

    /**
     * {@code drawBodyOverlays}: every layer over the disc already drawn at x, y ({@code rotateBody}: turned by
     * {@code rotationAngle} degrees about its centre, else drawn with {@code textureUOffset}); {@code phase} is the
     * crescent phase, {@code dayTicks} the world's total time plus the partial tick.
     */
    public static void overlays(GuiCanvas canvas, SolarSystem.Body body, float x, float y, float size, boolean rotateBody,
                                float rotationAngle, double dayTicks, float textureUOffset, float phase) {
        CBT_Impact impact = CelestialTraits.getTrait(body.name, CBT_Impact.class, true);
        CBT_Lights light = CelestialTraits.getTrait(body.name, CBT_Lights.class, true);
        List<NukeShocks.Shock> nukeShocks = NukeShocks.clientShocks(body.name, (long) dayTicks);
        double impactTime = impact != null ? dayTicks - impact.time : 0.0D;
        float impactAnimationTime = impact != null ? (float) impactTime : -1.0F;
        int lightIntensity = StardarMath.lightIntensity(light != null, light != null ? light.getIntensity() : 0, impactTime,
                PlanetShaders.CITY_LIGHT_LEVELS);
        float atmosphereAlpha = AtmosphereRender.density(body);
        float atmosphereDensity = AtmosphereRender.density(body);
        Vec3 atmosphereColor = AtmosphereRender.atmosphereColor(body);
        Vec3 cloudColor = AtmosphereRender.cloudColor(body);
        float weatherPartialTicks = (float) (dayTicks - Math.floor(dayTicks));
        float cloudLightningStrength = AtmosphereRender.cloudLightningStrength(body, weatherPartialTicks);
        int atmosphereStyle = AtmosphereRender.style(body);
        float atmosphereTime = (float) (dayTicks / 20.0D);
        float patternOffset = StardarMath.patternOffset(rotateBody, textureUOffset, body.getClientRotationalPeriod(), dayTicks);

        float[] p = new float[Sky.SHADER_PARAMS];
        p[PlanetShaders.P_PHASE] = phase;
        p[PlanetShaders.P_OFFSET] = textureUOffset;
        p[PlanetShaders.P_PATTERN_OFFSET] = patternOffset;
        p[PlanetShaders.P_ATMOSPHERE_COLOR] = (float) atmosphereColor.x;
        p[PlanetShaders.P_ATMOSPHERE_COLOR + 1] = (float) atmosphereColor.y;
        p[PlanetShaders.P_ATMOSPHERE_COLOR + 2] = (float) atmosphereColor.z;
        p[PlanetShaders.P_CLOUD_COLOR] = (float) cloudColor.x;
        p[PlanetShaders.P_CLOUD_COLOR + 1] = (float) cloudColor.y;
        p[PlanetShaders.P_CLOUD_COLOR + 2] = (float) cloudColor.z;
        p[PlanetShaders.P_CLOUD_TINT] = AtmosphereRender.cloudTintStrength(body);
        p[PlanetShaders.P_STORM_DARKNESS] = AtmosphereRender.cloudStormDarkness(body, weatherPartialTicks);
        p[PlanetShaders.P_ATMOSPHERE_ALPHA] = atmosphereAlpha;
        p[PlanetShaders.P_ATMOSPHERE_TIME] = atmosphereTime;
        p[PlanetShaders.P_STYLE] = atmosphereStyle;
        p[PlanetShaders.P_IMPACT_TIME] = impactAnimationTime;
        p[PlanetShaders.P_DENSITY] = atmosphereDensity;
        p[PlanetShaders.P_LIGHTNING_STRENGTH] = cloudLightningStrength;
        p[PlanetShaders.P_EVE_FLASH] = AtmosphereRender.eveFlashStrength(body, atmosphereTime);
        p[PlanetShaders.P_LIGHTNING_MODE] = AtmosphereRender.lightningMode(body);
        p[PlanetShaders.P_BLACKOUTS] = StardarMath.blackouts(impactTime);
        p[PlanetShaders.P_BODY_ALPHA_MASK] = 1;
        AtmosphereRender.nukeShockParams(p, PlanetShaders.P_NUKE, nukeShocks, dayTicks);

        Sky.Texture surface = Sky.Texture.of(NtmSpace.MOD_ID, body.texture());
        Sky.Texture mask = PlanetShaders.cityMask(body.name);
        boolean cloudsOrHaze = atmosphereStyle == AtmosphereRender.ATMOSPHERE_STYLE_CLOUDS
                || atmosphereStyle == AtmosphereRender.ATMOSPHERE_STYLE_HAZE;

        if (atmosphereAlpha > 0.001F) {
            layer(canvas, PlanetShaders.ATMOSPHERE, Sky.Blend.ALPHA, p, new Sky.Texture[] {surface}, x, y, size, rotateBody, rotationAngle);
        }
        layer(canvas, CRESCENT, Sky.Blend.ALPHA, p, new Sky.Texture[] {surface}, x, y, size, rotateBody, rotationAngle);
        if (lightIntensity > 0 && atmosphereDensity > 0.001F && cloudsOrHaze) {
            layer(canvas, EMISSIVE, Sky.Blend.ADDITIVE, p, new Sky.Texture[] {surface, PlanetShaders.cityLights(lightIntensity), mask},
                    x, y, size, rotateBody, rotationAngle);
        }
        if (lightIntensity > 0) {
            layer(canvas, NIGHT_LIGHTS, Sky.Blend.ADDITIVE, p, new Sky.Texture[] {surface, PlanetShaders.cityLights(lightIntensity), mask},
                    x, y, size, rotateBody, rotationAngle);
        }
        if (atmosphereAlpha > 0.001F && cloudLightningStrength > 0.001F && cloudsOrHaze) {
            layer(canvas, PlanetShaders.LIGHTNING, Sky.Blend.ADDITIVE, p, new Sky.Texture[] {surface, mask},
                    x, y, size, rotateBody, rotationAngle);
        }
        if (impact != null) {
            float lavaAlpha = StardarMath.lavaAlpha(impactTime);
            if (lavaAlpha > 0F) {
                // glColor4f(1, 1, 1, lavaAlpha), GL_SRC_ALPHA / GL_ONE; with the body's offset when it is not turned
                if (rotateBody) {
                    canvas.pose().pushPose();
                    canvas.pose().translate(x, y, 0F);
                    canvas.pose().mulPose(Axis.ZP.rotationDegrees(rotationAngle));
                    canvas.texturedQuads(IMPACT, Sky.Blend.ADDITIVE, 1F, 1F, 1F, lavaAlpha, StardarMath.texturedQuad(0F, 0F, size, 0F));
                    canvas.pose().popPose();
                } else {
                    canvas.texturedQuads(IMPACT, Sky.Blend.ADDITIVE, 1F, 1F, 1F, lavaAlpha, StardarMath.texturedQuad(x, y, size, textureUOffset));
                }
                LAVA.incrementAndGet();
            }
        }
        if (!nukeShocks.isEmpty()) {
            layer(canvas, PlanetShaders.NUKE, Sky.Blend.ADDITIVE, p, new Sky.Texture[0], x, y, size, rotateBody, rotationAngle);
        }
    }

    /** One shader layer on the body square: {@code drawTexturedQuad(x, y, size, 0)}, or turned about its centre. */
    private static void layer(GuiCanvas canvas, Sky.Shader shader, Sky.Blend blend, float[] params, Sky.Texture[] textures,
                              float x, float y, float size, boolean rotateBody, float rotationAngle) {
        if (rotateBody) {
            canvas.pose().pushPose();
            canvas.pose().translate(x, y, 0F);
            canvas.pose().mulPose(Axis.ZP.rotationDegrees(rotationAngle));
            canvas.shaderQuads(shader, blend, params, textures, StardarMath.texturedQuad(0F, 0F, size, 0F));
            canvas.pose().popPose();
        } else {
            canvas.shaderQuads(shader, blend, params, textures, StardarMath.texturedQuad(x, y, size, 0F));
        }
    }

    /**
     * Harness: the StarDar layers drawn on this client - each shader's GUI draws (by shader id), the glow bands and the
     * lava overlays, and the canvases Golem drew.
     */
    public static com.google.gson.JsonObject draws() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        com.google.gson.JsonObject shaders = new com.google.gson.JsonObject();
        for (Sky.Shader shader : ALL) shaders.addProperty(shader.id(), GuiCanvas.shaderDraws(shader));
        json.add("shaders", shaders);
        json.addProperty("glow", GLOWS.get());
        json.addProperty("lava", LAVA.get());
        json.addProperty("canvases", GuiCanvas.canvasesDrawn());
        return json;
    }
}
