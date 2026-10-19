package com.ntmspace.client.render.station;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.tileentity.station.TileEntityMachineHTR3;
import com.ntmspace.tileentity.station.TileEntityMachineHTRF4;
import com.ntmspace.tileentity.station.TileEntityMachineHTRNeo;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationComputer;
import com.ntmspace.tileentity.station.TileEntityXenonThruster;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.Set;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/**
 * The station blocks' renderers, hbm-space's {@code RenderOrbitalStation}, {@code RenderOrbitalComputer},
 * {@code RenderHTR3}, {@code RenderHTRF4}, {@code RenderHTRNeo} and {@code RenderXenonThruster}, declared through HBM's
 * {@link HbmBlockEntityRenderers#regGem} (one renderer, both the vanilla and the instanced backend).
 *
 * <p>Each 1.7 renderer turned its model by the block's facing ({@code getBlockMetadata() - offset}); the yaw tables are
 * kept per renderer. The exhausts (xenon, NEO) are drawn translucent and full-bright, stretched along their length by
 * 1.7's flicker; the NEO's second, additive "plasma sparkle" layer scrolled through the texture matrix is not drawn.
 */
public final class StationRenderers {

    private StationRenderers() {}

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = new HbmBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends CompatRenderState> void register(BlockEntityType<? extends T> type,
                                                                                         CompatBERProvider<T, S> provider) {
                renderers.blockEntity(type, provider);
            }
        };
        HbmBlockEntityRenderers.regGem(sink, StationBlocks.ORBITAL_STATION_BE.get(), PortRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, StationBlocks.ORBITAL_STATION_COMPUTER_BE.get(), ComputerRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, StationBlocks.HTR3_BE.get(), HTR3Renderer::new);
        HbmBlockEntityRenderers.regGem(sink, StationBlocks.HTRF4_BE.get(), HTRF4Renderer::new);
        HbmBlockEntityRenderers.regGem(sink, StationBlocks.HTRF4_NEO_BE.get(), HTRNeoRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, StationBlocks.XENON_THRUSTER_BE.get(), XenonRenderer::new);
    }

    static WavefrontObj obj(String path) {
        return WavefrontObj.load(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "models/" + path));
    }

    static String tex(String path) {
        return NtmSpace.MOD_ID + ":textures/models/" + path;
    }

    /** {@code RenderOrbitalStation} / {@code RenderOrbitalComputer}: N 0, W 90, S 180, E 270. */
    static float stationYaw(BlockEntity be) {
        return switch (MachineCore.facingOrdinal(be.getBlockState(), 2)) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
    }

    /** {@code RenderHTR3} / {@code RenderHTRF4} / {@code RenderHTRNeo}: S 270, E 0, N 90, W 180. */
    static float thrusterYaw(BlockEntity be) {
        return switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
            case 3 -> 270F;
            case 2 -> 90F;
            case 4 -> 180F;
            default -> 0F;
        };
    }

    // ---------------------------------------------------------------------------------------- docking port

    public static final class PortState extends CompatRenderState {
        public float yaw;
        public float arm;
    }

    /** {@code RenderOrbitalStation}: the port, and its four arms folding in (90 degrees) while a craft is docked. */
    static final class PortRenderer extends GemBER<TileEntityOrbitalStation, PortState> {
        private final WavefrontObj model = obj("machines/docking_port.obj");
        private final Surface surface = Surface.cutoutCull(tex("machines/docking_port.png"));

        @Override
        protected void setup(Gem.Model<PortState> declaration) {
            Gem.Part<PortState> port = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 1.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
            port.child(model.part("Port"), surface);
            for (int i = 0; i < 4; i++) {
                // cha cha real smooth
                port.joint().rotate(Gem.Axes.Y, 90F * i)
                        .child(model.part("ArmZP"), surface)
                        // one hop this time, criss cross, one hop this time
                        .translate(0F, -1.75F, -2F)
                        .angle(Gem.Axes.X, state -> -state.arm)
                        .translate(0F, 1.75F, 2F);
            }
        }

        @Override
        public PortState createRenderState() {
            return new PortState();
        }

        @Override
        public void extract(TileEntityOrbitalStation be, PortState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = stationYaw(be);
            state.arm = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        }
    }

    // ---------------------------------------------------------------------------------------- computer

    public static final class ComputerState extends CompatRenderState {
        public float yaw;
        public boolean hasDrive;
    }

    /** {@code RenderOrbitalComputer}: the console, and the drive while one is in. */
    static final class ComputerRenderer extends GemBER<TileEntityOrbitalStationComputer, ComputerState> {
        private final WavefrontObj model = obj("machines/spaceship_computer.obj");
        private final Surface surface = Surface.cutoutCull(tex("machines/spaceship_computer.png"));

        @Override
        protected void setup(Gem.Model<ComputerState> declaration) {
            Gem.Part<ComputerState> computer = declaration.joint()
                    .translate(0.5F, 0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
            computer.child(model.allExcept(Set.of("drive")), surface);
            computer.child(model.part("drive"), surface).stretch(state -> state.hasDrive ? 1F : 0F);
        }

        @Override
        public ComputerState createRenderState() {
            return new ComputerState();
        }

        @Override
        public void extract(TileEntityOrbitalStationComputer be, ComputerState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = stationYaw(be);
            state.hasDrive = be.hasDrive;
        }
    }

    // ---------------------------------------------------------------------------------------- LPW-3N

    public static final class HTR3State extends CompatRenderState {
        public float yaw;
        public float sway;
        public float h;
        public float v;
        public float piston;
        public float cover;
    }

    /**
     * {@code RenderHTR3}: the LPW-2's moving parts on the LPW-3N body, all driven by the engine's own clock
     * ({@code time}, which runs only while it burns). The bell pivots at (0, 3.5, -1).
     */
    static final class HTR3Renderer extends GemBER<TileEntityMachineHTR3, HTR3State> {
        private final WavefrontObj model = obj("machines/htr3.obj");
        private final Surface surface = Surface.cutoutCull(tex("machines/lpw2.png"));

        @Override
        protected void setup(Gem.Model<HTR3State> declaration) {
            Gem.Part<HTR3State> machine = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, -3.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw);

            machine.child(model.part("Center"), surface);
            machine.child(model.parts("PipeL1", "PipeL2", "PipeR1", "PipeR2"), surface);

            // renderMainAssembly
            Gem.Part<HTR3State> assembly = machine.joint().slide(Gem.Axes.Z, state -> -state.sway * 0.125F);
            assembly.child(model.part("Piston"), surface).slide(Gem.Axes.Z, state -> state.piston * 0.375F + 0.375F);
            assembly.child(model.part("Engine"), surface)
                    .translate(0F, 3.5F, -1F)
                    .angle(Gem.Axes.Y, state -> state.v * 2F)
                    .angle(Gem.Axes.X, state -> state.h * 2F)
                    .translate(0F, -3.5F, 1F);
            // renderShroud (outside the sway)
            machine.child(model.part("ShroudH"), surface).slide(Gem.Axes.Y, state -> -state.h * 0.125F);
            machine.child(model.part("ShroudV"), surface).slide(Gem.Axes.X, state -> state.v * 0.125F);

            machine.child(model.parts("CoverTop", "CoverBottom"), surface).slide(Gem.Axes.Z, state -> -state.cover * 0.125F);
            machine.child(model.parts("SuspensionBottom", "SuspensionTop"), surface)
                    .translate(0F, 0F, 3.5F)
                    .stretch(Gem.Axes.Z, state -> (3F + state.cover * 0.125F) / 3F)
                    .translate(0F, 0F, -3.5F);
        }

        @Override
        public HTR3State createRenderState() {
            return new HTR3State();
        }

        @Override
        public void extract(TileEntityMachineHTR3 be, HTR3State state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = thrusterYaw(be);
            double t = be.lastTime + (be.time - be.lastTime) * partialTicks;

            double swayTimer = (t / 3D) % (Math.PI * 4);
            state.sway = (float) ((Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5);

            double bellTimer = (t / 5D) % (Math.PI * 4);
            state.h = (float) ((Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D);
            state.v = (float) ((Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D);

            double pistonTimer = (t / 5D) % (Math.PI * 2);
            state.piston = (float) Math.sin(Math.PI / 2D * Math.cos(pistonTimer));

            double coverTimer = (t / 5D) % (Math.PI * 4);
            state.cover = (float) ((Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5);
        }
    }

    // ---------------------------------------------------------------------------------------- HTR-F4

    public static final class YawState extends CompatRenderState {
        public float yaw;
    }

    /** {@code RenderHTRF4}: the whole model, still. */
    static final class HTRF4Renderer extends GemBER<TileEntityMachineHTRF4, YawState> {
        private final WavefrontObj model = obj("machines/htrf4.obj");
        private final Surface surface = Surface.cutoutCull(tex("machines/lpw2.png"));

        @Override
        protected void setup(Gem.Model<YawState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, -2.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.all(), surface);
        }

        @Override
        public YawState createRenderState() {
            return new YawState();
        }

        @Override
        public void extract(TileEntityMachineHTRF4 be, YawState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = thrusterYaw(be);
        }
    }

    // ---------------------------------------------------------------------------------------- HTR-F4 NEO

    public static final class NeoState extends CompatRenderState {
        public float yaw;
        public float rotor;
        public float trailStretch;
        public float thrust;
        public float r, g, b;
    }

    /** {@code RenderHTRNeo}: base and engine, the rotor turning about z at y 2.5, the plasma exhaust in the plasma's colour. */
    static final class HTRNeoRenderer extends GemBER<TileEntityMachineHTRNeo, NeoState> {
        private final WavefrontObj model = obj("fusion/htrf4.obj");
        private final Surface surface = Surface.cutoutCull(tex("fusion/htrf4.png"));
        private final RenderType exhaust = CompatRenderTypes.entityTranslucent(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/models/fusion/htrf4_trail.png"));

        @Override
        protected void setup(Gem.Model<NeoState> declaration) {
            Gem.Part<NeoState> machine = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, -2.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
            machine.child(model.parts("Base", "Engine"), surface);
            machine.child(model.parts("Rotor", "Rotor2"), surface).spin(Gem.Axes.Z, 0F, 2.5F, 0F, state -> state.rotor);
        }

        @Override
        public NeoState createRenderState() {
            return new NeoState();
        }

        @Override
        public void extract(TileEntityMachineHTRNeo be, NeoState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = thrusterYaw(be);
            state.rotor = be.prevRotor + (be.rotor - be.prevRotor) * partialTicks;
            float flicker = be.getLevel() == null ? 0F : be.getLevel().getRandom().nextFloat();
            state.trailStretch = (1.2F - (flicker * flicker * 0.1F)) * be.thrustAmount;
            state.thrust = be.thrustAmount;
            state.r = be.plasmaR;
            state.g = be.plasmaG;
            state.b = be.plasmaB;
        }

        @Override
        protected void submitUndeclared(NeoState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            if (state.trailStretch <= 0) return;
            poseStack.pushPose();
            poseStack.translate(0.5F, -2.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.translate(0F, 0F, 12F);
            poseStack.scale(1F, 1F, state.trailStretch);
            poseStack.translate(0F, 0F, -12F);
            int color = argb(state.thrust, state.r, state.g, state.b);
            sink.submitCustomGeometry(poseStack, exhaust, (pose, buffer) ->
                    model.renderPart("Exhaust", pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, color));
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------------------------------- xenon

    public static final class XenonState extends CompatRenderState {
        public float yaw;
        public float trailStretch;
        public float thrust;
    }

    /** {@code RenderXenonThruster}: turned -90 first, then by facing (N 90, W 180, S 270, E 0), half a block back. */
    static final class XenonRenderer extends GemBER<TileEntityXenonThruster, XenonState> {
        private final WavefrontObj model = obj("xenon_thruster.obj");
        private final Surface surface = Surface.cutoutCull(tex("xenon_thruster.png"));
        private final RenderType exhaust = CompatRenderTypes.entityTranslucent(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/models/xenon_trail.png"));

        @Override
        protected void setup(Gem.Model<XenonState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, -1.0F, 0.5F)
                    .rotate(Gem.Axes.Y, -90F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .translate(0F, 0F, -0.5F)
                    .child(model.part("Thruster"), surface);
        }

        @Override
        public XenonState createRenderState() {
            return new XenonState();
        }

        @Override
        public void extract(TileEntityXenonThruster be, XenonState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
                case 2 -> 90F;
                case 4 -> 180F;
                case 3 -> 270F;
                default -> 0F;
            };
            float flicker = be.getLevel() == null ? 0F : be.getLevel().getRandom().nextFloat();
            state.trailStretch = (1.2F - (flicker * flicker * 0.2F)) * be.thrustAmount;
            state.thrust = be.thrustAmount;
        }

        @Override
        protected void submitUndeclared(XenonState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            if (state.trailStretch <= 0) return;
            poseStack.pushPose();
            poseStack.translate(0.5F, -1.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.translate(0F, 0F, -0.5F);
            poseStack.translate(0F, 0F, -1F);
            poseStack.scale(1F, 1F, state.trailStretch);
            poseStack.translate(0F, 0F, 1F);
            int color = argb(state.thrust, 1F, 1F, 1F);
            sink.submitCustomGeometry(poseStack, exhaust, (pose, buffer) ->
                    model.renderPart("Exhaust", pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, color));
            poseStack.popPose();
        }
    }

    static int argb(float a, float r, float g, float b) {
        int ia = Math.round(Math.max(0F, Math.min(1F, a)) * 255F);
        int ir = Math.round(Math.max(0F, Math.min(1F, r)) * 255F);
        int ig = Math.round(Math.max(0F, Math.min(1F, g)) * 255F);
        int ib = Math.round(Math.max(0F, Math.min(1F, b)) * 255F);
        return ia << 24 | ir << 16 | ig << 8 | ib;
    }
}
