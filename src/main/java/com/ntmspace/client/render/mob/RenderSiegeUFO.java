package com.ntmspace.client.render.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.entity.mob.siege.EntitySiegeUFO;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * hbm-space {@code RenderSiegeUFO}: {@code mini_ufo.obj} in its tier's skin ({@code ufo_siege_<tier>.png}), spinning
 * five degrees a tick, a quarter block up; once dead it tips about the (1, 0, 1) axis, and while it is hurt it rocks on
 * a wall-clock sine as 1.7 did. Drawn unculled, smooth-shaded.
 */
public class RenderSiegeUFO extends CompatEntityRenderer<EntitySiegeUFO, RenderSiegeUFO.State> {

    public RenderSiegeUFO(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySiegeUFO entity, State state, float partialTicks) {
        state.spin = (entity.tickCount + partialTicks) * 5 % 360D;
        state.alive = entity.isAlive();
        state.tilt = entity.deathTime + partialTicks;
        state.hurt = entity.invulnerableTime - partialTicks;
        state.hurtActive = entity.invulnerableTime > 0;
        state.skin = SpaceMobModels.texture("ufo_siege_" + entity.getTier().name);
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        ps.pushPose();
        ps.translate(0D, 0.25D, 0D);
        ps.mulPose(Axis.YP.rotationDegrees((float) state.spin));

        if (!state.alive) {
            // 1.7 glRotatef(tilt * 5, 1, 0, 1): GL normalises the axis
            ps.mulPose(new Quaternionf().rotateAxis(state.tilt * 5 * Mth.DEG_TO_RAD, 0.70710677F, 0F, 0.70710677F));
        } else if (state.hurtActive) {
            ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(System.currentTimeMillis() * 0.01D) * state.hurt)));
        }

        sink.submitCustomGeometry(ps, CompatRenderTypes.entityCutoutNoCull(state.skin),
                (pose, buffer) -> SpaceMobModels.miniUfo().renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        ps.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        double spin;
        boolean alive;
        float tilt;
        float hurt;
        boolean hurtActive;
        Identifier skin;
    }
}
