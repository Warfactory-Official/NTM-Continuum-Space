package com.ntmspace.breathing;

import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * A player's own oxygen count for the HUD's bubbles (1.7 synced all of {@code HbmLivingProps} to the player each tick;
 * here only the oxygen, when it changes).
 */
public final class OxygenSync {

    private OxygenSync() {}

    public record Payload(int oxygen) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.of(Payload.class, Ids.of(NtmSpace.MOD_ID, "oxygen"),
            (payload, buf) -> buf.writeVarInt(payload.oxygen()), buf -> new Payload(buf.readVarInt()));

    /** The local player's oxygen as last synced (client). */
    public static volatile int clientOxygen = 100;

    public static void init() {
        GolemNetwork.declarePlayToClient(CODEC);
    }

    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> clientOxygen = payload.oxygen());
    }

    static void send(ServerPlayer player, int oxygen) {
        if (GolemNetwork.canSendToPlayer(player, CODEC)) GolemNetwork.sendToPlayer(player, CODEC, new Payload(oxygen));
    }
}
