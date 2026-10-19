package com.ntmspace.inventory.rocket;

import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
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
 * hbm-space's {@code ContainerLaunchPadRocket} (188 x 236): rocket (37, 21), drive (37, 39), battery (167, 90), fuel
 * in (77, 21) and out (77, 39); player inventory at (14, 154), hotbar (14, 212). The first three are 1.7
 * {@code SlotNonRetarded} (they ask the pad what fits), the fuel slots take anything.
 */
public class ContainerLaunchPadRocket extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityLaunchPadRocket pad;

    public ContainerLaunchPadRocket(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityLaunchPadRocket te ? te : null);
    }

    public ContainerLaunchPadRocket(int containerId, Inventory playerInventory, @Nullable TileEntityLaunchPadRocket pad) {
        super(RocketBlocks.LAUNCH_PAD_ROCKET_MENU.get(), containerId);
        this.pad = pad;
        this.machine = pad != null ? pad : new SimpleContainer(5);

        addSlot(new Slot(machine, 0, 37, 21) { // Rocket slot
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(0, stack); }
        });
        addSlot(new Slot(machine, 1, 37, 39) { // Drive slot
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(1, stack); }
        });
        addSlot(new Slot(machine, 2, 167, 90) { // Battery slot
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(2, stack); }
        });
        addSlot(new Slot(machine, 3, 77, 21)); // Input
        addSlot(new Slot(machine, 4, 77, 39)); // Output

        Compat.addPlayerInventory(this::addSlot, playerInventory, 14, 154);
    }

    public @Nullable TileEntityLaunchPadRocket getPad() {
        return pad;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** 1.7 {@code ContainerBase.transferStackInSlot}: machine to player, player into the first slot that takes it. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 5) {
                if (!moveItemStackTo(stack, 5, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 5, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
