package com.ntmspace.client;

import net.minecraft.resources.ResourceKey;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.WarContent;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.ThatmoBattlefield;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.render.Sky;
import dev.golem.core.render.gem.Geometry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Thatmo's own sky, hbm-space's {@code SkyProviderThatmo} (with the client half of {@code WorldProviderThatmo}): the
 * battle over the moon, drawn on top of {@link MunSky}'s celestial sky.
 *
 * <ul>
 *   <li>{@link #model}, 1.7 {@code render3DModel} right after the celestial bodies while Thatmo has its battlefield trait:
 *       the railgun satellite ({@code railgun_main.obj}) as a black silhouette, ten times up at (-35, 4.5, 100) in the bare
 *       sky (1.7 drew it opaque in black, so neither its texture nor its own depth order shows).</li>
 *   <li>{@link #specialEffects}, 1.7 {@code renderSpecialEffects} after the body's rings and before an infected body's
 *       flesh sky (so vanilla's dark disc still covers it below the horizon, as 1.7's sky list did), every frame on
 *       Thatmo: the railgun's flash ({@code shockwave} and the space {@code flare}, growing with {@code flashd} and fading
 *       out), the shield it hits ({@code shockwave} and {@code cens} planes at (21.5, 33, -28)), Thatmo's meteors and
 *       their smoke (camera-facing quads pulled in to the render distance and scaled by their descent, divided by 41 and
 *       82), the railgun beam (three {@code BeamPronter} beams in {@code 0x202060}), the orange beam that grows into the
 *       shield, and six showers of 17 short orange beams each falling with {@code altitude} (seeded {@code Random(42)},
 *       spread by {@code randPos}).</li>
 *   <li>The battlefield's state ({@link ThatmoBattlefield}) ticks on the client in Thatmo with the trait, with its
 *       sounds ({@code misc.fireflash}, {@code misc.impact}); its flash and shield terms brighten Thatmo's sky colour, fog
 *       colour and sun brightness ({@link CelestialSkyColors}).</li>
 * </ul>
 *
 * Blending: all of it is additive - the flash, planes and meteors with the sky's additive state of that point
 * (1.7 {@code OpenGlHelper.glBlendFunc(SRC_ALPHA, ONE, ...)} from the sun on, as {@link WarSky}'s planes), the beams with
 * {@code BeamPronter}'s own {@code GL_SRC_ALPHA, GL_ONE} - so their order does not change the picture and same-coloured
 * quads are drawn as one batch. The beams are drawn from both sides (BeamPronter turns culling off). 1.7 also redrew
 * the black model before each arrived war projectile's planes ({@code SkyProviderCelestial}'s war loop): that is
 * {@link WarSky#shockwaves}. The server half of {@code updateWeather} (B2X carpet jets every 8 s near each player) is
 * {@link com.ntmspace.war.ThatmoStrikes}.
 */
public final class ThatmoSky {

    private ThatmoSky() {}

    /** The client's battlefield (1.7: statics of {@code WorldProviderThatmo}, kept when leaving Thatmo). */
    public static final ThatmoBattlefield STATE = ThatmoBattlefield.CLIENT_SKY;
    private static final Random RAND = new Random();

    private static final Sky.Texture SHOCKWAVE = Sky.Texture.of("hbm", "textures/particle/shockwave.png");
    private static final Sky.Texture CENS = Sky.Texture.of(NtmSpace.MOD_ID, "textures/particle/cens.png");
    private static final Sky.Texture SPACE_FLARE = Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/flare.png");
    private static final Sky.Texture FLARE = Sky.Texture.of("hbm", "textures/particle/flare.png");
    private static final Sky.Texture PARTICLE_BASE = Sky.Texture.of("hbm", "textures/particle/particle_base.png");

    private static final AtomicLong MODEL = new AtomicLong();
    private static final AtomicLong FRAMES = new AtomicLong();
    private static final AtomicLong FLASH = new AtomicLong();
    private static final AtomicLong SHIELD = new AtomicLong();
    private static final AtomicLong BILLBOARDS = new AtomicLong();
    private static final AtomicLong BEAM_QUADS = new AtomicLong();

    public static void init() {
        ClientTicks.onEndTick(ThatmoSky::tick);
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmspace_thatmo", ThatmoSky::query);
    }

    /** The dimension gate shared by the native battlefield render and tick routes. */
    public static boolean isThatmo(ResourceKey<Level> dimension) {
        return dimension.equals(SpaceDimensions.THATMO);
    }

    static boolean isThatmo(Level level) {
        return level != null && isThatmo(level.dimension());
    }

    private static boolean battlefield(Level level) {
        return isThatmo(level) && CelestialTraits.hasTrait(level, CelestialBodyTrait.CBT_Battlefield.class);
    }

    /** 1.7 {@code updateWeather} on the client, in Thatmo with the battlefield trait. */
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || !battlefield(level)) return;
        int sounds = STATE.tick(RAND, () -> level.getRandom().nextFloat(), player.getX(), player.getZ());
        if ((sounds & ThatmoBattlefield.SOUND_FIREFLASH) != 0) player.playSound(WarContent.MISC_FIREFLASH.get(), 10F, 1F);
        if ((sounds & ThatmoBattlefield.SOUND_IMPACT) != 0) player.playSound(WarContent.MISC_IMPACT.get(), 10F, 1F);
    }

    /** {@code WorldProviderThatmo.getSkyColor/getFogColor/getSunBrightness}'s addition: the flash plus the shield glow. */
    static float lightAdd(Level level) {
        return ThatmoBattlefield.lightAdd(level);
    }

    // ------------------------------------------------------------------------------------------------ render3DModel

    private static float[] railgun;

    /** {@code SkyProviderThatmo.render3DModel}: the railgun satellite's black silhouette (battlefield only). */
    static void model(Sky sky) {
        if (!battlefield(sky.level())) return;
        float[] quads = railgun();
        if (quads.length == 0) return;
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.translate(-35, 4.5, 100);
        pose.scale(10, 10, 10);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F)); // glRotated(180, 0, 5, 0)
        pose.mulPose(new org.joml.Quaternionf().rotateAxis(90F * Mth.DEG_TO_RAD, axis(-12, 5, 0)));
        // glColor4f(0, 0, 0, 1), blending off: black whatever the texture says
        sky.quads(Sky.Blend.ALPHA, 0F, 0F, 0F, 1F, quads);
        pose.popPose();
        MODEL.incrementAndGet();
    }

    /** {@code railgun_main.obj} (1.7 {@code ResourceManager.sat_rail}) as positions, each triangle a quad. */
    private static float[] railgun() {
        if (railgun != null) return railgun;
        try {
            Geometry geometry = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/sat/railgun_main.obj")).all();
            float[] v = geometry.vertices();
            int[] idx = geometry.indices();
            float[] out = new float[idx.length / 3 * 4 * 3];
            int o = 0;
            for (int t = 0; t + 2 < idx.length; t += 3) {
                for (int c = 0; c < 4; c++) {
                    int at = idx[t + Math.min(c, 2)] * Geometry.FLOATS_PER_VERTEX;
                    out[o++] = v[at];
                    out[o++] = v[at + 1];
                    out[o++] = v[at + 2];
                }
            }
            railgun = out;
        } catch (RuntimeException e) {
            NtmSpace.LOG.error("Could not load Thatmo's railgun model", e);
            railgun = new float[0];
        }
        return railgun;
    }

    // ------------------------------------------------------------------------------------------------ renderSpecialEffects

    /** Additive quads of one colour (and texture), in the sky pose of {@link #specialEffects}' entry. */
    private static final class Batch {
        final Sky.Texture texture;
        final float r, g, b, a;
        float[] v = new float[256];
        int n;

        Batch(Sky.Texture texture, float r, float g, float b, float a) {
            this.texture = texture;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        void vertex(Matrix4f m, double x, double y, double z, float u, float tv, Vector3f tmp) {
            int stride = texture == null ? 3 : 5;
            if (n + stride > v.length) v = java.util.Arrays.copyOf(v, v.length * 2);
            m.transformPosition((float) x, (float) y, (float) z, tmp);
            v[n++] = tmp.x;
            v[n++] = tmp.y;
            v[n++] = tmp.z;
            if (texture != null) {
                v[n++] = u;
                v[n++] = tv;
            }
        }
    }

    private static final class Batches {
        final Map<String, Batch> map = new LinkedHashMap<>();
        final Vector3f tmp = new Vector3f();

        Batch get(Sky.Texture texture, float r, float g, float b, float a) {
            String key = (texture == null ? "-" : texture.toString()) + "|" + r + "|" + g + "|" + b + "|" + a;
            return map.computeIfAbsent(key, k -> new Batch(texture, r, g, b, a));
        }

        void flush(Sky sky, boolean bothFacesUntextured) {
            for (Batch batch : map.values()) {
                if (batch.n == 0) continue;
                float[] v = java.util.Arrays.copyOf(batch.v, batch.n);
                if (batch.texture == null) {
                    sky.quads(Sky.Blend.ADDITIVE, batch.r, batch.g, batch.b, batch.a,
                            bothFacesUntextured ? CelestialSkyObjects.bothFaces(v, 3) : v);
                } else {
                    sky.texturedQuads(batch.texture, Sky.Blend.ADDITIVE, batch.r, batch.g, batch.b, batch.a, v);
                }
            }
            map.clear();
        }
    }

    /** {@code SkyProviderThatmo.renderSpecialEffects}, in 1.7's order (see the class comment for the blending). */
    static void specialEffects(Sky sky) {
        ClientLevel level = sky.level();
        if (!isThatmo(level)) return;
        FRAMES.incrementAndGet();
        float partialTicks = sky.partialTick();
        ThatmoBattlefield s = STATE;
        float alpha = s.flashAlpha();
        Batches textured = new Batches();
        Batches beams = new Batches();

        // the railgun's flash: shockwave, then the flare at half the size
        float var14 = s.flashd * 2 + partialTicks;
        Matrix4f m = new Matrix4f();
        rotate(m, 180.0, 0.0, 5.0, 0.0);
        rotate(m, 90.0, -12.0, 7.3F, -4.0);
        if (alpha > 0) {
            skyQuad(textured.get(SHOCKWAVE, 1, 1, 1, alpha), m, var14, textured.tmp);
            skyQuad(textured.get(SPACE_FLARE, 1, 1, 1, alpha), m, var14 * 0.5F, textured.tmp);
            FLASH.incrementAndGet();
        }

        float alt = s.altitude + partialTicks;
        float rnd = s.randPos;
        float beamscale = s.scale;
        float shieldscale = s.shield;
        float waveinterp = s.nmass;
        float shieldinterp = s.shielde;
        float shielalpha = s.csyw + partialTicks;
        float alphad = 1.0F - Math.min(1.0F, waveinterp / 100);
        float alpd = 1.0F - Math.min(1.0F, shielalpha / 100);

        // the shield: a shockwave plane and a cens plane at (21.5, 33, -28)
        if (shieldscale > 0 || shieldinterp > 0) SHIELD.incrementAndGet();
        if (shieldscale > 0) {
            m = shieldFrame(shieldscale);
            plane(textured.get(SHOCKWAVE, 1, 1, 1, alphad), m, textured.tmp);
        }
        if (shieldinterp > 0) {
            m = shieldFrame(shieldinterp);
            plane(textured.get(CENS, 1, 1, 1, alpd), m, textured.tmp);
        }

        // Thatmo's meteors (none in 'fragments' in 1.7) and their smoke
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera != null) {
            Vec3 pos = camera.getEyePosition(partialTicks);
            float viewY = camera.getViewYRot(partialTicks);
            float viewX = camera.getViewXRot(partialTicks);
            int renderDistance = mc.options.renderDistance().get();
            float rain = level.getRainLevel(partialTicks);
            for (ThatmoBattlefield.Meteor meteor : s.meteors) billboard(textured, meteor, pos, viewY, viewX, renderDistance, 41, rain);
            for (ThatmoBattlefield.Meteor fragment : s.fragments) billboard(textured, fragment, pos, viewY, viewX, renderDistance, 82, rain);
            for (ThatmoBattlefield.Meteor smoke : s.smoke) billboard(textured, smoke, pos, viewY, viewX, renderDistance, 82, rain);
        }

        // the railgun beam
        m = new Matrix4f();
        m.translate(16.5F, 28.5F, 100);
        m.scale(10, 10, 10);
        rotate(m, -63.5, 0.0, 0.0, 1.0);
        double flashLength = s.flashd * 0.5;
        prontBeam(beams, m, 0, flashLength, 0, true, 0x202060, 0x202060, 0, 1, 0F, 6, (float) 0.2 * 0.2F, alpha);
        prontBeam(beams, m, 0, flashLength, 0, true, 0x202060, 0x202060, 0, 1, 0F, 6, (float) 0.2 * 0.6F, alpha);
        prontBeam(beams, m, 0, flashLength, 0, false, 0x202060, 0x202060, (int) (level.getGameTime() / 5) % 1000, 25, 0.2F, 6,
                (float) 0.2 * 0.1F, alpha);

        // the orange beam growing into the shield
        m = new Matrix4f();
        m.translate(145, 75.5F, -15);
        rotate(m, 90.0, -10.0, -1.0, 50.0);
        rotate(m, 20.0, -0.0, -1.0, 1.0);
        m.scale(6, 6, 6);
        prontBeam(beams, m, 0, beamscale, 0, true, 0xFF9000, 0xFF9000, 0, 2, 0F, 2, alphad * 0.1F, 0.5F);

        // six showers of 17 short beams, one Random(42) through all of them
        Random random = new Random(42);
        m = new Matrix4f();
        rotate(m, 25.0 * rnd, -12.0, 5.0 + rnd, 2.0 + rnd);
        rotate(m, 15.0 * rnd, -11.0, 20.0 + rnd, 1.0 + rnd);
        m.translate(50 - rnd, alt - rnd * rnd, -10 * rnd);
        for (int i = 0; i < 17; i++) {
            m.translate(-rnd, 0 - random.nextInt(20), rnd);
            shower(beams, m);
        }

        m = new Matrix4f();
        rotate(m, 45.0 * rnd, -12.0, 5.0, 0.0);
        m.translate(-40 + rnd, alt - 50 * rnd, -90 * rnd);
        for (int i = 0; i < 17; i++) {
            m.translate(-rnd, 0 - random.nextInt(20), -1 - rnd);
            shower(beams, m);
        }

        m = new Matrix4f();
        rotate(m, -90.0 * rnd, -12.0, 5.0, -20 + -45);
        m.translate(-10 * rnd, rnd + alt - 110, -80 + rnd);
        for (int i = 0; i < 17; i++) {
            m.translate(-rnd, 0 - random.nextInt(20), -1 - rnd);
            shower(beams, m);
        }

        m = new Matrix4f();
        rotate(m, -90.0 * 0.5961033, -11.0, 5.0, -20 + -45);
        m.translate(80 * rnd, rnd + alt - 110, 55 + rnd);
        for (int i = 0; i < 17; i++) {
            m.translate(-rnd, 0 - random.nextInt(20), -1 - rnd);
            shower(beams, m);
        }

        m = new Matrix4f();
        rotate(m, -25.0 - rnd, -16.0, 5.0, 0.0);
        m.translate(-60 - rnd, alt - 150 * rnd, 20 * rnd);
        for (int i = 0; i < 17; i++) {
            m.translate(-rnd, 0 - random.nextInt(15), rnd);
            shower(beams, m);
        }

        m = new Matrix4f();
        rotate(m, -65.0 - rnd, -12.0, 5.0, 0.0);
        m.translate(50 + rnd, alt - 150 * rnd, -80 * rnd);
        for (int i = 0; i < 17; i++) {
            m.translate(rnd, 0 - random.nextInt(15), -rnd);
            shower(beams, m);
        }

        textured.flush(sky, false);
        for (Batch batch : beams.map.values()) BEAM_QUADS.addAndGet(batch.n / 12);
        beams.flush(sky, true);
    }

    /** One beam of a shower: {@code prontBeam((0, 5, 0), SPIRAL, SOLID, 0xFF9000, 0xFF9000, 0, 1, 0, 6, 0.04, 0.5)}. */
    private static void shower(Batches beams, Matrix4f m) {
        prontBeam(beams, m, 0, 5, 0, true, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
    }

    /** {@code glTranslated(21.5, 33, -28)}, {@code glScaled(scale)}, the shield's two turns. */
    private static Matrix4f shieldFrame(float scale) {
        Matrix4f m = new Matrix4f();
        m.translate(21.5F, 33, -28);
        m.scale(scale, scale, scale);
        rotate(m, 90.0, -10.0, -1.0, 50.0);
        rotate(m, 20.0, -0.0, -1.0, 1.0);
        return m;
    }

    /** The flash quad at y 100, {@code half} across each way, UV 0..1 (1.7's vertex order). */
    private static void skyQuad(Batch batch, Matrix4f m, float half, Vector3f tmp) {
        batch.vertex(m, -half, 100, -half, 0, 0, tmp);
        batch.vertex(m, half, 100, -half, 1, 0, tmp);
        batch.vertex(m, half, 100, half, 1, 1, tmp);
        batch.vertex(m, -half, 100, half, 0, 1, tmp);
    }

    /** {@code ResourceManager.plane} as one quad in its winding (see {@link WarSky}). */
    private static void plane(Batch batch, Matrix4f m, Vector3f tmp) {
        batch.vertex(m, 1, 0, 1, 1, 0, tmp);
        batch.vertex(m, 1, 0, -1, 1, 1, tmp);
        batch.vertex(m, -1, 0, -1, 0, 1, tmp);
        batch.vertex(m, -1, 0, 1, 0, 0, tmp);
    }

    /**
     * A meteor's or smoke puff's camera-facing quad: pulled in to the render distance along its direction from the eye,
     * scaled by {@code (1517 d - d^2) / divisor} over its distance ({@code d} its descent from y 2017); the meteor with
     * {@code flare} ({@code renderGlow(.., 1, 1, 1, ..)}: always full white), the smoke with {@code particle_base} in
     * {@code (0.6 dark, 0.6 dark, dark)}, darkening over 35 ticks.
     */
    private static void billboard(Batches batches, ThatmoBattlefield.Meteor meteor, Vec3 pos, float viewY, float viewX,
                                  int renderDistance, double divisor, float rain) {
        Vec3 vec = new Vec3(meteor.posX - pos.x, meteor.posY - pos.y, meteor.posZ - pos.z);
        double length = vec.length();
        double l = Math.min(renderDistance * 16, length);
        Vec3 n = vec.normalize();
        double descent = 2017d - meteor.posY;
        double quadratic = (-1 * Math.pow(descent, 2) + (1517 * descent)) / divisor;
        float scalar = (float) (quadratic / length);
        Matrix4f m = new Matrix4f();
        m.translate((float) (n.x * l), (float) (n.y * l), (float) (n.z * l));
        m.scale(scalar, scalar, scalar);
        m.rotate((180.0F - viewY) * Mth.DEG_TO_RAD, 0, 1, 0);
        m.rotate(-viewX * Mth.DEG_TO_RAD, 1, 0, 0);
        Batch batch;
        if (meteor.type == ThatmoBattlefield.MeteorType.SMOKE) {
            float dark = 1f - Math.min(((float) (meteor.age) / (float) (100f * 0.35F)), 1f);
            batch = batches.get(PARTICLE_BASE, (float) (0.6 * dark), (float) (0.6 * dark), dark, 1);
        } else {
            // renderGlow(flare, x = 1, y = 1, z = 1, partialTicks): x and y are the 1s, so 'entry' is always 1
            double x = 1, y = 1;
            double near = 0.51d * (Math.min(40000f, Math.max(0d, y - 35000d)) / 40000d);
            float entry = (float) (near * (1d - rain) + (1d - (Math.min(200d, Math.max(0d, x - 2017d)) / 200f)));
            batch = batches.get(FLARE, entry, entry, entry, entry);
        }
        float f4 = 1.0F, f5 = 0.5F, f6 = 0.25F;
        batch.vertex(m, 0.0F - f5, 0.0F - f6, 0, 1, 0, batches.tmp);
        batch.vertex(m, f4 - f5, 0.0F - f6, 0, 0, 0, batches.tmp);
        batch.vertex(m, f4 - f5, f4 - f6, 0, 0, 1, batches.tmp);
        batch.vertex(m, 0.0F - f5, f4 - f6, 0, 1, 1, batches.tmp);
        BILLBOARDS.incrementAndGet();
    }

    /**
     * hbm-space's {@code BeamPronter.prontBeam} with {@code EnumBeamType.SOLID} at {@code m}: the skeleton's yaw and pitch,
     * then per segment {@code layers} square prisms of half-width {@code thickness / layers * j}, each layer coloured
     * between the outer and the inner colour ({@code (j - 1) / (layers - 1)}), alpha {@code alpha}; the segment ends wobble
     * by {@code size} - a spiral by 45 degrees a segment from {@code start} degrees, a random wave by two turns from
     * {@code Random(start)}.
     */
    static void prontBeam(Batches out, Matrix4f m, double skX, double skY, double skZ, boolean spiral, int outerColor,
                          int innerColor, int start, int segments, float size, int layers, float thickness, float alpha) {
        Matrix4f b = new Matrix4f(m);
        float sYaw = (float) (Math.atan2(skX, skZ) * 180F / Math.PI);
        float sqrt = (float) Math.sqrt(skX * skX + skZ * skZ);
        float sPitch = (float) (Math.atan2(skY, (double) sqrt) * 180F / Math.PI);
        b.rotate(180 * Mth.DEG_TO_RAD, 0, 1, 0);
        b.rotate(sYaw * Mth.DEG_TO_RAD, 0, 1, 0);
        b.rotate((sPitch - 90) * Mth.DEG_TO_RAD, 1, 0, 0);

        Random rand = new Random(start);
        double length = Math.sqrt(skX * skX + skY * skY + skZ * skZ);
        double segLength = length / segments;
        double lastX = 0, lastY = 0, lastZ = 0;
        for (int i = 0; i <= segments; i++) {
            Vec3 spinner = new Vec3(size, 0, 0);
            if (spiral) {
                spinner = spinner.yRot((float) Math.PI * (float) start / 180F);
                spinner = spinner.yRot((float) Math.PI * 45F / 180F * i);
            } else {
                spinner = spinner.yRot((float) Math.PI * 2 * rand.nextFloat());
                spinner = spinner.yRot((float) Math.PI * 2 * rand.nextFloat());
            }
            double pX = spinner.x;
            double pY = segLength * i + spinner.y;
            double pZ = spinner.z;
            if (i > 0) {
                float radius = thickness / layers;
                for (int j = 1; j <= layers; j++) {
                    float inter = (float) (j - 1) / (float) (layers - 1);
                    int r1 = (outerColor & 0xFF0000) >> 16, g1 = (outerColor & 0x00FF00) >> 8, b1 = outerColor & 0x0000FF;
                    int r2 = (innerColor & 0xFF0000) >> 16, g2 = (innerColor & 0x00FF00) >> 8, b2 = innerColor & 0x0000FF;
                    int r = (int) (r1 + (r2 - r1) * inter), g = (int) (g1 + (g2 - g1) * inter), bl = (int) (b1 + (b2 - b1) * inter);
                    Batch batch = out.get(null, r / 255.0f, g / 255.0f, bl / 255.0f, alpha);
                    double w = radius * j;
                    Vector3f t = out.tmp;
                    batch.vertex(b, lastX + w, lastY, lastZ + w, 0, 0, t);
                    batch.vertex(b, lastX + w, lastY, lastZ - w, 0, 0, t);
                    batch.vertex(b, pX + w, pY, pZ - w, 0, 0, t);
                    batch.vertex(b, pX + w, pY, pZ + w, 0, 0, t);
                    batch.vertex(b, lastX - w, lastY, lastZ + w, 0, 0, t);
                    batch.vertex(b, lastX - w, lastY, lastZ - w, 0, 0, t);
                    batch.vertex(b, pX - w, pY, pZ - w, 0, 0, t);
                    batch.vertex(b, pX - w, pY, pZ + w, 0, 0, t);
                    batch.vertex(b, lastX + w, lastY, lastZ + w, 0, 0, t);
                    batch.vertex(b, lastX - w, lastY, lastZ + w, 0, 0, t);
                    batch.vertex(b, pX - w, pY, pZ + w, 0, 0, t);
                    batch.vertex(b, pX + w, pY, pZ + w, 0, 0, t);
                    batch.vertex(b, lastX + w, lastY, lastZ - w, 0, 0, t);
                    batch.vertex(b, lastX - w, lastY, lastZ - w, 0, 0, t);
                    batch.vertex(b, pX - w, pY, pZ - w, 0, 0, t);
                    batch.vertex(b, pX + w, pY, pZ - w, 0, 0, t);
                }
            }
            lastX = pX;
            lastY = pY;
            lastZ = pZ;
        }
    }

    /** {@code glRotated(angle, x, y, z)}: GL normalises the axis. */
    private static void rotate(Matrix4f m, double angle, double x, double y, double z) {
        Vector3f a = axis(x, y, z);
        m.rotate((float) Math.toRadians(angle), a.x, a.y, a.z);
    }

    private static Vector3f axis(double x, double y, double z) {
        return new Vector3f((float) x, (float) y, (float) z).normalize();
    }

    /**
     * Harness query {@code ntmspace_thatmo}: the battlefield state on this client and what the sky drew so far (frames of
     * special effects, model silhouettes, flash and shield frames, meteor and smoke quads, beam quads). With
     * {@code params.charge} it first sets the charge counter (a test hook: 1001 starts the railgun's flash at once, 299 the
     * shield hit), as 1.7's counter would reach it after its 50 s wait.
     */
    private static com.google.gson.JsonObject query(Minecraft mc, com.google.gson.JsonObject params) {
        if (params.has("charge")) STATE.chargetime = params.get("charge").getAsInt();
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        ClientLevel level = mc.level;
        json.addProperty("thatmo", isThatmo(level));
        json.addProperty("battlefield", level != null && battlefield(level));
        json.addProperty("chargetime", STATE.chargetime);
        json.addProperty("flashd", STATE.flashd);
        json.addProperty("scale", STATE.scale);
        json.addProperty("shield", STATE.shield);
        json.addProperty("shielde", STATE.shielde);
        json.addProperty("csyw", STATE.csyw);
        json.addProperty("altitude", STATE.altitude);
        json.addProperty("rand_pos", STATE.randPos);
        json.addProperty("meteors", STATE.meteors.size());
        json.addProperty("smoke", STATE.smoke.size());
        json.addProperty("light_add", level != null ? lightAdd(level) : 0);
        json.addProperty("frames", FRAMES.get());
        json.addProperty("model", MODEL.get());
        json.addProperty("flash", FLASH.get());
        json.addProperty("shield_frames", SHIELD.get());
        json.addProperty("billboards", BILLBOARDS.get());
        json.addProperty("beam_quads", BEAM_QUADS.get());
        return json;
    }
}
