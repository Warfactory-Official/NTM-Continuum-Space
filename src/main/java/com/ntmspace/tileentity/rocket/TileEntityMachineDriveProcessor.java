package com.ntmspace.tileentity.rocket;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.ntmspace.inventory.rocket.ContainerDriveProcessor;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.DriveProcessorBlocks;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.SpaceMachineItems;
import com.ntmspace.rocket.ItemVOTVdrive;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityMachineDriveProcessor}: processes a navigation drive ({@code hard_drive_full}) for the
 * body the machine stands on, and clones one onto an empty {@code hard_drive}.
 *
 * <ul>
 *   <li>Slots: 0 the drive, 1 the clone target, 2 the processing circuit ({@code circuit_processt1/2/3} = tier 1/2/3,
 *       anything else 0), 3 a battery. Every slot holds one item (1.7 {@code getInventoryStackLimit}).</li>
 *   <li>2,000 HE buffer; it works only at 75% charge or more (1,500 HE) and burns 200 HE a tick while processing.</li>
 *   <li>"Process" starts if the drive is not yet processed and the circuit's tier is at least
 *       {@link ItemVOTVdrive#getProcessingTier} from this body; 100 ticks later the drive is processed.</li>
 *   <li>"Clone" marks the drive copied and replaces the empty drive in slot 1 with a copy (the copy is marked too).</li>
 * </ul>
 *
 * The status line keeps 1.7's colour codes in the string (1.7 drew it with {@code drawString}, which reads them). The
 * odd bits are 1.7's: "Low tier" is only shown while the circuit stays the same (a circuit change clears the status),
 * and a running process stops only when the drive leaves, the charge drops or the tier is too low.
 */
public class TileEntityMachineDriveProcessor extends TileEntityMachineBase implements MenuProvider, IControlReceiver, IEnergyReceiverMK2 {

    public static final int SLOT_DRIVE = 0;
    public static final int SLOT_CLONE = 1;
    public static final int SLOT_CIRCUIT = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_COUNT = 4;
    public static final long MAX_POWER = 2_000;
    public static final int MAX_PROGRESS = 100;

    public long power;
    public long maxPower = MAX_POWER;

    public boolean isProcessing;
    public int progress;
    public int maxProgress = MAX_PROGRESS; // 5 seconds

    public Status status = Status.NONE;
    public boolean hasDrive = false;

    private int lastTier;

    public TileEntityMachineDriveProcessor(BlockPos pos, BlockState state) {
        super(DriveProcessorBlocks.DRIVE_PROCESSOR_BE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.machineDriveProcessor";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDriveProcessor te) {
        te.update(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDriveProcessor te) {}

    private PortDef[] cachedPorts;

    /** 1.7: power on the six sides of the core. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return cachedPorts;
    }

    /** 1.7 {@code updateEntity} (server side). */
    void update(Level level) {
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, maxPower);
        setupAllPorts(getPorts());
        updatePortPIFIFO();

        if (power < maxPower * 0.75) {
            isProcessing = false;
            status = Status.NO_POWER;
        } else if (!isDrive(getItem(SLOT_DRIVE))) {
            isProcessing = false;
            status = Status.NONE;
        } else if (getProcessingTier() < ItemVOTVdrive.getProcessingTier(getItem(SLOT_DRIVE), Orbit.bodyOf(level))) {
            isProcessing = false;
            status = Status.LOW_TIER;
        }

        if (lastTier != getProcessingTier()) {
            status = Status.NONE;
        }

        if (isProcessing) {
            power -= 200;

            status = Status.PROCESSING;
            progress++;

            if (progress >= maxProgress) {
                progress = 0;
                isProcessing = false;
                ItemVOTVdrive.setProcessed(getItem(SLOT_DRIVE), true);
                setChanged();
                status = Status.DONE;
            }
        } else {
            progress = 0;
        }

        lastTier = getProcessingTier();
        hasDrive = isDrive(getItem(SLOT_DRIVE));

        networkPackNT();
    }

    /** 1.7 {@code getItem() == ModItems.full_drive}: any destination. */
    private static boolean isDrive(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemVOTVdrive;
    }

    /** 1.7 {@code getProcessingTier}: the circuit in slot 2. */
    public int getProcessingTier() {
        ItemStack circuit = getItem(SLOT_CIRCUIT);
        if (circuit.isEmpty()) return 0;
        if (circuit.getItem() == SpaceMachineItems.circuit("PROCESST1")) return 1;
        if (circuit.getItem() == SpaceMachineItems.circuit("PROCESST2")) return 2;
        if (circuit.getItem() == SpaceMachineItems.circuit("PROCESST3")) return 3;
        return 0;
    }

    private void processDrive(boolean process) {
        if (!process) {
            isProcessing = false;
            return;
        }

        if (power < maxPower * 0.75) return;
        if (!isDrive(getItem(SLOT_DRIVE))) return;
        if (ItemVOTVdrive.getProcessed(getItem(SLOT_DRIVE))) return;
        if (level == null) return;

        // Check that our installed upgrade is a high enough tier
        if (getProcessingTier() >= ItemVOTVdrive.getProcessingTier(getItem(SLOT_DRIVE), Orbit.bodyOf(level))) {
            isProcessing = true;
        }
    }

    public void setProcessing(boolean process) {
        processDrive(process);
        setChanged();
    }

    public void cloneDrive() {
        if (power < maxPower * 0.75) return;
        if (!isDrive(getItem(SLOT_DRIVE))) return;
        if (getItem(SLOT_CLONE).isEmpty() || getItem(SLOT_CLONE).getItem() != RocketItems.HARD_DRIVE.get()) {
            status = Status.NO_TARGET;
            return;
        }

        ItemVOTVdrive.markCopied(getItem(SLOT_DRIVE));
        setItem(SLOT_CLONE, getItem(SLOT_DRIVE).copy());

        status = Status.CLONED;
        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("process")) {
            setProcessing(Tags.getBooleanOr(data, "process", false));
        }

        if (data.contains("clone")) {
            cloneDrive();
        }
    }

    /** 1.7 {@code hasPermission = isUseableByPlayer}, asked of the menu that holds this machine. */
    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerDriveProcessor menu && menu.getTile() == this && stillValid(player);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
        buf.writeBoolean(hasDrive);
        buf.writeEnum(status);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
        hasDrive = buf.readBoolean();
        status = buf.readEnum(Status.class);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("isProcessing", isProcessing);
        output.putInt("progress", progress);
        output.putString("status", status.name());
        output.putInt("lastTier", lastTier);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        isProcessing = input.getBooleanOr("isProcessing", false);
        progress = input.getIntOr("progress", 0);
        status = Status.valueOf(input.getStringOr("status", Status.NONE.name()));
        lastTier = input.getIntOr("lastTier", 0);
    }

    public int lastTier() {
        return lastTier;
    }

    /** The seven states of the processor's status line, independent of its rendered text. */
    public enum Status {
        NONE(""),
        NO_POWER("No power ", ChatFormatting.RED),
        LOW_TIER("Low tier ", ChatFormatting.RED),
        PROCESSING("Processing  ", ChatFormatting.GREEN, ChatFormatting.ITALIC),
        DONE("Done! ", ChatFormatting.GREEN),
        NO_TARGET("No target ", ChatFormatting.RED),
        CLONED("Drive cloned ", ChatFormatting.GREEN);

        private final String label;
        private final ChatFormatting[] style;

        Status(String label, ChatFormatting... style) {
            this.label = label;
            this.style = style;
        }

        public Component text() {
            return Component.literal(label).withStyle(style);
        }
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerDriveProcessor(containerId, inventory, this);
    }
}
