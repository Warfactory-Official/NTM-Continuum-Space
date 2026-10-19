package com.ntmspace.block.rocket;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineDriveProcessor}: two blocks ({@code getDimensions {0, 0, 0, 0, 0, 1}}, offset 0), the core
 * and one {@code TileEntityProxyCombo(inventory, power)} beside it. The core subscribes to power on all six faces
 * (1.7 {@code trySubscribe} over {@code VALID_DIRECTIONS}); the second block's proxy subscribes nothing, as in 1.7.
 * Its proxy's inventory exposes no slot (1.7 {@code TileEntityMachineBase} gives automation none), so hoppers do
 * nothing. Opens the GUI on use.
 */
public class MachineDriveProcessor extends BlockMultiblockMachine implements IEnergyConnectorBlock {

    public MachineDriveProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineDriveProcessor(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 0, 0, 0, 1}; }
    @Override public int getOffset() { return 0; }

    /** 1.7's six-direction subscription loop. */
    @Override public int coreMask() { return MASK_ALL; }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN);
    }
}
