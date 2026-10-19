package com.ntmspace.client;

import com.hbm.items.armor.ItemModInsert;
import com.ntmspace.atmosphere.Sealing;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Client half of {@code registry/SpaceSpecialItems}: hbm-space's {@code ItemModInsert.withCorrosionProtection()} tooltip
 * line. HBM's {@code ItemModInsert} is final and knows nothing of corrosion, so the gold line is added for every insert in
 * {@code #ntmspace:corrosion_protection} ({@code insert_cmb}, {@code insert_yharonite}) as 1.7's first line, right under
 * the name.
 */
public final class SpecialItemsClient {

    private SpecialItemsClient() {}

    public static void init() {
        dev.golem.core.client.Tooltips.onItemTooltip(SpecialItemsClient::corrosion);
    }

    private static void corrosion(ItemStack stack, List<Component> lines) {
        if (!(stack.getItem() instanceof ItemModInsert) || !stack.is(Sealing.CORROSION_PROTECTION)) return;
        Component line = Component.literal("Corrosive atmosphere protection").withStyle(ChatFormatting.GOLD);
        lines.add(Math.min(1, lines.size()), line);
    }
}
