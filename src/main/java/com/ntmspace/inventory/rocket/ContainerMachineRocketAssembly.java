package com.ntmspace.inventory.rocket;

import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
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
 * hbm-space's {@code ContainerMachineRocketAssembly} (192 x 224): the payload slot (18, 13), the shown stage's
 * fuselage / fins / thruster (18, 44 / 62 / 80), the result (42, 91) and the shown stage's "from" and "to" drives
 * (161, 54 / 170, 87); player inventory at (8, 142), hotbar (8, 200).
 *
 * <p>Only one stage's slots are shown at a time (1.7 {@code SlotLayer}: the others moved off screen); the layer is the
 * menu's and the screen changes it with a button, which also tells the VAB which stage's drives to read. The drive
 * slots hold ghost copies: a click puts one of the held drive in (the player keeps theirs) or clears the slot.
 * Shift-clicking from the player inventory does nothing (1.7).
 *
 * <p>Buttons: 0..4 show that stage, 100 construct, 101 deconstruct (1.7 sent the last two as control packets).
 */
public class ContainerMachineRocketAssembly extends CompatContainerMenu {

    public static final int BUTTON_CONSTRUCT = 100;
    public static final int BUTTON_DECONSTRUCT = 101;

    private final Container machine;
    private final @Nullable TileEntityMachineRocketAssembly assembly;
    private int layer = 0;

    public ContainerMachineRocketAssembly(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineRocketAssembly te ? te : null);
    }

    public ContainerMachineRocketAssembly(int containerId, Inventory playerInventory, @Nullable TileEntityMachineRocketAssembly assembly) {
        super(RocketBlocks.ROCKET_ASSEMBLY_MENU.get(), containerId);
        this.assembly = assembly;
        this.machine = assembly != null ? assembly : new SimpleContainer(TileEntityMachineRocketAssembly.SLOT_COUNT);

        int slotId = 0;

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

        // Result
        addSlot(new Slot(machine, slotId++, 42, 91) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ItemCustomRocket; }
        });

        // Drives
        for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
            addSlot(new DriveSlot(machine, slotId++, 161, 54, i));
            addSlot(new DriveSlot(machine, slotId++, 170, 87, i));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 142); // Player inventory, hotbar at 200
    }

    public @Nullable TileEntityMachineRocketAssembly getAssembly() {
        return assembly;
    }

    public int getLayer() {
        return layer;
    }

    /** 1.7 {@code GuiInfoContainerLayered.setLayer} / {@code GuiLayerPacket}. */
    public void setLayer(int layer) {
        this.layer = layer;
        if (assembly != null && !assembly.getLevel().isClientSide()) assembly.setCurrentStage(layer);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < RocketStruct.MAX_STAGES) {
            setLayer(id);
            return true;
        }
        if (assembly == null) return false;
        if (id == BUTTON_CONSTRUCT) {
            assembly.construct();
            return true;
        }
        if (id == BUTTON_DECONSTRUCT) {
            assembly.deconstruct();
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleClick(int index, int button, ClickAction action, Player player) {
        int size = TileEntityMachineRocketAssembly.SLOT_COUNT;

        //L/R: 0
        //M3: 3
        //SHIFT: 1
        //DRAG: 5
        if (index >= size && action == ClickAction.QUICK_MOVE) {
            return true;
        }

        if (index < TileEntityMachineRocketAssembly.SLOT_DRIVES || index >= size) {
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
        if (slot != null && slot.hasItem() && index < TileEntityMachineRocketAssembly.SLOT_DRIVES) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (!moveItemStackTo(stack, TileEntityMachineRocketAssembly.SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
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
