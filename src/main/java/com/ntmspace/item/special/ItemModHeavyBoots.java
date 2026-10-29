package com.ntmspace.item.special;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ItemArmorMod;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Wearable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * hbm-space {@code ItemModHeavyBoots}: a boots module that, while the wearer crouches in mid-air, pulls them down until
 * their fall matches the overworld (bodies with gravity up to 1.5, counted as at least 0.2).
 *
 * <p>1.7's {@code isValidArmor} lets it be worn on its own as boots; {@code HeavyBootsRenderLayer} draws it.
 */
public class ItemModHeavyBoots extends ItemArmorMod implements Wearable {

    public ItemModHeavyBoots(Item.Properties properties) {
        super(ArmorModHandler.boots_only, false, false, false, true, properties);
    }

    @Override
    public ArmorSlot wornSlot() {
        return ArmorSlot.BOOTS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return wear(level, player, hand);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.BLUE, "Increases fall speed in low gravity"));
        tooltip.add(coloured(ChatFormatting.BLUE, "Activated by crouching"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(coloured(ChatFormatting.GOLD, "Can be worn on its own!"));
        tooltip.add(Component.literal("We take no responsibility for any deaths that may")
                             .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("occur while using these boots without a space suit.")
                             .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(Component.literal("  ").append(stack.getHoverName()).append(" (")
                             .append(Component.translatable("armor.fastFall")).append(")")
                             .withStyle(ChatFormatting.DARK_PURPLE));
    }

    /**
     * Worn or socketed boots restore standard pull for a crouching airborne wearer in low gravity.
     */
    public static boolean pulls(Player player) {
        if (!player.isShiftKeyDown() || player.onGround()) return false;
        var feet = player.getItemBySlot(EquipmentSlot.FEET);
        return feet.getItem() instanceof ItemModHeavyBoots
                || ArmorModHandler.modItem(feet, ArmorModHandler.boots_only) instanceof ItemModHeavyBoots;
    }
}
