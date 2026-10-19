package com.ntmspace.client.render.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ntmspace.entity.rocket.EntityRideableRocketDummy;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The rocket's capsule hit box: 1.7 registered no renderer for it, so it draws nothing. */
public class RenderNothing extends CompatEntityRenderer<EntityRideableRocketDummy, CompatEntityRenderState> {

    public RenderNothing(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public CompatEntityRenderState createRenderState() {
        return new CompatEntityRenderState();
    }

    @Override
    public void extract(EntityRideableRocketDummy entity, CompatEntityRenderState state, float partialTicks) {}

    @Override
    public void submit(CompatEntityRenderState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {}
}
