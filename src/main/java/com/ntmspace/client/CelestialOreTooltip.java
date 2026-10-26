package com.ntmspace.client;

import dev.golem.core.client.Tooltips;
import com.ntmspace.block.BodyOre;
import com.ntmspace.block.CelestialOre;
import com.ntmspace.config.SpaceConfig;
import com.ntmspace.rocket.DriveBody;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The legacy BlockOre location tooltip, including the vanilla ore and coal event hooks. */
public final class CelestialOreTooltip {
    private static Map<Item, CelestialOre> ores;

    private CelestialOreTooltip() {}

    public static void init() {
        Tooltips.onItemTooltip(CelestialOreTooltip::append);
    }

    public static void append(ItemStack stack, List<Component> lines) {
        if (!SpaceConfig.showOreLocations) return;
        if (stack.is(Items.COAL_ORE)) {
            lines.add(Component.translatable("desc.ntmspace.ore.foundOn").withStyle(ChatFormatting.GOLD));
            lines.add(body(DriveBody.KERBIN, ChatFormatting.AQUA));
            return;
        }
        var ore = ores().get(stack.getItem());
        if (ore == null) return;
        var on = ore.spawnsOn();
        var bodies = DriveBody.values().length;
        if (on.isEmpty()) return;
        if (on.size() == bodies) {
            lines.add(Component.translatable("desc.ntmspace.ore.anywhere").withStyle(ChatFormatting.GOLD));
        } else if (on.size() == bodies - 1) {
            lines.add(Component.translatable("desc.ntmspace.ore.anywhereExcept").withStyle(ChatFormatting.GOLD));
            for (var body : DriveBody.values()) if (!on.contains(body)) lines.add(body(body, ChatFormatting.RED));
        } else {
            lines.add(Component.translatable("desc.ntmspace.ore.foundOn").withStyle(ChatFormatting.GOLD));
            for (var body : on) lines.add(body(body, ChatFormatting.AQUA));
        }
    }

    private static Component body(DriveBody body, ChatFormatting colour) {
        return Component.translatable("desc.ntmspace.ore.body", Component.translatable("body." + body.name)).withStyle(colour);
    }

    private static Map<Item, CelestialOre> ores() {
        if (ores != null) return ores;
        var map = new HashMap<Item, CelestialOre>();
        for (var ore : CelestialOre.values()) {
            map.put(BodyOre.item(ore.host()), ore);
            for (var body : ore.bodies()) map.put(BodyOre.item(ore.state(body)), ore);
        }
        map.put(Items.NETHER_QUARTZ_ORE, CelestialOre.QUARTZ);
        ores = map;
        return map;
    }
}
