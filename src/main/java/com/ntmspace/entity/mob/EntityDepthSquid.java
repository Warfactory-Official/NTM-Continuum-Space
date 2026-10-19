package com.ntmspace.entity.mob;

import dev.golem.core.data.CompatSquid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityDepthSquid}: a vanilla squid of the Sagan Sea in one of seven colours, with a real limb swing
 * (1.7's squid "just blasts through everything"; the depth squid's fins follow the walk cycle).
 *
 * <p>The colour is rolled in the constructor from the world's random and is neither saved nor synced, exactly as in
 * 1.7: each client, and each reload, rolls its own. Ported as found.
 */
public class EntityDepthSquid extends CompatSquid implements IEntityEnumMulti {

    public enum DepthSquid {
        AQUA,
        BLACK,
        ORANGE,
        OURPLE,
        RED,
        SILVER,
        VICIOUS,
    }

    public final DepthSquid type;

    public EntityDepthSquid(EntityType<? extends EntityDepthSquid> type, Level level) {
        super(type, level);
        this.type = DepthSquid.values()[level.getRandom().nextInt(DepthSquid.values().length)];
    }

    @Override
    public Enum<?> getEnum() {
        return this.type;
    }

    /** 1.7 {@code EntitySquid.setSize(0.95F, 0.95F)}; the modern squid is 0.8. */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.95F, 0.95F);
    }

    /** 1.7's squid had no age; hand 26.1's squid a group that never makes a baby (as HBM's plastic bag does). */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        return super.finalizeSpawn(level, difficulty, reason, data != null ? data : new AgeableMob.AgeableMobGroupData(false));
    }

    /** 1.7 {@code moveEntityWithHeading}: the squid's move, then the limb swing from the horizontal distance moved. */
    @Override
    public void travel(Vec3 input) {
        super.travel(input);
        calculateEntityAnimation(false);
    }

    /** 1.7 {@code EntitySquid.getCanSpawnHere}: between y 45 and 63, exclusive. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return getY() > 45.0D && getY() < 63.0D;
    }

    /** 1.7 {@code EntitySquid.dropFewItems}: {@code rand.nextInt(3 + looting) + 1} ink sacs (the type has no loot table). */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = source.getEntity() instanceof LivingEntity attacker
                ? dev.golem.core.world.Compat.lootingLevel(level, attacker) : 0;
        int j = this.random.nextInt(3 + looting) + 1;
        for (int k = 0; k < j; ++k) spawnAtLocation(level, new ItemStack(Items.INK_SAC));
    }
}
