package com.ntmspace.fork;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.Effects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * hbm-space {@code ItemFlask} metadata {@code NITAN} ("NITAN&copy; Ambrosia"), split as {@code flask_infusion_nitan}
 * (HBM splits the SHIELD flask as {@code flask_infusion_shield}). Drunk in 32 ticks like every flask, with no use
 * condition (1.7 only refused the SHIELD flask at the shield cap); drinking adds 1 to the NITAN count and gives
 * Ambrosia and Resistance IV for 3 minutes each.
 */
public class ItemFlaskNitan extends CompatItem {

    public ItemFlaskNitan(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (level.isClientSide()) return stack;
        Nitan.addNitanDose(player);
        Effects.addRegistered(player, ForkContent.NITAN_EFFECT.get(), 60 * 20 * 3, 0);
        Effects.add(player, Effects.resistance(), 60 * 20 * 3, 3);
        return stack;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.DRINK;
    }
}
