package com.ntmspace.entity.mob;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.data.CompatMob;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Compat;
import dev.golem.core.world.EntityLoot;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityBFAngel}, the "Radioactive Angel": a 5 x 4 flying boss (10,000 health, 500 experience,
 * fireproof, a boss bar) that hunts the nearest player in a 100-block box, or a random living thing it can see.
 * It circles its target in dashes, charges a 70-tick tesla beam (with a voice line at 30), lays three telegraphed
 * orbital strikes every 200 ticks, and for 100 ticks in every 300 throws balefire. It ignores hits for 5 ticks after
 * one lands. Its death lasts 50 ticks, ends in a tiny-tot flash and drops the Angelic Core.
 *
 * <p>Its death then starts an invasion on the body ({@code CBT_Invasion(0, 122, false)} if none, see
 * {@code war/Invasions.angelDied}). Its {@code beamTimer} is never armed in hbm-space, so the ground
 * beam (kept below) never fires there either.
 */
public class EntityBFAngel extends CompatMob implements Enemy, IRadiationImmune {

    // 1.7 dataWatcher 16 / 17 / 18 / 19
    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WAYPOINT_X =
            SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Y =
            SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Z =
            SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.INT);

    public int courseChangeCooldown;
    public int scanCooldown;

    private @Nullable DamageSource deathLoot;
    private @Nullable Player deathLootPlayer;

    public int hurtCooldown;
    public int beamTimer;
    private final List<LivingEntity> secondaries = new ArrayList<>();

    private double lockedX;
    private double lockedY;
    private double lockedZ;

    private int aoeCooldown = 0;
    private int aoeStage = 0;

    private final Vec3[] strikepositions = new Vec3[3];
    private final int[] strikeTimers = new int[3];
    private final boolean[] strikeArmed = new boolean[3];

    public int gatlingCharge;
    public boolean isChargingGatling;

    public EntityBFAngel(EntityType<? extends EntityBFAngel> type, Level level) {
        super(type, level);
        setNoGravity(true); // 1.7 EntityFlying never applied gravity in moveEntityWithHeading
        this.moveControl = new DashControl(this);
        this.lookControl = new EaseLookControl(this);
        this.xpReward = 500;
        this.deathTime = -30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10000.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new ScanGoal(this));
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(BEAM, false);
        builder.define(WAYPOINT_X, 0);
        builder.define(WAYPOINT_Y, 0);
        builder.define(WAYPOINT_Z, 0);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        // 1.7 EntityFlying.fall() was empty
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D; // 1.7 isInRangeToRenderDist
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.hurtCooldown > 0) return false;

        boolean hit = super.hurtServer(level, source, amount);
        if (hit) this.hurtCooldown = 5;
        return hit;
    }

    /** 1.7 {@code canAttackClass}: anything but another angel (its {@code IProjectile} clause never matched). */
    private boolean canAttackClass(Entity entity) {
        return entity.getClass() != getClass();
    }

    /** 1.7 {@code updateEntityActionState}, which ran on the server only. */
    @Override
    protected void customServerAiStep(ServerLevel level) {

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;

        LivingEntity target = getTarget();
        if (target != null && this.courseChangeCooldown <= 0) setCourse(target);

        tickBeam(level);
        tickWeapons(level, target);
        if (target == null) return;

        tickStrikes(level, target);
        // 1.7:352-364 orbit waypoint dead: setCourse resets the cooldown first
        if (this.courseChangeCooldown <= 10) {
            this.moveControl.setWantedPosition(getWaypointX(), getWaypointY(), getWaypointZ(),
                    2.0D * (this.courseChangeCooldown / 15.0));
        }
        this.courseChangeCooldown--;
        this.lookControl.setLookAt(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
    }

    // 1.7:129-165: the nearest eligible player, anything living in sight a secondary, one of those if no player
    private void scan() {
        List<Entity> entities = level().getEntities(this, getBoundingBox().inflate(100D, 50D, 100D));
        this.secondaries.clear();
        LivingEntity target = null;

        for (Entity entity : entities) {

            if (!entity.isAlive() || !canAttackClass(entity)) continue;

            if (entity instanceof Player player) {
                if (player.isCreative()) continue;
                if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

                if (target == null || distanceToSqr(entity) < distanceToSqr(target)) target = player;
            }

            if (entity instanceof LivingEntity living && distanceToSqr(living) < 100D * 100D
                    && getSensing().hasLineOfSight(living) && living != target) {
                this.secondaries.add(living);
            }
        }

        if (target == null && !this.secondaries.isEmpty()) {
            target = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
        }

        setTarget(target);
        this.scanCooldown = 50;
    }

    // 1.7:167-183
    private void setCourse(LivingEntity target) {
        Vec3 vec = new Vec3(getX() - target.getX(), 0D, getZ() - target.getZ());
        if (this.random.nextInt(3) > 0) vec = vec.yRot((float) Math.PI * 2F * this.random.nextFloat());

        double length = vec.length();
        double overshoot = 35D;

        int wX = (int) Math.floor(target.getX() - vec.x / length * overshoot);
        int wZ = (int) Math.floor(target.getZ() - vec.z / length * overshoot);

        int surface = level().getHeight(Heightmap.Types.WORLD_SURFACE, wX, wZ) + this.random.nextInt(15);
        setWaypoint(wX, Math.max(surface, (int) target.getY() - 5), wZ);

        this.courseChangeCooldown = 40 + this.random.nextInt(20);
    }

    /** The ground beam: kills the column below for as long as {@code beamTimer} runs (never armed in hbm-space). */
    private void tickBeam(ServerLevel level) {
        if (this.beamTimer <= 0 && getBeam()) setBeam(false);

        if (this.beamTimer > 0) {
            this.beamTimer--;

            int iy = level.getMinY(); // 1.7: 0, the world's floor

            if (iy < getY()) {
                AABB column = new AABB(getX(), iy, getZ(), getX(), getY(), getZ()).inflate(5, 0, 5);

                for (Entity e : level.getEntities(this, column)) {
                    if (canAttackClass(e)) {
                        Compat.hurt(e, level, ModDamageSources.source(level, ModDamageSources.COMBINE, this, e), 1000F);
                        dev.golem.core.world.Effects.ignite(e, 5);

                        if (e instanceof LivingEntity living) {
                            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
                        }
                    }
                }

                Vec3 motion = getDeltaMovement();
                HbmEffectNT.Ufo.send(level, getX(), iy + 0.5, getZ(), null, 150D);
                HbmEffectNT.Ufo.send(level, getX() + motion.x * 0.5, iy + 0.5, getZ() + motion.z * 0.5, null, 150D);
            }
        }
    }

    private void tickWeapons(ServerLevel level, LivingEntity target) {
        if (this.tickCount % 300 < 200) {
            if (!this.secondaries.isEmpty()) {
                // 1.7 drew a random secondary here only to forget the dead ones
                LivingEntity e = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
                if (!e.isAlive()) this.secondaries.remove(e);
            }

            if (target == null) return;

            if (!this.isChargingGatling) {
                this.isChargingGatling = true;
                this.gatlingCharge = 70;
                return;
            }

            if (this.gatlingCharge > 10) {
                this.lockedX = target.getX();
                this.lockedY = target.getY();
                this.lockedZ = target.getZ();
            }

            this.gatlingCharge--;

            if (this.gatlingCharge == 30) {
                level.playSound(null, this, SpaceMobs.BFA_TALK.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
            }

            if (this.gatlingCharge <= 0) {
                fireBeamAt(this.lockedX, this.lockedY, this.lockedZ);

                this.isChargingGatling = false;
                this.gatlingCharge = 0;
            }

        } else if (this.tickCount % 20 == 0) {

            if (!this.secondaries.isEmpty()) {
                LivingEntity e = this.secondaries.get(this.random.nextInt(this.secondaries.size()));

                if (e.isAlive()) {
                    rocketAttack(e);
                } else {
                    this.secondaries.remove(e);
                }

            } else if (target != null) {
                rocketAttack(target);
            }

        } else if (this.tickCount % 4 == 2 && target != null) {
            rocketAttack(target);
        }
    }

    /** Three telegraphed strikes around the target: 60 ticks of red dust and lock-on beeps each, then a beam from above. */
    private void tickStrikes(ServerLevel level, LivingEntity target) {
        if (this.aoeCooldown <= 0 && this.aoeStage == 0) {
            this.aoeStage = 1;

            for (int i = 0; i < 3; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 12;
                double offsetZ = (this.random.nextDouble() - 0.5) * 12;

                this.strikepositions[i] = new Vec3(target.getX() + offsetX, target.getY(), target.getZ() + offsetZ);

                this.strikeTimers[i] = -i * 4;
                this.strikeArmed[i] = true;
            }

            this.aoeCooldown = 200;
        }

        boolean allDone = true;
        for (int i = 0; i < 3; i++) {
            if (!this.strikeArmed[i]) continue;

            this.strikeTimers[i]++;
            if (this.strikeTimers[i] < 0) {
                allDone = false;
                continue;
            }

            Vec3 pos = this.strikepositions[i];

            if (this.strikeTimers[i] < 60 && this.strikeTimers[i] % 5 == 0) {
                level.playSound(null, pos.x, pos.y, pos.z, STINGER_LOCK_ON, SoundSource.HOSTILE, 1.0F, 1F);
                // 1.7 "vanillaburst" mode "reddust", 35 particles, motion 0.2, 100-block radius
                level.sendParticles(DustParticleOptions.REDSTONE, pos.x, pos.y, pos.z, 35, 0D, 0D, 0D, 0.2D);
            }

            if (this.strikeTimers[i] >= 60) {
                fireDownwardBeamAt(pos.x, pos.y, pos.z);
                this.strikeArmed[i] = false;
            } else {
                allDone = false;
            }
        }

        if (allDone) this.aoeStage = 0;

        if (this.aoeCooldown > 0) this.aoeCooldown--;
    }

    /** The death table waits for the explosion, while retaining the player credited at death. */
    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        deathLoot = source;
        deathLootPlayer = killedByPlayer ? compatLastHurtByPlayer() : null;
    }

    /** 1.7 {@code onDeathUpdate}, the clock started at -30: the alarm at -10, the flash, the drop and the omen at 19. */
    @Override
    protected void tickDeath() {

        if (getBeam()) setBeam(false);

        setDeltaMovement(getDeltaMovement().subtract(0D, 0.05D, 0D));

        if (this.deathTime == -10) {
            level().playSound(null, this, ModSounds.CHOPPER_DAMAGE.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        }

        if (this.deathTime == 19 && level() instanceof ServerLevel server) {
            HbmEffectNT.TinyTot.send(server, getX(), getY() + 0.5, getZ(), null, 250D);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.MUKE_EXPLOSION.get(), SoundSource.HOSTILE, 15.0F, 1.0F);

            if (deathLoot != null) {
                for (var drop : EntityLoot.roll(server, "ntmspace:entities/entity_bfa", this, deathLoot, deathLootPlayer)) {
                    spawnAtLocation(server, drop, 1F);
                }
                deathLoot = null;
                deathLootPlayer = null;
            }

            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200, 200, 200))) {
                player.sendSystemMessage(Component.translatable("chat.hbmspace.angel.flicker").withStyle(ChatFormatting.RED));
            }

            // 1.7: CelestialBody.getBody(world).modifyTraits(new CBT_Invasion(0, 122, false)) when the body had no
            // invasion yet
            com.ntmspace.war.Invasions.angelDied(server);
        }

        super.tickDeath();
    }

    private void fireBeamAt(double x, double y, double z) {
        double spawnX = getX() - 1;
        double spawnY = getY() + 4;
        double spawnZ = getZ();

        EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, MobWeaponsSpace.TESLA_OVERCHARGE_STILL, 8F);
        bullet.setPos(spawnX, spawnY, spawnZ);
        bullet.setRotationsFromVector(new Vec3(x - spawnX, y - spawnY, z - spawnZ));
        bullet.performHitscanExternal(250D);

        level().addFreshEntity(bullet);
        playSound(SpaceMobs.BFA_SHOOT.get(), 5.0F, 1.0F);
    }

    private void fireDownwardBeamAt(double x, double y, double z) {
        EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, MobWeaponsSpace.TESLA_OVERCHARGE_STILL, 6F);
        bullet.setPos(x, y + 40, z);
        bullet.setRotationsFromVector(new Vec3(0, -1, 0));
        bullet.performHitscanExternal(250D);

        level().addFreshEntity(bullet);
        playSound(SpaceMobs.BFA_SHOOT.get(), 5.0F, 1.0F);
    }

    private void rocketAttack(Entity e) {
        Vec3 heading = new Vec3(e.getX() - getX(), e.getY() + e.getBbHeight() / 2.0 - getY() - 0.5D, e.getZ() - getZ()).normalize();

        // 1.7: the gun constructor from the eyes with no offset and a spread of 1, then the motion overwritten
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this, MobWeaponsSpace.FLAME_BALEFIRE_STILL, 0.5F, 1, 0, 0, 0);
        double speed = 1.0;
        bullet.setDeltaMovement(heading.x * speed, heading.y * speed, heading.z * speed);

        level().addFreshEntity(bullet);
        playSound(ModSounds.TURRET_RICHARD_FIRE.get(), 5.0F, 1.0F);
    }

    /** {@code hbm:weapon.stingerLockOn}, an HBM sound with no registered event; played by id. */
    private static final SoundEvent STINGER_LOCK_ON =
            SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of("hbm", "weapon.stingerlockon"));

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM);
    }

    /** 1.7 named these getX/getY/getZ, which are the position getters here. */
    public void setWaypoint(int x, int y, int z) {
        this.entityData.set(WAYPOINT_X, x);
        this.entityData.set(WAYPOINT_Y, y);
        this.entityData.set(WAYPOINT_Z, z);
    }

    public int getWaypointX() {
        return this.entityData.get(WAYPOINT_X);
    }

    public int getWaypointY() {
        return this.entityData.get(WAYPOINT_Y);
    }

    public int getWaypointZ() {
        return this.entityData.get(WAYPOINT_Z);
    }

    private static final class ScanGoal extends Goal {

        private final EntityBFAngel angel;

        private ScanGoal(EntityBFAngel angel) {
            this.angel = angel;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.angel.scanCooldown > 0) this.angel.scanCooldown--;
            if (this.angel.scanCooldown <= 0) this.angel.scan();
        }
    }

    // 1.7 EntityFlying: motion zeroed every AI tick, a dash sets it; speedModifier is blocks per tick.
    // The raw return type matches the superclass signature across nodes.
    private static final class DashControl extends MoveControl {

        private DashControl(EntityBFAngel angel) {
            super(angel);
        }

        @Override
        public void tick() {
            Vec3 motion = Vec3.ZERO;
            if (this.operation == Operation.MOVE_TO) {
                this.operation = Operation.WAIT;
                motion = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(),
                        this.wantedZ - this.mob.getZ()).normalize().scale(this.speedModifier);
            }
            this.mob.setDeltaMovement(motion);
        }
    }

    // 1.7:385-398: eases yRot (+90) and xRot from one above the feet; LookControl.tick resets xRot every tick
    private static final class EaseLookControl extends LookControl {

        private EaseLookControl(EntityBFAngel angel) {
            super(angel);
        }

        @Override
        public void tick() {
            if (!isLookingAtTarget()) return;
            this.lookAtCooldown = 0;

            double dx = this.wantedX - this.mob.getX();
            double dy = this.wantedY - (this.mob.getY() + 1);
            double dz = this.wantedZ - this.mob.getZ();

            float yaw = (float) (Math.atan2(dz, dx) * 180D / Math.PI) + 90F;
            this.mob.setYRot(this.mob.getYRot() + Mth.wrapDegrees(yaw - this.mob.getYRot()) * 0.15F);

            float pitch = (float) (Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180D / Math.PI);
            this.mob.setXRot(this.mob.getXRot() + (pitch - this.mob.getXRot()) * 0.1F);
        }
    }
}
