package com.ntmspace.world;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** The dimensions reached by meteors and Bobmazon in the authored Space rules. */
public final class BodyReach {
    private BodyReach() {}

    /** The same surface classification supplied to HBM's live meteor-spawning query. */
    public static boolean meteorsStrike(ResourceKey<Level> dimension) {
        return CelestialBodies.of(dimension) != null;
    }

    /** 1.7 ItemBobmazonPacket.Handler: delivery drones reach dimension zero alone. */
    public static @Nullable Component bobmazonRefusal(ResourceKey<Level> dimension) {
        return dimension.equals(Level.OVERWORLD) ? null : Component.translatable("chat.hbmspace.bobmazon.outOfRange");
    }
}
