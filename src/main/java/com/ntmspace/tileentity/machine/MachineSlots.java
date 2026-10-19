package com.ntmspace.tileentity.machine;

import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.7 {@code FluidTank} slot helpers the progression machines share (the container moves are HBM's {@code MachineFluidContainerIO}). */
public final class MachineSlots {

    private MachineSlots() {}

    /**
     * 1.7 {@code FluidTank.setType(slot, slots)}: a fluid identifier in {@code slot} sets the tank's type; a different
     * type empties the tank ({@code setTankType}). Returns whether the type changed.
     */
    public static boolean setTypeFromId(FluidTankNTM tank, NTMItemHandler inventory, int slot, Level level, BlockPos pos) {
        ItemStack id = inventory.getStackInSlot(slot);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return false;
        FluidType type = ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), id);
        if (type == tank.getTankType()) return false;
        tank.setTankType(type);
        return true;
    }
}
