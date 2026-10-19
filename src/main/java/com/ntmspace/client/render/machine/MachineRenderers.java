package com.ntmspace.client.render.machine;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.tileentity.machine.TileEntityMachineAlkylation;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/**
 * The progression machines' renderers (part one): hbm-space's {@code RenderCryoDistill}, {@code RenderAlkylation} and
 * {@code RenderMilkReformer}, declared through HBM's {@link HbmBlockEntityRenderers#regGem}. Each 1.7 renderer turned
 * its model by the block's facing ({@code getBlockMetadata() - offset}); the yaw tables are kept per renderer. 1.7's
 * 65,536 render distance squared is a 256-block view distance.
 */
public final class MachineRenderers {

    private MachineRenderers() {}

    /** A registration sink for {@link HbmBlockEntityRenderers#regGem}. */
    public static HbmBlockEntityRenderers.Sink sink(ClientRegistrations.Renderers renderers) {
        return new HbmBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends CompatRenderState> void register(BlockEntityType<? extends T> type,
                                                                                         CompatBERProvider<T, S> provider) {
                renderers.blockEntity(type, provider);
            }
        };
    }

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = sink(renderers);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachines.CRYO_DISTILL_BE.get(), CryoRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachines.ALKYLATION_BE.get(), AlkylationRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachines.MILK_REFORMER_BE.get(), MilkReformerRenderer::new);
    }

    public static WavefrontObj obj(String path) {
        return WavefrontObj.load(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "models/" + path));
    }

    public static String tex(String path) {
        return NtmSpace.MOD_ID + ":textures/models/" + path;
    }

    /** The yaw table most 1.7 machine renderers used: N 0, W 90, S 180, E 270. */
    public static float yawNWSE(BlockEntity be) {
        return switch (MachineCore.facingOrdinal(be.getBlockState(), 2)) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
    }

    public static final class YawState extends CompatRenderState {
        public float yaw;
    }

    /** {@code RenderCryoDistill}: two blocks down, turned 180 first, then N 0, W 90, S 180, E 270; culled. */
    static final class CryoRenderer extends GemBER<TileEntityMachineCryoDistill, YawState> {
        private final WavefrontObj model = obj("machines/cryo_distill.obj");
        private final Surface surface = Surface.cutoutCull(tex("machines/cryo_distiller.png"));

        @Override
        protected void setup(Gem.Model<YawState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, -2.0F, 0.5F)
                    .rotate(Gem.Axes.Y, 180F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.all(), surface);
        }

        @Override
        public YawState createRenderState() {
            return new YawState();
        }

        @Override
        public void extract(TileEntityMachineCryoDistill be, YawState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = yawNWSE(be);
        }
    }

    /** {@code RenderAlkylation}: N 0, W 90, S 180, E 270; culled. */
    static final class AlkylationRenderer extends GemBER<TileEntityMachineAlkylation, YawState> {
        private final WavefrontObj model = obj("machines/alkylation_unit.obj");
        private final Surface surface = Surface.cutoutCull(tex("machines/alkylation_unit.png"));

        @Override
        protected void setup(Gem.Model<YawState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0.0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.all(), surface);
        }

        @Override
        public YawState createRenderState() {
            return new YawState();
        }

        @Override
        public void extract(TileEntityMachineAlkylation be, YawState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = yawNWSE(be);
        }
    }

    /** {@code RenderMilkReformer}: turned 180 first, then N 90, W 180, S 270, E 0; drawn without culling. */
    static final class MilkReformerRenderer extends GemBER<TileEntityMachineMilkReformer, YawState> {
        private final WavefrontObj model = obj("machines/milker.obj");
        private final Surface surface = Surface.cutoutNoCull(tex("machines/milker.png"));

        @Override
        protected void setup(Gem.Model<YawState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0.0F, 0.5F)
                    .rotate(Gem.Axes.Y, 180F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.all(), surface);
        }

        @Override
        public YawState createRenderState() {
            return new YawState();
        }

        @Override
        public void extract(TileEntityMachineMilkReformer be, YawState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
                case 2 -> 90F;
                case 4 -> 180F;
                case 3 -> 270F;
                default -> 0F;
            };
        }
    }
}
