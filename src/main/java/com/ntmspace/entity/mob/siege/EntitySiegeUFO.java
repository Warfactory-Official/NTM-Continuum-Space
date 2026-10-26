package com.ntmspace.entity.mob.siege;

import com.hbm.entity.mob.EntityUFOBase;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.interfaces.IRadiationImmune;
import com.ntmspace.entity.mob.MobWeaponsSpace;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntitySiegeUFO} ("Mini UFO", {@code entity_meme_ufo}): a small saucer of one of nine siege tiers
 * (rolled when spawned from an egg) with the tier's health (a quarter of it), speed, damage threshold and resistance,
 * fire immunity and friendly-fire rule. It shoots an emerald beam at its target every 20..44 ticks, leading it by a
 * position refreshed one tick in ten, and after four minutes climbs away and despawns. It drops the tier's items.
 *
 * <p>1.7 re-applied the tier when loading, which refilled the saucer's health on every reload; ported as found.
 */
public class EntitySiegeUFO extends EntityUFOBase implements IRadiationImmune {

    /** 1.7 dataWatcher 12: the tier. Synced by index (runtime), saved by name. */
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(EntitySiegeUFO.class, EntityDataSerializers.INT);

    private int attackCooldown;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    private int lifetime = 0;
    private boolean isRetreating = false;
    private static final int MAX_LIFETIME = 60 * 2 * 20;

    /** 1.7 {@code random.break}, vanilla's item-break sound, played by id (a Holder from 1.21 on, an event before). */
    private static final net.minecraft.sounds.SoundEvent ITEM_BREAK =
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of("minecraft", "entity.item.break"));

    public EntitySiegeUFO(EntityType<? extends EntitySiegeUFO> type, Level level) {
        super(type, level);
    }

    /** 1.7: {@code EntityFlying}'s defaults; {@link #setTier} sets speed and health. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(TIER, 0);
    }

    /** 1.7 {@code EntityFlying.moveEntityWithHeading}: move, then a flat 0.91 drag (0.546 on the ground). */
    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(onGround() ? 0.6D * 0.91D : 0.91D));
    }

    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 0.25);
        setHealth(getMaxHealth());
    }

    public SiegeTier getTier() {
        SiegeTier tier = SiegeTier.tiers[this.entityData.get(TIER)];
        return tier != null ? tier : SiegeTier.CLAY;
    }

    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    public boolean isRetreating() {
        return this.isRetreating;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        if (isInvulnerable()) return false; // 1.7 isEntityInvulnerable(): the entity's own flag

        SiegeTier tier = getTier();

        if (tier.fireProof && source.is(DamageTypeTags.IS_FIRE)) {
            clearFire();
            return false;
        }

        // 1.7: an EntityDamageSource whose entity is not a player; an environmental source has no entity and passes
        if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player)) return false;

        damage -= tier.dt;

        if (damage < 0) {
            level.playSound(null, this, ITEM_BREAK, SoundSource.HOSTILE, 5F, 1.0F + this.random.nextFloat() * 0.5F);
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // The base resets motion and selects a course before the retreat waypoint overrides it.
        super.customServerAiStep(level);
        if (isRemoved()) return;

        this.lifetime++;
        if (this.lifetime > MAX_LIFETIME) {
            this.isRetreating = true;
        }

        if (this.isRetreating) {
            this.target = null;
            setWaypoint((int) getX(), 255, (int) getZ());
            approachPosition(20);
            if (getY() > 250 || this.lifetime > MAX_LIFETIME + 300) {
                discard();
                return;
            }
        }

        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (this.attackCooldown > 0) this.attackCooldown--;

        if (!this.isRetreating && this.target != null) {

            if (this.lastTargetX == 0 && this.lastTargetY == 0 && this.lastTargetZ == 0) {
                this.lastTargetX = this.target.getX();
                this.lastTargetY = this.target.getY() + this.target.getEyeHeight();
                this.lastTargetZ = this.target.getZ();
            }

            if (this.random.nextInt(10) == 0) {
                this.lastTargetX = this.target.getX();
                this.lastTargetY = this.target.getY() + this.target.getEyeHeight();
                this.lastTargetZ = this.target.getZ();
            }

            if (this.attackCooldown == 0) {
                this.attackCooldown = 20 + this.random.nextInt(25);

                double spawnX = getX();
                double spawnY = getY();
                double spawnZ = getZ();

                EntityBulletBeamBase bullet = new EntityBulletBeamBase(this,
                        MobWeaponsSpace.EMERALD_OVERCHARGE_STILL, 8F);
                bullet.setPos(spawnX, spawnY, spawnZ);
                bullet.setRotationsFromVector(new Vec3(this.lastTargetX - spawnX, this.lastTargetY - spawnY,
                        this.lastTargetZ - spawnZ));
                bullet.performHitscanExternal(250D);

                level.addFreshEntity(bullet);
                playSound(SpaceMobs.BFA_SHOOT.get(), 2.0F, 1.0F);
            }
        }

        if (this.courseChangeCooldown > 0) {
            double speed = this.target == null ? 0.5D : 1.0D + (getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.0D);

            if (this.isRetreating) speed *= 2.0D;

            approachPosition(speed);
        }
    }

    /** 1.7 {@code onSpawnWithEgg}: a random tier. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setTier(SiegeTier.tiers[this.random.nextInt(SiegeTier.getLength())]);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putString("siegeTier", getTier().name);
        out.putInt("lifetime", this.lifetime);
        out.putBoolean("isRetreating", this.isRetreating);
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        SiegeTier tier = SiegeTier.byName(in.getStringOr("siegeTier", ""));
        // Base loading has already restored attributes and health; setTier would heal it to full.
        this.entityData.set(TIER, (tier != null ? tier : SiegeTier.DEFAULT_BUFF).id);
        this.lifetime = in.getIntOr("lifetime", 0);
        this.isRetreating = in.getBooleanOr("isRetreating", false);
    }

}
