package com.ntmspace.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidDirectionalMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.generic.BlockAlgaeFilm;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.LeftoverContent;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityAlgaeFilm}: an 8,000 mB carbon dioxide tank and an 8,000 mB oxygen tank. In orbit
 * ({@code worldObj.provider instanceof WorldProviderOrbit}), on half the ticks ({@code rand.nextBoolean()}), one mB of
 * CO2 becomes one mB of oxygen while there is CO2 and room. Every tick it takes CO2 from, and gives oxygen to, the two
 * sides square to its facing (1.7 {@code getOrientation(meta).getRotation(UP)}, east for a north front, and its opposite) - the port's net finds
 * it there ({@link IFluidDirectionalMK2}: both roles on those two sides, none elsewhere) - and syncs both tanks and the
 * orbit flag. Tanks saved as {@code t0}/{@code t1}, as 1.7.
 */
public class TileEntityAlgaeFilm extends TileEntityMachineBase implements IFluidStandardTransceiverMK2, IFluidDirectionalMK2 {

    public final FluidTankNTM[] tanks;
    public boolean canOperate;

    public TileEntityAlgaeFilm(BlockPos pos, BlockState state) {
        super(LeftoverContent.ALGAE_FILM_BE.get(), pos, state, 0);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.CARBONDIOXIDE, 8_000);
        tanks[1] = new FluidTankNTM(Fluids.OXYGEN, 8_000);
    }

    @Override
    public String getDefaultName() {
        return "container.algaeFilm";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAlgaeFilm te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortFIFO();
        te.step(level, Orbit.inOrbit(level), level.getRandom().nextBoolean());
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityAlgaeFilm te) {}

    /** One 1.7 {@code updateEntity} on the server: {@code orbit} and the {@code nextBoolean} given. */
    public void step(Level level, boolean orbit, boolean coin) {
        canOperate = orbit;

        if (canOperate && coin) {
            if (tanks[0].getFill() > 0 && tanks[1].getFill() < tanks[1].getMaxFill()) {
                tanks[0].setFill(tanks[0].getFill() - 1);
                tanks[1].setFill(tanks[1].getFill() + 1);
            }
        }

        setChanged();
        networkPackNT();
    }

    /** 1.7 {@code d = getOrientation(meta).getRotation(UP)} and {@code d.getOpposite()}: the two sides beside the front. */
    public static ForgeDirection[] ports(BlockState state) {
        Direction facing = state.hasProperty(BlockAlgaeFilm.FACING) ? state.getValue(BlockAlgaeFilm.FACING) : Direction.NORTH;
        ForgeDirection d = ForgeDirection.getOrientation(facing).getRotation(ForgeDirection.UP);
        return new ForgeDirection[] {d, d.getOpposite()};
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = new PortDef[] {PortDef.make(worldPosition, ports(getBlockState()))};
        return cachedPorts;
    }

    private boolean isPort(ForgeDirection side) {
        for (ForgeDirection port : ports(getBlockState())) {
            if (port == side) return true;
        }
        return false;
    }

    @Override
    public boolean providesOn(ForgeDirection side) {
        return isPort(side);
    }

    @Override
    public boolean receivesOn(ForgeDirection side) {
        return isPort(side);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[] {tanks[1]};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[] {tanks[0]};
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(canOperate);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        canOperate = buf.readBoolean();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }
}
