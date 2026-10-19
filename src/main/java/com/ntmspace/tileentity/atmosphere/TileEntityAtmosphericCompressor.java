package com.ntmspace.tileentity.atmosphere;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityAtmosphericCompressor} ({@code machine_atmo_vent}, "Atmospheric Compressor"): outdoors on a
 * body with air it sucks 200 mB a tick of one of the air's gases (the main one unless a screwdriver or a fluid identifier
 * picked another) into a 50,000 mB tank, taking those 200 mB out of the body's atmosphere, for 20,000 HE (1/50 of its
 * 1,000,000 HE buffer). In a sealed room, in orbit or in vacuum the tank's type is none and it does nothing. It hands the
 * gas out at the same eight points as the deuterium tower whose frame it shares.
 */
public class TileEntityAtmosphericCompressor extends TileEntityMachineBase implements IEnergyReceiverMK2, IFluidStandardSenderMK2 {

    public static final long MAX_POWER = 1_000_000;
    /** 1.7 {@code consumption}: an unused field (the step uses 200 and {@code max / 50}); kept for the record. */
    int consumption = 200;

    public float rot;
    public float prevRot;
    private float rotSpeed;
    public long power = 0;
    public final FluidTankNTM tank = new FluidTankNTM(SpaceFluids.EARTHAIR, 50000);

    public TileEntityAtmosphericCompressor(BlockPos pos, BlockState state) {
        super(AtmosphereMachines.ATMO_VENT_BE.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "container.air";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAtmosphericCompressor te) {
        te.updateServer(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityAtmosphericCompressor te) {
        float maxSpeed = 30F;
        if (te.hasPower()) {
            te.rotSpeed += 0.2F;
            if (te.rotSpeed > maxSpeed) te.rotSpeed = maxSpeed;
        } else {
            te.rotSpeed -= 0.1F;
            if (te.rotSpeed < 0) te.rotSpeed = 0;
        }
        te.prevRot = te.rot;
        te.rot += te.rotSpeed;
        if (te.rot >= 360) {
            te.rot -= 360;
            te.prevRot -= 360;
        }
    }

    void updateServer(Level level) {
        setupAllPorts(getPorts());
        updatePortPIFIFO();

        // Extractors will not work indoors (or in space oops)
        CBT_Atmosphere atmosphere = !Environment.inOrbit(level) && !AtmosphereHandler.hasAtmosphere(level, worldPosition)
                ? CelestialTraits.getTrait(level, CBT_Atmosphere.class)
                : null;

        if (atmosphere != null && atmosphere.getPressure() > 0.0D) {
            // If the atmosphere doesn't contain the fluid we're sucking up, pick a new one
            if (!atmosphere.hasFluid(tank.getTankType())) tank.setTankType(atmosphere.getMainFluid());
        } else {
            tank.setTankType(Fluids.NONE);
        }

        if (hasPower() && tank.getTankType() != Fluids.NONE && tank.getFill() + 200 <= tank.getMaxFill()) {
            tank.setFill(tank.getFill() + 200);
            power -= getMaxPower() / 50;
            CelestialTraits.capture(level, tank.getTankType(), 200);
        }

        setChanged();
        networkPackNT();
    }

    /** 1.7 {@code cycleGas} (screwdriver): the next gas of the air, thickest first, back to the first past the end or a trace. */
    public void cycleGas() {
        CBT_Atmosphere atmosphere = air();
        if (atmosphere == null) return;
        // 1.7 sorted the live trait in place; a copy here (the order is not saved, and the live one must not change)
        atmosphere = atmosphere.copy();
        atmosphere.sortDescending();
        FluidType currentFluid = tank.getTankType();
        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            if (atmosphere.fluids.get(i).fluid == currentFluid) {
                int targetIndex = i + 1;
                if (targetIndex >= atmosphere.fluids.size()) targetIndex = 0;
                if (atmosphere.fluids.get(targetIndex).pressure <= 0.0001) targetIndex = 0;
                tank.setTankType(atmosphere.fluids.get(targetIndex).fluid);
                break;
            }
        }
    }

    /** 1.7 {@code switchGas} (fluid identifier): only a gas the air has; false when nothing changed. */
    public boolean switchGas(FluidType targetFluid) {
        if (targetFluid == tank.getTankType()) return false;
        CBT_Atmosphere atmosphere = air();
        if (atmosphere == null) return false;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry.fluid == targetFluid) {
                tank.setTankType(targetFluid);
                return true;
            }
        }
        return false;
    }

    /** 1.7: the body's air unless the compressor stands in a sealed room (no orbit check here, as in 1.7). */
    private @Nullable CBT_Atmosphere air() {
        if (level == null) return null;
        return !AtmosphereHandler.hasAtmosphere(level, worldPosition) ? CelestialTraits.getTrait(level, CBT_Atmosphere.class) : null;
    }

    public boolean hasPower() {
        return power >= getMaxPower() / 50;
    }

    private PortDef[] cachedPorts;

    /** The 1.7 connection points as ports. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /** The deuterium tower's eight points (1.7 copied its {@code getConPos}). */
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x + dir.offsetX, y, z + dir.offsetZ, dir),
                new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),
                new DirPos(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                new DirPos(x - dir.offsetX - rot.offsetX, y, z - dir.offsetZ - rot.offsetZ, rot.getOpposite()),
                new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                new DirPos(x - dir.offsetX + rot.offsetX * 2, y, z - dir.offsetZ + rot.offsetZ * 2, rot),
        };
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tank.writeToNBT(output, "water"); // 1.7's key
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tank.readFromNBT(input, "water");
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return MAX_POWER; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {tank}; }
}
