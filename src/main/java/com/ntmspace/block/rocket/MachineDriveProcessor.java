package com.ntmspace.block.rocket;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineDriveProcessor}: two blocks ({@code getDimensions {0, 0, 0, 0, 0, 1}}, offset 0), the core
 * and one {@code TileEntityProxyCombo(inventory, power)} beside it. Power comes in at the back of both blocks and at
 * the two ends (1.7 {@code getPorts}). Its proxy's inventory exposes no slot (1.7 {@code TileEntityMachineBase} gives
 * automation none), so hoppers do nothing. Opens the GUI on use.
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

    @Override
    public int coreMask() {
        return MachinePorts.corePortFaces(BlockPos.ZERO, Direction.SOUTH,
                TileEntityMachineDriveProcessor.ports(BlockPos.ZERO, ForgeDirection.SOUTH));
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MachinePorts.portFaces(core, facing, TileEntityMachineDriveProcessor.ports(core, MachinePorts.dir(facing)),
                ROLE_POWER_IN, visitor);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN);
    }
}
