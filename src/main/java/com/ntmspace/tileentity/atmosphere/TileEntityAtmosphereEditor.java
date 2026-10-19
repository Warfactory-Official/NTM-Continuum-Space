package com.ntmspace.tileentity.atmosphere;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code BlockAtmosphereEditor.TileEntityAtmosphereEditor}: a creative tool that, while on, releases (or
 * captures) {@code 10^throughput} mB of one gas into (from) the body's air every 5 ticks. Default: off, Earth air,
 * emitting, throughput 10 (10^10 mB = 0.01 atm per 5 ticks). Saved by fluid name (1.7 saved the numeric fluid id).
 * 1.7 sent its state to players within 25 blocks every tick; the port's tile sync does the same job.
 */
public class TileEntityAtmosphereEditor extends TileEntityLoadedBase {

    public boolean isOn = false;
    public int throughputFactor = 10;
    public FluidType fluid = SpaceFluids.EARTHAIR;
    public boolean isEmitting = true;

    public TileEntityAtmosphereEditor(BlockPos pos, BlockState state) {
        super(AtmosphereMachines.ATMOSPHERE_EDITOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAtmosphereEditor te) {
        if (te.isOn && level.getGameTime() % 5 == 0) {
            if (te.isEmitting) {
                CelestialTraits.release(level, te.fluid, Math.pow(10, te.throughputFactor));
            } else {
                CelestialTraits.capture(level, te.fluid, Math.pow(10, te.throughputFactor));
            }
        }
        te.networkPackNT();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isOn", isOn);
        output.putInt("throughput", throughputFactor);
        Fluids.write(output, "fluid", fluid);
        output.putBoolean("emit", isEmitting);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isOn = input.getBooleanOr("isOn", false);
        throughputFactor = input.getIntOr("throughput", 10);
        fluid = Fluids.read(input, "fluid");
        isEmitting = input.getBooleanOr("emit", true);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeInt(throughputFactor);
        buf.writeInt(Fluids.networkId(fluid));
        buf.writeBoolean(isEmitting);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        throughputFactor = buf.readInt();
        fluid = Fluids.byNetworkId(buf.readInt());
        isEmitting = buf.readBoolean();
    }
}
