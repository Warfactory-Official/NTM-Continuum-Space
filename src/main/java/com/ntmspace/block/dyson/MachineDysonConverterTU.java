package com.ntmspace.block.dyson;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterTU;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineDysonConverterTU}, the Dyson Heat Converter: {@code getDimensions} {2, 0, 1, 6, 1, 1},
 * offset 6; the beam's intake is the front end (six ahead of the core, one up). The two cells above the core are
 * {@code TileEntityProxyCombo().heatSource()}s, so a heat consumer standing on the machine draws the core's heat.
 */
public class MachineDysonConverterTU extends BlockMultiblockMachine {

    public MachineDysonConverterTU(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonConverterTU(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {2, 0, 1, 6, 1, 1}; }
    @Override public int getOffset() { return 6; }

    /** The {@code makeExtra} heat cells one and two above the core. */
    @Override
    public boolean heatSourceAt(int lx, int ly, int lz) {
        return lx == 0 && lz == 0 && (ly == 1 || ly == 2);
    }
}
