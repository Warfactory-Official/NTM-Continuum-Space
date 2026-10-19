package com.ntmspace.tileentity.atmosphere;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.ntmspace.atmosphere.AtmosphereBlob;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.tileentity.TileEntityAirPump;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityAirScrubber} ({@code air_scrubber}, "CO2 Scrubber"): powered (more than 200 HE of its
 * 10,000), it finds the air vent of the sealed room it stands in every 5 ticks and claims that vent's waste gas; the vent
 * hands it up to 20 mB a tick, which the scrubber stores as CO2 (16,000 mB) for 10 HE per mB and sends out on all six
 * sides. A vent serves one powered scrubber at a time, and only while it breathes out oxygen.
 */
public class TileEntityAirScrubber extends TileEntityMachineBase implements IFluidStandardSenderMK2, IEnergyReceiverMK2,
        TileEntityAirPump.CO2Scrubber {

    public static final long MAX_POWER = 10000;

    private @Nullable TileEntityAirPump pump;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.CARBONDIOXIDE, 16_000);

    private long power;

    public float rot;
    public float prevRot;
    private float rotSpeed;

    public TileEntityAirScrubber(BlockPos pos, BlockState state) {
        super(AtmosphereMachines.AIR_SCRUBBER_BE.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "container.airScrubber";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAirScrubber te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        if (te.canOperate()) {
            // Fetch a new pump to scrub CO2 from
            if (level.getGameTime() % 5 == 0 && (te.pump == null || te.pump.getFluidPressure() == 0 || !te.pump.registerScrubber(te))) {
                te.pump = null;
                List<AtmosphereBlob> blobs = AtmosphereHandler.getBlobs(level, pos);
                for (AtmosphereBlob blob : blobs) {
                    if (blob != null) {
                        BlockEntity root = level.getBlockEntity(blob.getRootPosition());
                        if (root instanceof TileEntityAirPump candidate) {
                            te.pump = candidate;
                            if (!candidate.registerScrubber(te)) {
                                te.pump = null;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        }

        te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityAirScrubber te) {
        float maxSpeed = 30F;
        if (te.canOperate()) {
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

    private PortDef[] cachedPorts;

    /** 1.7: power in and CO2 out on all six sides. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return cachedPorts;
    }

    @Override
    public boolean canOperate() {
        return power > 200;
    }

    /** 1.7 {@code scrub}: takes as much of {@code amount} as the tank holds, 10 HE per mB; returns what it took. */
    @Override
    public int scrub(int amount) {
        if (!canOperate()) return 0;
        int add = Math.min(tank.getMaxFill() - tank.getFill(), amount);
        tank.setFill(tank.getFill() + add);
        power -= add * 10L;
        return add;
    }

    /** The vent this scrubber serves, or null (tests). */
    public @Nullable TileEntityAirPump pump() {
        return pump;
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
        tank.writeToNBT(output, "t");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tank.readFromNBT(input, "t");
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {tank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tank}; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
}
