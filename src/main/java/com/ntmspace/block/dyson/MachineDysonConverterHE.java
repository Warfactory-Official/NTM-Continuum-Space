package com.ntmspace.block.dyson;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.lib.DirPos;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineDysonConverterHE}, the Dyson Power Converter: {@code getDimensions} {2, 0, 4, 4, 1, 1},
 * offset 4; the beam's intake is the front end (four ahead of the core, one up) and the power output the
 * {@code TileEntityProxyCombo(false, true, false)} cell at the back end, connected from behind.
 */
public class MachineDysonConverterHE extends BlockMultiblockMachine implements IEnergyConnectorBlock {

    public MachineDysonConverterHE(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonConverterHE(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {2, 0, 4, 4, 1, 1}; }
    @Override public int getOffset() { return 4; }

    /** The {@code makeExtra} cell: four behind the core. */
    public static BlockPos powerCell(BlockPos core, Direction facing) {
        return core.relative(facing, -4);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        BlockPos cell = powerCell(core, facing);
        DirPos out = TileEntityDysonConverterHE.output(core, MachinePorts.dir(facing));
        MachinePorts.ports(core, facing, cell::equals, new DirPos[] {out}, ROLE_POWER, visitor);
    }

}
