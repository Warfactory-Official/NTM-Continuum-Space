package com.ntmspace.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.render.Sky;
import dev.golem.core.render.Skyboxes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The sky of a celestial body, ported from hbm-space's {@code SkyProviderCelestial.render} (and
 * {@code SkyProviderLaytheSunset} on Laythe): the sunset fan at the body's own sun angle, the star box turning with the
 * body's sidereal angle, the digamma star, the sun (tinted by its air, {@code getSunColor}) with its spiky flare, and every
 * other body of the solar system at its computed angle and size - large ones as their texture with the crescent shadow
 * of their phase, washed into the sky colour by day where there is air, small ones as points - and, seen from above
 * y 200, the body's own surface below. Kerbin (the overworld, {@code WorldProviderEarth}) gets it too, as in hbm-space.
 *
 * <p>Drawn through Golem Core's {@link Skyboxes} seam, replacing vanilla's sun, moon and stars, in the phase
 * {@link SpaceSkies#PHASE} so it comes before HBM's own skyboxes; 1.7's {@code glRotatef} calls map one to one onto the
 * sky pose. The crescent is hbm-space's {@code crescent.frag}, which quantises to a 16 x 16 grid, so it is drawn
 * exactly as 256 flat black cells with the shader's alpha. Off Kerbin vanilla's sunrise is switched off
 * ({@link CelestialSkyColors}) and drawn here, because vanilla's follows the dimension's vanilla sun angle.
 *
 * <p>Rings, atmosphere glows, destroyed and impacted bodies, satellites, stations and the airless sunset's charged dust
 * are {@link CelestialSkyObjects}'; meteors and war effects are {@link WarSky}'s; the shader layers (a planet's clouds,
 * emissive air, city lights, lightning and nuke rings, a destroyed sun's black hole, an infected body's flesh sphere) are
 * {@link PlanetShaders}'; Thatmo's battle (railgun, shield, its meteors and beams) is {@link ThatmoSky}'s. 1.7's black "blanking" quad behind the sun was
 * drawn with additive blending and so never showed; it is left out. The star's Dyson swarms are {@link DysonSky}'s,
 * drawn after the sun's flare.
 */
public final class MunSky implements Skyboxes.Skybox {

    private static final Sky.Texture NIGHT = texture("night");
    private static final Sky.Texture SUNSPIKE = texture("sunspike");
    private static final Sky.Texture DIGAMMA = texture("star_digamma");
    private static final Sky.Texture PLANET = texture("planet");

    private static Sky.Texture texture(String name) {
        return Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/" + name + ".png");
    }

    /**
     * Harness test hook ({@code ntmspace_sky} query, {@code pin_ticks}): the total world time the orbits are drawn at,
     * or NaN for the level's own. hbm-space placed every body by {@code getTotalWorldTime()} (the world's age, not its
     * time of day), so two saves at the same time of day show the other bodies in different places; pinning it makes
     * the sky of every node comparable. Client only, never saved.
     */
    static volatile double pinnedTicks = Double.NaN;

    /** {@code getTotalWorldTime() + partialTicks}, the time the orbits are computed at ({@link #pinnedTicks}). */
    static double skyTicks(ClientLevel level, float partialTicks) {
        double pinned = pinnedTicks;
        return Double.isNaN(pinned) ? level.getGameTime() + (double) partialTicks : pinned;
    }

    public static void init() {
        Skyboxes.SKYBOXES.addPhaseOrdering(SpaceSkies.PHASE, dev.golem.core.hook.Hook.DEFAULT_PHASE);
        Skyboxes.SKYBOXES.register(SpaceSkies.PHASE, new MunSky());
        CelestialSkyColors.init();
    }

    @Override
    public boolean replacesCelestial(ClientLevel level) {
        return SpaceSkies.body(level) != null;
    }

    @Override
    public void celestial(Sky sky) {
        ClientLevel level = sky.level();
        SolarSystem.Body body = SpaceSkies.body(level);
        float partialTicks = sky.partialTick();
        String air = SpaceSkies.airBody(level);
        boolean hasAtmosphere = air != null && Atmospheres.has(air);
        float pressure = hasAtmosphere ? (float) Atmospheres.pressure(air) : 0.0F;
        float visibility = hasAtmosphere ? Mth.clamp(2.0F - pressure, 0.1F, 1.0F) : 1.0F;
        double ticks = skyTicks(level, partialTicks);

        // the sky colour (1.7 skyR/G/B, thinned above y 300 by the colour modifier) and the planets' tint
        Vec3 skyColor = sky.skyColor();
        Vec3 planetTint = skyColor;
        float fogIntensity = CelestialSkyColors.fogIntensity(level);
        if (fogIntensity > 0.01F) {
            Vec3 fog = CelestialSkyColors.fogColor(level, SpaceSkies.angle(level, partialTicks));
            planetTint = new Vec3(clampedLerp(skyColor.x, fog.x, fogIntensity), clampedLerp(skyColor.y, fog.y, fogIntensity),
                    clampedLerp(skyColor.z, fog.z, fogIntensity));
        }

        boolean kerbin = SpaceSkies.isKerbin(level);
        float solarAngle = kerbin ? sky.celestialAngle() : SpaceSkies.angle(level, partialTicks);
        float siderealAngle = (float) SolarSystem.calculateSiderealAngle(body, ticks);
        float dust = com.hbm.handler.ImpactWorldHandler.getDustForClient(level);
        float starBrightness = SpaceSkies.starBrightness(level, partialTicks) * (1 - dust) * visibility;

        // Handle any special per-body sunset rendering (Kerbin's is vanilla's)
        if (!kerbin) renderSunset(sky, level, solarAngle);

        renderStars(sky, starBrightness, solarAngle + siderealAngle, body.axialTilt);

        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(body.axialTilt));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(solarAngle * 360.0F));

        // texture is 4 times larger than the sun itself, for the glow
        double sunSize = SolarSystem.calculateSunSize(body) * SolarSystem.SUN_RENDER_SCALE;
        double coronaSize = sunSize * (3 - Mth.clamp(pressure, 0.0F, 1.0F));
        renderSun(sky, body.getStar(), CelestialSkyColors.sunColor(level), (float) sunSize, (float) coronaSize, visibility, pressure);

        float blendAmount = hasAtmosphere ? Mth.clamp(1 - SpaceSkies.daylight(level, partialTicks), 0.25F, 1F) : 1F;
        List<SolarSystem.AstroMetric> metrics = SolarSystem.calculateClientMetricsFromBody(ticks, body, solarAngle);
        renderCelestials(sky, metrics, planetTint, visibility, blendAmount, SolarSystem.MAX_APPARENT_SIZE_SURFACE);
        // satellites, the held satellite's preview and the stations (1.7: while visibility > 0.2)
        if (visibility > 0.2F) CelestialSkyObjects.orbiters(sky, level, body, solarAngle);
        pose.popPose();

        // Thatmo's battlefield: the railgun satellite's silhouette (SkyProviderThatmo.render3DModel)
        ThatmoSky.model(sky);
        // the arrived war projectiles' shockwaves, each after the silhouette again (SkyProviderCelestial's war loop)
        WarSky.shockwaves(sky, level, partialTicks);

        // the body's own rings over its horizon (Dres)
        CelestialSkyObjects.ownRings(sky, body, visibility);
        // Thatmo's battle (SkyProviderThatmo.renderSpecialEffects): after the rings, before the flesh sky
        ThatmoSky.specialEffects(sky);
        // an infected body's sky (CBT_COMPROMISED): the flesh sphere (fle.frag)
        PlanetShaders.flesh(sky);
    }

    /**
     * After the dark disc: seen from above y 200, the body's surface texture below, fading in over 300 blocks and lit by
     * the sun's brightness ({@code SkyProviderCelestial.render}'s last quad).
     */
    @Override
    public void afterSky(Sky sky) {
        ClientLevel level = sky.level();
        SolarSystem.Body body = SpaceSkies.body(level);
        net.minecraft.world.entity.Entity camera = net.minecraft.client.Minecraft.getInstance().getCameraEntity();
        if (body == null || camera == null) return;
        double y = camera.getEyeY();
        float alpha = ((float) y - 200.0F) / 300.0F;
        if (alpha <= 0.0F) return;
        double sc = 1 / (y / 1000);
        float uvOffset = (float) ((camera.getX() / 1024) % 1);
        float base = SpaceSkies.isKerbin(level) ? vanillaSunBrightness(sky.celestialAngle()) : SpaceSkies.sunBrightness(level, sky.partialTick());
        float sunBrightness = CelestialSkyColors.sunBrightness(level, base, sky.partialTick());
        float s = (float) (115 * sc);
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        sky.texturedQuads(Sky.Texture.of(NtmSpace.MOD_ID, body.texture()), Sky.Blend.ALPHA, sunBrightness, sunBrightness, sunBrightness,
                Math.min(alpha, 1.0F),
                -s, 100, -s, uvOffset, 0,
                s, 100, -s, 1 + uvOffset, 0,
                s, 100, s, 1 + uvOffset, 1,
                -s, 100, s, uvOffset, 1);
        pose.popPose();
    }

    /** 1.7 {@code World.getSunBrightness} at {@code angle}, without weather. */
    private static float vanillaSunBrightness(float angle) {
        float f = 1.0F - (Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.2F);
        f = 1.0F - Mth.clamp(f, 0.0F, 1.0F);
        return f * 0.8F + 0.2F;
    }

    /** {@code BobMathUtil.clampedLerp}. */
    private static double clampedLerp(double a, double b, double t) {
        if (t < 0) return a;
        if (t > 1) return b;
        return a + (b - a) * t;
    }

    /** Whether this native sky uses Laythe's three additive sunset fans. */
    public static boolean laytheSunset(ResourceKey<Level> dimension) {
        return dimension.equals(SpaceDimensions.LAYTHE);
    }

    /**
     * {@code SkyProviderCelestial.renderSunset}: vanilla's sunrise fan (alpha blend) in the colours of
     * {@link CelestialSkyColors#skySunset}, or on Laythe {@code SkyProviderLaytheSunset}'s three wider fans with
     * {@code GL_ONE_MINUS_CONSTANT_ALPHA} (constant colour 0: additive). The airless glow's charged-dust points are not
     * drawn.
     */
    private static void renderSunset(Sky sky, ClientLevel level, float solarAngle) {
        boolean laythe = laytheSunset(level.dimension());
        float[] c = laythe ? CelestialSkyColors.providerSunset(level, solarAngle) : CelestialSkyColors.skySunset(level, solarAngle);
        if (c == null) return;
        float direction = Mth.sin(solarAngle * (float) Math.PI * 2.0F) < 0.0F ? 180.0F : 0.0F;
        PoseStack pose = sky.pose();
        if (!laythe) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(direction));
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            fan(sky, Sky.Blend.ALPHA, c, 100.0F, c[0], c[1], c[2], 120.0F, 40.0F * c[3]);
            // charged dust over an airless horizon (1.7: below 0.05 atm, in the same frame)
            String air = SpaceSkies.airBody(level);
            float pressure = air == null ? 0.0F : (float) Atmospheres.pressure(air);
            if (pressure < 0.05F) CelestialSkyObjects.chargedDust(sky, level, SpaceSkies.body(level), c[3], sky.partialTick());
            pose.popPose();
            return;
        }
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(direction));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        fan(sky, Sky.Blend.ADDITIVE, c, 150.0F, c[0], c[1], c[2], 160.0F, 90.0F * c[3]);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(135.0F));
        pose.translate(0, -60, 0);
        fan(sky, Sky.Blend.ADDITIVE, c, 100.0F, c[0], c[1], c[2], 100.0F, 90.0F);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(135.0F));
        pose.translate(0, -30, 0);
        fan(sky, Sky.Blend.ADDITIVE, c, 80.0F, c[0], c[1] * 0.2F, c[2], 100.0F, 90.0F);
        pose.popPose();
    }

    /**
     * A 1.7 sunset triangle fan of 16 segments: centre {@code (0, height, 0)} in {@code centre}'s colour and alpha, rim
     * {@code (sin * radius, cos * radius, -cos * depth)} in the rim colour with alpha 0.
     */
    private static void fan(Sky sky, Sky.Blend blend, float[] centre, float height, float rimR, float rimG, float rimB,
                            float radius, float depth) {
        int segments = 16;
        float[] v = new float[segments * 4 * 7];
        int o = 0;
        for (int j = 0; j < segments; j++) {
            float a0 = (float) j * (float) Math.PI * 2.0F / (float) segments;
            float a1 = (float) (j + 1) * (float) Math.PI * 2.0F / (float) segments;
            o = vertex(v, o, 0.0F, height, 0.0F, centre[0], centre[1], centre[2], centre[3]);
            o = vertex(v, o, Mth.sin(a0) * radius, Mth.cos(a0) * radius, -Mth.cos(a0) * depth, rimR, rimG, rimB, 0.0F);
            o = vertex(v, o, Mth.sin(a1) * radius, Mth.cos(a1) * radius, -Mth.cos(a1) * depth, rimR, rimG, rimB, 0.0F);
            o = vertex(v, o, 0.0F, height, 0.0F, centre[0], centre[1], centre[2], centre[3]);
        }
        sky.coloredQuads(blend, v);
    }

    private static int vertex(float[] v, int o, float x, float y, float z, float r, float g, float b, float a) {
        v[o] = x;
        v[o + 1] = y;
        v[o + 2] = z;
        v[o + 3] = r;
        v[o + 4] = g;
        v[o + 5] = b;
        v[o + 6] = a;
        return o + 7;
    }

    static void renderStars(Sky sky, float starBrightness, float siderealAngle, float axialTilt) {
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(axialTilt));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(siderealAngle * 360.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));

        if (starBrightness > 0.0F) {
            float alpha = starBrightness * 0.6F;
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            skyboxSide(sky, 4, alpha);

            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            skyboxSide(sky, 1, alpha);
            pose.popPose();

            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            skyboxSide(sky, 0, alpha);
            pose.popPose();

            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            skyboxSide(sky, 5, alpha);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            skyboxSide(sky, 2, alpha);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            skyboxSide(sky, 3, alpha);
            pose.popPose();
        }

        // demeter
        pose.pushPose();
        float brightness = Math.max(0.5F, starBrightness * 1.6F);
        float digamma = Minecraft.getInstance().player == null ? 0.0F
                : (float) com.hbm.capability.HbmLivingProps.getDigamma(Minecraft.getInstance().player);
        float size = 1 + digamma * 0.25F;
        float dist = 100F - digamma * 2.5F;
        pose.mulPose(Axis.XP.rotationDegrees(140.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        sky.texturedQuads(DIGAMMA, Sky.Blend.ADDITIVE, brightness, brightness, brightness, brightness,
                -size, dist, -size, 0, 0,
                size, dist, -size, 0, 1,
                size, dist, size, 1, 1,
                -size, dist, size, 1, 0);
        pose.popPose();

        pose.popPose();
    }

    /** {@code renderSkyboxSide}: one face of the star box, a third by a half of night.png. */
    private static void skyboxSide(Sky sky, int side, float alpha) {
        float u = side % 3 / 3.0F;
        float v = side / 3 / 2.0F;
        float w = 1.0F / 3.0F;
        sky.texturedQuads(NIGHT, Sky.Blend.ADDITIVE, 1.0F, 1.0F, 1.0F, alpha,
                -100, -100, -100, u, v,
                -100, -100, 100, u, v + 0.5F,
                100, -100, 100, u + w, v + 0.5F,
                100, -100, -100, u + w, v);
    }

    static void renderSun(Sky sky, SolarSystem.Body sun, float[] sunColor, float sunSize, float coronaSize, float visibility,
                                  float pressure) {
        // BLACK HOLE SUN, WON'T YOU COME: a destroyed star with a shader (only Kerbol has one, withShader(blackhole.frag, 3));
        // any other star draws as usual, as in 1.7
        if (sun.name.equals("kerbol") && CelestialSkyObjects.starDestroyed(sun)) {
            PlanetShaders.blackHole(sky, sunSize);
            return;
        }
        // tinted by the air (WorldProviderCelestial.getSunColor)
        sky.texturedQuads(Sky.Texture.of(NtmSpace.MOD_ID, sun.texture()), Sky.Blend.ADDITIVE, sunColor[0], sunColor[1], sunColor[2], visibility,
                -sunSize, 100, -sunSize, 0, 0,
                sunSize, 100, -sunSize, 1, 0,
                sunSize, 100, sunSize, 1, 1,
                -sunSize, 100, sunSize, 0, 1);
        // a big ol' spiky flare, less so with an atmosphere
        sky.texturedQuads(SUNSPIKE, Sky.Blend.ADDITIVE, sunColor[0], sunColor[1], sunColor[2], 1 - Mth.clamp(pressure, 0.0F, 1.0F) * 0.75F,
                -coronaSize, 99.9F, -coronaSize, 0, 0,
                coronaSize, 99.9F, -coronaSize, 1, 0,
                coronaSize, 99.9F, coronaSize, 1, 1,
                -coronaSize, 99.9F, coronaSize, 0, 1);
        // Draw the swarm members with depth occlusion; last, so they render transparent against the sun (1.7)
        DysonSky.renderSwarm(sky, sun, sunSize);
    }

    private static void renderCelestials(Sky sky, List<SolarSystem.AstroMetric> metrics, Vec3 planetTint, float visibility,
                                         float blendAmount, float maxSize) {
        renderCelestials(sky, metrics, planetTint, visibility, blendAmount, maxSize, null, 0);
    }

    /**
     * {@code renderCelestials} with 1.7's {@code orbiting} argument (the station sky, {@link OrbitSky}): the orbited body
     * is drawn without its axial tilt and with its texture (and the crescent's quantisation grid) scrolled by
     * {@code 1 - (worldTime / 1024) % 1}; {@code worldTime} includes the partial tick.
     */
    static void renderCelestials(Sky sky, List<SolarSystem.AstroMetric> metrics, Vec3 planetTint, float visibility,
                                 float blendAmount, float maxSize, SolarSystem.Body orbiting, double worldTime) {
        float blendDarken = 0.1F;
        PoseStack pose = sky.pose();
        double transitionMinSize = 0.01D;
        double transitionMaxSize = 0.5D;

        for (SolarSystem.AstroMetric metric : metrics) {
            if (metric.distance == 0) continue; // self

            pose.pushPose();
            double size = Mth.clamp(metric.apparentSize, 0, maxSize);
            boolean renderPoint = size < transitionMaxSize;
            boolean renderBody = size > transitionMinSize;

            boolean orbitingThis = metric.body == orbiting;
            float uvOffset = orbitingThis ? (float) (1 - ((worldTime / 1024) % 1)) : 0;
            float patternOffset = orbitingThis ? (float) -(worldTime / 1024.0D) : 0.0F;
            float axialTilt = orbitingThis ? 0 : metric.body.axialTilt;

            pose.mulPose(Axis.XP.rotationDegrees((float) metric.angle));
            pose.mulPose(Axis.ZP.rotationDegrees((float) metric.inclination));
            pose.mulPose(Axis.YP.rotationDegrees(axialTilt + 90.0F));

            float s = (float) size;
            if (renderBody) {
                // Draw the back half of the ring (obscured by body)
                CelestialSkyObjects.ringBack(sky, metric.body, metric.angle, s, visibility);
                com.ntmspace.trait.CBT_Destroyed destroyed = CelestialSkyObjects.destroyed(metric.body);
                PlanetShaders.BodyFrame shaded = new PlanetShaders.BodyFrame(sky.level(), metric.body, metric.phase, uvOffset,
                        patternOffset, s, sky.partialTick());
                if (destroyed != null) {
                    CelestialSkyObjects.destroyedBody(sky, metric.body, s, uvOffset, destroyed);
                } else {
                    CelestialSkyObjects.atmosphereGlow(sky, metric.body, s);
                    // 1.7 draws the body with blending off; the planet textures are opaque squares
                    if (uvOffset == 0) {
                        sky.texturedQuads(Sky.Texture.of(NtmSpace.MOD_ID, metric.body.texture()), Sky.Blend.ALPHA,
                                1.0F, 1.0F, 1.0F, 1.0F,
                                -s, 100, -s, 0, 0,
                                s, 100, -s, 1, 0,
                                s, 100, s, 1, 1,
                                -s, 100, s, 0, 1);
                    } else {
                        // 1.7 drew u = offset .. 1 + offset on a repeating texture; split at the wrap so no sampler mode matters
                        float wrap = uvOffset % 1;
                        float split = -s + 2 * s * (1 - wrap);
                        sky.texturedQuads(Sky.Texture.of(NtmSpace.MOD_ID, metric.body.texture()), Sky.Blend.ALPHA,
                                1.0F, 1.0F, 1.0F, 1.0F,
                                -s, 100, -s, wrap, 0,
                                split, 100, -s, 1, 0,
                                split, 100, s, 1, 1,
                                -s, 100, s, wrap, 1,
                                split, 100, -s, 0, 0,
                                s, 100, -s, wrap, 0,
                                s, 100, s, wrap, 1,
                                split, 100, s, 0, 1);
                    }
                    // clouds, haze or gas bands (atmosphere.frag), then the crescent, then emissive air, city lights and
                    // lightning (atmosphere_emissive.frag, nightlights.frag, lightning.frag), as 1.7 ordered them
                    PlanetShaders.atmosphereSurface(sky, shaded);
                    crescent(sky, (float) -metric.phase, s, uvOffset);
                    PlanetShaders.afterCrescent(sky, shaded);
                    CelestialSkyObjects.impact(sky, sky.level(), metric.body, s, uvOffset, sky.partialTick());

                    // Draw another layer on top to blend with the atmosphere (1.7 left the additive blend set here; a
                    // colour below 0 was clamped by GL, an alpha of 0 - no air - draws nothing)
                    float tintAlpha = 1 - blendAmount * visibility;
                    if (tintAlpha > 0.0F) {
                        sky.quads(Sky.Blend.ADDITIVE, Math.max(0.0F, (float) planetTint.x - blendDarken),
                                Math.max(0.0F, (float) planetTint.y - blendDarken), Math.max(0.0F, (float) planetTint.z - blendDarken),
                                tintAlpha,
                                -s, 100, -s, s, 100, -s, s, 100, s, -s, 100, s);
                    }
                }
                // Draw the front half of the ring (unobscured)
                CelestialSkyObjects.ringFront(sky, metric.body, metric.angle, s, visibility);
                // the nuke flashes and rings (nuke.frag), destroyed bodies included as in 1.7
                PlanetShaders.nukeOverlays(sky, shaded);
            }

            if (renderPoint) {
                float alpha = Mth.clamp(s * 100.0F, 0.0F, 1.0F);
                alpha *= 1 - Mth.clamp((s - (float) transitionMinSize) / (float) (transitionMaxSize - transitionMinSize), 0.0F, 1.0F);
                // additive: a point-only body inherits the sun's blend in 1.7, and a drawn body's overlays end by setting
                // the additive blend again (renderCelestials, after renderLightningOverlay)
                sky.texturedQuads(PLANET, Sky.Blend.ADDITIVE,
                        metric.body.color[0], metric.body.color[1], metric.body.color[2], alpha * visibility,
                        -1, 100, -1, 0, 0,
                        1, 100, -1, 1, 0,
                        1, 100, 1, 1, 1,
                        -1, 100, 1, 0, 1);
            }
            pose.popPose();
        }
    }

    /**
     * hbm-space's {@code crescent.frag} (offset 0, no alpha mask): the shadow of a body at {@code phase}, computed per
     * cell of its 16 x 16 quantisation and drawn as black cells, cells of one alpha in one batch.
     */
    private static void crescent(Sky sky, float phase, float size) {
        crescent(sky, phase, size, 0);
    }

    /**
     * The crescent with the shader's {@code offset}: {@code quantize(uv + offset) - offset} shifts the 16 x 16 grid
     * along u, so the cells start at {@code -frac(offset)} and the first and last are cut at the quad's edges; a cell's
     * value is its left edge, as in the shader.
     */
    static void crescent(Sky sky, float phase, float size, float offset) {
        java.util.Map<Integer, java.util.List<Float>> byAlpha = new java.util.HashMap<>();
        float shift = offset == 0 ? 0 : (float) (offset - Math.floor(offset / 0.0625F) * 0.0625F);
        int columns = shift == 0 ? 16 : 17;
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < 16; j++) {
                float fx = i * 0.0625F - shift;
                float fy = j * 0.0625F;
                float cellU0 = Math.max(fx, 0.0F), cellU1 = Math.min(fx + 0.0625F, 1.0F);
                if (cellU1 <= cellU0) continue;
                float ux = 2.25F * fx - 1.1F;
                float uy = 2.25F * fy - 1.1F;
                float sx = 2.0F * fx - 1.0F;
                float sy = 2.0F * fy - 1.0F;
                float lx = (float) Math.sin(phase * Math.PI);
                float lz = (float) Math.cos(phase * Math.PI);
                float nz = (float) Math.sqrt(1.0F - Mth.clamp(ux * ux + uy * uy, 0.0F, 1.0F));
                float brightness = ux * lx + nz * lz;
                brightness = Math.max(brightness, (Math.abs(phase) - 0.7F) * Mth.clamp(sx * sx + sy * sy, 0.0F, 1.0F));
                if (Math.abs(phase) < 0.5F) {
                    brightness = phase < 0.0F ? phase * 4.0F + 2.0F - ux : -phase * 4.0F + 2.0F + ux;
                }
                brightness = Math.max(brightness, 0.05F);
                float alpha = Mth.clamp(1.0F - brightness, 0.0F, 1.0F);
                if (alpha <= 0.0F) continue;
                int key = Math.round(alpha * 255.0F);
                float x0 = -size + 2 * size * cellU0, x1 = -size + 2 * size * cellU1;
                float z0 = -size + 2 * size * fy, z1 = -size + 2 * size * (fy + 0.0625F);
                java.util.List<Float> cells = byAlpha.computeIfAbsent(key, k -> new java.util.ArrayList<>());
                java.util.Collections.addAll(cells, x0, 100F, z0, x1, 100F, z0, x1, 100F, z1, x0, 100F, z1);
            }
        }
        for (java.util.Map.Entry<Integer, java.util.List<Float>> group : byAlpha.entrySet()) {
            float[] xyz = new float[group.getValue().size()];
            for (int k = 0; k < xyz.length; k++) xyz[k] = group.getValue().get(k);
            sky.quads(Sky.Blend.ALPHA, 0.0F, 0.0F, 0.0F, group.getKey() / 255.0F, xyz);
        }
    }
}
