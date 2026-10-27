package com.ntmspace.client;

import dev.golem.core.client.Tooltips;
import com.ntmspace.atmosphere.AtmosphereItemLore;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Atmosphere capability lines just below the item name, shared with the common capability formatters. */
public final class SpecialItemsClient {

    private SpecialItemsClient() {}

    public static void init() {
        Tooltips.onItemTooltip(SpecialItemsClient::atmosphere);
    }

    private static void atmosphere(ItemStack stack, List<Component> lines) {
        AtmosphereItemLore.armor(stack, line -> lines.add(Math.min(1, lines.size()), line));
        AtmosphereItemLore.insert(stack, line -> lines.add(Math.min(1, lines.size()), line));
    }
}
