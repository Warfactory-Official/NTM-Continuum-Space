package com.ntmspace.rocket;

import com.ntmspace.orbit.Orbit;
import com.ntmspace.world.SolarSystem;
import java.util.Locale;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code SolarSystem.Body}: the destinations a navigation drive can hold, in 1.7 order (the drive's 1.7
 * metadata; here one drive item per constant, {@code ntmspace:hard_drive_full_<name>}). Thatmo sat this one out in 1.7.
 */
public enum DriveBody {
    ORBIT(""),
    KERBIN("kerbin"),
    MUN("mun"),
    MINMUS("minmus"),
    DUNA("duna"),
    MOHO("moho"),
    DRES("dres"),
    EVE("eve"),
    IKE("ike"),
    LAYTHE("laythe"),
    TEKTO("tekto");

    /** The body's name ({@code ""} for the orbit). */
    public final String name;

    DriveBody(String name) {
        this.name = name;
    }

    /** {@code orbit}, {@code kerbin}, ...: item id suffix and overlay sprite name. */
    public String key() {
        return this == ORBIT ? "orbit" : name;
    }

    /** 1.7 {@code getBody()}: null for the orbit. */
    public SolarSystem.@Nullable Body getBody() {
        return this == ORBIT ? null : SolarSystem.body(name);
    }

    /** 1.7 {@code getDimensionId()}: the orbit level, or the body's surface. */
    public ResourceKey<Level> level() {
        return this == ORBIT ? Orbit.ORBIT : Orbit.levelOf(getBody());
    }

    /** 1.7 {@code getProcessingLevel(from)}: 0 for the orbit. */
    public int getProcessingLevel(SolarSystem.Body from) {
        if (this == ORBIT) return 0;
        return processingLevel(getBody(), from);
    }

    /**
     * 1.7 {@code CelestialBody.getProcessingLevel}: 0 to or from a moon of the same planet and for the body itself,
     * else 1, raised to the body's minimum (Eve and Dres 2, Laythe and Tekto 3; 1.7 {@code withMinProcessingLevel}).
     */
    public static int processingLevel(SolarSystem.Body to, SolarSystem.Body from) {
        int level;
        if (to == from) {
            level = 0;
        } else if (to == from.parent || to.parent == from) {
            level = 0;
        } else {
            level = 1;
        }
        return Math.max(level, minProcessingLevel(to));
    }

    private static int minProcessingLevel(SolarSystem.Body body) {
        return switch (body.name) {
            case "eve", "dres" -> 2;
            case "laythe", "tekto", "nissee" -> 3;
            default -> 0;
        };
    }

    public static @Nullable DriveBody byName(String name) {
        for (DriveBody body : values()) {
            if (body.key().equals(name.toLowerCase(Locale.ROOT)) || body.name.equals(name)) return body;
        }
        return null;
    }

    /** The drive body whose surface {@code body} is, or null (Thatmo, the gas giants...). */
    public static @Nullable DriveBody of(SolarSystem.@Nullable Body body) {
        if (body == null) return null;
        for (DriveBody drive : values()) {
            if (drive != ORBIT && drive.name.equals(body.name)) return drive;
        }
        return null;
    }
}
