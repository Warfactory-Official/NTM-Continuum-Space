package com.ntmspace.satellite;

import com.hbm.items.machine.ItemSatChip;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * {@code sat_dyson_relay}: HBM's satellite chip with 1.7 {@code ItemSatellite}'s line for it, "Allows a Dyson Receiver to
 * function at night" ({@code item.sat.desc.dyson_relay}).
 */
public class ItemDysonRelay extends ItemSatChip {

    public ItemDysonRelay(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, list, flag);
        list.add(Component.translatable("item.sat.desc.dyson_relay"));
    }
}
