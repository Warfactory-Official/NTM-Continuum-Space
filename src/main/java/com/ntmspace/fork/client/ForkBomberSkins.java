package com.ntmspace.fork.client;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.entity.RenderBomber;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.fork.ForkBombers;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.Random;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * hbm-space {@code RenderBomber}'s two new airframes, as HBM bomber skins (ADDON-API-INVENTORY C-25), after HBM's
 * heading, roll and wobble:
 * <ul>
 *   <li>9, the airliner: {@code S(30 / 3.1)}, {@code R(180, Y)}, the {@code 737} model with {@code airlinerplaceholder};</li>
 *   <li>10, the B2X: {@code S(3)}, {@code T(2, 0, 0)}, {@code R(-90, Y)}, {@code b2} with {@code b_2}, then two xenon
 *       exhaust plumes (colour 0.25 / 0.88 / 0.82, unlit, full-bright, blended without depth writes) at
 *       {@code (+-0.56, -0.56, 1)}, turned about, {@code S(0.5, 0.5, stretch)}, {@code T(0, 0, 1)}, each frame's stretch
 *       {@code 2 * (1.2 - r^2 * 0.2)} for a fresh random {@code r}.</li>
 * </ul>
 * 1.7 drew every bomber with culling off.
 */
public final class ForkBomberSkins {

    private ForkBomberSkins() {}

    private static final Identifier AIRLINER_TEX = Ids.of("ntmspace", "textures/models/airlinerplaceholder.png");
    private static final Identifier B2X_TEX = Ids.of("ntmspace", "textures/entity/b_2.png");
    private static final Identifier TRAIL_TEX = Ids.of("ntmspace", "textures/models/xenon_trail.png");
    private static final int TRAIL_COLOR = 0xFF000000 | (int) (0.25 * 255) << 16 | (int) (0.88 * 255) << 8 | (int) (0.82 * 255);

    private static WavefrontObj airliner;
    private static WavefrontObj b2x;
    private static WavefrontObj thruster;
    private static final Random RAND = new Random();

    public static void init() {
        RenderBomber.registerSkin(ForkBombers.AIRLINER_SKIN, ForkBomberSkins::airliner);
        RenderBomber.registerSkin(ForkBombers.B2X_SKIN, ForkBomberSkins::b2x);
    }

    private static void airliner(PoseStack pose, SubmitSink sink, int light) {
        if (airliner == null) airliner = WavefrontObj.load(Ids.of("ntmspace", "models/737.obj"));
        WavefrontObj model = airliner;
        float s = 30F / 3.1F;
        pose.scale(s, s, s);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(AIRLINER_TEX),
                (p, b) -> model.renderAll(p, b, light, OverlayTexture.NO_OVERLAY));
    }

    private static void b2x(PoseStack pose, SubmitSink sink, int light) {
        if (b2x == null) b2x = WavefrontObj.load(Ids.of("ntmspace", "models/mobs/b2.obj"));
        if (thruster == null) thruster = WavefrontObj.load(Ids.of("ntmspace", "models/xenon_thruster.obj"));
        WavefrontObj model = b2x;
        WavefrontObj exhaust = thruster;
        pose.scale(3F, 3F, 3F);
        pose.translate(2F, 0F, 0F);
        pose.mulPose(Axis.YP.rotationDegrees(-90F));
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(B2X_TEX),
                (p, b) -> model.renderAll(p, b, light, OverlayTexture.NO_OVERLAY));

        float r = RAND.nextFloat();
        float stretch = (1.2F - r * r * 0.2F) * 2F;
        RenderType trail = CompatRenderTypes.entityTranslucent(TRAIL_TEX);
        for (float side : new float[] {-0.56F, 0.56F}) {
            pose.pushPose();
            pose.translate(side, -0.56F, 1F);
            pose.mulPose(Axis.YP.rotationDegrees(180F));
            pose.scale(0.5F, 0.5F, stretch);
            pose.translate(0F, 0F, 1F);
            sink.submitCustomGeometry(pose, trail,
                    (p, b) -> exhaust.renderPart("Exhaust", p, b, 0xF000F0, OverlayTexture.NO_OVERLAY, TRAIL_COLOR));
            pose.popPose();
        }
    }
}
