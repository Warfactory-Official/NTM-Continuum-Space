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
 * hbm-space {@code EntitySifterEel} (swim speed 1.8, watches players 8 blocks away), with one of six skins. The server chooses the skin once and preserves it through saves and entity tracking. It drops nothing (1.7 had no drop item).
 */
public class EntitySifterEel extends EntityFish implements IEntityEnumMulti {

    public enum SifterEel {
        PLAIN,
        FAST,
        EXOTIC,
        PHASED,
        ELEMENTAL,
        PERFECT,
    }

    private static final SifterEel[] VARIANTS = SifterEel.values();
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(EntitySifterEel.class, EntityDataSerializers.BYTE);

    public EntitySifterEel(EntityType<? extends EntitySifterEel> type, Level level) {
        super(type, level, 1.8, 8.0F);
        if (!level.isClientSide()) entityData.set(VARIANT, (byte) level.getRandom().nextInt(VARIANTS.length));
    }

    @Override
    public SifterEel getEnum() {
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
