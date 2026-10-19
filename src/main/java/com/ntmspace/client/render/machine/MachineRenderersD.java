package com.ntmspace.client.render.machine;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.tileentity.machine.TileEntityDishControl;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * The progression machines' renderers, part D: hbm-space's {@code RenderStardar} and {@code RenderDishControl}.
 */
public final class MachineRenderersD {

    private MachineRenderersD() {}

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = MachineRenderers.sink(renderers);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesD.STARDAR_BE.get(), StardarRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesD.DISH_CONTROL_BE.get(), DishControlRenderer::new);
    }

    public static final class StardarState extends CompatRenderState {
        public float yaw;
        public float dishYaw;
        public float dishPitch;
    }

    /**
     * {@code RenderStardar}: three blocks down (the core is the dish platform); the base turned 180 and then N 0, W 90,
     * S 180, E 270; the turntable ("rotation") turned by the dish yaw, the dish ("pitch") also tilted by the dish pitch
     * about y 10.6. Both angles are interpolated as 1.7 did. The {@code drive} group is drawn only by the item.
     * 1.7's infinite render box and 65,536 render distance squared: off-screen, 256 blocks.
     */
    static final class StardarRenderer extends GemBER<TileEntityMachineStardar, StardarState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/stardar.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/stardar.png"));

        @Override
        protected void setup(Gem.Model<StardarState> declaration) {
            Gem.Part<StardarState> root = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, -3.0F, 0.5F);
            root.joint().rotate(Gem.Axes.Y, 180F).angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.part("base"), surface);

            Gem.Part<StardarState> turntable = root.joint().angle(Gem.Axes.Y, state -> state.dishYaw);
            turntable.child(model.part("rotation"), surface);
            float dishOffset = 10.6F;
            turntable.child(model.part("pitch"), surface)
                    .translate(0F, dishOffset, 0F)
                    .angle(Gem.Axes.X, state -> state.dishPitch)
                    .translate(0F, -dishOffset, 0F);
        }

        @Override
        public StardarState createRenderState() {
            return new StardarState();
        }

        @Override
        public void extract(TileEntityMachineStardar be, StardarState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = MachineRenderers.yawNWSE(be);
            state.dishYaw = be.prevDishYaw + (be.dishYaw - be.prevDishYaw) * partialTicks;
            state.dishPitch = be.prevDishPitch + (be.dishPitch - be.prevDishPitch) * partialTicks;
        }
    }

    public static final class DishControlState extends CompatRenderState {
        public float yaw;
        public boolean hasDrive;
    }

    /**
     * {@code RenderDishControl}: turned 90 and then N 0, W 90, S 180, E 270; the console and joystick, and the drive while
     * the linked StarDar holds one.
     */
    static final class DishControlRenderer extends GemBER<TileEntityDishControl, DishControlState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/dish_controller.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/dish_controller.png"));

        @Override
        protected void setup(Gem.Model<DishControlState> declaration) {
            Gem.Part<DishControlState> console = declaration.joint()
                    .translate(0.5F, 0F, 0.5F)
                    .rotate(Gem.Axes.Y, 90F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
            console.child(model.parts("dish_control", "joystick"), surface);
            console.child(model.part("drive"), surface).stretch(state -> state.hasDrive ? 1F : 0F);
        }

        @Override
        public DishControlState createRenderState() {
            return new DishControlState();
        }

        @Override
        public void extract(TileEntityDishControl be, DishControlState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 2)) {
                case 4 -> 90F;
                case 3 -> 180F;
                case 5 -> 270F;
                default -> 0F;
            };
            state.hasDrive = be.starDarHasDisk();
        }
    }
}
