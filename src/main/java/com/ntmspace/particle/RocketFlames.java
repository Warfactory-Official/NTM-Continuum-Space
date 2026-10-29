package com.ntmspace.particle;

import com.hbm.particle.RocketFlameColor;

/**
 * The colors and packed brightness of ParticleRocketBF, Fusion, MUD, SCH and UP in Space 1.7.
 */
public final class RocketFlames {
    public static final RocketFlameColor BALEFIRE = new RocketFlameColor(0F, 1F, 0F, false, false, false, 240);
    public static final RocketFlameColor FUSION = new RocketFlameColor(1F, 1F, 1F, false, false, true, 240);
    public static final RocketFlameColor MUD = new RocketFlameColor(1F, 1F, 0F, false, false, false, 20);
    public static final RocketFlameColor SCHRABIDIUM = new RocketFlameColor(1F, 1F, 1F, true, false, false, 240);
    public static final RocketFlameColor URANIUM_PLUTONIUM = new RocketFlameColor(0F, 1F, 0F, false, false, false, 80);

    private RocketFlames() {
    }

    public static RocketFlameColor forEffect(String effect) {
        return switch (effect) {
            case "missileContrail" -> RocketFlameColor.FLAME;
            case "missileContrailbf" -> BALEFIRE;
            case "missileContrailf" -> FUSION;
            case "missileContrailMUD" -> MUD;
            case "missileContrailSCH" -> SCHRABIDIUM;
            case "missileContrailUP" -> URANIUM_PLUTONIUM;
            default -> throw new IllegalArgumentException("Unknown rocket flame effect " + effect);
        };
    }
}
