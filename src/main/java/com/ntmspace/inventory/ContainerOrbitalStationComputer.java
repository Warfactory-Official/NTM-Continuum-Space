package com.ntmspace.inventory;

import com.ntmspace.registry.StationBlocks;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** hbm-space's {@code ContainerOrbitalStationComputer}: no slots at all; the screen only sends controls. */
public final class ContainerOrbitalStationComputer extends AbstractContainerMenu {

    private final @Nullable TileEntityOrbitalStationComputer computer;

    public ContainerOrbitalStationComputer(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityOrbitalStationComputer c ? c : null);
    }

    public ContainerOrbitalStationComputer(int containerId, Inventory playerInventory, @Nullable TileEntityOrbitalStationComputer computer) {
        super(StationBlocks.ORBITAL_STATION_COMPUTER_MENU.get(), containerId);
        this.computer = computer;
    }

    public @Nullable TileEntityOrbitalStationComputer computer() {
        return computer;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 1.7 {@code canInteractWith}: always. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
