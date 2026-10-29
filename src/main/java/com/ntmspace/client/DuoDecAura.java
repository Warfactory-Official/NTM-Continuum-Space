package com.ntmspace.client;

import com.hbm.handler.SpecialPlayers;
import com.ntmspace.fork.ForkCapes;
import com.ntmspace.particle.ParticleDuoDec;
import com.ntmspace.registry.SpaceParticles;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.Particles;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space ModEventHandler.onPlayerTick START and ParticleUtil's two independent note rolls.
 */
public final class DuoDecAura {
    private static final UUID DUODEC = UUID.fromString(ForkCapes.DUODEC_);

    private DuoDecAura() {
    }

    public static void init() {
        ClientRegistrations.particles(providers -> {
            providers.register(SpaceParticles.DUOEWE.get(), new ParticleDuoDec.Provider(true));
            providers.register(SpaceParticles.DUODEC.get(), new ParticleDuoDec.Provider(false));
        });
        SpecialPlayers.TICK_START.register(DuoDecAura::playerStart);
    }

    public static void playerStart(Player player) {
        if (!player.level().isClientSide() || !player.getUUID().equals(DUODEC)
                || player.isInvisible() || player.isShiftKeyDown()) return;
        var random = player.getRandom();
        var point = new Vec3(3 * random.nextDouble(), 0, 0)
                .zRot((float) (random.nextDouble() * Math.PI))
                .yRot((float) (random.nextDouble() * Math.PI * 2));
        var x = player.getX() + point.x;
        var y = player.getY() + 1 + point.y;
        var z = player.getZ() + point.z;
        if (random.nextInt(12) == 0) emit((ClientLevel) player.level(), x, y, z, true);
        if (random.nextInt(12) == 0) emit((ClientLevel) player.level(), x, y, z, false);
    }

    private static void emit(ClientLevel level, double x, double y, double z, boolean tune) {
        var quality = Particles.quality();
        if (quality == Particles.Quality.MINIMAL
                || quality == Particles.Quality.DECREASED && !level.getRandom().nextBoolean()) return;
        Minecraft.getInstance().particleEngine.createParticle(
                tune ? SpaceParticles.DUOEWE.get() : SpaceParticles.DUODEC.get(), x, y, z, 0, 0, 0);
    }
}
