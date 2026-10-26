package com.ntmspace.tileentity.machine;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.inventory.machine.ContainerVacuumCircuit;
import com.ntmspace.recipe.VacuumCircuitRecipes;
import com.ntmspace.recipe.VacuumCircuitRecipes.VacuumCircuitRecipe;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityMachineVacuumCircuit} (the Vacuum Solderer): solders wafers (slots 0-1) onto boards
 * (slots 2-3) by its {@code ntmspace:vacuum_circuit} recipe into slot 4, only in a near vacuum (no air at the machine, or
 * below 0.001 atm: 1.7 {@code ChunkAtmosphereManager.getAtmosphere}, the atmosphere port's
 * {@link AtmosphereHandler#getAtmosphere}). Slot 5 battery, 6-7 upgrades (speed and power, three levels each).
 *
 * <p>Per tick while working: {@code consumption} HE, and the recipe is done after {@code processTime} ticks
 * ({@code duration - duration * speed / 6 + duration * power / 3}; {@code consumption + consumption * speed - consumption
 * * power / 6}). The buffer follows the recipe: {@code max(consumption * 20, power)} (2,000 HE idle).
 *
 * <p>Speed and power upgrades are read from the two upgrade slots exposed by the menu.
 */
public class TileEntityMachineVacuumCircuit extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2,
        IUpgradeInfoProvider, WorldlyContainer {

    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_UPGRADE_START = 6;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;
    private static final int[] SLOTS = {0, 1, 2, 3, 4};

    public long power;
    public long maxPower = 2_000;
    public long consumption;

    public int progress;
    public int processTime = 1;

    private @Nullable VacuumCircuitRecipe recipe;
    public final RandomSource outputRandom = RandomSource.create();
    public ItemStack display = ItemStack.EMPTY;

    public boolean canOperate = true;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineVacuumCircuit(BlockPos pos, BlockState state) {
        super(SpaceMachinesB.VACUUM_CIRCUIT_BE.get(), pos, state, 8);
    }

    @Override
    public String getDefaultName() {
        return "container.machineVacuumCircuit";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineVacuumCircuit te) {
        te.canOperate = isNearVacuum(level, pos);
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();
        te.work(level);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineVacuumCircuit te) {}

    /** 1.7: {@code atmosphere == null || atmosphere.getPressure() <= 0.001}. */
    public static boolean isNearVacuum(Level level, BlockPos pos) {
        CBT_Atmosphere atmosphere = AtmosphereHandler.getAtmosphere(level, pos);
        return atmosphere == null || atmosphere.getPressure() <= 0.001;
    }

    /** 1.7 {@code updateEntity} past the atmosphere check. */
    public void work(Level level) {
        power = Library.chargeTEFromItems(inventory, 5, getPower(), getMaxPower());
        recipe = VacuumCircuitRecipes.INSTANCE.getRecipe(new ItemStack[] {
                inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2), inventory.getStackInSlot(3)});
        long intendedMaxPower;

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int redLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int blueLevel = upgradeManager.getLevel(UpgradeType.POWER);

        if (recipe != null) {
            processTime = recipe.duration - (recipe.duration * redLevel / 6) + (recipe.duration * blueLevel / 3);
            consumption = recipe.power + (recipe.power * redLevel) - (recipe.power * blueLevel / 6);
            intendedMaxPower = recipe.power * 20;

            if (canProcess(recipe)) {
                progress++;
                power -= consumption;

                if (progress >= processTime) {
                    progress = 0;
                    consumeAndProduce(recipe);
                }

                if (level.getGameTime() % 20 == 0) {
                    ForgeDirection dir = MachineCore.facing(getBlockState());
                    ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
                    CompoundTag dPart = new CompoundTag();
                    dPart.putString("type", "tau");
                    dPart.putByte("count", (byte) 3);
                    HbmEffectNT.Tau.send(level, worldPosition.getX() + 0.5 + dir.offsetX * 0.625 + rot.offsetX * 0.5,
                            worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5 + dir.offsetZ * 0.625 + rot.offsetZ * 0.5, dPart, 25);
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
            consumption = 100;
            intendedMaxPower = 2000;
        }

        maxPower = Math.max(intendedMaxPower, power);
        display = recipe == null ? ItemStack.EMPTY : new ItemStack(recipe.previewItem());
    }

    public boolean canProcess(VacuumCircuitRecipe recipe) {
        if (!canOperate) return false;
        if (power < consumption) return false;
        return recipe.canFitOutput(0, inventory.getStackInSlot(SLOT_OUTPUT), inventory.getSlotLimit(SLOT_OUTPUT));
    }

    private void consumeAndProduce(VacuumCircuitRecipe recipe) {
        var made = recipe.output(outputRandom);
        consumeItems(recipe);
        if (!made.isEmpty()) {
            var out = inventory.getStackInSlot(SLOT_OUTPUT);
            inventory.setStackInSlot(SLOT_OUTPUT, out.isEmpty() ? made
                    : ItemStacks.copyWithCount(out, out.getCount() + made.getCount()));
        }
        setChanged();
    }

    private void writeDisplay(ByteBuf output) {
        output.writeBoolean(recipe != null);
        if (recipe != null) output.writeInt(BuiltInRegistries.ITEM.getId(recipe.previewItem()));
    }

    private void readDisplay(ByteBuf input) {
        display = input.readBoolean() ? new ItemStack(BuiltInRegistries.ITEM.byId(input.readInt()))
                : ItemStack.EMPTY;
    }

    public void consumeItems(VacuumCircuitRecipe recipe) {
        for (var aStack : recipe.wafer()) {
            for (int i = 0; i < 2; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (aStack.matchesRecipe(stack, true) && stack.getCount() >= aStack.stacksize) { removeItem(i, aStack.stacksize); break; }
            }
        }
        for (var aStack : recipe.pcb()) {
            for (int i = 2; i < 4; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (aStack.matchesRecipe(stack, true) && stack.getCount() >= aStack.stacksize) { removeItem(i, aStack.stacksize); break; }
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 2) {
            for (int i = 0; i < 2; i++) if (i != slot && sameItem(inventory.getStackInSlot(i), stack)) return false;
            return VacuumCircuitRecipes.INSTANCE.isWafer(stack);
        } else if (slot < 4) {
            for (int i = 2; i < 4; i++) if (i != slot && sameItem(inventory.getStackInSlot(i), stack)) return false;
            return VacuumCircuitRecipes.INSTANCE.isPcb(stack);
        }
        return false;
    }

    /** 1.7 {@code isItemEqual}: same item and damage. */
    private static boolean sameItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && a.getDamageValue() == b.getDamageValue();
    }

    /** The upgrade plug sound (1.7 {@code setInventorySlotContents} for slots 6-7). */
    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[] {6, 7};
    }

    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 4; }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(level, getBlockState(), worldPosition, conPos(worldPosition));
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: two points beyond each side of the 3 x 3 (world directions, it does not turn). */
    public static DirPos[] conPos(BlockPos core) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z),
        };
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(consumption);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeBoolean(canOperate);
        writeDisplay(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        consumption = buf.readLong();
        progress = buf.readInt();
        processTime = buf.readInt();
        canOperate = buf.readBoolean();
        readDisplay(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 0L);
        progress = input.getIntOr("progress", 0);
        processTime = input.getIntOr("processTime", 0);
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 3, p.getZ() + 2);
    }

    @Override public long getPower() { return Math.max(Math.min(power, maxPower), 0); }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(SpaceMachinesB.MACHINE_VACUUM_CIRCUIT.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        return upgrades;
    }

    /** The current recipe as a {@link GenericRecipe} (null when idle). */
    public @Nullable GenericRecipe currentRecipe() {
        return recipe;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerVacuumCircuit(containerId, playerInventory, this);
    }
}
