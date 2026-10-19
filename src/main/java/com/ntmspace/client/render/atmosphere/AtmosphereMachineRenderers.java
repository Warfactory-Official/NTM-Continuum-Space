package com.ntmspace.client.render.atmosphere;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.client.render.machine.MachineRenderers;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.tileentity.atmosphere.TileEntityAirScrubber;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmoTower;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericCompressor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The atmosphere machines' renderers: hbm-space's {@code RenderAtmoTower}, {@code RenderAtmosphericCompressor},
 * {@code RenderAtmosphericEmitter} and {@code RenderAirScrubber}, through HBM's {@link HbmBlockEntityRenderers#regGem},
 * each with its 1.7 yaw/offset table. The tower, compressor and emitter drew with culling off, the scrubber with it on.
 */
public final class AtmosphereMachineRenderers {

    private AtmosphereMachineRenderers() {}

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = MachineRenderers.sink(renderers);
        HbmBlockEntityRenderers.regGem(sink, AtmosphereMachines.ATMO_TOWER_BE.get(), TowerRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, AtmosphereMachines.ATMO_VENT_BE.get(), CompressorRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, AtmosphereMachines.ATMO_EMITTER_BE.get(), EmitterRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, AtmosphereMachines.AIR_SCRUBBER_BE.get(), ScrubberRenderer::new);
    }

    /** A machine turned by the deuterium tower's table, with a turning part. */
    public static final class TowerState extends CompatRenderState {
        public float yaw;
        public float tx;
        public float tz;
        public float spin;
    }

    /**
     * The deuterium tower's table ({@code RenderDeuteriumTower}, copied by the tower and the compressor): drawn from the
     * block's corner, turned 180, then N 0 and one back, S 180 and one along x, W 90 and (1, -1), E 270.
     */
    static void towerYaw(BlockEntity be, TowerState state) {
        switch (MachineCore.facingOrdinal(be.getBlockState(), 2)) {
            case 3 -> { state.yaw = 180F; state.tx = 1F; state.tz = 0F; }
            case 4 -> { state.yaw = 90F; state.tx = 1F; state.tz = -1F; }
            case 5 -> { state.yaw = 270F; state.tx = 0F; state.tz = 0F; }
            default -> { state.yaw = 0F; state.tx = 0F; state.tz = -1F; }
        }
    }

    static Gem.Part<TowerState> towerFrame(Gem.Model<TowerState> declaration) {
        return declaration.offScreen().viewDistance(256).joint()
                .rotate(Gem.Axes.Y, 180F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .slide(Gem.Axes.X, state -> state.tx)
                .slide(Gem.Axes.Z, state -> state.tz);
    }

    /** {@code RenderAtmoTower}: the deuterium tower's table, the whole model. */
    static final class TowerRenderer extends GemBER<TileEntityAtmoTower, TowerState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/atmo_tower.obj");
        private final Surface surface = Surface.cutoutNoCull(MachineRenderers.tex("machines/machine_atmo_tower.png"));

        @Override
        protected void setup(Gem.Model<TowerState> declaration) {
            towerFrame(declaration).child(model.all(), surface);
        }

        @Override
        public TowerState createRenderState() {
            return new TowerState();
        }

        @Override
        public void extract(TileEntityAtmoTower be, TowerState state, float partialTicks, Vec3 cameraPosition) {
            towerYaw(be, state);
        }
    }

    /**
     * {@code RenderAtmosphericCompressor}: the deuterium tower's table; the body, then the fan turned about a point 0.19
     * off the axis ({@code T(-0.19, 0, 0.19) R(rot, 0, -1, 0) T(0.19, 0, -0.19)}, 1.7: "this somehow fucking works").
     */
    static final class CompressorRenderer extends GemBER<TileEntityAtmosphericCompressor, TowerState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/atmo_vent.obj");
        private final Surface surface = Surface.cutoutNoCull(MachineRenderers.tex("machines/atmo_vent.png"));

        @Override
        protected void setup(Gem.Model<TowerState> declaration) {
            Gem.Part<TowerState> frame = towerFrame(declaration);
            frame.child(model.part("Body_Cylinder"), surface);
            frame.child(model.part("Fan_Cylinder.001"), surface)
                    .translate(-0.19F, 0F, 0.19F)
                    .angle(Gem.Axes.Y, state -> -state.spin)
                    .translate(0.19F, 0F, -0.19F);
        }

        @Override
        public TowerState createRenderState() {
            return new TowerState();
        }

        @Override
        public void extract(TileEntityAtmosphericCompressor be, TowerState state, float partialTicks, Vec3 cameraPosition) {
            towerYaw(be, state);
            state.spin = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        }
    }

    /** {@code RenderAtmosphericEmitter}: centred, N 90, W 180, S 270, E 0, then (-0.5, 0, 0.5); the fan turned by -fan. */
    static final class EmitterRenderer extends GemBER<TileEntityAtmosphericEmitter, TowerState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/outtake.obj");
        private final Surface surface = Surface.cutoutNoCull(MachineRenderers.tex("machines/outtake.png"));

        @Override
        protected void setup(Gem.Model<TowerState> declaration) {
            Gem.Part<TowerState> frame = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .translate(-0.5F, 0F, 0.5F);
            frame.child(model.part("Base"), surface);
            frame.child(model.part("Fan"), surface).angle(Gem.Axes.Y, state -> -state.spin);
        }

        @Override
        public TowerState createRenderState() {
            return new TowerState();
        }

        @Override
        public void extract(TileEntityAtmosphericEmitter be, TowerState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
                case 2 -> 90F;
                case 4 -> 180F;
                case 3 -> 270F;
                default -> 0F;
            };
            state.spin = be.prevFan + (be.fan - be.prevFan) * partialTicks;
        }
    }

    /** {@code RenderAirScrubber}: centred, turned 180, then N 0, W 90, S 180, E 270; the base, the fan turned by rot. */
    static final class ScrubberRenderer extends GemBER<TileEntityAirScrubber, TowerState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/air_scrubber.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/air_scrubber.png"));

        @Override
        protected void setup(Gem.Model<TowerState> declaration) {
            Gem.Part<TowerState> frame = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F)
                    .rotate(Gem.Axes.Y, 180F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
            frame.child(model.part("Base"), surface);
            frame.child(model.part("Fan"), surface).angle(Gem.Axes.Y, state -> state.spin);
        }

        @Override
        public TowerState createRenderState() {
            return new TowerState();
        }

        @Override
        public void extract(TileEntityAirScrubber be, TowerState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = MachineRenderers.yawNWSE(be);
            state.spin = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        }
    }
}
