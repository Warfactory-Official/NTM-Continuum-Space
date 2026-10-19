package com.ntmspace.world;

/**
 * The pure parts of hbm-space's StarDar planet drawing ({@code GUIMachineStardar.drawBody}/{@code drawBodyOverlays} and
 * {@code AtmosphereRenderUtil.renderAtmosphereGlow2D}), side-neutral so GameTests can pin them: the quads in 1.7's vertex
 * order and the per-body values the shader layers are fed.
 */
public final class StardarMath {

    private StardarMath() {}

    /**
     * {@code drawTexturedQuad(x, y, size, uOffset)}: a square of {@code size} centred on x, y, u from {@code uOffset} to
     * {@code 1 + uOffset} (the texture repeats), as {@code x y z u v} for the vertices (left bottom, right bottom, right
     * top, left top).
     */
    public static float[] texturedQuad(float x, float y, float size, float uOffset) {
        float half = size * 0.5F;
        float minU = uOffset;
        float maxU = 1F + uOffset;
        return new float[] {
                x - half, y + half, 0, minU, 1,
                x + half, y + half, 0, maxU, 1,
                x + half, y - half, 0, maxU, 0,
                x - half, y - half, 0, minU, 0};
    }

    /** {@code drawPartialTex(x, y, w, h, u1, v1, u2, v2)} as {@code x y z u v}. */
    public static float[] partialTex(float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
        return new float[] {
                x, y + h, 0, u1, v2,
                x + w, y + h, 0, u2, v2,
                x + w, y, 0, u2, v1,
                x, y, 0, u1, v1};
    }

    /**
     * {@code renderAtmosphereGlow2D(.., centerX, centerY, size, visibility 1)}: four bands around the body square, each two
     * quads (from nothing at the outer edge, {@code 1.15 * radius * (1 + glow / 4)}, to half the glow at
     * {@code 1.075 * radius} and the full glow at {@code 0.98 * radius}), in the atmosphere's colour times 1.15 clamped
     * to 1, as {@code x y z r g b a} per vertex in 1.7's order; null when the glow is at most 0.001 (none drawn).
     */
    public static float[] glowBand(double centerX, double centerY, double size, float glowAlpha, double atmoR, double atmoG,
                                   double atmoB) {
        if (glowAlpha <= 0.001F) return null;
        float r = clamp((float) atmoR * 1.15F), g = clamp((float) atmoG * 1.15F), b = clamp((float) atmoB * 1.15F);
        double radius = size * 0.5D;
        float leading = glowAlpha;
        float trailing = glowAlpha;
        double inner = radius * 0.98D;
        double middle = radius * 1.075D;
        double outer = radius * 1.15D * (1.0D + glowAlpha * 0.25D);
        double cx = centerX, cy = centerY;
        Vertices v = new Vertices(r, g, b);
        // Top band
        v.a = 0.0F;
        v.add(cx - outer, cy - outer).add(cx + outer, cy - outer);
        v.a = leading / 2.0F;
        v.add(cx + middle, cy - middle);
        v.a = trailing / 2.0F;
        v.add(cx - middle, cy - middle);
        v.add(cx - middle, cy - middle);
        v.a = leading / 2.0F;
        v.add(cx + middle, cy - middle);
        v.a = leading;
        v.add(cx + inner, cy - inner);
        v.a = trailing;
        v.add(cx - inner, cy - inner);
        // Left band (1.7's name; it is the +x side)
        v.a = 0.0F;
        v.add(cx + outer, cy - outer).add(cx + outer, cy + outer);
        v.a = leading / 2.0F;
        v.add(cx + middle, cy + middle).add(cx + middle, cy - middle);
        v.add(cx + middle, cy - middle).add(cx + middle, cy + middle);
        v.a = leading;
        v.add(cx + inner, cy + inner).add(cx + inner, cy - inner);
        // Bottom band
        v.a = 0.0F;
        v.add(cx + outer, cy + outer).add(cx - outer, cy + outer);
        v.a = trailing / 2.0F;
        v.add(cx - middle, cy + middle);
        v.a = leading / 2.0F;
        v.add(cx + middle, cy + middle);
        v.add(cx + middle, cy + middle);
        v.a = trailing / 2.0F;
        v.add(cx - middle, cy + middle);
        v.a = trailing;
        v.add(cx - inner, cy + inner);
        v.a = leading;
        v.add(cx + inner, cy + inner);
        // Right band
        v.a = 0.0F;
        v.add(cx - outer, cy + outer).add(cx - outer, cy - outer);
        v.a = trailing / 2.0F;
        v.add(cx - middle, cy - middle).add(cx - middle, cy + middle);
        v.add(cx - middle, cy + middle).add(cx - middle, cy - middle);
        v.a = trailing;
        v.add(cx - inner, cy - inner).add(cx - inner, cy + inner);
        return v.out;
    }

    /** 1.7 Tessellator colour state: the colour set last applies to the vertices added after it. */
    private static final class Vertices {
        final float[] out = new float[32 * 7];
        final float r, g, b;
        float a;
        int n;

        Vertices(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        Vertices add(double x, double y) {
            int o = n++ * 7;
            out[o] = (float) x;
            out[o + 1] = (float) y;
            out[o + 2] = 0;
            out[o + 3] = r;
            out[o + 4] = g;
            out[o + 5] = b;
            out[o + 6] = a;
            return this;
        }
    }

    private static float clamp(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }

    /**
     * {@code atmospherePatternOffset}: the texture offset, except on a body drawn with its offset (not turned) and a
     * usable rotational period, where it is the turns so far ({@code dayTicks / period}).
     */
    public static float patternOffset(boolean rotateBody, float textureUOffset, double period, double dayTicks) {
        float offset = textureUOffset;
        if (!rotateBody && period > 0D && !Double.isNaN(period) && !Double.isInfinite(period)) {
            offset = (float) (dayTicks / period);
        }
        return offset;
    }

    /**
     * The city-light texture index: the lights' intensity clamped to the {@code levels} textures while less than 40 ticks
     * have passed since an impact (or with none; {@code impactTime} 0), else 0 (no lights drawn).
     */
    public static int lightIntensity(boolean hasLights, int intensity, double impactTime, int levels) {
        if (!hasLights || impactTime >= 40.0D) return 0;
        return Math.max(0, Math.min(intensity, levels - 1));
    }

    /** {@code activeBlackouts}: one more city blacked out each 8 ticks after an impact, at most 5. */
    public static int blackouts(double impactTime) {
        return Math.max(0, Math.min((int) (impactTime / 8.0D), 5));
    }

    /** The impact's lava overlay alpha: it fades in over 10 ticks. */
    public static float lavaAlpha(double impactTime) {
        return (float) Math.min(impactTime * 0.1D, 1.0D);
    }
}
