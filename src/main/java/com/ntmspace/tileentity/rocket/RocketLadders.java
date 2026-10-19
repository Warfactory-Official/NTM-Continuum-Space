package com.ntmspace.tileentity.rocket;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code HbmPlayerProps.isOnLadder}: a player inside a machine's ladder box climbs as on a vanilla ladder
 * ({@code EntityLivingBase.moveEntityWithHeading}): falling slowed to 0.15, no fall damage, sneaking holds on, and moving
 * into a wall climbs at 0.2.
 */
public final class RocketLadders {

    private RocketLadders() {}

    public static void climb(Player player) {
        Vec3 motion = player.getDeltaMovement();
        double y = Math.max(motion.y, -0.15D);
        if (player.isShiftKeyDown() && y < 0) y = 0;
        if (player.horizontalCollision) y = 0.2D;
        player.setDeltaMovement(motion.x, y, motion.z);
        player.resetFallDistance();
    }
}
