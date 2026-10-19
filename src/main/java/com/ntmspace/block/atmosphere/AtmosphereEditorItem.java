package com.ntmspace.block.atmosphere;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** {@code BlockAtmosphereEditor.addInformation}: its four gold lines, as 1.7 wrote them (not translated). */
public class AtmosphereEditorItem extends CompatBlockItem {

    public AtmosphereEditorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use screwdriver to turn on and off").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Use hand drill to increase/decrease throughput").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Use defuser to switch emission/capture mode").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Use fluid identifier to change fluid").withStyle(ChatFormatting.GOLD));
    }
}
