package com.ntmspace.client;

import com.ntmspace.trait.CBT_Dyson;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.render.Sky;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * hbm-space's {@code SkyProviderCelestial.renderSwarm}: the star's Dyson swarms (all its {@code CBT_Dyson} members) as
 * black one-pixel points around the sun, drawn last in {@link MunSky#renderSun} (so on every body's sky and on the
 * station's) in the sun's frame.
 *
 * <p>1.7 drew {@code GL_POINTS} through {@code swarm.vert}: member {@code i} goes to ring {@code i % 3} (tilted 80 degrees
 * about X, then turned 0, 60 or -60 about Y), at angle {@code i + time} on a circle of radius {@code sunSize / 2} around
 * the sun, {@code time = (worldTime + partialTicks) / 800} (the level's time of day, Golem {@code TimeOfDay}); the shader lifts and squeezes each point by a hash of its
 * index within its ring's draw ({@code gl_VertexID}): {@code y = (cos(t) r + sin(t)(1 - r)) * 0.2 r2^2},
 * {@code o = cos(2y) * (1 - (r3 - 0.5) * 0.05)} with {@code t = id + time}, {@code hash(x) = fract(cos(124.123 x) * 412)}.
 * The sun was first drawn into the depth buffer as a quad a quarter of its size at 100.1, hiding the members behind it.
 * Alpha blended, black, alpha {@code clamp(displayHeight / fov * radius * 0.002, 0, 1)}.
 *
 * <p>The sky seam draws quads without depth, so here the shader runs on the CPU (in float, as on the GPU; the hash of a
 * large argument may round differently there, which only reshuffles the pseudo-random scatter), a member is dropped
 * when the ray from the eye to it passes through that quarter-size quad first (what the depth test did), and each point
 * is a camera-facing quad one pixel across at its distance. The field of view is the frame's dynamic one (Golem
 * {@code Fov.current}: 1.7's {@code getFOVModifier}, which sprinting and potions widen).
 */
public final class DysonSky {

    private static final float RING_TILT = 80.0F;
    private static final float[] RING_YAW = {0.0F, 60.0F, -60.0F};

    private DysonSky() {}

    static void renderSwarm(Sky sky, SolarSystem.Body sun, float sunSize) {
        CBT_Dyson dyson = CelestialTraits.getTrait(sun.name, CBT_Dyson.class, true);
        int swarmCount = dyson != null ? dyson.size() : 0;
        if (swarmCount <= 0) return;
        float[] quads = swarmQuads(sky.level(), sky.partialTick(), sunSize, swarmCount);
        if (quads.length >= 12) sky.quads(Sky.Blend.ALPHA, 0.0F, 0.0F, 0.0F, alpha(sunSize * 0.5), quads);
    }

    /** 1.7: {@code clamp((displayHeight / currentFov) * swarmRadius * 0.002, 0, 1)}. */
    static float alpha(double swarmRadius) {
        Minecraft mc = Minecraft.getInstance();
        float swarmScreenSize = (float) ((mc.getWindow().getHeight() / fov()) * swarmRadius * 0.002);
        return Mth.clamp(swarmScreenSize, 0, 1);
    }

    private static float fov() {
        return dev.golem.core.client.Fov.current();
    }

    private static float[] swarmQuads(ClientLevel level, float partialTicks, float sunSize, int swarmCount) {
        double swarmRadius = sunSize * 0.5;
        // 1.7 getWorldTime(): the time of day (the body's own clock on a body, the overworld's in orbit)
        float time = ((float) dev.golem.core.world.TimeOfDay.get(level) + partialTicks) / 800.0F;
        double pixel = Math.tan(Math.toRadians(fov()) / 2) / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
        double occluder = sunSize * 0.25D;

        float[] out = new float[swarmCount * 12];
        int n = 0;
        float tiltSin = (float) Math.sin(Math.toRadians(RING_TILT)), tiltCos = (float) Math.cos(Math.toRadians(RING_TILT));
        for (int ring = 0; ring < 3; ring++) {
            double yawSin = Math.sin(Math.toRadians(RING_YAW[ring])), yawCos = Math.cos(Math.toRadians(RING_YAW[ring]));
            int vertexId = 0;
            for (int i = ring; i < swarmCount; i += 3, vertexId++) {
                // the vertex: SkyProviderCelestial
                float t = i + time;
                double x = Math.cos(t);
                double z = Math.sin(t);
                // swarm.vert, gl_VertexID within this ring's draw
                float[] p = shader(vertexId, time, (float) x, (float) z);
                // glRotatef(80, X), then glRotatef(yaw, Y), glScaled(radius), glTranslatef(0, 100, 0)
                double ry = p[1] * tiltCos - p[2] * tiltSin;
                double rz = p[1] * tiltSin + p[2] * tiltCos;
                double qx = (p[0] * yawCos + rz * yawSin) * swarmRadius;
                double qz = (-p[0] * yawSin + rz * yawCos) * swarmRadius;
                double qy = ry * swarmRadius + 100.0D;

                // hidden by the sun's depth quad (a quarter of its size, at 100.1)
                if (qy > 100.1D && Math.abs(qx * 100.1D / qy) < occluder && Math.abs(qz * 100.1D / qy) < occluder) continue;

                n = point(out, n, qx, qy, qz, pixel);
            }
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    /** {@code swarm.vert} for one vertex: {@code (x * o, y, z * o)}. */
    static float[] shader(int vertexId, float iTime, float x, float z) {
        float v = vertexId;
        float t = v + iTime;
        float r = hash(v);
        float r2 = hash(v + 0.5F);
        float r3 = hash(v + 0.75F);

        float y = ((float) Math.cos(t) * r + (float) Math.sin(t) * (1.0F - r)) * 0.2F * r2 * r2;
        float o = (float) Math.cos(y * 2.0F) * (1.0F - ((r3 - 0.5F) * 0.05F));
        return new float[] {x * o, y, z * o};
    }

    /** {@code float hash(float x) { return fract(cos(x * 124.123) * 412.0); }} */
    static float hash(float x) {
        float c = (float) Math.cos(x * 124.123F) * 412.0F;
        return c - (float) Math.floor(c);
    }

    /** One quad facing the eye (at the origin), one pixel across at its distance, wound counter-clockwise as seen. */
    private static int point(float[] out, int n, double qx, double qy, double qz, double pixel) {
        double d = Math.sqrt(qx * qx + qy * qy + qz * qz);
        if (d < 1.0E-6) return n;
        double nx = qx / d, ny = qy / d, nz = qz / d;
        // u = n x up (or n x east when looking straight up), w = u x n: u x w = -n, towards the eye
        double ux = ny * 0 - nz * 1, uy = nz * 0 - nx * 0, uz = nx * 1 - ny * 0;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1.0E-6) {
            ux = 0; uy = nz; uz = -ny;
            ul = Math.sqrt(uy * uy + uz * uz);
        }
        ux /= ul; uy /= ul; uz /= ul;
        double wx = uy * nz - uz * ny, wy = uz * nx - ux * nz, wz = ux * ny - uy * nx;
        double h = d * pixel; // half a pixel at distance d
        double[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (double[] c : corners) {
            out[n++] = (float) (qx + (ux * c[0] + wx * c[1]) * h);
            out[n++] = (float) (qy + (uy * c[0] + wy * c[1]) * h);
            out[n++] = (float) (qz + (uz * c[0] + wz * c[1]) * h);
        }
        return n;
    }
}
