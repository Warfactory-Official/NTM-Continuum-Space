package com.ntmspace.block.war;

import com.ntmspace.tileentity.war.TileEntityMachineWarController;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code WarController} ({@code BlockMachineBase(Material.iron, 0)}): a plain steel cube whose click opens
 * the war controller's GUI (1.7 GUI id 0, the tile entity's {@code IGUIProvider}); sneaking passes. Breaking it spills
 * its slots (1.7 {@code BlockMachineBase.breakBlock}). Not rotatable.
 */
public class WarController extends CompatBlock implements TickingBlock {

    public WarController(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineWarController(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TileEntityMachineWarController te) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, te, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
