package com.ntmspace.block.rocket;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code MachineRocketAssembly} (VAB): a 9 x 9 deck on four corner legs two blocks high (1.7
 * {@code getDimensions {0, 2, 4, 4, 4, 4}} for the space check, offset 4, height offset 2; {@code fillSpace} fills the
 * deck and eight leg boxes). The service platforms and spires its block entity raises are further cells of the same
 * multiblock ({@link #placeDynamicCell}); the teardown visits every position they can take. Unusable in orbit.
 */
public class MachineRocketAssembly extends BlockMultiblockMachine implements ILookOverlay {

    public MachineRocketAssembly(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRocketAssembly(pos, state);
    }

    // funky behaviour, but the space checking is still done regularly
    @Override public int[] getDimensions() { return new int[] {0, 2, 4, 4, 4, 4}; }
    @Override public int getOffset() { return 4; }
    @Override public int getHeightOffset() { return 2; }

    private static final int[][] BOXES = {
            // Top
            {0, 0, 4, 4, 4, 4},
            // Leggies
            {0, 2, 4, -3, 4, -4}, {0, 2, 4, -4, 4, -3},
            {0, 2, 4, -3, -4, 4}, {0, 2, 4, -4, -3, 4},
            {0, 2, -3, 4, -4, 4}, {0, 2, -4, 4, -3, 4},
            {0, 2, -3, 4, 4, -4}, {0, 2, -4, 4, 4, -3}};

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);
    }

    @Override
    protected void visitTeardownCells(BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        TileEntityMachineRocketAssembly.visitDynamicCells(core, pos -> visitor.cell(pos, MASK_NONE));
    }

    /** A platform or spire cell at {@code at}, owned by the VAB at {@code core}. */
    public void placeDynamicCell(Level level, BlockPos core, BlockPos at) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = at.immutable();
        Direction facing = level.getBlockState(core).getValue(FACING);
        withoutTeardown(() -> level.setBlock(pos, defaultBlockState().setValue(FACING, facing).setValue(CELL, Boolean.TRUE), 3));
        if (isCellOf(level.getBlockState(pos))) MultiblockCoreIndex.put(server, pos, core);
    }

    /** Removes a platform or spire cell of the VAB at {@code core}; anything else is left alone. */
    public void removeDynamicCell(Level level, BlockPos core, BlockPos at) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = at.immutable();
        if (!isCellOf(level.getBlockState(pos))) return;
        long owner = MultiblockCoreIndex.lookup(server, pos);
        if (owner != Long.MIN_VALUE && owner != core.asLong()) return;
        withoutTeardown(() -> level.removeBlock(pos, false));
        MultiblockCoreIndex.remove(server, pos);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (Orbit.inOrbit(level)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!Orbit.inOrbit(level)) return null;
        String text = "&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                + Component.translatable("atmosphere.yesOrbit").getString() + " ! ! !";
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, List.of(text));
    }
}
