package com.ntmspace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CBT_Impact;
import com.ntmspace.trait.CBT_Lights;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.NukeShocks;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.render.Sky;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's shader-drawn sky layers ({@code SkyProviderCelestial}), through Golem's {@link Sky#shaderQuads} with the
 * 1.7 GLSL ported verbatim ({@code assets/ntmspace/shaders/core/sky_*.fsh}):
 *
 * <ul>
 *   <li>On a body seen in the sky, in 1.7's order after its disc: the atmosphere surface ({@code atmosphere.frag}: clouds,
 *       storm darkening, haze, a gas giant's bands; alpha blended), then after the crescent the emissive air glow over
 *       city lights ({@code atmosphere_emissive.frag}), the city lights of its night side ({@code nightlights.frag},
 *       {@code CBT_Lights}), lightning flashes in its clouds ({@code lightning.frag}; Eve's own flash schedule), all
 *       additive; after the front half of its rings the nuke flashes and rings ({@code nuke.frag}).</li>
 *   <li>A destroyed sun ({@code CBT_Destroyed} on the star): the black hole ({@code blackhole.frag}) on a quad three times
 *       the sun's size.</li>
 *   <li>An infected body ({@code CBT_COMPROMISED}, trait {@code infected}): the flesh sphere ({@code fle.frag}) over the
 *       whole sky, 1.7's {@code sphere_v2} model scaled 194.5 x 70.5 x 94.5.</li>
 * </ul>
 *
 * Parameters (the shaders' {@code PARAM(i)}, in place of 1.7's uniforms): 0 phase, 1 offset, 2 patternOffset, 3-5
 * atmosphereColor, 6-8 cloudColor, 9 cloudTintStrength, 10 cloudStormDarkness, 11 atmosphereAlpha, 12 atmosphereTime, 13
 * atmosphereStyle, 14 impactTime, 15 atmosphereDensity, 16 cloudLightningStrength, 17 eveFlashStrength, 18 lightningMode,
 * 19 blackouts, 20 useBodyAlphaMask, 21 nukeShockCount, 22-25 nukeShockTime, 26-29 nukeShockCenterX, 30-33
 * nukeShockCenterY, 34-37 nukeShockStrength; the black hole and the flesh read {@code iTime} at 0.
 */
public final class PlanetShaders {

    private PlanetShaders() {}

    public static final Sky.Shader ATMOSPHERE = Sky.Shader.of(NtmSpace.MOD_ID, "sky_atmosphere", 1);
    public static final Sky.Shader EMISSIVE = Sky.Shader.of(NtmSpace.MOD_ID, "sky_atmosphere_emissive", 2);
    public static final Sky.Shader NIGHT_LIGHTS = Sky.Shader.of(NtmSpace.MOD_ID, "sky_nightlights", 2);
    public static final Sky.Shader LIGHTNING = Sky.Shader.of(NtmSpace.MOD_ID, "sky_lightning", 2);
    public static final Sky.Shader NUKE = Sky.Shader.of(NtmSpace.MOD_ID, "sky_nuke", 0);
    public static final Sky.Shader BLACK_HOLE = Sky.Shader.of(NtmSpace.MOD_ID, "sky_blackhole", 1);
    public static final Sky.Shader FLESH = Sky.Shader.of(NtmSpace.MOD_ID, "sky_flesh", 1);

    /** Every shader, for the harness query. */
    public static final List<Sky.Shader> ALL = List.of(ATMOSPHERE, EMISSIVE, NIGHT_LIGHTS, LIGHTNING, NUKE, BLACK_HOLE, FLESH);

    static final int P_PHASE = 0, P_OFFSET = 1, P_PATTERN_OFFSET = 2, P_ATMOSPHERE_COLOR = 3, P_CLOUD_COLOR = 6,
            P_CLOUD_TINT = 9, P_STORM_DARKNESS = 10, P_ATMOSPHERE_ALPHA = 11, P_ATMOSPHERE_TIME = 12, P_STYLE = 13,
            P_IMPACT_TIME = 14, P_DENSITY = 15, P_LIGHTNING_STRENGTH = 16, P_EVE_FLASH = 17, P_LIGHTNING_MODE = 18,
            P_BLACKOUTS = 19, P_BODY_ALPHA_MASK = 20, P_NUKE = 21;

    private static final Sky.Texture[] CITY_LIGHTS = {texture("citylights_0"), texture("citylights_1"),
            texture("citylights_2"), texture("citylights_3")};
    private static final Sky.Texture DEFAULT_MASK = texture("default_mask");
    /** hbm-space's {@code withCityMask}: Kerbin, Duna and Laythe. */
    private static final Map<String, Sky.Texture> CITY_MASKS = Map.of("kerbin", texture("kerbin_mask"),
            "duna", texture("duna_mask"), "laythe", texture("laythe_mask"));
    /** How many city-light textures there are ({@code citylights_0..3}); the StarDar clamps to it too. */
    static final int CITY_LIGHT_LEVELS = CITY_LIGHTS.length;

    /** {@code citylights_<level>}, the texture of a light intensity (0 to {@link #CITY_LIGHT_LEVELS} - 1). */
    static Sky.Texture cityLights(int level) {
        return CITY_LIGHTS[level];
    }

    /** {@code body.cityMask}, or {@code default_mask} for a body without one. */
    static Sky.Texture cityMask(String body) {
        return CITY_MASKS.getOrDefault(body, DEFAULT_MASK);
    }

    /** hbm-space's {@code shaders/iChannel1.png}. */
    static final Sky.Texture NOISE = texture("ichannel1");

    private static Sky.Texture texture(String name) {
        return Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/" + name + ".png");
    }

    /** The values one body's layers share this frame (1.7's locals in {@code renderCelestials}). */
    static final class BodyFrame {
        final SolarSystem.Body body;
        final float phase;
        final float uvOffset;
        final float patternOffset;
        final float size;
        final float[] params = new float[Sky.SHADER_PARAMS];
        final int lightIntensity;
        final float atmosphereAlpha;
        final float atmosphereDensity;
        final float lightningStrength;
        final int style;
        final List<NukeShocks.Shock> shocks;

        BodyFrame(ClientLevel level, SolarSystem.Body body, double metricPhase, float uvOffset, float patternOffset, float size,
                  float partialTicks) {
            this.body = body;
            this.phase = (float) -metricPhase;
            this.uvOffset = uvOffset;
            this.patternOffset = patternOffset;
            this.size = size;
            long total = level.getGameTime();
            CBT_Impact impact = CelestialTraits.getTrait(body.name, CBT_Impact.class, true);
            CBT_Lights light = CelestialTraits.getTrait(body.name, CBT_Lights.class, true);
            double impactTime = impact != null ? (total - impact.time) + partialTicks : 0;
            float impactAnimationTime = impact != null ? (float) impactTime : -1.0F;
            this.lightIntensity = light != null && impactTime < 40 ? Mth.clamp(light.getIntensity(), 0, CITY_LIGHTS.length - 1) : 0;
            int activeBlackouts = Math.min((int) (impactTime / 8), 5);
            Vec3 atmosphereColor = AtmosphereRender.atmosphereColor(body);
            Vec3 cloudColor = AtmosphereRender.cloudColor(body);
            this.atmosphereAlpha = AtmosphereRender.density(body);
            this.atmosphereDensity = AtmosphereRender.density(body);
            this.lightningStrength = AtmosphereRender.cloudLightningStrength(body, partialTicks);
            this.style = AtmosphereRender.style(body);
            float atmosphereTime = ((float) total + partialTicks) / 20.0F;
            this.shocks = NukeShocks.clientShocks(body.name, total);
            params[P_PHASE] = phase;
            params[P_OFFSET] = uvOffset;
            params[P_PATTERN_OFFSET] = patternOffset;
            params[P_ATMOSPHERE_COLOR] = (float) atmosphereColor.x;
            params[P_ATMOSPHERE_COLOR + 1] = (float) atmosphereColor.y;
            params[P_ATMOSPHERE_COLOR + 2] = (float) atmosphereColor.z;
            params[P_CLOUD_COLOR] = (float) cloudColor.x;
            params[P_CLOUD_COLOR + 1] = (float) cloudColor.y;
            params[P_CLOUD_COLOR + 2] = (float) cloudColor.z;
            params[P_CLOUD_TINT] = AtmosphereRender.cloudTintStrength(body);
            params[P_STORM_DARKNESS] = AtmosphereRender.cloudStormDarkness(body, partialTicks);
            params[P_ATMOSPHERE_ALPHA] = atmosphereAlpha;
            params[P_ATMOSPHERE_TIME] = atmosphereTime;
            params[P_STYLE] = style;
            params[P_IMPACT_TIME] = impactAnimationTime;
            params[P_DENSITY] = atmosphereDensity;
            params[P_LIGHTNING_STRENGTH] = lightningStrength;
            params[P_EVE_FLASH] = AtmosphereRender.eveFlashStrength(body, atmosphereTime);
            params[P_LIGHTNING_MODE] = AtmosphereRender.lightningMode(body);
            params[P_BLACKOUTS] = activeBlackouts;
            params[P_BODY_ALPHA_MASK] = 0;
            AtmosphereRender.nukeShockParams(params, P_NUKE, shocks, total + (double) partialTicks);
        }

        Sky.Texture surface() {
            return Sky.Texture.of(NtmSpace.MOD_ID, body.texture());
        }

        Sky.Texture cityMask() {
            return CITY_MASKS.getOrDefault(body.name, DEFAULT_MASK);
        }
    }

    /** {@code drawPlanetShaderQuad}: the body's square at y 100, UV 0..1. */
    private static float[] quad(float size) {
        return new float[] {
                -size, 100, -size, 0, 0,
                size, 100, -size, 1, 0,
                size, 100, size, 1, 1,
                -size, 100, size, 0, 1};
    }

    /** {@code renderAtmosphereSurface}: before the crescent; none below an alpha of 0.001. */
    static void atmosphereSurface(Sky sky, BodyFrame f) {
        if (f.atmosphereAlpha <= 0.001F) return;
        sky.shaderQuads(ATMOSPHERE, Sky.Blend.ALPHA, true, f.params, new Sky.Texture[] {f.surface()}, quad(f.size));
    }

    /** {@code renderAtmosphereEmissive}, {@code renderNightLights} and {@code renderLightningOverlay}, after the crescent. */
    static void afterCrescent(Sky sky, BodyFrame f) {
        boolean cloudsOrHaze = f.style == AtmosphereRender.ATMOSPHERE_STYLE_CLOUDS || f.style == AtmosphereRender.ATMOSPHERE_STYLE_HAZE;
        if (f.lightIntensity > 0 && f.atmosphereDensity > 0.001F && cloudsOrHaze) {
            sky.shaderQuads(EMISSIVE, Sky.Blend.ADDITIVE, true, f.params,
                    new Sky.Texture[] {CITY_LIGHTS[f.lightIntensity], f.cityMask()}, quad(f.size));
        }
        if (f.lightIntensity > 0) {
            sky.shaderQuads(NIGHT_LIGHTS, Sky.Blend.ADDITIVE, true, f.params,
                    new Sky.Texture[] {CITY_LIGHTS[f.lightIntensity], f.cityMask()}, quad(f.size));
        }
        if (f.atmosphereAlpha > 0.001F && f.lightningStrength > 0.001F && cloudsOrHaze) {
            sky.shaderQuads(LIGHTNING, Sky.Blend.ADDITIVE, true, f.params, new Sky.Texture[] {f.surface(), f.cityMask()},
                    quad(f.size));
        }
    }

    /** {@code renderNukeImpactOverlays}: after the front half of the rings, while the body has shocks. */
    static void nukeOverlays(Sky sky, BodyFrame f) {
        if (f.shocks.isEmpty()) return;
        sky.shaderQuads(NUKE, Sky.Blend.ADDITIVE, true, f.params, new Sky.Texture[0], quad(f.size));
    }

    /**
     * {@code renderSun}'s black hole for a destroyed star with a shader (Kerbol): a quad three times the sun's size at
     * y 100, turned -90 degrees about y ("fix orbital plane"), {@code iTime} the time of day plus the partial tick over 20,
     * the noise texture bound, alpha blended.
     */
    static void blackHole(Sky sky, float sunSize) {
        ClientLevel level = sky.level();
        float time = ((float) dev.golem.core.world.TimeOfDay.get(level) + sky.partialTick()) / 20.0F;
        float s = sunSize * 3.0F; // withShader(blackhole.frag, 3)
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        float[] params = new float[1];
        params[0] = time;
        sky.shaderQuads(BLACK_HOLE, Sky.Blend.ALPHA, true, params, new Sky.Texture[] {NOISE}, quad(s));
        pose.popPose();
    }

    /** The {@code iTime} 1.7's flesh shader drew with: set after the draw, so each frame used the previous frame's. */
    private static float fleshTime;
    private static float[] sphere;

    /**
     * The flesh sphere over an infected body's sky ({@code CBT_COMPROMISED}): {@code sphere_v2} scaled 194.5 x 70.5 x 94.5
     * and turned 90 degrees about z, both faces, alpha blended, {@code iTime} = (time of day + partial tick) * 0.2 * 0.05.
     */
    static void flesh(Sky sky) {
        ClientLevel level = sky.level();
        if (!CelestialTraits.hasTrait(CelestialTraits.bodyOf(level), CelestialBodyTrait.CBT_Compromised.class, true)) return;
        float[] geometry = sphere();
        if (geometry == null) return;
        float time = ((float) dev.golem.core.world.TimeOfDay.get(level) + sky.partialTick()) * 0.2F;
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.scale(194.5F, 70.5F, 94.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        sky.shaderQuads(FLESH, Sky.Blend.ALPHA, false, new float[] {fleshTime}, new Sky.Texture[] {NOISE}, geometry);
        pose.popPose();
        fleshTime = time * 0.05F;
    }

    /** {@code ResourceManager.sphere_v2} ({@code models/spherev2.obj}): its 216 quads, positions only, 0 UVs. */
    private static float[] sphere() {
        if (sphere != null) return sphere.length == 0 ? null : sphere;
        List<float[]> vertices = new ArrayList<>();
        List<Float> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager()
                .open(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "models/spherev2.obj")), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts[0].equals("v")) {
                    vertices.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                } else if (parts[0].equals("f") && parts.length == 5) {
                    for (int i = 1; i <= 4; i++) {
                        float[] v = vertices.get(Integer.parseInt(parts[i].split("/")[0]) - 1);
                        out.add(v[0]);
                        out.add(v[1]);
                        out.add(v[2]);
                        out.add(0.0F);
                        out.add(0.0F);
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            NtmSpace.LOG.error("Could not read the flesh sphere model", e);
            sphere = new float[0];
            return null;
        }
        sphere = new float[out.size()];
        for (int i = 0; i < sphere.length; i++) sphere[i] = out.get(i);
        return sphere;
    }

    /** Harness: how many draws each layer has made on this client. */
    public static com.google.gson.JsonObject draws() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        for (Sky.Shader shader : ALL) json.addProperty(shader.id(), Sky.shaderDraws(shader));
        return json;
    }
}
