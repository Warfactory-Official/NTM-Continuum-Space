package com.ntmspace.entity.missile;

import com.hbm.explosion.ExplosionLarge;
import com.ntmspace.registry.DropPodContent;
import com.ntmspace.tileentity.machine.storage.TileEntityCombatDropPod;
import dev.golem.core.data.CompatThrowableProjectile;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space {@code EntityCombatDropPod}: the alien invasion's drop pod. It falls straight down four blocks a tick (1.7
 * replaced {@code EntityThrowable.onUpdate} wholesale: motion forced to (0, -1, 0), four position steps per tick, no
 * interpolation), and as soon as the block one above its position is not air it plays the impact, places the
 * {@code combat_drop} block there and hands it the payload, the saved entity it carries and how many of it to release.
 *
 * <p>The landing test and the placed block share the floored position, including at negative coordinates. The client
 * runs the same fall and the server removes the falling entity after landing, including when its payload is empty.
 */
public class EntityCombatDropPod extends CompatThrowableProjectile {

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(EntityCombatDropPod.class, EntityDataSerializers.INT);

    /** The payload: a saved entity with its registry id under {@code "id"} (1.7 {@code EntityList} name). */
    private @Nullable CompoundTag entityType;
    private int amount;
    private int color;

    public EntityCombatDropPod(EntityType<? extends EntityCombatDropPod> type, Level level) {
        super(type, level);
    }

    public static EntityCombatDropPod create(Level level) {
        return new EntityCombatDropPod(DropPodContent.COMBAT_POD.get(), level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(COLOR, 0); // 1.7 dataWatcher slot 20
    }

    @Override
    public boolean fireImmune() {
        return true; // 1.7: isImmuneToFire = true
    }

    public void setColor(int color) {
        this.color = color;
        this.entityData.set(COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public void setPayload(CompoundTag entityData, int amount, int color) {
        this.entityType = entityData;
        this.amount = amount;
        this.setColor(color);
    }

    public @Nullable CompoundTag getPayload() {
        return entityType;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public void tick() {
        // 1.7 replaces EntityThrowable.onUpdate wholesale: no super.tick()
        double motionX = 0, motionY = -1, motionZ = 0;
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        Level level = level();

        for (int i = 0; i < 4; i++) {
            double posX = getX(), posY = getY(), posZ = getZ();
            var pos = BlockPos.containing(posX, posY + 1, posZ);

            if (!level.getBlockState(pos).isAir()
                    && !level.isClientSide()) {
                ExplosionLarge.spawnParticles(level, posX, posY + 1, posZ, 50);
                level.playSound(null, posX, posY, posZ, DropPodContent.HATCH_IMPACT.get(), SoundSource.BLOCKS, 10.0F,
                        0.5F + this.random.nextFloat() * 0.5F);

                level.setBlockAndUpdate(pos, DropPodContent.COMBAT_DROP.get().defaultBlockState());

                if (level.getBlockEntity(pos) instanceof TileEntityCombatDropPod capsule) {
                    if (this.entityType != null) {
                        capsule.setPayload(this.entityType, this.amount, this.color);
                        BlockState state = level.getBlockState(pos);
                        level.sendBlockUpdated(pos, state, state, 3); // 1.7 markBlockForUpdate
                    }
                }
                this.discard();
                break;
            }

            this.setPos(posX + motionX, posY + motionY, posZ + motionZ);
        }
    }

    // 1.7: isInRangeToRenderDist(distance < 500000); the parameter was the squared distance
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.entityType = input.read("entityType", CompoundTag.CODEC).orElse(new CompoundTag());
        this.color = input.getIntOr("color", 0);
        this.amount = input.getIntOr("amount", 0);
        this.setColor(this.color);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("color", color);
        output.putInt("amount", amount);
        // the payload names its entity type by registry id ("id": "hbm:entity_glyphid"), never a number
        if (entityType != null) output.store("entityType", CompoundTag.CODEC, entityType);
    }
}
