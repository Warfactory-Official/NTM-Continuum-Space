package com.ntmspace.client.render.dyson;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.client.render.machine.MachineRenderers;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterAnatmogenesis;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterTU;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.dyson.TileEntityDysonReceiver;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's Dyson renderers ({@code RenderDysonLauncher}, {@code RenderDysonReceiver}, {@code RenderDysonConverterHE},
 * {@code RenderDysonConverterTU}, {@code RenderDysonConverterAnatmogenesis}) through HBM's
 * {@link HbmBlockEntityRenderers#regGem}. Each turns its model N 0, W 90, S 180, E 270, culled; 1.7's 65,536 render
 * distance squared is a 256-block view distance, and the oversized bounding boxes mean "draw off screen".
 */
public final class DysonRenderers {

    private DysonRenderers() {}

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = MachineRenderers.sink(renderers);
        HbmBlockEntityRenderers.regGem(sink, DysonContent.LAUNCHER_BE.get(), LauncherRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, DysonContent.RECEIVER_BE.get(), ReceiverRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, DysonContent.CONVERTER_HE_BE.get(), ConverterHERenderer::new);
        HbmBlockEntityRenderers.regGem(sink, DysonContent.CONVERTER_TU_BE.get(), ConverterTURenderer::new);
        HbmBlockEntityRenderers.regGem(sink, DysonContent.CONVERTER_ANATMOGENESIS_BE.get(), AnatmogenesisRenderer::new);
    }

    // ---------------------------------------------------------------------------------------- launcher

    public static final class LauncherState extends CompatRenderState {
        public float yaw;
        public float rotor;
        public boolean payloadOnRotor;
        public boolean spinningDown;
        public float payload;
    }

    /**
     * {@code RenderDysonLauncher}: "Launch" fixed; "The_Thing_That_Rotates" spun about an axis tilted 45 degrees about
     * X through (0, 8.5, 0), carrying a "Payload" while the launcher holds members and is not spinning down; while it
     * spins down, the payload flies off sideways, ten blocks a tick along the tilted launch direction.
     */
    static final class LauncherRenderer extends GemBER<TileEntityDysonLauncher, LauncherState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/dyson_spinlaunch.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/dyson_spinlaunch.png"));

        @Override
        protected void setup(Gem.Model<LauncherState> declaration) {
            Gem.Part<LauncherState> root = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
            root.child(model.part("Launch"), surface);

            Gem.Part<LauncherState> rotor = root.joint()
                    .translate(0F, 8.5F, 0F).rotate(Gem.Axes.X, 45F).angle(Gem.Axes.Y, s -> s.rotor)
                    .rotate(Gem.Axes.X, -45F).translate(0F, -8.5F, 0F);
            rotor.child(model.part("The_Thing_That_Rotates"), surface);
            rotor.child(model.part("Payload"), surface).visibleIf(s -> s.payloadOnRotor);

            root.joint()
                    .translate(1.0F, 8.5F, 0F).rotate(Gem.Axes.X, 45F).rotate(Gem.Axes.Y, 90F)
                    .slide(Gem.Axes.X, s -> s.payload * 10.0F)
                    .rotate(Gem.Axes.X, -45F).translate(0F, -8.5F, 0F)
                    .child(model.part("Payload"), surface).visibleIf(s -> s.spinningDown);
        }

        @Override
        public LauncherState createRenderState() {
            return new LauncherState();
        }

        @Override
        public void extract(TileEntityDysonLauncher be, LauncherState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = MachineRenderers.yawNWSE(be);
            state.rotor = be.lastRotation + (be.rotation - be.lastRotation) * partialTicks;
            state.payloadOnRotor = be.satCount > 0 && !be.isSpinningDown;
            state.spinningDown = be.isSpinningDown;
            state.payload = be.payloadTicks + partialTicks;
        }
    }

    // ---------------------------------------------------------------------------------------- receiver

    public static final class ReceiverState extends CompatRenderState {
        public float yaw;
        public float coils;
        public boolean receiving;
        public int length;
        public int start;
    }

    /**
     * {@code RenderDysonReceiver}: "DysonReceiver" fixed, "Coil1"/"Coil3" and "Coil2" turning opposite ways about Z through
     * (0, 1.5, 0) by the world time while receiving; the beam ({@code BeamPronter.prontBeamwithDepth}) from 1.5 up along
     * the receiver's back, {@code beamLength} long, colour 0xff8800 at alpha 0.5 under additive blending (drawn as
     * 0x7f4400, the same sum): a two-layer 0.4 spiral of one segment and a two-layer 0.4 random wave of
     * {@code length / 2} segments, size 0.0625, seeded by the world time mod 1000.
     */
    static final class ReceiverRenderer extends GemBER<TileEntityDysonReceiver, ReceiverState> {
        private static final int BEAM_COLOR = 0x7f4400;
        private final WavefrontObj model = MachineRenderers.obj("machines/dyson_receiver.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/dyson_receiver.png"));

        @Override
        protected void setup(Gem.Model<ReceiverState> declaration) {
            Gem.Part<ReceiverState> root = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
            root.child(model.part("DysonReceiver"), surface);
            Gem.Part<ReceiverState> hub = root.joint().translate(0F, 1.5F, 0F);
            Gem.Part<ReceiverState> forward = hub.joint().angle(Gem.Axes.Z, s -> s.coils).translate(0F, -1.5F, 0F);
            forward.child(model.part("Coil1"), surface);
            forward.child(model.part("Coil3"), surface);
            hub.joint().angle(Gem.Axes.Z, s -> -s.coils).translate(0F, -1.5F, 0F).child(model.part("Coil2"), surface);
        }

        @Override
        public ReceiverState createRenderState() {
            return new ReceiverState();
        }

        @Override
        public void extract(TileEntityDysonReceiver be, ReceiverState state, float partialTicks, Vec3 cameraPosition) {
            Level level = be.getLevel();
            long time = level == null ? 0 : level.getGameTime();
            state.yaw = MachineRenderers.yawNWSE(be);
            state.coils = be.isReceiving ? time + partialTicks : 0;
            state.receiving = be.isReceiving;
            state.length = be.beamLength;
            state.start = (int) (time % 1000);
        }

        @Override
        protected void submitUndeclared(ReceiverState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            if (!state.receiving) return;
            int length = state.length;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.translate(0F, 1.5F, 0F);
            BeamPronter.prontBeam(poseStack, sink, new Vec3(0, 0, length), BeamPronter.EnumWaveType.SPIRAL,
                    BeamPronter.EnumBeamType.SOLID, 0, 1, 0F, BEAM_COLOR, BEAM_COLOR, 2, 0.4F);
            BeamPronter.prontBeam(poseStack, sink, new Vec3(0, 0, length), BeamPronter.EnumWaveType.RANDOM,
                    BeamPronter.EnumBeamType.SOLID, state.start, length / 2, 0.0625F, BEAM_COLOR, BEAM_COLOR, 2, 0.4F);
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------------------------------- converters

    public static final class CoilState extends CompatRenderState {
        public float yaw;
        public float coils;
    }

    /** {@code RenderDysonConverterHE}: as the receiver's coils, turning by the world time while it holds power. */
    static final class ConverterHERenderer extends GemBER<TileEntityDysonConverterHE, CoilState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/dyson_he_converter.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/dyson_he_converter.png"));

        @Override
        protected void setup(Gem.Model<CoilState> declaration) {
            Gem.Part<CoilState> root = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
            root.child(model.part("HE_Converter"), surface);
            Gem.Part<CoilState> hub = root.joint().translate(0F, 1.5F, 0F);
            Gem.Part<CoilState> forward = hub.joint().angle(Gem.Axes.Z, s -> s.coils).translate(0F, -1.5F, 0F);
            forward.child(model.part("Coil1"), surface);
            forward.child(model.part("Coil3"), surface);
            hub.joint().angle(Gem.Axes.Z, s -> -s.coils).translate(0F, -1.5F, 0F).child(model.part("Coil2"), surface);
        }

        @Override
        public CoilState createRenderState() {
            return new CoilState();
        }

        @Override
        public void extract(TileEntityDysonConverterHE be, CoilState state, float partialTicks, Vec3 cameraPosition) {
            Level level = be.getLevel();
            state.yaw = MachineRenderers.yawNWSE(be);
            state.coils = be.isConverting && level != null ? level.getGameTime() + partialTicks : 0;
        }
    }

    public static final class YawState extends CompatRenderState {
        public float yaw;
    }

    /** {@code RenderDysonConverterTU}: the whole model, turned. */
    static final class ConverterTURenderer extends GemBER<TileEntityDysonConverterTU, YawState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/dyson_tu_converter.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/dyson_tu_converter.png"));

        @Override
        protected void setup(Gem.Model<YawState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw)
                    .child(model.all(), surface);
        }

        @Override
        public YawState createRenderState() {
            return new YawState();
        }

        @Override
        public void extract(TileEntityDysonConverterTU be, YawState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = MachineRenderers.yawNWSE(be);
        }
    }

    public static final class BobState extends CompatRenderState {
        public float yaw;
        public float bob;
    }

    /**
     * {@code RenderDysonConverterAnatmogenesis}: "Anatmogenesis" fixed, "Coils" lifted by
     * {@code sin(t * 0.1) * 0.025 + random * 0.02} (a fresh random each frame) while converting.
     */
    static final class AnatmogenesisRenderer extends GemBER<TileEntityDysonConverterAnatmogenesis, BobState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/dyson_anatmogenesis.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/dyson_anatmogenesis.png"));

        @Override
        protected void setup(Gem.Model<BobState> declaration) {
            Gem.Part<BobState> root = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
            root.child(model.part("Anatmogenesis"), surface);
            root.joint().slide(Gem.Axes.Y, s -> s.bob).child(model.part("Coils"), surface);
        }

        @Override
        public BobState createRenderState() {
            return new BobState();
        }

        @Override
        public void extract(TileEntityDysonConverterAnatmogenesis be, BobState state, float partialTicks, Vec3 cameraPosition) {
            Level level = be.getLevel();
            state.yaw = MachineRenderers.yawNWSE(be);
            if (be.isConverting && level != null) {
                float t = level.getGameTime() + partialTicks;
                state.bob = (float) (Math.sin(t * 0.1) * 0.025 + level.getRandom().nextFloat() * 0.02);
            } else {
                state.bob = 0;
            }
        }
    }
}
