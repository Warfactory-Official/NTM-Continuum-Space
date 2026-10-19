package com.ntmspace.entity.rocket;

import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.OrbitalStation;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.7 {@code EntityRideableRocket.serialize}/{@code deserialize} ("only used for station syncing"): during a transfer
 * the rocket sends its pilot the travelling station, which becomes the client's {@link OrbitalStation#clientStation}
 * (sky, progress, the transfer overlay).
 */
public final class RocketStationSync {

    private RocketStationSync() {}

    /** The station, written by {@link OrbitalStation#serialize}; decoded into a fresh station on the client. */
    public record Payload(OrbitalStation station) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.ofRegistry(Payload.class, Ids.of(NtmSpace.MOD_ID, "rocket_station"),
            (payload, buf) -> payload.station().serialize(buf), buf -> new Payload(OrbitalStation.deserialize(buf)));

    public static void init() {
        GolemNetwork.declarePlayToClient(CODEC);
    }

    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> OrbitalStation.clientStation = payload.station());
    }

    static void send(ServerPlayer player, OrbitalStation station) {
        if (GolemNetwork.canSendToPlayer(player, CODEC)) GolemNetwork.sendToPlayer(player, CODEC, new Payload(station));
    }
}
