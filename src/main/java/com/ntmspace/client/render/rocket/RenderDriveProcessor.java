package com.ntmspace.client.render.rocket;

import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.rocket.MachineDriveProcessor;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code RenderDriveProcessor}: {@code drive_processor.obj} turned 90 degrees, then by the facing (1.7 meta
 * - offset: 2 north 0, 4 west 90, 3 south 180, 5 east 270), its {@code Base} always and the {@code Drive} while a drive
 * is in slot 0. The item ({@code renderInventory} scale 7, {@code renderCommon} translate (0.5, 0, 0), both groups) is
 * declared in {@code DriveProcessorBlocks}.
 */
public final class RenderDriveProcessor implements CompatBER<TileEntityMachineDriveProcessor, RenderDriveProcessor.State> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/machines/drive_processor.obj"));
    private final RenderType skin = CompatRenderTypes.entityCutoutCull(Ids.of(NtmSpace.MOD_ID, "textures/models/machines/drive_processor.png"));

    public static final class State extends CompatRenderState {
        public float yaw;
        public boolean hasDrive;
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }

    @Override
    public void extract(TileEntityMachineDriveProcessor te, State state, float partialTicks, Vec3 camera) {
        state.hasDrive = te.hasDrive;
        state.yaw = 90F + switch (ForgeDirection.getOrientation(te.getBlockState().getValue(MachineDriveProcessor.FACING))) {
            case NORTH -> 0F;
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));

        sink.submitCustomGeometry(pose, skin, (p, buffer) -> model.renderPart("Base", p, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        if (state.hasDrive) {
            sink.submitCustomGeometry(pose, skin, (p, buffer) -> model.renderPart("Drive", p, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        }

        pose.popPose();
    }
}
