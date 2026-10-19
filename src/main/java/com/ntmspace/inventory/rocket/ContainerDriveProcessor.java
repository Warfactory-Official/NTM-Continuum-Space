package com.ntmspace.inventory.rocket;

import com.ntmspace.registry.DriveProcessorBlocks;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import dev.golem.core.world.Compat;
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
 * hbm-space's {@code ContainerDriveProcessor} (176 x 207): drive (30, 18), clone target (50, 38), circuit (81, 24),
 * battery (134, 72), all plain 1.7 slots; player inventory at (8, 125), hotbar 183.
 */
public class ContainerDriveProcessor extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineDriveProcessor te;

    public ContainerDriveProcessor(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineDriveProcessor t ? t : null);
    }

    public ContainerDriveProcessor(int containerId, Inventory playerInventory, @Nullable TileEntityMachineDriveProcessor te) {
        super(DriveProcessorBlocks.DRIVE_PROCESSOR_MENU.get(), containerId);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(4) {
            @Override public int getMaxStackSize() { return 1; }
        };

        // 0 - active drive slot
        // 1 - cloning drive slot
        // 2 - upgrade slot
        // 3 - battery slot
        addSlot(new Slot(machine, 0, 30, 18));
        addSlot(new Slot(machine, 1, 50, 38));
        addSlot(new Slot(machine, 2, 81, 24));
        addSlot(new Slot(machine, 3, 134, 72));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 125);
    }

    public @Nullable TileEntityMachineDriveProcessor getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /**
     * 1.7 {@code ContainerBase.transferStackInSlot}: a machine slot goes to the player, a player slot into the first
     * machine slot with room (one item each).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int size = machine.getContainerSize();
            if (index < size) {
                if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, size, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, stack);
        }
        return result;
    }
}
