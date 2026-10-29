package com.ntmspace.world;

import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.orbit.Orbit;
import dev.golem.core.world.Respawns;
import dev.golem.core.world.Respawns.Destination;
import dev.golem.core.world.WorldAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Space 1.7 providers select the death world's breathable bed without discarding a temporarily unusable point. */
public final class SpaceRespawns {
    private SpaceRespawns() {
    }

    public static void init() {
        WorldAttributes.BED_SPAWN.register((level, pos, allowed) ->
                level instanceof ServerLevel server && celestial(level) ? canUseBed(server, pos) : allowed);
        Respawns.RULES.register(SpaceRespawns::destination);
        Respawns.FALLBACKS.register(player -> player.level().dimension().equals(SpaceDimensions.DUNA)
                && MartianWorld.is(player.level().getServer()) ? new Respawns.WorldSpawn(
                (ServerLevel) player.level()) : null);
    }

    private static boolean celestial(Level level) {
        return !level.dimension().equals(Level.OVERWORLD)
                && (level.dimension().equals(Orbit.ORBIT) || CelestialBodies.of(level) != null);
    }

    private static boolean canUseBed(ServerLevel level, BlockPos pos) {
        return level.dimension().equals(SpaceDimensions.DUNA) && MartianWorld.is(level.getServer())
                || AtmosphereHandler.canBreathe(AtmosphereHandler.getAtmosphere(level, pos));
    }

    private static @Nullable Destination destination(ServerPlayer player) {
        var level = (ServerLevel) player.level();
        var point = Respawns.stored(player);
        if (level.dimension().equals(SpaceDimensions.DUNA) && MartianWorld.is(level.getServer())) {
            if (point != null && point.dimension().equals(SpaceDimensions.DUNA)) return null;
            return new Respawns.WorldSpawn(level);
        }
        if (point == null) return null;
        var target = level.getServer().getLevel(point.dimension());
        if (target == null || !celestial(target)) return null;
        if (level == target && canUseBed(target, point.pos())) return null;
        return Respawns.DEFAULT;
    }
}
