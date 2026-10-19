package com.ntmspace.entity.mob.ai;

import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.world.Navigations;
import java.util.EnumSet;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * hbm-space {@code EntityAIStepTowardsTarget}, the war machines' gait: every {@code stepDuration + stepCooldown} ticks
 * it beeps and paths {@code stepDistance} blocks straight towards the target (unless within {@code minDistance}), or
 * to a random block within 5 when there is none. 1.7 mutex 0: it runs beside every other task.
 */
public class EntityAIStepTowardsTarget extends Goal {

    private final PathfinderMob entity;
    private final double stepDistance;
    private final double speed;
    private final int stepDuration;
    private final int stepCooldown;
    private final double minDistance;
    private int stepTimer;
    /** 1.7 kept its own {@code java.util.Random}, not the entity's. */
    private final Random random = new Random();

    public EntityAIStepTowardsTarget(PathfinderMob entity, double stepDistance, double speed, int stepDuration,
                                     int stepCooldown, double minDistance) {
        this.entity = entity;
        this.stepDistance = stepDistance;
        this.speed = speed;
        this.stepDuration = stepDuration;
        this.stepCooldown = stepCooldown;
        this.minDistance = minDistance;
        this.stepTimer = 0;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        this.stepTimer = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    /** 1.7 updated every task every tick; the timers count ticks. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.stepTimer > 0) {
            this.stepTimer--;
        } else {
            LivingEntity target = this.entity.getTarget();
            if (target != null && target.isAlive()) {
                double dx = target.getX() - this.entity.getX();
                double dz = target.getZ() - this.entity.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                this.entity.playSound(SpaceMobs.BEEP.get(), 1.0F, 1.0F);

                if (distance > this.minDistance) {
                    dx = (dx / distance) * this.stepDistance;
                    dz = (dz / distance) * this.stepDistance;

                    Navigations.navigateTo(this.entity.getNavigation(), this.entity.getX() + dx, this.entity.getY(),
                            this.entity.getZ() + dz, this.speed);
                }
            } else {
                // no target: a random nearby block
                int randomX = Mth.floor(this.entity.getX()) + this.random.nextInt(10) - 5;
                int randomZ = Mth.floor(this.entity.getZ()) + this.random.nextInt(10) - 5;
                int randomY = Mth.floor(this.entity.getY());
                Navigations.navigateTo(this.entity.getNavigation(), randomX, randomY, randomZ, this.speed);
            }
            this.stepTimer = this.stepDuration + this.stepCooldown;
        }
    }
}
