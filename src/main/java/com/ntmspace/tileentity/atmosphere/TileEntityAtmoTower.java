package com.ntmspace.tileentity.atmosphere;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityDeuteriumTower;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.AtmosphereMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityAtmoTower} ({@code machine_atmo_tower}, "Atmospheric Liquefaction Tower"): HBM's deuterium
 * tower with a 50,000 mB Earth-air input, a 5,000 mB nitrogen output and a 1,000,000 HE buffer. It does not touch the
 * body's air: the Earth air comes through the ducts (an atmospheric compressor collects it).
 *
 * <p>hbm-space changed the extractor's step for every deuterium machine (its {@code TileEntityDeuteriumExtractor}): a
 * step needs {@code max / 100} HE and more than 200 HE, and costs {@code max / 100} (1.7 upstream: {@code max / 20}).
 * HBM's own deuterium machines keep upstream's rule; this tower runs hbm-space's: up to 100 mB of nitrogen from 5,000 mB
 * of Earth air for 10,000 HE a tick (50:1, divided first and multiplied after, as 1.7).
 */
public class TileEntityAtmoTower extends TileEntityDeuteriumTower {

    public static final long MAX_POWER = 1_000_000;

    public TileEntityAtmoTower(BlockPos pos, BlockState state) {
        super(AtmosphereMachines.ATMO_TOWER_BE.get(), pos, state);
        tanks[0] = new FluidTankNTM(SpaceFluids.EARTHAIR, 50_000);
        tanks[1] = new FluidTankNTM(SpaceFluids.NITROGEN, 5_000);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public String getDefaultName() {
        return "block.ntmspace.machine_atmo_tower";
    }

    /** hbm-space's {@code TileEntityDeuteriumExtractor.updateEntity} with the tower's tanks and power. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAtmoTower tower) {
        tower.setupAllPorts(tower.getPorts());
        tower.updatePortPIFIFO();
        tower.step();
        tower.setChanged();
        tower.networkPackNT();
    }

    /** One conversion step (hbm-space's rule, see the class comment). */
    public void step() {
        if (hasPower() && power > 200 && hasEnoughWater() && tanks[1].getMaxFill() > tanks[1].getFill()) {
            int convert = Math.min(tanks[1].getMaxFill(), tanks[0].getFill()) / 50;
            convert = Math.min(convert, tanks[1].getMaxFill() - tanks[1].getFill());
            tanks[0].setFill(tanks[0].getFill() - convert * 50); // dividing first, then multiplying, removes rounding (1.7)
            tanks[1].setFill(tanks[1].getFill() + convert);
            power -= getMaxPower() / 100;
        }
    }

    @Override
    protected boolean hasPower() {
        return power >= getMaxPower() / 100;
    }
}
