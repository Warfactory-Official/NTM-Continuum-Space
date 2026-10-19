package com.ntmspace.tileentity.war;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.tileentity.TileEntityMachineBase;
import com.ntmspace.inventory.war.ContainerMachineWarController;
import com.ntmspace.registry.WarContent;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.satellite.SatelliteWar;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityMachineWarController} ({@code container.warController}): four slots, of which 1 holds a
 * navigation drive ({@code full_drive}) and 2 a satellite chip. Every tick the chip's frequency is read and, while a
 * drive is in slot 1, the satellite on that frequency (orbit-aware, 1.7 {@code SatelliteSavedData.getData(world, x, z)})
 * is aimed at the drive's body if it is a war satellite ({@link SatelliteWar#setTarget}). The GUI's {@code setpos}
 * writes coordinates onto the drive ({@code xcoord}/{@code zcoord}).
 *
 * <p>Ported as found: slots 0 and 3 do nothing; the machine claims to take power (1.7 {@code IEnergyReceiverMK2} with a
 * 0 HE buffer, never subscribed, {@code isLoaded} false), so here it is no energy receiver at all and the GUI shows
 * "0/0HE". An orbit drive has no body: 1.7 crashed on it ({@code getBody()} is null); the port skips it.
 *
 * <p>The GUI read the chosen satellite from the client's satellite cache ({@code getClientSats}); here the server
 * sends what that cache would have held: whether a satellite is on the frequency, its 1.7 class name (the GUI printed
 * {@code getClass().getSimpleName()}) and its {@code health} (1.7 {@code SatelliteBase.health}, never set: 0).
 */
public class TileEntityMachineWarController extends TileEntityMachineBase implements MenuProvider, IControlReceiver {

    public int id;

    /** Client view of the satellite on {@link #id} (see the class comment). */
    public boolean satPresent;
    public String satName = "";
    public int satHealth;

    public TileEntityMachineWarController(BlockPos pos, BlockState state) {
        super(WarContent.WAR_CONTROLLER_BE.get(), pos, state, 4);
    }

    @Override
    public String getDefaultName() {
        return "container.warController";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineWarController te) {
        te.update((ServerLevel) level);
    }

    void update(ServerLevel level) {
        this.id = ISatChip.getFreqS(getItem(2));

        SatelliteSavedData data = SatelliteSavedData.get(level, worldPosition);
        Satellite sat = data.getSatFromFreq(this.id);
        this.satPresent = sat != null;
        this.satName = sat == null ? "" : simpleName(sat);
        this.satHealth = 0;
        networkPackNT();

        ItemStack drive = getItem(1);
        if (drive.isEmpty() || !(drive.getItem() instanceof ItemVOTVdrive)) return;

        ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestination(drive);
        SolarSystem.Body body = destination == null ? null : destination.body.getBody();
        if (body == null) return; // an orbit drive: 1.7 NullPointerException

        if (sat instanceof SatelliteWar satelliteWar) {
            satelliteWar.setTarget(body.name);
        }
    }

    /** 1.7 {@code sat.getClass().getSimpleName()}: the 1.7 class names, which the port's classes keep. */
    private static String simpleName(Satellite sat) {
        return sat.getClass().getSimpleName();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("xcoord") && data.contains("zcoord")) {
            updateDriveCoords(Tags.getIntOr(data, "xcoord", 0), Tags.getIntOr(data, "zcoord", 0));
        }
    }

    private void updateDriveCoords(int x, int z) {
        ItemStack drive = getItem(1);
        if (drive.isEmpty() || !(drive.getItem() instanceof ItemVOTVdrive)) return;
        ItemVOTVdrive.setCoordinates(drive, x, z);
        setChanged();
    }

    /** 1.7 {@code hasPermission = isUseableByPlayer}, asked of the menu that holds this machine. */
    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerMachineWarController menu && menu.getTile() == this && stillValid(player);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(id);
        buf.writeBoolean(satPresent);
        buf.writeUtf(satName);
        buf.writeInt(satHealth);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        id = buf.readInt();
        satPresent = buf.readBoolean();
        satName = buf.readUtf();
        satHealth = buf.readInt();
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerMachineWarController(containerId, inventory, this);
    }
}
