package com.ntmspace.inventory.machine;

import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
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
 * hbm-space's {@code ContainerMachineCryoDistill} (176 x 238): battery (26, 90), fluid identifier (26, 108), and four
 * unload pairs at x 80/98/116/134. The machine's slot 10 has no slot, as in 1.7.
 */
public class ContainerMachineCryoDistill extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineCryoDistill te;

    public ContainerMachineCryoDistill(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(pos) instanceof TileEntityMachineCryoDistill t ? t : null);
    }

    public ContainerMachineCryoDistill(int id, Inventory playerInv, @Nullable TileEntityMachineCryoDistill te) {
        super(SpaceMachines.CRYO_DISTILL_MENU.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(11);

        addSlot(MachineMenus.validated(machine, 0, 26, 90));
        addSlot(MachineMenus.validated(machine, 1, 80, 90));
        addSlot(MachineMenus.takeOnly(machine, 2, 80, 108));
        addSlot(MachineMenus.validated(machine, 3, 98, 90));
        addSlot(MachineMenus.takeOnly(machine, 4, 98, 108));
        addSlot(MachineMenus.validated(machine, 5, 116, 90));
        addSlot(MachineMenus.takeOnly(machine, 6, 116, 108));
        addSlot(MachineMenus.validated(machine, 7, 26, 108));
        addSlot(MachineMenus.validated(machine, 8, 134, 90));
        addSlot(MachineMenus.takeOnly(machine, 9, 134, 108));

        MachineMenus.playerInventory(this::addSlot, playerInv, 156, 214);
    }

    public @Nullable TileEntityMachineCryoDistill getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /**
     * 1.7 {@code transferStackInSlot}, kept as it was: machine slots go to indices 8 and up (so also the last two machine
     * slots), and a fluid identifier goes to index 11, which is a player slot, not the identifier slot.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 9) {
                if (!moveItemStackTo(stack, 8, slots.size(), true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(result)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (result.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 11, 12, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 1, 2, false) && !moveItemStackTo(stack, 3, 4, false)
                    && !moveItemStackTo(stack, 5, 6, false) && !moveItemStackTo(stack, 7, 8, false)
                    && !moveItemStackTo(stack, 9, 10, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
