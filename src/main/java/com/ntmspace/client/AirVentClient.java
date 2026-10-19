package com.ntmspace.client;

import com.hbm.client.particle.ParticleCreators;
import com.ntmspace.tileentity.TileEntityAirPump;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** The air vent's plume: hbm-space's {@code TileEntityAirPump.spawnParticles}, HBM's cooling-tower particle in the gas's colour. */
public final class AirVentClient {

    private AirVentClient() {}

    public static void init() {
        TileEntityAirPump.CLIENT_PARTICLES = (pump, color) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!(pump.getLevel() instanceof ClientLevel level) || mc.player == null) return;
            RandomSource rand = level.getRandom();
            BlockPos pos = pump.getBlockPos();
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 0.1F);
            data.putFloat("base", 0.3F);
            data.putFloat("max", 1F);
            data.putInt("life", 20 + rand.nextInt(20));
            data.putInt("color", color);
            ParticleCreators.TOWER.makeParticle(level, mc.player, rand,
                    pos.getX() + 0.5 + rand.nextDouble() - 0.5, pos.getY() + 1, pos.getZ() + 0.5 + rand.nextDouble() - 0.5, data);
        };
    }
}
