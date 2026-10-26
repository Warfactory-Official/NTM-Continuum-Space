package com.ntmspace.block.machine;

import dev.golem.core.hook.Verdict;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The powered greenhouse's physical dummy beam; the facing preserves the replaced cell's exact state. */
public final class HydroponicLampCell extends BlockMultiblockCell
        implements ILookOverlay, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public HydroponicLampCell(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockMultiblockCore.FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var owner = MultiblockSurface.ownerOf(level, pos);
        return owner == null ? Shapes.block() : owner.block().getShape(
                owner.state().setValue(BlockMultiblockMachine.CELL, true), level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var owner = MultiblockSurface.ownerOf(level, pos);
        return owner == null ? Shapes.block() : owner.block().getCollisionShape(
                owner.state().setValue(BlockMultiblockMachine.CELL, true), level, pos, context);
    }

    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        var owner = MultiblockSurface.ownerOf(level, pos);
        return owner != null && owner.block() instanceof ILookOverlay overlay
                ? overlay.getLookOverlay(level, pos, player) : null;
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection direction) {
        var owner = MultiblockSurface.ownerOf(level, pos);
        return owner != null && owner.block().canConnect(level, pos, direction);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection direction) {
        var owner = MultiblockSurface.ownerOf(level, new BlockPos(x, y, z));
        return owner != null && owner.block().canConnect(type, level, x, y, z, direction);
    }

    /** The roof lamps never replace the greenhouse's plant-supporting base. */
    @Override
    protected Verdict onSustainPlant(BlockGetter level, BlockPos soilPos,
                                                       BlockState soil, BlockState plant) {
        return Verdict.DENY;
    }
}
