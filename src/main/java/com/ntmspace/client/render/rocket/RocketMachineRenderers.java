package com.ntmspace.client.render.rocket;

import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.rocket.LaunchPadRocket;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketStruct.RocketStage;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code RenderRocketAssembly} and {@code RenderLaunchPadRocket}, their GL chains on a pose stack; the
 * rocket itself is {@link RocketPronter}, as in flight.
 */
public final class RocketMachineRenderers {

    private RocketMachineRenderers() {}

    private static WavefrontObj obj(String path) {
        return WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/" + path));
    }

    private static void drawPart(SubmitSink sink, PoseStack pose, WavefrontObj model, String part, RenderType type, int light) {
        sink.submitCustomGeometry(pose, type, (p, buffer) -> model.renderPart(part, p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    public static class RocketState extends CompatRenderState {
        public @Nullable RocketStruct rocket;
        public float yaw;
        public int height;
        public boolean canSeeSky;
    }

    /**
     * {@code RenderRocketAssembly}: the deck ({@code Base}, 2 below the core), and for a rocket without extra issues the
     * rocket standing on it (the bottom thruster sunk into the deck) and a {@code Support} column with a {@code Level}
     * deck at every stage's top. 1.7 switched its rotation on the raw core metadata (12..15), which no case matched:
     * the VAB is never turned.
     */
    public static final class Assembly implements CompatBER<TileEntityMachineRocketAssembly, RocketState> {
        private final WavefrontObj model = obj("machines/rocket_assembly.obj");
        private final RenderType skin = CompatRenderTypes.entityCutoutCull(Ids.of(NtmSpace.MOD_ID, "textures/models/machines/rocket_assembly.png"));
        private final RocketPronter pronter = new RocketPronter();

        @Override public RocketState createRenderState() { return new RocketState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityMachineRocketAssembly te, RocketState state, float partialTicks, Vec3 camera) {
            state.rocket = te.rocket;
        }

        @Override
        public void submit(RocketState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            pose.pushPose();
            pose.translate(0.5, -2.0, 0.5D);

            drawPart(sink, pose, model, "Base", skin, state.lightCoords);

            RocketStruct rocket = state.rocket;
            if (rocket != null && rocket.extraIssues.size() == 0) {
                pose.pushPose();
                pose.translate(0F, 2.95F, 0F);

                if (rocket.stages.size() > 0) {
                    RocketStage stage = rocket.stages.get(0);
                    if (stage.thruster != null) {
                        pose.translate(0, -stage.thruster.height(), 0);
                    }
                }

                pronter.pront(rocket, null, pose, sink, state.lightCoords, false, 0, 0, 0F);
                pose.popPose();

                for (int i = 0; i < rocket.stages.size(); i++) {
                    RocketStage stage = rocket.stages.get(i);
                    RocketStage nextStage = i < rocket.stages.size() - 1 ? rocket.stages.get(i + 1) : null;

                    double targetHeight = 0;
                    if (stage.fuselage != null) targetHeight += stage.fuselage.height() * stage.getStack();
                    if (nextStage != null && nextStage.thruster != null) targetHeight += nextStage.thruster.height();

                    while (targetHeight > 1) {
                        pose.translate(0, 1, 0);
                        drawPart(sink, pose, model, "Support", skin, state.lightCoords);
                        targetHeight--;
                    }
                    pose.translate(0, targetHeight, 0);

                    drawPart(sink, pose, model, "Level", skin, state.lightCoords);
                }
            }

            pose.popPose();
        }
    }

    /**
     * {@code RenderLaunchPadRocket}: the pad ({@code Base}) turned by its facing (N 90, W 180, S 270, E 0), the service
     * tower (base, a segment per 3 blocks, small segments for the rest and the cap) when the pad sees the sky and the
     * rocket is 8 or more high, and the rocket standing 3 up.
     */
    public static final class Pad implements CompatBER<TileEntityLaunchPadRocket, RocketState> {
        private final WavefrontObj model = obj("machines/rocket_pad.obj");
        private final RenderType skin = CompatRenderTypes.entityCutoutCull(Ids.of(NtmSpace.MOD_ID, "textures/models/machines/rocket_pad.png"));
        private final RenderType support = CompatRenderTypes.entityCutoutNoCull(Ids.of(NtmSpace.MOD_ID, "textures/models/machines/rocket_pad_support.png"));
        private final RocketPronter pronter = new RocketPronter();

        @Override public RocketState createRenderState() { return new RocketState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityLaunchPadRocket te, RocketState state, float partialTicks, Vec3 camera) {
            state.rocket = te.rocket;
            state.height = te.height;
            state.canSeeSky = te.canSeeSky;
            state.yaw = switch (ForgeDirection.getOrientation(te.getBlockState().getValue(LaunchPadRocket.FACING))) {
                case NORTH -> 90F;
                case WEST -> 180F;
                case SOUTH -> 270F;
                default -> 0F;
            };
        }

        @Override
        public void submit(RocketState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            pose.pushPose();
            pose.translate(0.5D, 0, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw));

            drawPart(sink, pose, model, "Base", skin, state.lightCoords);

            if (state.canSeeSky && state.height >= 8) {
                pose.pushPose();

                drawPart(sink, pose, model, "Tower_Base", support, state.lightCoords);

                for (int oy = 8; oy < state.height - 2; oy += 3) {
                    drawPart(sink, pose, model, "Tower_Segment", support, state.lightCoords);
                    pose.translate(0, 3, 0);
                }

                pose.translate(0, -3, 0);

                for (int oy = 0; oy < (state.height - 2) % 3; oy++) {
                    drawPart(sink, pose, model, "Tower_Segment_Small", support, state.lightCoords);
                    pose.translate(0, 1, 0);
                }

                pose.translate(0, -1, 0);
                drawPart(sink, pose, model, "Tower_Cap", support, state.lightCoords);

                pose.popPose();
            }

            if (state.rocket != null) {
                pose.translate(0.0F, 3.0F, 0.0F);
                pronter.pront(state.rocket, pose, sink, state.lightCoords);
            }

            pose.popPose();
        }
    }
}
