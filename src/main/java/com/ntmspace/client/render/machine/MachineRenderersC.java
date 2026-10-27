package com.ntmspace.client.render.machine;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.FoundryRenderHelper;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.tileentity.machine.TileEntityMachineGasDock;
import com.ntmspace.tileentity.machine.TileEntityMachineMagma;
import com.ntmspace.tileentity.machine.TileEntityMachineSolarPanel;
import com.ntmspace.tileentity.machine.TileEntityRadiator;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Sprites;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.Set;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The progression machines' renderers, part C: hbm-space's {@code RenderMagma}, {@code RenderSolarPanel},
 * {@code RenderRadiator} and {@code RenderDecoBlock}'s gas dock, through HBM's {@link HbmBlockEntityRenderers#regGem}.
 * Each keeps its 1.7 yaw table; 1.7's 65,536 render distance squared is a 256-block view distance.
 */
public final class MachineRenderersC {

    private MachineRenderersC() {}

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = MachineRenderers.sink(renderers);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesC.MAGMA_BE.get(), MagmaRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesC.SOLAR_BE.get(), SolarRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesC.RADIATOR_BE.get(), RadiatorRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, SpaceMachinesC.GAS_DOCK_BE.get(), GasDockRenderer::new);
    }

    // ---------------------------------------------------------------------------------------- magma drill

    public static final class MagmaState extends CompatRenderState {
        public float yaw;
        public float drill;
        public float lava;
        public int dirOrdinal;
        public int pourColor = -1;
        public float pourLen;
    }

    /**
     * {@code RenderMagma}: S 0, E 90, N 180, W 270, three blocks down; everything but the drill head and the blades,
     * the drill head turning with the drill and the blades against it; a 3x3 lava surface at the lava height (the top
     * face of {@code Blocks.lava}'s still icon), and the old {@code "foundry"} pour stream out of the front.
     */
    static final class MagmaRenderer extends GemBER<TileEntityMachineMagma, MagmaState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/magma_drill.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/magma_drill.png"));
        private final RenderType lavaType = CompatRenderTypes.entitySolid(dev.golem.core.Ids.parse("minecraft:textures/atlas/blocks.png"));
        private final RenderType streamType = CompatRenderTypes.entitySolid(dev.golem.core.Ids.of("hbm", "textures/models/machines/lava_gray.png"));

        @Override
        protected void setup(Gem.Model<MagmaState> declaration) {
            Gem.Part<MagmaState> machine = declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .translate(0F, -3F, 0F);
            // 1.7 named the parts "DrillHead" and "Blades"; its OBJ loader matched names ignoring case ("Drillhead")
            machine.child(model.allExcept(Set.of("Drillhead", "Blades")), surface);
            machine.child(model.part("Drillhead"), surface).angle(Gem.Axes.Y, state -> state.drill);
            machine.child(model.part("Blades"), surface).angle(Gem.Axes.Y, state -> -state.drill);
        }

        @Override
        public MagmaState createRenderState() {
            return new MagmaState();
        }

        @Override
        public void extract(TileEntityMachineMagma be, MagmaState state, float partialTicks, Vec3 cameraPosition) {
            int meta = MachineCore.facingOrdinal(be.getBlockState(), 3);
            state.dirOrdinal = meta;
            state.yaw = switch (meta) {
                case 5 -> 90F;
                case 2 -> 180F;
                case 4 -> 270F;
                default -> 0F;
            };
            state.drill = be.prevDrillRotation + (be.drillRotation - be.prevDrillRotation) * partialTicks;
            state.lava = be.prevLavaHeight + (be.lavaHeight - be.prevLavaHeight) * partialTicks;
            state.pourColor = be.pourColor;
            state.pourLen = be.pourLen;
        }

        @Override
        protected void submitUndeclared(MagmaState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            if (state.lava > 0.01F) {
                poseStack.pushPose();
                poseStack.translate(0.5F, 0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
                poseStack.translate(0F, -3F, 0F);
                float height = state.lava;
                int light = state.lightCoords;
                sink.submitCustomGeometry(poseStack, lavaType, (pose, buffer) -> lava(pose, buffer, height, light));
                poseStack.popPose();
            }
            if (state.pourColor >= 0 && state.pourLen > 0) {
                ForgeDirection dir = ForgeDirection.getOrientation(state.dirOrdinal);
                poseStack.pushPose();
                poseStack.translate(0.5F + dir.offsetX * 3.875F, 1F, 0.5F + dir.offsetZ * 3.875F);
                float len = state.pourLen;
                int rgb = state.pourColor;
                sink.submitCustomGeometry(poseStack, streamType,
                        (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, dir, len, 0.625F, 0.625F, rgb));
                poseStack.popPose();
            }
        }

        /** 1.7 {@code renderFaceYPos(Blocks.lava, ox - 0.5, 0, oz - 0.5, lava)} with bounds up to the lava height, ox/oz -1..1. */
        private static void lava(PoseStack.Pose pose, VertexConsumer buffer, float height, int light) {
            TextureAtlasSprite sprite = Sprites.sprite(Sprites.Atlas.BLOCKS, dev.golem.core.Ids.parse("minecraft:block/lava_still"));
            float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    float x0 = ox - 0.5F, x1 = ox + 0.5F, z0 = oz - 0.5F, z1 = oz + 0.5F;
                    up(buffer, pose, x0, height, z0, u0, v0, light);
                    up(buffer, pose, x0, height, z1, u0, v1, light);
                    up(buffer, pose, x1, height, z1, u1, v1, light);
                    up(buffer, pose, x1, height, z0, u1, v0, light);
                }
            }
        }

        private static void up(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
            Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
        }
    }

    // ---------------------------------------------------------------------------------------- solar panel

    public static final class SolarState extends CompatRenderState {
        public float yaw;
        public boolean south, north, east, west, up, down;
    }

    /**
     * {@code RenderSolarPanel}: the panel turned 90 and then N 0, W 90, S 180, E 270; a connector on each face of the
     * core where {@code Library.canConnect} finds a power connector (south/north as drawn, east/west turned 90; not
     * turned with the panel).
     */
    static final class SolarRenderer extends GemBER<TileEntityMachineSolarPanel, SolarState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/solar_panel.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/solar_panel.png"));

        @Override
        protected void setup(Gem.Model<SolarState> declaration) {
            Gem.Part<SolarState> root = declaration.offScreen().viewDistance(256).joint().translate(0.5F, 0F, 0.5F);
            root.joint().rotate(Gem.Axes.Y, 90F).angle(Gem.Axes.Y, state -> state.yaw)
                    .child(model.part("solar_panel"), surface);
            root.child(model.part("connector_front"), surface).visibleIf(state -> state.south);
            root.child(model.part("connector_side"), surface).visibleIf(state -> state.north);
            Gem.Part<SolarState> turned = root.joint().rotate(Gem.Axes.Y, 90F);
            turned.child(model.part("connector_front"), surface).visibleIf(state -> state.east);
            turned.child(model.part("connector_side"), surface).visibleIf(state -> state.west);
            root.child(model.part("connector_top"), surface).visibleIf(state -> state.up);
            root.child(model.part("connector_bottom"), surface).visibleIf(state -> state.down);
        }

        @Override
        public SolarState createRenderState() {
            return new SolarState();
        }

        @Override
        public void extract(TileEntityMachineSolarPanel be, SolarState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = MachineRenderers.yawNWSE(be);
            var mask = be.getPowerConnectionMask();
            state.south = (mask & 1 << Direction.SOUTH.ordinal()) != 0;
            state.north = (mask & 1 << Direction.NORTH.ordinal()) != 0;
            state.east = (mask & 1 << Direction.EAST.ordinal()) != 0;
            state.west = (mask & 1 << Direction.WEST.ordinal()) != 0;
            state.up = (mask & 1 << Direction.UP.ordinal()) != 0;
            state.down = (mask & 1 << Direction.DOWN.ordinal()) != 0;
        }
    }

    // ---------------------------------------------------------------------------------------- radiator

    /** {@code RenderRadiator}: centred half a block up, N 90, W 180, S 270, E 0, half a block back, laid on its side. */
    static final class RadiatorRenderer extends GemBER<TileEntityRadiator, MachineRenderers.YawState> {
        private final WavefrontObj model = MachineRenderers.obj("machines/radiator.obj");
        private final Surface surface = Surface.cutoutCull(MachineRenderers.tex("machines/radiator.png"));

        @Override
        protected void setup(Gem.Model<MachineRenderers.YawState> declaration) {
            declaration.offScreen().viewDistance(256).joint()
                    .translate(0.5F, 0.5F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw)
                    .translate(-0.5F, 0F, 0F)
                    .rotate(Gem.Axes.Z, -90F)
                    .child(model.all(), surface);
        }

        @Override
        public MachineRenderers.YawState createRenderState() {
            return new MachineRenderers.YawState();
        }

        @Override
        public void extract(TileEntityRadiator be, MachineRenderers.YawState state, float partialTicks, Vec3 cameraPosition) {
            state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 5)) {
                case 2 -> 90F;
                case 4 -> 180F;
                case 3 -> 270F;
                default -> 0F;
            };
        }
    }

    // ---------------------------------------------------------------------------------------- gas dock

    public static final class GasDockState extends CompatRenderState {
        public float lift;
        public boolean rocket;
    }

    /**
     * {@code RenderDecoBlock} for {@code gas_dock}: its {@code T(0.5, 1.5, 0.5) Rz(180)} and the dock's
     * {@code Rz(180) T(0, -1.5, 0)} cancel to {@code T(0.5, 0, 0.5)}; the sat dock model in {@code gas_dock.png}, and
     * while {@code launchTicks < 100} the miner rocket in {@code gas_rocket.png} (1.7's {@code gasRocket.png}, renamed:
     * a resource path is lower case, the mixed-case one crashed the renderer) (culling off), 0.75 above the pad plus
     * its flight height.
     */
    static final class GasDockRenderer extends GemBER<TileEntityMachineGasDock, GasDockState> {
        private final WavefrontObj pad = WavefrontObj.load(dev.golem.core.Ids.of("hbm", "models/sat_dock.obj"));
        private final WavefrontObj rocket = WavefrontObj.load(dev.golem.core.Ids.of("hbm", "models/miner_rocket.obj"));
        private final Surface padSurface = Surface.cutoutCull(MachineRenderers.tex("gas_dock.png"));
        private final Surface rocketSurface = Surface.cutoutNoCull(NtmSpace.MOD_ID + ":textures/models/gas_rocket.png");

        @Override
        protected void setup(Gem.Model<GasDockState> declaration) {
            Gem.Part<GasDockState> root = declaration.offScreen().viewDistance(256).joint().translate(0.5F, 0F, 0.5F);
            root.child(pad.all(), padSurface);
            root.child(rocket.all(), rocketSurface).slide(Gem.Axes.Y, state -> state.lift).visibleIf(state -> state.rocket);
        }

        @Override
        public GasDockState createRenderState() {
            return new GasDockState();
        }

        @Override
        public void extract(TileEntityMachineGasDock dock, GasDockState state, float partialTicks, Vec3 cameraPosition) {
            state.rocket = dock.launchTicks < 100;
            state.lift = 0.75F + Mth.clamp(dock.launchTicks + (dock.hasRocket ? -partialTicks : partialTicks), 0, 200);
        }
    }
}
