package com.ntmspace.tileentity.atmosphere;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityAtmosphericEmitter} ({@code machine_atmo_emitter}, "Atmospheric Emitter"): the terraformer.
 * Its 50,000 mB tank takes any gas set with a fluid identifier; every tick it vents half of what it holds (at least
 * 1 mB) into the body's air ({@code FT_Gaseous.release}) and as pollution, for 10 HE per mB (250,000 HE buffer).
 * Antimatter in the tank blows it up (a 10-strength burning explosion). Client: a motor loop and the gas's plume.
 */
public class TileEntityAtmosphericEmitter extends TileEntityLoadedBase implements IEnergyReceiverMK2, IFluidStandardReceiverMK2 {

    public static final long MAX_POWER = 250_000;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 50_000);
    public long power;

    public float fan = 0;
    public float prevFan = 0;
    private @Nullable AudioWrapper audio;
    private boolean motorSoundActive;

    /** Client: the gas plume (HBM's cooling-tower particle in the gas's colour), set by the client start-up. */
    public static volatile @Nullable Consumer<TileEntityAtmosphericEmitter> CLIENT_PARTICLES;

    public TileEntityAtmosphericEmitter(BlockPos pos, BlockState state) {
        super(AtmosphereMachines.ATMO_EMITTER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAtmosphericEmitter te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();
        te.networkPackNT();

        if (te.tank.getFill() > 0) {
            int toSpill = Math.max(te.tank.getFill() / 2, 1);
            if (te.power >= toSpill * 10L) {
                if (te.tank.getTankType().hasTrait(FluidTraitSimple.FT_Amat.class)) {
                    // 1.7 newExplosion(null, ..., 10F, flaming = true, smoking = true)
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10F, true, Level.ExplosionInteraction.TNT);
                    return;
                }
                te.tank.setFill(te.tank.getFill() - toSpill);
                FluidTrait.onRelease(level, pos, te.tank.getTankType(), FluidReleaseType.SPILL, toSpill);
                CelestialTraits.release(level, te.tank.getTankType(), toSpill);
                te.power -= toSpill * 10L;
            }
        }
        te.motorSoundActive = WorldSounds.machineLoop(level, pos, te.isRunning(), te.motorSoundActive);
        // 1.7 did not mark the tile dirty here; the port saves what changed
        te.setChanged();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityAtmosphericEmitter te) {
        te.prevFan = te.fan;
        if (te.isRunning()) {
            te.fan += 45;
            if (te.fan >= 360) {
                te.fan -= 360;
                te.prevFan -= 360;
            }

            if (te.audio == null) {
                te.audio = te.createAudioLoop();
                if (te.audio != null) te.audio.startSound();
            } else if (!te.audio.isPlaying()) {
                te.audio.stopSound();
                te.audio = te.createAudioLoop();
                if (te.audio != null) te.audio.startSound();
            }
            if (te.audio != null) {
                te.audio.keepAlive();
                te.audio.updateVolume(te.getVolume(0.25F));
            }

            Consumer<TileEntityAtmosphericEmitter> particles = CLIENT_PARTICLES;
            if (te.tank.getFill() > 0 && particles != null) particles.accept(te);
        } else if (te.audio != null) {
            te.audio.stopSound();
            te.audio = null;
        }
    }

    /** 1.7's client test: enough power for this tick's spill (also with an empty tank, which then spins idle). */
    public boolean isRunning() {
        return power >= Math.max(tank.getFill() / 2, 1) * 10L;
    }

    private @Nullable AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(ModSounds.MOTOR.get(), SoundSource.BLOCKS, worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), 0.25F, 10F, 1.0F, 20);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    private PortDef[] cachedPorts;

    /** The 1.7 connection points as ports. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(level, getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /** 1.7 {@code getConPos} (meta - 10: the facing). */
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x + dir.offsetX, y, z + dir.offsetZ, dir),
                new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                new DirPos(x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ, rot),
                new DirPos(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                new DirPos(x - rot.offsetX - dir.offsetX, y, z - rot.offsetZ - dir.offsetZ, rot.getOpposite())
        };
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
        tank.readFromNBT(input, "t");
        power = input.getLongOr("power", 0L);
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

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {tank}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tank}; }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return MAX_POWER; }
}
