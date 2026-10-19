package com.ntmspace.item.special;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ArmorEnvsuit;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.gear.ArmorFSB;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

/** hbm-space {@code ItemModFlippers}: a boots module that pushes a swimming player along their look vector. */
public class ItemModFlippers extends ItemArmorMod {

    public ItemModFlippers(Item.Properties properties) {
        super(ArmorModHandler.boots_only, false, false, false, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.BLUE, "Increases swim speed"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.DARK_PURPLE, stack, " (increased swim speed)"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof Player player)) return;

        // Skip if we already have armor that applies swim speed buff (1.7: an environment suit worn as a full set)
        if (armor.getItem() instanceof ArmorEnvsuit && ArmorFSB.hasFSBArmor(player)) return;

        if (entity.isInWater()) {
            // 1.7 moveForward is the forward input, modern zza
            double mo = 0.08D * player.zza;
            Vec3 look = entity.getLookAngle();
            entity.setDeltaMovement(entity.getDeltaMovement().add(look.x * mo, look.y * mo, look.z * mo));
        }
    }
}
