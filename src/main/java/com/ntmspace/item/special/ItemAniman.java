package com.ntmspace.item.special;

import com.hbm.particle.helper.LivingEffects;
import com.hbm.sound.ModSounds;
import com.ntmspace.registry.SpaceSpecialItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code animan} ("Problematic Meal"), an {@code ItemPill(0)}: always edible, eaten in 10 ticks, refused under
 * potion sickness. Eating it applies potion sickness like every pill, plays {@code player.oil}, gives "GOD WATCHES YOU"
 * ({@code ntmspace:run}) for 10 seconds, makes the player vomit 25 particles, plays the vomit sound and gives Hunger XX
 * for 3 seconds. HBM's {@code ItemPill} is a closed switch over HBM's pills, so this is its own class with the same
 * shape.
 */
public class ItemAniman extends CompatItem {

    public ItemAniman(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (com.hbm.config.VersatileConfig.hasPotionSickness(player)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return compatVanillaUse(level, player, hand);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) apply(player, level);
        return result;
    }

    private static void apply(Player player, Level level) {
        com.hbm.config.VersatileConfig.applyPotionSickness(player, 5);

        BlockPos pos = new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()));
        level.playSound(null, pos, SpaceSpecialItems.PLAYER_OIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        Effects.addRegistered(player, SpaceSpecialItems.RUN.get(), 10 * 20, 0);

        LivingEffects.vomit(player, "normal", 25);

        level.playSound(null, pos, ModSounds.VOMIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        Effects.add(player, MobEffects.HUNGER, 60, 19);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        // 1.7 ItemPill: the ".desc" lang line, split at '$'
        tooltip.add(Component.translatable(getDescriptionId() + ".desc"));
    }
}
