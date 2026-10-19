package com.ntmspace.probe;

import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The probe's own play payload beside NTM: Space's: declared in {@code declare()}, flushed once by Golem Core for every
 * mod. {@code /ntmprobe ping <n>} sends it; the client keeps the last value for the harness query.
 */
public final class ProbeNet {

    private ProbeNet() {}

    public record Ping(int value) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Ping> CODEC = GolemCodec.of(Ping.class, Ids.of(ProbeEntry.MOD_ID, "ping"),
            (payload, buf) -> buf.writeVarInt(payload.value()), buf -> new Ping(buf.readVarInt()));

    /** The last ping the client received (client). */
    public static volatile int lastPing = -1;

    public static void init() {
        GolemNetwork.declarePlayToClient(CODEC);
    }

    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> lastPing = payload.value());
    }

    public static boolean send(ServerPlayer player, int value) {
        if (!GolemNetwork.canSendToPlayer(player, CODEC)) return false;
        GolemNetwork.sendToPlayer(player, CODEC, new Ping(value));
        return true;
    }
}
