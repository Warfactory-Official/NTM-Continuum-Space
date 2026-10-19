package com.ntmspace.world.gen.legacy;

/**
 * The 1.7.10 {@code MathHelper} functions the ported generators call. {@link #sin}/{@link #cos} read 1.7's 65536-entry
 * table with an {@code int} index cast; modern {@code Mth} casts through {@code long}, which differs for large angles,
 * so the 1.7 form is kept.
 */
public final class Math17 {

    private static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < 65536; ++i) SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0D / 65536.0D);
    }

    private Math17() {}

    public static float sin(float value) {
        return SIN_TABLE[(int) (value * 10430.378F) & 65535];
    }

    public static float cos(float value) {
        return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
    }

    public static float sqrt_float(float value) {
        return (float) Math.sqrt(value);
    }

    public static int floor_double(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    public static int ceiling_double_int(double value) {
        int i = (int) value;
        return value > i ? i + 1 : i;
    }

    public static double clamp_double(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    /** 1.7 {@code denormalizeClamp}: lerp from {@code low} to {@code high} by {@code t}, clamped to the ends. */
    public static double denormalizeClamp(double low, double high, double t) {
        return t < 0.0D ? low : (t > 1.0D ? high : low + (high - low) * t);
    }
}
