package com.ntmspace.item;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * A machine's block item with 1.7 {@code ITooltipProvider.addStandardInfo}: the {@code <block description id>.desc}
 * text split at {@code $}, yellow. 1.7 showed it while shift was held; like HBM's {@code DescBlockItem} it is always
 * shown here.
 */
public class MachineDescItem extends CompatBlockItem {

    public MachineDescItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = getBlock().getDescriptionId() + ".desc";
        for (String line : Component.translatable(key).getString().split("\\$")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
