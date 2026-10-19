package com.ntmspace.entity.mob.ai;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code EntityAIBehemothGun}, the war behemoth's weapons. While a target is between 2 and 50 blocks away
 * it fires every {@code delay} ticks: inside {@code switchAttackDistance} a 7.62 mm armour-piercing round from ten
 * blocks up, beyond it an artillery shell (HBM's shell type 9) after a reload of {@code reloadDelay} attempts. 1.7
 * mutex 0.
 */
public class EntityAIBehemothGun extends Goal {

    private final PathfinderMob owner;
    private LivingEntity target;
    private final int delay;
    private int timer;
    private final int switchAttackDistance;
    private boolean artilleryMode;
    private int reloadTimer;
    private final int reloadDelay;

    public EntityAIBehemothGun(PathfinderMob owner, boolean checkSight, boolean nearbyOnly, int delay,
                               int switchAttackDistance, int reloadDelay) {
        this.owner = owner;
        this.delay = delay;
        this.timer = delay;
        this.switchAttackDistance = switchAttackDistance;
        this.artilleryMode = false;
        this.reloadTimer = reloadDelay;
        this.reloadDelay = reloadDelay;
        setFlags(EnumSet.noneOf(Flag.class));
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
            this.artilleryMode = dist > this.switchAttackDistance;
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
            } else {
                fireGatlingBarrage();
            }
            this.timer = this.delay;
        }
        this.owner.setYRot(this.owner.getYHeadRot());
    }

    private void fireGatlingBarrage() {
        double xOffset = Math.cos(Math.toRadians(this.owner.getYRot())) * 1.5;
        double zOffset = Math.sin(Math.toRadians(this.owner.getYRot())) * 1.5;
        double yOffset = 10;

        double targetX = this.target.getX() - (this.owner.getX() + xOffset);
        double targetY = this.target.getY() + this.target.getEyeHeight() - (this.owner.getY() + yOffset);
        double targetZ = this.target.getZ() - (this.owner.getZ() + zOffset);

        double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        targetX /= distance;
        targetY /= distance;
        targetZ /= distance;

        // 1.7 setKnockback(0) mutates HBM's shared r762_ap config (so every 7.62 AP round loses its knockback once a
        // behemoth has fired); hbm-space's side effect, ported as found. The muzzle "offset" arguments are absolute
        // coordinates in 1.7 and are overwritten by the setPosition straight after.
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.owner, XFactory762mm.r762_ap.setKnockback(0), 0.5F, 0.01F,
                this.owner.getX() + xOffset, this.owner.getY() + yOffset, this.owner.getZ() + zOffset);
        bullet.setPos(this.owner.getX() + xOffset, this.owner.getY() + yOffset, this.owner.getZ() + zOffset);
        bullet.setDeltaMovement(targetX * 0.5D, targetY * 0.5D, targetZ * 0.5D);

        this.owner.level().addFreshEntity(bullet);
        this.owner.playSound(ModSounds.GUN_MINIGUN_FIRE.get(), 15.0F, 1.0F);
    }

    private void fireArtilleryShell() {
        if (this.reloadTimer <= 0) {
            EntityArtilleryShell grenade = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), this.owner.level());
            grenade.setType(9);
            grenade.setPos(this.owner.getX(), this.owner.getY() + 14, this.owner.getZ());
            Vec3 vec = new Vec3(this.target.getX() - this.owner.getX(), 0, this.target.getZ() - this.owner.getZ());
            double motionX = vec.x * 0.05D;
            double motionY = 0.5D + this.owner.getRandom().nextDouble() * 0.5D;
            double motionZ = vec.z * 0.05D;
            grenade.setDeltaMovement(motionX, motionY, motionZ);
            grenade.setThrowableHeading(motionX, motionY * 6, motionZ, 5F, 0F);
            // 1.7 passed (x, z, y) to setTarget(x, y, z): the shell's target height is the target's z and its z the
            // target's height. hbm-space's argument order, ported as found.
            grenade.setTarget(this.target.getX(), this.target.getZ(), this.target.getY());
            this.owner.level().playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                    ModSounds.TURRET_JEREMY_FIRE.get(), SoundSource.HOSTILE, 25.0F, 1.0F);
            this.owner.level().addFreshEntity(grenade);
            this.reloadTimer = this.reloadDelay;
        } else {
            this.reloadTimer--;
        }
    }
}
