package com.ntmspace.fork.antimatter;

import com.hbm.inventory.container.ContainerNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.ntmspace.fork.ForkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code ContainerNukeAntimatter}: lead (26,15), lead (134,14), core (81,15), hafnium (26,44), hafnium
 * (134,43); player inventory at y = 140.
 */
public class ContainerNukeAntimatter extends ContainerNukeBomb {

    public static final int SHOWN = 5;

    private static final int[][] POSITIONS = {{26, 15}, {134, 14}, {81, 15}, {26, 44}, {134, 43}};

    public ContainerNukeAntimatter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeAntimatter(int containerId, Inventory playerInventory, TileEntityAntimatter bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeAntimatter(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ForkContent.NUKE_ANTIMATTER_MENU.get(), containerId, playerInventory, bomb, pos,
                ForkContent.NUKE_ANTIMATTER.get(), SHOWN, POSITIONS, 140);
    }

    public @Nullable TileEntityAntimatter getAntimatter() {
        return bomb instanceof TileEntityAntimatter amat ? amat : null;
    }

    /**
     * Empty any visible bomb slot into the player's inventory, hotbar first. The old three-slot bounds included
     * the bomb's own components; the visible five-slot menu keeps those components out of the destination range.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= SHOWN) return ItemStack.EMPTY;
        var slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var result = stack.copy();
        if (!this.moveItemStackTo(stack, SHOWN, this.slots.size(), true)) return ItemStack.EMPTY;
        slot.onQuickCraft(stack, result);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }
}
