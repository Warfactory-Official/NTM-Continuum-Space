package com.ntmspace.tileentity.station;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.RocketFlameColor;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.Propulsion;
import com.ntmspace.particle.RocketFlames;
import com.ntmspace.registry.StationBlocks;
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
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityMachineHTRF4}, the legacy "HTR-F4 Station Thruster": a plasma drive with a 64,000 mB
 * deuterium-tritium plasma tank, 250,000 HE per mB burned (1 GHE buffer), 1.6 GN of thrust, nine seconds to start and to
 * stop; prograde facing south, orbit only.
 *
 * <p>Odd but 1.7: nothing fills the plasma tank. 1.7's comment says "plasma heater filling handled separately", but
 * hbm-space has no plasma heater any more and the engine subscribes its connection points to power only; it is the
 * "(LEGACY)" thruster, superseded by the NEO. Ported as it is: power connects, plasma does not.
 */
public class TileEntityMachineHTRF4 extends TileEntityMachineBase implements Propulsion, IFluidStandardReceiverMK2, IEnergyReceiverMK2, OrbitConnector {

    public final FluidTankNTM[] tanks;

    public long power;
    public static final long maxPower = 1_000_000_000;

    public static final int POWER_COST_MULTIPLIER = 250_000;

    public boolean isOn;
    private float speed;
    public double lastTime;
    public double time;
    public float soundtime;
    private AudioWrapper audio;

    private boolean hasRegistered;

    public int fuelCost;

    public TileEntityMachineHTRF4(BlockPos pos, BlockState state) {
        super(StationBlocks.HTRF4_BE.get(), pos, state, 0);
        tanks = new FluidTankNTM[]{new FluidTankNTM(Fluids.PLASMA_DT, 64000)};
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTRF4 te) {
        if (!Orbit.inOrbit(level)) return;

        if (!te.hasRegistered) {
            if (te.isFacingPrograde()) te.registerPropulsion();
            te.hasRegistered = true;
            te.isOn = false;
        }

        // Only fill power, plasma heater filling handled separately (see the class comment)
        te.setupPowerPorts(te.getPorts());
        te.updateAllPorts();
        te.receivePower();

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
        te.networkPackNT();
        te.lastTime = te.time;
        te.time += te.speed;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTRF4 te) {
        if (!Orbit.inOrbit(level)) return;

        if (te.isOn) {
            te.speed += 0.05D;
            if (te.speed > 1) te.speed = 1;

            if (te.soundtime > 18) {
                if (te.audio == null) {
                    te.audio = te.createAudioLoop();
                    if (te.audio != null) te.audio.startSound();
                } else if (!te.audio.isPlaying()) {
                    te.audio.stopSound();
                    te.audio = te.createAudioLoop();
                    if (te.audio != null) te.audio.startSound();
                }
                if (te.audio != null) {
                    te.audio.updateVolume(te.getVolume(1F));
                    te.audio.keepAlive();
                }

                ForgeDirection dir = MachineCore.facing(state).getRotation(ForgeDirection.UP);
                com.hbm.client.ClientEffects.spawnRocketFlame(level, pos.getX() + dir.offsetX * 12, pos.getY() + 1,
                        pos.getZ() + dir.offsetZ * 12, 3, dir.offsetX * 10, 0, dir.offsetZ * 10,
                        40 + level.getRandom().nextInt(40), te.contrailType());
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

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(level, getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /**
     * 1.7 {@code getConPos}: the two blocks either side of the far end of the long axis (power).
     */
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    /**
     * {@link #getConPos} for a core at {@code core} facing {@code dir}.
     */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[]{
                new DirPos(x - rot.offsetX * 10 + dir.offsetX, y, z - rot.offsetZ * 10 + dir.offsetZ, rot),
                new DirPos(x - rot.offsetX * 10 - dir.offsetX, y, z - rot.offsetZ * 10 - dir.offsetZ, rot),
        };
    }

    private AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(StationSounds.LPW_LOOP.get(), SoundSource.BLOCKS, worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(), 0.25F, 27.5F, 1.0F, 20);
    }

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
        buf.writeLong(power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        soundtime = buf.readFloat();
        fuelCost = buf.readInt();
        power = buf.readLong();
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
        return MachineCore.facing(getBlockState()) == ForgeDirection.SOUTH;
    }

    /**
     * 1.7 {@code getRenderBoundingBox} (the renderer draws off-screen, so only Forge 1.20.1 asks).
     */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 11, p.getY() - 2, p.getZ() - 11, p.getX() + 12, p.getY() + 3, p.getZ() + 12);
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    /**
     * 1.7 {@code fuelCost * POWER_COST_MULTIPLIER}: an int product, overflowing past 8,589 mB, kept as 1.7 computed it.
     */
    public long powerCost() {
        return fuelCost * POWER_COST_MULTIPLIER;
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
            errors.add(Component.literal(
                    ChatFormatting.RED + " - Insufficient power: needs " + BobMathUtil.getShortNumber(
                            powerCost()) + "HE"));
        }
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() < fuelCost) {
                errors.add(Component.literal(ChatFormatting.RED + " - Insufficient fuel: needs " + fuelCost + "mB"));
            }
        }
    }

    @Override
    public float getThrust() {
        return 1_600_000_000.0F; // F1 thrust
    }

    @Override
    public int startBurn() {
        isOn = true;
        power -= powerCost();
        for (FluidTankNTM tank : tanks) tank.setFill(tank.getFill() - fuelCost);
        return 180;
    }

    @Override
    public int endBurn() {
        isOn = false;
        return 180; // Cooldown
    }

    @Override
    public String getDefaultName() {
        return "container.htrf4";
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    /**
     * 1.7 never subscribed the tank (see the class comment): nothing is received.
     */
    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[0];
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

    public RocketFlameColor contrailType() {
        return tanks[0].getTankType() == Fluids.PLASMA_BF
                ? RocketFlames.BALEFIRE : RocketFlames.FUSION;
    }
}
