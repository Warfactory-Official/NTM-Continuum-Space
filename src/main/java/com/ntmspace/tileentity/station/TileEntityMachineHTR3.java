package com.ntmspace.tileentity.station;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.Propulsion;
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
 * hbm-space's {@code TileEntityMachineHTR3}, the "LPW-3N Station Thruster": a nuclear thermal rocket fed with a
 * PWR-heated rocket propellant (superheated hydrogen by default, 1,280,000 mB) at the end of its long axis; 800 MN of
 * thrust, four seconds to start and to stop. It works facing south (prograde) in orbit only. The bell sways with
 * {@link #time}, which runs while it burns.
 */
public class TileEntityMachineHTR3 extends TileEntityMachineBase implements Propulsion, IFluidStandardReceiverMK2 {

    public final FluidTankNTM[] tanks;

    public boolean isOn;
    private float speed;
    public double lastTime;
    public double time;
    public float soundtime;
    private AudioWrapper audio;

    private boolean hasRegistered;

    public int fuelCost;

    public TileEntityMachineHTR3(BlockPos pos, BlockState state) {
        super(StationBlocks.HTR3_BE.get(), pos, state, 0);
        tanks = new FluidTankNTM[] {new FluidTankNTM(SpaceFluids.SUPERHEATED_HYDROGEN, 1_280_000)};
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTR3 te) {
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
                level.playSound(null, pos, StationSounds.HTR_START.get(), SoundSource.BLOCKS, 1.5F, 1F);
            } else if (te.soundtime > 20) {
                te.soundtime = 20;
            }
        } else {
            te.soundtime--;
            if (te.soundtime == 19) {
                level.playSound(null, pos, StationSounds.HTR_STOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
            } else if (te.soundtime <= 0) {
                te.soundtime = 0;
            }
        }

        te.setChanged();
        te.networkPackNT();
        te.lastTime = te.time;
        te.time += te.speed;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTR3 te) {
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
                // 1.7 effectNT "missileContrail" (MUD/SCH/UP variants for the bromide fuels: a particle port's colours)
                com.hbm.client.ClientEffects.spawnRocketFlame(level, pos.getX() + dir.offsetX * 7, pos.getY() + 1,
                        pos.getZ() + dir.offsetZ * 7, 3, dir.offsetX * 10, 0, dir.offsetZ * 10, 40 + level.getRandom().nextInt(40));
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

    /** 1.7 {@code getContrailType}: the exhaust colour of the fuel. */
    public static String getContrailType(FluidType type) {
        if (type == SpaceFluids.GAS_WATZ || type == com.hbm.inventory.fluid.Fluids.WASTEGAS || type == SpaceFluids.GASEOUS_THORIUM_BROMIDE) return "missileContrailMUD";
        if (type == SpaceFluids.GASEOUS_SCHRABIDIUM_BROMIDE) return "missileContrailSCH";
        if (type == SpaceFluids.GASEOUS_URANIUM_BROMIDE || type == SpaceFluids.GASEOUS_PLUTONIUM_BROMIDE) return "missileContrailUP";
        return "missileContrail";
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: one point, six blocks out along the long axis. */
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    /** {@link #getConPos} for a core at {@code core} facing {@code facing}. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection facing) {
        ForgeDirection dir = facing.getRotation(ForgeDirection.UP);
        return new DirPos[] {new DirPos(core.getX() - dir.offsetX * 6, core.getY(), core.getZ() - dir.offsetZ * 6, dir)};
    }

    private AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(StationSounds.HTR_LOOP.get(), SoundSource.BLOCKS, worldPosition.getX(), worldPosition.getY(),
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

    /** 1.7 {@code getRenderBoundingBox} (the renderer draws off-screen, so only Forge 1.20.1 asks). */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 10, p.getY() - 3, p.getZ() - 10, p.getX() + 11, p.getY() + 4, p.getZ() + 11);
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
        return 800_000_000.0F;
    }

    @Override
    public int startBurn() {
        isOn = true;
        for (FluidTankNTM tank : tanks) tank.setFill(tank.getFill() - fuelCost);
        return 80;
    }

    @Override
    public int endBurn() {
        isOn = false;
        return 80; // Cooldown
    }

    @Override
    public String getDefaultName() {
        return "container.htr";
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
