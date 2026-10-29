package com.ntmspace.client;

import com.ntmspace.world.SkyMath;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.war.WarSatellites;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.Ids;
import dev.golem.core.render.Sky;
import dev.golem.core.render.Skyboxes;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The war in a celestial body's sky (hbm-space {@code SkyProviderCelestial} and {@code WorldProviderCelestial}):
 *
 * <ul>
 *   <li>Every war projectile of the body that has arrived ({@code travel <= 0}) draws a {@code shockwave} plane and,
 *       at 0.4 of its size, a {@code cens} plane, at (x + 70, y, z + 50) in the bare sky, growing with its flash and
 *       fading out as it reaches 100 (additive). These are drawn in the celestial pass ({@link #shockwaves}, from
 *       {@link MunSky}), after Thatmo's railgun silhouette and before the dark disc, with the silhouette drawn again before
 *       each projectile, as 1.7's sky did.</li>
 *   <li>While a projectile's impact animation runs ({@code animtime > 0}) the sky colour gains {@code (100 - flash) *
 *       0.5} per projectile ({@link #skyFlash}, before the thinning by pressure; an airless sky stays black) and the
 *       light map's sun brightness {@code 100 - flash} ({@link #insideBrightness}).</li>
 *   <li>The invasion's meteors ({@code WorldProviderCelestial.Meteor}): from y 2017 they fall at (-31.2, -20.8, 20) a
 *       tick, leaving a smoke puff each tick (60 ticks), dying below y 500; drawn as camera-facing quads pulled in to the
 *       render distance and scaled by their descent, the meteor with {@code flare}, the smoke with
 *       {@code particle_base} darkening to blue.</li>
 *   <li>The railgun satellites ({@code SatelliteRailgun.render}): {@code railgun_main.obj} five times up in the sun's
 *       frame at a spot from its frequency, with its firing flash ({@code flare} and {@code shockwave} planes) while
 *       {@code interp} runs, and its beams ({@code BeamPronter}: two straight and a zigzag, additive prisms), when the
 *       sky's visibility is above 0.2. In play a railgun never fires (see {@link WarSatellites}), so the flash and the
 *       beams show only after {@code /ntmwar railgun <freq> fire}. The railguns come from the general satellite sky
 *       cache ({@link com.ntmspace.satellite.SatelliteSky}).</li>
 * </ul>
 *
 * The rest is drawn after the sky (Golem {@link Skyboxes#afterSky}); none of it is depth-tested in 1.7's sky either. The model is
 * drawn opaque and unlit with depth off, so its own faces are not depth-sorted (1.7 cleared and used the depth buffer).
 */
public final class WarSky implements Skyboxes.Skybox {

    private static final Sky.Texture SHOCKWAVE = Sky.Texture.of("hbm", "textures/particle/shockwave.png");
    private static final Sky.Texture CENS = Sky.Texture.of(NtmSpace.MOD_ID, "textures/particle/cens.png");
    private static final Sky.Texture FLARE = Sky.Texture.of("hbm", "textures/particle/flare.png");
    private static final Sky.Texture SPACE_FLARE = Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/flare.png");
    private static final Sky.Texture PARTICLE_BASE = Sky.Texture.of("hbm", "textures/particle/particle_base.png");
    private static final Sky.Texture RAILGUN = Sky.Texture.of(NtmSpace.MOD_ID, "textures/models/sat/railgun_main.png");

    static void init() {
        Skyboxes.SKYBOXES.register(SpaceSkies.PHASE, new WarSky());
    }

    // ------------------------------------------------------------------------------------------------ the flash

    /** {@code WorldProviderCelestial.getSkyColor}'s war term: {@code (100 - flash) * 0.5} per running impact. */
    public static float skyFlash(Level level) {
        float add = 0;
        CBT_War war = CelestialTraits.getTrait(level, CBT_War.class);
        if (war == null) return 0;
        for (CBT_War.Projectile projectile : war.getProjectiles()) {
            if (projectile.getAnimtime() > 0) add += (100 - projectile.getFlashtime()) * 0.5F;
        }
        return add;
    }

    /** {@code WorldProviderCelestial.getSunBrightness}'s "brightness inside of the atmosphere": {@code 100 - flash} each. */
    public static float insideBrightness(Level level) {
        return SkyMath.insideBrightness(CelestialTraits.getTrait(level, CBT_War.class));
    }

    // ------------------------------------------------------------------------------------------------ meteors

    /** hbm-space {@code WorldProviderCelestial.MeteorType}. */
    public enum MeteorType { STANDARD, FRAGMENT, SMOKE }

    /** hbm-space {@code WorldProviderCelestial.Meteor}. */
    public static final class Meteor {
        public double posX, posY, posZ;
        public double prevPosX, prevPosY, prevPosZ;
        public double motionX, motionY, motionZ;
        public boolean isDead = false;
        public long age;
        public MeteorType type;

        public Meteor(double posX, double posY, double posZ) {
            this(posX, posY, posZ, MeteorType.STANDARD, -31.2, -20.8, 20);
        }

        public Meteor(double posX, double posY, double posZ, MeteorType type, double motionX, double motionY, double motionZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.type = type;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
        }

        Meteor update(Random rand) {
            if (this.posY <= 500 && this.type != MeteorType.SMOKE) this.isDead = true;
            if (this.type == MeteorType.SMOKE) {
                this.age++;
                if (this.age >= 60) this.isDead = true;
            }
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
            if (this.type != MeteorType.SMOKE && this.type != MeteorType.FRAGMENT) {
                return new Meteor((this.posX + rand.nextInt(16)) - 8, (this.posY + rand.nextInt(16)), (this.posZ + rand.nextInt(16)) - 8,
                        MeteorType.SMOKE, 0, 0, 0);
            }
            return null;
        }
    }

    /** 1.7 {@code WorldProviderCelestial.meteors}: one list on the client, whatever the body. */
    public static final List<Meteor> METEORS = new ArrayList<>();
    private static final Random RAND = new Random();

    /** 1.7 {@code Meteor.addMeteor}: a meteor up to 8,000 blocks off the player, at y 2017. */
    public static void addMeteor() {
        Entity player = Minecraft.getInstance().player;
        if (player == null) return;
        Random rand = RAND;
        METEORS.add(new Meteor(player.getX() + rand.nextInt(16000) - 8000, 2017, player.getZ() + rand.nextInt(16000) - 8000));
    }

    /** 1.7 {@code updateWeather} on the client, in the player's celestial level: every meteor moves, smoke is left. */
    static void tickMeteors(ClientLevel level) {
        if (SpaceSkies.body(level) == null) return;
        ListIterator<Meteor> iterator = METEORS.listIterator();
        while (iterator.hasNext()) {
            Meteor meteor = iterator.next();
            Meteor fragment = meteor.update(RAND);
            if (meteor.isDead) iterator.remove();
            if (fragment != null) iterator.add(fragment);
        }
    }

    // ------------------------------------------------------------------------------------------------ drawing

    @Override
    public void afterSky(Sky sky) {
        ClientLevel level = sky.level();
        SolarSystem.Body body = SpaceSkies.body(level);
        if (body == null) return;
        float partialTicks = sky.partialTick();

        String air = SpaceSkies.airBody(level);
        boolean hasAtmosphere = air != null && Atmospheres.has(air);
        float pressure = hasAtmosphere ? (float) Atmospheres.pressure(air) : 0.0F;
        float visibility = hasAtmosphere ? Mth.clamp(2.0F - pressure, 0.1F, 1.0F) : 1.0F;
        float solarAngle = SpaceSkies.isKerbin(level) ? sky.celestialAngle() : SpaceSkies.angle(level, partialTicks);

        if (visibility > 0.2F) railguns(sky, body, solarAngle);
        meteors(sky, partialTicks);
    }

    private static void railguns(Sky sky, SolarSystem.Body body, float solarAngle) {
        List<WarSatellites.Railgun> railguns = WarSatellites.clientRailguns();
        if (railguns.isEmpty()) return;
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(body.axialTilt));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(solarAngle * 360.0F));
        for (WarSatellites.Railgun railgun : railguns) railgun(sky, railgun.freq(), railgun.interp());
        pose.popPose();
    }

    /** 1.7 {@code SatelliteRailgun.render} (beams left out, see the class comment). */
    private static void railgun(Sky sky, long id, float interp) {
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.scale(5, 5, 5);

        double rounded = Math.round(id / 1000.0);
        double x = ((id % 10) - 5) * 2;
        double y = (((id / 10) % 10) - 5) * 2;

        double xPos = Math.min(Math.max(-rounded + 30 + x, -50), 50);
        double yPos = Math.min(Math.max(-rounded - 20 + y, -50), 50);

        pose.translate(xPos, yPos, 20);
        float alped = 1.0F - Math.min(1.0F, interp / 100);

        if (interp > 0) {
            pose.pushPose();
            pose.translate(1, 5.5, 0);
            pose.scale(interp * 0.2F, interp * 0.2F, interp * 0.2F);
            plane(sky, SPACE_FLARE, Sky.Blend.ADDITIVE, alped);
            plane(sky, SHOCKWAVE, Sky.Blend.ADDITIVE, alped);
            pose.popPose();
        }

        // the firing beams: two straight 'spirals' and a random zigzag, 2 blocks long per point of interp
        pose.pushPose();
        pose.translate(1, 5.5, 0);
        float length = interp * 2;
        long time = sky.level().getGameTime();
        beam(sky, true, 0, 1, 0F, 6, (float) 1.6 * 1.2F * alped, alped * 0.2F, length);
        beam(sky, true, 0, 1, 0F, 6, (float) 0.7 * 0.6F, alped * 0.6F, length);
        beam(sky, false, (int) (time / 5) % 1000, 35, 0.2F, 6, (float) 0.2 * 0.1F, alped, length);
        pose.popPose();

        pose.mulPose(Axis.ZP.rotationDegrees(-90));
        sky.texturedQuads(RAILGUN, Sky.Blend.ALPHA, 1F, 1F, 1F, 1F, railgunQuads());
        pose.popPose();
    }

    /** BeamPronter's colour for every railgun beam ({@code 0x202060}, outer and inner). */
    private static final float BEAM_R = 0x20 / 255F, BEAM_G = 0x20 / 255F, BEAM_B = 0x60 / 255F;

    /**
     * hbm-space's {@code BeamPronter.prontBeam} (SOLID) along +y for {@code length}: per segment, {@code layers} square
     * prisms of growing half-width {@code thickness / layers * j}, additive, culling off; the segment ends wobble by
     * {@code size} - a SPIRAL by 45 degrees a segment from {@code start} degrees, a RANDOM by two random turns seeded
     * with {@code start}. Skeleton (0, length, 0): BeamPronter's yaw is 0 and its pitch 90, so only its 180 degree turn
     * remains.
     */
    private static void beam(Sky sky, boolean spiral, int start, int segments, float size, int layers, float thickness,
                             float alpha, float length) {
        if (length <= 0) return; // 1.7 drew zero-length prisms
        PoseStack pose = sky.pose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180));
        Random rand = new Random(start);
        double segLength = length / segments;
        double lastX = 0, lastY = 0, lastZ = 0;
        float radius = thickness / layers;
        List<Float> quads = new ArrayList<>();
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
                for (int j = 1; j <= layers; j++) {
                    double w = radius * j;
                    quad(quads, lastX + w, lastY, lastZ + w, lastX + w, lastY, lastZ - w, pX + w, pY, pZ - w, pX + w, pY, pZ + w);
                    quad(quads, lastX - w, lastY, lastZ + w, lastX - w, lastY, lastZ - w, pX - w, pY, pZ - w, pX - w, pY, pZ + w);
                    quad(quads, lastX + w, lastY, lastZ + w, lastX - w, lastY, lastZ + w, pX - w, pY, pZ + w, pX + w, pY, pZ + w);
                    quad(quads, lastX + w, lastY, lastZ - w, lastX - w, lastY, lastZ - w, pX - w, pY, pZ - w, pX + w, pY, pZ - w);
                }
            }
            lastX = pX;
            lastY = pY;
            lastZ = pZ;
        }
        float[] xyz = new float[quads.size()];
        for (int k = 0; k < xyz.length; k++) xyz[k] = quads.get(k);
        // culling off in 1.7: each quad in both windings
        sky.quads(Sky.Blend.ADDITIVE, BEAM_R, BEAM_G, BEAM_B, alpha, CelestialSkyObjects.bothFaces(xyz, 3));
        pose.popPose();
    }

    private static void quad(List<Float> out, double... v) {
        for (double d : v) out.add((float) d);
    }

    private static float[] railgunQuads;

    /** Forge's authored face order and half-texel UV inset, retained by the sky submission. */
    private static float[] railgunQuads() {
        if (railgunQuads != null) return railgunQuads;
        railgunQuads = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/sat/railgun_main.obj")).texturedQuads(0.0005F);
        return railgunQuads;
    }

    private static float[] planeQuads;

    /** The authored two-triangle plane, including its face order and Forge UV inset. */
    private static void plane(Sky sky, Sky.Texture texture, Sky.Blend blend, float alpha) {
        if (planeQuads == null)
            planeQuads = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/plane.obj")).texturedQuads(0.0005F);
        sky.texturedQuads(texture, blend, 1F, 1F, 1F, alpha, planeQuads);
    }

    /**
     * 1.7 {@code SkyProviderCelestial.render}'s war loop, called by {@link MunSky} right after Thatmo's railgun silhouette
     * ({@code render3DModel}) and before the rings, Thatmo's battle and the dark disc, as in 1.7. Before each arrived
     * projectile's planes 1.7 called {@code render3DModel} again, so the black silhouette covers the previous
     * projectile's planes where they overlap it (Thatmo with its battlefield only; elsewhere the model is empty).
     */
    static void shockwaves(Sky sky, ClientLevel level, float partialTicks) {
        CBT_War war = CelestialTraits.getTrait(level, CBT_War.class);
        if (war == null) return;
        PoseStack pose = sky.pose();
        Quaternionf first = new Quaternionf().rotateAxis(90F * Mth.DEG_TO_RAD, normal(-10, -1, 50));
        Quaternionf second = new Quaternionf().rotateAxis(20F * Mth.DEG_TO_RAD, normal(0, -1, 1));
        for (CBT_War.Projectile projectile : war.getProjectiles()) {
            float thing = projectile.getFlashtime() + partialTicks;
            if (projectile.getTravel() > 0) continue;
            float alpd = 1.0F - Math.min(1.0F, thing / 100);

            ThatmoSky.model(sky);
            pose.pushPose();
            pose.translate(projectile.getTranslateX() + 70, projectile.getTranslateY(), projectile.getTranslateZ() + 50);
            pose.scale(thing, thing, thing);
            pose.mulPose(first);
            pose.mulPose(second);
            plane(sky, SHOCKWAVE, Sky.Blend.ADDITIVE, alpd);
            pose.popPose();

            pose.pushPose();
            pose.translate(projectile.getTranslateX() + 70, projectile.getTranslateY(), projectile.getTranslateZ() + 50);
            pose.scale(thing * 0.4F, thing * 0.4F, thing * 0.4F);
            pose.mulPose(first);
            pose.mulPose(second);
            plane(sky, CENS, Sky.Blend.ADDITIVE, alpd);
            pose.popPose();
        }
    }

    private static org.joml.Vector3f normal(float x, float y, float z) {
        return new org.joml.Vector3f(x, y, z).normalize();
    }

    private static void meteors(Sky sky, float partialTicks) {
        if (METEORS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera == null) return;
        Vec3 pos = camera.getEyePosition(partialTicks);
        float viewY = camera.getViewYRot(partialTicks);
        float viewX = camera.getViewXRot(partialTicks);
        float rainStrength = sky.level().getRainLevel(partialTicks);
        int renderDistance = mc.options.renderDistance().get();
        PoseStack pose = sky.pose();

        for (Meteor meteor : METEORS) {
            pose.pushPose();

            // optimised 3 sqrt per meteor to just 1 (1.7)
            Vec3 offset = new Vec3(meteor.posX - pos.x, meteor.posY - pos.y, meteor.posZ - pos.z);
            double offsetLength = offset.length();
            double distance = Math.min(renderDistance * 16, offsetLength);
            Vec3 offsetNormal = offsetLength >= 1.0E-4D ? offset.scale(1 / offsetLength) : offset;
            pose.translate(offsetNormal.x * distance, offsetNormal.y * distance, offsetNormal.z * distance);

            double descent = 2017d - meteor.posY;
            double quadratic = (-(descent * descent) + (1517 * descent)) / 41;
            float scalar = (float) (quadratic / offsetLength);
            pose.scale(scalar, scalar, scalar);

            pose.mulPose(Axis.YP.rotationDegrees(180.0F - viewY));
            pose.mulPose(Axis.XP.rotationDegrees(-viewX));

            if (meteor.type == MeteorType.SMOKE) {
                // renderSmoke: glColor4f(0.6 dark, 0.6 dark, dark, 1) overrides the earlier glColor4d(1, 0, 0, 1)
                float dark = 1f - Math.min(((float) meteor.age / (100f * 0.35F)), 1f);
                billboard(sky, PARTICLE_BASE, 0.6F * dark, 0.6F * dark, dark, 1F);
            } else {
                // renderGlow(1, 1, 1, rain): with x = y = 1 its brightness is always 1 (1.7)
                double near = 0.51d * (Math.min(40000f, Math.max(0d, 1 - 35000d)) / 40000d);
                float entry = (float) (near * (1d - rainStrength) + (1d - (Math.min(200d, Math.max(0d, 1 - 2017d)) / 200f)));
                billboard(sky, FLARE, entry, entry, entry, entry);
            }
            pose.popPose();
        }
    }

    /** {@code renderSmoke}/{@code renderGlow}'s quad: (-0.5, -0.25) to (0.5, 0.75), u mirrored. */
    private static void billboard(Sky sky, Sky.Texture texture, float r, float g, float b, float a) {
        float f4 = 1.0F, f5 = 0.5F, f6 = 0.25F;
        sky.texturedQuads(texture, Sky.Blend.ADDITIVE, r, g, b, a,
                0.0F - f5, 0.0F - f6, 0, 1, 0,
                f4 - f5, 0.0F - f6, 0, 0, 0,
                f4 - f5, f4 - f6, 0, 0, 1,
                0.0F - f5, f4 - f6, 0, 1, 1);
    }
}
