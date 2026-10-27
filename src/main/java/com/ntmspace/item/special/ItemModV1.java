package com.ntmspace.item.special;

import com.hbm.items.armor.ItemModCloud;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * hbm-space {@code ItemModV1}: a chestplate module, +50% movement speed (1.7 operation 2, multiply total) and three dashes.
 *
 * <p>The chestplate module supplies blood-fuel healing through the server hurt and death handlers.
 */
public class ItemModV1 extends ItemModCloud {

    public ItemModV1(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        // 1.7 AttributeModifier(speed, "V1 SPEED", 0.5, 2)
        modifiers.add(new Modifier(Modifier.MOVEMENT_SPEED, 0.5D, Attributes.Op.MULTIPLY_TOTAL));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.RED, "BLOOD IS FUEL"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.RED, stack, " (BLOOD IS FUEL)"));
    }

    @Override
    public int getDashes() {
        return 3;
    }
}
