package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.entity.mob.siege.EntitySiegeCraft;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * hbm-space {@code RenderSiegeCraft}: {@code siege_ufo.obj} half a block up, spinning five degrees a tick (tipping about
 * (1, 0, 1) once dead). The {@code UFO} part in the tier's skin ({@code siege_craft_<tier>.png}), smooth and unculled; the
 * {@code Coils} untextured, unlit and fullbright, from green to red by health. While beaming: the balefire glint over the
 * hull (1.7 {@code renderClassicGlint(..., 0.5, 1, 1, speed 5, scale 1)}: two layers of {@code glintBF.png} tinted
 * {@code (0.5, 1, 1) * 0.76}, baked into {@code glint_siege_craft.png}), arcs on all eight coils (otherwise each coil
 * arcs one frame in five, re-rolled every four ticks), and three stacked beams to the lock-on.
 */
public class RenderSiegeCraft extends CompatEntityRenderer<EntitySiegeCraft, RenderSiegeCraft.State> {

    private static final Identifier WHITE = Ids.of("hbm", "textures/models/misc/white.png");
    private static final Identifier GLINT = Ids.of(NtmSpace.MOD_ID, "textures/misc/glint_siege_craft.png");

    private static WavefrontObj model;

    private static WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/mobs/siege_ufo.obj"));
        return model;
    }

    public RenderSiegeCraft(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    public boolean culled(EntitySiegeCraft entity) {
        return false; // 1.7 ignoreFrustumCheck
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySiegeCraft entity, State state, float partialTicks) {
        state.spin = (entity.tickCount + partialTicks) * 5 % 360D;
        state.alive = entity.isAlive();
        state.tilt = entity.deathTime + partialTicks;
        state.health = entity.getHealth() / entity.getMaxHealth();
        state.beam = entity.getBeam();
        state.lockon = entity.getLockon().subtract(entity.getX(), entity.getY(), entity.getZ());
        state.age = entity.tickCount;
        state.skin = Ids.of(NtmSpace.MOD_ID, "textures/entity/siege_craft_" + entity.getTier().name + ".png");
        LocalPlayer player = Minecraft.getInstance().player;
        state.glintClock = (player == null ? 0 : player.tickCount) + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        WavefrontObj obj = model();

        ps.pushPose();
        ps.translate(0D, 0.5D, 0D);
        ps.pushPose();

        ps.mulPose(Axis.YP.rotationDegrees((float) state.spin));

        if (!state.alive) {
            // 1.7 glRotatef(tilt * 5, 1, 0, 1): GL normalises the axis
            ps.mulPose(new Quaternionf().rotateAxis(state.tilt * 5 * Mth.DEG_TO_RAD, 0.70710677F, 0F, 0.70710677F));
        }

        sink.submitCustomGeometry(ps, CompatRenderTypes.entityCutoutNoCull(state.skin),
                (pose, buffer) -> obj.renderPart("UFO", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        // GL_LIGHTING off, lightmap 240/240, glColor3f(1 - health, health, 0)
        int coil = coilColor(state.health);
        sink.submitCustomGeometry(ps, CompatRenderTypes.flatCutout(WHITE),
                (pose, buffer) -> obj.renderPart("Coils", pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, coil));

        if (state.beam) {
            Geometry hull = obj.part("UFO");
            float offset = state.glintClock;
            sink.order(1).submitCustomGeometry(ps, CompatRenderTypes.glint(GLINT, false), (pose, buffer) -> {
                for (int k = 0; k < 2; k++) {
                    float movement = offset * (0.001F + k * 0.003F) * 5F;
                    Matrix3f texture = new Matrix3f().scaling(1F, 1F, 1F)
                                                     .rotateZ((float) Math.toRadians(30F - k * 60F))
                                                     .mul(new Matrix3f(1F, 0F, 0F, 0F, 1F, 0F, 0F, movement, 1F));
                    hull.draw(pose, Vertexes.uvTransformed(buffer, texture), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        Random rand = new Random(state.age / 4);
        int start = (int) (System.currentTimeMillis() % 1000) / 50;

        ps.pushPose();
        for (int i = 0; i < 8; i++) {
            ps.mulPose(Axis.YP.rotationDegrees(45F));
            if (rand.nextInt(5) == 0 || state.beam) {
                ps.pushPose();
                ps.translate(4D, 0D, 0D);
                BeamPronter.prontBeam(ps, sink, new Vec3(-1.125, 0, 2.875), EnumWaveType.RANDOM, EnumBeamType.LINE,
                        start, 15, 0.125F, 0x80d0ff, 0xffffff, 1, 0F);
                ps.popPose();
            }
        }
        ps.popPose();
        ps.popPose();

        if (state.beam) {
            Vec3 delta = state.lockon;
            double length = delta.length();
            double scale = 0.1D;
            BeamPronter.prontBeam(ps, sink, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, state.age / 6,
                    (int) (length / 2 + 1), (float) scale * 1F, 0x101020, 0x101020, 4, 0.25F);
            BeamPronter.prontBeam(ps, sink, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, state.age / 2,
                    (int) (length / 2 + 1), (float) scale * 7F, 0x202060, 0x202060, 2, 0.0625F);
            BeamPronter.prontBeam(ps, sink, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, state.age / 4,
                    (int) (length / 2 + 1), (float) scale * 7F, 0x202060, 0x202060, 2, 0.0625F);
        }

        ps.popPose();
    }

    public static int coilColor(EntitySiegeCraft entity) {
        return coilColor(entity.getHealth() / entity.getMaxHealth());
    }

    private static int coilColor(float health) {
        return 0xFF000000 | ((int) (255 * Mth.clamp(1F - health, 0F, 1F)) << 16)
                | ((int) (255 * Mth.clamp(health, 0F, 1F)) << 8);
    }

    public static final class State extends CompatEntityRenderState {
        double spin;
        boolean alive;
        float tilt;
        float health;
        boolean beam;
        Vec3 lockon = Vec3.ZERO;
        int age;
        float glintClock;
        Identifier skin;
    }
}
