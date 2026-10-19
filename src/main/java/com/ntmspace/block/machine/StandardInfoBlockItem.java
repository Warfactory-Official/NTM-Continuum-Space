package com.ntmspace.block.machine;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * A block item with 1.7 {@code ITooltipProvider.addStandardInfo}'s description: the {@code "$"}-separated lines of
 * {@code block.ntmspace.<id>.desc}, yellow. 1.7 hid them behind a held shift (read on the client); like HBM's
 * {@code DescBlockItem} they are always shown.
 */
public class StandardInfoBlockItem extends CompatBlockItem {

    private final String key;

    public StandardInfoBlockItem(Block block, Properties properties, String id) {
        super(block, properties);
        this.key = "block.ntmspace." + id + ".desc";
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        for (String line : Component.translatable(key).getString().split("\\$")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
