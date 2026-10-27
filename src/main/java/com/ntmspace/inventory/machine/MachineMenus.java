package com.ntmspace.inventory.machine;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot kinds the progression machines' menus share (1.7 {@code Slot}, {@code SlotTakeOnly}, the player inventory rows). */
public final class MachineMenus {

    private MachineMenus() {}

    /** A machine slot that takes what the machine's {@code isItemValidForSlot} allows (a 1.7 plain {@code Slot}, with the
     * insert check 1.7 made in {@code ISidedInventory}; the port's menus use it for clicks too, as HBM's own do). */
    public static Slot validated(Container machine, int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(index, stack);
            }
        };
    }

    /** A plain 1.7 {@code Slot}: anything goes. */
    public static Slot plain(Container machine, int index, int x, int y) {
        return new Slot(machine, index, x, y);
    }

    /** 1.7 {@code SlotTakeOnly}. */
    public static Slot takeOnly(Container machine, int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    /** Finishes a successful shift-click, including the output slot's crafted-item and achievement callbacks. */
    public static ItemStack quickMove(Player player, Slot slot, Predicate<ItemStack> route) {
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var before = stack.copy();
        if (!route.test(stack)) return ItemStack.EMPTY;
        slot.onQuickCraft(stack, before);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return before;
    }

    /** The 27 inventory slots from {@code top} and the hotbar at {@code hotbarY}, x from 8 (1.7's loops). */
    public static void playerInventory(Consumer<Slot> add, Inventory inventory, int top, int hotbarY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) add.accept(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, top + i * 18));
        }
        for (int i = 0; i < 9; i++) add.accept(new Slot(inventory, i, 8 + i * 18, hotbarY));
    }
}
