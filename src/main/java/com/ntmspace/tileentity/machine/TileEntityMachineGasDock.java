package com.ntmspace.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityMachineGasDock} (Gas Giant Harvester): on a body whose planet is a gas giant, a fuelled
 * scoop rocket (500 mB hydrogen + 500 mB oxygen) flies up, scoops 8,000 mB of the giant's gas into the 64,000 mB tank,
 * and comes back. The rocket leaves 20 ticks after it last landed ({@code launchTicks} -20) and is back at 100. It takes
 * hydrogen and oxygen (32,000 mB each) and gives the gas at every port.
 */
public class TileEntityMachineGasDock extends TileEntityMachineBase implements IFluidStandardTransceiverMK2 {

    /** 1.7 {@code CelestialBody.withGas}: the planets that have a gas (Jool, Sarnus, Urlum, Neidon). */
    private static final Map<String, FluidType> GAS = Map.of(
            "jool", SpaceFluids.JOOLGAS,
            "sarnus", SpaceFluids.SARNUSGAS,
            "urlum", SpaceFluids.UGAS,
            "neidon", SpaceFluids.NGAS);

    public final FluidTankNTM[] tanks;

    public boolean hasRocket = true;
    public int launchTicks = 0;

    public TileEntityMachineGasDock(BlockPos pos, BlockState state) {
        super(SpaceMachinesC.GAS_DOCK_BE.get(), pos, state, 0);
        tanks = new FluidTankNTM[3];
        tanks[0] = new FluidTankNTM(SpaceFluids.JOOLGAS, 64_000);
        tanks[1] = new FluidTankNTM(Fluids.HYDROGEN, 32_000);
        tanks[2] = new FluidTankNTM(Fluids.OXYGEN, 32_000);
    }

    @Override
    public String getDefaultName() {
        return "container.gasDock";
    }

    /** The gas of {@code body}'s planet (1.7 {@code CelestialBody.getPlanet(world).gas}), or null. */
    public static FluidType gasOf(String body) {
        return GAS.get(CelestialTraits.planetOf(body));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasDock te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortFIFO();
        te.step(level, CelestialTraits.bodyOf(level));
    }

    /** One 1.7 {@code updateEntity} on the server, on {@code body}. */
    public void step(Level level, String body) {
        FluidType gas = gasOf(body);

        launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);

        if (gas != null) {
            tanks[0].setTankType(gas);

            if (hasFuel() && launchTicks <= -20) {
                hasRocket = false;
                collectGas();
            } else if (launchTicks >= 100) {
                hasRocket = true;
            }
        }

        setChanged();
        networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasDock te) {
        te.launchTicks = Mth.clamp(te.launchTicks + (te.hasRocket ? -1 : 1), te.hasRocket ? -20 : 0, 100);
        if (te.launchTicks > 0 && te.launchTicks < 100) {
            // 1.7 ParticleUtil.spawnGasFlame on the client: one flame below the rising rocket, falling
            CompoundTag flame = new CompoundTag();
            flame.putDouble("moX", 0.0);
            flame.putDouble("moY", -1.0);
            flame.putDouble("moZ", 0.0);
            HbmEffectNT.GasFlame.summonParticle(level, pos.getX() + 0.5, pos.getY() + 0.5 + te.launchTicks, pos.getZ() + 0.5, flame);

            if (te.launchTicks < 10) {
                // 1.7 ExplosionLarge.spawnShock on the client: smoke mode "shock", half a block up
                CompoundTag shock = new CompoundTag();
                shock.putInt("count", 1 + level.getRandom().nextInt(3));
                shock.putDouble("strength", 1 + level.getRandom().nextGaussian());
                HbmEffectNT.Smoke_Shock.summonParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, shock);
            }
        }
    }

    private void collectGas() {
        if (tanks[1].getFill() < 500) return;
        if (tanks[2].getFill() < 500) return;
        if (tanks[0].getFill() + 8000 > tanks[0].getMaxFill()) return;

        tanks[1].setFill(tanks[1].getFill() - 500);
        tanks[2].setFill(tanks[2].getFill() - 500);
        tanks[0].setFill(tanks[0].getFill() + 8000);
    }

    private boolean hasFuel() {
        return tanks[1].getFill() >= 500 && tanks[2].getFill() >= 500;
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, conPos(worldPosition));
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: the 3x3 below the pad, then the three blocks beyond each side (world axes). */
    public static DirPos[] conPos(BlockPos core) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        List<DirPos> list = new ArrayList<>();
        // Below
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) list.add(new DirPos(x + dx, y - 1, z + dz, ForgeDirection.DOWN));
        }
        // Sides
        for (int i = -1; i <= 1; i++) {
            list.add(new DirPos(x + i, y, z + 2, ForgeDirection.SOUTH));
            list.add(new DirPos(x + i, y, z - 2, ForgeDirection.NORTH));
            list.add(new DirPos(x + 2, y, z + i, ForgeDirection.EAST));
            list.add(new DirPos(x - 2, y, z + i, ForgeDirection.WEST));
        }
        return list.toArray(new DirPos[0]);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(hasRocket);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        hasRocket = buf.readBoolean();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        // 1.7's readFromNBT wrote "hasRocker" and its writeToNBT read it (swapped): the flag was never saved, so a
        // reloaded dock always starts with its rocket home. Kept: not saved.
        tanks[0].writeToNBT(output, "gas");
        tanks[1].writeToNBT(output, "f1");
        tanks[2].writeToNBT(output, "f2");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "gas");
        tanks[1].readFromNBT(input, "f1");
        tanks[2].readFromNBT(input, "f2");
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 1, p.getZ() + 2);
    }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[0]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tanks[1], tanks[2]}; }
}
