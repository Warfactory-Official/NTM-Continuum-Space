package com.ntmspace.client;

import com.hbm.client.particle.ParticleCreators;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.breathing.ItemModOxy;
import com.ntmspace.client.render.atmosphere.AtmosphereMachineRenderers;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import dev.golem.core.client.ClientRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * The atmosphere machines' client half: their renderers, the emitter's gas plume, and the PLSS breathing loop
 * (hbm-space {@code ItemModOxy.modUpdate}: {@code player.plss_breathing}, volume 0.1, range 5, kept alive 10 ticks, on the
 * local player only and never in creative).
 */
public final class AtmosphereMachinesClient {

    private static @Nullable AudioWrapper breathing;

    public static @Nullable AudioWrapper breathing() {
        return breathing;
    }

    private AtmosphereMachinesClient() {}

    public static void init() {
        ClientRegistrations.renderers(AtmosphereMachineRenderers::register);
        TileEntityAtmosphericEmitter.CLIENT_PARTICLES = AtmosphereMachinesClient::emitterPlume;
        ItemModOxy.CLIENT_BREATHING = (entity, inUse) -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (entity != player) return; // only play breathing audio for self
            if (inUse && !player.getAbilities().instabuild) {
                if (breathing == null || !breathing.isPlaying()) {
                    breathing = AudioSystem.getLoopedSound(AtmosphereMachines.PLSS_BREATHING.get(), SoundSource.PLAYERS,
                            (float) player.getX(), (float) player.getY(), (float) player.getZ(), 0.1F, 5.0F, 1.0F, 10);
                    if (breathing != null) {
                        breathing.attachTo(player);
                        breathing.startSound();
                    }
                }
                if (breathing != null) breathing.keepAlive();
            } else if (breathing != null) {
                breathing.stopSound();
                breathing = null;
            }
        };
    }

    /** 1.7 {@code TileEntityAtmosphericEmitter} client branch: the "tower" particle in the gas's colour, within 100 blocks. */
    private static void emitterPlume(TileEntityAtmosphericEmitter emitter) {
        Minecraft mc = Minecraft.getInstance();
        if (!(emitter.getLevel() instanceof ClientLevel level) || mc.player == null) return;
        BlockPos pos = emitter.getBlockPos();
        if (mc.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >= 100 * 100) return;
        ForgeDirection dir = MachineCore.facing(emitter.getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        RandomSource rand = level.getRandom();
        CompoundTag data = new CompoundTag();
        data.putFloat("lift", 0.5F);
        data.putFloat("base", 0.375F);
        data.putFloat("max", 3F);
        data.putInt("life", 100 + rand.nextInt(50));
        data.putString("type", "tower");
        data.putInt("color", emitter.tank.getTankType().clientMetadata().getColor());
        data.putDouble("mY", 0.2);
        ParticleCreators.TOWER.makeParticle(level, mc.player, rand,
                pos.getX() + 0.5 - dir.offsetX * 0.5 + rot.offsetX * 0.5, pos.getY() + 2.5,
                pos.getZ() + 0.5 - dir.offsetZ * 0.5 + rot.offsetZ * 0.5, data);
    }
}
