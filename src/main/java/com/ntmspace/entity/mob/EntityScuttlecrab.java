package com.ntmspace.entity.mob;

import dev.golem.core.data.CompatWaterAnimal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityScuttlecrab}, Laythe's reef crab: a 1.7 {@code EntityWaterMob} (default 0.6 x 1.8 box, 20
 * health) with no AI tasks, so 1.7 ran {@code EntityLiving.updateEntityActionState}: no walking input, a random turn
 * now and then, a glance at a player within 8 blocks, and a jump while in water 80% of the time. It walks on the sea
 * floor because it answers "not in water" while it updates; its strafe and forward inputs are swapped and the strafe
 * flips direction every 20..99 ticks ("crab walk"). With 1.7's zero inputs the crab therefore only turns and sinks;
 * both oddities are ported as written. One crab in eight is clay-infused (skin rolled per client, as 1.7 did).
 */
public class EntityScuttlecrab extends CompatWaterAnimal implements IEntityEnumMulti {

    public enum Scuttlecrab {
        TROPICAL,
        CLAYINFUSED,
    }

    public final Scuttlecrab type;

    /** Walk on the seafloor: the water check answers false while the living update runs. */
    private boolean seafloorWalking = false;

    // crab walk
    private float crabDirection = 1;
    private int switchTimer = 0;

    // 1.7 EntityLiving.updateEntityActionState state
    private float randomYawVelocity;
    private @Nullable Entity currentTarget;
    private int numTicksToChaseTarget;

    public EntityScuttlecrab(EntityType<? extends EntityScuttlecrab> type, Level level) {
        super(type, level);
        this.type = level.getRandom().nextInt(8) == 0 ? Scuttlecrab.CLAYINFUSED : Scuttlecrab.TROPICAL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes();
    }

    @Override
    public Enum<?> getEnum() {
        return this.type;
    }

    /** 1.7 {@code EntityWaterMob.allowLeashing()} was false. */
    @Override
    protected boolean compatCanBeLeashed() {
        return false;
    }

    @Override
    public boolean isInWater() {
        if (this.seafloorWalking) return false;
        return super.isInWater();
    }

    @Override
    public void aiStep() {
        this.seafloorWalking = true;
        super.aiStep();
        this.seafloorWalking = false;
    }

    /** 1.7 {@code moveEntityWithHeading(strafe, forward)} called super with {@code (forward * dir, strafe)}. */
    @Override
    public void travel(Vec3 input) {
        this.switchTimer--;
        if (this.switchTimer <= 0) {
            this.crabDirection *= -1;
            this.switchTimer = 20 + level().getRandom().nextInt(80);
        }

        super.travel(new Vec3(input.z * this.crabDirection, input.y, input.x));
    }

    /** 1.7 {@code EntityLiving.updateEntityActionState}, the legacy AI of a mob without tasks (despawning aside). */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        this.xxa = 0.0F;
        this.zza = 0.0F;
        float f = 8.0F;

        if (this.random.nextFloat() < 0.02F) {
            Player player = level.getNearestPlayer(this, f);
            if (player != null) {
                this.currentTarget = player;
                this.numTicksToChaseTarget = 10 + this.random.nextInt(20);
            } else {
                this.randomYawVelocity = (this.random.nextFloat() - 0.5F) * 20.0F;
            }
        }

        if (this.currentTarget != null) {
            getLookControl().setLookAt(this.currentTarget, 10.0F, getMaxHeadXRot());
            if (this.numTicksToChaseTarget-- <= 0 || !this.currentTarget.isAlive()
                    || this.currentTarget.distanceToSqr(this) > (double) (f * f)) {
                this.currentTarget = null;
            }
        } else {
            if (this.random.nextFloat() < 0.05F) {
                this.randomYawVelocity = (this.random.nextFloat() - 0.5F) * 20.0F;
            }
            setYRot(getYRot() + this.randomYawVelocity);
            setXRot(0.0F);
        }

        // isInWater() answers false here (seafloor walking), so in 1.7 this only ever set the jump flag in lava
        if (isInWater() || isInLava()) {
            setJumping(this.random.nextFloat() < 0.8F);
        }
    }

    /** 1.7 {@code getDropItem() = Items.clay_ball} through {@code EntityLiving.dropFewItems}: 0..2 plus 0..looting. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = source.getEntity() instanceof LivingEntity attacker
                ? dev.golem.core.world.Compat.lootingLevel(level, attacker) : 0;
        int j = this.random.nextInt(3);
        if (looting > 0) j += this.random.nextInt(looting + 1);
        for (int k = 0; k < j; ++k) spawnAtLocation(level, new ItemStack(Items.CLAY_BALL));
    }
}
