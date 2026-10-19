package com.ntmspace.tileentity.dyson;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmspace.dyson.DysonConverter;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityDysonConverterAnatmogenesis}, the Dyson Anatmogenesis Terraformer ("an-" without,
 * "atmo-" vapour, "genesis" creation: an atmosphere from nothing, "the survival version of the creative atmosphere
 * editor"). A beam entering its intake (five ahead of the core, one up) is turned into {@code energy / 28,800,000} mB of
 * its gas (100 THE/s make 0.1 atm in 8 hours), released into the body's air - or, in capture mode (screwdriver),
 * taken out of it ({@link CelestialTraits#release}/{@link CelestialTraits#capture}, 1.7 {@code FT_Gaseous}). The gas
 * is set with a fluid identifier (gases only); oxygen by default.
 *
 * <p>The gas is saved by name ({@code "fluid"}, never 1.7's numeric id).
 */
public class TileEntityDysonConverterAnatmogenesis extends TileEntityLoadedBase implements DysonConverter {

    public FluidType fluid = Fluids.OXYGEN;
    public boolean isEmitting = true;

    public long gasProduced;

    public boolean isConverting;

    // 100THE/s will produce 0.1atm in 8 hours
    public static final long HE_TO_MB = 28_800_000;

    /** Client effects (the gas plume), installed by the client. */
    public static volatile java.util.function.Consumer<TileEntityDysonConverterAnatmogenesis> CLIENT_EFFECTS = null;

    public TileEntityDysonConverterAnatmogenesis(BlockPos pos, BlockState state) {
        super(DysonContent.CONVERTER_ANATMOGENESIS_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDysonConverterAnatmogenesis te) {
        te.isConverting = te.gasProduced > 0;

        te.networkPackNT();
        te.gasProduced = 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityDysonConverterAnatmogenesis te) {
        if (te.isConverting && CLIENT_EFFECTS != null) CLIENT_EFFECTS.accept(te);
    }

    @Override
    public boolean provideEnergy(int x, int y, int z, long energy) {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        int rx = worldPosition.getX() + dir.offsetX * 5;
        int ry = worldPosition.getY() + 1;
        int rz = worldPosition.getZ() + dir.offsetZ * 5;

        if (x != rx || y != ry || z != rz) return false;

        long volume = energy / HE_TO_MB;
        gasProduced += volume;

        if (level != null) {
            if (isEmitting) {
                CelestialTraits.release(level, fluid, volume);
            } else {
                CelestialTraits.capture(level, fluid, volume);
            }
        }

        return true;
    }

    @Override
    public long maximumEnergy() {
        return Long.MAX_VALUE;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isConverting);
        buf.writeLong(gasProduced);
        buf.writeInt(Fluids.networkId(fluid));
        buf.writeBoolean(isEmitting);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isConverting = buf.readBoolean();
        gasProduced = buf.readLong();
        fluid = Fluids.byNetworkId(buf.readInt());
        isEmitting = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        Fluids.write(output, "fluid", fluid);
        output.putBoolean("emit", isEmitting);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        fluid = Fluids.read(input, "fluid");
        isEmitting = input.getBooleanOr("emit", false);
    }
}
