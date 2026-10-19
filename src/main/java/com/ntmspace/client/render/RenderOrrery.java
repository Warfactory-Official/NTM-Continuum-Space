package com.ntmspace.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.generic.BlockOrrery;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * hbm-space's {@code RenderOrrery} and {@code OrreryPronter}: from the block's centre, the star as a unit cube showing
 * the middle quarter of its texture (UV 0.375 to 0.625), then, scaled so the star's radius (at most
 * {@link SolarSystem#ORRERY_MAX_RADIUS} km) is one block, every body of the system as a cube of its own texture at its
 * squeezed orbital position ({@link SolarSystem#calculatePositionsOrrery}), its radius in km for an edge (small bodies
 * {@code (r / 63)^2 + 1000}), and its orbit as a closed 32-point line loop in the body's colour. 1.7 swapped y and z
 * (orbital plane = the world's horizontal plane).
 *
 * <p>1.7 drew the cubes full-bright and alpha-weighted additive ({@code SRC_ALPHA, ONE}) with face culling on and depth
 * writing on. The port uses Golem's {@code plasmaGlow} (the same blend, full-bright, depth-tested; it does not write
 * depth and does not cull), so the back faces are left out here, on the CPU, by the camera's side of each face. The
 * orbit lines are vanilla's line type (1.7 drew them under the same additive blend: on a dark sky a full-alpha line
 * reads the same, over a bright sky the port's line is opaque where 1.7's added).
 */
public final class RenderOrrery extends GemBER<BlockOrrery.TileEntityOrrery, RenderOrrery.State> {

    public static final class State extends CompatRenderState {
        public double worldTime;
        public float partialTicks;
    }

    private static final int FULL_BRIGHT = 0xF000F0;

    // 1.7 caches the metrics per (world time, partial tick); every orrery in view shares one list
    private static long cachedTime = Long.MIN_VALUE;
    private static float cachedPartial = Float.NaN;
    private static List<SolarSystem.OrreryMetric> metrics = List.of();

    @Override
    protected void setup(Gem.Model<State> declaration) {
        // 1.7 render box x-49..x+50, y-19..y+20 and getMaxRenderDistanceSquared 65536
        declaration.offScreen().viewDistance(256);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockOrrery.TileEntityOrrery orrery, State state, float partialTicks, Vec3 cameraPosition) {
        state.worldTime = orrery.getLevel() == null ? 0 : orrery.getLevel().getGameTime();
        state.partialTicks = partialTicks;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        SolarSystem.Body sun = SolarSystem.KERBOL;
        long time = (long) state.worldTime;
        if (time != cachedTime || state.partialTicks != cachedPartial) {
            metrics = SolarSystem.calculatePositionsOrrery(state.worldTime, state.partialTicks, sun);
            cachedTime = time;
            cachedPartial = state.partialTicks;
        }

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);

        // the sun: a unit cube, the middle of its texture
        cube(pose, sink, sun, 0.375F, 0.625F);

        // scale so the sun is 1x1x1
        double scale = 1 / Math.min(sun.radiusKm, SolarSystem.ORRERY_MAX_RADIUS);
        pose.scale((float) scale, (float) scale, (float) scale);

        for (SolarSystem.OrreryMetric metric : metrics) {
            pose.pushPose();
            double bodyScale = metric.body.radiusKm;
            if (bodyScale < 2_000) bodyScale = (bodyScale / 63) * (bodyScale / 63) + 1_000;
            pose.translate(metric.position[0], metric.position[2], metric.position[1]);
            pose.scale((float) bodyScale, (float) bodyScale, (float) bodyScale);
            cube(pose, sink, metric.body, 0F, 1F);
            pose.popPose();

            float[] c = metric.body.color;
            int color = 0xFF000000 | channel(c[0]) << 16 | channel(c[1]) << 8 | channel(c[2]);
            double[][] path = metric.orbitalPath;
            sink.submitCustomGeometry(pose, LineRenderTypes.lines(1F), (last, buffer) -> {
                for (int i = 1; i < path.length; i++) segment(buffer, last, path[i - 1], path[i], color);
                segment(buffer, last, path[path.length - 1], path[0], color);
            });
        }
        pose.popPose();
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, (int) (value * 255.0F)));
    }

    /** One {@code GL_LINES} pair, y and z swapped as 1.7 did. */
    private static void segment(VertexConsumer buffer, PoseStack.Pose pose, double[] from, double[] to, int color) {
        float x0 = (float) from[0], y0 = (float) from[2], z0 = (float) from[1];
        float x1 = (float) to[0], y1 = (float) to[2], z1 = (float) to[1];
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }
        Vertexes.line(buffer, pose, x0, y0, z0, color, dx, dy, dz, 1F);
        Vertexes.line(buffer, pose, x1, y1, z1, color, dx, dy, dz, 1F);
    }

    /**
     * 1.7 {@code RenderUtil.renderBlock(tessellator, uvMin, uvMax)}: a unit cube about the origin, each face's UV running
     * from {@code uvMax} to {@code uvMin}; only the faces turned to the camera (1.7's {@code GL_CULL_FACE}).
     */
    private static void cube(PoseStack pose, SubmitSink sink, SolarSystem.Body body, float uvMin, float uvMax) {
        Matrix4f matrix = new Matrix4f(pose.last().pose());
        Matrix3f normals = new Matrix3f(pose.last().normal());
        sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(Ids.of(NtmSpace.MOD_ID, body.texture())), (last, buffer) -> {
            for (float[][] face : FACES) {
                if (!facesCamera(matrix, normals, face[4])) continue;
                for (int i = 0; i < 4; i++) {
                    float[] v = face[i];
                    Vertexes.vertex(buffer, last, v[0], v[1], v[2], 1F, 1F, 1F, 1F,
                            v[3] > 0 ? uvMax : uvMin, v[4] > 0 ? uvMax : uvMin, OverlayTexture.NO_OVERLAY, FULL_BRIGHT,
                            face[4][0], face[4][1], face[4][2]);
                }
            }
        });
    }

    /** Whether the face with outward {@code normal} (centre = normal / 2) is turned to the camera at the origin. */
    private static boolean facesCamera(Matrix4f matrix, Matrix3f normals, float[] normal) {
        Vector3f centre = matrix.transformPosition(new Vector3f(normal[0] * 0.5F, normal[1] * 0.5F, normal[2] * 0.5F));
        Vector3f facing = normals.transform(new Vector3f(normal[0], normal[1], normal[2]));
        return facing.dot(centre) < 0;
    }

    /** 1.7's six faces in its vertex order: {x, y, z, u is max, v is max}, then the outward normal. */
    private static final float[][][] FACES = {
            {{-0.5F, +0.5F, -0.5F, 1, 1}, {+0.5F, +0.5F, -0.5F, 0, 1}, {+0.5F, -0.5F, -0.5F, 0, 0}, {-0.5F, -0.5F, -0.5F, 1, 0}, {0, 0, -1}},
            {{-0.5F, +0.5F, +0.5F, 1, 1}, {-0.5F, +0.5F, -0.5F, 0, 1}, {-0.5F, -0.5F, -0.5F, 0, 0}, {-0.5F, -0.5F, +0.5F, 1, 0}, {-1, 0, 0}},
            {{+0.5F, +0.5F, +0.5F, 1, 1}, {-0.5F, +0.5F, +0.5F, 0, 1}, {-0.5F, -0.5F, +0.5F, 0, 0}, {+0.5F, -0.5F, +0.5F, 1, 0}, {0, 0, 1}},
            {{+0.5F, +0.5F, -0.5F, 1, 1}, {+0.5F, +0.5F, +0.5F, 0, 1}, {+0.5F, -0.5F, +0.5F, 0, 0}, {+0.5F, -0.5F, -0.5F, 1, 0}, {1, 0, 0}},
            {{-0.5F, -0.5F, -0.5F, 1, 1}, {+0.5F, -0.5F, -0.5F, 0, 1}, {+0.5F, -0.5F, +0.5F, 0, 0}, {-0.5F, -0.5F, +0.5F, 1, 0}, {0, -1, 0}},
            {{+0.5F, +0.5F, -0.5F, 1, 1}, {-0.5F, +0.5F, -0.5F, 0, 1}, {-0.5F, +0.5F, +0.5F, 0, 0}, {+0.5F, +0.5F, +0.5F, 1, 0}, {0, 1, 0}},
    };
}
