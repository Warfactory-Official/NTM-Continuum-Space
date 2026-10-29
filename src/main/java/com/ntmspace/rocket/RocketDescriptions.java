package com.ntmspace.rocket;

import static com.hbm.items.weapon.ItemCustomMissilePart.stat;

import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

/** Space 1.7's replacement missile descriptions: rocket masses, thrust and ISP rather than part health. */
public final class RocketDescriptions {
    private RocketDescriptions() {
    }

    public static void init() {
        ItemCustomMissilePart.DESCRIBE.register(RocketDescriptions::part);
        ItemCustomMissile.DESCRIBE.register(RocketDescriptions::missile);
    }

    private static boolean part(ItemCustomMissilePart part, Consumer<Component> output) {
        if (BuiltInRegistries.ITEM.getKey(part).toString().equals(RocketParts.POD)) return true;
        if (part.title() != null)
            output.accept(Component.literal("\"" + part.title() + "\"").withStyle(ChatFormatting.DARK_PURPLE));
        var rocket = RocketParts.get(part);
        switch (part.type) {
            case CHIP -> output.accept(stat("inaccuracy", Component.literal(part.inaccuracy() * 100 + "%")));
            case WARHEAD -> {
                output.accept(stat("size", part.bottom.getDisplay()));
                output.accept(stat("type", part.warheadType().getDisplay()));
                if (!rocket.isApollo() && !rocket.isSatellite())
                    output.accept(stat("strength", Component.literal(String.valueOf(part.warheadStrength()))));
                output.accept(mass(part));
            }
            case FUSELAGE -> {
                output.accept(stat("topSize", part.top.getDisplay()));
                output.accept(stat("bottomSize", part.bottom.getDisplay()));
                output.accept(stat("fuelType", fuelName(part.fuelType())));
                output.accept(stat("fuelAmount", Component.literal((int) part.fuelAmount() + "mB")));
                output.accept(mass(part));
            }
            case FINS -> {
                output.accept(stat("size", part.top.getDisplay()));
                output.accept(stat("inaccuracy", Component.literal(part.inaccuracy() * 100 + "%")));
            }
            case THRUSTER -> {
                output.accept(stat("size", part.top.getDisplay()));
                output.accept(stat("fuelType", fuelName(part.fuelType())));
                output.accept(stat("fuelConsumption", Component.literal(part.consumption() + "l/tick")));
                output.accept(stat("maxPayload", Component.literal((Float) part.attributes[2] * 1000 + "kg")));
                output.accept(stat("thrust", Component.literal(rocket.thrust() + "N")));
                output.accept(stat("isp", Component.literal(rocket.isp() + "s")));
                output.accept(mass(part));
            }
        }
        if (part.rarity != null) output.accept(stat("rarity", part.rarity.getDisplay()));
        // 1.7 indented the byline and the witty line with three spaces
        if (part.author() != null) output.accept(Component.literal("   ").append(Component.translatable("item.missile.part.by"))
                .append(" " + part.author()).withStyle(ChatFormatting.WHITE));
        if (part.witty() != null) output.accept(Component.literal("   \"" + part.witty() + "\"")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        return true;
    }

    private static Component mass(ItemCustomMissilePart part) {
        return stat("mass", Component.literal(RocketParts.get(part).mass() + "kg"));
    }

    private static boolean missile(MissileStruct parts, ItemCustomMissilePart chip, Consumer<Component> output) {
        var warhead = parts.warhead();
        var fuselage = parts.fuselage();
        output.accept(missileStat("warhead", warhead.warheadType().getDisplay()));
        output.accept(missileStat("strength", Component.literal(String.valueOf(warhead.warheadStrength()))));
        output.accept(missileStat("fuelType", fuelName(fuselage.fuelType())));
        output.accept(missileStat("fuelAmount", Component.literal((int) fuselage.fuelAmount() + "l")));
        output.accept(missileStat("chipInaccuracy", Component.literal(chip.inaccuracy() * 100 + "%")));
        output.accept(missileStat("finInaccuracy", Component.literal(parts.fins() != null
                ? parts.fins().inaccuracy() * 100 + "%" : "100%")));
        output.accept(
                missileStat("size", fuselage.top.getDisplay().copy().append("/").append(fuselage.bottom.getDisplay())));
        float health = warhead.health + fuselage.health + parts.thruster().health;
        if (parts.fins() != null) health += parts.fins().health;
        output.accept(missileStat("health", Component.literal(health + "HP")));
        return true;
    }

    private static Component fuelName(FuelType fuel) {
        if (fuel != FuelType.KEROSENE && fuel != FuelType.SOLID && fuel != FuelType.HYDROGEN
                && fuel != FuelType.XENON && fuel != FuelType.BALEFIRE) return fuel.getDisplay();
        return Component.translatable("item.custom_missile_part.fuel." + fuel.name().toLowerCase(Locale.ROOT))
                        .withStyle(fuel == FuelType.KEROSENE ? ChatFormatting.LIGHT_PURPLE
                                : fuel == FuelType.SOLID ? ChatFormatting.GOLD : fuel == FuelType.HYDROGEN
                                ? ChatFormatting.DARK_AQUA : fuel == FuelType.XENON ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN);
    }

    private static Component missileStat(String key, Component value) {
        return ItemCustomMissilePart.stat("item.missile.desc." + key, value);
    }
}
