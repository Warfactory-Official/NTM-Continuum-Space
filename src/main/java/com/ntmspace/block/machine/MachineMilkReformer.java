package com.ntmspace.block.machine;

import com.ntmspace.registry.SpaceMachines;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineMilkReformer}: a 3 x 3 x 7 tower ({@code {6, 0, 1, 1, 1, 1}}, offset 1), the four bottom
 * corners {@code TileEntityProxyCombo().fluid().power()}; its eight connection points sit beside the corners. Opens the
 * GUI on use.
 */
public class MachineMilkReformer extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ICapabilityBlock {

    // music listened to: https://www.youtube.com/watch?v=9imCm6CrNZ8&list=RD9imCm6CrNZ8&index=1 (hbm-space)

    private static final int[] DIMS = {6, 0, 1, 1, 1, 1};

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(SpaceMachines.MILK_REFORMER_BE).powerIn().fluidIn().fluidOut().items().fe()
                .faces(TileEntityMachineMilkReformer.class, (be, side) -> side != Direction.DOWN)
                .fluidFaces(TileEntityMachineMilkReformer.class, (be, side) -> side != Direction.DOWN);
    }

    public MachineMilkReformer(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineMilkReformer(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 1; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MachinePorts.ports(core, facing, pos -> MachinePorts.inBox(core, DIMS, facing, pos),
                TileEntityMachineMilkReformer.conPos(core), ROLE_POWER_IN | ROLE_FLUID, visitor);
    }

    /** 1.7 {@code makeExtra} at the four bottom corners (world x/z +-1, not turned with the machine; it is symmetric). */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int dx : new int[] {1, -1}) {
            for (int dz : new int[] {1, -1}) visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
        }
    }
}
