package com.ntmspace.entity.mob.ai;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code EntityAITankshell}, the tankbot's cannon. While a target is between 2 and 50 blocks away it counts
 * down {@code delay} ticks per attempt and, in "artillery mode" (target farther than {@code switchAttackDistance}),
 * counts down a reload of {@code reloadDelay} attempts and then fires: beyond 30 blocks one rocket (1.7's
 * {@code rocket_qd[1]}, the {@code [3]} variant below 30 health), closer an eight-shell spread of explosive 12 gauge.
 * Inside {@code switchAttackDistance} nothing fires (1.7 had no close-range branch). 1.7 mutex 0.
 */
public class EntityAITankshell extends Goal {

    /** The spent shell the cannon ejects: hbm-space registered it as {@code "TankGa"} on every shot; once here. */
    public static final String CASING = "TankGa";

    private final PathfinderMob owner;
    private LivingEntity target;
    private final int delay;
    private int timer;
    private final int attackDistance;
    private boolean artilleryMode;
    private int reloadTimer;
    private final int reloadDelay;

    public EntityAITankshell(PathfinderMob owner, int delay, int switchAttackDistance, int reloadDelay) {
        this(owner, true, true, delay, switchAttackDistance, reloadDelay);
    }

    public EntityAITankshell(PathfinderMob owner, boolean checkSight, boolean nearbyOnly, int delay,
                             int switchAttackDistance, int reloadDelay) {
        this.owner = owner;
        this.delay = delay;
        this.timer = delay;
        this.attackDistance = switchAttackDistance;
        this.artilleryMode = true;
        this.reloadTimer = reloadDelay;
        this.reloadDelay = reloadDelay;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    /** Registers {@link #CASING} with HBM's casing table; both sides call it once (the client draws it by name). */
    public static void registerCasing() {
        if (SpentCasing.fromName(CASING) != null) return;
        new SpentCasing(SpentCasing.CasingType.SHOTGUN).setColor(0xE5DD00, SpentCasing.COLOR_CASE_12GA).setScale(10F)
                .register(CASING).setupSmoke(0.02F, 0.5D, 60, 20).setMaxAge(60);
    }

    @Override
    public boolean canUse() {
        LivingEntity entity = this.owner.getTarget();

        if (entity == null) {
            return false;
        } else {
            this.target = entity;
            double dist = new Vec3(this.target.getX() - this.owner.getX(), this.target.getY() - this.owner.getY(),
                    this.target.getZ() - this.owner.getZ()).length();
            this.artilleryMode = dist > this.attackDistance;
            return dist > 2 && dist < 50;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !this.owner.getNavigation().isDone();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.timer--;
        if (this.timer <= 0) {
            if (this.artilleryMode) {
                fireArtilleryShell();
            }
            this.timer = this.delay;
        }
        this.owner.setYRot(this.owner.getYHeadRot());
    }

    /**
     * 1.7 sent the {@code "casing"} aux packet with the cannon's ejector (motion 0/0.6/-1, angle range 0.1) to players
     * within 50 blocks; HBM's casing emitter is the modern path (motion rotated by the head yaw, 50-block radius is the
     * emitter's own).
     */
    protected void spawnCasing() {
        CasingCreator.composeEffect(this.owner.level(), this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                this.owner.getYHeadRot(), 0F, -1D, 0.6D, 0D, 0.1D, 5F, 10F, SpentCasing.fromName(CASING));
    }

    private void fireArtilleryShell() {
        if (this.reloadTimer <= 0) {
            double xOffset = Math.cos(Math.toRadians(this.owner.getYHeadRot())) * 0;
            double zOffset = Math.sin(Math.toRadians(this.owner.getYHeadRot())) * 1.5;
            double yOffset = 10;

            double targetX = this.target.getX() - (this.owner.getX() + xOffset);
            double targetY = this.target.getY() + this.target.getEyeHeight() - (this.owner.getY() + yOffset);
            double targetZ = this.target.getZ() - (this.owner.getZ() + zOffset);

            double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
            targetX /= distance;
            targetY /= distance;
            targetZ /= distance;
            spawnCasing();

            if (distance > 30) {
                // 1.7's "standard gun" constructor with absolute coordinates as the muzzle offset; the position and
                // motion are overwritten straight after, so only the damage (4 x the config's multiplier) survives
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.owner,
                        XFactoryRocket.rocket_qd[this.owner.getHealth() < 30 ? 3 : 1], 4, 0.01F,
                        this.owner.getX(), this.owner.getY() + 3, this.owner.getZ());
                bullet.setPos(this.owner.getX(), this.owner.getY() + 3, this.owner.getZ());
                bullet.setDeltaMovement(targetX * 0.5D, targetY * 0.1D, targetZ * 0.5D);
                this.owner.level().addFreshEntity(bullet);
            } else {
                for (int i = 0; i < 8; i++) {
                    EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.owner, XFactory12ga.g12_explosive, 4, 0.01F,
                            this.owner.getX(), this.owner.getY() + 3, this.owner.getZ());
                    bullet.setPos(this.owner.getX(), this.owner.getY() + 3, this.owner.getZ());
                    // 1.7 Math.random(), not the entity's random
                    bullet.setDeltaMovement(targetX * (0.2 + (0.5 * Math.random())), targetY * 0.1,
                            targetZ * (0.2 + (0.5 * Math.random())));
                    this.owner.level().addFreshEntity(bullet);
                }
            }

            this.owner.level().playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                    ModSounds.TURRET_JEREMY_FIRE.get(), SoundSource.HOSTILE, 25.0F, 1.0F);
            this.reloadTimer = this.reloadDelay;
        } else {
            this.reloadTimer--;
        }

        if (this.reloadTimer == 20) {
            this.owner.level().playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                    ModSounds.TURRET_JEREMY_RELOAD.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
        }
    }
}
