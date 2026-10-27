package com.ntmspace.trait;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server authority for celestial time, published to a separate client rendering mirror. */
public final class CelestialTimeSync {

    private static final int CORRECTION_INTERVAL = 20;
    private static long lastMultiplier = 1;
    private static boolean installed;

    private CelestialTimeSync() {}

    public record Payload(long multiplier) implements GolemPayload {
        public Payload {
            if (multiplier < 1) throw new IllegalArgumentException("Invalid celestial time multiplier: " + multiplier);
        }
        @Override public Identifier id() { return CODEC.id(); }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.of(Payload.class,
            Ids.of(NtmSpace.MOD_ID, "celestial_time"),
            (payload, buffer) -> buffer.writeVarLong(payload.multiplier()),
            buffer -> new Payload(buffer.readVarLong()));

    public static synchronized void init() {
        if (installed) return;
        installed = true;
        GolemNetwork.declarePlayToClient(CODEC);
        Lifecycle.onServerStarted(server -> resetServer());
        Lifecycle.onServerStopping(server -> resetServer());
        Lifecycle.onPlayerJoin((player, firstTime) -> send(player));
        Lifecycle.onServerTick(CelestialTimeSync::tick);
    }

    /** The server's lifecycle does not alter an integrated client's independently received state. */
    private static void resetServer() {
        SolarSystem.TIME_MULTIPLIER = 1;
        lastMultiplier = 1;
    }

    private static void tick(MinecraftServer server) {
        var multiplier = SolarSystem.TIME_MULTIPLIER;
        if (multiplier == lastMultiplier && server.getTickCount() % CORRECTION_INTERVAL != 0) return;
        lastMultiplier = multiplier;
        var payload = new Payload(multiplier);
        for (var player : server.getPlayerList().getPlayers()) {
            if (GolemNetwork.canSendToPlayer(player, CODEC)) GolemNetwork.sendToPlayer(player, CODEC, payload);
        }
    }

    private static void send(ServerPlayer player) {
        if (GolemNetwork.canSendToPlayer(player, CODEC)) {
            GolemNetwork.sendToPlayer(player, CODEC, new Payload(SolarSystem.TIME_MULTIPLIER));
        }
    }

    /** The same client mirror update used by the native packet handler; it never writes server authority. */
    public static boolean applyClient(long multiplier) {
        if (multiplier < 1) throw new IllegalArgumentException("Invalid celestial time multiplier: " + multiplier);
        if (SolarSystem.CLIENT_TIME_MULTIPLIER == multiplier) return false;
        SolarSystem.CLIENT_TIME_MULTIPLIER = multiplier;
        return true;
    }

}
