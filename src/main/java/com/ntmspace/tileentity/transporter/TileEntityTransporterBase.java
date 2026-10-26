package com.ntmspace.tileentity.transporter;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidMultiUserMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePort;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import com.hbm.world.chunk.ChunkLoaderTicket;
import com.hbm.world.chunk.ChunkLoadingManager;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.item.ItemTransporterLinker.TransporterInfo;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.net.GolemNetwork;
import java.util.Arrays;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityTransporterBase}: a pad that moves the contents of its input slots and input tanks into
 * the output slots and output tanks of the transporter it is linked to, whenever the subclass allows ({@link #canSend})
 * and pays for it ({@link #hasSent}). Links are made in pairs by the linker ({@link #receiveControl}: {@code linkedTo},
 * {@code unlink}); the name is set from the pad's screen ({@code name}).
 *
 * <p>Inventory layout (1.7): input slots {@code [0, slotCount/2)}, output slots up to {@code slotCount}, then one fluid
 * identifier slot per input tank, then extra slots, then one identifier slot per extra tank. Tanks: input
 * {@code [0, tankCount/2)}, output up to {@code tankCount}, then the extra (fuel) tanks.
 *
 * <p>Port notes: the linked transporter's level is saved by id ({@code "dimension"}, 1.7 {@code dimensionId}); a pad keeps
 * its own chunk loaded through HBM's {@link ChunkLoadingManager} (1.7 {@code ChunkLoaderManager.forceChunk} on placement),
 * claimed on its first server tick and released when the block is removed. As in 1.7, the fuel tanks belong to a separate
 * {@link FuelReceiver} (HIGH priority) whose ports sit at the tank points ({@link #fluidUserAt}); the cargo points reach
 * the pad itself, whose receiving tanks are the cargo input tanks.
 */
public abstract class TileEntityTransporterBase extends TileEntityMachineBase
        implements IControlReceiver, IFluidStandardTransceiverMK2, IFluidMultiUserMK2, WorldlyContainer {

    private String name = "Transporter";

    public final FluidTankNTM[] tanks;

    // The transporter we're sending our contents to
    protected @Nullable TileEntityTransporterBase linkedTransporter;
    private @Nullable TransporterInfo linkedTransporterInfo;

    private final int inputSlotMax;
    private final int outputSlotMax;

    private final int inputTankMax;
    private final int outputTankMax;

    private @Nullable ChunkLoaderTicket ticket;

    protected TileEntityTransporterBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount, int tankCount, int tankSize,
                                        int extraSlots, int extraTanks, int extraTankSize) {
        super(type, pos, state, slotCount + tankCount / 2 + extraSlots + extraTanks);

        tanks = new FluidTankNTM[tankCount + extraTanks];
        for (int i = 0; i < tankCount; i++) {
            tanks[i] = new FluidTankNTM(Fluids.NONE, tankSize);
        }
        for (int i = tankCount; i < tankCount + extraTanks; i++) {
            tanks[i] = new FluidTankNTM(Fluids.NONE, extraTankSize);
        }

        inputSlotMax = slotCount / 2;
        outputSlotMax = slotCount;

        inputTankMax = tankCount / 2;
        outputTankMax = tankCount;
    }

    @Override
    public String getDefaultName() {
        return "container.transporter";
    }

    public int inputSlotMax() { return inputSlotMax; }
    public int outputSlotMax() { return outputSlotMax; }
    public int inputTankMax() { return inputTankMax; }
    public int outputTankMax() { return outputTankMax; }

    /** 1.7 {@code updateEntity}, server half. */
    protected void updateServer() {
        if (!(level instanceof ServerLevel server)) return;

        // 1.7 ChunkLoaderManager.forceChunk (placement): the pad keeps its own chunk loaded
        if (ticket == null || ticket.isReleased()) {
            ticket = ChunkLoadingManager.requestTicket(server, "ntmspace:transporter");
            ticket.force(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        }

        // Set tank types and split fills
        for (int i = 0; i < inputTankMax; i++) {
            setType(tanks[i], outputSlotMax + i);

            // Evenly distribute fluids between all matching tanks
            for (int o = i + 1; o < inputTankMax; o++) {
                splitFill(tanks[i], tanks[o]);
            }
        }
        for (int i = inputTankMax; i < outputTankMax; i++) {
            for (int o = i + 1; o < outputTankMax; o++) {
                splitFill(tanks[i], tanks[o]);
            }
        }
        for (int i = outputTankMax; i < tanks.length; i++) {
            setType(tanks[i], outputSlotMax + inputTankMax + i - outputTankMax);
        }

        updateConnections();

        fetchLinkedTransporter();

        if (linkedTransporter != null && canSend(linkedTransporter)) {
            boolean isDirty = false;

            int sentItems = 0;
            int sentFluid = 0;

            // Move all items into the target
            for (int i = 0; i < inputSlotMax; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    int beforeSize = stack.getCount();
                    ItemStack rest = tryAddItemToInventory(linkedTransporter, linkedTransporter.inputSlotMax, linkedTransporter.outputSlotMax - 1, stack);
                    inventory.setStackInSlot(i, rest);
                    int afterSize = rest.getCount();
                    sentItems += beforeSize - afterSize;
                    isDirty = true;
                }
            }

            // Move all fluids into the target
            for (int i = 0; i < inputTankMax; i++) {
                int o = i + inputTankMax;

                linkedTransporter.tanks[o].setTankType(tanks[i].getTankType());

                var moved = tanks[i].transferTo(linkedTransporter.tanks[o], tanks[i].getFill());
                if (moved > 0) {
                    sentFluid += moved;
                    isDirty = true;
                }
            }

            hasSent(linkedTransporter, sentItems + (sentFluid / 1000));

            if (isDirty) {
                markChanged();
                linkedTransporter.markChanged();
            }
        }

        networkPackNT();
    }

    /** 1.7 {@code FluidTank.setType(in, slots)} with in == out: an identifier in the slot retypes the tank (and empties it). */
    private void setType(FluidTankNTM tank, int slot) {
        ItemStack id = inventory.getStackInSlot(slot);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId identifier)) return;
        tank.setTankTypeByIdentifier(identifier.getType(null, 0, 0, 0, id));
    }

    /**
     * 1.7 {@code InventoryUtil.tryAddItemToInventory} on the target's raw slots (no slot rules): first onto matching stacks
     * in {@code start..end}, then the rest into the first empty slot. Returns what is left.
     */
    private static ItemStack tryAddItemToInventory(TileEntityTransporterBase target, int start, int end, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            ItemStack held = target.inventory.getStackInSlot(i);
            if (!held.isEmpty() && dev.golem.core.world.ItemStacks.sameItemAndData(held, rest)) {
                int transfer = Math.min(rest.getCount(), held.getMaxStackSize() - held.getCount());
                if (transfer > 0) {
                    ItemStack grown = held.copy();
                    grown.grow(transfer);
                    target.inventory.setStackInSlot(i, grown);
                    rest.shrink(transfer);
                }
            }
        }
        if (rest.isEmpty()) return ItemStack.EMPTY;
        for (int i = start; i <= end; i++) {
            if (target.inventory.getStackInSlot(i).isEmpty()) {
                target.inventory.setStackInSlot(i, rest);
                return ItemStack.EMPTY;
            }
        }
        return rest;
    }

    /**
     * 1.7 {@code updateConnections}: cargo tanks at the cargo points, fuel tanks at the tank points; the inserter pulls one
     * item a tick from the inventory at {@link #getInsertPos}, the extractor pushes one into the one at {@link #getExtractPos}.
     */
    private void updateConnections() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(level, getBlockState(), worldPosition, getConPos());
        setupFluidPorts(cachedPorts);
        updatePortFIFO();
        if (fuelPorts == null) {
            PortDef fuel = PortDef.combine(MachinePorts.ports(level, getBlockState(), worldPosition, getTankPos()));
            fuelPorts = TilePort.oneToMany(fuelReceiver, tanks.length - outputTankMax, fuel);
        }
        for (int i = 0; i < fuelPorts.length; i++) {
            fuelPorts[i].setupFluids(tanks[outputTankMax + i], level);
            fuelPorts[i].update(level);
            fuelPorts[i].checkSubscribe(level);
        }
        for (DirPos pos : getInsertPos()) tryLoad(pos.getPos(), pos.getDir());
        for (DirPos pos : getExtractPos()) tryUnload(pos.getPos(), pos.getDir());
    }

    private void tryLoad(BlockPos pos, ForgeDirection dir) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof Container inv) || inv == this) return;

        WorldlyContainer sided = inv instanceof WorldlyContainer w ? w : null;
        Direction side = dir.toDirection();
        int[] access = sided != null ? sided.getSlotsForFace(side) : null;

        for (int i = 0; i < (access != null ? access.length : inv.getContainerSize()); i++) {
            int slot = access != null ? access[i] : i;
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && (sided == null || sided.canTakeItemThroughFace(slot, stack, side))) {
                for (int j = 0; j < inputSlotMax; j++) {
                    ItemStack held = inventory.getStackInSlot(j);
                    // 1.7 used a non-short-circuit & here; both sides are side-effect free
                    if (!held.isEmpty() && held.getCount() < held.getMaxStackSize() & dev.golem.core.world.ItemStacks.sameItemAndData(held, stack)) {
                        inv.removeItem(slot, 1);
                        inv.setChanged();
                        ItemStack grown = held.copy();
                        grown.grow(1);
                        inventory.setStackInSlot(j, grown);
                        return;
                    }
                }

                for (int j = 0; j < inputSlotMax; j++) {
                    if (inventory.getStackInSlot(j).isEmpty()) {
                        ItemStack one = stack.copyWithCount(1);
                        inv.removeItem(slot, 1);
                        inv.setChanged();
                        inventory.setStackInSlot(j, one);
                        return;
                    }
                }
            }
        }
    }

    private void tryUnload(BlockPos pos, ForgeDirection dir) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof Container inv) || inv == this) return;

        WorldlyContainer sided = inv instanceof WorldlyContainer w ? w : null;
        Direction side = dir.toDirection();
        int[] access = sided != null ? sided.getSlotsForFace(side) : null;

        for (int i = inputSlotMax; i < outputSlotMax; i++) {
            ItemStack out = inventory.getStackInSlot(i);

            if (!out.isEmpty()) {
                for (int j = 0; j < (access != null ? access.length : inv.getContainerSize()); j++) {
                    int slot = access != null ? access[j] : j;

                    if (!inv.canPlaceItem(slot, out)) continue;

                    ItemStack target = inv.getItem(slot);

                    if (!target.isEmpty() && dev.golem.core.world.ItemStacks.sameItemAndData(out, target)
                            && target.getCount() < Math.min(target.getMaxStackSize(), inv.getMaxStackSize())) {
                        removeItem(i, 1);
                        target.grow(1);
                        inv.setChanged();
                        return;
                    }
                }

                for (int j = 0; j < (access != null ? access.length : inv.getContainerSize()); j++) {
                    int slot = access != null ? access[j] : j;

                    if (!inv.canPlaceItem(slot, out)) continue;

                    if (inv.getItem(slot).isEmpty() && (sided != null ? sided.canPlaceItemThroughFace(slot, out, side) : inv.canPlaceItem(slot, out))) {
                        inv.setItem(slot, out.copyWithCount(1));
                        removeItem(i, 1);
                        return;
                    }
                }
            }
        }
    }

    // splitting is commutative, order don't matter
    private static void splitFill(FluidTankNTM in, FluidTankNTM out) {
        in.balanceWith(out);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return Arrays.copyOfRange(tanks, inputTankMax, outputTankMax);
    }

    /** 1.7: the cargo input tanks only; the fuel tanks receive through {@link FuelReceiver}. */
    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return Arrays.copyOfRange(tanks, 0, inputTankMax);
    }

    /** 1.7 {@code trySubscribeFuel} at {@code getTankPos}: a duct sitting on a tank point reaches the fuel receiver. */
    @Override
    public @Nullable IFluidUserMK2 fluidUserAt(BlockPos pos, @Nullable Direction face) {
        if (face == null) return null;
        BlockPos duct = pos.relative(face);
        for (DirPos point : getTankPos()) {
            if (point.getPos().equals(duct)) return fuelReceiver;
        }
        return null;
    }

    /** The fuel receiver, one instance per pad (the networks tell users apart by identity). */
    public IFluidUserMK2 fuelReceiver() {
        return fuelReceiver;
    }

    private final FuelReceiver fuelReceiver = new FuelReceiver();
    private PortDef[] cachedPorts;
    private TilePort[] fuelPorts;

    /** 1.7 {@code FuelReceiver}: receives into the extra (fuel) tanks, HIGH priority; gone with the pad (1.7 {@code invalidate}). */
    private class FuelReceiver implements IFluidStandardReceiverMK2 {

        @Override
        public boolean isLoaded() {
            return !isRemoved() && TileEntityTransporterBase.this.isLoaded();
        }

        @Override
        public FluidTankNTM[] getAllTanks() {
            return TileEntityTransporterBase.this.getAllTanks();
        }

        @Override
        public FluidTankNTM[] getReceivingTanks() {
            return Arrays.copyOfRange(tanks, outputTankMax, tanks.length);
        }

        @Override
        public ConnectionPriority getFluidPriority() {
            return ConnectionPriority.HIGH;
        }
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    // ---------------------------------------------------------------------------------------------- sync

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);

        if (linkedTransporterInfo != null) {
            buf.writeBoolean(true);
            buf.writeUtf(linkedTransporterInfo.dimension);
            buf.writeInt(linkedTransporterInfo.x);
            buf.writeInt(linkedTransporterInfo.y);
            buf.writeInt(linkedTransporterInfo.z);
        } else {
            buf.writeBoolean(false);
        }

        for (FluidTankNTM tank : tanks) tank.serialize(buf);

        buf.writeUtf(name);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);

        linkedTransporter = null;
        if (buf.readBoolean()) {
            String dimension = buf.readUtf();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            linkedTransporterInfo = new TransporterInfo("Linked Transporter", dimension, x, y, z);
        } else {
            linkedTransporterInfo = null;
        }

        for (FluidTankNTM tank : tanks) tank.deserialize(buf);

        name = buf.readUtf();
    }

    // ---------------------------------------------------------------------------------------------- subclass

    protected abstract DirPos[] getConPos();
    protected abstract DirPos[] getTankPos();
    protected abstract DirPos[] getInsertPos();
    protected abstract DirPos[] getExtractPos();

    // Designated overrides for delaying sending or requiring fuel
    protected abstract boolean canSend(TileEntityTransporterBase linkedTransporter);
    protected abstract void hasSent(TileEntityTransporterBase linkedTransporter, int quantitySent);
    protected abstract void hasConnected(TileEntityTransporterBase linkedTransporter);

    // Turns items and fluids into a "mass" of sorts
    protected int itemCount() {
        int count = 0;
        for (int i = 0; i < inputSlotMax; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) count += stack.getCount();
        }
        for (int i = 0; i < inputTankMax; i++) {
            count += tanks[i].getFill() / 1000;
        }
        return count;
    }

    public String getTransporterName() {
        return name;
    }

    /** 1.7: sets the name here and sends it to the server (the client side of the pad's screen). */
    public void setTransporterName(String name) {
        this.name = name;
        CompoundTag data = new CompoundTag();
        data.putString("name", name);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(worldPosition, data));
    }

    private void fetchLinkedTransporter() {
        if (linkedTransporter != null && linkedTransporter.isRemoved()) linkedTransporter = null;
        if (linkedTransporter == null && linkedTransporterInfo != null && level != null && level.getServer() != null) {
            ServerLevel transporterWorld = level.getServer().getLevel(ItemTransporterLinker.levelKey(linkedTransporterInfo.dimension));
            if (transporterWorld == null) return;
            BlockPos pos = linkedTransporterInfo.pos();
            // 1.7 loaded the chunk to read its tile entity; the other pad keeps its chunk loaded itself
            if (!transporterWorld.isLoaded(pos)) return;
            BlockEntity te = transporterWorld.getBlockEntity(pos);
            if (te instanceof TileEntityTransporterBase transporter) {
                linkedTransporter = transporter;
            }
        }
    }

    public @Nullable TransporterInfo getLinkedTransporter() {
        return linkedTransporterInfo;
    }

    /** The linked block entity, as last fetched (server). */
    public @Nullable TileEntityTransporterBase linkedTransporterEntity() {
        return linkedTransporter;
    }

    // ---------------------------------------------------------------------------------------------- automation

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, outputSlotMax).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= inputSlotMax && slot < outputSlotMax;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < inputSlotMax;
    }

    // ---------------------------------------------------------------------------------------------- save

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        name = input.getStringOr("name", "");
        linkedTransporter = null;
        String dimension = input.getStringOr("dimension", "");
        int[] coords = input.getIntArrayOr("linkedTo", new int[0]);
        if (coords.length >= 3 && !dimension.isEmpty()) {
            linkedTransporterInfo = new TransporterInfo("Linked Transporter", dimension, coords[0], coords[1], coords[2]);
        } else {
            linkedTransporterInfo = null;
        }
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("name", name);
        if (linkedTransporterInfo != null) {
            output.putString("dimension", linkedTransporterInfo.dimension);
            output.putIntArray("linkedTo", new int[] {linkedTransporterInfo.x, linkedTransporterInfo.y, linkedTransporterInfo.z});
        }
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    // ---------------------------------------------------------------------------------------------- links

    public void unlinkTransporter() {
        if (linkedTransporter != null) {
            linkedTransporter.linkedTransporter = null;
            linkedTransporter.linkedTransporterInfo = null;
            linkedTransporter.markChanged();
        }

        linkedTransporter = null;
        linkedTransporterInfo = null;
    }

    @Override
    public void setRemoved() {
        if (level != null && fuelPorts != null) for (TilePort port : fuelPorts) port.disableIfPresent(level);
        super.setRemoved();
    }

    /** 1.7 {@code BlockTransporterRocket.breakBlock}: unforce the chunk and unlink the pair; then the inventory drops. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            fetchLinkedTransporter();
            unlinkTransporter();
            if (ticket != null) {
                ticket.release();
                ticket = null;
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    // Is commutative, will automatically link and unlink its pair
    @Override
    public void receiveControl(CompoundTag nbt) {
        if (Tags.contains(nbt, "name")) name = Tags.getStringOr(nbt, "name", name);
        if (Tags.contains(nbt, "unlink")) {
            fetchLinkedTransporter();
            unlinkTransporter();
        }
        if (Tags.contains(nbt, "linkedTo")) {
            fetchLinkedTransporter();
            // If already linked, unlink the target
            if (linkedTransporter != null) {
                linkedTransporter.linkedTransporter = null;
                linkedTransporter.linkedTransporterInfo = null;
                linkedTransporter.markChanged();
            }

            linkedTransporter = null;

            int[] coords = Tags.getIntArrayOr(nbt, "linkedTo", new int[0]);
            String dimension = Tags.getStringOr(nbt, "dimension", "");
            if (coords.length >= 3 && !dimension.isEmpty()) {
                linkedTransporterInfo = new TransporterInfo("Linked Transporter", dimension, coords[0], coords[1], coords[2]);

                fetchLinkedTransporter();

                if (linkedTransporter != null && level != null) {
                    linkedTransporter.linkedTransporterInfo = TransporterInfo.from(ItemTransporterLinker.dimensionOf(level), this);
                    linkedTransporter.fetchLinkedTransporter();

                    hasConnected(linkedTransporter);
                    linkedTransporter.markChanged();
                }
            }
        }

        markChanged();
        setChanged();
    }

    /** 1.7 {@code isUseableByPlayer}: within the machine's reach (the linker's screen sends from the held item, not a menu). */
    @Override
    public boolean hasPermission(Player player) {
        return level != null && BlockMultiblockCore.isWithinInteractionRange(level, worldPosition, player);
    }
}
