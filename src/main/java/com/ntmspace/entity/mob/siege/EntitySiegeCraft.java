package com.ntmspace.entity.mob.siege;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityUFOBase;
import com.hbm.entity.projectile.EntitySiegeLaser;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.PlasmaBlastPayload;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntitySiegeCraft} ({@code entity_micro_ufo}), the invasion's big saucer: 7 x 1, fireproof, a siege
 * tier with 25 times the tier's health. Every 30..39 ticks it fires a fan of seven siege lasers (5 degrees apart) at its
 * target, coloured from green to red by its health; every 300 ticks it locks on (110: the stinger lock-on sound, 120..60:
 * the lock-on follows the target, 100..40: red dust at the lock-on) and for the last 40 ticks beams the lock-on: 1,000
 * combine damage, 5 s of fire and 5 RAD to everything in a 2-block box (but creepers and ghasts, 1.7
 * {@code canAttackClass}), with a cyan plasma blast. After four minutes it climbs away at double speed and is gone at
 * y 250 or 15 s later. Drops its tier's items when a player kills it. Its death lasts 20 ticks and ends in a tiny-tot
 * flash.
 *
 * <p>1.7 re-applied the tier when loading, which refilled the saucer's health on every reload; ported as found (as
 * {@link EntitySiegeUFO}). The tier is saved by name. 1.7's beam reached through blocks (an AABB query, no ray), kept.
 * 1.7 also implemented {@code IBossDisplayData}, but its renderer's {@code BossStatus} call is commented out: no bar.
 */
public class EntitySiegeCraft extends EntityUFOBase implements IRadiationImmune {

    // 1.7 dataWatcher 12 (tier), 13/14/15 (lock-on), 16 (beam). The tier is synced by index (runtime), saved by name.
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LOCK_X =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Y =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Z =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> BEAM =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.BYTE);

    private int attackCooldown;
    private int beamCountdown;

    private int lifetime = 0;
    private boolean isRetreating = false;
    private static final int MAX_LIFETIME = 60 * 4 * 20;

    /** 1.7 {@code random.break}, vanilla's item-break sound, played by id (as {@link EntitySiegeUFO}). */
    private static final net.minecraft.sounds.SoundEvent ITEM_BREAK =
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of("minecraft", "entity.item.break"));

    /** 1.7 {@code hbm:weapon.stingerLockOn}: HBM's {@code weapon.stingerlockon} sound, which HBM has no registered event for. */
    private static final net.minecraft.sounds.SoundEvent STINGER_LOCK_ON =
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of("hbm", "weapon.stingerlockon"));

    public EntitySiegeCraft(EntityType<? extends EntitySiegeCraft> type, Level level) {
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
        builder.define(LOCK_X, 0F);
        builder.define(LOCK_Y, 0F);
        builder.define(LOCK_Z, 0F);
        builder.define(BEAM, (byte) 0);
    }

    /** 1.7 {@code EntityFlying.moveEntityWithHeading}: move, then a flat 0.91 drag (0.546 on the ground). */
    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(onGround() ? 0.6D * 0.91D : 0.91D));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        if (isInvulnerable()) return false;

        SiegeTier tier = getTier();

        if (tier.fireProof && source.is(DamageTypeTags.IS_FIRE)) {
            clearFire();
            return false;
        }

        // noFF can't be harmed by other mobs (1.7: an EntityDamageSource whose entity is no player)
        if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player)) return false;

        damage -= tier.dt;

        if (damage < 0) {
            level.playSound(null, this, ITEM_BREAK, SoundSource.HOSTILE, 5F, 1.0F + this.random.nextFloat() * 0.5F);
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurtServer(level, source, damage);
    }

    /** 1.7 {@code onDeathUpdate}: no beam, sinking, the tiny-tot flash and the muke sound at 19. */
    @Override
    protected void tickDeath() {
        this.beamCountdown = 200;
        setBeam(false);

        setDeltaMovement(getDeltaMovement().subtract(0D, 0.05D, 0D));

        if (this.deathTime == 19 && level() instanceof ServerLevel server) {
            HbmEffectNT.TinyTot.send(server, getX(), getY() + 0.5, getZ(), null, 250D);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.MUKE_EXPLOSION.get(), SoundSource.HOSTILE, 15.0F, 1.0F);
        }

        super.tickDeath();
    }

    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 25);
        setHealth(getMaxHealth());
    }

    public SiegeTier getTier() {
        int id = this.entityData.get(TIER);
        SiegeTier tier = id >= 0 && id < SiegeTier.tiers.length ? SiegeTier.tiers[id] : null;
        return tier != null ? tier : SiegeTier.CLAY;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam ? (byte) 1 : (byte) 0);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM) == 1;
    }

    public void setLockon(double x, double y, double z) {
        this.entityData.set(LOCK_X, (float) x);
        this.entityData.set(LOCK_Y, (float) y);
        this.entityData.set(LOCK_Z, (float) z);
    }

    public Vec3 getLockon() {
        return new Vec3(this.entityData.get(LOCK_X), this.entityData.get(LOCK_Y), this.entityData.get(LOCK_Z));
    }

    /** The current target (harness/GameTest view). */
    public @Nullable Entity target() {
        return this.target;
    }

    public boolean isRetreating() {
        return this.isRetreating;
    }

    public int lifetime() {
        return this.lifetime;
    }

    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    public void setBeamCountdown(int ticks) {
        this.beamCountdown = ticks;
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + this.random.nextInt(5);
    }

    @Override
    protected int wanderHeightOffset() {
        return 10 + this.random.nextInt(2);
    }

    /** 1.7 {@code updateEntityActionState}: the base saucer logic first, then this craft's. */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (isRemoved()) return;

        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

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

        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.beamCountdown > 0) this.beamCountdown--;

        if (this.random.nextInt(50) == 0) {
            CompoundTag dPart = new CompoundTag();
            dPart.putByte("count", (byte) (2 + this.random.nextInt(3)));
            HbmEffectNT.Tau.send(level, getX() + this.random.nextGaussian() * 2, getY() + this.random.nextGaussian(),
                    getZ() + this.random.nextGaussian() * 2, dPart, 50D);
        }

        boolean beam = false;

        if (this.target == null || this.beamCountdown <= 0) {
            this.beamCountdown = 300; // 200 - 100: nothing, 100 - 40: update lockon, 40 - 20: fix lockon, 20 - 0: beam (1.7)
        } else {

            if (this.beamCountdown >= 60 && this.beamCountdown < 120) {
                double x = this.target.getX();
                double y = this.target.getY() + this.target.getBbHeight() * 0.5;
                double z = this.target.getZ();
                setLockon(x, y, z);

                if (this.beamCountdown == 110) {
                    level.playSound(null, this.target, STINGER_LOCK_ON, SoundSource.HOSTILE, 2F, 0.75F);
                }
            }

            if (this.beamCountdown >= 40 && this.beamCountdown < 100) {
                Vec3 lockon = getLockon();
                // 1.7 vanillaburst "reddust", motion 0.2, count 5
                level.sendParticles(DustParticleOptions.REDSTONE, lockon.x, lockon.y, lockon.z, 5, 0D, 0D, 0D, 0.2D);
            }

            if (this.beamCountdown < 40) {

                Vec3 lockon = getLockon();

                if (this.beamCountdown == 39) {
                    level.playSound(null, lockon.x, lockon.y, lockon.z, ModSounds.UFO_BLAST.get(), SoundSource.HOSTILE, 5.0F,
                            0.9F + level.getRandom().nextFloat() * 0.2F);
                }

                List<Entity> entities = level.getEntities(this, new AABB(lockon, lockon).inflate(2, 2, 2));

                for (Entity e : entities) {
                    if (canAttackClass(e)) {
                        dev.golem.core.world.Compat.hurt(e, level, ModDamageSources.source(level, ModDamageSources.COMBINE, this, e), 1000F);
                        e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 5 * 20)); // setFire(5)

                        if (e instanceof LivingEntity living) {
                            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
                        }
                    }
                }

                PlasmaBlastPayload.send(level, lockon.x, lockon.y, lockon.z, 0.0F, 0.75F, 1.0F,
                        -90 + this.random.nextFloat() * 180, this.random.nextFloat() * 180F, 5F);
                beam = true;
            }
        }

        setBeam(beam);

        if (this.attackCooldown == 0 && this.target != null) {
            this.attackCooldown = 30 + this.random.nextInt(10);

            double x = getX();
            double y = getY();
            double z = getZ();

            Vec3 vec = new Vec3(this.target.getX() - x, this.target.getY() + this.target.getBbHeight() * 0.5 - y,
                    this.target.getZ() - z).normalize();
            SiegeTier tier = getTier();

            float health = getHealth() / getMaxHealth();

            int r = (int) (0xff * (1 - health));
            int g = (int) (0xff * health);
            int b = 0;
            int color = (r << 16) | (g << 8) | b;

            for (int i = 0; i < 7; i++) {

                // 1.7 Vec3.rotateAroundY = modern yRot
                Vec3 copy = vec.yRot((float) Math.PI / 180F * (i - 3) * 5F);

                EntitySiegeLaser laser = new EntitySiegeLaser(ModNtmEntities.SIEGE_LASER.get(), level, this);
                laser.setPos(x, y, z);
                laser.setThrowableHeading(copy.x, copy.y, copy.z, 1F, 0.0F);
                laser.setColor(color);
                laser.setDamage(tier.damageMod);
                laser.setBreakChance(tier.laserBreak * 2);
                if (tier.laserIncendiary) laser.setIncendiary();
                level.addFreshEntity(laser);
            }

            playSound(ModSounds.BALLS_LASER.get(), 2.0F, 1.0F);
        }

        if (this.courseChangeCooldown > 0) {
            double speed = this.target == null ? 0.5D : 1.0D + (getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.0D);

            if (this.isRetreating) speed *= 2.0D;

            approachPosition(speed);
        }
    }

    /** 1.7 {@code EntityLiving.canAttackClass}: anything but a creeper or a ghast (exact classes). */
    private static boolean canAttackClass(Entity e) {
        return e.getType() != EntityType.CREEPER && e.getType() != EntityType.GHAST;
    }

    /** 1.7 {@code setCourseWithoutTaget}: a waypoint 15 blocks (Gaussian) away, 5..10 above the ground. */
    @Override
    protected void setCourseWithoutTarget() {
        int x = (int) Math.floor(getX() + this.random.nextGaussian() * 15);
        int z = (int) Math.floor(getZ() + this.random.nextGaussian() * 15);
        setWaypoint(x, level().getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 5 + this.random.nextInt(6), z);
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
