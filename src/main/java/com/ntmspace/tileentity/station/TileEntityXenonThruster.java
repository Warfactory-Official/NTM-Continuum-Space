package com.ntmspace.tileentity.station;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.Propulsion;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityXenonThruster}: an ion engine that burns xenon (4,000 mB tank) and 5,000 HE per mB burned
 * (20 MHE buffer) for 1.4 MN of thrust; ten seconds to start and ten to cool. It powers the station only facing west
 * (prograde) and only in orbit. Power and xenon come in at the three blocks behind it.
 */
public class TileEntityXenonThruster extends TileEntityMachineBase implements Propulsion, IFluidStandardReceiverMK2, IEnergyReceiverMK2 {

    public final FluidTankNTM[] tanks;

    public long power;
    public static final long maxPower = 20_000_000;

    public static final int POWER_COST_MULTIPLIER = 5_000;

    public boolean isOn;
    public float thrustAmount;

    private boolean hasRegistered;

    public int fuelCost;

    public TileEntityXenonThruster(BlockPos pos, BlockState state) {
        super(StationBlocks.XENON_THRUSTER_BE.get(), pos, state, 0);
        tanks = new FluidTankNTM[] {new FluidTankNTM(Fluids.XENON, 4_000)};
    }

    @Override
    public String getDefaultName() {
        return "container.xenonThruster";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityXenonThruster te) {
        if (!Orbit.inOrbit(level)) return;

        if (!te.hasRegistered) {
            if (te.isFacingPrograde()) te.registerPropulsion();
            te.hasRegistered = true;
            te.isOn = false;
        }

        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();
        te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityXenonThruster te) {
        if (!Orbit.inOrbit(level)) return;
        if (te.isOn) {
            te.thrustAmount += 0.01D;
            if (te.thrustAmount > 1) te.thrustAmount = 1;
        } else {
            te.thrustAmount -= 0.01D;
            if (te.thrustAmount < 0) te.thrustAmount = 0;
        }
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: the three blocks behind the engine (relative to its facing). */
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    /** {@link #getConPos} for a core at {@code core} facing {@code dir}. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x - dir.offsetX - rot.offsetX, y, z - dir.offsetZ - rot.offsetZ, dir),
                new DirPos(x - dir.offsetX, y, z - dir.offsetZ, dir),
                new DirPos(x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ, dir),
        };
    }

    /** 1.7 {@code invalidate} and {@code onChunkUnload}. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (hasRegistered) {
            unregisterPropulsion();
            hasRegistered = false;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeLong(power);
        buf.writeInt(fuelCost);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        power = buf.readLong();
        fuelCost = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("on", isOn);
        output.putLong("power", power);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isOn = input.getBooleanOr("on", false);
        power = input.getLongOr("power", 0L);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }

    public boolean isFacingPrograde() {
        return MachineCore.facing(getBlockState()) == ForgeDirection.WEST;
    }

    /** 1.7 {@code getRenderBoundingBox} (the renderer draws off-screen, so only Forge 1.20.1 asks). */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 2, p.getY() - 1, p.getZ() - 2, p.getX() + 3, p.getY() + 2, p.getZ() + 3);
    }

    /**
     * 1.7 {@code fuelCost * POWER_COST_MULTIPLIER}: an int product, so a burn costing over 429,496 mB overflows (and a
     * negative cost passes the check and adds power on start). Kept as 1.7 computed it.
     */
    public long powerCost() {
        return fuelCost * POWER_COST_MULTIPLIER;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public boolean canPerformBurn(int shipMass, double deltaV) {
        FT_Rocket trait = tanks[0].getTankType().getTrait(FT_Rocket.class);
        int isp = trait != null ? trait.getISP() : 300;

        fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);

        if (power < powerCost()) return false;

        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() < fuelCost) return false;
        }

        return true;
    }

    @Override
    public void addErrors(List<Component> errors) {
        if (power < powerCost()) {
            errors.add(Component.literal(ChatFormatting.RED + " - Insufficient power: needs "
                    + BobMathUtil.getShortNumber(powerCost()) + "HE"));
        }

        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() < fuelCost) {
                errors.add(Component.literal(ChatFormatting.RED + " - Insufficient fuel: needs " + fuelCost + "mB"));
            }
        }
    }

    @Override
    public float getThrust() {
        // but le realisme :((((
        // do not speak to me of realisme, miss I can carry 2,880,000kg of dirt in my fucking pocket
        return 1_400_000;
    }

    @Override
    public int startBurn() {
        isOn = true;
        power -= powerCost();
        for (FluidTankNTM tank : tanks) tank.setFill(tank.getFill() - fuelCost);
        return 200; // ten second prewarm
    }

    @Override
    public int endBurn() {
        isOn = false;
        return 200; // ten second recharge time
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }
}
