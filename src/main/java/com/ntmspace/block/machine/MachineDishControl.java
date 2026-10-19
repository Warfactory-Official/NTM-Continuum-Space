package com.ntmspace.block.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.ntmspace.tileentity.machine.TileEntityDishControl;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import dev.golem.core.gui.Menus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code MachineDishControl} ("StarDar Remote Access Console"): two blocks ({@code getDimensions}
 * {0, 0, 0, 0, 0, 1}, offset 0). Used without sneaking it opens the linked StarDar's GUI; unlinked, or linked to a
 * position that is no StarDar any more, it says so in chat and lets the held item act (1.7 returned {@code false} from
 * {@code onBlockActivated}, so a held reactor sensor then linked it: see {@code StardarSensorLink}).
 */
public class MachineDishControl extends BlockMultiblockMachine {

    public MachineDishControl(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDishControl(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 0, 0, 0, 1}; }
    @Override public int getOffset() { return 0; }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(core) instanceof TileEntityDishControl control)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // Check if a dish was not assigned
        if (!control.isLinked) {
            player.sendSystemMessage(prefix().append(Component.literal("Dish not linked!").withStyle(ChatFormatting.RED)));
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // also check that you're still pointing at a stardar...
        BlockPos link = control.linkPosition;
        if (!(level.getBlockState(link).getBlock() instanceof MachineStardar)
                || !(level.getBlockEntity(link) instanceof TileEntityMachineStardar stardar)) {
            player.sendSystemMessage(prefix().append(Component.literal("Missing control target!").withStyle(ChatFormatting.RED)));
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // Trigger the StarDar UI to open
        if (player instanceof ServerPlayer serverPlayer) Menus.openWithPos(serverPlayer, stardar, link);
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code ChatBuilder}: {@code [<console name>] } in dark aqua. */
    private MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }
}
