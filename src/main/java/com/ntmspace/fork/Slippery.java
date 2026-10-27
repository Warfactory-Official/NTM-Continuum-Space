package com.ntmspace.fork;

import com.hbm.handler.ArmorUtil;
import com.ntmspace.NtmSpace;
import com.hbm.api.environment.Environment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Slippery horizontal motion runs once before movement: on the server for mobs and on the local client for players.
 * Idle damping is symmetric, and a stationary entity keeps its zero horizontal velocity.
 * Nitan supplies the existing hazmat exemption through HBM's armor hook.
 */
public final class Slippery {

    private Slippery() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        Environment.LIVING_TICK_START.register(Slippery::tick);
        ArmorUtil.registerHazmatEffect(NtmSpace.MOD_ID + ":nitan", ForkContent.NITAN_EFFECT);
    }

    public static boolean slippery(LivingEntity entity) {
        return dev.golem.core.world.Effects.hasRegistered(entity, ForkContent.SLIPPERY_EFFECT.get());
    }

    public static void tick(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!entity.level().isClientSide() || !player.isLocalPlayer() || player.getAbilities().flying) return;
        } else if (entity.level().isClientSide()) {
            return;
        }
        if (entity.onGround() && slippery(entity)) slide(entity);
    }

    /** One application of hbm-space's slide to an entity on the ground. */
    public static void slide(LivingEntity entity) {
        double slipperiness = 0.6;
        double inertia = 0.1;
        boolean isMoving = entity.zza != 0.0F || entity.xxa != 0.0F;
        Vec3 m = entity.getDeltaMovement();
        double mx = m.x, mz = m.z;
        if (mx == 0D && mz == 0D) return;

        double angle = Math.atan2(mz, mx);
        double targetX = Math.cos(angle) * slipperiness;
        double targetZ = Math.sin(angle) * slipperiness;
        mx += (targetX - mx) * inertia;
        mz += (targetZ - mz) * inertia;

        if (!isMoving) {
            mx *= (1.0 - 0.1);
            mz *= (1.0 - 0.1);
            double totalVelocity = Math.sqrt(mx * mx + mz * mz);
            double smoothingAmount = totalVelocity * 0.02;
            if (totalVelocity > 0) {
                mx -= mx / totalVelocity * smoothingAmount;
                mz -= mz / totalVelocity * smoothingAmount;
            }
        }
        entity.setDeltaMovement(mx, m.y, mz);
    }
}
