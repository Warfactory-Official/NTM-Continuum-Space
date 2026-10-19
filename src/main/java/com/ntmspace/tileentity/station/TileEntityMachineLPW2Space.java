package com.ntmspace.tileentity.station;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModPowerGenBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineLPW2;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.Propulsion;
import com.ntmspace.registry.StationSounds;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's rewrite of {@code TileEntityMachineLPW2}: the LPW-2 as an orbital-station engine. It burns reformed
 * kerosene and oxygen (256,000 mB each, the same amount of both per burn) for 2 GN of thrust ("F1 thrust"), six seconds
 * to start and six to cool. It does nothing at all outside orbit (1.7 {@code updateEntity} returns first), so off orbit
 * the machine stands still: the renderer's clock ({@link #animationTime}) only runs while the engine spins up in orbit.
 * It powers the station only facing south (prograde). Propellant comes in at the two ports either side of the engine,
 * three blocks up.
 *
 * <p>Substituted for HBM's {@code hbm:machine_lpw2} block entity type in {@code SpaceEntry.substitute}; the renderer is
 * HBM's {@code RenderLPW2}, which reads {@link #animationTime}.
 */
public class TileEntityMachineLPW2Space extends TileEntityMachineLPW2 implements Propulsion, IFluidStandardReceiverMK2 {

    public final FluidTankNTM[] tanks;

    public boolean isOn;
    private float speed;
    public double lastTime;
    public double time;
    public float soundtime;
    private AudioWrapper audio;

    private boolean hasRegistered;

    public int fuelCost;

    public TileEntityMachineLPW2Space(BlockPos pos, BlockState state) {
        super(ModPowerGenBlockEntities.LPW2.get(), pos, state);
        tanks = new FluidTankNTM[] {
                new FluidTankNTM(Fluids.KEROSENE_REFORM, 256_000),
                new FluidTankNTM(Fluids.OXYGEN, 256_000)
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineLPW2Space te) {
        if (!Orbit.inOrbit(level)) return;

        if (!te.hasRegistered) {
            if (te.isFacingPrograde()) te.registerPropulsion();
            te.hasRegistered = true;
            te.isOn = false;
        }

        te.setupAllPorts(te.getPorts());
        te.updatePortFIFO();

        if (te.isOn) {
            te.soundtime++;
            if (te.soundtime == 1) {
                level.playSound(null, pos, StationSounds.LPW_START.get(), SoundSource.BLOCKS, 1.5F, 1F);
            } else if (te.soundtime > 20) {
                te.soundtime = 20;
            }
        } else {
            te.soundtime--;
            if (te.soundtime == 19) {
                level.playSound(null, pos, StationSounds.LPW_STOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
            } else if (te.soundtime <= 0) {
                te.soundtime = 0;
            }
        }

        te.setChanged();
        te.networkPackNT(); // 1.7 networkPackNT(250)

        // 1.7 advanced the clock on both sides; the server's speed never leaves 0
        te.lastTime = te.time;
        te.time += te.speed;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineLPW2Space te) {
        if (!Orbit.inOrbit(level)) return;

        if (te.isOn) {
            te.speed += 0.05D;
            if (te.speed > 1) te.speed = 1;

            if (te.soundtime > 18) {
                if (te.audio == null) {
                    te.audio = te.createAudioLoop();
                    if (te.audio != null) te.audio.startSound();
                } else if (!te.audio.isPlaying()) {
                    // 1.7 rebootAudio
                    te.audio.stopSound();
                    te.audio = te.createAudioLoop();
                    if (te.audio != null) te.audio.startSound();
                }

                if (te.audio != null) {
                    te.audio.updateVolume(te.getVolume(1F));
                    te.audio.keepAlive();
                }

                ForgeDirection dir = MachineCore.facing(state).getRotation(ForgeDirection.UP);
                // 1.7 effectNT "missileContrail", scale 3, out of the bell along the long axis
                com.hbm.client.ClientEffects.spawnRocketFlame(level, pos.getX() + dir.offsetX * 8.5, pos.getY() + 4.5,
                        pos.getZ() + dir.offsetZ * 8.5, 3, dir.offsetX * 10, 0, dir.offsetZ * 10, 20 + level.getRandom().nextInt(20));
            }
        } else {
            te.speed -= 0.05D;
            if (te.speed < 0) te.speed = 0;

            if (te.audio != null) {
                te.audio.stopSound();
                te.audio = null;
            }
        }

        te.lastTime = te.time;
        te.time += te.speed;
    }

    /** hbm-space's {@code RenderLPW2}: the moving parts follow the engine's spin-up clock instead of world time. */
    @Override
    public double animationTime(float partialTicks) {
        return lastTime + (time - lastTime) * partialTicks;
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: four blocks out either side of the engine, one back along the long axis, three up. */
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    /** {@link #getConPos} for a core at {@code core} facing {@code dir}. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x + dir.offsetX * 4 - rot.offsetX, y + 3, z + dir.offsetZ * 4 - rot.offsetZ, rot),
                new DirPos(x - dir.offsetX * 4 - rot.offsetX, y + 3, z - dir.offsetZ * 4 - rot.offsetZ, rot.getOpposite())
        };
    }

    private AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(StationSounds.LPW_LOOP.get(), SoundSource.BLOCKS, worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), 0.25F, 27.5F, 1.0F, 20);
    }

    /** 1.7 {@code invalidate} and {@code onChunkUnload}. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (hasRegistered) {
            unregisterPropulsion();
            hasRegistered = false;
        }
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeFloat(soundtime);
        buf.writeInt(fuelCost);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        soundtime = buf.readFloat();
        fuelCost = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("on", isOn);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isOn = input.getBooleanOr("on", false);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }

    public boolean isFacingPrograde() {
        return MachineCore.facing(getBlockState()) == ForgeDirection.SOUTH;
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

        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() < fuelCost) return false;
        }

        return true;
    }

    @Override
    public void addErrors(List<Component> errors) {
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() < fuelCost) {
                errors.add(Component.literal(ChatFormatting.RED + " - Insufficient fuel: needs " + fuelCost + "mB"));
            }
        }
    }

    @Override
    public float getThrust() {
        return 2_000_000_000.0F; // F1 thrust
    }

    @Override
    public int startBurn() {
        isOn = true;
        for (FluidTankNTM tank : tanks) tank.setFill(tank.getFill() - fuelCost);
        return 120;
    }

    @Override
    public int endBurn() {
        isOn = false;
        return 120; // Cooldown
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }
}
