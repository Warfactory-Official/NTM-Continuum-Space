package com.ntmspace.tileentity.dyson;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.dyson.DysonConverter;
import com.ntmspace.registry.DysonContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityDysonConverterHE}, the Dyson Power Converter: a beam entering its intake (four ahead of
 * the core, one up) sets its buffer to the beam's energy, which it provides out of its back (the power cell four behind
 * the core) to whatever is connected there. Every second tick while it holds power it throws {@code "tau"} sparks (every
 * tenth tick {@code "hadron"} flashes) at four points over the coils; after ten such ticks without a new beam it empties
 * its buffer - "to prevent this machine acting like an endgame battery, but still be able to transmit every drop of
 * power". Its maximum power is its current power (1.7, kept: a network can only ever take what it holds).
 */
public class TileEntityDysonConverterHE extends TileEntityLoadedBase implements DysonConverter, IEnergyProviderMK2 {

    public long power;

    public boolean isConverting;
    private int cooldown;

    public TileEntityDysonConverterHE(BlockPos pos, BlockState state) {
        super(DysonContent.CONVERTER_HE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDysonConverterHE te) {
        te.setupPowerPorts(te.getPorts());
        te.updateAllPorts();
        te.providePower();
        te.step(level);
    }

    /** The power output: 1.7 {@code DirPos(core + dir * 5, dir)} with {@code dir} the facing's opposite. */
    public static DirPos output(BlockPos core, ForgeDirection facing) {
        ForgeDirection dir = facing.getOpposite();
        return new DirPos(core.getX() + dir.offsetX * 5, core.getY(), core.getZ() + dir.offsetZ * 5, dir);
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(level, getBlockState(), worldPosition, output(worldPosition, MachineCore.facing(getBlockState())));
        return cachedPorts;
    }

    public void step(Level level) {
        ForgeDirection dir = MachineCore.facing(getBlockState()).getOpposite();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        isConverting = power > 0;

        if (isConverting && level.getGameTime() % 2 == 0) {
            HbmEffectNT effect = level.getGameTime() % 10 == 0 ? HbmEffectNT.Hadron : HbmEffectNT.Tau;
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
            spark(level, effect, x + dir.offsetX * 4 + rot.offsetX, y + 2.25, z + dir.offsetZ * 4 + rot.offsetZ);
            spark(level, effect, x + dir.offsetX * 4 - rot.offsetX, y + 2.25, z + dir.offsetZ * 4 - rot.offsetZ);
            spark(level, effect, x + dir.offsetX * 3 + rot.offsetX, y + 2.75, z + dir.offsetZ * 3 + rot.offsetZ);
            spark(level, effect, x + dir.offsetX * 3 - rot.offsetX, y + 2.75, z + dir.offsetZ * 3 - rot.offsetZ);

            cooldown++;
            if (cooldown > 10) {
                // To prevent this machine acting like an endgame battery, but still be able to transmit every drop of power
                // this machine will clear its buffers (almost) immediately after transmitting power
                power = 0;
                cooldown = 0;
            }
        }

        networkPackNT();
    }

    /** 1.7 {@code AuxParticlePacketNT} of type tau/hadron with {@code count} 1, to everyone within 25 of the core. */
    private void spark(Level level, HbmEffectNT effect, double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putByte("count", (byte) 1);
        double cx = worldPosition.getX(), cy = worldPosition.getY(), cz = worldPosition.getZ();
        effect.sendAround(level, x, y, z, data, cx, cy, cz, 25);
    }

    @Override
    public boolean provideEnergy(int x, int y, int z, long energy) {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        int rx = worldPosition.getX() + dir.offsetX * 4;
        int ry = worldPosition.getY() + 1;
        int rz = worldPosition.getZ() + dir.offsetZ * 4;

        if (x != rx || y != ry || z != rz) return false;

        power = energy;
        cooldown = 0;

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
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isConverting = buf.readBoolean();
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return power; }
}
