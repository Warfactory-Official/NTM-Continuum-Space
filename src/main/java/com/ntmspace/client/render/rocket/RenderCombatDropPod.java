package com.ntmspace.client.render.rocket;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ntmspace.NtmSpace;
import com.ntmspace.entity.missile.EntityCombatDropPod;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * hbm-space {@code RenderCombatDropPod}: the falling pod, the whole {@code combat_drop_pod.obj} at the entity's position
 * (1.7's translate up and back down by 7 cancels), culled and smooth-shaded, in the skin of its synced colour: 0 white,
 * 1 red, 2 yellow, anything else white. 1.7 set {@code ignoreFrustumCheck}; the distance gate stays with the entity.
 */
public class RenderCombatDropPod extends CompatEntityRenderer<EntityCombatDropPod, RenderCombatDropPod.State> {

    static final String MODEL = "models/combat_drop_pod.obj";
    private static WavefrontObj model;

    public RenderCombatDropPod(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    public static WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, MODEL));
        return model;
    }

    /** 1.7 {@code ResourceManager.combat_pod_skin_*} by colour. */
    public static String skin(int color) {
        String name = switch (color) {
            case 1 -> "red";
            case 2 -> "yellow";
            default -> "white";
        };
        return NtmSpace.MOD_ID + ":textures/models/bombs/combat_drop_pod." + name + ".png";
    }

    @Override
    public boolean culled(EntityCombatDropPod entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCombatDropPod entity, State state, float partialTicks) {
        state.color = entity.getColor();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        RenderType type = CompatRenderTypes.entityCutout(Ids.parse(skin(state.color)));
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                model().renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    public static final class State extends CompatEntityRenderState {
        int color;
    }
}
