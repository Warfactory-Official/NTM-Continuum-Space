package com.ntmspace.tileentity.dyson;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.items.ISatChip;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CBT_Dyson;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityDysonLauncher}: a spinlauncher that throws swarm members into solar orbit. With a
 * satellite ID-chip (the swarm's number, slot 1), at least one swarm member (slot 0, at most four) and a full charge
 * per tick, it spins up for 132 ticks drawing {@code 20 MHE / 132} each tick, then launches up to four members into the
 * star's swarm ({@link CBT_Dyson#launch}) and spins down for 68 ticks. Sunset overdrive (a FATO plushie) spins up in
 * 38 ticks and down in 12, at {@code 20 MHE / 38} a tick. It pulls swarm members one at a time out of an inventory nine
 * blocks behind it.
 *
 * <p>The launch's sounds are as loud as the air is thick ({@code min(16 * (1 - (1 - p)^3), 4)}); its exhaust burst
 * (1.7 {@code "spinlaunch"}: {@code min(20, 80p)} rocket-flame puffs nine to the side, twelve up) is sent as 1.7 sent
 * it, to everyone within 150 blocks at that moment: the effect {@link #SPINLAUNCH} in HBM's particle payload.
 */
public class TileEntityDysonLauncher extends TileEntityMachineBase implements IEnergyReceiverMK2 {

    public static final int SLOT_MEMBERS = 0;
    public static final int SLOT_CHIP = 1;

    public int swarmId;
    public int swarmCount;

    public long power;
    public static final long MAX_POWER = 20_000_000;

    public static final int MEMBERS_PER_LAUNCH = 4;

    // SHAKE IT LIKE IT'S HEAT, OVERDRIVE
    public boolean sunsetOverdrive = false;

    public boolean isOperating;
    public boolean isSpinningDown;
    public int operatingTime;

    public float rotation;
    public float lastRotation;
    public float speed;

    public int payloadTicks;

    /** The member count in slot 0 as the client last heard it (1.7 {@code satCount}). */
    public int satCount;

    /**
     * hbm-space's overdrive item, its FATO plushie ({@code BlockPlushie.PlushieType.FATO}, hidden from creative). The
     * hbm-space plushies are not part of this family; by default any item whose id path is {@code plushie_fato}
     * qualifies, so the plushie works as soon as it is registered under that name.
     */
    public static volatile Predicate<ItemStack> OVERDRIVE =
            stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("plushie_fato");

    /**
     * 1.7's {@code "spinlaunch"} aux particle: tag {@code count}, {@code scale}, {@code moX/moY/moZ}, {@code maxAge};
     * the client's handler is {@code DysonClient.launchBurst}.
     */
    public static final com.hbm.particle.helper.HbmEffectNT SPINLAUNCH =
            com.hbm.particle.helper.HbmEffectNT.register("ntmspace:spinlaunch");

    /**
     * Registers {@link #SPINLAUNCH} by initialising this class: called at mod construction ({@code DysonContent.init}),
     * as {@code HbmEffectNT.register} asks, so both sides know the name before any launcher exists.
     */
    public static void registerEffects() {}

    public TileEntityDysonLauncher(BlockPos pos, BlockState state) {
        super(DysonContent.LAUNCHER_BE.get(), pos, state, 2);
    }

    /** 1.7 {@code getInventoryStackLimit} 4. */
    @Override
    protected NTMItemHandler createInventory(int slotCount) {
        return new NTMItemHandler(slotCount, 4) {
            @Override
            protected void onContentsChanged(int slot) {
                TileEntityDysonLauncher.this.setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntityDysonLauncher.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override
    public int getMaxStackSize() {
        return 4;
    }

    @Override
    public String getDefaultName() {
        return "container.machineDysonLauncher";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDysonLauncher te) {
        te.step(level);
    }

    /** One 1.7 {@code updateEntity} on the server. */
    public void step(Level level) {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        setupAllPorts(getPorts());
        updatePortPIFIFO();
        for (DirPos pos : invPos(worldPosition, dir)) tryLoad(level, pos);

        ItemStack members = inventory.getStackInSlot(SLOT_MEMBERS);
        swarmId = ISatChip.getFreqS(inventory.getStackInSlot(SLOT_CHIP));
        swarmCount = CBT_Dyson.count(level, swarmId);

        isOperating = !isSpinningDown && power >= getPowerPerTick() && !members.isEmpty()
                && members.is(DysonContent.SWARM_MEMBER.get()) && swarmId > 0;

        if (isSpinningDown) {
            operatingTime++;

            if (operatingTime > getSpinDownTime()) {
                isSpinningDown = false;
                operatingTime = 0;
            }
        } else if (isOperating) {
            if (operatingTime == 0) {
                float pitch = sunsetOverdrive ? 1.0F : 0.25F;
                level.playSound(null, worldPosition.getX(), worldPosition.getY() + 8, worldPosition.getZ(),
                        DysonContent.SPINCHARGE.get(), SoundSource.BLOCKS, 1.5F, pitch);
            }

            operatingTime++;
            power -= getPowerPerTick();

            if (operatingTime > getSpinUpTime()) {
                int toLaunch = Math.min(members.getCount(), MEMBERS_PER_LAUNCH);
                CBT_Dyson.launch(level, swarmId, toLaunch);

                double pressure = pressure(level);
                double scaledPressure = 1.0 - Math.pow(1.0 - pressure, 3);

                float volume = Math.min((float) scaledPressure * 16.0F, 4.0F);

                ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
                double sx = worldPosition.getX() + rot.offsetX * 6, sy = worldPosition.getY() + 8, sz = worldPosition.getZ() + rot.offsetZ * 6;
                level.playSound(null, sx, sy, sz, DysonContent.SPINSHOT.get(), SoundSource.BLOCKS, volume, 0.9F + level.getRandom().nextFloat() * 0.3F);
                level.playSound(null, sx, sy, sz, DysonContent.SPINSHOT.get(), SoundSource.BLOCKS, volume, 1F + level.getRandom().nextFloat() * 0.3F);

                int count = Math.min(20, (int) (pressure * 80));
                net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                data.putInt("count", count);
                data.putFloat("scale", 3);
                data.putDouble("moX", dir.offsetX * 10);
                data.putDouble("moY", 10);
                data.putDouble("moZ", dir.offsetZ * 10);
                data.putInt("maxAge", 10 + count / 2 + level.getRandom().nextInt(5));
                SPINLAUNCH.sendAround(level, worldPosition.getX() + rot.offsetX * 9, worldPosition.getY() + 12,
                        worldPosition.getZ() + rot.offsetZ * 9, data, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), 150);

                members.shrink(toLaunch);
                inventory.setStackInSlot(SLOT_MEMBERS, members.isEmpty() ? ItemStack.EMPTY : members);

                operatingTime = 0;
                isSpinningDown = true;
            }
        } else {
            operatingTime = 0;
        }

        networkPackNT();
    }

    /** 1.7 {@code CelestialBody.getTrait(world, CBT_Atmosphere.class).getPressure()}, 0 without air. */
    public static double pressure(Level level) {
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        return atmosphere != null ? atmosphere.getPressure() : 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityDysonLauncher te) {
        float acceleration = te.sunsetOverdrive ? 2.5F : 0.75F;
        float deceleration = te.sunsetOverdrive ? 15.0F : 3.0F;
        float resetSpeed = te.sunsetOverdrive ? 30.0F : 8.0F;

        if (te.isOperating) {
            te.speed += acceleration;
            if (te.speed > 90) te.speed = 90;
        } else if (te.speed > 0.1F) {
            te.speed -= deceleration;
            if (te.speed < resetSpeed) te.speed = resetSpeed;
        }

        te.lastRotation = te.rotation;
        if (!te.isOperating && te.speed <= resetSpeed && te.rotation > 360 - resetSpeed * 1.5) {
            te.lastRotation -= 360;
            te.rotation = 0;
            te.speed = 0;
        } else {
            te.rotation += te.speed;
        }

        if (te.rotation >= 360) {
            te.rotation -= 360;
            te.lastRotation -= 360;
        }

        if (te.isSpinningDown) {
            te.payloadTicks++;
        } else {
            te.payloadTicks = 0;
        }

    }

    public int getSpinUpTime() { return sunsetOverdrive ? 38 : 132; }
    public int getSpinDownTime() { return sunsetOverdrive ? 12 : 68; }
    public long getPowerPerTick() { return MAX_POWER / getSpinUpTime(); }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(level, getBlockState(), worldPosition, conPos(worldPosition, MachineCore.facing(getBlockState())));
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: three to the side of the five power cells, facing them. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        DirPos[] out = new DirPos[5];
        for (int k = 0; k < 5; k++) {
            out[k] = new DirPos(x - dir.offsetX * k - rot.offsetX * 3, y, z - dir.offsetZ * k - rot.offsetZ * 3, rot.getOpposite());
        }
        return out;
    }

    /** 1.7 {@code getInvPos}: the inventory nine blocks behind the core. */
    public static DirPos[] invPos(BlockPos core, ForgeDirection dir) {
        return new DirPos[] {
                new DirPos(core.getX() - dir.offsetX * 9, core.getY(), core.getZ() - dir.offsetZ * 9, dir.getOpposite()),
        };
    }

    /** 1.7 {@code tryLoad}: one swarm member a tick out of the inventory at {@code pos}, while slot 0 holds fewer than four. */
    private void tryLoad(Level level, DirPos pos) {
        ItemStack held = inventory.getStackInSlot(SLOT_MEMBERS);
        if (!held.isEmpty() && held.getCount() >= MEMBERS_PER_LAUNCH) return;

        BlockEntity te = level.getBlockEntity(pos.getPos());
        if (!(te instanceof Container inv)) return;

        WorldlyContainer sided = inv instanceof WorldlyContainer w ? w : null;
        net.minecraft.core.Direction side = pos.getDir().toDirection();
        int[] access = sided != null ? sided.getSlotsForFace(side) : null;

        for (int i = 0; i < (access != null ? access.length : inv.getContainerSize()); i++) {
            int slot = access != null ? access[i] : i;
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && stack.is(DysonContent.SWARM_MEMBER.get())
                    && (sided == null || sided.canTakeItemThroughFace(slot, stack, side))) {
                ItemStack removed = inv.removeItem(slot, 1);
                if (held.isEmpty()) {
                    inventory.setStackInSlot(SLOT_MEMBERS, removed);
                } else {
                    held.grow(1);
                    inventory.setStackInSlot(SLOT_MEMBERS, held);
                }
                break;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(swarmId);
        buf.writeLong(power);
        buf.writeBoolean(isOperating);
        buf.writeBoolean(isSpinningDown);
        buf.writeInt(swarmCount);
        buf.writeBoolean(sunsetOverdrive);
        buf.writeInt(inventory.getStackInSlot(SLOT_MEMBERS).getCount());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        swarmId = buf.readInt();
        power = buf.readLong();
        isOperating = buf.readBoolean();
        isSpinningDown = buf.readBoolean();
        swarmCount = buf.readInt();
        sunsetOverdrive = buf.readBoolean();
        satCount = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("spinDown", isSpinningDown);
        output.putInt("time", operatingTime);
        output.putBoolean("overdrive", sunsetOverdrive);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0);
        isSpinningDown = input.getBooleanOr("spinDown", false);
        operatingTime = input.getIntOr("time", 0);
        sunsetOverdrive = input.getBooleanOr("overdrive", false);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
        if (slot == SLOT_MEMBERS) return itemStack.is(DysonContent.SWARM_MEMBER.get());
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[] {SLOT_MEMBERS};
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
}
