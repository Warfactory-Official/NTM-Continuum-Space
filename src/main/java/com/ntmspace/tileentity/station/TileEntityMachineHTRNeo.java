package com.ntmspace.tileentity.station;

import com.hbm.capability.MachineCaps;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.fusion.FusionLink;
import com.hbm.tileentity.machine.fusion.IFusionPowerReceiver;
import com.hbm.util.BobMathUtil;
import com.ntmspace.atmosphere.BlockSealable;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityMachineHTRNeo}, the "HTR-F4 NEO Station Thruster": driven by a running fusion torus's
 * plasma (the torus beam lands ten blocks behind the core, the plasma end of the long axis), cooled like the other
 * Albion machines, and needing a full 200 MHE buffer, all of it spent on each burn. Its fuel is the plasma energy itself:
 * a burn needs as many TU per tick as {@code getFuelCost} gives in mB at a fixed ISP of 250. 1.6 GN of thrust, nine
 * seconds to start and to stop; prograde facing south.
 *
 * <p>Odd but 1.7: unlike the other thrusters this one has no "in orbit" check, so one placed facing south anywhere
 * registers itself with the station whose grid square matches its x/z. Ported as it is.
 */
public class TileEntityMachineHTRNeo extends TileEntityCooledBase implements Propulsion, IFusionPowerReceiver, BlockSealable {

    public long plasmaEnergy;
    public long plasmaEnergySync;

    public static final long maxPower = 200_000_000L;

    public static final int COOLANT_USE = 50;

    public float rotor;
    public float prevRotor;
    public float rotorSpeed;

    public float soundtime;
    private AudioWrapper audio;

    private boolean hasRegistered;

    public boolean isOn;
    public int fuelCost;
    public float thrustAmount;

    public float plasmaR;
    public float plasmaG;
    public float plasmaB;

    public TileEntityMachineHTRNeo(BlockPos pos, BlockState state) {
        super(StationBlocks.HTRF4_NEO_BE.get(), pos, state, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTRNeo te) {
        // 1.7 updateEntity has no inOrbit check here, unlike the other thrusters: the NEO registers with the station of
        // its square wherever it stands (kept; the station travel GameTest uses a square of its own because of it)
        if (!te.hasRegistered) {
            if (te.isFacingPrograde()) te.registerPropulsion();
            te.hasRegistered = true;
            te.isOn = false;
        }

        te.exchangeCoolant(level);
        te.plasmaEnergySync = te.plasmaEnergy;
        te.updateCooling();

        if (te.isOn) {
            te.soundtime++;
            if (te.soundtime == 1) {
                level.playSound(null, pos, StationSounds.HTRF_START.get(), SoundSource.BLOCKS, 1.5F, 1F);
            } else if (te.soundtime > 30) {
                te.soundtime = 30;
            }
        } else {
            te.soundtime--;
            if (te.soundtime == 29) {
                level.playSound(null, pos, StationSounds.HTR_STOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
            } else if (te.soundtime <= 0) {
                te.soundtime = 0;
            }
        }

        // the torus beam lands on the plasma end (1.7 nodespace: a node ten blocks behind the core)
        FusionLink.claim(level, te, te.plasmaPos(), MachineCaps.PLASMA_IN);

        te.setChanged();
        te.networkPackNT();
        te.plasmaEnergy = 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTRNeo te) {
        if (te.power >= maxPower || te.isOn) te.rotorSpeed += 0.125F;
        else te.rotorSpeed -= 0.125F;

        te.rotorSpeed = Mth.clamp(te.rotorSpeed, 0F, 15F);

        te.prevRotor = te.rotor;
        te.rotor += te.rotorSpeed;

        if (te.rotor >= 360F) {
            te.rotor -= 360F;
            te.prevRotor -= 360F;
        }

        if (te.isOn) {
            if (te.soundtime > 28) {
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
            }

            te.thrustAmount += 0.01D;
            if (te.thrustAmount > 1) te.thrustAmount = 1;
        } else {
            if (te.audio != null) {
                te.audio.stopSound();
                te.audio = null;
            }

            te.thrustAmount -= 0.01D;
            if (te.thrustAmount < 0) te.thrustAmount = 0;
        }
    }

    /** Where the torus beam meets this engine: ten blocks back along the long axis. */
    public BlockPos plasmaPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState()).getRotation(ForgeDirection.UP);
        return new BlockPos(worldPosition.getX() + dir.offsetX * -10, worldPosition.getY(), worldPosition.getZ() + dir.offsetZ * -10);
    }

    private AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(StationSounds.HTR_LOOP.get(), SoundSource.BLOCKS, worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), 0.25F, 27.5F, 1.0F, 20);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) FusionLink.release(level, plasmaPos());
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
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
        // genuinely useful method refactor
        plasmaR = r;
        plasmaG = g;
        plasmaB = b;
    }

    /** 1.7 {@code getConPos}: power and coolant at the four corners of the base, two below the core. */
    @Override
    public List<DirPos> getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    /** {@link #getConPos} for a core at {@code core} facing {@code dir}. */
    public static List<DirPos> conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return List.of(
                new DirPos(x - rot.offsetX * 5 - dir.offsetX * 3, y - 2, z - rot.offsetZ * 5 - dir.offsetZ * 3, dir.getOpposite()),
                new DirPos(x - rot.offsetX * -1 - dir.offsetX * 3, y - 2, z - rot.offsetZ * -1 - dir.offsetZ * 3, dir.getOpposite()),
                new DirPos(x - rot.offsetX * 5 - dir.offsetX * -3, y - 2, z - rot.offsetZ * 5 - dir.offsetZ * -3, dir),
                new DirPos(x - rot.offsetX * -1 - dir.offsetX * -3, y - 2, z - rot.offsetZ * -1 - dir.offsetZ * -3, dir));
    }

    @Override
    public boolean canPerformBurn(int shipMass, double deltaV) {
        fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, 250); // i think this engine *itself* would have a base ISP..?

        if (plasmaEnergySync < fuelCost) return false;
        if (power < maxPower) return false;
        if (!isCool()) return false;

        return true;
    }

    @Override
    public void addErrors(List<Component> errors) {
        if (plasmaEnergySync < fuelCost) {
            errors.add(Component.literal(ChatFormatting.RED + "Insufficient plasma energy: needs " + BobMathUtil.getShortNumber(fuelCost) + " TU"));
        }
        if (power < maxPower) {
            errors.add(Component.literal(ChatFormatting.RED + "Insufficient power"));
        }
        if (!isCool()) {
            errors.add(Component.literal(ChatFormatting.RED + "Coolant loop not operational!"));
        }
    }

    @Override
    public float getThrust() {
        return 1_600_000_000.0F;
    }

    @Override
    public int startBurn() {
        isOn = true;
        power = 0;
        return 180;
    }

    @Override
    public int endBurn() {
        isOn = false;
        return 180;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(plasmaEnergySync);
        buf.writeBoolean(isOn);
        buf.writeInt(fuelCost);
        buf.writeFloat(soundtime);
        buf.writeFloat(plasmaR);
        buf.writeFloat(plasmaG);
        buf.writeFloat(plasmaB);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        plasmaEnergy = buf.readLong();
        isOn = buf.readBoolean();
        fuelCost = buf.readInt();
        soundtime = buf.readFloat();
        plasmaR = buf.readFloat();
        plasmaG = buf.readFloat();
        plasmaB = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("plasma", plasmaEnergy);
        output.putBoolean("on", isOn);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        plasmaEnergy = input.getLongOr("plasma", 0L);
        isOn = input.getBooleanOr("on", false);
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    /** 1.7 {@code getRenderBoundingBox} (the renderer draws off-screen, so only Forge 1.20.1 asks). */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 11, p.getY() - 2, p.getZ() - 11, p.getX() + 12, p.getY() + 3, p.getZ() + 12);
    }

    @Override
    public String getDefaultName() {
        return "container.htrfneo";
    }

    public boolean isFacingPrograde() {
        return MachineCore.facing(getBlockState()) == ForgeDirection.SOUTH;
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos) {
        return true;
    }
}
