package com.ntmspace.handler;

import com.hbm.handler.SpecialPlayers;
import com.hbm.particle.helper.HbmEffectNT;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import java.util.Random;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Reentry gas-flame puffs at both ends of a player tick, also delivered to nearby observers. */
public final class ReentryFlames {

    private static final Random RANDOM = new Random();
    private static boolean started;

    private ReentryFlames() {}

    public static synchronized void init() {
        if (started) return;
        started = true;
        SpecialPlayers.TICK_START.register(ReentryFlames::playerTick);
        SpecialPlayers.TICK_END.register(ReentryFlames::playerTick);
    }

    public static void playerTick(Player player) {
        double y = player.getY();
        if (y <= 300D || y >= 1000D) return;
        double x = player.getX() - 1D + 3D * RANDOM.nextDouble();
        var level = player.level();
        var atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        if (!eligible(y, atmosphere == null ? 0D : atmosphere.getPressure(),
                player.isPassenger(), player.getDeltaMovement())) return;

        double z = player.getZ();
        if (level instanceof ServerLevel) {
            HbmEffectNT.GasFlame.send(level, x, y, z, null, 150D);
        } else {
            HbmEffectNT.GasFlame.summonParticle(level, x, y, z, null);
        }
    }

    private static boolean eligible(double y, double pressure, boolean riding, Vec3 motion) {
        return y > 300D && y < 1000D && pressure > 0.05D && !riding
                && (Math.abs(motion.x) > 1D || Math.abs(motion.y) > 1D || Math.abs(motion.z) > 1D);
    }
}
