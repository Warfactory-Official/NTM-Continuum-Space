package com.ntmspace.fork.client;

import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.RenderBobble;
import com.hbm.client.render.item.CustomItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.fork.bobble.BlockSpaceBobble;
import com.ntmspace.fork.bobble.BlockSpaceBobble.Type;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code RenderBobble} for its four figures: HBM's socket, rest-pose figure and engraved name
 * ({@code RenderBobble.renderFigure}, ADDON-API-INVENTORY C-24) at quarter scale, turned {@code -(22.5 * rotation + 90)};
 * MrKimkimora is hbm-space's {@code MrKimkimora} model part (extracted from its {@code bobble.obj}) in place of the
 * figure. The item ({@link Item}) is 1.7 {@code ItemRenderLibrary}'s bobblehead: the same {@code renderBobble} at HBM's
 * bobblehead item frame.
 */
public final class RenderSpaceBobble extends GemBER<BlockSpaceBobble.Bobble, RenderSpaceBobble.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public Type type = Type.GWEN;
        public long time;
    }

    private static final Identifier[] SKINS = new Identifier[Type.VALUES.length];

    static {
        for (Type type : Type.VALUES) SKINS[type.ordinal()] = Ids.of("ntmspace", "textures/models/trinkets/" + type.texture + ".png");
    }

    private static @Nullable WavefrontObj kimkimora;

    private static WavefrontObj kimkimora() {
        if (kimkimora == null) kimkimora = WavefrontObj.load(Ids.of("ntmspace", "models/trinkets/mrkimkimora.obj"));
        return kimkimora;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockSpaceBobble.Bobble bobble, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = -(22.5F * bobble.getBlockState().getValue(BlockTrinket.ROTATION) + 90F);
        state.type = bobble.type();
        state.time = System.currentTimeMillis();
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.scale(0.25F, 0.25F, 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        renderBobble(pose, sink, state.type, state.time, state.lightCoords);
        pose.popPose();
    }

    /** 1.7 {@code renderBobble(type)}. */
    static void renderBobble(PoseStack pose, SubmitSink sink, Type type, long time, int light) {
        Identifier skin = SKINS[type.ordinal()];
        boolean guy = type != Type.MRKIMKIMORA;
        RenderBobble.renderFigure(pose, sink, skin, type.skinLayers, guy, type.label, time, light);
        if (!guy) {
            WavefrontObj model = kimkimora();
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(skin),
                    (p, b) -> model.renderPart("MrKimkimora", p, b, light, OverlayTexture.NO_OVERLAY));
        }
    }

    /**
     * The bobblehead items' renderer ({@code ntmspace:bobblehead}, HBM's {@code hbm:custom_item}), 1.7
     * {@code ItemRenderLibrary}'s bobblehead: inventory {@code T(0, -3.5, 0) S(10)}, every slot {@code S(0.5)}, then
     * {@code renderBobble}, in the {@code machine} frame.
     */
    public static final class Item implements CustomItemRenderer {

        @Override
        public boolean handles(ItemStack stack, CustomItemRenderer.Type type) {
            return true;
        }

        @Override
        public void render(ItemStack stack, CustomItemRenderer.Type type, PoseStack pose, SubmitSink sink, int light,
                           int overlay, @Nullable LivingEntity owner) {
            BlockSpaceBobble.Type bobble = stack.getItem() instanceof com.hbm.items.block.ItemBlockDecoVariant variant
                    ? BlockSpaceBobble.Type.of(variant.variant()) : BlockSpaceBobble.Type.GWEN;
            pose.pushPose();
            if (type == CustomItemRenderer.Type.INVENTORY) {
                pose.translate(0D, -3.5D, 0D);
                pose.scale(10F, 10F, 10F);
            }
            pose.scale(0.5F, 0.5F, 0.5F);
            renderBobble(pose, sink, bobble, System.currentTimeMillis(), light);
            pose.popPose();
        }

        /** The head bobs on the wall clock. */
        @Override
        public boolean animated(ItemStack stack, CustomItemRenderer.Type type) {
            return true;
        }
    }
}
