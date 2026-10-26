package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ntmspace.entity.mob.EntityWarBehemoth;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * hbm-space {@code RenderBehemoth} + {@code ModelBehemoth}: the war machine at 1.2x, its hull pitched by the entity's
 * tick-interpolated aim pitch, six sheets (body, helmet, eye, hip, knee, leg) and two legs placed by a two-bone IK solve from a stepping
 * foot target. No shadow ({@code shadowOpaque = 0}).
 *
 * <p>The client entity eases its aim once per tick. Extraction reads that interpolated aim without changing entity
 * rotation, so render frame rate and repeated frame preparation cannot change its next simulation step.
 */
public class RenderBehemoth extends RenderObjMob<EntityWarBehemoth> {

    public RenderBehemoth(EntityRendererProvider.Context context) {
        super(context, 1.0F);
        this.shadowStrength = 0F;
    }

    @Override
    protected Identifier skin(EntityWarBehemoth entity) {
        return SpaceMobModels.BEHEMOTH_BODY;
    }

    @Override
    protected void extractMore(EntityWarBehemoth behemoth, State state, float partialTicks) {
        state.rawPitch = behemoth.aimPitch(partialTicks);
    }

    @Override
    protected void draw(State state, PoseStack ps, Parts parts) {
        WavefrontObj m = SpaceMobModels.behemoth();

        rotX(ps, 180);
        ps.translate(0, -1.5F, 0);

        double s = 1.2;
        ps.scale((float) s, (float) s, (float) s);

        double walkCycle = state.walkAnimationPos;

        ps.translate(0, -0.5F, 0F);

        rotY(ps, -180);

        ps.pushPose();
        ps.translate(0, 8.6F, 0.5F);
        rotX(ps, -state.rawPitch);
        ps.translate(0, -8.7F, -0.5F);
        parts.part(m, "body", SpaceMobModels.BEHEMOTH_BODY);
        parts.part(m, "hatch1", SpaceMobModels.BEHEMOTH_BODY);
        parts.part(m, "hatch2", SpaceMobModels.BEHEMOTH_BODY);
        parts.part(m, "gun", SpaceMobModels.BEHEMOTH_BODY);
        parts.part(m, "helmet", SpaceMobModels.BEHEMOTH_HELMET);
        parts.part(m, "eye", SpaceMobModels.BEHEMOTH_EYE);
        ps.popPose();

        double stepHeight = 5.5;
        double stepLength = 2.5;
        double stepTime = walkCycle * 0.25;

        double leftFootY = Math.sin(stepTime) * stepHeight - 2.0;
        double leftFootX = Math.sin(stepTime) * stepLength + 5.0;
        double leftFootZ = Math.cos(stepTime) * stepLength + 1.5;

        double rightFootY = Math.sin(stepTime + Math.PI) * stepHeight - 4.0;
        double rightFootX = Math.sin(stepTime + Math.PI) * stepLength + 5.0;
        double rightFootZ = Math.cos(stepTime + Math.PI) * stepLength + 1.5;

        leftFootY = Math.max(leftFootY, 7.0);
        rightFootY = Math.max(rightFootY, 7.0);

        ps.pushPose();
        renderLegIK(ps, parts, m, "right", rightFootX, rightFootY, rightFootZ);
        ps.popPose();

        ps.pushPose();
        renderLegIK(ps, parts, m, "left", leftFootX, leftFootY, leftFootZ);
        ps.popPose();
    }

    private static void renderLegIK(PoseStack ps, Parts parts, WavefrontObj m, String leg,
                                    double targetFootX, double targetFootY, double targetFootZ) {
        double thighLength = 6.0;
        double shinLength = 8.0;

        double deltaX = targetFootX;
        double deltaY = targetFootY - 8.5; // hip height offset
        double deltaZ = targetFootZ - 1.5; // foot z offset

        double distanceToTarget = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double cosKneeAngle = (thighLength * thighLength + shinLength * shinLength - distanceToTarget * distanceToTarget)
                / (2 * thighLength * shinLength);
        double kneeAngle = Math.acos(cosKneeAngle);
        double hipAngle = Math.atan2(deltaY, deltaX)
                - Math.atan2(shinLength * Math.sin(kneeAngle), thighLength + shinLength * Math.cos(kneeAngle));

        boolean right = leg.equals("right");
        ps.translate(0, 8.5F, 0.5F);
        rotX(ps, hipAngle * (180.0 / Math.PI));
        ps.translate(0, -8.6F, -0.5F);
        parts.part(m, right ? "hip_right" : "hip_left", SpaceMobModels.BEHEMOTH_HIP);
        ps.translate(0, 8.6F, 0.5F);

        ps.pushPose();
        ps.translate(0, -1.8F, -0F);
        rotX(ps, kneeAngle * (180.0 / Math.PI));

        ps.translate(0, -6.7F, -0.5F);
        parts.part(m, right ? "knee_right" : "knee_left", SpaceMobModels.BEHEMOTH_KNEE);
        ps.translate(0, 6.7F, 0.5F);

        ps.translate(0, -5.6F, 0F);
        rotX(ps, cosKneeAngle * (-90.0 / Math.PI));
        ps.translate(0, -1F, -0.5F);
        parts.part(m, right ? "leg_right" : "leg_left", SpaceMobModels.BEHEMOTH_LEG);

        ps.translate(0, 1F, 0.5F);
        ps.popPose();
    }
}
