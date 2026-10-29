package com.ntmspace.block.rocket;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MachineRocketAssembly} (VAB): a 9 x 9 deck on four corner legs two blocks high (1.7
 * {@code getDimensions {0, 2, 4, 4, 4, 4}} for the space check, offset 4, height offset 2; {@code fillSpace} fills the
 * deck and eight leg boxes). The service platforms and spires its block entity raises are further cells of the same
 * multiblock ({@link #placeDynamicCell}); the teardown visits every position they can take. Unusable in orbit.
 */
public class MachineRocketAssembly extends BlockMultiblockMachine implements ILookOverlay {
    private static final int[] NO_PLATFORMS = new int[0];

    public MachineRocketAssembly(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRocketAssembly(pos, state);
    }

    // funky behaviour, but the space checking is still done regularly
    @Override
    public int[] getDimensions() {
        return new int[]{0, 2, 4, 4, 4, 4};
    }

    @Override
    public int getOffset() {
        return 4;
    }

    @Override
    public int getHeightOffset() {
        return 2;
    }

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
    public void visitPlacedCells(BlockGetter level, BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        if (level.getBlockEntity(core) instanceof TileEntityMachineRocketAssembly vab)
            visitRaised(core, vab.spireHeight(), vab.platforms(), visitor);
    }

    @Override
    public void visitPlacementCells(BlockPos core, Direction facing, ItemStack stack, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        var data = ItemStacks.blockEntityData(stack, RocketBlocks.ROCKET_ASSEMBLY_BE.get());
        if (data != null) visitRaised(core, Tags.getIntOr(data, "spire", 0),
                Tags.getIntArrayOr(data, "platforms", NO_PLATFORMS), visitor);
    }

    public static void visitRaised(BlockPos core, int spire, int[] platforms, CellVisitor visitor) {
        var pos = new BlockPos.MutableBlockPos();
        for (int y = 1; y <= spire; y++) {
            visitor.cell(pos.setWithOffset(core, 0, y, 0), MASK_NONE);
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz = -1; sz <= 1; sz += 2)
                    visitor.cell(pos.setWithOffset(core, sx * 4, y, sz * 4), MASK_NONE);
        }
        for (int y : platforms)
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++)
                    if (!column(x, z)) visitor.cell(pos.setWithOffset(core, x, y, z), MASK_NONE);
    }

    private static boolean column(int x, int z) {
        return x == 0 && z == 0 || Math.abs(x) == 4 && Math.abs(z) == 4;
    }

    @Override
    public boolean claimsCell(BlockGetter level, BlockPos core, BlockPos cell, Direction facing) {
        if (super.claimsCell(level, core, cell, facing)) return true;
        int x = cell.getX() - core.getX(), y = cell.getY() - core.getY(), z = cell.getZ() - core.getZ();
        if (y < 1 || Math.abs(x) > 4 || Math.abs(z) > 4
                || !(level.getBlockEntity(core) instanceof TileEntityMachineRocketAssembly vab)) return false;
        if (column(x, z)) return y <= vab.spireHeight();
        for (int platform : vab.platforms()) if (platform == y) return true;
        return false;
    }

    @Override
    public @Nullable BlockPos findClientCore(BlockGetter level, BlockPos cell) {
        var pos = cell.mutable();
        var core = descend(level, pos, cell);
        if (core != null) return MultiblockSurface.coreClaims(level, core, cell) ? core : null;
        int foot = pos.getY();
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                pos.set(cell.getX() + sx * 4, foot + 2, cell.getZ() + sz * 4);
                if (MultiblockSurface.coreClaims(level, pos, cell)) return pos.immutable();
            }
        int x = runStart(level, cell, Direction.WEST), z = runStart(level, cell, Direction.NORTH);
        pos.set(x + 4 + Math.floorDiv(cell.getX() - x, 9) * 9, cell.getY(),
                z + 4 + Math.floorDiv(cell.getZ() - z, 9) * 9);
        core = descend(level, pos, cell);
        return core != null && MultiblockSurface.coreClaims(level, core, cell)
                ? core : super.findClientCore(level, cell);
    }

    private @Nullable BlockPos descend(BlockGetter level, BlockPos.MutableBlockPos pos, BlockPos from) {
        while (readable(level, pos, from)) {
            var state = level.getBlockState(pos);
            if (!state.is(this)) break;
            if (!isCellState(state)) return pos.immutable();
            pos.move(Direction.DOWN);
        }
        pos.move(Direction.UP);
        return null;
    }

    private int runStart(BlockGetter level, BlockPos cell, Direction toward) {
        var pos = cell.mutable();
        do {
            pos.move(toward);
        }
        while (readable(level, pos, cell) && level.getBlockState(pos).is(this));
        pos.move(toward.getOpposite());
        return toward.getAxis().choose(pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean readable(BlockGetter level, BlockPos pos, BlockPos from) {
        return level instanceof Level world ? !world.isOutsideBuildHeight(pos) && world.hasChunkAt(pos)
                : Math.max(Math.abs(pos.getX() - from.getX()), Math.max(Math.abs(pos.getY() - from.getY()),
                Math.abs(pos.getZ() - from.getZ()))) <= MultiblockSurface.REGION_REACH;
    }

    @Override
    protected void visitTeardownCells(ServerLevel level, BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        TileEntityMachineRocketAssembly.visitDynamicCells(level, core, pos -> visitor.cell(pos, MASK_NONE));
    }

    /**
     * A platform or spire cell at {@code at}, owned by the VAB at {@code core}.
     */
    public void placeDynamicCell(Level level, BlockPos core, BlockPos at) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = at.immutable();
        Direction facing = level.getBlockState(core).getValue(FACING);
        withoutTeardown(
                () -> level.setBlock(pos, defaultBlockState().setValue(FACING, facing).setValue(CELL, Boolean.TRUE),
                        3));
        if (isCellOf(level.getBlockState(pos))) MultiblockCoreIndex.put(server, pos, core);
    }

    /**
     * Removes a platform or spire cell of the VAB at {@code core}; anything else is left alone.
     */
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
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player,
                                          BlockHitResult hit) {
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
