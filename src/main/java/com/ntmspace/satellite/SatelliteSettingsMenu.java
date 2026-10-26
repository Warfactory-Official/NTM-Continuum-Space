package com.ntmspace.satellite;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** A held satellite's open settings screen, bound to the exact stack and hand that opened it. */
public final class SatelliteSettingsMenu extends AbstractContainerMenu {

    public final InteractionHand hand;
    private final Player owner;
    private final ItemStack stack;

    public SatelliteSettingsMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(SatelliteSettings.MENU.get(), containerId);
        this.hand = hand;
        owner = inventory.player;
        stack = owner.getItemInHand(hand);
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.getItemInHand(hand) == stack && !stack.isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
