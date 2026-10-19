package com.ntmspace.entity.rocket;

import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * The rocket's client-side puffs: 1.7 {@code ParticleUtil.spawnGasFlame} on the client (the gas-fire particle at its
 * default size) and {@code ExplosionLarge.spawnShock} (the shock smoke ring), both through HBM's effect handlers.
 */
final class RocketEffects {

    private RocketEffects() {}

    static void gasFlame(Level level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        CompoundTag data = new CompoundTag();
        data.putDouble("moX", motionX);
        data.putDouble("moY", motionY);
        data.putDouble("moZ", motionZ);
        HbmEffectNT.GasFlame.summonParticle(level, x, y, z, data);
    }

    static void shock(Level level, double x, double y, double z, int count, double strength) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        data.putDouble("strength", strength);
        HbmEffectNT.Smoke_Shock.summonParticle(level, x, y + 0.5, z, data);
    }
}
