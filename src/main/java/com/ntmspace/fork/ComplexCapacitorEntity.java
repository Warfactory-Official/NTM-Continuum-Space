package com.ntmspace.fork;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.MachineCapacitorBus;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.EnergyPush;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityCapacitor}. The graph sees it as a receiver only: it subscribed to the input net and
 * delivered by hand, so a provider speed of zero keeps the input net from drawing it back down.
 */
public class ComplexCapacitorEntity extends TileEntityLoadedBase implements IEnergyReceiverMK2, IRORValueProvider {

    public long power;
    protected long maxPower;
    public long powerReceived;
    public long powerSent;
    public long lastPowerReceived;
    public long lastPowerSent;

    public ComplexCapacitorEntity(BlockPos pos, BlockState state) {
        super(ForkBaseChanges.CAPACITOR_COMPLEX_BE.get(), pos, state);
        this.maxPower = state.getBlock() instanceof ComplexCapacitor capacitor ? capacitor.power : 0L;
    }

    /** The side it was placed against, which is the input side. */
    public ForgeDirection inputSide() {
        return ForgeDirection.getOrientation(getBlockState().getValue(ComplexCapacitor.FACING));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ComplexCapacitorEntity capacitor) {
        if (!(level instanceof ServerLevel server)) return;

        capacitor.setupPowerPorts(capacitor.getPorts());
        capacitor.updateAllPorts();
        capacitor.receivePower();

        ForgeDirection dir = capacitor.inputSide().getOpposite();
        BlockPos target = pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);

        // a straight run of buses; a bend ends the run with nothing at the end of it
        boolean didStep = false;
        ForgeDirection last = null;
        while (level.getBlockState(target).getBlock() instanceof MachineCapacitorBus) {
            ForgeDirection current = ForgeDirection.getOrientation(level.getBlockState(target).getValue(MachineCapacitorBus.FACING));
            if (!didStep) last = current;
            didStep = true;
            if (last != current) {
                target = null;
                break;
            }
            target = target.offset(current.offsetX, current.offsetY, current.offsetZ);
        }

        if (target != null && last != null) {
            long toProvide = Math.min(capacitor.getPower(), capacitor.getMaxPower() / 300);
            long accepted = EnergyPush.push(server, target, last, toProvide, capacitor);
            if (accepted > 0) capacitor.usePower(accepted);
        }

        capacitor.networkPackNT();
        capacitor.lastPowerSent = capacitor.powerSent;
        capacitor.lastPowerReceived = capacitor.powerReceived;
        capacitor.powerSent = 0;
        capacitor.powerReceived = 0;
    }

    private PortDef[] cachedPorts;

    /** 1.7 subscribed on the input side only. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = new PortDef[] {PortDef.make(worldPosition, inputSide())};
        return cachedPorts;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(powerReceived);
        buf.writeLong(powerSent);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        power = buf.readLong();
        maxPower = buf.readLong();
        powerReceived = buf.readLong();
        powerSent = buf.readLong();
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (power + this.getPower() <= this.getMaxPower()) {
            if (!simulate) {
                this.setPower(power + this.getPower());
                this.powerReceived += power;
            }
            return 0;
        }
        long capacity = this.getMaxPower() - this.getPower();
        long overshoot = power - capacity;
        if (!simulate) {
            this.powerReceived += capacity;
            this.setPower(this.getMaxPower());
        }
        return overshoot;
    }

    @Override
    public void usePower(long power) {
        this.powerSent += Math.min(this.getPower(), power);
        this.setPower(this.getPower() - power);
    }

    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getProviderSpeed() { return 0L; }
    @Override public long getReceiverSpeed() { return this.getMaxPower() / 100; }
    @Override public ConnectionPriority getPriority() { return ConnectionPriority.LOW; }
    @Override public void setPower(long power) { this.power = power; }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == inputSide();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        long saved = input.getLongOr("maxPower", 0L);
        if (saved > 0) maxPower = saved;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + this.power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (maxPower == 0 ? 0 : this.power * 100 / this.maxPower);
        return null;
    }
}
