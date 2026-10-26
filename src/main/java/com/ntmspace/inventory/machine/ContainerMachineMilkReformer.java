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

    public long getPower() {
        return te != null ? te.power : 0L;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** Routes against the nine visible machine slots; batteries enter slot zero and canisters each input in turn. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return MachineMenus.quickMove(player, slots.get(index), stack -> {
            if (index < TileEntityMachineMilkReformer.SLOT_COUNT) {
                return moveItemStackTo(stack, TileEntityMachineMilkReformer.SLOT_COUNT, slots.size(), true);
            }
            if (Library.isBattery(stack)) return moveItemStackTo(stack, TileEntityMachineMilkReformer.SLOT_BATTERY,
                    TileEntityMachineMilkReformer.SLOT_BATTERY + 1, false);
            for (var input = TileEntityMachineMilkReformer.SLOT_INPUT_IN; input < TileEntityMachineMilkReformer.SLOT_COUNT; input += 2)
                if (moveItemStackTo(stack, input, input + 1, false)) return true;
            return false;
        });
    }
}
