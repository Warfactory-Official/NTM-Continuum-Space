package com.ntmspace.entity.mob;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * hbm-space {@code EntityScrapFish} (swim speed 0.8, watches players 4 blocks away), with one of seven skins chosen once on the server and preserved by saves and entity tracking. It drops 1..3+looting machine parts from HBM's
 * {@code POOL_MACHINE_PARTS} item pool.
 */
public class EntityScrapFish extends EntityFish implements IEntityEnumMulti {

    public enum ScrapFish {
        STEEL,
        ALUMINIUM,
        ISOTOPE,
        CADMIUM,
        TECH,
        BLOOD,
        HORROR,
    }

    private static final ScrapFish[] VARIANTS = ScrapFish.values();
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(EntityScrapFish.class, EntityDataSerializers.BYTE);

    public EntityScrapFish(EntityType<? extends EntityScrapFish> type, Level level) {
        super(type, level, 0.8, 4.0F);
        if (!level.isClientSide()) entityData.set(VARIANT, (byte) level.getRandom().nextInt(VARIANTS.length));
    }

    @Override
    public ScrapFish getEnum() {
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

}
