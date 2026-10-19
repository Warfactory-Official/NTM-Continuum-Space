package com.ntmspace.client.render.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ntmspace.entity.mob.EntityMoonCow;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.living.CompatEntityModel;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * hbm-space {@code RenderMoonCow} + {@code ModelMoonCow}: 1.7's {@code ModelCow} (its own boxes here, so the 64 x 32
 * {@code moon_cow.png} maps as in 1.7) with a 0.7-scale cube of HBM's boron glass on the head, drawn in the head's
 * frame a quarter block forward ({@code RenderUtil.renderBlock}). Shadow 0.7.
 *
 * <p>A calf draws as the adult at half size; 1.7's {@code ModelQuadruped} kept the calf's head at full size.
 */
public class RenderMoonCow extends CompatMobRenderer<EntityMoonCow, RenderMoonCow.State> {

    public RenderMoonCow(EntityRendererProvider.Context context) {
        super(context, LivingModel.custom(new ModelCow17(ModelCow17.createBodyLayer().bakeRoot())), 0.7F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected Identifier texture(State state) {
        return SpaceMobModels.MOON_COW;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        if (state.isBaby) poseStack.scale(0.5F, 0.5F, 0.5F);
    }

    /** The helmet: 1.7 drew it after the cow, in model space, from the head's pivot and rotations. */
    @Override
    protected void submitExtra(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.isInvisible) return;
        poseStack.pushPose();
        RenderObjMob.livingFrame(state, poseStack, flipDegrees(), state.isBaby ? 0.5F : 1F);
        float f5 = 0.0625F;
        poseStack.translate(0F * f5, 4F * f5, -8F * f5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        poseStack.translate(0F, 0F, -0.25F);
        poseStack.scale(0.7F, 0.7F, 0.7F);
        int light = state.lightCoords;
        int overlay = OverlayTexture.pack(OverlayTexture.u(0F), OverlayTexture.v(state.hasRedOverlay));
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.entityCutoutNoCull(SpaceMobModels.GLASS_BORON),
                (pose, buffer) -> renderBlock(pose, buffer, light, overlay));
        poseStack.popPose();
    }

    /** 1.7 {@code RenderUtil.renderBlock(tessellator, 0, 1)}: a unit cube about the origin, faces in its order and UVs. */
    private static void renderBlock(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
        float[][] faces = {
                {-0.5F, +0.5F, -0.5F, 1, 1, +0.5F, +0.5F, -0.5F, 0, 1, +0.5F, -0.5F, -0.5F, 0, 0, -0.5F, -0.5F, -0.5F, 1, 0, 0, 0, -1},
                {-0.5F, +0.5F, +0.5F, 1, 1, -0.5F, +0.5F, -0.5F, 0, 1, -0.5F, -0.5F, -0.5F, 0, 0, -0.5F, -0.5F, +0.5F, 1, 0, -1, 0, 0},
                {+0.5F, +0.5F, +0.5F, 1, 1, -0.5F, +0.5F, +0.5F, 0, 1, -0.5F, -0.5F, +0.5F, 0, 0, +0.5F, -0.5F, +0.5F, 1, 0, 0, 0, 1},
                {+0.5F, +0.5F, -0.5F, 1, 1, +0.5F, +0.5F, +0.5F, 0, 1, +0.5F, -0.5F, +0.5F, 0, 0, +0.5F, -0.5F, -0.5F, 1, 0, 1, 0, 0},
                {-0.5F, -0.5F, -0.5F, 1, 1, +0.5F, -0.5F, -0.5F, 0, 1, +0.5F, -0.5F, +0.5F, 0, 0, -0.5F, -0.5F, +0.5F, 1, 0, 0, -1, 0},
                {+0.5F, +0.5F, -0.5F, 1, 1, -0.5F, +0.5F, -0.5F, 0, 1, -0.5F, +0.5F, +0.5F, 0, 0, +0.5F, +0.5F, +0.5F, 1, 0, 0, 1, 0},
        };
        for (float[] f : faces) {
            for (int v = 0; v < 4; v++) {
                int i = v * 5;
                dev.golem.core.render.Vertexes.vertex(buffer, pose, f[i], f[i + 1], f[i + 2], -1, f[i + 3], f[i + 4],
                        overlay, light, f[20], f[21], f[22]);
            }
        }
    }

    public static final class State extends CompatLivingRenderState {}

    /**
     * 1.7 {@code ModelCow} ({@code ModelQuadruped(12, 0)} with the cow's head, horns, body and udder, legs moved one
     * pixel out and the front pair one pixel forward), posed by {@code ModelQuadruped.setRotationAngles}.
     */
    static final class ModelCow17 extends CompatEntityModel<State> {

        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart leg1;
        private final ModelPart leg2;
        private final ModelPart leg3;
        private final ModelPart leg4;

        ModelCow17(ModelPart root) {
            super(root);
            this.head = root.getChild("head");
            this.body = root.getChild("body");
            this.leg1 = root.getChild("leg1");
            this.leg2 = root.getChild("leg2");
            this.leg3 = root.getChild("leg3");
            this.leg4 = root.getChild("leg4");
        }

        static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4F, -4F, -6F, 8, 8, 6)
                            .texOffs(22, 0).addBox(-5F, -5F, -4F, 1, 3, 1)
                            .texOffs(22, 0).addBox(4F, -5F, -4F, 1, 3, 1),
                    PartPose.offset(0F, 4F, -8F));
            root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(18, 4).addBox(-6F, -10F, -7F, 12, 18, 10)
                            .texOffs(52, 0).addBox(-2F, 2F, -8F, 4, 6, 1),
                    PartPose.offset(0F, 5F, 2F));
            CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4, 12, 4);
            root.addOrReplaceChild("leg1", leg, PartPose.offset(-4F, 12F, 7F));
            root.addOrReplaceChild("leg2", leg, PartPose.offset(4F, 12F, 7F));
            root.addOrReplaceChild("leg3", leg, PartPose.offset(-4F, 12F, -6F));
            root.addOrReplaceChild("leg4", leg, PartPose.offset(4F, 12F, -6F));
            return LayerDefinition.create(mesh, 64, 32);
        }

        @Override
        protected void pose(State state) {
            float limbSwing = state.walkAnimationPos;
            float limbSwingAmount = state.walkAnimationSpeed;
            this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.body.xRot = (float) Math.PI / 2F;
            this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.leg3.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }
    }
}
