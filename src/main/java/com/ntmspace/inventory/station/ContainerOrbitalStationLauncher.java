package com.ntmspace.inventory.station;

import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.ntmspace.registry.StationLauncherBlocks;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationLauncher;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ContainerOrbitalStationLauncher} (192 x 224): target drive (42, 55), fuel in (42, 73) and out
 * (42, 91), the payload (18, 13), the shown stage's fuselage / fins / thruster (18, 44 / 62 / 80) and its "from" and
 * "to" drives (161, 54 / 170, 87); player inventory at (8, 142), hotbar (8, 200).
 *
 * <p>Only one stage's slots are shown at a time (1.7 {@code SlotLayer}); the layer is the menu's and the screen changes
 * it with a button, which also tells the launcher which stage's drives to read. The stage drive slots hold ghost copies:
 * a click puts one of the held drive in (the player keeps theirs) or clears the slot. Shift-clicking from the player
 * inventory does nothing (1.7). The target drive slot is 1.7 {@code SlotNonRetarded} (asks the launcher what fits); the
 * fuel slots take anything.
 *
 * <p>Buttons: 0..4 show that stage (1.7 {@code GuiLayerPacket}).
 */
public class ContainerOrbitalStationLauncher extends CompatContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityOrbitalStationLauncher launcher;
    private int layer = 0;

    public ContainerOrbitalStationLauncher(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityOrbitalStationLauncher te ? te : null);
    }

    public ContainerOrbitalStationLauncher(int containerId, Inventory playerInventory, @Nullable TileEntityOrbitalStationLauncher launcher) {
        super(StationLauncherBlocks.ORBITAL_STATION_LAUNCHER_MENU.get(), containerId);
        this.launcher = launcher;
        this.machine = launcher != null ? launcher : new SimpleContainer(TileEntityOrbitalStationLauncher.SLOT_COUNT);

        int slotId = 0;

        // Target drive
        addSlot(new Slot(machine, slotId++, 42, 55) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE, stack); }
        });

        // Fuel in
        addSlot(new Slot(machine, slotId++, 42, 73));

        // Fuel out
        addSlot(new Slot(machine, slotId++, 42, 91));

        // Capsule slot
        addSlot(new Slot(machine, slotId++, 18, 13) {
            @Override public boolean mayPlace(ItemStack stack) { return TileEntityMachineRocketAssembly.isCapsule(stack); }
        });

        // Stages
        for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
            addSlot(new PartSlot(machine, slotId++, 18, 44, i, PartType.FUSELAGE));
            addSlot(new PartSlot(machine, slotId++, 18, 62, i, PartType.FINS));
            addSlot(new PartSlot(machine, slotId++, 18, 80, i, PartType.THRUSTER));
        }

        // Drives
        for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
            addSlot(new DriveSlot(machine, slotId++, 161, 54, i));
            addSlot(new DriveSlot(machine, slotId++, 170, 87, i));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 142); // Player inventory, hotbar at 200
    }

    public @Nullable TileEntityOrbitalStationLauncher getLauncher() {
        return launcher;
    }

    public int getLayer() {
        return layer;
    }

    /** 1.7 {@code GuiInfoContainerLayered.setLayer} / {@code GuiLayerPacket} ({@code IStage.setCurrentStage}). */
    public void setLayer(int layer) {
        this.layer = layer;
        if (launcher != null && launcher.getLevel() != null && !launcher.getLevel().isClientSide()) launcher.setCurrentStage(layer);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < RocketStruct.MAX_STAGES) {
            setLayer(id);
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleClick(int index, int button, ClickAction action, Player player) {
        int size = TileEntityOrbitalStationLauncher.SLOT_COUNT;

        //L/R: 0
        //M3: 3
        //SHIFT: 1
        //DRAG: 5
        if (index >= size && action == ClickAction.QUICK_MOVE) {
            return true;
        }

        if (index < TileEntityOrbitalStationLauncher.SLOT_DRIVES || index >= size) {
            return false;
        }

        Slot slot = getSlot(index);
        ItemStack held = getCarried();

        if (!held.isEmpty() && !slot.mayPlace(held)) return true;

        slot.set(held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1));
        slot.setChanged();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 1.7 ContainerBase.transferStackInSlot: machine slots go to the player inventory
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem() && index < TileEntityOrbitalStationLauncher.SLOT_DRIVES) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (!moveItemStackTo(stack, TileEntityOrbitalStationLauncher.SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** 1.7 {@code SlotLayer}: shown only on its stage's layer. */
    private class LayerSlot extends Slot {
        private final int slotLayer;

        LayerSlot(Container container, int index, int x, int y, int layer) {
            super(container, index, x, y);
            this.slotLayer = layer;
        }

        @Override
        public boolean isActive() {
            return layer == slotLayer;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive();
        }
    }

    /** 1.7 {@code SlotRocketPart}: a part of one type. */
    private final class PartSlot extends LayerSlot {
        private final PartType type;

        PartSlot(Container container, int index, int x, int y, int layer, PartType type) {
            super(container, index, x, y, layer);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!super.mayPlace(stack)) return false;
            RocketPart part = RocketParts.get(stack);
            return part != null && part.type() == type;
        }
    }

    /** 1.7 {@code SlotDrive}. */
    private final class DriveSlot extends LayerSlot {
        DriveSlot(Container container, int index, int x, int y, int layer) {
            super(container, index, x, y, layer);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive()) return false;
            return stack.getItem() instanceof ItemVOTVdrive;
        }
    }
}
