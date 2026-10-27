package com.ntmspace.fork.machine;

import dev.golem.core.data.CarriedState;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.util.InventoryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import com.hbm.interfaces.IRepairable;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's damaged-machine rules for its two damageable HBM machines (the Leviathan turbine and the powered
 * condenser): 1.7 {@code onBlockExploded} (the first explosion that reaches the machine damages it, a second one removes
 * the core; the cells never drop or break from the blast), {@code IRepairable.tryRepairMultiblock} (a blowtorch and the
 * repair materials, taken from the inventory) and {@code IRepairable.addGenericOverlay} ("Repair with:" while holding a
 * blowtorch). A machine an NBT structure places is damaged ({@code INBTTileEntityTransformable.transformTE}): the Dres
 * breeder hall's turbine and condenser.
 */
public final class DamagedMachines {

    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");

    private DamagedMachines() {}

    /** The block entity side: damaged state, the explosion that last hit it, and its repair. */
    public interface Damageable extends IRepairable {
        boolean isDamaged();

        void setDamaged(boolean damaged);

        @Nullable Object lastExplosion();

        void lastExplosion(@Nullable Object explosion);

        List<AStack> getRepairMaterials();

        default void damage() { setDamaged(true); }

        @Override default void repair(Player player) { setDamaged(false); }

        @Override default void tryExtinguish(Level level, BlockPos pos, EnumExtinguishType type) {}
    }

    public static boolean damaged(BlockEntity entity) {
        return entity.getBlockState().getValue(DAMAGED);
    }

    /** Detached copies may have a level, but must never update the live entity at the same coordinates. */
    public static void set(BlockEntity entity, boolean damaged) {
        if (damaged(entity) == damaged) return;
        var state = entity.getBlockState().setValue(DAMAGED, damaged);
        var level = entity.getLevel();
        if (level == null || level.getBlockEntity(entity.getBlockPos()) != entity) entity.setBlockState(state);
        else level.setBlock(entity.getBlockPos(), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        entity.setChanged();
    }

    /** 1.7 {@code onBlockExploded}; true when {@code pos} belongs to a damageable machine (which then owns the hit). */
    public static boolean hit(ServerLevel level, BlockPos pos, Object explosion) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof Damageable machine)) return false;
        if (machine.lastExplosion() == explosion) return true;
        machine.lastExplosion(explosion);
        if (!machine.isDamaged()) {
            machine.damage();
        } else {
            level.removeBlock(core, false);
        }
        return true;
    }

    /** 1.7 {@code IRepairable.tryRepairMultiblock}: true when the machine was repaired (or will be, on the server). */
    public static boolean repair(Level level, BlockPos pos, Player player) {
        Damageable machine = damaged(level, pos);
        if (machine == null) return false;
        return IRepairable.tryRepairMultiblock(level, ((BlockEntity) machine).getBlockPos(), player);
    }

    /**
     * hbm-space {@code IRepairable.addGenericOverlay}: a damaged machine always lists its repair, with a "Tool:
     * Blowtorch" line while no blowtorch is held (1.7 showed the list only with one in hand).
     */
    public static ILookOverlay.@Nullable Data overlay(Level level, BlockPos pos, Player player, String title) {
        Damageable machine = damaged(level, pos);
        if (machine == null) return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GOLD + "Repair with:");
        if (!(player.getMainHandItem().getItem() instanceof ItemBlowtorch)) text.add("Tool: Blowtorch");
        for (AStack stack : machine.getRepairMaterials()) {
            try {
                ItemStack display = stack.getCyclingDisplayStack();
                text.add("- " + display.getHoverName().getString() + " x" + stack.stacksize);
            } catch (Exception ex) {
                text.add(ChatFormatting.RED + "- ERROR");
            }
        }
        return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
    }

    /**
     * 1.7 {@code IPersistentNBT.getDrops} with hbm-space's {@code writeNBT}: a damaged machine drops as a damaged item
     * ({@code damaged} in the item's carried block data; {@link #applyCarried} restores it when the item is placed,
     * through HBM's multiblock placement, which hands the stack's data to the new core). An intact machine drops plain.
     */
    public static void markDrops(net.minecraft.world.level.block.Block block, List<ItemStack> drops,
                                 net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be == null) {
            BlockPos pos = BlockPos.containing(params.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN));
            be = params.getLevel().getBlockEntity(pos);
        }
        if (!(be instanceof Damageable machine)) return;
        for (var i = 0; i < drops.size(); i++) {
            var original = drops.get(i);
            if (!original.is(block.asItem())) continue;
            var stack = new ItemStack(block.asItem(), original.getCount());
            CarriedState.writeItemFlag(stack, DAMAGED, "damaged", machine.isDamaged());
            drops.set(i, stack);
        }
    }

    /** 1.7 {@code readNBT} of the dropped item's data: {@code damaged}. */
    public static void applyCarried(Damageable machine, net.minecraft.nbt.CompoundTag data) {
        if (dev.golem.core.data.Tags.getBooleanOr(data, "damaged", false)) machine.setDamaged(true);
    }

    public static boolean carriedDamage(ItemStack stack) {
        return CarriedState.readItemFlag(stack, "damaged");
    }

    /** The damaged machine {@code pos} belongs to, or null. */
    public static @Nullable Damageable damaged(Level level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockEntity be = level.getBlockEntity(core);
        return be instanceof Damageable machine && machine.isDamaged() ? machine : null;
    }
}
