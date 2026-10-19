package com.ntmspace.client.render.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketParts;
import dev.golem.core.render.SubmitSink;
import net.minecraft.util.Mth;

/**
 * hbm-space's {@code RenderDropPod}: in orbit the whole pod; on a surface the body, the door swinging open on landing
 * and shut while awaiting launch (2 degrees a tick), four airbrakes out at 65 degrees while falling fast and folding in
 * over a second once the retro burn slows it, and the legs pulled in during launch.
 *
 * <p>Like 1.7's single renderer instance, the airbrake timing is one wall-clock state shared by every pod on screen.
 */
final class RenderDropPod implements RocketPronter.PartRenderer {

    private boolean brakes;
    private long brakeStart;

    @Override
    public void render(RocketPronter pronter, RocketRenderState rocket, PoseStack pose, SubmitSink sink, int light) {
        RocketPart pod = RocketParts.get(RocketParts.POD);
        if (pod == null) return;

        if (rocket.inOrbit) {
            pronter.draw(pod.mesh(), pod.skin(), pose, sink, light);
            return;
        }

        RocketState state = rocket.state;
        float lerpTimer = (float) rocket.stateTimer + rocket.interp;

        pronter.drawPart(pod.mesh(), "DropPod", pod.skin(), pose, sink, light);

        // Render door
        pose.pushPose();
        {
            float doorRotation = 0;
            if (state == RocketState.LANDED) {
                doorRotation = Mth.clamp(lerpTimer * 2, 0, 90);
            } else if (state == RocketState.AWAITING) {
                doorRotation = Mth.clamp(90 - lerpTimer * 2, 0, 90);
            }

            pose.translate(0.69291F, 2.8333F, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(doorRotation));
            pose.translate(-0.69291F, -2.8333F, 0);
            pronter.drawPart(pod.mesh(), "Door", pod.skin(), pose, sink, light);
        }
        pose.popPose();

        // Render airbrakes
        pose.pushPose();
        {
            float brakeRotation = 0;
            if (state == RocketState.LANDING) {
                if (rocket.motionY > -0.4F) {
                    if (brakes) {
                        brakes = false;
                        brakeStart = System.currentTimeMillis();
                    }
                } else {
                    brakes = true;
                }

                if (!brakes) {
                    float t = Mth.clamp((float) (System.currentTimeMillis() - brakeStart) / 1000, 0, 1);
                    brakeRotation = (1 - t) * 65;
                } else {
                    brakeRotation = 65;
                }
            }

            for (int i = 0; i < 4; i++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(i * 90 - 45));
                pose.translate(0.46194, 3.5, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(brakeRotation));
                pose.translate(-0.46194, -3.5, 0);
                pose.mulPose(Axis.YP.rotationDegrees(45));
                pronter.drawPart(pod.mesh(), "Airbrake0", pod.skin(), pose, sink, light);
                pose.popPose();
            }
        }
        pose.popPose();

        // Render legs
        pose.pushPose();
        {
            double legExtension = 1;
            if (state == RocketState.LAUNCHING) legExtension = 0;

            pose.translate(0, -legExtension * 0.5, 0);
            pronter.drawPart(pod.mesh(), "Legs", pod.skin(), pose, sink, light);
        }
        pose.popPose();
    }
}
