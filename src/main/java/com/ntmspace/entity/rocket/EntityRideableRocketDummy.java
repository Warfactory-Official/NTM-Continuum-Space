package com.ntmspace.entity.rocket;

import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code EntityRideableRocket.EntityRideableRocketDummy}: a 4 x 2.5 hit box around the capsule of a rocket
 * taller than 8 blocks, so the capsule can be clicked and the look overlay read; it forwards both to the rocket. It is
 * never saved (1.7 refused to write it and died on load).
 */
public class EntityRideableRocketDummy extends CompatEntity implements com.hbm.interfaces.IEntityLookOverlay {

    private static final EntityDataAccessor<Integer> PARENT = SynchedEntityData.defineId(EntityRideableRocketDummy.class, EntityDataSerializers.INT);

    public @Nullable EntityRideableRocket parent;

    public EntityRideableRocketDummy(EntityType<? extends EntityRideableRocketDummy> type, Level level) {
        super(type, level);
    }

    public void setParent(EntityRideableRocket parent) {
        this.parent = parent;
        entityData.set(PARENT, parent.getId());
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(PARENT, 0);
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (parent == null || parent.isRemoved()) {
                discard();
            }
        } else {
            if (parent == null) {
                Entity entity = level().getEntity(entityData.get(PARENT));
                if (entity instanceof EntityRideableRocket rocket) {
                    parent = rocket;
                }
            }
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void loadData(DataIn input) {
        discard();
    }

    @Override
    protected void saveData(DataOut output) {}

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (parent == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return parent.onInteractDummy(player, hand);
    }

    @Override
    public com.hbm.interfaces.ILookOverlay.@Nullable Data getLookOverlay(Player player) {
        return parent == null ? null : parent.getLookOverlay(player);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        if (parent == null) return false;
        return parent.onHurtDummy(source, amount);
    }
}
