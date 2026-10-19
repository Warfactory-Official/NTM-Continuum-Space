package com.ntmspace.tileentity.machine.storage;

import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmspace.registry.DropPodContent;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.Entities;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space {@code TileEntityCombatDropPod}: the landed pod. It waits 40 ticks ({@code delay}), then swings its two
 * hatches open (each tick 20% of the way to +90 / -90 degrees, on both sides: the client animates from the synced
 * state) and, on the server, releases {@code amount} copies of the payload entity at the block's centre, one block up,
 * with the hatch sound. With hbm-space's {@code SpaceConfig.combatPodDespawn} (default off) the block also removes itself
 * whenever the world time is a multiple of 1000.
 *
 * <p>1.7 synced the whole tile entity once through its description packet ({@code markBlockForUpdate}); here that is the
 * full update tag of {@link TileEntityLoadedBase} plus {@code sendBlockUpdated}. Its {@code IBufPacketReceiver} methods
 * were never called (no {@code networkPack}), so they are not ported.
 *
 * <p>1.7 rebuilt each released entity from the saved payload unchanged, so every copy carried the same UUID; a modern
 * world refuses a second entity with a known UUID, so each copy is loaded without the saved {@code "UUID"} and keeps the
 * fresh one it was created with.
 */
public class TileEntityCombatDropPod extends TileEntityLoadedBase {

    /** hbm-space {@code SpaceConfig.combatPodDespawn} ({@code 1.94_combatPodDespawn}), default false. */
    public static boolean combatPodDespawn = false;

    public @Nullable CompoundTag entityType;
    public int amount;
    public int color;
    public int delay = 40;

    public double hatchopen;
    public double hatchopen2;
    public double prevHatchopen;
    public double prevHatchopen2;

    public TileEntityCombatDropPod(BlockPos pos, BlockState state) {
        super(DropPodContent.COMBAT_DROP_BE.get(), pos, state);
    }

    public void setPayload(CompoundTag entityType, int amount, int color) {
        this.entityType = entityType;
        this.amount = amount;
        this.color = color;
        setChanged();
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCombatDropPod te) {
        te.updateEntity(level, pos, false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCombatDropPod te) {
        te.updateEntity(level, pos, true);
    }

    /** 1.7 {@code updateEntity}, run on both sides as 1.7 did. */
    private void updateEntity(Level level, BlockPos pos, boolean server) {
        prevHatchopen = hatchopen;
        prevHatchopen2 = hatchopen2;

        if (delay > 0) {
            delay--;
            return;
        }

        if (delay == 0) {
            hatchopen += (90 - hatchopen) * 0.2;
            hatchopen2 += (-90 - hatchopen2) * 0.2;
        }

        if (entityType != null && amount > 0 && server) {
            for (int i = 0; i < amount; i++) {
                Entity entity = createEntityFromNBT(entityType, level);
                if (entity != null) {
                    entity.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    level.addFreshEntity(entity);
                }
            }
            amount = 0;
            setChanged();
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), DropPodContent.HATCH_OPEN.get(), SoundSource.BLOCKS,
                    10.0F, 0.9F);
            return;
        }

        // 1.7 also ran this on the client; the server's removal reaches the client anyway
        if (combatPodDespawn && server && level.getGameTime() % 1000 == 0) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    /** 1.7 {@code EntityList.createEntityFromNBT}: the type named by {@code "id"}, loaded from the tag; null if unknown. */
    public static @Nullable Entity createEntityFromNBT(CompoundTag tag, Level level) {
        String id = Tags.getStringOr(tag, "id", "");
        // the entity registry is defaulted (pig): an unknown id must not resolve
        if (id.isEmpty() || !BuiltInRegistries.ENTITY_TYPE.containsKey(Ids.parse(id))) return null;
        EntityType<?> type = Entities.byId(id);
        if (type == null) return null;
        Entity entity = Entities.createDetached(type, level);
        if (entity == null) return null;
        CompoundTag data = tag.copy();
        data.remove("UUID"); // see the class comment
        Entities.loadFromTag(entity, data);
        return entity;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        // the payload names its entity type by registry id, never a number
        if (entityType != null) output.store("EntityType", CompoundTag.CODEC, entityType);
        output.putInt("amount", amount);
        output.putInt("color", color);
        output.putInt("delay", delay);
        output.putDouble("hatchopen", hatchopen);
        output.putDouble("hatchopen2", hatchopen2);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        // 1.7 getCompoundTag/getInteger/getDouble: an absent key reads as empty / 0
        entityType = input.read("EntityType", CompoundTag.CODEC).orElse(new CompoundTag());
        amount = input.getIntOr("amount", 0);
        color = input.getIntOr("color", 0);
        delay = input.getIntOr("delay", 0);
        hatchopen = input.getDoubleOr("hatchopen", 0);
        hatchopen2 = input.getDoubleOr("hatchopen2", 0);
    }
}
