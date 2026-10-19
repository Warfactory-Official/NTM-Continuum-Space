package com.ntmspace.inventory.machine;

import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ContainerStardar} (176 x 256): the drive slot at (150, 124), the player inventory from y 174 and
 * the hotbar at 232. 1.7's {@code ContainerBase} shift-click: machine slot to the player, player stack to the machine.
 */
public class ContainerStardar extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineStardar te;

    public ContainerStardar(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(pos) instanceof TileEntityMachineStardar t ? t : null);
    }

    public ContainerStardar(int id, Inventory playerInv, @Nullable TileEntityMachineStardar te) {
        super(SpaceMachinesD.STARDAR_MENU.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(1) {
            @Override public int getMaxStackSize() { return 1; }
        };

        addSlot(MachineMenus.plain(machine, 0, 150, 124));

        MachineMenus.playerInventory(this::addSlot, playerInv, 174, 232);
    }

    public @Nullable TileEntityMachineStardar getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** 1.7 {@code ContainerBase.transferStackInSlot}: the drive slot to the player inventory, a player stack to the drive slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
