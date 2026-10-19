package com.ntmspace.satellite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

/**
 * hbm-space's per-satellite orbit settings ({@code SatelliteBase} fields and the {@code sat*} item NBT): inclination,
 * altitude in km, phase offset, blinking and its period, owner and colour. A satellite item carries them in the
 * {@code ntmspace:sat_orbit} component (the defaults where it has none, 1.7 {@code ensureItemData}); a satellite in
 * orbit carries them in HBM's {@code Satellite.extra} under {@link #KEY}. Saved by name, no numbers but the values.
 */
public record SatelliteOrbit(float inclination, float altitude, float phaseOffset, boolean blinking, float blinkPeriod,
                             String owner, float colorR, float colorG, float colorB) {

    public static final float DEFAULT_INCLINATION = 0F;
    public static final float MIN_INCLINATION = -180.0F;
    public static final float MAX_INCLINATION = 180.0F;
    /** 1.7 {@code AstronomyUtil.DEFAULT_ALTITUDE_KM}. */
    public static final float DEFAULT_ALTITUDE_KM = 100.0F;
    public static final float MIN_ALTITUDE_KM = 80.0F;
    public static final float MAX_ALTITUDE_KM = 125.0F;
    public static final float MIN_BLINK_PERIOD = 0.3F;
    public static final float MAX_BLINK_PERIOD = 1.0F;
    public static final boolean DEFAULT_IS_BLINKING = false;
    public static final float DEFAULT_BLINK_PERIOD = MIN_BLINK_PERIOD;
    public static final String DEFAULT_OWNER = "None";
    public static final float DEFAULT_PHASE_OFFSET = 0.0F;

    /** The key in {@code Satellite.extra}. */
    public static final String KEY = "ntmspace:orbit";

    public static final Codec<SatelliteOrbit> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("inclination", DEFAULT_INCLINATION).forGetter(SatelliteOrbit::inclination),
            Codec.FLOAT.optionalFieldOf("altitude", DEFAULT_ALTITUDE_KM).forGetter(SatelliteOrbit::altitude),
            Codec.FLOAT.optionalFieldOf("phase_offset", DEFAULT_PHASE_OFFSET).forGetter(SatelliteOrbit::phaseOffset),
            Codec.BOOL.optionalFieldOf("blinking", DEFAULT_IS_BLINKING).forGetter(SatelliteOrbit::blinking),
            Codec.FLOAT.optionalFieldOf("blink_period", DEFAULT_BLINK_PERIOD).forGetter(SatelliteOrbit::blinkPeriod),
            Codec.STRING.optionalFieldOf("owner", DEFAULT_OWNER).forGetter(SatelliteOrbit::owner),
            Codec.FLOAT.fieldOf("r").forGetter(SatelliteOrbit::colorR),
            Codec.FLOAT.fieldOf("g").forGetter(SatelliteOrbit::colorG),
            Codec.FLOAT.fieldOf("b").forGetter(SatelliteOrbit::colorB)
    ).apply(i, SatelliteOrbit::new));

    public SatelliteOrbit {
        phaseOffset = normalizePhaseOffset(phaseOffset);
        blinkPeriod = clampBlinkPeriod(blinkPeriod);
    }

    /** 1.7 {@code getItemData} on a chip without NBT: the defaults and the satellite class's registered colour. */
    public static SatelliteOrbit defaults(float[] color) {
        return new SatelliteOrbit(DEFAULT_INCLINATION, DEFAULT_ALTITUDE_KM, DEFAULT_PHASE_OFFSET, DEFAULT_IS_BLINKING,
                DEFAULT_BLINK_PERIOD, DEFAULT_OWNER, color[0], color[1], color[2]);
    }

    /** Stores the settings on a satellite's {@code extra} data under {@link #KEY} (1.7 {@code SatelliteBase.writeToNBT}). */
    public void write(CompoundTag extra) {
        CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this).result().ifPresent(tag -> extra.put(KEY, tag));
    }

    /** The settings a satellite's {@code extra} data holds, or the defaults with {@code color} (1.7 {@code readFromNBT}). */
    public static SatelliteOrbit read(CompoundTag extra, float[] color) {
        net.minecraft.nbt.Tag tag = extra.get(KEY);
        if (tag == null) return defaults(color);
        return CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).result().orElseGet(() -> defaults(color));
    }

    public static float clampBlinkPeriod(float blinkPeriod) {
        return Math.max(MIN_BLINK_PERIOD, Math.min(MAX_BLINK_PERIOD, blinkPeriod));
    }

    public static float normalizePhaseOffset(float phaseOffset) {
        float wrapped = phaseOffset % 360.0F;
        if (wrapped < 0.0F) wrapped += 360.0F;
        return wrapped;
    }

    /** 1.7 {@code getOrbitSpeedKmPerSecond}. */
    public float orbitSpeedKmPerSecond() {
        double radiusKm = Math.max(1.0D, altitude);
        double turnsPerSecond = altitudeOrbitSpeed(altitude) / 30.0D;
        return (float) (2.0D * Math.PI * radiusKm * turnsPerSecond);
    }

    /** 1.7 {@code applyPhaseOffsetToOrbitAngle}. */
    public static float applyPhaseOffsetToOrbitAngle(float phaseOffset, float altitude, double baseAngle, float fullRotation) {
        double orbitSpeed = altitudeOrbitSpeed(altitude);
        double phase = normalizePhaseOffset(phaseOffset) / 360.0D * fullRotation;
        double angle = baseAngle * orbitSpeed + phase;
        double wrapped = angle % fullRotation;
        if (wrapped < 0.0D) wrapped += fullRotation;
        return (float) wrapped;
    }

    private static double altitudeOrbitSpeed(float altitude) {
        return Math.pow((double) DEFAULT_ALTITUDE_KM / Math.max(1.0D, altitude), 1.5D);
    }
}
