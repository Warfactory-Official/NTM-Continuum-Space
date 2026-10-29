package com.ntmspace.client;

import com.ntmspace.world.SkyMath;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.satellite.SatelliteOrbit;
import com.ntmspace.satellite.SatelliteSky;
import com.ntmspace.satellite.SpaceSatellites;
import com.ntmspace.trait.CBT_Destroyed;
import com.ntmspace.trait.CBT_Impact;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.render.Sky;
import dev.golem.core.render.TexturePixels;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The objects of hbm-space's {@code SkyProviderCelestial} that {@link MunSky} draws through these helpers:
 *
 * <ul>
 *   <li>Planetary rings: a ringed body in the sky (Dres, Sarnus, Urlum) behind and in front of its disc
 *       ({@code renderCelestials}), and the body's own rings arching over its surface ({@code renderRings}, Dres).</li>
 *   <li>The atmosphere glow around a body with air or a gas giant ({@code renderAtmosphereGlow}, three bands of
 *       gradient quads in the air's colour).</li>
 *   <li>A destroyed body ({@code CBT_Destroyed}): its disc replaced by 30 drifting fragments of its texture and of
 *       {@code destroyed.png}, a growing shockwave and a fading flare.</li>
 *   <li>An impact on a body in the sky ({@code CBT_Impact}): its lava overlay, shockwave and flare.</li>
 *   <li>The satellites in orbit ({@code SatelliteBase.renderDefault}: a {@code satellite.png} quad in the satellite's
 *       colour on its inclined orbit at its altitude, blinking if set), Kerbin's "Bobmazon" delivery satellite, the
 *       preview of a held satellite with its orbit line ({@code renderHeldSatellitePreview}) and the orbital stations
 *       above the body ({@code renderStation}). Railguns are {@link WarSky}'s.</li>
 *   <li>An airless body's charged dust at sunrise and sunset: 1,024 one-pixel points over the horizon
 *       ({@code renderSunset}).</li>
 * </ul>
 *
 * The layers drawn by hbm-space's GLSL shaders (the atmosphere surface, emissive air, city lights, lightning, nuke rings,
 * the infected flesh sphere and the black hole that replaces a destroyed sun) are {@link PlanetShaders}'. A body's
 * crescent is {@link MunSky}'s exact reproduction of {@code crescent.frag}.
 */
public final class CelestialSkyObjects {

    private static final Sky.Texture RINGS = texture("rings");
    private static final Sky.Texture DESTROYED = texture("destroyed");
    private static final Sky.Texture IMPACT = texture("impact");
    private static final Sky.Texture STATION = texture("station");
    private static final Sky.Texture SATELLITE = texture("satellite");
    private static final Sky.Texture SHOCKWAVE = Sky.Texture.of("hbm", "textures/particle/shockwave.png");
    private static final Sky.Texture SHOCK_FLARE = Sky.Texture.of("hbm", "textures/particle/flare.png");

    /** hbm-space's gas giants ({@code withGas}) and their gas colours ({@code Fluids}: JOOLGAS, SARNUSGAS, UGAS, NGAS). */
    private static final Map<String, Integer> GAS_GIANTS = Map.of("jool", 0x96B596, "sarnus", 0xE47D5C, "urlum", 0x718C9A,
            "neidon", 0xA37BA3);

    /** {@code CelestialBody.withBlockTextures}'s surface sheet, including bodies whose atmosphere can be removed. */
    private static final Map<String, String> SURFACES = Map.of(
            "mun", "hbm:textures/blocks/moon_turf.png",
            "minmus", "ntmspace:textures/blocks/minmus_regolith.png",
            "ike", "ntmspace:textures/blocks/ike_regolith.png",
            "moho", "ntmspace:textures/blocks/moho_regolith.png",
            "dres", "hbm:textures/blocks/sellafield_slaked.png",
            "kerbin", "minecraft:textures/block/dirt.png",
            "duna", "ntmspace:textures/blocks/duna_sands.png",
            "eve", "ntmspace:textures/blocks/eve_silt.png",
            "laythe", "ntmspace:textures/blocks/laythe_silt.png",
            "tekto", "ntmspace:textures/blocks/rubber_silt.png");
    private static final Object2IntOpenHashMap<String> SURFACE_TEXELS = new Object2IntOpenHashMap<>();

    static void init() {
        ClientResourceReloads.register(NtmSpace.MOD_ID, "charged_dust_surfaces", SURFACE_TEXELS::clear);
    }

    public static String surfaceTexture(SolarSystem.Body body) {
        return SURFACES.get(body.name);
    }

    private static int surfaceTexel(String texture) {
        try {
            return TexturePixels.read(Ids.parse(texture)).sampleNearestRepeat(0F, 0F);
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot read charged-dust surface " + texture, failure);
        }
    }
    /** {@code SatelliteBase.renderDefault} for Kerbin's delivery satellite (1.7 "JEFF BOZOS"): its seed and colour. */
    private static final int BOBMAZON_SEED = 1916169;

    private CelestialSkyObjects() {}

    private static Sky.Texture texture(String name) {
        return Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/" + name + ".png");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // rings

    /** {@code renderCelestials}: the back half of a ringed body's ring, before its disc (additive, both faces). */
    static void ringBack(Sky sky, SolarSystem.Body body, double angle, float size, float visibility) {
        if (body.rings == null) return;
        float ringSize = size * body.rings[1];
        ring(sky, body, angle, visibility,
                -ringSize, 0, -ringSize, 0, 0,
                ringSize, 0, -ringSize, 1, 0,
                ringSize, 0, 0, 1, 0.5F,
                -ringSize, 0, 0, 0, 0.5F);
    }

    /** {@code renderCelestials}: the front half, after the disc and its overlays. */
    static void ringFront(Sky sky, SolarSystem.Body body, double angle, float size, float visibility) {
        if (body.rings == null) return;
        float ringSize = size * body.rings[1];
        ring(sky, body, angle, visibility,
                -ringSize, 0, 0, 0, 0.5F,
                ringSize, 0, 0, 1, 0.5F,
                ringSize, 0, ringSize, 1, 1,
                -ringSize, 0, ringSize, 0, 1);
    }

    private static void ring(Sky sky, SolarSystem.Body body, double angle, float visibility, float... quad) {
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.translate(0.0F, 100.0F, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -angle));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F - body.rings[0]));
        pose.mulPose(Axis.YP.rotationDegrees((float) angle));
        // 1.7 drew it with culling off: both windings, of which the camera sees one
        sky.texturedQuads(RINGS, Sky.Blend.ADDITIVE, body.rings[2], body.rings[3], body.rings[4], visibility, bothFaces(quad, 5));
        pose.popPose();
    }

    /**
     * {@code render}: the rings of the body itself, a 400-block square 20 blocks to the side of the sky's centre, below
     * the horizon, tilted by the axial tilt less the ring tilt ({@code renderRings}).
     */
    static void ownRings(Sky sky, SolarSystem.Body body, float visibility) {
        if (body.rings == null) return;
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(body.axialTilt - body.rings[0]));
        pose.translate(0, -100, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        float ringSize = 200;
        float offset = -20.0F;
        sky.texturedQuads(RINGS, Sky.Blend.ADDITIVE, body.rings[2], body.rings[3], body.rings[4], visibility,
                offset, -ringSize, -ringSize, 0, 0,
                offset, ringSize, -ringSize, 1, 0,
                offset, ringSize, ringSize, 1, 1,
                offset, -ringSize, ringSize, 0, 1);
        pose.popPose();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // atmospheres (AtmosphereRenderUtil)

    /** 1.7 {@code body.gas != null}: Jool, Sarnus, Urlum and Neidon. */
    static boolean isGasGiant(SolarSystem.Body body) {
        return GAS_GIANTS.containsKey(body.name);
    }

    /** {@code getAtmosphereGlowAlpha}: 0.35 for a gas giant, else by the air's pressure (none below 0.02 atm). */
    static float glowAlpha(SolarSystem.Body body) {
        if (GAS_GIANTS.containsKey(body.name)) return 0.35F;
        if (!Atmospheres.has(body.name)) return 0.0F;
        float pressure = Mth.clamp((float) Atmospheres.pressure(body.name), 0.0F, 3.0F);
        if (pressure <= 0.02F) return 0.0F;
        return Mth.clamp(0.08F + pressure * 0.16F, 0.08F, 0.5F);
    }

    /** {@code getBodyAtmosphereColor}: a gas giant's gas, else the air's fluids weighted by pressure; white without. */
    static Vec3 atmosphereColor(SolarSystem.Body body) {
        Integer gas = GAS_GIANTS.get(body.name);
        if (gas != null) return hex(gas);
        double total = 0, r = 0, g = 0, b = 0;
        for (Atmospheres.Entry entry : Atmospheres.of(body.name)) {
            if (entry.pressure() <= 0.0D) continue;
            Vec3 color = fluidColor(entry.fluid());
            r += color.x * entry.pressure();
            g += color.y * entry.pressure();
            b += color.z * entry.pressure();
            total += entry.pressure();
        }
        if (total <= 0.0D) return new Vec3(1, 1, 1);
        return new Vec3(Mth.clamp(r / total, 0, 1), Mth.clamp(g / total, 0, 1), Mth.clamp(b / total, 0, 1));
    }

    /** {@code WorldProviderCelestial.getAtmosphereFluidColor}. */
    static Vec3 fluidColor(String fluid) {
        if (fluid.equals(Atmospheres.EVEAIR)) return new Vec3(53F / 255F, 32F / 255F, 74F / 255F);
        // Slightly redder "red sand" tint for Duna-like atmospheres.
        if (fluid.equals(Atmospheres.DUNAAIR)) return new Vec3(198F / 255F, 96F / 255F, 64F / 255F);
        // Neutral/desaturated CO2 tint.
        if (fluid.equals(Atmospheres.CARBONDIOXIDE)) return new Vec3(188F / 255F, 192F / 255F, 198F / 255F);
        if (fluid.equals(Atmospheres.EARTHAIR) || fluid.equals(Atmospheres.OXYGEN) || fluid.equals(Atmospheres.NITROGEN)) {
            return new Vec3(0.7529412F, 0.84705883F, 1.0F);
        }
        return hex(Atmospheres.color(fluid));
    }

    private static Vec3 hex(int color) {
        return new Vec3(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }

    /**
     * {@code renderAtmosphereGlow} (visibility 1): three bands around the disc, from the glow's alpha at 0.98 of its size
     * to half of it at 1.075 and nothing at 1.15 (wider with a denser glow), alpha-blended, culling off.
     */
    static void atmosphereGlow(Sky sky, SolarSystem.Body body, float size) {
        float glowAlpha = glowAlpha(body);
        if (glowAlpha <= 0.001F) return;
        float leading = glowAlpha;
        float trailing = glowAlpha;
        Vec3 atmo = atmosphereColor(body);
        float r = Mth.clamp((float) atmo.x * 1.15F, 0.0F, 1.0F);
        float g = Mth.clamp((float) atmo.y * 1.15F, 0.0F, 1.0F);
        float b = Mth.clamp((float) atmo.z * 1.15F, 0.0F, 1.0F);
        float inner = size * 0.98F;
        float middle = size * 1.075F;
        float outer = (float) (size * 1.15D * (1.0D + glowAlpha * 0.25D));
        float y = 100.0F;
        float[] v = {
                // Top band
                -outer, y, -outer, r, g, b, 0, outer, y, -outer, r, g, b, 0,
                middle, y, -middle, r, g, b, leading / 2, -middle, y, -middle, r, g, b, trailing / 2,
                -middle, y, -middle, r, g, b, trailing / 2, middle, y, -middle, r, g, b, leading / 2,
                inner, y, -inner, r, g, b, leading, -inner, y, -inner, r, g, b, trailing,
                // Left band
                outer, y, -outer, r, g, b, 0, outer, y, outer, r, g, b, 0,
                middle, y, middle, r, g, b, leading / 2, middle, y, -middle, r, g, b, leading / 2,
                middle, y, -middle, r, g, b, leading / 2, middle, y, middle, r, g, b, leading / 2,
                inner, y, inner, r, g, b, leading, inner, y, -inner, r, g, b, leading,
                // Bottom band
                outer, y, outer, r, g, b, 0, -outer, y, outer, r, g, b, 0,
                -middle, y, middle, r, g, b, trailing / 2, middle, y, middle, r, g, b, leading / 2,
                middle, y, middle, r, g, b, leading / 2, -middle, y, middle, r, g, b, trailing / 2,
                -inner, y, inner, r, g, b, trailing, inner, y, inner, r, g, b, leading,
                // Right band
                -outer, y, outer, r, g, b, 0, -outer, y, -outer, r, g, b, 0,
                -middle, y, -middle, r, g, b, trailing / 2, -middle, y, middle, r, g, b, trailing / 2,
                -middle, y, middle, r, g, b, trailing / 2, -middle, y, -middle, r, g, b, trailing / 2,
                -inner, y, -inner, r, g, b, trailing, -inner, y, inner, r, g, b, trailing,
        };
        sky.coloredQuads(Sky.Blend.ALPHA, bothFaces(v, 7));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // destroyed and impacted bodies

    static @Nullable CBT_Destroyed destroyed(SolarSystem.Body body) {
        return CelestialTraits.getTrait(body.name, CBT_Destroyed.class, true);
    }

    /** Whether the star of {@code level}'s sky is destroyed ({@code getStar().hasTrait(CBT_Destroyed.class)}). */
    static boolean starDestroyed(SolarSystem.Body body) {
        return SkyMath.starDestroyed(body, true);
    }

    /** {@code renderCelestials}' destroyed branch, in place of the body's disc and overlays. */
    static void destroyedBody(Sky sky, SolarSystem.Body body, float size, float uvOffset, CBT_Destroyed d) {
        PoseStack pose = sky.pose();
        Sky.Texture surface = Sky.Texture.of(NtmSpace.MOD_ID, body.texture());
        double destroyedProgress = d.destProgress + size * 0.5;
        float alpha = (float) (1.0F - Math.min(1.0F, destroyedProgress / 100));
        Random random = new Random(12);
        int numQuads = 30;
        for (int i = 0; i < numQuads; i++) {
            double radius = (random.nextDouble() * size) * d.destProgress;
            double randomTheta = random.nextDouble() * Math.PI * 2;
            double randomPhi = random.nextDouble() * Math.PI;
            double randomX = radius * Math.sin(randomPhi) * Math.cos(randomTheta) * 0.7;
            double randomZ = radius * Math.cos(randomPhi) * 0.7;
            float randomRotation = random.nextFloat() * 360.0F;
            float uMin = (float) random.nextDouble();
            float vMin = (float) random.nextDouble();
            float uMax = (float) Math.min(uMin + (random.nextDouble() * 0.2), 1.0);
            float vMax = (float) Math.min(vMin + (random.nextDouble() * 0.2), 1.0);

            pose.pushPose();
            pose.translate(randomX * -0.05, 0, randomZ * -0.05);
            pose.mulPose(Axis.YP.rotationDegrees(randomRotation * d.destProgress * 0.05F));
            float q = (float) (size * random.nextDouble() * 0.1);
            sky.texturedQuads(surface, Sky.Blend.ADDITIVE, 1, 1, 1, 1,
                    -q, 100, -q, uMin, vMin, q, 100, -q, uMax, vMin, q, 100, q, uMax, vMax, -q, 100, q, uMin, vMax);
            pose.popPose();

            pose.pushPose();
            pose.translate(randomX * 0.04, 0, randomZ * 0.04);
            pose.mulPose(Axis.YP.rotationDegrees(randomRotation * d.destProgress * 0.05F));
            q = (float) (size * random.nextDouble() * 0.07);
            sky.texturedQuads(DESTROYED, Sky.Blend.ADDITIVE, 1, 1, 1, 1,
                    -q, 100, -q, uMin, vMin, q, 100, -q, uMax, vMin, q, 100, q, uMax, vMax, -q, 100, q, uMin, vMax);
            pose.popPose();
        }
        float shock = (float) ((d.destProgress * 0.5) * size * 0.1);
        wrapped(sky, SHOCKWAVE, Sky.Blend.ADDITIVE, 1, 1, 1, alpha, shock, uvOffset);
        wrapped(sky, SHOCK_FLARE, Sky.Blend.ADDITIVE, 1, 1, 1, Math.min(alpha * 2, 1.0F), size * 3, uvOffset);
    }

    /** {@code renderCelestials}' impact overlays ({@code CBT_Impact}), after the disc and its crescent. */
    static void impact(Sky sky, ClientLevel level, SolarSystem.Body body, float size, float uvOffset, float partialTicks) {
        CBT_Impact impact = CelestialTraits.getTrait(body.name, CBT_Impact.class, true);
        if (impact == null) return;
        double impactTime = (level.getGameTime() - impact.time) + partialTicks;
        double lavaAlpha = Math.min(impactTime * 0.1, 1.0);
        float impactSize = (float) ((impactTime * 0.1) * size * 0.035);
        double impactAlpha = 1.0 - Math.min(1.0, impactTime * 0.0015);
        float flareSize = size * 1.5F;
        double flareAlpha = 1.0 - Math.min(1.0, impactTime * 0.002);
        if (lavaAlpha > 0) wrapped(sky, IMPACT, Sky.Blend.ADDITIVE, 1, 1, 1, (float) lavaAlpha, size, uvOffset);
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.translate(-size * 0.5, 0, size * 0.4);
        // impact shockwave, increases in size and fades out
        if (impactAlpha > 0) wrapped(sky, SHOCKWAVE, Sky.Blend.ADDITIVE, 1, 1, 1, (float) impactAlpha, impactSize, 0);
        // impact flare, remains static in size and fades out
        if (flareAlpha > 0) wrapped(sky, SHOCK_FLARE, Sky.Blend.ADDITIVE, 1, 1, 1, (float) flareAlpha, flareSize, 0);
        pose.popPose();
    }

    /** A square of half-size {@code s} at y 100 with u from {@code uvOffset} to 1 + it (split at the wrap, as MunSky). */
    private static void wrapped(Sky sky, Sky.Texture texture, Sky.Blend blend, float r, float g, float b, float a, float s,
                                float uvOffset) {
        float wrap = uvOffset - (float) Math.floor(uvOffset);
        if (wrap == 0) {
            sky.texturedQuads(texture, blend, r, g, b, a,
                    -s, 100, -s, 0, 0, s, 100, -s, 1, 0, s, 100, s, 1, 1, -s, 100, s, 0, 1);
            return;
        }
        float split = -s + 2 * s * (1 - wrap);
        sky.texturedQuads(texture, blend, r, g, b, a,
                -s, 100, -s, wrap, 0, split, 100, -s, 1, 0, split, 100, s, 1, 1, -s, 100, s, wrap, 1,
                split, 100, -s, 0, 0, s, 100, -s, wrap, 0, s, 100, s, wrap, 1, split, 100, s, 0, 1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // satellites and stations

    /**
     * {@code render}'s objects in orbit, in the sun's frame (only while the sky's visibility is above 0.2): Kerbin's
     * delivery satellite, every satellite but the railguns ({@link WarSky}), the held satellite's preview and the
     * stations, all additive.
     */
    static void orbiters(Sky sky, ClientLevel level, SolarSystem.Body body, float solarAngle) {
        // JEFF BOZOS WOULD LIKE TO KNOW YOUR LOCATION ... to send you a pakedge :)))
        if (SpaceSkies.isKerbin(level)) {
            satellite(sky, solarAngle, BOBMAZON_SEED, 1.0F, 0.534F, 0.385F, SatelliteOrbit.DEFAULT_INCLINATION,
                    SatelliteOrbit.DEFAULT_ALTITUDE_KM, SatelliteOrbit.DEFAULT_PHASE_OFFSET, SatelliteOrbit.DEFAULT_IS_BLINKING,
                    SatelliteOrbit.DEFAULT_BLINK_PERIOD);
        }
        // Light up the sky
        SatelliteSky.View view = SatelliteSky.client();
        for (SatelliteSky.Sat sat : view.satellites()) {
            if (sat.type().equals("ntmspace:railgun")) continue; // SatelliteRailgun.render: WarSky
            SatelliteOrbit o = sat.orbit();
            satellite(sky, solarAngle, sat.freq(), o.colorR(), o.colorG(), o.colorB(), o.inclination(), o.altitude(),
                    o.phaseOffset(), o.blinking(), o.blinkPeriod());
        }
        heldPreview(sky, body, solarAngle);
        // Stations, too
        for (SatelliteSky.Station station : view.stations()) station(sky, station, solarAngle);
    }

    /** {@code SatelliteBase.renderDefault}. */
    static void satellite(Sky sky, float solarAngle, long seed, float r, float g, float b, float inclination, float altitude,
                          float phaseOffset, boolean blinking, float blinkPeriod) {
        double ticks = (double) System.currentTimeMillis() / 50.0D;
        float orbitAngle = SatelliteOrbit.applyPhaseOffsetToOrbitAngle(phaseOffset, altitude, (ticks / 600.0D) * -360.0D, 360.0F);
        float renderAltitude = Math.max(1.0F, altitude);
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(solarAngle * -360.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(inclination));
        pose.mulPose(Axis.XP.rotationDegrees(orbitAngle));
        float size = 0.5F;
        sky.texturedQuads(SATELLITE, Sky.Blend.ADDITIVE, r, g, b, blinkAlpha(blinking, blinkPeriod),
                -size, renderAltitude, -size, 0, 0,
                size, renderAltitude, -size, 0, 1,
                size, renderAltitude, size, 1, 1,
                -size, renderAltitude, size, 1, 0);
        pose.popPose();
    }

    /** {@code SatelliteBase.renderOrbitLine}: a 72-segment loop at the satellite's altitude, alpha 0.35. */
    static void orbitLine(Sky sky, float solarAngle, float r, float g, float b, float inclination, float altitude,
                          boolean blinking, float blinkPeriod) {
        float renderAltitude = Math.max(1.0F, altitude);
        float alpha = 0.35F * blinkAlpha(blinking, blinkPeriod);
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(solarAngle * -360.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(inclination));
        float[] loop = new float[72 * 3];
        for (int i = 0; i < 72; i++) {
            double angle = Math.PI * 2.0D * i / 72.0D;
            loop[i * 3] = 0;
            loop[i * 3 + 1] = (float) (renderAltitude * Math.cos(angle));
            loop[i * 3 + 2] = (float) (renderAltitude * Math.sin(angle));
        }
        sky.lines(Sky.Blend.ADDITIVE, r, g, b, alpha, true, loop);
        pose.popPose();
    }

    /** {@code SatelliteBase.getBlinkAlpha}: a saw from 1 to 0 over the blink period, by the wall clock. */
    static float blinkAlpha(boolean blinking, float blinkPeriod) {
        if (!blinking) return 1.0F;
        long cycleMillis = (long) (SatelliteOrbit.clampBlinkPeriod(blinkPeriod) * 1000.0F);
        if (cycleMillis <= 0L) return 1.0F;
        return 1.0F - (float) (System.currentTimeMillis() % cycleMillis) / cycleMillis;
    }

    /**
     * {@code renderHeldSatellitePreview}: a satellite item in the main hand shows where it would orbit, with its orbit
     * line, unless it is one that is sent to another body (relay, miners).
     */
    static void heldPreview(Sky sky, SolarSystem.Body body, float solarAngle) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack held = player.getMainHandItem();
        SatelliteType type = held.isEmpty() ? null : XSatelliteRegistry.typeFor(held);
        if (type == null || !SpaceSatellites.isSpace(type)) return;
        String target = SpaceSatellites.targetBody(type);
        if (target != null && !target.equals(body.name)) return;
        SatelliteOrbit o = SpaceSatellites.orbit(held);
        orbitLine(sky, solarAngle, o.colorR(), o.colorG(), o.colorB(), o.inclination(), o.altitude(), o.blinking(), o.blinkPeriod());
        satellite(sky, solarAngle, ISatChip.getFreqS(held), o.colorR(), o.colorG(), o.colorB(), o.inclination(), o.altitude(),
                o.phaseOffset(), o.blinking(), o.blinkPeriod());
    }

    /** {@code renderStation}: a {@code station.png} quad on a path seeded by the station's grid position, 80 s a lap. */
    static void station(Sky sky, SatelliteSky.Station station, float solarAngle) {
        long seed = station.x() * 1024L + station.z();
        double ticks = (double) (System.currentTimeMillis() % (1600 * 50)) / 50;
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(solarAngle * -360.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-40.0F + (float) (seed % 800) * 0.1F - 5.0F));
        pose.mulPose(Axis.YP.rotationDegrees((float) (seed % 50) * 0.1F - 20.0F));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (seed % 80) * 0.1F - 2.5F));
        pose.mulPose(Axis.XP.rotationDegrees((float) ((ticks / 1600.0D) * -360.0D)));
        float size = 0.8F;
        sky.texturedQuads(STATION, Sky.Blend.ADDITIVE, 0.8F, 1, 1, 1,
                -size, 100, -size, 0, 0,
                size, 100, -size, 0, 1,
                size, 100, size, 1, 1,
                -size, 100, size, 1, 0);
        pose.popPose();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // charged dust

    /**
     * {@code renderSunset}'s charged dust over an airless horizon (below 0.05 atm, while its faint sunrise glow shows):
     * 1,024 points scattered by a fixed seed, drifting with the body's time. 1.7 bound the surface sheet but supplied no
     * UVs, so its texel at (0,0) modulates the random tint and glow alpha, additive with a zero blend constant.
     * In the sunset fan's frame (the caller's pose).
     */
    static void chargedDust(Sky sky, ClientLevel level, SolarSystem.Body body, float glowAlpha, float partialTicks) {
        String surface = surfaceTexture(body);
        if (surface == null) return;
        Random rand = new Random(0);
        double time = ((double) dev.golem.core.world.TimeOfDay.get(level) + partialTicks) * 0.002;
        float r = 0.5F + rand.nextFloat() * 0.5F;
        float g = 0.5F + rand.nextFloat() * 0.5F;
        float b = 0.5F + rand.nextFloat() * 0.5F;
        float a = Math.min(rand.nextFloat() * glowAlpha * 4.0F, 1.0F);
        int texel = SURFACE_TEXELS.computeIntIfAbsent(surface, CelestialSkyObjects::surfaceTexel);
        r *= ((texel >> 16) & 255) / 255F;
        g *= ((texel >> 8) & 255) / 255F;
        b *= (texel & 255) / 255F;
        a *= ((texel >>> 24) & 255) / 255F;
        float[] points = new float[1024 * 3];
        for (int i = 0; i < 1024; i++) {
            points[i * 3] = (float) (rand.nextGaussian() * 50);
            points[i * 3 + 1] = 100;
            double z = Math.abs(rand.nextGaussian() * 20) + time;
            points[i * 3 + 2] = (float) -((z % Math.abs(rand.nextGaussian())) * 20);
        }
        sky.points(null, Sky.Blend.ADDITIVE, r, g, b, a, points);
    }

    // ---------------------------------------------------------------------------------------------------------------

    /** A quad list followed by the same quads in reverse winding: 1.7's {@code GL_CULL_FACE} off on a culled seam. */
    static float[] bothFaces(float[] quads, int stride) {
        int vertices = quads.length / stride;
        float[] out = new float[quads.length * 2];
        System.arraycopy(quads, 0, out, 0, quads.length);
        int o = quads.length;
        for (int q = 0; q < vertices / 4; q++) {
            for (int c = 3; c >= 0; c--) {
                System.arraycopy(quads, (q * 4 + c) * stride, out, o, stride);
                o += stride;
            }
        }
        return out;
    }
}
