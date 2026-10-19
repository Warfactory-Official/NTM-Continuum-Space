package com.ntmspace.client.render;

import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.block.terrain.BlockGeysirElectric;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code RenderVol2}, the discharge geyser's flash: while the charge is 0, a dark spiral (6 layers, 8
 * segments, {@code 0x101020}) and two jittering blue arcs ({@code 0x202060}, 26 segments) a hundred blocks plus the
 * flash long, fading as the flash grows ({@code alpha = 1 - min(1, flash / 110)}). 1.7 drew the beams additively
 * ({@code SRC_ALPHA, ONE}) with a beam alpha; HBM's beam pass is additive without alpha, so the colours are scaled by
 * it here. The transform chain is 1.7's for metadata 0 (the only one any geyser has): straight up from the block's foot.
 */
public final class RenderGeysirElectric extends GemBER<BlockGeysirElectric.Discharge, RenderGeysirElectric.State> {

    public static final class State extends CompatRenderState {
        public int charge;
        public float flash;
        public long time;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockGeysirElectric.Discharge geyser, State state, float partialTicks, Vec3 cameraPosition) {
        state.charge = geyser.charge;
        state.flash = geyser.flash;
        state.time = geyser.getLevel() == null ? 0L : geyser.getLevel().getGameTime();
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.charge != 0) return;
        float scale = state.flash;
        float alpha = 1.0F - Math.min(1.0F, scale / 110);

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        // metadata 0
        pose.translate(0D, 0.5D, -0.5D);
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.translate(0D, 0.5D, 0.5D);

        Vec3 beam = new Vec3(0, 0, -100 - scale);
        int spiral = scaled(0x101020, Math.min(1F, alpha * 2F));
        int arc = scaled(0x202060, 0.5F * alpha);
        BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0, 8, 0F,
                spiral, spiral, 6, 3 * alpha);
        BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.RANDOM, EnumBeamType.SOLID, (int) (state.time / 2) % 1000, 26, 3F,
                arc, arc, 2, 0.0625F * 2);
        BeamPronter.prontBeam(pose, sink, beam, EnumWaveType.RANDOM, EnumBeamType.SOLID, (int) (state.time / 4) % 1000, 26, 3F,
                arc, arc, 2, 0.0625F * 2);
        pose.popPose();
    }

    /** An RGB colour times {@code alpha}: 1.7's {@code SRC_ALPHA, ONE} blend on an additive pass without alpha. */
    private static int scaled(int rgb, float alpha) {
        int r = Math.round((rgb >> 16 & 255) * alpha);
        int g = Math.round((rgb >> 8 & 255) * alpha);
        int b = Math.round((rgb & 255) * alpha);
        return r << 16 | g << 8 | b;
    }
}
