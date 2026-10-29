package com.ntmspace.fork;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.IBatteryItem;
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

    /**
     * The side it was placed against, which is the input side.
     */
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
            ForgeDirection current = ForgeDirection.getOrientation(
                    level.getBlockState(target).getValue(MachineCapacitorBus.FACING));
            if (!didStep) last = current;
            didStep = true;
            if (last != current) {
                target = null;
                break;
            }
            target = target.offset(current.offsetX, current.offsetY, current.offsetZ);
        }

        if (target != null && last != null) {
            long toProvide = Math.min(capacitor.getPower(), capacitor.getBusProviderSpeed());
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

    /**
     * 1.7 subscribed on the input side only.
     */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = new PortDef[]{PortDef.make(worldPosition, inputSide())};
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
        var stored = buf.readLong();
        setMaximum(buf.readLong());
        setPower(stored);
        powerReceived = buf.readLong();
        powerSent = buf.readLong();
    }

    @Override
    public long transferPower(long amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative energy transfer");
        var next = IBatteryItem.adjustCharge(power, amount, maxPower, false);
        var accepted = next - power;
        if (!simulate && accepted > 0) {
            setPower(next);
            powerReceived = IBatteryItem.adjustCharge(powerReceived, accepted, Long.MAX_VALUE, false);
        }
        return amount - accepted;
    }

    @Override
    public void usePower(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative energy debit");
        var taken = Math.min(power, amount);
        if (taken == 0) return;
        powerSent = IBatteryItem.adjustCharge(powerSent, taken, Long.MAX_VALUE, false);
        setPower(power - taken);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public long getProviderSpeed() {
        return 0L;
    }

    /**
     * Maximum rate sent through the capacitor bus by {@link #serverTick}.
     */
    public long getBusProviderSpeed() {
        return getMaxPower() / 300;
    }

    @Override
    public long getReceiverSpeed() {
        return this.getMaxPower() / 100;
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public void setPower(long power) {
        this.power = IBatteryItem.clampCharge(power, maxPower);
        setChanged();
        dataChanged();
    }

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
        setMaximum(input.getLongOr("maxPower", maxPower));
        setPower(input.getLongOr("power", 0L));
    }

    /**
     * Restores the actual carried ceiling before bounding its stored charge.
     */
    public void restore(CapacitorCharge state) {
        setMaximum(state.maxPower());
        setPower(state.power());
    }

    private void setMaximum(long value) {
        if (value <= 0) throw new IllegalArgumentException("A complex capacitor needs positive capacity");
        maxPower = Math.min(value, ((ComplexCapacitor) getBlockState().getBlock()).power);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + this.power;
        if ((PREFIX_VALUE + "fillpercent").equals(name))
            return "" + (maxPower == 0 ? 0 : CapacitorCharge.fillPercent(power, maxPower));
        return null;
    }
}
