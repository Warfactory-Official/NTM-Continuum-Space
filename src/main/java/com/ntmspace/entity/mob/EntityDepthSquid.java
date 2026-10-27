package com.ntmspace.entity.mob;

import dev.golem.core.data.CompatSquid;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityDepthSquid}: a vanilla squid of the Sagan Sea in one of seven colours, with a real limb swing
 * (1.7's squid "just blasts through everything"; the depth squid's fins follow the walk cycle).
 *
 * <p>The server chooses the colour once and preserves it through saves and entity tracking.
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

    private static final DepthSquid[] VARIANTS = DepthSquid.values();
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(EntityDepthSquid.class, EntityDataSerializers.BYTE);

    public EntityDepthSquid(EntityType<? extends EntityDepthSquid> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) entityData.set(VARIANT, (byte) level.getRandom().nextInt(VARIANTS.length));
    }

    @Override
    public DepthSquid getEnum() {
        return VARIANTS[entityData.get(VARIANT)];
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(VARIANT, (byte) 0);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        MobVariants.load(entityData, VARIANT, VARIANTS, input);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        MobVariants.save(entityData, VARIANT, VARIANTS, output);
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

}
