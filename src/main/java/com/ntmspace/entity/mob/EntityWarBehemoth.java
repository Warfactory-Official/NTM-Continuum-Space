package com.ntmspace.entity.mob;

import dev.golem.core.world.EntityInterpolation;
import com.hbm.sound.ModSounds;
import com.ntmspace.entity.mob.ai.EntityAIBehemothGun;
import com.ntmspace.entity.mob.ai.EntityAIStepTowardsTarget;
import dev.golem.core.data.CompatMonster;
import dev.golem.core.data.SynchedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityWarBehemoth}, the two-legged war machine of Thatmo (1.75 x 6.35, 200 health, steps 5 blocks
 * high, never despawns): it strides towards its target ({@link EntityAIStepTowardsTarget}), sprays 7.62 mm rounds up
 * close and lobs artillery shells from range ({@link EntityAIBehemothGun}), has no air control and falls with an
 * extra 0.1 a tick. 1.7's knockback resistance of 100 is clamped to vanilla's maximum of 1, the same "never knocked".
 *
 * <p>Thatmo lists it as a creature (weight 2); later it belongs to the war system.
 */
public class EntityWarBehemoth extends CompatMonster {

    /** 1.7 dataWatcher 19: the attack target's entity id (0 for none), for the aiming renderer. */
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(EntityWarBehemoth.class, EntityDataSerializers.INT);

    public float aimPitch;
    public float aimPitchO;

    public EntityWarBehemoth(EntityType<? extends EntityWarBehemoth> type, Level level) {
        super(type, level);
        dev.golem.core.world.Compat.avoidWater(this);
        dev.golem.core.world.StepHeight.set(this, "ntmspace:war_behemoth", 5.0D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 100.0D)
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        // 1.7: EntityLiving, nearby only, anything but another behemoth - Mob is its counterpart
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, true,
                dev.golem.core.world.Targeting.selector(target -> !(target instanceof EntityWarBehemoth))));
        this.goalSelector.addGoal(3, new EntityAIStepTowardsTarget(this, 50, 0.3D, 50, 10, 0.6));
        this.goalSelector.addGoal(4, new EntityAIBehemothGun(this, true, true, 3, 35, 30));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
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

    /** 1.7 {@code jumpMovementFactor = 0}: no steering while airborne. */
    @Override
    protected float getFlyingSpeed() {
        return 0.0F;
    }

    /** 1.7 {@code canDespawn() = false}. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** As the tankbot: 1.7's {@code EntityMob} rule for the periodic spawner only. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL) return true;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        BlockPos pos = blockPosition();
        if (level.getBrightness(LightLayer.SKY, pos) > this.random.nextInt(32)) return false;
        return level.getMaxLocalRawBrightness(pos) <= this.random.nextInt(8);
    }

    /** 1.7: as the tankbot (egg, fire, magic, projectile, explosion, above 50). */
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

    /** The client tick owns aiming; renderer extraction only interpolates the two saved angles. */
    public float aimPitch(float partialTick) {
        return Mth.lerp(partialTick, aimPitchO, aimPitch);
    }

    /** 1.7 {@code onUpdate}: heavier gravity on the server, 0.1 a tick on top of vanilla's. */
    @Override
    public void tick() {
        if (level().isClientSide() && level().getEntity(getTargetId()) instanceof LivingEntity target) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double targetYaw = Math.atan2(dz, dx) * (180D / Math.PI) - 90D;
            EntityInterpolation.pullYaw(this, targetYaw);
        }
        super.tick();
        if (level().isClientSide()) {
            aimPitchO = aimPitch;
            if (level().getEntity(getTargetId()) instanceof LivingEntity target) {
                double dx = target.getX() - getX();
                double dy = target.getEyeY() - getEyeY();
                double dz = target.getZ() - getZ();
                double pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * -Mth.RAD_TO_DEG;
                aimPitch += (float) (Mth.wrapDegrees(pitch - aimPitch) * 0.1D);
            }
        } else {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, motion.y - 0.10D, motion.z);
        }
    }
}
