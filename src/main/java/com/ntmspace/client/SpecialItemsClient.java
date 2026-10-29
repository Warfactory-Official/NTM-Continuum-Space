package com.ntmspace.client;

import dev.golem.core.client.Tooltips;
import com.ntmspace.atmosphere.AtmosphereItemLore;
import com.ntmspace.client.render.HeavyBootsRenderLayer;
import dev.golem.core.render.HumanoidLayers;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Special items' client hooks: atmosphere lines below the item name and the worn heavy boots layer. */
public final class SpecialItemsClient {

    private SpecialItemsClient() {}

    public static void init() {
        Tooltips.onItemTooltip(SpecialItemsClient::atmosphere);
        HumanoidLayers.register(HeavyBootsRenderLayer::new);
    }

    private static void atmosphere(ItemStack stack, List<Component> lines) {
        AtmosphereItemLore.armor(stack, line -> lines.add(Math.min(1, lines.size()), line));
        AtmosphereItemLore.insert(stack, line -> lines.add(Math.min(1, lines.size()), line));
    }
}
