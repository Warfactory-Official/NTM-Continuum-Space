package com.ntmspace.inventory.war;

import com.ntmspace.registry.WarContent;
import com.ntmspace.tileentity.war.TileEntityMachineWarController;
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
 * hbm-space's {@code ContainerMachineWarController} (176 x 204): four plain slots at (6, 52), (29, 11) the drive,
 * (49, 11) the satellite chip and (29, 31); player inventory at (8, 103), hotbar at 162 (1.7 {@code playerInv(inv, 8,
 * 103, 162)}, one pixel lower than the usual 58 below).
 */
public class ContainerMachineWarController extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineWarController te;

    public ContainerMachineWarController(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineWarController t ? t : null);
    }

    public ContainerMachineWarController(int containerId, Inventory playerInventory, @Nullable TileEntityMachineWarController te) {
        super(WarContent.WAR_CONTROLLER_MENU.get(), containerId);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(4);

        // 4 Slots (shifted upwards by modifying y values) (1.7)
        addSlot(new Slot(machine, 0, 6, 52));
        addSlot(new Slot(machine, 1, 29, 11));
        addSlot(new Slot(machine, 2, 49, 11));
        addSlot(new Slot(machine, 3, 29, 31));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }
    }

    public @Nullable TileEntityMachineWarController getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** 1.7 {@code transferStackInSlot}: a machine slot goes to the player (from the end), a player slot into slots 0..3. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 4) {
                if (!moveItemStackTo(stack, 4, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
