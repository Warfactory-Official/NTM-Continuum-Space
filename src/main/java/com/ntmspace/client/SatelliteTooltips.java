package com.ntmspace.client;

import com.hbm.items.ISatChip;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.rocket.RocketFuels;
import com.ntmspace.satellite.SpaceSatellites;
import dev.golem.core.client.Tooltips;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The launchable satellite's warhead information, editable orbit and hand-deployment prompts.
 */
public final class SatelliteTooltips {
    private SatelliteTooltips() {
    }

    public static void init() {
        Tooltips.onItemTooltip(SatelliteTooltips::append);
    }

    private static void append(ItemStack stack, List<Component> lines) {
        var type = XSatelliteRegistry.typeFor(stack);
        if (type == null || !SpaceSatellites.isSpace(type)) return;
        var client = Minecraft.getInstance();
        int mass = SpaceSatellites.launchMass(stack);
        var frequency = Language.getInstance().getOrDefault("satchip.frequency") + ": " + ISatChip.getFreqS(stack);
        lines.removeIf(line -> line.getString().equals(frequency));
        if (!(stack.getItem() instanceof ItemCustomMissilePart)) {
            lines.add(ItemCustomMissilePart.stat("size", ItemCustomMissilePart.PartSize.SIZE_20.getDisplay()));
            lines.add(ItemCustomMissilePart.stat("type", RocketFuels.SATELLITE.getDisplay()));
            lines.add(ItemCustomMissilePart.stat("mass", Component.literal(mass + "kg")));
        }
        var orbit = SpaceSatellites.orbit(stack);
        if (client.options.keyShift.isDown()) {
            stat(lines, "frequency", ISatChip.getFreqS(stack));
            stat(lines, "owner", orbit.owner());
            stat(lines, "speed", Math.round(orbit.orbitSpeedKmPerSecond() * 10F) / 10F + "km/s");
            stat(lines, "altitude", number(orbit.altitude()) + "km");
            stat(lines, "inclination", number(orbit.inclination()) + "°");
            stat(lines, "phase", number(Math.round(orbit.phaseOffset() * 10F) / 10F) + "°");
            stat(lines, "color", String.format(Locale.ROOT, "#%02X%02X%02X", Math.round(orbit.colorR() * 255F),
                    Math.round(orbit.colorG() * 255F), Math.round(orbit.colorB() * 255F)));
        } else {
            lines.add(Component.translatable("desc.tooltip.hold", "LSHIFT")
                               .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        switch (type.name()) {
            case "horizons" -> {
                for (var kind : List.of("single_use", "orbital_module", "melter"))
                    lines.add(Component.translatable("item.ntmspace.sat.desc.gerald." + kind));
            }
            case "relay" -> lines.add(Component.translatable("item.ntmspace.sat.desc.foeq"));
            case "ntmspace:railgun" -> lines.add(Component.translatable("item.ntmspace.sat.desc.war"));
            case "precision_laser", "detector", "ray_scan" -> {
            }
            default ->
                    lines.add(Component.translatable("item.ntmspace.sat.desc." + type.name().replace("ntmspace:", "")));
        }
        if (mass <= 16_000) {
            lines.add(Component.translatable("item.ntmspace.sat.desc.launch_by_hand").withStyle(ChatFormatting.GOLD));
            // Ingredient and menu tooltips can render without a world.
            if (client.level != null && Orbit.inOrbit(client.level))
                lines.add(Component.translatable("item.ntmspace.sat.desc.deploy_orbit").withStyle(ChatFormatting.BOLD));
        }
    }

    private static String number(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }

    private static void stat(List<Component> lines, String key, Object value) {
        lines.add(Component.translatable("item.ntmspace.sat.desc." + key).withStyle(ChatFormatting.YELLOW)
                           .append(": ").append(Component.literal(value.toString()).withStyle(ChatFormatting.GOLD)));
    }
}
