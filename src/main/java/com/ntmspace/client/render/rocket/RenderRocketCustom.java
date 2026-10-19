package com.ntmspace.client.render.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.orbit.Orbit;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * hbm-space's {@code RenderRocketCustom}: the rocket's stages under {@code Ry(yaw - 90) Rz(pitch) Ry(-(yaw - 90))},
 * deployed on a surface (legs out), not in orbit, with the stage separation and fairing timers.
 */
public class RenderRocketCustom extends CompatEntityRenderer<EntityRideableRocket, RocketRenderState> {

    private final RocketPronter pronter = new RocketPronter();

    public RenderRocketCustom(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntityRideableRocket entity) {
        return false;
    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }

    @Override
    public void extract(EntityRideableRocket entity, RocketRenderState state, float partialTicks) {
        state.yaw = entity.getViewYRot(partialTicks);
        state.pitch = entity.getPitch(partialTicks);
        state.rocket = entity.getRocket();
        state.state = entity.getState();
        state.stateTimer = entity.getStateTimer();
        state.interp = partialTicks;
        state.inOrbit = Orbit.inOrbit(entity.level());
        state.decoupleTimer = entity.decoupleTimer;
        state.shroudTimer = entity.shroudTimer;
        state.motionY = entity.getDeltaMovement().y;
        state.preview = entity.preview;
    }

    @Override
    public void submit(RocketRenderState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.rocket == null) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        pose.mulPose(Axis.YN.rotationDegrees(state.yaw - 90.0F));

        if (state.preview) {
            // 1.7 prontRocket(rocket, tex) in the screens: deployed, no stage animation, no part renderer
            pronter.pront(state.rocket, pose, sink, state.lightCoords);
        } else {
            pronter.pront(state.rocket, state, pose, sink, state.lightCoords, !state.inOrbit, state.decoupleTimer, state.shroudTimer, state.interp);
        }

        pose.popPose();
    }
}
