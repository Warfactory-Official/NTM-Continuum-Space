package com.ntmspace.rocket;

import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.satellite.SpaceSatellites;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemCustomRocket} ({@code rocket_custom}): an assembled rocket, its design in the
 * {@code ntmspace:rocket} component ({@link RocketStruct.Design}, part ids) and whether the pad filled it in
 * {@code ntmspace:rocket_fuelled}. The satellite frequency is HBM's {@code hbm:sat_frequency} ({@link ISatChip}).
 */
public class ItemCustomRocket extends CompatItem implements ISatChip {

    public ItemCustomRocket(Properties properties) {
        super(properties);
    }

    public static ItemStack build(RocketStruct rocket) {
        ItemStack stack = new ItemStack(RocketItems.ROCKET_CUSTOM.get());
        RocketItems.ROCKET.set(stack, rocket.toDesign());
        if (rocket.satFreq != 0) ISatChip.setFreqS(stack, rocket.satFreq);
        // 1.7 SatelliteBase.ensureItemData: preserve the payload's settings on the assembled rocket.
        if (rocket.satOrbit != null) {
            SpaceSatellites.ORBIT.set(stack, rocket.satOrbit);
        } else if (rocket.capsule != null) {
            var payload = new ItemStack(rocket.capsule.item());
            if (XSatelliteRegistry.typeFor(payload) != null) {
                SpaceSatellites.ORBIT.set(stack, SpaceSatellites.orbit(payload));
            }
        }
        return stack;
    }

    public static ItemStack build(RocketStruct rocket, boolean hasFuel) {
        ItemStack stack = build(rocket);
        setFuel(stack, hasFuel);
        return stack;
    }

    /** The rocket a stack holds, or null for no rocket stack (1.7: or a rocket without NBT). */
    public static @Nullable RocketStruct get(@Nullable ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemCustomRocket)) return null;
        return RocketItems.ROCKET.get(stack).map(design -> {
            var rocket = RocketStruct.fromDesign(design);
            rocket.satOrbit = SpaceSatellites.ORBIT.get(stack).orElse(null);
            return rocket;
        }).orElse(null);
    }

    public static boolean hasFuel(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return RocketItems.ROCKET_FUELLED.getOr(stack, false);
    }

    public static void setFuel(@Nullable ItemStack stack, boolean hasFuel) {
        if (stack == null || stack.isEmpty()) return;
        RocketItems.ROCKET_FUELLED.set(stack, hasFuel);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list, TooltipFlag flag) {
        RocketStruct rocket = get(stack);

        if (rocket == null) return;

        Component payload = rocket.capsule != null ? rocket.capsule.item().getName(rocket.capsule.item().getDefaultInstance())
                : Component.literal("?");
        list.add(Component.literal("Payload: ").withStyle(ChatFormatting.BOLD)
                .append(payload.copy().withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY))));
        list.add(Component.literal("Stages: ").withStyle(ChatFormatting.BOLD)
                .append(Component.literal(String.valueOf(rocket.stages.size())).withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY))));

        if (hasFuel(stack)) {
            list.add(Component.literal("Is fully fueled").withStyle(ChatFormatting.GRAY));
        }

        if (getFreq(stack) != 0) {
            list.add(Component.literal("Satellite Frequency: ").withStyle(ChatFormatting.BOLD)
                    .append(Component.literal(String.valueOf(getFreq(stack))).withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY))));
        }
    }
}
