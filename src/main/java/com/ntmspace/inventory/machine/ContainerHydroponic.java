package com.ntmspace.inventory.machine;

import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
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
 * hbm-space's {@code ContainerHydroponic} (a {@code ContainerBase}, 176 x 186): seeds (67, 18), fertiliser (67, 54),
 * battery (147, 54), three take-only outputs at x 111 from y 18, the player inventory from y 104 (hotbar 162).
 */
public class ContainerHydroponic extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final @Nullable TileEntityHydroponic te;

    public ContainerHydroponic(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(pos) instanceof TileEntityHydroponic t ? t : null);
    }

    public ContainerHydroponic(int id, Inventory playerInv, @Nullable TileEntityHydroponic te) {
        super(SpaceMachinesB.HYDROBAY_MENU.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(MACHINE_SLOTS);

        // Inputs
        addSlot(MachineMenus.validated(machine, 0, 67, 18));
        addSlot(MachineMenus.validated(machine, 1, 67, 54));
        // Battery (1.7 plain Slot; the machine's own insert check refuses it, as 1.7's did for automation)
        addSlot(MachineMenus.plain(machine, 2, 147, 54));
        // Outputs
        for (int i = 0; i < 3; i++) addSlot(MachineMenus.takeOnly(machine, i + 3, 111, 18 + i * 18));

        MachineMenus.playerInventory(this::addSlot, playerInv, 104, 162);
    }

    public @Nullable TileEntityHydroponic getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** 1.7 {@code ContainerBase.transferStackInSlot}: machine slots to the player, player slots into any machine slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, stack);
        }
        return result;
    }
}
