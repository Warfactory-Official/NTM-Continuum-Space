package com.ntmspace.item;

import com.hbm.capability.HbmLivingProps;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.Effects;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * hbm-space's three new {@code ItemEnergy().makeGlass()} drinks (no bottle opener needed): strawberry milkshake
 * ({@code glass_smilk}), tea ({@code teacup}) and honey ({@code bottle_honey}). Values are hbm-space's
 * {@code ItemEnergy.onEaten} branches; like every {@code ItemEnergy} they are drunk in 32 ticks, refused while potion
 * sickness lasts, apply 5 s of potion sickness ({@code VersatileConfig.applyPotionSickness}, off at the default
 * config) and hand back their container.
 *
 * <p>1.7 assigned the container by mutating the shared item ({@code setContainerItem} inside {@code onEaten}); the
 * container is fixed per item here, which is what that mutation always ended up as.
 */
public final class SpaceDrinkItem extends CompatItem {

    public enum Kind { STRAWBERRY_MILKSHAKE, TEA, HONEY }

    private final Kind kind;
    private final Supplier<? extends Item> container;

    public SpaceDrinkItem(Item.Properties properties, Kind kind, Supplier<? extends Item> container) {
        super(properties);
        this.kind = kind;
        this.container = container;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.DRINK;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (com.hbm.config.VersatileConfig.hasPotionSickness(player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel) || !(entity instanceof Player player)) return stack;
        com.hbm.config.VersatileConfig.applyPotionSickness(player, 5);
        switch (kind) {
            case STRAWBERRY_MILKSHAKE -> player.heal(6F); // 1.7: "ideas welcome pls thanks"
            case TEA -> {
                player.heal(3F);
                Effects.add(player, Effects.resistance(), 30 * 20, 4);
            }
            case HONEY -> {
                player.heal(9F);
                HbmLivingProps.setDigamma(player, Math.max(HbmLivingProps.getDigamma(player) - 0.3F, 0F));
            }
        }
        if (player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        ItemStack empty = container.get().getDefaultInstance();
        if (stack.isEmpty()) return empty;
        if (!player.getInventory().add(empty)) player.drop(empty, false);
        return stack;
    }

    /** For tests. */
    public Kind kind() {
        return kind;
    }
}
