package com.ntmspace.item.special;

import dev.golem.core.world.Skies;
import net.minecraft.server.level.ServerLevel;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * hbm-space {@code ItemWandTime}: right-click a block to double the celestial time multiplier (sneak: halve it, never
 * below 1). The server publishes its authority to each client's separate celestial rendering rate.
 */
public class ItemWandTime extends CompatItem {

    public ItemWandTime(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Creative-only item"));
        tooltip.add(Component.literal("\"Wibbly wobbly, timey-wimey... stuff\"").withStyle(ChatFormatting.ITALIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (context.getLevel().isClientSide() || player == null) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            SolarSystem.TIME_MULTIPLIER /= 2;
            if (SolarSystem.TIME_MULTIPLIER < 1) SolarSystem.TIME_MULTIPLIER = 1;
        } else if (SolarSystem.TIME_MULTIPLIER <= Long.MAX_VALUE / 2) {
            SolarSystem.TIME_MULTIPLIER *= 2;
        }
        Skies.refreshAfterRateChange(
                ((ServerLevel) context.getLevel()).getServer());

        player.sendSystemMessage(Component.literal("Celestial Time Multiplier set to: " + SolarSystem.TIME_MULTIPLIER));
        return InteractionResult.SUCCESS;
    }
}
