package com.ntmspace.tileentity.dyson;

import com.hbm.api.tile.IHeatSource;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmspace.dyson.DysonConverter;
import com.ntmspace.registry.DysonContent;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityDysonConverterTU}, the Dyson Heat Converter: a beam entering its intake (six ahead of
 * the core, one up) adds its energy as heat (TU), saturating at {@code Integer.MAX_VALUE}; heat consumers draw it from
 * the core and the two heat cells above it. It does not tick.
 */
public class TileEntityDysonConverterTU extends TileEntityLoadedBase implements DysonConverter, IHeatSource {

    public int heatEnergy;

    public TileEntityDysonConverterTU(BlockPos pos, BlockState state) {
        super(DysonContent.CONVERTER_TU_BE.get(), pos, state);
    }

    @Override
    public boolean provideEnergy(int x, int y, int z, long energy) {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        int rx = worldPosition.getX() + dir.offsetX * 6;
        int ry = worldPosition.getY() + 1;
        int rz = worldPosition.getZ() + dir.offsetZ * 6;

        if (x != rx || y != ry || z != rz) return false;

        if (energy > Integer.MAX_VALUE) {
            heatEnergy = Integer.MAX_VALUE;
            setChanged();
            return true;
        }
        heatEnergy += energy;
        if (heatEnergy < 0) heatEnergy = Integer.MAX_VALUE; // prevent overflow
        setChanged();

        return true;
    }

    @Override
    public long maximumEnergy() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        heatEnergy = Math.max(0, heatEnergy - heat);
        setChanged();
    }

    /** 1.7 wrote the heat as a long and read it as an int (numeric tags converted); saved and read as a long here. */
    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("heatEnergy", heatEnergy);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        heatEnergy = (int) input.getLongOr("heatEnergy", 0);
    }
}
