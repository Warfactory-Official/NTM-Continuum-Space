package com.ntmspace.block.flora;

import dev.golem.core.world.Durability;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * hbm-space's {@code ModEventHandler.onUseHoe}: a hoe used on rubber grass or rubber silt turns it into rubber farmland
 * and wears one point off the hoe. The 1.7 event fired before the hoe's own checks, so it works on any face and with a
 * block above; it made no sound.
 */
public final class RubberSoil {

    private RubberSoil() {}

    public static InteractionResult till(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack,
                                         Supplier<? extends Block> farmland) {
        if (!(stack.getItem() instanceof HoeItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.setBlock(pos, farmland.get().defaultBlockState(), 3);
            Durability.hurt(stack, 1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
