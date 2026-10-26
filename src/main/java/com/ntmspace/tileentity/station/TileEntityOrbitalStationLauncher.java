package com.ntmspace.tileentity.station;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
import com.hbm.items.ModFuelItems;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.ntmspace.block.station.BlockOrbitalStation;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.inventory.station.ContainerOrbitalStationLauncher;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.StationLauncherBlocks;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketTarget;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket.SolidFuelTank;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityOrbitalStationLauncher}: a docking port that builds and launches rockets in orbit. It acts
 * almost entirely like a port, except that it does not register itself with its station, so nothing tries to dock here.
 *
 * <p>Slots (1.7 order): 0 destination drive, 1 fuel in (also solid rocket fuel, 250 kg each), 2 fuel out, 3 payload,
 * 4..18 five stages of fuselage / fins / thruster, 19..28 two ghost drive slots per stage ("from" and "to", read by the
 * issue list). Every tick the stages are read from the slots (an empty stage pulls the one above it down), the tanks
 * take the fuels of the rocket at the stages' full capacity (the launch pad's {@link TileEntityLaunchPadRocket#updateStorageTanks}),
 * ten tanks of 64,000 mB when idle. "COMMIT TO LAUNCH" needs a valid rocket, a processed drive the rocket can reach and
 * full tanks; it spawns the rideable rocket hanging under the port, docked, and empties the machine. No power (1.7's
 * power code is commented out). Stack limit 8.
 */
public class TileEntityOrbitalStationLauncher extends TileEntityMachineBase implements MenuProvider, IControlReceiver,
        IFluidStandardReceiverMK2 {

    public static final int SLOT_DRIVE = 0;
    public static final int SLOT_FUEL_IN = 1;
    public static final int SLOT_FUEL_OUT = 2;
    public static final int SLOT_PAYLOAD = 3;
    public static final int SLOT_STAGES = 4;
    /** launch: drive + fuel in + fuel out; construction: capsule + stages + program drives */
    public static final int SLOT_COUNT = 1 + 1 + 1 + 1 + RocketStruct.MAX_STAGES * 3 + RocketStruct.MAX_STAGES * 2;
    public static final int SLOT_DRIVES = SLOT_COUNT - RocketStruct.MAX_STAGES * 2;

    public RocketStruct rocket = new RocketStruct();

    private @Nullable EntityRideableRocket docked;

    public FluidTankNTM[] tanks;
    public SolidFuelTank solidFuel = new SolidFuelTank();

    public float rot;
    public float prevRot;

    public int currentStage;

    // Client synced state information
    public boolean hasDocked = false;
    public boolean hasRider = false;

    public TileEntityOrbitalStationLauncher(BlockPos pos, BlockState state) {
        super(StationLauncherBlocks.ORBITAL_STATION_LAUNCHER_BE.get(), pos, state, SLOT_COUNT);

        tanks = new FluidTankNTM[RocketStruct.MAX_STAGES * 2]; // enough tanks for any combination of rocket stages
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTankNTM(Fluids.NONE, 64_000);
    }

    @Override
    public String getDefaultName() {
        return "container.orbitalStationLauncher";
    }

    /** The "from" and "to" drives of the stage the screen shows. */
    public ItemStack fromDrive() {
        return getItem(SLOT_DRIVES + currentStage * 2);
    }

    public ItemStack toDrive() {
        return getItem(SLOT_DRIVES + currentStage * 2 + 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStationLauncher te) {
        if (!Orbit.inOrbit(level)) return;
        te.updateServer();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStationLauncher te) {
        if (!Orbit.inOrbit(level)) return;
        te.prevRot = te.rot;
        if (te.hasDocked) {
            te.rot += 2.25F;
            if (te.rot > 90) te.rot = 90;
        } else {
            te.rot -= 2.25F;
            if (te.rot < 0) te.rot = 0;
        }
    }

    /** 1.7 {@code updateEntity}'s server half (the orbit check is the caller's; GameTests call it directly). */
    public void updateServer() {
        // ROCKET CONSTRUCTION //
        // updates the orbital station information and syncs it to the client, if necessary
        ItemVOTVdrive.getTarget(getItem(SLOT_DRIVE), level);
        ItemVOTVdrive.getTarget(fromDrive(), level);
        ItemVOTVdrive.getTarget(toDrive(), level);

        rocket = buildRocket();

        // ROCKET LAUNCHING
        updateTanks();

        // 1.7 subscribed the rim for each typed tank only while the rocket is valid
        setupFluidInPorts(tanks, PortDef.combine(BlockOrbitalStation.rimPorts(worldPosition)));
        updateAllPorts();
        if (rocket.validate()) receiveFluid(tanks);

        for (FluidTankNTM tank : tanks) {
            if (tank.getTankType() != Fluids.NONE) MachineFluidContainerIO.load(tank, inventory, SLOT_FUEL_IN, SLOT_FUEL_OUT);
        }
        ItemStack fuelIn = getItem(SLOT_FUEL_IN);
        if (!fuelIn.isEmpty() && fuelIn.getItem() == ModFuelItems.rocket_fuel.get() && solidFuel.level < solidFuel.max) {
            removeItem(SLOT_FUEL_IN, 1);
            solidFuel.level += 250;
            if (solidFuel.level > solidFuel.max) solidFuel.level = solidFuel.max;
        }

        if (docked != null && (docked.isRemoved() || docked.getState() == RocketState.UNDOCKING)) {
            undockRocket();
        }

        hasDocked = docked != null;
        hasRider = hasDocked && docked.getFirstPassenger() != null;

        setChanged();
        networkPackNT();
    }

    /**
     * The rocket in the slots; an empty stage takes the one above it. 1.7's shift test is {@code i + 3 < MAX_STAGES * 3}
     * with {@code i} starting at slot 4 (copied from the VAB, whose stages start at slot 1), so a lone fifth stage is
     * never pulled down into an empty fourth: kept.
     */
    RocketStruct buildRocket() {
        RocketStruct built = new RocketStruct(getItem(SLOT_PAYLOAD));
        if (!getItem(SLOT_PAYLOAD).isEmpty() && getItem(SLOT_PAYLOAD).getItem() instanceof ISatChip) {
            built.satFreq = ISatChip.getFreqS(getItem(SLOT_PAYLOAD));
        }
        for (int i = SLOT_STAGES; i < RocketStruct.MAX_STAGES * 3 + 3; i += 3) {
            if (getItem(i).isEmpty() && getItem(i + 1).isEmpty() && getItem(i + 2).isEmpty()) {
                // Check for later stages and shift them up into empty stages
                if (i + 3 < RocketStruct.MAX_STAGES * 3 && (!getItem(i + 3).isEmpty() || !getItem(i + 4).isEmpty() || !getItem(i + 5).isEmpty())) {
                    setItem(i, getItem(i + 3));
                    setItem(i + 1, getItem(i + 4));
                    setItem(i + 2, getItem(i + 5));
                    setItem(i + 3, ItemStack.EMPTY);
                    setItem(i + 4, ItemStack.EMPTY);
                    setItem(i + 5, ItemStack.EMPTY);
                } else {
                    break;
                }
            }
            built.addStage(getItem(i), getItem(i + 1), getItem(i + 2));
        }
        return built;
    }

    public void enterCapsule(Player player) {
        if (docked == null || docked.getFirstPassenger() != null) return;
        docked.board(player);
    }

    public void dockRocket(EntityRideableRocket rocket) {
        docked = rocket;
    }

    public void undockRocket() {
        docked = null;
    }

    public @Nullable EntityRideableRocket getDocked() {
        return docked;
    }

    private boolean hasDrive() {
        return getItem(SLOT_DRIVE).getItem() instanceof ItemVOTVdrive;
    }

    private boolean areTanksFull() {
        for (FluidTankNTM tank : tanks) if (tank.getTankType() != Fluids.NONE && tank.getFill() < tank.getMaxFill()) return false;
        if (solidFuel.level < solidFuel.max) return false;
        return true;
    }

    private boolean canReachDestination() {
        // Check that the drive is processed
        if (!ItemVOTVdrive.getProcessed(getItem(SLOT_DRIVE))) {
            return false;
        }

        String capsule = rocket.capsule != null ? rocket.capsule.id() : "";

        DriveBody target = ItemVOTVdrive.getBody(getItem(SLOT_DRIVE));
        if (target == DriveBody.ORBIT && !RocketParts.CAPSULE.equals(capsule) && !RocketParts.STATION_CORE.equals(capsule))
            return false;

        RocketTarget from = RocketTarget.of(level, worldPosition.getX(), worldPosition.getZ());
        RocketTarget to = ItemVOTVdrive.getTarget(getItem(SLOT_DRIVE), level);

        if (!to.isValid() && !RocketParts.STATION_CORE.equals(capsule)) return false;
        if (to.isValid() && RocketParts.STATION_CORE.equals(capsule)) return false;

        // Check if the stage can make the journey
        return rocket.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit());
    }

    public boolean canLaunch() {
        return rocket.validate() && hasDrive() && areTanksFull() && canReachDestination();
    }

    public void launch(@Nullable LivingEntity player) {
        if (!canLaunch()) return;

        ItemStack stack = ItemCustomRocket.build(rocket);

        EntityRideableRocket entity = EntityRideableRocket.create(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                worldPosition.getZ() + 0.5, stack).withProgram(getItem(SLOT_DRIVE)).launchedBy(player);
        entity.setPos(entity.getX(), entity.getY() - entity.getBbHeight(), entity.getZ());
        level.addFreshEntity(entity);

        // Deplete all fills
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTankNTM(Fluids.NONE, 64_000);
        solidFuel.level = solidFuel.max = 0;

        setItem(SLOT_DRIVE, ItemStack.EMPTY);
        for (int i = SLOT_PAYLOAD; i < SLOT_COUNT; i++) setItem(i, ItemStack.EMPTY);

        dockRocket(entity);
    }

    /** 1.7 {@code getInventoryStackLimit}. */
    @Override
    public int getMaxStackSize() {
        return 8;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (index == SLOT_DRIVE && !(stack.getItem() instanceof ItemVOTVdrive)) return false;
        if (index == SLOT_FUEL_IN && stack.getItem() != ModFuelItems.rocket_fuel.get()) return false;
        return true;
    }

    /** 1.7 {@code isBreaking}: the drive slots hold ghost copies, which are not dropped. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        for (int i = SLOT_DRIVES; i < SLOT_COUNT; i++) setItem(i, ItemStack.EMPTY);
        super.preRemoveSideEffects(pos, state);
    }

    private void updateTanks() {
        if (!rocket.validate()) return;
        TileEntityLaunchPadRocket.updateStorageTanks(rocket, tanks, solidFuel, false);
    }

    public List<Component> findIssues() {
        List<Component> issues = new ArrayList<>();

        if (!rocket.validate()) return issues;

        TileEntityLaunchPadRocket.findTankIssues(issues, tanks, solidFuel);
        if (TileEntityLaunchPadRocket.findDriveIssues(issues, rocket, getItem(SLOT_DRIVE))) return issues;

        // Check that the rocket is actually capable of reaching our destination
        RocketTarget from = RocketTarget.of(level, worldPosition.getX(), worldPosition.getZ());
        RocketTarget to = ItemVOTVdrive.getTarget(getItem(SLOT_DRIVE), level);

        TileEntityLaunchPadRocket.findTravelIssues(issues, rocket, from, to);

        return issues;
    }

    public void setCurrentStage(int stage) {
        currentStage = Math.max(0, Math.min(stage, RocketStruct.MAX_STAGES - 1));
    }

    // ---------------------------------------------------------------------------------------- sync and save

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        rocket.writeToByteBuffer(buf);

        buf.writeBoolean(hasDocked);
        buf.writeBoolean(hasRider);

        buf.writeInt(solidFuel.level);
        buf.writeInt(solidFuel.max);

        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        rocket = RocketStruct.readFromByteBuffer(buf);

        hasDocked = buf.readBoolean();
        hasRider = buf.readBoolean();

        solidFuel.level = buf.readInt();
        solidFuel.max = buf.readInt();

        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("solid", solidFuel.level);
        output.putInt("maxSolid", solidFuel.max);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        solidFuel.level = input.getIntOr("solid", 0);
        solidFuel.max = input.getIntOr("maxSolid", 0);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerOrbitalStationLauncher menu && menu.getLauncher() == this;
    }

    @Override
    public void receiveControl(CompoundTag data) {}

    @Override
    public void receiveControl(Player player, CompoundTag data) {
        if (dev.golem.core.data.Tags.getBooleanOr(data, "launch", false)) {
            launch(player);
        }
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    /** 1.7 subscribed the rim to each typed tank only while the rocket is valid. */
    @Override
    public FluidTankNTM[] getReceivingTanks() {
        if (!rocket.validate()) return new FluidTankNTM[0];
        List<FluidTankNTM> typed = new ArrayList<>();
        for (FluidTankNTM tank : tanks) if (tank.getTankType() != Fluids.NONE) typed.add(tank);
        return typed.toArray(new FluidTankNTM[0]);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerOrbitalStationLauncher(containerId, inventory, this);
    }
}
