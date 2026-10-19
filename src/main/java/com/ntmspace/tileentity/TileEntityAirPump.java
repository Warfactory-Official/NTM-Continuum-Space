package com.ntmspace.tileentity;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.ntmspace.atmosphere.AtmosphereBlob;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.atmosphere.AtmosphereProvider;
import com.ntmspace.registry.AtmosphereContent;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityAirPump}, the air vent: while its tank (16,000 mB of oxygen by default, any gas set with
 * a fluid identifier) holds at least 20 mB it keeps a sealed room ({@link AtmosphereBlob}) rooted at itself, filling it
 * 1 mB per block at up to 20 a tick and then trickling (each trickled mB becomes waste gas a CO2 scrubber can take and
 * plants can return). The room's pressure is {@code filled / volume * 0.2} atm, so a full room holds 0.2 atm of oxygen,
 * enough to breathe (0.09) and to burn (0.09). An unsealed room vents 20 mB a tick into the void.
 *
 * <p>Values and order are 1.7's. The OpenComputers component (getAtmosphereDetails/getFluid/isSealed) belongs to the
 * computer port.
 */
public class TileEntityAirPump extends TileEntityLoadedBase implements IFluidStandardReceiverMK2, AtmosphereProvider {

    private int onTicks = 0;
    private boolean registered = false;

    // Bubble registration wait, so it has time to fill out the bubble before running (for recovering fill in a chunk that just loaded)
    private int recovering = 0;

    // Tile initialisation wait, for systems that run too fast and finish bubbling before chunks have time to load
    private int registerWait = 10;

    private @Nullable AtmosphereBlob currentBlob;
    private int airFill = 0; // How many blocks are filled with air in the blob
    private int wasteFill = 0; // How many air blocks worth of waste gas has been produced
    private int bonusFill = 0; // How much waste gas has been returned, is consumed before the tank

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.OXYGEN, 16000);

    private final Random rand = new Random();

    /** Used for synchronizing printable info. */
    public @Nullable CBT_Atmosphere currentAtmosphere;

    private @Nullable CO2Scrubber scrubber;

    /** Client: draws the vent's gas plume (set by the client start-up; the plume is HBM's cooling-tower particle). */
    public static volatile @Nullable BiConsumer<TileEntityAirPump, Integer> CLIENT_PARTICLES;

    /**
     * hbm-space's {@code TileEntityAirScrubber} as the vent sees it: it takes waste gas (CO2) out of the room for power.
     * The scrubber machine (a two-high OBJ multiblock) is left to the machine port; it implements this.
     */
    public interface CO2Scrubber {
        boolean isLoaded();
        boolean isRemoved();
        boolean canOperate();
        /** Takes up to {@code amount} of waste gas, returns how much it took. */
        int scrub(int amount);
    }

    public TileEntityAirPump(BlockPos pos, BlockState state) {
        this(AtmosphereContent.AIR_VENT_ENTITY.get(), pos, state);
    }

    protected TileEntityAirPump(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAirPump pump) {
        pump.updateServer(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityAirPump pump) {
        if (pump.onTicks > 0 && level.getGameTime() % 2 == 0) {
            BiConsumer<TileEntityAirPump, Integer> particles = CLIENT_PARTICLES;
            if (particles != null) particles.accept(pump, pump.tank.getTankType().getColor());
        }
    }

    private void updateServer(Level level) {
        setupAllPorts(getPorts());
        updatePortFIFO();
        if (onTicks > 0) onTicks--;
        if (registerWait > 0) registerWait--;

        if (tank.getFill() + bonusFill >= 20) {
            onTicks = 20;

            if (registerWait > 0) {
                // do nothing
            } else if (!registered) {
                AtmosphereHandler.registerAtmosphere(this);
                registered = true;
                if (airFill > 1) recovering = 100;
            } else if (recovering > 0) {
                recovering--;
                if (currentBlob != null) recovering = 0;
            } else {
                if (currentBlob != null) {
                    int size = currentBlob.getBlobSize();
                    if (size != 0) {
                        if (airFill > size) airFill = size;
                        if (wasteFill > size) wasteFill = size;

                        // Fill the blob from the tank, 1mB per block
                        int toFill = Math.min(size - airFill, 20);
                        airFill += toFill;

                        // Fill to the brim, and then trickle randomly afterwards
                        if (toFill > 0) {
                            consumeFromTank(toFill);
                        } else if (rand.nextBoolean()) {
                            consumeFromTank(1);
                            wasteFill++;
                        }

                        // Fill scrubbers with waste gas
                        if (scrubber != null && scrubber.isLoaded() && !scrubber.isRemoved()) {
                            int toScrub = Mth.clamp(wasteFill, 0, 20);
                            int scrubbed = scrubber.scrub(toScrub);
                            wasteFill -= scrubbed;
                        }
                    } else {
                        currentBlob = null;
                    }
                }

                if (currentBlob == null) {
                    // Venting to vacuum
                    consumeFromTank(20);
                    airFill = 0;
                    wasteFill = 0;
                }
            }
        } else {
            if (registered) {
                AtmosphereHandler.unregisterAtmosphere(this);
                registered = false;
                currentBlob = null;
                airFill = 0;
                wasteFill = 0;
            }
        }

        if (level.getGameTime() % 5 == 0) {
            currentAtmosphere = AtmosphereHandler.getAtmosphere(level, worldPosition);
        }

        setChanged();
        networkPackNT();
    }

    private void consumeFromTank(int amount) {
        int bonusConsumption = Mth.clamp(amount, 0, bonusFill);
        bonusFill -= bonusConsumption;
        amount -= bonusConsumption;
        // 1.7 set the fill unclamped: the tank may read below zero for a tick when it vents; clamped here
        tank.setFill(Math.max(tank.getFill() - amount, 0));
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (registered) {
            AtmosphereHandler.unregisterAtmosphere(this);
            registered = false;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(onTicks);
        buf.writeInt(airFill);
        buf.writeInt(wasteFill);
        buf.writeInt(bonusFill);
        tank.serialize(buf);
        if (currentAtmosphere != null) {
            buf.writeBoolean(true);
            currentAtmosphere.writeToBytes(buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        onTicks = buf.readInt();
        airFill = buf.readInt();
        wasteFill = buf.readInt();
        bonusFill = buf.readInt();
        tank.deserialize(buf);
        if (buf.readBoolean()) {
            currentAtmosphere = new CBT_Atmosphere();
            currentAtmosphere.readFromBytes(buf);
        } else {
            currentAtmosphere = null;
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "at");
        output.putInt("fill", airFill);
        output.putInt("waste", wasteFill);
        output.putInt("bonus", bonusFill);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "at");
        airFill = input.getIntOr("fill", 0);
        wasteFill = input.getIntOr("waste", 0);
        bonusFill = input.getIntOr("bonus", 0);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    private PortDef[] cachedPorts;

    /** 1.7 {@code subscribeToAllAround}: the vent takes its gas on all six sides. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return cachedPorts;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[] {tank};
    }

    // AtmosphereProvider

    @Override
    public int getMaxBlobRadius() {
        return 256;
    }

    @Override
    public BlockPos getRootPosition() {
        return worldPosition;
    }

    @Override
    public FluidType getFluidType() {
        return tank.getTankType();
    }

    @Override
    public double getFluidPressure() {
        if (currentBlob == null || currentBlob.getBlobSize() == 0) return 0;
        return ((double) airFill / (double) currentBlob.getBlobSize()) * 0.2;
    }

    public boolean hasSeal() {
        return airFill > 1;
    }

    @Override
    public void onBlobCreated(AtmosphereBlob blob) {
        currentBlob = blob;
    }

    @Override
    public void consume(int amount) {
        airFill -= amount;
        wasteFill += amount;
        if (airFill < 1) airFill = 1;
    }

    @Override
    public void produce(int amount) {
        int produced = Mth.clamp(amount, 0, wasteFill);
        bonusFill += produced;
        wasteFill -= produced;
    }

    public boolean isRecycling() {
        return bonusFill > 0;
    }

    /** The current room, or null (for tests and the look overlay). */
    public @Nullable AtmosphereBlob blob() {
        return currentBlob;
    }

    public int airFill() {
        return airFill;
    }

    /** 1.7 {@code registerScrubber}: a powered CO2 scrubber in the room claims this vent's waste gas. */
    public boolean registerScrubber(CO2Scrubber scrubber) {
        if (!this.isLoaded || this.isRemoved()) return false;
        if (tank.getTankType() != Fluids.OXYGEN) return false;
        if (this.scrubber == scrubber) return true;
        if (this.scrubber != null && this.scrubber.isLoaded() && !this.scrubber.isRemoved() && this.scrubber.canOperate()) return false;
        this.scrubber = scrubber;
        return true;
    }
}
