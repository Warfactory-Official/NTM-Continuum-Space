package com.ntmspace.fork;

import com.hbm.api.environment.Environment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code slippery} ("Butterfingers") slide and the {@code nitan} hazmat rule:
 * <ul>
 *   <li>{@code ModEventHandler.onLivingUpdate} (server, every living thing on the ground) and {@code onPlayerTick} (a
 *       player not flying, both tick phases, both sides - the client's copy is what moves a player): the ground motion
 *       is pulled 10% towards 0.6 blocks a tick in its own direction, and without movement input slowed by 10% and
 *       another 2% of its speed ("god weeps"). The server half runs on HBM's {@code Environment.LIVING_TICK}, the
 *       client's on Golem's client tick ({@code ForkClient}).</li>
 *   <li>{@code ArmorUtil.checkForHazmat}: an entity under {@code nitan} counts as wearing hazmat (HBM hook
 *       {@code ArmorUtil.HAZMAT}, ADDON-API-INVENTORY H-71).</li>
 * </ul>
 */
public final class Slippery {

    private Slippery() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        Environment.LIVING_TICK.register(entity -> {
            if (entity.level().isClientSide() || !slippery(entity)) return;
            if (entity.onGround()) slide(entity);
        });
        com.hbm.handler.ArmorUtil.HAZMAT.register(entity -> dev.golem.core.world.Effects.hasRegistered(entity, ForkContent.NITAN_EFFECT.get()));
    }

    public static boolean slippery(LivingEntity entity) {
        return dev.golem.core.world.Effects.hasRegistered(entity, ForkContent.SLIPPERY_EFFECT.get());
    }

    /** hbm-space {@code onPlayerTick}'s half: a slippery player that is not flying slides when on the ground. */
    public static void playerTick(Player player) {
        if (slippery(player) && !player.getAbilities().flying && player.onGround()) slide(player);
    }

    /** One application of hbm-space's slide to an entity on the ground. */
    public static void slide(LivingEntity entity) {
        double slipperiness = 0.6;
        double inertia = 0.1;
        boolean isMoving = entity.zza != 0.0F || entity.xxa != 0.0F;
        Vec3 m = entity.getDeltaMovement();
        double mx = m.x, mz = m.z;

        double angle = Math.atan2(mz, mx);
        double targetX = Math.cos(angle) * slipperiness;
        double targetZ = Math.sin(angle) * slipperiness;
        mx += (targetX - mx) * inertia;
        mz += (targetZ - mz) * inertia;

        if (!isMoving) {
            mx *= (1.0 - 0.1);
            double totalVelocity = Math.sqrt(mx * mx + mz * mz);
            double smoothingAmount = totalVelocity * 0.02;
            // never zero here: at rest atan2(0, 0) = 0 already pulled the motion 0.06 towards +X (1.7: a standing
            // slippery entity drifts east)
            if (totalVelocity > 0) {
                mx -= mx / totalVelocity * smoothingAmount;
                mz -= mz / totalVelocity * smoothingAmount;
            }
        }
        entity.setDeltaMovement(mx, m.y, mz);
    }
}
