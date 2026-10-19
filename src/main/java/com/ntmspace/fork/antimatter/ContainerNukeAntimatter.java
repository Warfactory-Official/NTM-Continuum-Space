package com.ntmspace.fork.antimatter;

import com.hbm.inventory.container.ContainerNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.ntmspace.fork.ForkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code ContainerNukeAntimatter}: lead (26,15), lead (134,14), core (81,15), hafnium (26,44), hafnium
 * (134,43); player inventory at y = 140.
 */
public class ContainerNukeAntimatter extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {{26, 15}, {134, 14}, {81, 15}, {26, 44}, {134, 43}};

    public ContainerNukeAntimatter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeAntimatter(int containerId, Inventory playerInventory, TileEntityAntimatter bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeAntimatter(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ForkContent.NUKE_ANTIMATTER_MENU.get(), containerId, playerInventory, bomb, pos,
                ForkContent.NUKE_ANTIMATTER.get(), 5, POSITIONS, 140);
    }

    public @Nullable TileEntityAntimatter getAntimatter() {
        return bomb instanceof TileEntityAntimatter amat ? amat : null;
    }

    /**
     * hbm-space {@code transferStackInSlot}: slots 0..2 merge into everything from slot 2 on, backwards (the hotbar
     * fills first, and the bomb's slots 2..4 are part of the range - kept); every other slot does nothing. The core slot
     * skips itself (1.7's merge could not match the one-item pellet with itself either).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index > 2) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (!this.moveItemStackTo(stack, index == 2 ? 3 : 2, this.slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
