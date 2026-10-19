package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.ntmspace.tileentity.machine.TileEntityMachineSolarPanel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineSolar} (Solar Panel): a 1x1x5 strip along the placement direction ({@code getDimensions}
 * {0, 0, 2, 2, 0, 0}, offset 0), core in the middle. 1.7 stamped no {@code makeExtra} cells, so only the core touches
 * the networks, on all six faces (its {@code tryProvide} at every neighbour). No GUI.
 */
public class MachineSolar extends BlockMultiblockMachine implements IEnergyConnectorBlock {

    public MachineSolar(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSolarPanel(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 2, 2, 0, 0}; }
    @Override public int getOffset() { return 0; }

    /** The core provides on every face (1.7 {@code ForgeDirection.VALID_DIRECTIONS}). */
    @Override public int coreMask() { return MASK_ALL; }
}
