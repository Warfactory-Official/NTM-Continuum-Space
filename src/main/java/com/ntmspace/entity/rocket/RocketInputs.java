package com.ntmspace.entity.rocket;

import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * The pilot's jump key (1.7 {@code rider.isJumping}, which the server read from the vehicle input packet). The input
 * packet's jump flag is not readable on the server on every target, so the client tells the server it is jumping
 * while it rides a rocket, every tick the key is held ({@code JUMP TO LAUNCH}).
 */
public final class RocketInputs {

    private RocketInputs() {}

    public record Jump() implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Jump> CODEC = GolemCodec.of(Jump.class, Ids.of(NtmSpace.MOD_ID, "rocket_jump"),
            (payload, buf) -> {}, buf -> new Jump());

    public static void init() {
        GolemNetwork.registerPlayToServer(CODEC, (payload, player) -> {
            if (player.getVehicle() instanceof EntityRideableRocket rocket) {
                rocket.pilotJumpTime = player.level().getGameTime();
            }
        });
    }

    /** Client: sent each tick the jump key is held while riding a rocket. */
    public static void sendJump() {
        GolemNetwork.sendToServer(CODEC, new Jump());
    }
}
