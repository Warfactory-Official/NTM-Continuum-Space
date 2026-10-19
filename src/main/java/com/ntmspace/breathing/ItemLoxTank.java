package com.ntmspace.breathing;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code lox_tank} ({@code ItemSyringe} branch, "Oxygen Tank"): used, it fills the PLSS in the worn helmet
 * with 1000 mB of oxygen and is used up if any of it went in.
 */
public class ItemLoxTank extends CompatItem {

    public ItemLoxTank(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && ArmorModHandler.hasMods(helmet)) {
            ItemStack tankStack = ArmorModHandler.pryMod(helmet, ArmorModHandler.plate_only);
            if (tankStack.isEmpty() || !(tankStack.getItem() instanceof IFillableItem fillable)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!fillable.acceptsFluid(Fluids.OXYGEN, tankStack)) return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (fillable.tryFill(Fluids.OXYGEN, 1000, tankStack) < 1000) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.JETPACK_TANK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            ArmorModHandler.applyMod(helmet, tankStack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Fills a worn PLSS with 1000mB of oxygen"));
    }
}
