package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.recipe.AlkylationRecipes;
import com.ntmspace.registry.SpaceMachines;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityMachineAlkylation}: every second tick one step of its {@code ntmspace:alkylation} recipe
 * (100 mB of the primary input in tank 0, the secondary in tank 1, two outputs into tanks 2 and 3), 4,000 HE a step
 * (40 kHE/s) from a 1 MHE buffer. Power, both inputs and both outputs go through the ports of its six side cells. It has
 * eleven slots and no GUI (as in 1.7, where nothing used them).
 *
 * <p>Not ported: 1.7 {@code IPersistentNBT} (deferred, as for HBM's own machines).
 */
public class TileEntityMachineAlkylation extends TileEntityMachineBase implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, NotBelowConnector {

    public static final long CONSUMPTION = 4_000;
    public long power;
    public static final long maxPower = 1_000_000;

    public final FluidTankNTM[] tanks;

    public TileEntityMachineAlkylation(BlockPos pos, BlockState state) {
        super(SpaceMachines.ALKYLATION_BE.get(), pos, state, 11);
        tanks = new FluidTankNTM[4];
        tanks[0] = new FluidTankNTM(SpaceFluids.CHLOROMETHANE, 8_000);
        tanks[1] = new FluidTankNTM(Fluids.NONE, 4_000);
        tanks[2] = new FluidTankNTM(Fluids.UNSATURATEDS, 8_000);
        tanks[3] = new FluidTankNTM(Fluids.CHLORINE, 8_000);
    }

    @Override
    public String getDefaultName() {
        return "container.alkylation";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAlkylation te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();
        long time = level.getGameTime();
        if (time % 2 == 0) te.alkylate();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAlkylation te) {}

    /** 1.7 {@code alkylate}. */
    public void alkylate() {
        AlkylationRecipes.Output out = AlkylationRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        if (out == null) {
            // 1.7 leaves tank 1's type alone here
            tanks[2].setTankType(Fluids.NONE);
            tanks[3].setTankType(Fluids.NONE);
            return;
        }

        tanks[1].setTankType(out.secondary().type);
        tanks[2].setTankType(out.out1().type);
        tanks[3].setTankType(out.out2().type);

        if (power < out.power()) return; // 40 kHE/s
        if (tanks[0].getFill() < out.input()) return;
        if (tanks[1].getFill() < out.secondary().fill) return;

        if (tanks[2].getFill() + out.out1().fill > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + out.out2().fill > tanks[3].getMaxFill()) return;

        tanks[0].setFill(tanks[0].getFill() - out.input());
        tanks[1].setFill(tanks[1].getFill() - out.secondary().fill);
        tanks[2].setFill(tanks[2].getFill() + out.out1().fill);
        tanks[3].setFill(tanks[3].getFill() + out.out2().fill);

        power -= out.power();
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = ports(worldPosition, MachineCore.facing(getBlockState()));
        return cachedPorts;
    }

    /**
     * 1.7 {@code getPorts} (rotated about DOWN, as 1.7 wrote it). The four end corners are listed twice, and a power
     * port that finds a node on its cell takes that one over, so a corner takes power on the face listed first only.
     */
    public static PortDef[] ports(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new PortDef[] {
                PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, rot),
                PortDef.make(x + rot.offsetX + dir.offsetX * 2, y, z + rot.offsetZ + dir.offsetZ * 2, rot),
                PortDef.make(x + rot.offsetX - dir.offsetX * 2, y, z + rot.offsetZ - dir.offsetZ * 2, rot),

                PortDef.make(x + rot.offsetX + dir.offsetX * 2, y, z + rot.offsetZ + dir.offsetZ * 2, dir),
                PortDef.make(x - rot.offsetX + dir.offsetX * 2, y, z - rot.offsetZ + dir.offsetZ * 2, dir),

                PortDef.make(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                PortDef.make(x - rot.offsetX + dir.offsetX * 2, y, z - rot.offsetZ + dir.offsetZ * 2, rot.getOpposite()),
                PortDef.make(x - rot.offsetX - dir.offsetX * 2, y, z - rot.offsetZ - dir.offsetZ * 2, rot.getOpposite()),

                PortDef.make(x + rot.offsetX - dir.offsetX * 2, y, z + rot.offsetZ - dir.offsetZ * 2, dir.getOpposite()),
                PortDef.make(x - rot.offsetX - dir.offsetX * 2, y, z - rot.offsetZ - dir.offsetZ * 2, dir.getOpposite()),
        };
    }

    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        return PortDef.connections(ports(core, dir));
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 3, p.getY(), p.getZ() - 3, p.getX() + 3, p.getY() + 3, p.getZ() + 3);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[2], tanks[3]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tanks[0], tanks[1]}; }
}
