package com.ntmspace.entity.mob;

import dev.golem.core.data.CompatWaterAnimal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code EntityFish}, the base of Laythe's scutterfish, scrapfish and sifter eel: a 1.7 water mob with three
 * AI tasks and its own swimming. Every 1/200 ticks (or whenever it is out of water) it picks a point up to 10 blocks
 * away and +-4 up or down, and each tick pushes its motion 0.01 towards it plus a little noise. In water the move
 * applies only {@code motionY *= 0.8} and no horizontal drag at all, so a fish overshoots and swings around its target
 * like a spring - 1.7 behaviour, ported as written. Out of water it flops: pitch creeps, it jumps and is kicked about.
 *
 * <p>The 2 x 2 hitbox is 1.7's ({@code setSize(2.0F, 2.0F)} in this base, for every fish).
 */
public abstract class EntityFish extends CompatWaterAnimal {

    private Vec3 currentSwimTarget;

    protected EntityFish(EntityType<? extends EntityFish> type, Level level, double swimSpeed, float watchDistance) {
        super(type, level);
        getNavigation().setCanFloat(true);

        // 1.7 added these in the constructor with the subclass's speed and range; registerGoals runs before them
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, watchDistance));
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, swimSpeed));

        this.currentSwimTarget = new Vec3(getX(), getY(), getZ());
    }

    /** 1.7: EntityWaterMob's defaults with 15 health and a follow range of 10. */
    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 10.0D);
    }

    /** 1.7 {@code EntityWaterMob.allowLeashing()} was false. */
    @Override
    protected boolean compatCanBeLeashed() {
        return false;
    }

    /** 1.7 {@code updateAITasks}, after the tasks: out of water a fish tips, jumps and is kicked about on the ground. */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (!isInWater()) {
            setXRot(getXRot() + 0.2F);
            setYRot(0.0F);
            setJumping(true);
            if (onGround()) {
                push(0.4 * this.random.nextDouble() - 0.4 * this.random.nextDouble(), 0.0,
                        0.4 * this.random.nextDouble() - 0.4 * this.random.nextDouble());
            }
        }
    }

    /**
     * 1.7 {@code onLivingUpdate}: vanilla's update first, then the swim target, the push towards it and the turn to
     * face the motion. 1.7 also ran this on the client, where the result was overwritten by the server's position and
     * rotation updates; a modern client never moves a mob itself, so it runs on the server only.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        updateSwimTarget();
        moveTowardsTarget();
        updateRotation();
    }

    private void updateSwimTarget() {
        if (this.random.nextInt(200) == 0 || !isInWater()) {
            double targetX = getX() + (this.random.nextDouble() - 0.5) * 20.0;
            double targetY = Mth.clamp(getY() + (this.random.nextDouble() - 0.5) * 8.0,
                    level().getMinY(), level().getMinY() + level().getHeight() - 1);
            double targetZ = getZ() + (this.random.nextDouble() - 0.5) * 20.0;
            this.currentSwimTarget = new Vec3(targetX, targetY, targetZ);
        }
    }

    private void moveTowardsTarget() {
        double deltaX = this.currentSwimTarget.x - getX();
        double deltaY = this.currentSwimTarget.y - getY();
        double deltaZ = this.currentSwimTarget.z - getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        double dirX = deltaX / distance;
        double dirY = deltaY / distance;
        double dirZ = deltaZ / distance;

        double speed = 0.01;
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x + dirX * speed + (this.random.nextDouble() - 0.5) * 0.02,
                motion.y + dirY * speed + (this.random.nextDouble() - 0.5) * 0.02,
                motion.z + dirZ * speed + (this.random.nextDouble() - 0.5) * 0.02);
        // 1.7 then called an updateRotation(float, float, float) overload that did nothing with its arguments
    }

    /** 1.7 {@code moveEntityWithHeading}: in water, motion as it stands (y damped), plus the limb swing; out of it, vanilla. */
    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, motion.y * 0.8F, motion.z);
            move(MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    private void updateRotation() {
        Vec3 motion = getDeltaMovement();
        float targetYaw = (float) (Math.atan2(motion.z, motion.x) * (180D / Math.PI)) - 90F;
        float targetPitch = (float) -(Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * (180D / Math.PI));

        setYRot(approach(getYRot(), targetYaw, 10.0F));
        setXRot(approach(getXRot(), targetPitch, 10.0F));
    }

    private static float approach(float current, float target, float maxIncrement) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxIncrement) delta = maxIncrement;
        if (delta < -maxIncrement) delta = -maxIncrement;
        return current + delta;
    }
}
