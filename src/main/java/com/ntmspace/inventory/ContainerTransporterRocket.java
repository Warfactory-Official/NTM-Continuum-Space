package com.ntmspace.inventory;

import com.ntmspace.registry.TransporterBlocks;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
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
 * hbm-space's {@code ContainerTransporterRocket} (230 x 236): input slots 2 x 4 at (8, 34), output slots 2 x 4 at
 * (98, 34), the four input-tank identifier slots at (8, 106), the two fuel identifier slots at (188, 90), the player's
 * inventory at (8, 154) and hotbar at (8, 212). Shift-click moves between the pad (all 22 slots) and the player.
 */
public final class ContainerTransporterRocket extends AbstractContainerMenu {

    private static final int SLOTS = 22;

    private final Container transporterInventory;
    private final @Nullable TileEntityTransporterRocket transporter;

    public ContainerTransporterRocket(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityTransporterRocket te ? te : null);
    }

    public ContainerTransporterRocket(int containerId, Inventory playerInventory, @Nullable TileEntityTransporterRocket transporter) {
        super(TransporterBlocks.TRANSPORTER_ROCKET_MENU.get(), containerId);
        this.transporter = transporter;
        this.transporterInventory = transporter != null ? transporter : new SimpleContainer(SLOTS);

        addSlots(transporterInventory, 0, 8, 34, 2, 4); // Input slots
        addSlots(transporterInventory, 8, 98, 34, 2, 4); // Output slots

        addSlots(transporterInventory, 16, 8, 106, 1, 4); // Input Fluid ID slots
        addSlots(transporterInventory, 20, 188, 90, 1, 2); // Fuel Fluid ID slots

        addSlots(playerInventory, 9, 8, 154, 3, 9); // Player inventory
        addSlots(playerInventory, 0, 8, 212, 1, 9); // Player hotbar
    }

    public @Nullable TileEntityTransporterRocket transporter() {
        return transporter;
    }

    // I'm gonna make a farken helper function for this shit, why was it done the old way for 9 whole ass years?
    private void addSlots(Container inv, int from, int x, int y, int rows, int cols) {
        int slotSize = 18;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                addSlot(new Slot(inv, col + row * cols + from, x + col * slotSize, y + row * slotSize));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index <= transporterInventory.getContainerSize() - 1) {
                if (!moveItemStackTo(stack, transporterInventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, transporterInventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    /** 1.7 {@code canInteractWith}: always. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
