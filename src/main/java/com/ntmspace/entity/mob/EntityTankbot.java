package com.ntmspace.entity.mob;

import com.hbm.interfaces.IRadiationImmune;
import com.hbm.particle.helper.EntityEffects;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import com.ntmspace.entity.mob.ai.EntityAIStepTowardsTarget;
import com.ntmspace.entity.mob.ai.EntityAITankshell;
import dev.golem.core.data.CompatMonster;
import dev.golem.core.data.SynchedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityTankbot}, Thatmo's six-legged walking cannon (2.45 x 3.25, 180 health, fireproof, never
 * despawns): it steps towards whatever it hunts every 220 ticks with a beep ({@link EntityAIStepTowardsTarget}) and
 * shells it from range ({@link EntityAITankshell}); its body turns towards its head at one degree a tick. Its death
 * lasts 45 ticks of gas flames and ends in a non-griefing explosion.
 *
 * <p>Thatmo lists it as a creature (weight 6); later it belongs to the war system.
 */
public class EntityTankbot extends CompatMonster implements IRadiationImmune {

    /** 1.7 dataWatcher 20: the attack target's entity id (0 for none), for the head-tracking renderer. */
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(EntityTankbot.class, EntityDataSerializers.INT);

    /** Client: the head's smoothed yaw towards the target (1.7 kept it on the entity, the renderer updated it per frame). */
    public double headTargetYaw;

    public EntityTankbot(EntityType<? extends EntityTankbot> type, Level level) {
        super(type, level);
        dev.golem.core.world.Compat.avoidWater(this);
        this.deathTime = -50;
    }

    /** 1.7: EntityMob's defaults (movement 0.7) with follow range 60, attack 15, knockback resistance 1, health 180. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.2D));
        // 1.7: EntityLiving, nearby only, anything but another tankbot - Mob is its counterpart
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, true,
                dev.golem.core.world.Targeting.selector(target -> !(target instanceof EntityTankbot))));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(3, new EntityAIStepTowardsTarget(this, 50, 0.2D, 200, 20, 0.6));
        this.goalSelector.addGoal(4, new EntityAITankshell(this, true, true, 2, 10, 30));
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(TARGET_ID, 0);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        this.entityData.set(TARGET_ID, target != null ? target.getId() : 0);
    }

    /** 1.7 {@code canDespawn() = false}. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    /** 1.7 {@code renderDistanceWeight = 24}. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = getBoundingBox().getSize();
        if (Double.isNaN(size)) size = 1.0D;
        size *= 64.0D * 24.0D;
        return distance < size * size;
    }

    /**
     * 1.7 {@code EntityMob.getCanSpawnHere} for the periodic spawner: not peaceful, and 1.7's light check (sky light
     * above a random 0..31 refuses; otherwise the light must not exceed a random 0..7). 1.7's chunk-generation spawn
     * never asked; the type's own rule is the plain mob rule, so that one does not either.
     */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL) return true;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        BlockPos pos = blockPosition();
        if (level.getBrightness(LightLayer.SKY, pos) > this.random.nextInt(32)) return false;
        return level.getMaxLocalRawBrightness(pos) <= this.random.nextInt(8);
    }

    /** 1.7 {@code onDeathUpdate}, with the death clock started at -50: the alarm at -30, flames from it, the blast at -5. */
    @Override
    protected void tickDeath() {
        if (this.deathTime == -30) {
            level().playSound(null, this, ModSounds.CHOPPER_DAMAGE.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        }
        if (this.deathTime >= -30 && level() instanceof ServerLevel server) {
            EntityEffects.gasFlame(server, getX(), getY() + 1, getZ(), this.random.nextGaussian(), 0.4,
                    this.random.nextGaussian(), 6.5F, 150D);
        }
        if (this.deathTime == -5 && level() instanceof ServerLevel server) {
            // The flaming blast ignites air over surviving solid surfaces while NONE keeps the floor intact.
            server.explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.NONE);
            ExplosionCreator.composeEffectSmall(server, getX(), getY(), getZ());
            discard();
            return;
        }

        super.tickDeath();
    }

    /**
     * 1.7: an egg kills it one time in ten (no experience), fire and magic do nothing, projectiles a quarter,
     * explosions half, and anything above 50 counts a quarter past 50.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(10) == 0) {
            this.xpReward = 0;
            setHealth(0F);
            return true;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) amount = 0F;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) amount = 0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= 0.25F;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;

        if (amount > 50F) amount = 50F + (amount - 50F) * 0.25F;

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_CYBERCRAB.get();
    }

    /** 1.7 {@code onUpdate}: the body turns towards the head, at most one degree a tick. */
    @Override
    public void tick() {
        super.tick();

        float targetYaw = this.yHeadRot;
        float currentYaw = getYRot();
        float maxYawChange = 1F;

        float yawDifference = targetYaw - currentYaw;

        while (yawDifference < -180.0F) yawDifference += 360.0F;
        while (yawDifference >= 180.0F) yawDifference -= 360.0F;

        if (yawDifference > maxYawChange) yawDifference = maxYawChange;
        if (yawDifference < -maxYawChange) yawDifference = -maxYawChange;

        setYRot(getYRot() + yawDifference);
    }
}
