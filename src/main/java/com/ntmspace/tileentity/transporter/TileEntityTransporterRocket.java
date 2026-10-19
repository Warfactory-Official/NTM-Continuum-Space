package com.ntmspace.tileentity.transporter;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.inventory.ContainerTransporterRocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.registry.TransporterBlocks;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityTransporterRocket} ("Rocket Transporter Pad"): 16 cargo slots (8 in, 8 out), 8 cargo
 * tanks of 128,000 mB (4 in, 4 out) and two 64,000 mB fuel tanks (hydrogen, oxygen; retypable by identifier). One
 * rocket shuttles between the two linked pads: the pad holding it sends once it has sat 20 ticks
 * ({@code launchTicks == -20}), the other pad's rocket has been gone 100 ticks, the cargo mass reaches the threshold and
 * both fuel tanks hold the trip's cost; the cost is {@code SolarSystem.getCostBetween} for 100 kg per item (and per
 * 1,000 mB) with the fuel's {@link FT_Rocket} thrust and specific impulse, at most 64,000 mB of each.
 */
public class TileEntityTransporterRocket extends TileEntityTransporterBase implements MenuProvider {

    public boolean hasRocket = true;
    public int launchTicks = 0;

    public int threshold = 0;

    private static final int MASS_MULT = 100;

    public TileEntityTransporterRocket(BlockPos pos, BlockState state) {
        super(TransporterBlocks.TRANSPORTER_ROCKET_BE.get(), pos, state, 16, 8, 128_000, 0, 2, 64_000);

        tanks[8].setTankType(Fluids.HYDROGEN);
        tanks[9].setTankType(Fluids.OXYGEN);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityTransporterRocket te) {
        te.updateServer();

        // If our transporter state sync is incorrect, fix whichever one gets updated first
        if (te.linkedTransporter instanceof TileEntityTransporterRocket other) {
            if (te.hasRocket == other.hasRocket) {
                te.hasRocket = !te.hasRocket;
            }
        }

        te.tickLaunch();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityTransporterRocket te) {
        te.tickLaunch();

        if (te.launchTicks > 0 && te.launchTicks < 100) {
            // 1.7 ParticleUtil.spawnGasFlame on the client: one flame below the rising rocket, falling
            CompoundTag flame = new CompoundTag();
            flame.putDouble("moX", 0.0);
            flame.putDouble("moY", -1.0);
            flame.putDouble("moZ", 0.0);
            HbmEffectNT.GasFlame.summonParticle(level, pos.getX() + 0.5, pos.getY() + 0.5 + te.launchTicks, pos.getZ() + 0.5, flame);

            if (te.launchTicks < 10) {
                // 1.7 ExplosionLarge.spawnShock on the client: smoke mode "shock", half a block up
                CompoundTag shock = new CompoundTag();
                shock.putInt("count", 1 + level.getRandom().nextInt(3));
                shock.putDouble("strength", 1 + level.getRandom().nextGaussian());
                HbmEffectNT.Smoke_Shock.summonParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, shock);
            }
        }
    }

    private void tickLaunch() {
        launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);
    }

    public int getThreshold() {
        return threshold == 0 ? 0 : (int) Math.pow(2, threshold - 1);
    }

    /** 1.7 {@code CelestialBody.getTarget(world, x, z)}: the station's body when in orbit, the level's body otherwise. */
    private record Target(SolarSystem.Body body, boolean inOrbit) {}

    private static Target targetOf(Level level, BlockPos pos) {
        if (Orbit.inOrbit(level) && level.getServer() != null) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(level.getServer(), pos.getX(), pos.getZ());
            return new Target(station.orbiting, true);
        }
        return new Target(Orbit.bodyOf(level), false);
    }

    /** 1.7 {@code getAtmosphericDrag(body.getTrait(CBT_Atmosphere.class))}, as the pressure the port's sum takes. */
    private static double pressureOf(SolarSystem.Body body) {
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(body.name, CBT_Atmosphere.class, false);
        return atmosphere == null ? 0 : atmosphere.getPressure();
    }

    /** The send cost, mB of each fuel tank, for {@code mass} (items and buckets) to the linked pad. */
    public int sendCost(TileEntityTransporterBase linkedTransporter, int mass, FT_Rocket fuelStats) {
        Target from = targetOf(level, worldPosition);
        Target to = targetOf(linkedTransporter.getLevel(), linkedTransporter.getBlockPos());
        return Math.min(64_000, SolarSystem.getCostBetween(from.body(), to.body(), mass * MASS_MULT, (int) fuelStats.getThrust(),
                fuelStats.getISP(), from.inOrbit(), to.inOrbit(), pressureOf(from.body()), pressureOf(to.body())));
    }

    private @Nullable FT_Rocket fuelStats() {
        FT_Rocket fuelStats = tanks[8].getTankType().getTrait(FT_Rocket.class);
        if (fuelStats == null) fuelStats = tanks[9].getTankType().getTrait(FT_Rocket.class);
        return fuelStats;
    }

    // Check that we have enough fuel to send to our destination
    @Override
    protected boolean canSend(TileEntityTransporterBase linkedTransporter) {
        if (launchTicks > -20) return false;
        if (!(linkedTransporter instanceof TileEntityTransporterRocket other) || other.launchTicks < 100) return false;
        if (!hasRocket) return false;

        int mass = itemCount();
        if (mass < getThreshold()) return false;

        FT_Rocket fuelStats = fuelStats();
        if (fuelStats == null) return false;

        int sendCost = sendCost(linkedTransporter, mass, fuelStats);

        return tanks[8].getFill() >= sendCost && tanks[9].getFill() >= sendCost;
    }

    @Override
    protected void hasSent(TileEntityTransporterBase linkedTransporter, int quantitySent) {
        // Recalculate send cost from what was actually successfully sent
        FT_Rocket fuelStats = fuelStats();
        // 1.7 dereferenced it unchecked (canSend has just found one)
        int sendCost = fuelStats == null ? 0 : sendCost(linkedTransporter, quantitySent, fuelStats);

        tanks[8].setFill(tanks[8].getFill() - sendCost);
        tanks[9].setFill(tanks[9].getFill() - sendCost);

        hasRocket = false;
        ((TileEntityTransporterRocket) linkedTransporter).hasRocket = true;
    }

    @Override
    protected void hasConnected(TileEntityTransporterBase linkedTransporter) {
        hasRocket = true;
        if (linkedTransporter instanceof TileEntityTransporterRocket other) other.hasRocket = false;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerTransporterRocket(containerId, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(hasRocket);
        buf.writeInt(threshold);
        super.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        hasRocket = buf.readBoolean();
        threshold = buf.readInt();
        super.deserialize(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        hasRocket = input.getBooleanOr("rocket", false);
        threshold = input.getIntOr("threshold", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("rocket", hasRocket);
        output.putInt("threshold", threshold);
    }

    private ForgeDirection dir() {
        return MachineCore.facing(getBlockState());
    }

    @Override
    protected DirPos[] getConPos() {
        return conPos(worldPosition, dir());
    }

    @Override
    protected DirPos[] getTankPos() {
        return tankPos(worldPosition, dir());
    }

    @Override
    protected DirPos[] getInsertPos() {
        ForgeDirection dir = dir();
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[] {
                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir),
        };
    }

    @Override
    protected DirPos[] getExtractPos() {
        ForgeDirection dir = dir();
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[] {
                new DirPos(x + dir.offsetX * 2, y, z + dir.offsetZ * 2, dir.getOpposite()),
        };
    }

    /** 1.7 {@code getConPos} for a core at {@code core} facing {@code dir}: the cargo duct points. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x + dir.offsetX * 2 - rot.offsetX, y, z + dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
                new DirPos(x + dir.offsetX * 2 + rot.offsetX, y, z + dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 - rot.offsetX, y, z - dir.offsetZ * 2 - rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir),

                new DirPos(x + dir.offsetX - rot.offsetX, y + 1, z + dir.offsetZ - rot.offsetZ, ForgeDirection.UP),
                new DirPos(x + dir.offsetX + rot.offsetX, y + 1, z + dir.offsetZ + rot.offsetZ, ForgeDirection.UP),
                new DirPos(x - dir.offsetX - rot.offsetX, y + 1, z - dir.offsetZ - rot.offsetZ, ForgeDirection.UP),
                new DirPos(x - dir.offsetX + rot.offsetX, y + 1, z - dir.offsetZ + rot.offsetZ, ForgeDirection.UP),
        };
    }

    /** 1.7 {@code getTankPos}: the two fuel duct points. */
    public static DirPos[] tankPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x + dir.offsetX - rot.offsetX * 3, y, z + dir.offsetZ - rot.offsetZ * 3, rot),
                new DirPos(x - dir.offsetX - rot.offsetX * 3, y, z - dir.offsetZ - rot.offsetZ * 3, rot),
        };
    }

    @Override
    public void receiveControl(CompoundTag nbt) {
        super.receiveControl(nbt);
        // 1.7 sent the slider as a double and read it with getInteger (which casts any number)
        if (Tags.contains(nbt, "threshold")) threshold = (int) Tags.getDoubleOr(nbt, "threshold", threshold);
    }
}
