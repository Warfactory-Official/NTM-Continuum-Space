package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ntmspace.entity.mob.EntityTankbot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * hbm-space {@code RenderTankbot} + {@code ModelTankbot}: the OBJ at twice its size, four legs swinging from the walk
 * cycle and the turret turned by the head yaw less a smoothed yaw towards the target ("this makes NO SENSE but its
 * working okay", says the 1.7 comment; ported as written). 1.7 smoothed that yaw per rendered frame on the entity
 * ({@code setLivingAnimations}); {@link #extractMore} does the same on the client entity. No shadow
 * ({@code shadowOpaque = 0}).
 */
public class RenderTankbot extends RenderObjMob<EntityTankbot> {

    public RenderTankbot(EntityRendererProvider.Context context) {
        super(context, 1.0F);
        this.shadowStrength = 0F;
    }

    @Override
    protected Identifier skin(EntityTankbot entity) {
        return SpaceMobModels.TANKBOT;
    }

    @Override
    protected void extractMore(EntityTankbot entity, State state, float partialTicks) {
        Entity target = entity.level().getEntity(entity.getTargetId());
        if (target instanceof LivingEntity) {
            double dx = target.getX() - entity.getX();
            double dz = target.getZ() - entity.getZ();

            double targetYaw = Math.atan2(dz, dx) * (180 / Math.PI) - 90;
            double deltaYaw = Mth.wrapDegrees(targetYaw - entity.headTargetYaw);
            entity.headTargetYaw += deltaYaw * 0.1;
        }
        state.headTargetYaw = (float) entity.headTargetYaw;
        state.headYawAbsolute = entity.getYHeadRot();
    }

    @Override
    protected void draw(State state, PoseStack ps, Parts parts) {
        WavefrontObj m = SpaceMobModels.tankbot();

        rotX(ps, 180);
        ps.translate(0, -1.5F, 0);

        double s = 2;
        ps.scale((float) s, (float) s, (float) s);

        double walkCycle = state.walkAnimationPos;

        double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
        double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);

        parts.part(m, "body");

        ps.pushPose();
        ps.translate(0.25, 0.625, 0.0625);
        rotY(ps, 10);
        rotY(ps, -cy3 * 10);
        rotZ(ps, cy2 * -2);
        ps.translate(-0.25, -0.625, -0.0625);
        parts.part(m, "front2foot");
        parts.part(m, "front2leg");
        parts.part(m, "front2knee");
        ps.popPose();

        ps.pushPose();
        ps.translate(0.25, 0.625, 0.4375);
        rotY(ps, cy3 - cy2 * 10);
        rotZ(ps, cy2 - cy3 * -2);
        ps.translate(-0.25, -0.625, -0.4375);
        parts.part(m, "front1foot");
        parts.part(m, "front1leg");
        parts.part(m, "front1knee");
        ps.popPose();

        ps.pushPose();
        ps.translate(0.25, 0.625, 0.9375);
        rotY(ps, cy3 - cy2 * 10);
        rotZ(ps, cy2 - cy3 * -2);
        ps.translate(-0.25, -0.625, -0.9375);
        parts.part(m, "back1foot");
        parts.part(m, "back1leg");
        parts.part(m, "back1knee");
        ps.popPose();

        ps.pushPose();
        ps.translate(0.25, 0.625, 0.9375);
        rotY(ps, cy2 - cy3 * 5);
        rotX(ps, cy2 * 2);
        ps.translate(-0.25, -0.625, -0.9375);
        parts.part(m, "back2foot");
        parts.part(m, "back2leg");
        parts.part(m, "back2knee");
        ps.popPose();

        ps.pushPose();
        rotY(ps, -state.headTargetYaw + state.headYawAbsolute);
        rotY(ps, -90);
        parts.part(m, "head");
        parts.part(m, "barrelbottom");
        parts.part(m, "barreltop");
        ps.popPose();
    }
}
