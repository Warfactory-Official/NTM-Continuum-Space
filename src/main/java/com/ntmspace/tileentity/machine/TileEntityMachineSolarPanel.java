package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.environment.Environment;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.ntmspace.registry.SpaceMachinesC;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityMachineSolarPanel}: pushes its buffer (1,000 HE) into the six neighbours of the core,
 * then, if the sky is visible above it, adds {@code ceil(sun * 25 * sunPower)} HE, where {@code sun} = sky light - sky
 * darkening - 11 (1 to 4 by day) and {@code sunPower} is 1.7 {@code WorldProviderCelestial.getSunPower}
 * ({@link Environment#sunlight}: the body's distance from the sun, less any eclipse; NTM: Space answers it in
 * {@link SunPower}). "Balanced around 100he/t on Earth."
 */
public class TileEntityMachineSolarPanel extends TileEntityLoadedBase implements IEnergyProviderMK2 {

    private long power;
    private long maxpwr = 1_000;

    public TileEntityMachineSolarPanel(BlockPos pos, BlockState state) {
        super(SpaceMachinesC.SOLAR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSolarPanel te) {
        te.setupPowerPorts(te.getPorts());
        te.updateAllPorts();
        te.providePower();

        // Sun power ranges from 1-4. The sky light at the panel itself: HBM's multiblocks have 1.7's light opacity 0
        int sun = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() - 11;

        if (sun <= 0 || !level.canSeeSky(pos.above())) {
            return;
        }

        te.power += te.getOutput(sun, (float) Environment.sunlight(level, pos));

        if (te.power > te.getMaxPower()) te.power = te.getMaxPower();
        te.setChanged();
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return cachedPorts;
    }

    /** 1.7 {@code getOutput}: balanced around 100 HE/t on Earth (sun 4, sun power 1). */
    public long getOutput(int sun, float sunPower) {
        return Mth.ceil(sun * 25 * sunPower);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxpwr; }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("maxpwr", maxpwr);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        maxpwr = input.getLongOr("maxpwr", 1_000L);
    }
}
