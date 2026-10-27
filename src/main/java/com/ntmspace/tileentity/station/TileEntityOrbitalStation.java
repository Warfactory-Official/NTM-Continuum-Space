package com.ntmspace.tileentity.station;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.ntmspace.block.station.BlockOrbitalStation;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationDock;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityOrbitalStation}: a docking port (and, for the core, the station itself). Every server
 * tick in orbit it registers with its station ({@link OrbitalStation#addPort}); the core also ticks the station's travel
 * ({@link OrbitalStation#update}) and sends the station to the clients near it, which becomes
 * {@link OrbitalStation#clientStation}.
 *
 * <p>A docked reusable craft ({@link StationDock}) is refuelled from the port's hydrazine tank: the tank is resized to the
 * fuel the craft needs to reach the ground of the orbited body ({@link #getFillRequirement}; 500 mB for a hop to another
 * station) and takes hydrazine from ducts only while such a craft is docked. When the craft leaves, the fuel it used is
 * drained; 16 slots hold what a stowed craft leaves behind.
 */
public class TileEntityOrbitalStation extends TileEntityMachineBase implements IFluidStandardReceiverMK2 {

    private @Nullable OrbitalStation station;
    private @Nullable StationDock docked;

    public final FluidTankNTM[] tanks;
    private boolean subscribed;

    public boolean isReserved = false;

    // Client synced state information
    public boolean hasDocked = false;
    public boolean hasRider = false;
    public boolean needsFuel = false;
    public boolean hasFuel = false;
    /** The names of the stored items, for the look overlay (1.7 synced item ids). */
    public List<String> storedNames = new ArrayList<>();

    public float rot;
    public float prevRot;

    public TileEntityOrbitalStation(BlockPos pos, BlockState state) {
        super(StationBlocks.ORBITAL_STATION_BE.get(), pos, state, 16);
        tanks = new FluidTankNTM[] {new FluidTankNTM(SpaceFluids.HYDRAZINE, 16_000)};
    }

    @Override
    public String getDefaultName() {
        return "container.orbitalStation";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStation te) {
        if (!Orbit.inOrbit(level) || level.getServer() == null) return;

        // Station TEs handle syncing information about the current orbital parameters to players on the station
        te.station = OrbitalStation.getStationFromPosition(level.getServer(), pos.getX(), pos.getZ());

        if (te.isCore()) te.station.update(level);
        te.station.addPort(te);

        StationDock docked = te.docked;
        te.subscribed = false;
        if (docked != null && docked.isReusable()) {
            int fillRequirement = te.getFillRequirement(false); // Use higher fill requirement for tank sizing

            // Update tank sizes based on fuel requirement, preserving existing fills
            for (FluidTankNTM tank : te.tanks) tank.changeTankSize(Math.max(fillRequirement, tank.getFill()));

            // Connections: 1.7 subscribed the rim to its tank only now
            te.subscribed = true;
        }

        te.setupFluidInPorts(te.tanks, PortDef.combine(BlockOrbitalStation.rimPorts(pos)));
        te.updateAllPorts();
        if (te.subscribed) te.receiveFluid(te.tanks);

        if (docked != null && (docked.entity().isRemoved() || docked.isUndocking())) {
            // Drain tanks only upon successful undocking, just in case the pod gets stashed before the player can launch (in multi-player stations)
            if (!docked.entity().isRemoved() && docked.isReusable()) {
                boolean toOrbit = docked.targetInOrbit();
                for (FluidTankNTM tank : te.tanks) tank.changeTankSize(Math.max(0, tank.getFill() - te.getFillRequirement(toOrbit)));
            }
            te.undockRocket();
            docked = null;
        }

        // if we have enough fuel, transition to ready to launch
        if (docked != null && docked.isReusable()) {
            docked.fuelled(te.hasSufficientFuel(docked.targetInOrbit()));
        }

        te.hasDocked = docked != null;
        te.hasRider = te.hasDocked && docked.hasRider();
        te.needsFuel = te.hasDocked && docked.isReusable();
        te.hasFuel = te.needsFuel && te.hasSufficientFuel(docked.targetInOrbit());

        if (te.hasDocked) te.isReserved = false;

        te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStation te) {
        if (!Orbit.inOrbit(level)) return;
        if (te.isCore() && te.station != null) te.station.update(level);

        te.prevRot = te.rot;
        if (te.hasDocked) {
            te.rot += 2.25F;
            if (te.rot > 90) te.rot = 90;
        } else {
            te.rot -= 2.25F;
            if (te.rot < 0) te.rot = 0;
        }
    }

    /** 1.7 {@code invalidate}: a removed port leaves its station and drops its craft. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && station != null) {
            if (!isCore()) station.removePort(this);
            if (docked != null) docked.dropAndDie();
        }
    }

    public boolean isCore() {
        return getBlockState().getBlock() instanceof BlockOrbitalStation block && block.isCore();
    }

    public void enterCapsule(Player player) {
        if (docked == null || docked.hasRider()) return;
        docked.board(player);
    }

    public void dockRocket(StationDock rocket) {
        despawnRocket();
        docked = rocket;
    }

    public void undockRocket() {
        docked = null;
    }

    /** 1.7 {@code despawnRocket}: the craft's items go into the first free slots (nav drive first), the craft goes. */
    public void despawnRocket() {
        if (docked == null) return;
        Deque<ItemStack> itemsToStuff = new ArrayDeque<>(docked.stow());
        for (int i = 0; i < getContainerSize() && !itemsToStuff.isEmpty(); i++) {
            if (getItem(i).isEmpty()) setItem(i, itemsToStuff.pop());
        }
        docked = null;
    }

    public void reservePort() {
        isReserved = true;
    }

    public @Nullable StationDock getDocked() {
        return docked;
    }

    public boolean hasStoredItems() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return true;
        }
        return false;
    }

    public void giveStoredItems(Player player) {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
                setItem(i, ItemStack.EMPTY);
            }
        }
        player.inventoryMenu.broadcastChanges();
        setChanged();
    }

    public boolean hasSufficientFuel(boolean toOrbit) {
        int fillRequirement = getFillRequirement(toOrbit);
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() < fillRequirement) return false;
        }
        return true;
    }

    /**
     * 1.7 {@code getFillRequirement}: 500 mB to another station; otherwise the fuel to land a craft of its launch mass
     * (4,000 kg with none) from the orbited body's orbit, at the tank fluid's rocket thrust and ISP (600 kN and 300 s
     * without the trait).
     */
    public int getFillRequirement(boolean toOrbit) {
        if (toOrbit) return 500; // Transferring between stations is much cheaper

        int mass = docked != null ? docked.launchMass() : 4_000;

        FT_Rocket fuelTrait = tanks[0].getTankType().getTrait(FT_Rocket.class);
        int thrust = fuelTrait != null ? (int) fuelTrait.getThrust() : 600_000;
        int isp = fuelTrait != null ? fuelTrait.getISP() : 300;

        SolarSystem.Body body = station != null ? station.orbiting : SolarSystem.body("kerbin");
        return SolarSystem.getCostBetween(body, body, mass, thrust, isp, false, true, Orbit.pressureOf(body), Orbit.pressureOf(body));
    }

    /** Whether {@code stack} is a craft a free port would dock ({@link StationDock#CRAFT_ITEMS}). */
    public static boolean isCraftItem(ItemStack stack) {
        return StationDock.CRAFT_ITEMS.invoker().test(stack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    // ---------------------------------------------------------------------------------------- sync and save

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);

        buf.writeBoolean(isCore() && station != null);
        if (isCore() && station != null) station.serialize(buf);

        buf.writeBoolean(hasDocked);
        buf.writeBoolean(hasRider);
        buf.writeBoolean(needsFuel);
        buf.writeBoolean(hasFuel);

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        buf.writeVarInt(ids.size());
        for (String id : ids) buf.writeUtf(id);

        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);

        if (buf.readBoolean()) OrbitalStation.clientStation = station = OrbitalStation.deserialize(buf);

        hasDocked = buf.readBoolean();
        hasRider = buf.readBoolean();
        needsFuel = buf.readBoolean();
        hasFuel = buf.readBoolean();

        List<String> names = new ArrayList<>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            names.add(new ItemStack(BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.parse(id))).getHoverName().getString());
        }
        storedNames = names;

        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
        // Migrate old hydrogen tanks
        tanks[0].setTankType(SpaceFluids.HYDRAZINE);
    }

    /** 1.7 {@code getRenderBoundingBox} (the renderer draws off-screen, so only Forge 1.20.1 asks). */
    public AABB renderBounds() {
        BlockPos p = getBlockPos();
        return new AABB(p.getX() - 2, p.getY() - 2, p.getZ() - 2, p.getX() + 3, p.getY() + 2, p.getZ() + 3);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return subscribed ? tanks : new FluidTankNTM[0];
    }

    /** The immediate face query follows the dock state; network storage remains subscription-gated above. */
    @Override
    public boolean exposesFluidContact(@Nullable FluidType type,
                                       ForgeDirection dir, boolean receiving) {
        return needsFuel && IFluidStandardReceiverMK2.super.exposesFluidContact(type, dir, receiving);
    }

    /** The station this port belongs to, once it ticked (server), or the synced one (client core). */
    public @Nullable OrbitalStation station() {
        return station;
    }

    /** The fluid this port refuels with (GameTests). */
    public FluidType fuel() {
        return tanks[0].getTankType();
    }
}
