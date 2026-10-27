package com.ntmspace.block.flora;

import net.minecraft.network.chat.Component;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.FloraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's seed foods ({@code ItemSeedFood}: strawberry, mint leaves, raw coffee bean, tea seeds, chlorinated seeds):
 * food (their properties carry the 1.7 hunger and saturation) that plants its crop when used on the top of farmland or
 * rubber farmland with air above (1.7: the ground's {@code canSustainPlant} for a {@code Crop} plant, which is vanilla
 * farmland, and rubber farmland that sustains anything). Planting makes no sound, as in 1.7.
 *
 * <p>Each seed is its crop's block item (upstream gives every crop an {@code ItemBlockBase}), keeping its own
 * item name rather than the crop's.
 */
public class ItemSpaceSeedFood extends BlockItem {

    private final String itemKey;

    public ItemSpaceSeedFood(Properties properties, Block crop, String id) {
        super(crop, properties);
        this.itemKey = "item." + NtmSpace.MOD_ID + "." + id;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(itemKey);
    }

    public static boolean isSeedGround(BlockState soil) {
        return soil.is(Blocks.FARMLAND) || soil.is(FloraBlocks.RUBBER_FARMLAND.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (context.getClickedFace() != Direction.UP) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player != null && (!player.mayUseItemAt(pos, Direction.UP, stack) || !player.mayUseItemAt(pos.above(), Direction.UP, stack))) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!isSeedGround(level.getBlockState(pos)) || !level.isEmptyBlock(pos.above())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.setBlock(pos.above(), getBlock().defaultBlockState(), 3);
            if (player == null || !player.isCreative()) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
