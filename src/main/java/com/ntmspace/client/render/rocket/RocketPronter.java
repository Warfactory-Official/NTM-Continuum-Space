package com.ntmspace.client.render.rocket;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketStruct.RocketStage;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MissilePronter.prontRocket}: the stages bottom to top, each stage's fuselages stacked
 * ({@code getStack}) and clustered round the axis ({@code getCluster}), the bottom stage deployed (landing legs out on a
 * surface), the stage being dropped falling away ({@code decoupleTimer}), and the fairing ("shroud") between stages
 * drawn below the next stage's thruster and blown off during a transfer ({@code shroudTimer}). The capsule goes on top,
 * or its own renderer for the drop pod.
 *
 * <p>1.7 cut the shroud with {@code GL_CLIP_PLANE0} at the thruster's height ({@code y <= height} in the model's
 * frame); here the triangles are clipped against that plane on the CPU before they are submitted, the same cut.
 */
public final class RocketPronter {

    /** 1.7 {@code ResourceManager.universal}: the shroud's texture. */
    private static final Identifier UNIVERSAL = Ids.of("hbm", "textures/models/thegadget3_.png");

    private final Map<Identifier, WavefrontObj> meshes = new HashMap<>();
    private final Map<Identifier, RenderType> skins = new HashMap<>();

    /** A capsule with its own renderer (1.7 {@code RenderRocketPart}). */
    public interface PartRenderer {
        void render(RocketPronter pronter, RocketRenderState state, PoseStack pose, SubmitSink sink, int light);
    }

    private static final Map<String, PartRenderer> RENDERERS = new HashMap<>();

    static {
        RENDERERS.put("RenderDropPod", new RenderDropPod());
    }

    public WavefrontObj mesh(Identifier id) {
        return meshes.computeIfAbsent(id, WavefrontObj::load);
    }

    private RenderType skin(Identifier id) {
        return skins.computeIfAbsent(id, CompatRenderTypes::entityCutoutCull);
    }

    /** 1.7 {@code prontRocket(rocket, tex)}: deployed, no stage animation (the screens and the item). */
    public void pront(RocketStruct rocket, PoseStack pose, SubmitSink sink, int light) {
        pront(rocket, null, pose, sink, light, true, 0, 0, 0F);
    }

    /** Attaches a set of stages together. */
    public void pront(RocketStruct rocket, @Nullable RocketRenderState entity, PoseStack pose, SubmitSink sink, int light,
                      boolean isDeployed, int decoupleTimer, int shroudTimer, float interp) {
        pose.pushPose();

        boolean hasShroud = false;

        for (RocketStage stage : rocket.stages) {
            int stack = stage.getStack();
            int cluster = stage.getCluster();

            if (isDeployed && stage.thruster != null && stage.fins != null && stage.fins.height() > stage.thruster.height()) {
                pose.translate(0, stage.fins.height() - stage.thruster.height(), 0);
            }

            for (int c = 0; c < cluster; c++) {
                pose.pushPose();

                if (decoupleTimer > 0) {
                    float decoupleLerp = decoupleTimer + interp;
                    pose.translate(0, -decoupleLerp, 0);
                    pose.mulPose(Axis.XP.rotationDegrees(decoupleLerp));
                }

                if (c > 0) {
                    float spin = (float) c / (float) (cluster - 1);
                    pose.mulPose(Axis.YP.rotationDegrees(360.0F * spin));

                    if (stage.fuselage != null) {
                        pose.translate(stage.fuselage.bottomRadius(), 0, 0);
                    } else if (stage.thruster != null) {
                        pose.translate(stage.thruster.topRadius(), 0, 0);
                    }
                }

                if (stage.thruster != null) {
                    if (hasShroud && stage.fuselage != null) {
                        if (shroudTimer > 0) {
                            float shroudLerp = shroudTimer + interp;
                            pose.pushPose();
                            pose.translate(0, -shroudLerp, 0);
                            pose.mulPose(Axis.XP.rotationDegrees(shroudLerp * 0.5F));
                        }

                        drawClipped(stage.fuselage.getShroud(), UNIVERSAL, (float) stage.thruster.height(), pose, sink, light);

                        if (shroudTimer > 0) {
                            pose.popPose();
                        }
                    }

                    if (!hasShroud || shroudTimer > 0) {
                        draw(stage.thruster.getModel(isDeployed), stage.thruster.skin(), pose, sink, light);
                    }

                    pose.translate(0, stage.thruster.height(), 0);
                }

                if (stage.fuselage != null) {
                    if (stage.fins != null) {
                        draw(stage.fins.getModel(isDeployed), stage.fins.skin(), pose, sink, light);
                    }

                    for (int s = 0; s < stack; s++) {
                        draw(stage.fuselage.getModel(isDeployed), stage.fuselage.skin(), pose, sink, light);
                        pose.translate(0, stage.fuselage.height(), 0);
                    }
                }

                pose.popPose();
            }

            if (stage.thruster != null) pose.translate(0, stage.thruster.height(), 0);
            if (stage.fuselage != null) pose.translate(0, stage.fuselage.height() * stack, 0);

            // Only the bottom-most stage can be deployed OR staged
            isDeployed = false;
            decoupleTimer = 0;
            if (hasShroud) shroudTimer = 0; // Only the bottom-most shroud (second stage from bottom) should animate
            hasShroud = true;
        }

        RocketPart capsule = rocket.capsule;
        if (capsule != null) {
            PartRenderer renderer = capsule.renderer() == null ? null : RENDERERS.get(capsule.renderer());
            if (entity != null && renderer != null) {
                renderer.render(this, entity, pose, sink, light);
            } else {
                draw(capsule.mesh(), capsule.skin(), pose, sink, light);
            }
        }

        pose.popPose();
    }

    public void draw(Identifier meshId, Identifier skinId, PoseStack pose, SubmitSink sink, int light) {
        WavefrontObj mesh = mesh(meshId);
        sink.submitCustomGeometry(pose, skin(skinId), (p, buffer) -> mesh.renderAll(p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    public void drawPart(Identifier meshId, String part, Identifier skinId, PoseStack pose, SubmitSink sink, int light) {
        WavefrontObj mesh = mesh(meshId);
        sink.submitCustomGeometry(pose, skin(skinId), (p, buffer) -> mesh.renderPart(part, p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    /** The whole mesh, cut to {@code y <= maxY} (1.7's clip plane {@code (0, -1, 0, maxY)}). */
    private void drawClipped(Identifier meshId, Identifier skinId, float maxY, PoseStack pose, SubmitSink sink, int light) {
        float[] vertices = mesh(meshId).all().vertices();
        sink.submitCustomGeometry(pose, skin(skinId), (p, buffer) -> clip(vertices, maxY, p, buffer, light));
    }

    /** Sutherland-Hodgman against one plane: each triangle becomes nothing, itself, a triangle or a quad. */
    static void clip(float[] v, float maxY, PoseStack.Pose pose, VertexConsumer buffer, int light) {
        float[][] in = new float[3][];
        float[][] out = new float[4][];
        for (int t = 0; t + 24 <= v.length; t += 24) {
            for (int k = 0; k < 3; k++) {
                float[] vertex = new float[8];
                System.arraycopy(v, t + k * 8, vertex, 0, 8);
                in[k] = vertex;
            }
            int n = 0;
            for (int k = 0; k < 3; k++) {
                float[] a = in[k];
                float[] b = in[(k + 1) % 3];
                boolean aIn = a[1] <= maxY;
                boolean bIn = b[1] <= maxY;
                if (aIn) out[n++] = a;
                if (aIn != bIn) {
                    float f = (maxY - a[1]) / (b[1] - a[1]);
                    float[] m = new float[8];
                    for (int i = 0; i < 8; i++) m[i] = a[i] + (b[i] - a[i]) * f;
                    m[1] = maxY;
                    out[n++] = m;
                }
            }
            if (n < 3) continue;
            emit(out[0], pose, buffer, light);
            emit(out[1], pose, buffer, light);
            emit(out[2], pose, buffer, light);
            emit(n == 4 ? out[3] : out[2], pose, buffer, light);
        }
    }

    private static void emit(float[] a, PoseStack.Pose pose, VertexConsumer buffer, int light) {
        Vertexes.vertex(buffer, pose, a[0], a[1], a[2], -1, a[3], a[4], OverlayTexture.NO_OVERLAY, light, a[5], a[6], a[7]);
    }
}
