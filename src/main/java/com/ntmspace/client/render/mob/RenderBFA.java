package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ntmspace.entity.mob.EntityBFAngel;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code RenderBFA}: the angel's body and eye, three pairs of wings beating on their own sine phases, the
 * pale flash of 64 additive rays behind it (1.7 {@code renderFlash}, culled, no depth write), and while its ground
 * beam is up the UFO's three stacked beams down to the first block. It pushes the boss bar for every frame it draws,
 * as 1.7 did. 1.7 drew the wings with GL lighting off; here they take the entity lighting like the body.
 */
public class RenderBFA extends CompatEntityRenderer<EntityBFAngel, RenderBFA.State> {

    private static final double SCALE = 2D;

    public RenderBFA(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    public boolean culled(EntityBFAngel entity) {
        return false; // 1.7 ignoreFrustumCheck
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityBFAngel entity, State state, float partialTicks) {
        com.hbm.client.BossStatus.set(entity, net.minecraft.world.BossEvent.BossBarColor.PINK); // 1.7 render-pushed BossStatus
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.time = entity.tickCount + partialTicks;
        state.partial = partialTicks;
        state.beam = entity.getBeam();
        state.beamLength = state.beam ? entity.getY() - groundBelow(entity) : 0D;
        state.age = entity.tickCount;
    }

    /** 1.7 walked down from {@code ceil(posY)} to 0 for the first non-air block; 0 when there is none. */
    private static int groundBelow(EntityBFAngel entity) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(entity.getX());
        int z = Mth.floor(entity.getZ());
        for (int y = (int) Math.ceil(entity.getY()); y >= entity.level().getMinY(); y--) {
            if (!entity.level().getBlockState(pos.set(x, y, z)).isAir()) return y;
        }
        return entity.level().getMinY();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        WavefrontObj m = SpaceMobModels.bfangel();
        int light = state.lightCoords;

        ps.pushPose();
        ps.translate(0D, 1D, 0D);
        ps.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        ps.mulPose(Axis.XP.rotationDegrees(state.pitch));

        double time = state.time;
        double cy0 = Math.sin(time * 0.3 % (Math.PI * 2));
        double cy1 = Math.sin(time * 0.25 % (Math.PI * 2) - Math.PI * 0.2);
        double cy2 = Math.sin(time * 0.3 % (Math.PI * 2) - Math.PI * 0.4);

        // 1.7 passed (time + f1), time already holding f1
        renderFlash(ps, sink, 0.4D, (float) (time + state.partial) * 0.06F);

        ps.pushPose();
        ps.scale(2F, 2F, 2F);
        ps.translate(0.5D, 0.2D, 0.5D);
        ps.translate(0F, 0.5F, 0F);

        part(ps, sink, m, "eye", light);
        part(ps, sink, m, "body", light);

        wing(ps, sink, m, "wingL1", light, Axis.ZP, cy0 * 2, Axis.YP, cy0 * 20);
        wing(ps, sink, m, "wingR1", light, Axis.ZP, cy0 * -2, Axis.YP, cy0 * -20);
        wing(ps, sink, m, "wingL2", light, Axis.ZP, cy1 * 5, Axis.YP, cy1 * 20);
        wing(ps, sink, m, "wingR2", light, Axis.ZP, cy1 * -5, Axis.YP, cy1 * -20);
        wing(ps, sink, m, "wingL3", light, Axis.XP, cy1 * 5, Axis.YP, cy2 * 20);
        wing(ps, sink, m, "wingR3", light, Axis.XP, cy1 * -5, Axis.YP, cy2 * -20);

        ps.popPose();

        if (state.beam && state.beamLength > 0D) {
            Vec3 skeleton = new Vec3(0D, -state.beamLength, 0D);
            // hbm-space's extra prontBeam argument (256, a fade distance) has no counterpart in HBM's BeamPronter
            BeamPronter.prontBeam(ps, sink, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                    0, (int) (state.beamLength + 1), 0F, 0x101020, 0x101020, 6, (float) SCALE * 0.75F);
            BeamPronter.prontBeam(ps, sink, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                    state.age / 2, (int) (state.beamLength / 2 + 1), (float) SCALE * 1.5F, 0x202060, 0x202060, 2, 0.0625F);
            BeamPronter.prontBeam(ps, sink, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                    state.age / 4, (int) (state.beamLength / 2 + 1), (float) SCALE * 1.5F, 0x202060, 0x202060, 2, 0.0625F);
        }

        ps.popPose();
    }

    private static void part(PoseStack ps, SubmitSink sink, WavefrontObj m, String name, int light) {
        sink.submitCustomGeometry(ps, CompatRenderTypes.entityCutoutNoCull(SpaceMobModels.BFANGEL),
                (pose, buffer) -> m.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    private static void wing(PoseStack ps, SubmitSink sink, WavefrontObj m, String name, int light,
                             Axis first, double firstDegrees, Axis second, double secondDegrees) {
        ps.pushPose();
        ps.mulPose(first.rotationDegrees((float) firstDegrees));
        ps.mulPose(second.rotationDegrees((float) secondDegrees));
        part(ps, sink, m, name, light);
        ps.popPose();
    }

    /**
     * 1.7 {@code renderFlash}: 64 triangle fans from a fixed seed (425), each turned five more times on top of the
     * last, centre {@code (1, 0.6, 0.6, intensity^2 * 2)}, rim {@code (0.1, 0.6, 0.1, 0)}, additive, culled.
     */
    private static void renderFlash(PoseStack ps, SubmitSink sink, double intensity, float seed) {
        Random random = new Random(425L);
        float scale = 2;
        float centreAlpha = (float) (intensity * intensity) * 2F;

        ps.pushPose();
        ps.translate(1D, 2.2D, 1D);

        for (int i = 0; i < 64; i++) {
            ps.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F + seed));
            ps.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F + seed));
            ps.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + seed));
            ps.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F + seed));
            ps.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F + seed));

            float vert1 = (random.nextFloat() * 20.0F + 5.0F + 1 * 10.0F) * (float) (intensity * intensity * scale);
            float vert2 = (random.nextFloat() * 2.0F + 1.0F + 1 * 2.0F) * (float) (intensity * intensity * scale);

            sink.submitCustomGeometry(ps, CompatRenderTypes.dragonRays(),
                    (pose, buffer) -> ray(pose, buffer, centreAlpha, vert1, vert2));
        }

        ps.popPose();
    }

    /** 1.7's fan (centre, A, B, C, A) as its three triangles. */
    private static void ray(PoseStack.Pose pose, VertexConsumer buffer, float centreAlpha, float vert1, float vert2) {
        float ax = -0.866F * vert2, az = -0.5F * vert2;
        float bx = 0.866F * vert2, bz = -0.5F * vert2;
        float cz = 1.0F * vert2;
        triangle(pose, buffer, centreAlpha, ax, vert1, az, bx, vert1, bz);
        triangle(pose, buffer, centreAlpha, bx, vert1, bz, 0F, vert1, cz);
        triangle(pose, buffer, centreAlpha, 0F, vert1, cz, ax, vert1, az);
    }

    private static void triangle(PoseStack.Pose pose, VertexConsumer buffer, float centreAlpha,
                                 float x1, float y1, float z1, float x2, float y2, float z2) {
        Vertexes.color(buffer, pose, 0F, 0F, 0F, 1.0F, 0.6F, 0.6F, centreAlpha);
        Vertexes.color(buffer, pose, x1, y1, z1, 0.1F, 0.6F, 0.1F, 0F);
        Vertexes.color(buffer, pose, x2, y2, z2, 0.1F, 0.6F, 0.1F, 0F);
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
        double time;
        float partial;
        boolean beam;
        double beamLength;
        int age;
    }
}
