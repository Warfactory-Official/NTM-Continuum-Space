package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.living.CompatEntityModel;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * A 1.7 {@code RenderLiving} whose {@code ModelBase} drew OBJ parts instead of boxes (hbm-space's scutterfish,
 * scuttlecrab, scrapfish, depth squid, sifter eel, tankbot and war behemoth models). The vanilla living renderer keeps
 * the body yaw, hurt flash, death tilt, shadow and name tag; the body model is empty, and the OBJ is drawn in
 * {@link #submitExtra} after restating 1.7's living frame exactly ({@link #livingFrame}), so the 1.7 model code's
 * GL transforms apply unchanged inside it.
 *
 * @param <T> the mob
 */
public abstract class RenderObjMob<T extends Mob> extends CompatMobRenderer<T, RenderObjMob.State> {

    protected RenderObjMob(EntityRendererProvider.Context context, float shadow) {
        super(context, LivingModel.custom(new EmptyModel()), shadow);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The skin of this frame (1.7 {@code getEntityTexture}). */
    protected abstract Identifier skin(T entity);

    /** Anything else the model reads, beyond what the living state carries. */
    protected void extractMore(T entity, State state, float partialTicks) {}

    /** 1.7 {@code ModelBase.render}, in the model space {@link #livingFrame} sets up. */
    protected abstract void draw(State state, PoseStack poseStack, Parts parts);

    @Override
    protected final void extract(T entity, State state, float partialTicks) {
        state.skin = skin(entity);
        extractMore(entity, state, partialTicks);
    }

    @Override
    protected Identifier texture(State state) {
        return state.skin;
    }

    @Override
    public void submitExtra(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.isInvisible) return;
        poseStack.pushPose();
        livingFrame(state, poseStack, flipDegrees());
        int overlay = OverlayTexture.pack(OverlayTexture.u(0F), OverlayTexture.v(state.hasRedOverlay));
        draw(state, poseStack, new Parts(sink, poseStack, state.skin, state.lightCoords, overlay));
        poseStack.popPose();
    }

    /**
     * 1.7 {@code RendererLivingEntity.doRender} up to the model: {@code rotateCorpse} (body yaw and the death tilt),
     * the {@code (-1, -1, 1)} flip and the {@code -24/16 - 1/128} lift.
     */
    public static void livingFrame(CompatLivingRenderState state, PoseStack poseStack, float flipDegrees) {
        livingFrame(state, poseStack, flipDegrees, 1F);
    }

    /** {@link #livingFrame} with the renderer's own scale ({@code preRenderCallback}, vanilla's {@code scale}) applied after the flip. */
    public static void livingFrame(CompatLivingRenderState state, PoseStack poseStack, float flipDegrees, float modelScale) {
        poseStack.scale(state.scale, state.scale, state.scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyRot));
        if (state.deathTime > 0F) {
            float f = Mth.sqrt((state.deathTime - 1F) / 20F * 1.6F);
            if (f > 1F) f = 1F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * flipDegrees));
        }
        poseStack.scale(-1F, -1F, 1F);
        poseStack.scale(modelScale, modelScale, modelScale);
        poseStack.translate(0F, -24F * 0.0625F - 0.0078125F, 0F);
    }

    /** 1.7 {@code GL11.glRotated(angle, x, y, z)} about a principal axis, in degrees. */
    protected static void rotX(PoseStack poseStack, double degrees) {
        poseStack.mulPose(Axis.XP.rotationDegrees((float) degrees));
    }

    protected static void rotY(PoseStack poseStack, double degrees) {
        poseStack.mulPose(Axis.YP.rotationDegrees((float) degrees));
    }

    protected static void rotZ(PoseStack poseStack, double degrees) {
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) degrees));
    }

    /** Draws named OBJ parts at the pose stack's current pose. */
    public static final class Parts {
        private final SubmitSink sink;
        private final PoseStack poseStack;
        private final Identifier texture;
        private final int light;
        private final int overlay;

        Parts(SubmitSink sink, PoseStack poseStack, Identifier texture, int light, int overlay) {
            this.sink = sink;
            this.poseStack = poseStack;
            this.texture = texture;
            this.light = light;
            this.overlay = overlay;
        }

        /** One part with the frame's skin (1.7 {@code renderPart}). */
        public void part(WavefrontObj model, String name) {
            part(model, name, this.texture);
        }

        /** One part with another sheet (1.7 rebound the texture between parts). */
        public void part(WavefrontObj model, String name, Identifier texture) {
            int light = this.light;
            int overlay = this.overlay;
            this.sink.submitCustomGeometry(this.poseStack, CompatRenderTypes.entityCutoutNoCull(texture),
                    (pose, buffer) -> model.renderPart(name, pose, buffer, light, overlay));
        }
    }

    public static final class State extends CompatLivingRenderState {
        public Identifier skin;
        /** The squid's interpolated tentacle angle (radians) and body pitch (degrees). */
        public float tentacleAngle;
        public float squidPitch;
        /** The tankbot's absolute head yaw and its smoothed yaw towards the target. */
        public float headYawAbsolute;
        public float headTargetYaw;
        /** The behemoth's tick-interpolated aiming pitch. */
        public float rawPitch;
    }

    /** A body with no parts: the OBJ is drawn in {@link #submitExtra}. */
    private static final class EmptyModel extends CompatEntityModel<State> {
        EmptyModel() {
            super(LayerDefinition.create(new MeshDefinition(), 16, 16).bakeRoot());
        }

        @Override
        protected void pose(State state) {}
    }
}
