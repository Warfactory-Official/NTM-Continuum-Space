package com.ntmspace.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ntmspace.NtmSpace;
import com.ntmspace.item.special.ItemModHeavyBoots;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.ModelParts;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Worn;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * hbm-space {@code ItemModHeavyBoots.armorRender}: worn boots draw the legs of 1.7's {@code ModelBiped(0.5F)}, unculled
 * like every 1.7 living model. Socketed boots draw nothing, as in 1.7.
 */
public class HeavyBootsRenderLayer extends CompatHumanoidLayer {

    private static final Identifier TEXTURE =
            Ids.of(NtmSpace.MOD_ID, "textures/entity/equipment/humanoid_leggings/heavy_boots.png");
    private static final ModelPart RIGHT_LEG = bake(false);
    private static final ModelPart LEFT_LEG = bake(true);

    @SuppressWarnings("rawtypes")
    public HeavyBootsRenderLayer(RenderLayerParent parent) {
        super(parent);
    }

    @Override
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
            CameraInfo camera) {
        // 1.7 drew them on players only; a 26.1 baby humanoid is a model of its own, so no baby draws them.
        if (parts.baby || !(worn.feet().getItem() instanceof ItemModHeavyBoots)) return;
        leg(pose, sink, light, parts.rightLeg(), RIGHT_LEG);
        leg(pose, sink, light, parts.leftLeg(), LEFT_LEG);
    }

    private static void leg(PoseStack pose, SubmitSink sink, int light, ModelPart wearer, ModelPart boot) {
        pose.pushPose();
        wearer.translateAndRotate(pose);
        ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutNoCull(TEXTURE), boot, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** A {@code ModelBiped(0.5F)} leg: the 4x12x4 box at UV 0, 16 grown by 0.5, mirrored on the left. */
    private static ModelPart bake(boolean left) {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder box = CubeListBuilder.create().texOffs(0, 16);
        if (left) box.mirror();
        mesh.getRoot().addOrReplaceChild("leg", box.addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
