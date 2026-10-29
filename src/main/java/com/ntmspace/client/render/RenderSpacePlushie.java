package com.ntmspace.client.render;

import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.RenderPlushie;
import com.hbm.client.render.item.CustomItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.generic.BlockSpacePlushie;
import com.ntmspace.block.generic.BlockSpacePlushie.Type;
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
 * hbm-space's {@code RenderPlushie} for its vocaloid plushies: from the block's centre at its foot, turned
 * {@code -(22.5 * rotation + 90)} about Y, wobbling while the squish timer runs ({@code 1 - sin(s) * s * 0.025} tall, s =
 * timer - partial tick), then Teto, Miku and Neru at half size and FAT TETO at full width and half height; each its OBJ
 * ({@code models/trinkets/teto|miku|neru.obj}; FAT TETO is Teto's), culled, with its blushing texture
 * ({@code _squished}) while the timer is above 6. The item ({@link Item}): inventory {@code T(0, -6, 0) S(6)}, every slot
 * {@code T(0, 0.25, 0)}, then {@code T(0, 0.25, 0) S(1.5)} (FAT TETO {@code S(3, 1.5, 3)}).
 */
public final class RenderSpacePlushie extends GemBER<BlockSpacePlushie.Plushie, RenderSpacePlushie.State> {

    public static final class State extends CompatRenderState {
        public int rotation;
        public Type type = Type.TETO;
        public float squish;
        public int squishTimer;
        public boolean miserable;
        public double misery;
    }

    private static final Identifier[] TEXTURE = new Identifier[Type.VALUES.length];
    private static final Identifier[] BLUSH = new Identifier[Type.VALUES.length];

    static {
        for (Type type : Type.VALUES) {
            String name = model(type);
            TEXTURE[type.ordinal()] = Ids.of(NtmSpace.MOD_ID, "textures/models/trinkets/" + name + ".png");
            BLUSH[type.ordinal()] = Ids.of(NtmSpace.MOD_ID, "textures/models/trinkets/" + name + "_squished.png");
        }
    }

    private final WavefrontObj[] models = new WavefrontObj[Type.VALUES.length];

    public RenderSpacePlushie() {
        for (Type type : Type.VALUES) {
            models[type.ordinal()] = WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/trinkets/" + model(type) + ".obj"));
        }
    }

    /** FAT TETO is drawn with Teto's model and textures. */
    private static String model(Type type) {
        return type == Type.FATO ? "teto" : type.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockSpacePlushie.Plushie plushie, State state, float interp, Vec3 cameraPosition) {
        state.rotation = plushie.getBlockState().getValue(BlockTrinket.ROTATION);
        state.type = plushie.type();
        state.squishTimer = plushie.squishTimer;
        state.squish = plushie.squishTimer > 0 ? plushie.squishTimer - interp : -1F;
        state.miserable = plushie.miseryFactor > 0.01F;
        state.misery = plushie.lastMiseryFactor + (plushie.miseryFactor - plushie.lastMiseryFactor) * interp;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        RenderPlushie.rootPose(pose, state.type, state.rotation, state.miserable, state.misery, state.squish >= 0F,
                state.squish);
        renderPlushie(pose, sink, state.type, state.squishTimer, state.lightCoords);
        pose.popPose();
    }

    /** 1.7 {@code renderPlushie(type, squishTimer)}. */
    void renderPlushie(PoseStack pose, SubmitSink sink, Type type, int squishTimer, int light) {
        WavefrontObj model = models[type.ordinal()];
        Identifier texture = squishTimer > 6 ? BLUSH[type.ordinal()] : TEXTURE[type.ordinal()];
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(texture),
                (p, b) -> model.renderAll(p, b, light, OverlayTexture.NO_OVERLAY));
    }

    /** The plushie items' renderer ({@code ntmspace:plushie}, HBM's {@code hbm:custom_item}). */
    public static final class Item implements CustomItemRenderer {

        private final RenderSpacePlushie renderer = new RenderSpacePlushie();

        @Override
        public boolean handles(ItemStack stack, CustomItemRenderer.Type type) {
            return true;
        }

        @Override
        public void render(ItemStack stack, CustomItemRenderer.Type type, PoseStack pose, SubmitSink sink, int light,
                           int overlay, @Nullable LivingEntity owner) {
            BlockSpacePlushie.Type plushie = stack.getItem() instanceof com.hbm.items.block.ItemBlockDecoVariant variant
                    ? BlockSpacePlushie.Type.of(variant.variant()) : BlockSpacePlushie.Type.TETO;
            pose.pushPose();
            if (type == CustomItemRenderer.Type.INVENTORY) {
                pose.translate(0D, -6D, 0D);
                pose.scale(6F, 6F, 6F);
            }
            pose.translate(0D, 0.25D, 0D);
            pose.translate(0D, 0.25D, 0D);
            if (plushie == BlockSpacePlushie.Type.FATO) {
                pose.scale(3.0F, 1.5F, 3.0F);
            } else {
                pose.scale(1.5F, 1.5F, 1.5F);
            }
            renderer.renderPlushie(pose, sink, plushie, 0, light);
            pose.popPose();
        }
    }
}
