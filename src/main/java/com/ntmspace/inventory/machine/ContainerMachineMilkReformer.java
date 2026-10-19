package com.ntmspace.inventory.machine;

import com.hbm.lib.Library;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** hbm-space's {@code ContainerMachineMilkReformer} (176 x 238): battery (79, 8), input pair at x 45, output pairs at x 95/122/149. */
public class ContainerMachineMilkReformer extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineMilkReformer te;

    public ContainerMachineMilkReformer(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(pos) instanceof TileEntityMachineMilkReformer t ? t : null);
    }

    public ContainerMachineMilkReformer(int id, Inventory playerInv, @Nullable TileEntityMachineMilkReformer te) {
        super(SpaceMachines.MILK_REFORMER_MENU.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(11);

        addSlot(MachineMenus.validated(machine, 0, 79, 8));   // battery
        addSlot(MachineMenus.validated(machine, 1, 45, 88));  // canister input
        addSlot(MachineMenus.takeOnly(machine, 2, 45, 106));
        addSlot(MachineMenus.validated(machine, 3, 95, 88));
        addSlot(MachineMenus.takeOnly(machine, 4, 95, 106));
        addSlot(MachineMenus.validated(machine, 5, 122, 88));
        addSlot(MachineMenus.takeOnly(machine, 6, 122, 106));
        addSlot(MachineMenus.validated(machine, 7, 149, 88));
        addSlot(MachineMenus.takeOnly(machine, 8, 149, 106));

        MachineMenus.playerInventory(this::addSlot, playerInv, 156, 214);
    }

    public @Nullable TileEntityMachineMilkReformer getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /**
     * 1.7 {@code transferStackInSlot}, kept as it was: indices up to 10 count as machine slots (the first two player slots
     * too) and go to index 11 and up; a battery tries the empty range 1..0, so it never moves.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 10) {
                if (!moveItemStackTo(stack, 11, slots.size(), true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(result)) {
                return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 1, 2, false) && !moveItemStackTo(stack, 3, 4, false)
                    && !moveItemStackTo(stack, 5, 6, false) && !moveItemStackTo(stack, 7, 8, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
