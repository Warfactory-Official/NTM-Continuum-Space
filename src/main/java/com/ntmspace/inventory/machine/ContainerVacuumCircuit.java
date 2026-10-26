package com.ntmspace.inventory.machine;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.lib.Library;
import com.ntmspace.recipe.VacuumCircuitRecipes;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
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
 * hbm-space's {@code ContainerVacuumCircuit} (176 x 204): wafers 0-1 and boards 2-3 in a 2 x 2 at (26, 18)
 * ({@code SlotNonRetarded}), the output (98, 27) ({@code SlotCraftingOutput}), the battery (143, 72), the upgrades
 * (89, 72) and (107, 72) ({@code SlotUpgrade}), the player inventory from y 122 (hotbar 180).
 */
public class ContainerVacuumCircuit extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineVacuumCircuit te;

    public ContainerVacuumCircuit(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(pos) instanceof TileEntityMachineVacuumCircuit t ? t : null);
    }

    public ContainerVacuumCircuit(int id, Inventory playerInv, @Nullable TileEntityMachineVacuumCircuit te) {
        super(SpaceMachinesB.VACUUM_CIRCUIT_MENU.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(8);

        // Inputs
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) addSlot(MachineMenus.validated(machine, i * 2 + j, 26 + j * 18, 18 + i * 18));
        // Output
        addSlot(new SlotCraftingOutput(playerInv.player, machine, 4, 98, 27));
        // Battery
        addSlot(MachineMenus.plain(machine, 5, 143, 72));
        // Upgrades
        addSlot(upgrade(machine, 6, 89, 72));
        addSlot(upgrade(machine, 7, 107, 72));

        MachineMenus.playerInventory(this::addSlot, playerInv, 122, 180);
    }

    /** 1.7 {@code SlotUpgrade}: machine upgrades only, one at a time. */
    private static Slot upgrade(Container machine, int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return Library.isMachineUpgrade(stack); }
            @Override public int getMaxStackSize() { return 1; }
        };
    }

    public @Nullable TileEntityMachineVacuumCircuit getTile() {
        return te;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** Batteries, upgrades, wafers and boards enter their own machine slots; outputs enter the player inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return MachineMenus.quickMove(player, slots.get(index), stack -> {
            if (index < TileEntityMachineVacuumCircuit.SLOT_COUNT)
                return moveItemStackTo(stack, TileEntityMachineVacuumCircuit.SLOT_COUNT, slots.size(), true);
            if (Library.isBattery(stack)) return moveItemStackTo(stack, TileEntityMachineVacuumCircuit.SLOT_BATTERY,
                    TileEntityMachineVacuumCircuit.SLOT_BATTERY + 1, false);
            if (Library.isMachineUpgrade(stack)) return moveItemStackTo(stack, TileEntityMachineVacuumCircuit.SLOT_UPGRADE_START,
                    TileEntityMachineVacuumCircuit.SLOT_UPGRADE_END + 1, false);
            return VacuumCircuitRecipes.INSTANCE.isWafer(stack) && moveItemStackTo(stack, 0, 2, false)
                    || VacuumCircuitRecipes.INSTANCE.isPcb(stack) && moveItemStackTo(stack, 2, 4, false);
        });
    }
}
