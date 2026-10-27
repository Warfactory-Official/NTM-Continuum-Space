package com.ntmspace.orbit;

import com.hbm.handler.DroppedItemHooks;
import com.ntmspace.config.SpaceConfig;
import java.util.List;
import com.hbm.api.environment.Environment;
import com.hbm.packet.PlayerInformPayload;
import dev.golem.core.hook.Hook;
import dev.golem.core.hook.Verdict;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * What the orbit level is to HBM and to vanilla, from hbm-space's {@code WorldProviderOrbit}, {@code CelestialBody} and
 * {@code ModEventHandler}:
 *
 * <ul>
 *   <li>{@link Environment#ORBIT}: true there (1.7 {@code CelestialBody.inOrbit}).</li>
 *   <li>Pressure and breathing are not answered here: {@code atmosphere.AtmosphereHooks} gives orbit no body air and
 *       sealed station rooms their own.</li>
 *   <li>{@link Environment#SUNLIGHT}: the station's sun ({@code WorldProviderOrbit.getSunPower}).</li>
 *   <li>Gravity ({@code CelestialBody.getGravity}): an entity "with gravity" (1.7 {@code HbmLivingProps.hasGravity}:
 *       inside a sealed atmosphere, {@link #HAS_GRAVITY}) has 1.6 times the station's switch, under 0.2 is none; every
 *       other entity floats. Water mobs keep standard gravity, flying mobs are skipped, as 1.7.</li>
 *   <li>Living free fall uses the body's transient attributes and vanilla travel hooks in
 *       {@link com.ntmspace.world.SpaceEnvironment}; it is applied once on both logical sides.</li>
 *   <li>Dropped items hover: each actual item tick before vanilla physics, x/z motion * 0.9 and y motion 0.04 (vanilla's item gravity then
 *       makes it exactly 0).</li>
 *   <li>The orbit fall: a player who is not riding a craft and comes within {@link OrbitalStation#WARNING_SIZE} of the
 *       buffer is warned ({@code info.orbitfall}); inside the {@link OrbitalStation#BUFFER_SIZE} edge of its square it
 *       falls to the orbited body, at a random x/z within {@link Orbit#MAX_PROBE_DISTANCE} and y 800.</li>
 * </ul>
 */
public final class OrbitEnvironment {

    /** 1.7 {@code AstronomyUtil.STANDARD_GRAVITY}. */
    public static final float STANDARD_GRAVITY = 1.6F;
    /** 1.7 {@code ServerProxy.ID_GAS_HAZARD}, the info slot the orbit-fall warning uses. */
    private static final int ID_GAS_HAZARD = 12;

    /** Whether a living entity in orbit has gravity (1.7: it stands in a sealed atmosphere). */
    @FunctionalInterface
    public interface SealedGravity {
        Verdict hasGravity(LivingEntity entity);
    }

    /** {@link SealedGravity} answers, in order; the first non-PASS decides, nothing deciding means free fall. */
    public static final Hook<SealedGravity> HAS_GRAVITY = Hook.of(listeners -> entity ->
            Verdict.first(listeners, listener -> listener.hasGravity(entity)));

    private static final RandomSource RAND = RandomSource.create();

    private OrbitEnvironment() {}

    static void init() {
        Environment.ORBIT.register(level -> Orbit.inOrbit(level) ? Verdict.ALLOW : Verdict.PASS);
        // pressure and breathing in orbit come from atmosphere.AtmosphereHooks: no body air there, but a sealed station
        // room has its own (1.7 ChunkAtmosphereManager); a listener here would answer first and deny every room
        Environment.SUNLIGHT.register((level, pos) -> {
            if (!Orbit.inOrbit(level) || level.getServer() == null) return Double.NaN;
            OrbitalStation station = OrbitalStation.getStationFromPosition(level.getServer(), pos.getX(), pos.getZ());
            return station.getSunPower(level.getGameTime());
        });
        Environment.GRAVITY.register(entity -> Orbit.inOrbit(entity.level()) && entity instanceof LivingEntity living
                ? gravity(living) / STANDARD_GRAVITY : Double.NaN);
        Lifecycle.onServerTick(OrbitEnvironment::serverTick);
        DroppedItemHooks.onBeforeTick(OrbitEnvironment::tickItem);
    }

    /**
     * 1.7 {@code CelestialBody.getGravity(entity)} in orbit, in 1.7 units (1.6 is Earth's): water mobs standard, an entity
     * with gravity 1.6 times its station's switch (0 below 0.2), any other 0.
     */
    public static float gravity(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.animal.fish.WaterAnimal) return STANDARD_GRAVITY;
        if (!HAS_GRAVITY.invoker().hasGravity(entity).orElse(false)) return 0;
        OrbitalStation station = entity.level().isClientSide() || entity.level().getServer() == null
                ? OrbitalStation.clientStation
                : OrbitalStation.getStationFromPosition(entity.level().getServer(), (int) entity.getX(), (int) entity.getZ());
        float gravity = STANDARD_GRAVITY * station.gravityMultiplier;
        if (gravity < 0.2) return 0;
        return gravity;
    }

    /** Called from ItemEntity.tick so vanilla owns frozen-entity and ticking-chunk eligibility. */
    public static void tickItem(ItemEntity item) {
        if (item.level().isClientSide() || !Orbit.inOrbit(item.level())) return;
        var motion = item.getDeltaMovement();
        item.setDeltaMovement(motion.x * 0.9D, 0.03999999910593033D, motion.z * 0.9D);
    }

    private static void serverTick(MinecraftServer server) {
        ServerLevel orbit = server.getLevel(Orbit.ORBIT);
        if (orbit == null) return;

        for (ServerPlayer player : List.copyOf(orbit.players())) orbitFall(player);
    }

    /** 1.7: "Check for players attempting to cross over to another orbital grid". */
    private static void orbitFall(ServerPlayer player) {
        if (player.getVehicle() instanceof StationDock) return;

        double rx = Math.abs(player.getX()) % OrbitalStation.STATION_SIZE;
        double rz = Math.abs(player.getZ()) % OrbitalStation.STATION_SIZE;

        int minBuffer = OrbitalStation.BUFFER_SIZE;
        int maxBuffer = OrbitalStation.STATION_SIZE - minBuffer;

        int minWarning = OrbitalStation.BUFFER_SIZE + OrbitalStation.WARNING_SIZE;
        int maxWarning = OrbitalStation.STATION_SIZE - minWarning;

        if (rx < minWarning || rx > maxWarning || rz < minWarning || rz > maxWarning) {
            GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(ID_GAS_HAZARD, 3000,
                    ChatFormatting.RED, new PlayerInformPayload.Translated("info.orbitfall")));
        }

        if (rx < minBuffer || rx > maxBuffer || rz < minBuffer || rz > maxBuffer) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(player.level().getServer(), (int) player.getX(), (int) player.getZ());
            var range = SpaceConfig.maxProbeDistance;
            SpaceTeleporter.teleport(player, Orbit.levelOf(station.orbiting),
                    RAND.nextInt(range * 2) - range, 800,
                    RAND.nextInt(range * 2) - range, false);
        }
    }
}
