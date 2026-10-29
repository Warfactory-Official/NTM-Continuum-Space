package com.ntmspace.tileentity.rocket;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.util.Mth;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.IClimbable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModFuelItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.ntmspace.block.rocket.LaunchPadRocket;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.inventory.rocket.ContainerLaunchPadRocket;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketTarget;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityLaunchPadRocket}: holds a rocket (slot 0), its navigation drive (1), a battery (2) and a
 * fuel input/output (3/4, which also takes solid rocket fuel). Its tanks take the fuels the loaded rocket's stages
 * burn, at the stages' full capacity ({@link #updateStorageTanks}); a rocket that comes off a station or pad already
 * fuelled fills them at once. "COMMIT TO LAUNCH" needs 75 kHE, full tanks, a processed drive the rocket can reach, and
 * spawns the rideable rocket three blocks up. A rocket taller than 8 blocks gets a service tower and crew platform, if
 * the pad sees the sky.
 *
 * <p>The OpenComputers component {@code ntm_rocket_pad} is not ported (HBM's CC: Tweaked peripherals are its own
 * family). 1.7 sorted the tanks by numeric fluid id (descending, NONE last); here by 1.7's declaration order of the same
 * fluids, which is what those ids were.
 */
public class TileEntityLaunchPadRocket extends TileEntityMachineBase implements MenuProvider, IControlReceiver,
        IEnergyReceiverMK2, IFluidStandardReceiverMK2, IClimbable {

    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DRIVE = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FUEL_IN = 3;
    public static final int SLOT_FUEL_OUT = 4;
    public static final int SLOT_COUNT = SLOT_FUEL_OUT + 1;

    public long power;
    public final long maxPower = 100_000;

    public static class SolidFuelTank {
        public int level = 0;
        public int max = 0;
    }

    public SolidFuelTank solidFuel = new SolidFuelTank();

    public FluidTankNTM[] tanks;

    public boolean canSeeSky = true;
    public @Nullable RocketStruct rocket;
    public int height;
    // port: the tower as built, which the world's top can cut short of the rocket's height
    private int tower;

    public int towerHeight() {
        return tower;
    }

    public TileEntityLaunchPadRocket(BlockPos pos, BlockState state) {
        super(RocketBlocks.LAUNCH_PAD_ROCKET_BE.get(), pos, state, SLOT_COUNT); // 0 rocket, 1 drive, 2 battery, 3/4 liquid/solid fuel in/out
        tanks = new FluidTankNTM[RocketStruct.MAX_STAGES * 2]; // enough tanks for any combination of rocket stages
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTankNTM(Fluids.NONE, 64_000);
    }

    @Override
    public String getDefaultName() {
        return "container.launchPadRocket";
    }

    private Direction facing() {
        return getBlockState().getValue(LaunchPadRocket.FACING);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPadRocket te) {
        ItemVOTVdrive.publishStation(te.getItem(SLOT_DRIVE), level);

        // Setup tanks required for the current rocket
        te.updateTanks();

        te.setupAllPorts(te.getPorts());
        te.updateAllPorts();
        te.receivePower();
        if (te.hasRocket()) te.receiveFluid(te.getReceivingTanks());

        // Fills, note that the liquid input also takes solid fuel
        te.power = Library.chargeTEFromItems(te.inventory, SLOT_BATTERY, te.power, te.maxPower);
        for (FluidTankNTM tank : te.tanks) {
            if (tank.getTankType() != Fluids.NONE) MachineFluidContainerIO.load(tank, te.inventory, SLOT_FUEL_IN, SLOT_FUEL_OUT);
        }
        if (!te.getItem(SLOT_FUEL_IN).isEmpty() && te.getItem(SLOT_FUEL_IN).getItem() == ModFuelItems.rocket_fuel.get() && te.solidFuel.level < te.solidFuel.max) {
            te.removeItem(SLOT_FUEL_IN, 1);
            te.solidFuel.level += 250;
            if (te.solidFuel.level > te.solidFuel.max) te.solidFuel.level = te.solidFuel.max;
        }

        te.rocket = ItemCustomRocket.get(te.getItem(SLOT_ROCKET));
        if (te.rocket != null) {
            int newHeight = (int) Math.floor(te.rocket.getHeight() - (te.rocket.capsule != null ? te.rocket.capsule.height() : 0) + 1);
            if (newHeight <= 8) newHeight = 0;

            if (newHeight != te.height) {
                // Check that the pad is entirely unobstructed
                te.canSeeSky = !te.isPadObstructed(level);

                if (te.canSeeSky) te.buildTower(level, newHeight);

                te.height = newHeight;
            }
        }

        te.registerClimbable();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPadRocket te) {
        te.registerClimbable();
    }

    /** Fills in the tower with structure cells (1.7 under {@code safeRem}). */
    private void buildTower(Level level, int newHeight) {
        LaunchPadRocket block = RocketBlocks.LAUNCH_PAD_ROCKET.get();
        var old = new LongOpenHashSet();
        LaunchPadRocket.visitTower(worldPosition, facing(), tower, (pos, ignored) -> old.add(pos.asLong()));
        tower = Math.min(newHeight, level.getMaxY() - worldPosition.getY() - 3);
        var next = new LongOpenHashSet();
        LaunchPadRocket.visitTower(worldPosition, facing(), tower, (pos, ignored) -> next.add(pos.asLong()));
        for (long pos : old) if (!next.contains(pos)) block.removeDynamicCell(level, worldPosition, BlockPos.of(pos));
        for (long pos : next) if (!old.contains(pos)) block.placeDynamicCell(level, worldPosition, BlockPos.of(pos));
    }

    @Override
    public boolean isEntityInClimbAABB(LivingEntity entity) {
        var block = (LaunchPadRocket) getBlockState().getBlock();
        var dir = ForgeDirection.getOrientation(facing());
        var rot = dir.getRotation(ForgeDirection.UP);
        var bounds = entity.getBoundingBox();
        return intersectsLadder(bounds, block, rot, dir, 6, 0, 3)
                || height > 8 && intersectsLadder(bounds, block, rot, dir, 2, 3, height + 3);
    }

    private boolean intersectsLadder(AABB bounds, LaunchPadRocket block, ForgeDirection rot, ForgeDirection dir,
                                     int offset, int minY, int maxY) {
        int x = worldPosition.getX() - rot.offsetX * offset - dir.offsetX * 5;
        int z = worldPosition.getZ() - rot.offsetZ * offset - dir.offsetZ * 5;
        var column = new AABB(x + 0.25, worldPosition.getY() + minY, z + 0.25,
                x + 0.75, worldPosition.getY() + maxY, z + 0.75)
                .move(-rot.offsetX * 0.5, 0, -rot.offsetZ * 0.5);
        if (!bounds.intersects(column)) return false;
        int first = Math.max(worldPosition.getY() + minY, Mth.floor(bounds.minY));
        int last = Math.min(worldPosition.getY() + maxY - 1, Mth.floor(bounds.maxY));
        for (int y = first; y <= last; y++) {
            var pos = new BlockPos(x, y, z);
            var state = level.getBlockState(pos);
            if (!state.is(block)) continue;
            var box = block.climbBox(state);
            if (box != null && bounds.intersects(box.move(pos))) return true;
        }
        return false;
    }

    @Override
    public void setRemoved() {
        unregisterClimbable();
        super.setRemoved();
    }

    /** 1.7 {@code canBlockSeeTheSky(x, y + 2, z)} of the core column. */
    private boolean isPadObstructed(Level level) {
        return !level.canSeeSky(worldPosition.above(2));
    }

    // ---------------------------------------------------------------------------------------- launch

    public void launch(@Nullable LivingEntity player) {
        if (!canLaunch()) return;

        EntityRideableRocket entity = EntityRideableRocket.create(level, worldPosition.getX() + 0.5, worldPosition.getY() + 3.0,
                worldPosition.getZ() + 0.5, getItem(SLOT_ROCKET)).withProgram(getItem(SLOT_DRIVE)).launchedBy(player);
        level.addFreshEntity(entity);

        // Deplete all fills
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTankNTM(Fluids.NONE, 64_000);
        solidFuel.level = solidFuel.max = 0;

        power -= (long) (maxPower * 0.75);

        setItem(SLOT_ROCKET, ItemStack.EMPTY);
        setItem(SLOT_DRIVE, ItemStack.EMPTY);
    }

    private PortDef[] cachedPorts;

    /** 1.7 {@code getConPos}: the 13 input cells, each aimed back out of the pad. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection back = ForgeDirection.getOrientation(facing()).getOpposite();
            List<BlockPos> inputs = LaunchPadRocket.inputs(worldPosition, facing());
            cachedPorts = new PortDef[inputs.size()];
            for (int i = 0; i < cachedPorts.length; i++) cachedPorts[i] = PortDef.make(inputs.get(i), back);
        }
        return cachedPorts;
    }

    private boolean hasRocket() {
        return ItemCustomRocket.get(getItem(SLOT_ROCKET)) != null;
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

        RocketStruct rocket = ItemCustomRocket.get(getItem(SLOT_ROCKET));
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
        return hasRocket() && hasDrive() && power >= maxPower * 0.75 && areTanksFull() && canReachDestination();
    }

    private void updateTanks() {
        if (!hasRocket()) return;

        RocketStruct rocket = ItemCustomRocket.get(getItem(SLOT_ROCKET));

        // If the rocket is already fueled, unmark it and fill the tanks
        boolean hasFuel = ItemCustomRocket.hasFuel(getItem(SLOT_ROCKET));
        if (hasFuel) ItemCustomRocket.setFuel(getItem(SLOT_ROCKET), false);

        updateStorageTanks(rocket, tanks, solidFuel, hasFuel);
    }

    public static void updateStorageTanks(RocketStruct rocket, FluidTankNTM[] tanks, SolidFuelTank solidFuel, boolean hasFuel) {
        Map<FluidType, Integer> fuels = rocket.getFillRequirement();

        // Remove solid fuels (listed as NONE fluid) from tank updates
        if (fuels.containsKey(Fluids.NONE)) {
            solidFuel.max = fuels.get(Fluids.NONE);
            if (hasFuel) solidFuel.level = solidFuel.max;
            fuels.remove(Fluids.NONE);
        } else {
            solidFuel.max = 0;
        }

        // Check to see if any of the current tanks already fulfil fuelling requirements
        List<FluidTankNTM> keepTanks = new ArrayList<>();
        for (FluidTankNTM tank : tanks) {
            if (fuels.containsKey(tank.getTankType())) {
                tank.changeTankSize(fuels.get(tank.getTankType()));
                keepTanks.add(tank);
                fuels.remove(tank.getTankType());
            }
        }

        // Add new tanks
        for (Entry<FluidType, Integer> entry : fuels.entrySet()) {
            keepTanks.add(new FluidTankNTM(entry.getKey(), entry.getValue()));
        }

        // Fill tanks if rocket had fuel
        if (hasFuel) {
            for (FluidTankNTM tank : keepTanks) {
                tank.setFill(tank.getMaxFill());
            }
        }

        // Sort and fill the tank array to place NONE at the end
        keepTanks.sort((a, b) -> order(b.getTankType()) - order(a.getTankType()));
        while (keepTanks.size() < RocketStruct.MAX_STAGES * 2) {
            keepTanks.add(new FluidTankNTM(Fluids.NONE, 64_000));
        }

        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = keepTanks.get(i);
        }
    }

    /**
     * 1.7's numeric fluid id for the sort: HBM's 1.7 ids, and hbm-space's hydrazine, which it declared after every 1.7
     * fluid (so above them).
     */
    private static int order(FluidType type) {
        if (type == Fluids.NONE) return 0;
        if (type == SpaceFluids.HYDRAZINE) return Integer.MAX_VALUE / 2;
        int legacy = Fluids.legacyId(type);
        return legacy < 0 ? Integer.MAX_VALUE / 4 : legacy;
    }

    public static void findTankIssues(List<Component> issues, FluidTankNTM[] tanks, SolidFuelTank solidFuel) {
        for (FluidTankNTM tank : tanks) {
            if (tank.getTankType() == Fluids.NONE) continue;
            int fill = tank.getFill();
            int maxFill = tank.getMaxFill();
            String tankName = tank.getTankType().getDisplayNameString();
            if (tankName.contains(" ")) {
                String[] split = tankName.split(" ");
                tankName = split[split.length - 1];
            }
            ChatFormatting color = fill < maxFill ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            issues.add(Component.literal(fill + "/" + maxFill + "mB " + tankName).withStyle(color));
        }

        if (solidFuel.max > 0) {
            ChatFormatting color = solidFuel.level < solidFuel.max ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            issues.add(Component.literal(solidFuel.level + "/" + solidFuel.max + "kg Solid Fuel").withStyle(color));
        }
    }

    public static boolean findDriveIssues(List<Component> issues, RocketStruct rocket, ItemStack drive) {
        if (drive.isEmpty() || !(drive.getItem() instanceof ItemVOTVdrive)) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.noDrive").withStyle(ChatFormatting.YELLOW));
            return true;
        }

        if (!ItemVOTVdrive.getProcessed(drive)) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.unprocessed").withStyle(ChatFormatting.RED));
            return true;
        }

        String capsule = rocket.capsule != null ? rocket.capsule.id() : "";
        DriveBody target = ItemVOTVdrive.getBody(drive);
        if (target == DriveBody.ORBIT && !RocketParts.CAPSULE.equals(capsule) && !RocketParts.STATION_CORE.equals(capsule)) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.satelliteOrbit").withStyle(ChatFormatting.RED));
            return true;
        }

        return false;
    }

    public static void findTravelIssues(List<Component> issues, RocketStruct rocket, RocketTarget from, RocketTarget to) {
        String capsule = rocket.capsule != null ? rocket.capsule.id() : "";
        if (to.inOrbit() && !to.isValid() && !RocketParts.STATION_CORE.equals(capsule)) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.notLaunched").withStyle(ChatFormatting.RED));
        }

        if (to.inOrbit() && to.isValid() && RocketParts.STATION_CORE.equals(capsule)) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.launched").withStyle(ChatFormatting.RED));
        }

        if (!rocket.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit())) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.unreachable").withStyle(ChatFormatting.RED));
        }
    }

    public List<Component> findIssues() {
        List<Component> issues = new ArrayList<>();

        if (!hasRocket()) return issues;

        // Check that the rocket is fully fueled and capable of leaving our starting planet
        RocketStruct rocket = ItemCustomRocket.get(getItem(SLOT_ROCKET));

        if (!canSeeSky) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.obstructed").withStyle(ChatFormatting.RED));
        }

        if (power < maxPower * 0.75) {
            issues.add(Component.translatable("desc.gui.launchPadRocket.noPower").withStyle(ChatFormatting.RED));
        }

        findTankIssues(issues, tanks, solidFuel);
        if (findDriveIssues(issues, rocket, getItem(SLOT_DRIVE))) return issues;

        // Check that the rocket is actually capable of reaching our destination
        RocketTarget from = RocketTarget.of(level, worldPosition.getX(), worldPosition.getZ());
        RocketTarget to = ItemVOTVdrive.getTarget(getItem(SLOT_DRIVE), level);

        findTravelIssues(issues, rocket, from, to);

        return issues;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (index == SLOT_ROCKET && !(stack.getItem() instanceof ItemCustomRocket)) return false;
        if (index == SLOT_DRIVE && !(stack.getItem() instanceof ItemVOTVdrive)) return false;
        if (index == SLOT_BATTERY && !(stack.getItem() instanceof IBatteryItem)) return false;
        if (index == SLOT_FUEL_IN && stack.getItem() != ModFuelItems.rocket_fuel.get()) return false;
        return true;
    }

    // ---------------------------------------------------------------------------------------- sync and save

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);

        buf.writeLong(power);
        buf.writeInt(solidFuel.level);
        buf.writeInt(solidFuel.max);

        buf.writeInt(height);
        buf.writeInt(tower);
        buf.writeBoolean(canSeeSky);

        if (rocket != null) {
            buf.writeBoolean(true);
            rocket.writeToByteBuffer(buf);
        } else {
            buf.writeBoolean(false);
        }

        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);

        power = buf.readLong();
        solidFuel.level = buf.readInt();
        solidFuel.max = buf.readInt();

        height = buf.readInt();
        tower = buf.readInt();
        canSeeSky = buf.readBoolean();

        if (buf.readBoolean()) {
            rocket = RocketStruct.readFromByteBuffer(buf);
        } else {
            rocket = null;
        }

        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("solid", solidFuel.level);
        output.putInt("maxSolid", solidFuel.max);
        output.putInt("height", height);
        output.putInt("tower", tower);
        output.putBoolean("sky", canSeeSky);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        solidFuel.level = input.getIntOr("solid", 0);
        solidFuel.max = input.getIntOr("maxSolid", 0);
        height = input.getIntOr("height", 0);
        tower = input.getIntOr("tower", height);
        canSeeSky = input.getBooleanOr("sky", true);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerLaunchPadRocket menu && menu.getPad() == this;
    }

    @Override
    public void receiveControl(CompoundTag data) {}

    @Override
    public void receiveControl(Player player, CompoundTag data) {
        if (dev.golem.core.data.Tags.getBooleanOr(data, "launch", false)) {
            launch(player);
        }
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getAllTanks() { return this.tanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return this.tanks; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerLaunchPadRocket(containerId, inventory, this);
    }

    /** 1.7 {@code isUseableByPlayer}: within 32 blocks of the core. */
    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 1024;
    }
}
