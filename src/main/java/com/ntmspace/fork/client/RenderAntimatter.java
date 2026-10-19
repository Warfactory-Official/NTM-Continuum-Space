package com.ntmspace.fork.client;

import com.hbm.blocks.bomb.NukeBombBlock;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.fork.antimatter.TileEntityAntimatter;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code RenderAntimatter}: the {@code blomb} OBJ (culling off) turned by the 1.7 facing like the Balefire
 * Bomb, and while the core is in: a near-black sphere ({@code sphere_uv}, colour 0x010101, scale 0.1) bobbing at 2.4
 * blocks, and three half-alpha beams from one block up - a dark spiral (0x101020, six layers) and two jittering blue ones
 * (0x202060) re-seeded every 2 and 4 ticks.
 */
public class RenderAntimatter extends GemBER<TileEntityAntimatter, RenderAntimatter.State> {

    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of("ntmspace", "models/bombs/blomb.obj"));
    private static final WavefrontObj SPHERE = WavefrontObj.load(Ids.of("hbm", "models/sphere_uv.obj"));
    /** 1.7 facing 2 -> 90, 3 -> 270, 4 -> 180, 5 -> 0 (the Balefire Bomb's table). */
    private static final float[] YAWS = {90F, 270F, 180F, 0F};
    private static final double SCALE = 0.1;

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> bomb = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        Gem.Part<State> core = bomb.joint().visibleIf(state -> state.core)
                .translate(0F, 2.4F, 0F)
                .slide(Gem.Axes.Y, state -> state.bob)
                .scale((float) SCALE);
        // 1.7 drew it untextured with glColor3ub(1, 1, 1) and lighting on
        core.child(SPHERE.all(), Surface.solid("hbm:textures/models/misc/white.png"))
                .tint(state -> 0xFF010101);
        bomb.child(MODEL.all(true), Surface.cutoutNoCull("ntmspace:textures/models/bombs/blomb_tex.png"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityAntimatter bomb, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = bomb.getBlockState();
        int facing = bs.hasProperty(NukeBombBlock.FACING) ? bs.getValue(NukeBombBlock.FACING) : 2;
        state.yaw = facing >= 2 && facing <= 5 ? YAWS[facing - 2] : YAWS[0];
        state.core = bomb.hasAmatCore();
        long time = bomb.getLevel() == null ? 0L : bomb.getLevel().getGameTime();
        state.time = time;
        state.bob = (float) (Math.sin(((time + partialTicks) * 0.1D) % (Math.PI * 2D)) * 0.125 * SCALE);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (!state.core) return;
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.translate(0D, 1D, 0D);
        Vec3 up = new Vec3(0, 2.5, 0);
        // prontBeam(skeleton, wave, beam, outer, inner, start, segments, size, layers, thickness, alpha 0.5)
        BeamPronter.prontBeam(pose, sink, up, BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID,
                0, 1, 0F, 0x80101020, 0x80101020, 6, (float) SCALE * 0.1F);
        BeamPronter.prontBeam(pose, sink, up, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                (int) (state.time / 2) % 1000, 6, (float) SCALE, 0x80202060, 0x80202060, 2, 0.0625F * (float) SCALE);
        BeamPronter.prontBeam(pose, sink, up, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                (int) (state.time / 4) % 1000, 6, (float) SCALE, 0x80202060, 0x80202060, 2, 0.0625F * (float) SCALE);
        pose.popPose();
    }

    public static final class State extends CompatRenderState {
        float yaw;
        boolean core;
        long time;
        float bob;
    }
}
