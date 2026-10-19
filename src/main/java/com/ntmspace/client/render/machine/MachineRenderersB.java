package com.ntmspace.client.render.machine;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.entity.FramedItem;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The progression machines' renderers, part B: hbm-space's {@code RenderHydroponic} and {@code RenderVacuumCircuit}
 * (see {@link MachineRenderers} for the conventions). Both turn their model N 90, W 180, S 270, E 0 and are drawn culled.
 */
public final class MachineRenderersB {

    private MachineRenderersB() {}

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = MachineRenderers.sink(renderers);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesB.HYDROBAY_BE.get(), HydroponicRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesB.VACUUM_CIRCUIT_BE.get(), VacuumCircuitRenderer::new);
    }

    /** The yaw table of both 1.7 renderers: case 2 90, 4 180, 3 270, 5 0. */
    static float yaw(BlockEntity be) {
        return switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
    }

    public static final class HydroponicState extends CompatRenderState {
        public float yaw;
        public boolean lit;
    }

    /**
     * {@code RenderHydroponic}: the {@code Base} part, then, while the machine has 200 HE, the {@code Lights} part at full
     * brightness, additive ({@code SRC_ALPHA, ONE}), unculled and not writing depth ({@link Surface#glow}).
     */
    static final class HydroponicRenderer extends GemBER<TileEntityHydroponic, HydroponicState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/hydroponic.obj");
        private final Surface base = Surface.cutoutCull(MachineRenderers.tex("machines/hydroponic.png"));
        private final Surface lights = Surface.glow(MachineRenderers.tex("machines/hydroponic.png"));

        @Override
        protected void setup(Gem.Model<HydroponicState> declaration) {
            Gem.Part<HydroponicState> machine = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
            machine.child(model.part("Base"), base);
            machine.child(model.part("Lights"), lights).visibleIf(state -> state.lit);
        }

        @Override
        public HydroponicState createRenderState() {
            return new HydroponicState();
        }

        @Override
        public void extract(TileEntityHydroponic be, HydroponicState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = yaw(be);
            state.lit = be.power >= 200;
        }
    }

    public static final class VacuumCircuitState extends CompatRenderState {
        public float yaw;
        public final CompatItemRenderState displayItem = new CompatItemRenderState();
        public boolean showItem;
        public FramedItem.Arm arm = FramedItem.Arm.SPRITE;
    }

    /**
     * {@code RenderVacuumCircuit}: the solderer, and the current recipe's output lying on the work plate: translate
     * (0.625, 1.25, 0.5), turned 90 about Y and -90 about X, scaled 1.5, drawn as a framed {@code EntityItem} by
     * {@code RenderDecoItem} (which does not bob: {@link FramedItem}'s chains include the bob, taken back out here).
     */
    static final class VacuumCircuitRenderer extends GemBER<TileEntityMachineVacuumCircuit, VacuumCircuitState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/vacuum_solderer.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/vacuum_solderer.png"));

        @Override
        protected void setup(Gem.Model<VacuumCircuitState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.all(), surface);
        }

        @Override
        public VacuumCircuitState createRenderState() {
            return new VacuumCircuitState();
        }

        @Override
        public void extract(TileEntityMachineVacuumCircuit be, VacuumCircuitState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = yaw(be);
            state.showItem = false;
            ItemStack display = be.display;
            if (display != null && !display.isEmpty()) {
                ItemStack one = display.copy();
                one.setCount(1);
                state.arm = FramedItem.resolveArm(state.displayItem, one, be.getLevel());
                state.showItem = !state.displayItem.isEmpty();
            }
            if (!state.showItem) state.displayItem.clear();
        }

        @Override
        protected void submitUndeclared(VacuumCircuitState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            if (!state.showItem) return;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.translate(0.625D, 1.25D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            poseStack.scale(1.5F, 1.5F, 1.5F);
            // RenderDecoItem.shouldBob() is false: undo the bob FramedItem's chains start with
            poseStack.translate(0.0, -0.1, 0.0);
            if (state.arm.block()) FramedItem.framedBlockPrefix(poseStack);
            else FramedItem.framedFlatArm(poseStack, state.arm);
            FramedItem.draw(poseStack, state.displayItem, sink, state.lightCoords);
            poseStack.popPose();
        }
    }
}
