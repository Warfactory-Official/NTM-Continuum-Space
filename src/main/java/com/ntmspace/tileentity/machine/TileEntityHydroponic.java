package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.block.machine.MachineHydroponic;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.inventory.machine.ContainerHydroponic;
import com.ntmspace.registry.SpaceMachinesB;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Plants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityHydroponic} (the Industrial Greenhouse): takes CO2 (16,000 mB) and gives O2 (16,000 mB),
 * 15 mB per crop growth stage ({@code ChunkAtmosphereHandler.CROP_GROWTH_CONVERSION}, the atmosphere port's
 * {@link AtmosphereHandler#CROP_GROWTH_CONVERSION}). Power: 2,000 HE buffer, 25 HE drained every tick; at 200 HE or more
 * the lamps are lit ({@link MachineHydroponic#LIT}) and, with at least 100 mB CO2, the three plant positions above the
 * base (core and its two lateral neighbours, one up) are planted from slot 0, ticked about ten times faster than in the
 * open ({@code rand(120)} random ticks), fertilised from the fertiliser store ({@code rand(60)}), and harvested into
 * slots 3-5 when fully grown. Without the power or the CO2 a plant is held at the stage it had.
 *
 * <p>Slots: 0 seeds, 1 fertiliser (bone meal 1, HBM fertiliser powder 9; store 200), 2 battery, 3-5 output.
 *
 * <p>Block updates: 1.7 placed, reset and removed plants with flags 3 (neighbours notified: the crops beside re-check
 * their soil, which the machine sustains, {@link MachineHydroponic}) and 2 (clients only). The modern spellings are
 * {@link Block#UPDATE_ALL} (the neighbours' shape update is that re-check) and {@link Block#UPDATE_CLIENTS} plus
 * {@link Block#UPDATE_KNOWN_SHAPE} (1.7 had no shape updates).
 */
public class TileEntityHydroponic extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2,
        IEnergyReceiverMK2, WorldlyContainer {

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5};
    /** 1.7 {@code setBlock(..., 3)}. */
    static final int PLACE_FLAGS = Block.UPDATE_ALL;
    /** 1.7 {@code setBlockMetadataWithNotify(..., 2)}. */
    static final int META_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public final FluidTankNTM[] tanks;
    public long power;
    public static final long maxPower = 2_000;

    public int fertilizer;
    public static final int maxFertilizer = 200;

    private boolean lightsOn = false;
    private final int[] prevMeta = new int[3];

    public TileEntityHydroponic(BlockPos pos, BlockState state) {
        super(SpaceMachinesB.HYDROBAY_BE.get(), pos, state, 6);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.CARBONDIOXIDE, 16_000);
        tanks[1] = new FluidTankNTM(Fluids.OXYGEN, 16_000);
    }

    @Override
    public String getDefaultName() {
        return "container.hydrobay";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHydroponic te) {
        if (!(level instanceof ServerLevel server)) return;
        if (te.cachedPowerPorts == null) {
            Ports ports = ports(pos, MachineCore.facing(state));
            te.cachedPowerPorts = MachinePorts.ports(state, pos, ports.power());
            te.cachedFluidPorts = MachinePorts.ports(state, pos, ports.fluid());
        }
        te.setupPowerPorts(te.cachedPowerPorts);
        te.setupFluidPorts(te.cachedFluidPorts);
        te.updatePortPIFIFO();
        te.update(server);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityHydroponic te) {}

    /** 1.7 {@code updateEntity}, server side, after the network subscriptions. */
    public void update(ServerLevel level) {
        ForgeDirection rot = MachineCore.facing(getBlockState()).getRotation(ForgeDirection.UP);
        RandomSource rand = level.getRandom();

        if (power > 0) power = Math.max(power - 25, 0);

        ItemStack fert = inventory.getStackInSlot(1);
        if (!fert.isEmpty()) {
            int strength = getFertilizerStrength(fert);
            if (strength > 0 && fertilizer <= maxFertilizer - strength) {
                removeItem(1, 1);
                fertilizer += strength;
                setChanged();
            }
        }

        power = Library.chargeTEFromItems(inventory, 2, power, maxPower);

        BlockPos left = worldPosition.offset(-rot.offsetX, 2, -rot.offsetZ);
        BlockPos right = worldPosition.offset(rot.offsetX, 2, rot.offsetZ);
        if (power >= 200) {
            if (!lightsOn) {
                setLamps(level, left, right, true);
                lightsOn = true;
            }
        } else if (lightsOn) {
            setLamps(level, left, right, false);
            lightsOn = false;
        }

        for (int i = 0; i < 3; i++) {
            BlockPos at = worldPosition.offset(rot.offsetX * (i - 1), 1, rot.offsetZ * (i - 1));
            BlockState plant = level.getBlockState(at);

            // Minimum CO2 pressure required to start growing
            if (power >= 200 && tanks[0].getFill() >= 100) {

                // Attempt planting a new plant; only allows single block crops
                if (!(plant.getBlock() instanceof BonemealableBlock)) {
                    BlockState planted = HydroponicPlants.plantFor(inventory.getStackInSlot(0));
                    if (planted == null) continue;
                    level.setBlock(at, planted, PLACE_FLAGS);
                    prevMeta[i] = 0;
                    removeItem(0, 1);
                    setChanged();
                    plant = level.getBlockState(at);
                    if (!(plant.getBlock() instanceof BonemealableBlock)) continue;
                }

                // Increase growth speed by about x10 (1.7 updateTick is the random tick)
                if (rand.nextInt(120) == 0) {
                    plant.randomTick(level, at, rand);
                    plant = level.getBlockState(at);
                    if (!(plant.getBlock() instanceof BonemealableBlock)) continue;
                }
                BonemealableBlock growable = (BonemealableBlock) plant.getBlock();

                boolean fullyGrown = false;
                if (Plants.isValidBonemealTarget(growable, level, at, plant)) { // should consume bonemeal, if not, assume fully grown
                    if (fertilizer > 0 && rand.nextInt(60) == 0) {
                        if (growable.isBonemealSuccess(level, rand, at, plant)) { // does bonemeal apply
                            growable.performBonemeal(level, rand, at, plant);
                            level.levelEvent(1505, at, 0); // 1.7 playAuxSFX(2005): the bone meal particles
                        }
                        // now consume the bonemeal
                        fertilizer--;
                    }
                } else {
                    fullyGrown = true;
                }

                BlockState grown = level.getBlockState(at);
                int newMeta = HydroponicPlants.stage(grown);
                if (newMeta != prevMeta[i]) {
                    // each growth stage sequesters carbon
                    int toProduce = Math.max(newMeta - prevMeta[i], 0) * AtmosphereHandler.CROP_GROWTH_CONVERSION;
                    tanks[0].setFill(Math.max(tanks[0].getFill() - toProduce, 0));
                    tanks[1].setFill(Math.min(tanks[1].getFill() + toProduce, tanks[1].getMaxFill()));
                    prevMeta[i] = newMeta;
                }

                // after collecting produced O2, break any fully grown plants unless there is no space to collect the drops
                if (fullyGrown && attemptHarvest(Block.getDrops(grown, level, at, null))) {
                    level.setBlock(at, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                    setChanged();
                }
            } else if (plant.getBlock() instanceof BonemealableBlock) {
                // pause growth until sufficient CO2 added
                BlockState held = HydroponicPlants.withStage(plant, prevMeta[i]);
                if (held != plant) level.setBlock(at, held, META_FLAGS);
            }
        }
    }

    /** 1.7 swapped the cells between {@code hydrobay} and {@code dummy_beam} under {@code safeRem}. */
    private void setLamps(Level level, BlockPos left, BlockPos right, boolean lit) {
        BlockMultiblockCore.withoutTeardown(() -> {
            for (BlockPos at : new BlockPos[] {left, right}) {
                BlockState cell = level.getBlockState(at);
                if (cell.getBlock() instanceof MachineHydroponic && cell.getValue(BlockMultiblockMachine.CELL)) {
                    level.setBlock(at, cell.setValue(MachineHydroponic.LIT, lit), PLACE_FLAGS);
                }
            }
        });
    }

    public ItemStack[] getValidFertilizers() {
        return new ItemStack[] {new ItemStack(Items.BONE_MEAL), new ItemStack(ModFinalConsumableItems.powder_fertilizer.get())};
    }

    static int getFertilizerStrength(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.BONE_MEAL)) return 1;
        if (stack.is(ModFinalConsumableItems.powder_fertilizer.get())) return 9;
        return 0;
    }

    /**
     * 1.7 {@code attemptHarvest}: every drop goes into slots 3-5 (existing stacks first, then the first empty slot), or
     * the slots are put back and the plant stays. Kept as 1.7 wrote it: the put-back restores the slots' stack objects,
     * so what an earlier drop already merged into an existing stack stays there.
     */
    private boolean attemptHarvest(List<ItemStack> drops) {
        ItemStack[] originals = {inventory.getStackInSlot(3), inventory.getStackInSlot(4), inventory.getStackInSlot(5)};
        for (ItemStack drop : drops) {
            if (!addToOutputs(drop.copy())) {
                for (int i = 0; i < 3; i++) inventory.getStacks().set(3 + i, originals[i]);
                return false;
            }
        }
        return true;
    }

    /** 1.7 {@code InventoryUtil.tryAddItemToInventory(slots, 3, 5, stack) == null}. */
    private boolean addToOutputs(ItemStack stack) {
        if (stack.isEmpty()) return true;
        for (int i = 3; i <= 5; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStacks.sameItemAndData(slot, stack)) {
                int transfer = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (transfer > 0) {
                    slot.grow(transfer);
                    stack.shrink(transfer);
                    if (stack.isEmpty()) return true;
                }
            }
        }
        for (int i = 3; i <= 5; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.getStacks().set(i, stack);
                return true;
            }
        }
        return false;
    }

    private PortDef[] cachedPowerPorts;
    private PortDef[] cachedFluidPorts;

    /** 1.7 {@code getFluidPos} and {@code getPowerPos}. */
    public record Ports(DirPos[] fluid, DirPos[] power) {}

    public static Ports ports(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        DirPos[] fluid = {
                new DirPos(x + rot.offsetX * 2 + dir.offsetX, y, z + rot.offsetZ * 2 + dir.offsetZ, dir),
                new DirPos(x - rot.offsetX * 2 + dir.offsetX, y, z - rot.offsetZ * 2 + dir.offsetZ, dir),
                new DirPos(x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ, dir.getOpposite()),
                new DirPos(x - rot.offsetX * 2 - dir.offsetX, y, z - rot.offsetZ * 2 - dir.offsetZ, dir.getOpposite()),
        };
        DirPos[] power = {new DirPos(x, y + 3, z, ForgeDirection.UP)};
        return new Ports(fluid, power);
    }

    /** The three plant positions, 1.7's loop order. */
    public static BlockPos[] plantPositions(BlockPos core, Direction facing) {
        ForgeDirection rot = ForgeDirection.getOrientation(facing).getRotation(ForgeDirection.UP);
        BlockPos[] out = new BlockPos[3];
        for (int i = 0; i < 3; i++) out[i] = core.offset(rot.offsetX * (i - 1), 1, rot.offsetZ * (i - 1));
        return out;
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 2, p.getY(), p.getZ() - 2, p.getX() + 3, p.getY() + 2, p.getZ() + 3);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return HydroponicPlants.isPlantable(stack);
        if (slot == 1) return getFertilizerStrength(stack) > 0;
        return false;
    }

    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 3; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(fertilizer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
        power = buf.readLong();
        fertilizer = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
        for (int i = 0; i < 3; i++) output.putInt("p" + i, prevMeta[i]);
        output.putLong("power", power);
        output.putBoolean("lights", lightsOn);
        output.putInt("fertilizer", fertilizer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
        for (int i = 0; i < 3; i++) prevMeta[i] = input.getIntOr("p" + i, 0);
        power = input.getLongOr("power", 0L);
        lightsOn = input.getBooleanOr("lights", false);
        fertilizer = input.getIntOr("fertilizer", 0);
    }

    public boolean lightsOn() {
        return lightsOn;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tanks[0]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[1]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerHydroponic(containerId, playerInventory, this);
    }
}
