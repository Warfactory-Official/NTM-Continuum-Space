package com.ntmspace.atmosphere;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.ntmspace.trait.CelestialTraits;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Gas-only identifiers shared by the real vent and emitter activation paths. */
public final class AtmosphereText {
    private AtmosphereText() {}

    public static void retypeGaseous(ItemStack stack, Player player, FluidTankNTM tank, BlockEntity machine) {
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return;
        var pos = machine.getBlockPos();
        retypeGaseous(identifier.getType(machine.getLevel(), pos.getX(), pos.getY(), pos.getZ(), stack), player, tank, machine);
    }

    /** The identifier selects an exact resource, retaining the tank's normal identifier-pinning behavior. */
    public static void retypeGaseous(FluidType type, Player player, FluidTankNTM tank, BlockEntity machine) {
        if (!CelestialTraits.isGas(type)) return;
        tank.setTankTypeByIdentifier(type);
        machine.setChanged();
        player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                .append(type.getDisplayName()).append("!"));
    }
}
