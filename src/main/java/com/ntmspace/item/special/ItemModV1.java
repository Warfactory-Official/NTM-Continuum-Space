package com.ntmspace.item.special;

import com.hbm.handler.ArmorModHandler;
import com.hbm.interfaces.IArmorModDash;
import com.hbm.items.armor.ItemArmorMod;
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
 * <p>"BLOOD IS FUEL": hbm-space's {@code EntityDamageUtil.wasAttackedByV1} (heal on hit and on kill, giblets) looks for
 * the V1 in the chestplate's {@code extra} module slot, but the V1 is a {@code plate_only} module and only ever sits in
 * that slot, so the check never succeeded in 1.7. That dead path is not ported; the tooltip line is.
 */
public class ItemModV1 extends ItemArmorMod implements IArmorModDash {

    public ItemModV1(Item.Properties properties) {
        super(ArmorModHandler.plate_only, false, true, false, false, properties);
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
