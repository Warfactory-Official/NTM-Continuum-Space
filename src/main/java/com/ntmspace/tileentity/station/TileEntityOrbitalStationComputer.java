package com.ntmspace.tileentity.station;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.TileEntityMachineBase;
import com.ntmspace.inventory.ContainerOrbitalStationComputer;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.world.SolarSystem;
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
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityOrbitalStationComputer}: holds the navigation drive of the journey under way (one slot),
 * and takes the station's name and gravity switch from its screen ({@code receiveControl}: {@code name},
 * {@code gravity}).
 */
public class TileEntityOrbitalStationComputer extends TileEntityMachineBase implements MenuProvider, IControlReceiver {

    public boolean hasDrive;

    public TileEntityOrbitalStationComputer(BlockPos pos, BlockState state) {
        super(StationBlocks.ORBITAL_STATION_COMPUTER_BE.get(), pos, state, 1);
    }

    /** 1.7 {@code travelTo}: starts the journey (if the station can) and keeps the drive; false when already there. */
    public boolean travelTo(SolarSystem.Body body, ItemStack drive) {
        if (level == null || level.getServer() == null) return false;
        OrbitalStation station = OrbitalStation.getStationFromPosition(level.getServer(), worldPosition.getX(), worldPosition.getZ());

        if (station.orbiting == body) return false;

        station.travelTo(level, body);
        setItem(0, drive);
        markChanged();

        return true;
    }

    public boolean isTravelling() {
        if (level == null || level.getServer() == null) return false;
        OrbitalStation station = OrbitalStation.getStationFromPosition(level.getServer(), worldPosition.getX(), worldPosition.getZ());
        return station.state != OrbitalStation.StationState.ORBIT;
    }

    @Override
    public String getDefaultName() {
        return "container.orbitalStationComputer";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStationComputer te) {
        te.hasDrive = !te.getItem(0).isEmpty();
        te.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(hasDrive);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        hasDrive = buf.readBoolean();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerOrbitalStationComputer(containerId, inventory, this);
    }

    /**
     * 1.7 answered {@code true} to anyone; HBM's control contract asks a menu-driven receiver to check that the
     * sender's open menu is its own, which is the only way the 1.7 screen sent anything.
     */
    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerOrbitalStationComputer menu && menu.computer() == this;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (level == null || level.getServer() == null) return;
        OrbitalStation station = OrbitalStation.getStationFromPosition(level.getServer(), worldPosition.getX(), worldPosition.getZ());

        if (dev.golem.core.data.Tags.contains(data, "name")) {
            station.name = dev.golem.core.data.Tags.getStringOr(data, "name", "");
        }

        if (dev.golem.core.data.Tags.contains(data, "gravity")) {
            station.gravityMultiplier = dev.golem.core.data.Tags.getBooleanOr(data, "gravity", false) ? 1 : 0;
        }
    }

    /** 1.7 {@code getRenderBoundingBox} (the renderer draws off-screen, so only Forge 1.20.1 asks). */
    public AABB renderBounds() {
        BlockPos p = getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 2, p.getZ() + 1);
    }
}
