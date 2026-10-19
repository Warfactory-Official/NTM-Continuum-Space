package com.ntmspace.tileentity.machine;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.TileEntityMachineBase;
import com.ntmspace.inventory.machine.ContainerStardar;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.rocket.ItemVOTVdrive;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityMachineStardar}: one drive slot (stack limit 1). It programs an empty {@code hard_drive}
 * into a navigation drive for the body (or a new or the current orbital station) the GUI picks, reads the landing area
 * around a navigation drive's approximate destination as a 256 x 256 height map for the GUI, and moves the landing
 * coordinates to the spot clicked on that map. The dish turns towards a random point every 15 to 30 seconds.
 *
 * <p>Height map: 1.7 {@code CelestialBody.getHeightmap} loaded or generated each of the 16 x 16 chunks synchronously,
 * all in one tick, in the body's world and read {@code Chunk.heightMap} (the first block above the topmost block that
 * blocks light). Here the chunks are claimed through Golem {@link Chunks#claimForLoading} (load or generate in the
 * background, without ticking them) and read as they arrive, so the server thread never waits on generation; after
 * {@link HeightmapScan#FALLBACK_TICKS} the rest is loaded synchronously, {@link HeightmapScan#FALLBACK_PER_TICK} per tick.
 * The cost is the generation of up to 256 chunks of the destination body (once; then the region files answer). The GUI
 * shows 1.7's "Loading surface map..." until the map is complete. Heights are the {@code MOTION_BLOCKING} height map
 * (+1 like 1.7's), the nearest modern map to 1.7's light-opacity one: it differs on blocks that block light but not
 * motion or the reverse (glass, fences).
 *
 * <p>The 1.7 OpenComputers component ({@code ntm_stardar}) is not ported: NTM: Space has no CC: Tweaked build wiring.
 */
public class TileEntityMachineStardar extends TileEntityMachineBase implements MenuProvider, IControlReceiver {

    // 1.7 oddity, kept: pointAtTime, targetYaw and targetPitch are STATIC, so every StarDar in the game (and, in
    // singleplayer, the client and server halves alike) shares one pointing schedule and one target, and the last
    // StarDar loaded overwrites the target with its saved one.
    private static long pointAtTime = 0;

    // Sent by the server for the client to smoothly lerp to
    public static float targetYaw = 0;
    public static float targetPitch = 0;

    // Used to point the dish on the client
    public float dishYaw = 0;
    public float dishPitch = 0;
    public float prevDishYaw = 0;
    public float prevDishPitch = 0;

    public boolean radarMode;

    private final float maxSpeedYaw = 0.5F;

    public int @Nullable [] heightmap;
    public boolean updateHeightmap = false;
    /** 1.7 {@code previousStack} compared with {@code isItemEqual} (item and damage = the drive's body = the item here). */
    private @Nullable Item previousItem;
    private @Nullable HeightmapScan scan;

    /** Whether the slot holds anything (synced: the remote console draws the drive from it). */
    private boolean clientHasDrive;

    public TileEntityMachineStardar(BlockPos pos, BlockState state) {
        super(SpaceMachinesD.STARDAR_BE.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.machineStardar";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineStardar te) {
        RandomSource rand = level.getRandom();
        if (level.getGameTime() >= pointAtTime) {
            pointAtTime = level.getGameTime() + rand.nextInt(300) + 300;

            targetYaw = Mth.wrapDegrees(rand.nextFloat() * 360);
            targetPitch = rand.nextFloat() * 80;
        }

        ItemStack slot = te.getItem(0);
        if (slot.getItem() instanceof ItemVOTVdrive) {
            if ((te.heightmap == null && te.scan == null) || slot.getItem() != te.previousItem) {
                te.previousItem = slot.getItem();

                ItemVOTVdrive.Destination destination = ItemVOTVdrive.getApproximateDestination(slot, rand);
                te.cancelScan();

                if (destination.body != DriveBody.ORBIT && level.getServer() != null) {
                    ServerLevel body = level.getServer().getLevel(destination.body.level());
                    if (body != null) {
                        // 1.7 filled a fresh array at once; the GUI says "loading" until the scan is done
                        te.heightmap = null;
                        te.updateHeightmap = true;
                        te.scan = new HeightmapScan(body, destination.x >> 4, destination.z >> 4);
                    }
                }
            }

            if (te.scan != null && te.scan.tick()) {
                com.ntmspace.NtmSpace.LOG.info("StarDar height map ready: 256 chunks of {} around chunk {}, {} after {} ticks",
                        dev.golem.core.Ids.keyId(te.scan.level.dimension()), te.scan.baseX + 8, te.scan.baseZ + 8, te.scan.ticks);
                te.heightmap = te.scan.map;
                te.updateHeightmap = true;
                te.scan = null;
            }
        } else {
            te.cancelScan();
            te.previousItem = null;
            if (te.heightmap != null) {
                te.heightmap = null;
                te.updateHeightmap = true;
            }
        }

        te.networkPackNT();
        te.updateHeightmap = false;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineStardar te) {
        float yawOffset = Mth.wrapDegrees(targetYaw - te.dishYaw);
        float moveYaw = clamp17(yawOffset, -te.maxSpeedYaw, te.maxSpeedYaw);

        float pitchOffset = targetPitch - te.dishPitch;
        // 1.7: 0 / 0 when the yaw is on target, and 1.7's clamp then hands the full offset back (see clamp17)
        float pitchSpeed = (moveYaw / yawOffset) * Math.abs(pitchOffset);
        float movePitch = clamp17(pitchOffset, -pitchSpeed, pitchSpeed);

        te.prevDishYaw = te.dishYaw;
        te.prevDishPitch = te.dishPitch;
        te.dishYaw += moveYaw;
        te.dishPitch += movePitch;
    }

    /**
     * 1.7 {@code MathHelper.clamp_float}: {@code x < min ? min : x > max ? max : x}. With NaN bounds both tests fail and
     * {@code x} comes back, which the dish relies on; {@code Mth.clamp} would answer NaN.
     */
    static float clamp17(float x, float min, float max) {
        return x < min ? min : (x > max ? max : x);
    }

    private void cancelScan() {
        if (scan != null) {
            scan.cancel();
            scan = null;
        }
    }

    @Override
    public void setRemoved() {
        cancelScan();
        super.setRemoved();
    }

    public boolean hasDrive() {
        return level != null && level.isClientSide() ? clientHasDrive : !getItem(0).isEmpty();
    }

    // -------------------------------------------------------------------------------------------- control

    /**
     * 1.7 {@code processDrive}: an empty drive becomes the navigation drive for {@code target} (a body name, or
     * {@code orbit}). With a position ({@code ix}/{@code iz} not both 0, outside the orbit) it is processed with that
     * approximate destination (the GUI sends the StarDar's own position for the body it stands on); an orbit drive is
     * processed for the current station (on a station) or a free grid square.
     */
    public void processDrive(String target, int ix, int iz) {
        if (level == null) return;
        DriveBody body = DriveBody.byName(target);
        if (body == null) return;

        if (!getItem(0).is(RocketItems.HARD_DRIVE.get())) return;

        ItemStack drive = new ItemStack(RocketItems.drive(body));

        if ((ix != 0 || iz != 0) && !Orbit.inOrbit(level)) {
            RocketItems.DRIVE.set(drive, DriveData.EMPTY.withApproximate(ix, iz).withProcessed(true));
        } else if (body == DriveBody.ORBIT) {
            int[] pos;

            // if we're on a station, return our current station as a drive
            if (Orbit.inOrbit(level)) {
                pos = new int[] {OrbitalStation.gridOf(worldPosition.getX()), OrbitalStation.gridOf(worldPosition.getZ())};
            } else {
                pos = StationData.get(level.getServer()).findFreeSpace();
            }

            RocketItems.DRIVE.set(drive, DriveData.EMPTY.withCoordinates(pos[0], pos[1]).withProcessed(true));
        }

        setItem(0, drive);

        // Now point the dish at the target planet. 1.7 oddity, kept: pointAtTime is set to a duration, not a world
        // time, so the next tick (world time >= 300..599) picks a new random target straight away.
        RandomSource rand = level.getRandom();
        pointAtTime = rand.nextInt(300) + 300;
        targetYaw = Mth.wrapDegrees(rand.nextFloat() * 360);
        targetPitch = rand.nextFloat() * 80;

        setChanged();
    }

    /**
     * 1.7 {@code updateDriveCoords}: the landing spot clicked on the height map ({@code x}, {@code z} its pixel, 0..255)
     * becomes the drive's coordinates. 1.7 oddity, kept: it is taken relative to the approximate destination minus 128,
     * while the map starts at the destination's chunk minus 8 chunks, so the spot is off by the destination's position
     * inside its chunk ({@code ax & 15}, {@code az & 15}).
     */
    public void updateDriveCoords(int x, int z) {
        if (level == null) return;
        ItemStack slot = getItem(0);
        if (!(slot.getItem() instanceof ItemVOTVdrive)) return;

        ItemVOTVdrive.Destination destination = ItemVOTVdrive.getApproximateDestination(slot, level.getRandom());
        ItemVOTVdrive.setCoordinates(slot, destination.x + x - 8 * 16, destination.z + z - 8 * 16);

        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "pid")) {
            processDrive(Tags.getStringOr(data, "pid", ""), Tags.getIntOr(data, "ix", 0), Tags.getIntOr(data, "iz", 0));
        }

        if (Tags.contains(data, "px") && Tags.contains(data, "pz")) {
            updateDriveCoords(Tags.getIntOr(data, "px", 0), Tags.getIntOr(data, "pz", 0));
        }
        if (Tags.contains(data, "radarmode")) {
            radarMode = Tags.getBooleanOr(data, "radarmode", false);
        }
    }

    /**
     * 1.7 answered {@code isUseableByPlayer}, which the StarDar made always true; HBM's control contract asks a
     * menu-driven receiver to check that the sender's open menu is this StarDar's (the only way the 1.7 GUI sent).
     */
    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerStardar menu && menu.getTile() == this;
    }

    // -------------------------------------------------------------------------------------------- inventory and sync

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** 1.7 {@code isUseableByPlayer}: always, so the remote console can keep the GUI open from anywhere. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(targetYaw);
        buf.writeFloat(targetPitch);

        buf.writeBoolean(radarMode);
        buf.writeBoolean(!getItem(0).isEmpty());
        buf.writeBoolean(updateHeightmap);
        if (updateHeightmap) {
            if (heightmap != null) {
                buf.writeInt(heightmap.length);
                for (int h : heightmap) buf.writeByte(h);
            } else {
                buf.writeInt(0);
            }
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        targetYaw = buf.readFloat();
        targetPitch = buf.readFloat();

        radarMode = buf.readBoolean();
        clientHasDrive = buf.readBoolean();
        if (buf.readBoolean()) {
            updateHeightmap = true;
            int count = buf.readInt();
            if (count > 0) {
                int[] map = new int[count];
                for (int i = 0; i < count; i++) map[i] = buf.readUnsignedByte();
                heightmap = map;
            } else {
                heightmap = null;
            }
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("radarmode", radarMode);
        output.putFloat("yaw", targetYaw);
        output.putFloat("pitch", targetPitch);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        radarMode = input.getBooleanOr("radarmode", false);
        targetYaw = input.getFloatOr("yaw", 0F);
        targetPitch = input.getFloatOr("pitch", 0F);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (level != null && !level.isClientSide()) updateHeightmap = true; // new viewer, send them the heightmap just in case
        return new ContainerStardar(containerId, playerInventory, this);
    }

    // -------------------------------------------------------------------------------------------- height map

    /**
     * The 16 x 16 chunks around a destination chunk ({@code -8..7} on each axis), read into a 256 x 256 height map
     * ({@code map[z * 256 + x]}, 1.7's layout) as they load.
     */
    public static final class HeightmapScan {

        /** Ticks to wait for the background loads before loading the rest on the server thread. */
        public static final int FALLBACK_TICKS = 600;
        /** Chunks loaded synchronously per tick after {@link #FALLBACK_TICKS}. */
        public static final int FALLBACK_PER_TICK = 2;

        final ServerLevel level;
        final int baseX;
        final int baseZ;
        final boolean[] done = new boolean[256];
        public final int[] map = new int[256 * 256];
        int remaining = 256;
        int ticks;

        public HeightmapScan(ServerLevel level, int chunkX, int chunkZ) {
            this.level = level;
            this.baseX = chunkX - 8;
            this.baseZ = chunkZ - 8;
            for (int i = 0; i < 256; i++) Chunks.claimForLoading(level, baseX + (i & 15), baseZ + (i >> 4));
        }

        /** Reads every chunk that has arrived; true once the map is complete. */
        public boolean tick() {
            ticks++;
            int budget = ticks > FALLBACK_TICKS ? FALLBACK_PER_TICK : 0;
            for (int i = 0; i < 256 && remaining > 0; i++) {
                if (done[i]) continue;
                int cx = baseX + (i & 15), cz = baseZ + (i >> 4);
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null && budget > 0) {
                    budget--;
                    chunk = Chunks.loadNow(level, cx, cz);
                }
                if (chunk == null) continue;
                read(chunk, (i & 15) * 16, (i >> 4) * 16);
                done[i] = true;
                remaining--;
                Chunks.releaseForLoading(level, cx, cz);
            }
            return remaining == 0;
        }

        private void read(LevelChunk chunk, int ox, int oz) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    map[(z + oz) * 256 + (x + ox)] = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
                }
            }
        }

        public void cancel() {
            for (int i = 0; i < 256; i++) {
                if (!done[i]) Chunks.releaseForLoading(level, baseX + (i & 15), baseZ + (i >> 4));
            }
            remaining = 0;
        }
    }
}
