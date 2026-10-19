package com.ntmspace.orbit;

import com.ntmspace.registry.StationBlocks;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code dim.orbit.OrbitalStation}: one station of the orbit level. Stations are spread over the level in
 * {@link #STATION_SIZE} squares; leaving one's square (past {@link #BUFFER_SIZE}) drops the player back to the orbited
 * body, and the square a player stands in decides the body drawn below and the station's gravity.
 *
 * <p>The station's persistent part (grid square, name, bodies, travel state, gravity switch) is {@link StationData},
 * saved by name. Its live part, as in 1.7, is rebuilt by the block entities every tick: the docking ports register
 * themselves ({@link #addPort}), the engines register as {@link Propulsion}. The client knows one station,
 * {@link #clientStation}, sent by the station core's block entity.
 *
 * <p>Travel: {@link #travelTo} checks every engine can make the Hohmann burn for a fixed 200 t ship, then runs LEAVING
 * (the slowest engine's spool-up) -> TRANSFER ({@link #calculateTransferTime}) -> ARRIVING (the slowest cool-down) ->
 * ORBIT, switching the orbited body at the end of TRANSFER.
 */
public final class OrbitalStation {

    /** The side of each station's square of the orbit level. */
    public static final int STATION_SIZE = 1024;
    /** The edge of a square that drops you out of orbit (so other stations stay out of sight). */
    public static final int BUFFER_SIZE = 256;
    /** The band inside the buffer that warns about falling out of orbit. */
    public static final int WARNING_SIZE = 32;
    /** 1.7: always static, "to not punish building big cool stations". */
    public static final int SHIP_MASS = 200_000;

    public enum StationState {
        ORBIT, // big chillin
        LEAVING, // prepare engines for transfer
        TRANSFER, // going from A to B
        ARRIVING, // spool down engines
    }

    public String name = ""; // I dub thee

    public SolarSystem.Body orbiting;
    public SolarSystem.Body target;

    private double eclipseAmount;

    public StationState state = StationState.ORBIT;
    public int stateTimer;
    public int maxStateTimer = 100;

    public boolean hasStation = false;

    /** The station's grid square. */
    public int dX;
    public int dZ;

    public boolean hasEngines = true;
    public List<BlockPos> errorsAt = new ArrayList<>();
    public int errorTimer;

    public float gravityMultiplier = 1;

    private @Nullable TileEntityOrbitalStation mainPort;
    // 1.7 HashMap / HashSet: iteration order is insertion order here, 1.7's followed the hash of the coordinates
    private final Map<BlockPos, TileEntityOrbitalStation> ports = new LinkedHashMap<>();
    private int portIndex = 0;
    private final Set<Propulsion> engines = new LinkedHashSet<>();

    /** The station the client is on (1.7 starts it orbiting body 0, Kerbin, until the core syncs). */
    public static OrbitalStation clientStation = new OrbitalStation(SolarSystem.body("kerbin"));

    /** For the client. */
    public OrbitalStation(SolarSystem.Body orbiting) {
        this.orbiting = orbiting;
        this.target = orbiting;
    }

    /** For the server: the station of grid square {@code x, z}. */
    public OrbitalStation(SolarSystem.Body orbiting, int x, int z) {
        this(orbiting);
        this.dX = x;
        this.dZ = z;
    }

    // ---------------------------------------------------------------------------------------- travel

    public void travelTo(Level level, SolarSystem.Body target) {
        if (state != StationState.ORBIT) return; // only when at rest can we start a new journey
        if (!canTravel(orbiting, target)) return;

        setState(StationState.LEAVING, getLeaveTime());
        this.target = target;
    }

    /** Ticked by the station core's block entity, on both sides. */
    public void update(Level level) {
        if (!level.isClientSide()) {
            eclipseAmount = -1;

            if (state == StationState.LEAVING) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.TRANSFER, getTransferTime(level));
                }
            } else if (state == StationState.TRANSFER) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.ARRIVING, getArriveTime());
                    orbiting = target;
                }
            } else if (state == StationState.ARRIVING) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.ORBIT, 0);
                }
            }

            if (level.getServer() != null) StationData.get(level.getServer()).setDirty();

            hasEngines = engines.size() > 0;

            errorTimer--;
            if (errorTimer <= 0) {
                errorsAt = new ArrayList<>();
                errorTimer = 0;
            }
        }

        stateTimer++;
    }

    private boolean canTravel(SolarSystem.Body from, SolarSystem.Body to) {
        if (engines.size() == 0) return false;

        double deltaV = SolarSystem.getDeltaVBetween(from, to);
        int shipMass = SHIP_MASS;
        float totalThrust = getTotalThrust();

        boolean canTravel = true;
        errorsAt = new ArrayList<>();

        for (Propulsion engine : engines) {
            float massPortion = engine.getThrust() / totalThrust;
            if (!engine.canPerformBurn(Math.round(shipMass * massPortion), deltaV)) {
                BlockEntity te = engine.getBlockEntity();
                canTravel = false;
                errorsAt.add(te.getBlockPos());
                errorTimer = 100;
            }
        }

        return canTravel;
    }

    private float getTotalThrust() {
        float thrust = 0;
        for (Propulsion engine : engines) thrust += engine.getThrust();
        return thrust;
    }

    // Has the side effect of beginning engine burns
    private int getLeaveTime() {
        int leaveTime = 20;
        for (Propulsion engine : engines) {
            int time = engine.startBurn();
            if (time > leaveTime) leaveTime = time;
        }
        return leaveTime;
    }

    // And this one will end engine burns
    private int getArriveTime() {
        int arriveTime = 20;
        for (Propulsion engine : engines) {
            int time = engine.endBurn();
            if (time > arriveTime) arriveTime = time;
        }
        return arriveTime;
    }

    private int getTransferTime(Level level) {
        if (mainPort == null || mainPort.getLevel() == null) return -1;

        int size = calculateSize();
        double distance = SolarSystem.calculateDistanceBetweenTwoBodies(mainPort.getLevel().getGameTime(), orbiting, target);
        float thrust = getTotalThrust();

        return calculateTransferTime(distance, size, thrust);
    }

    /** 1.7 {@code calculateTransferTime}: ticks of TRANSFER for {@code distance} km, a top-down area and the thrust. */
    public static int calculateTransferTime(double distance, int size, float thrust) {
        return (int) (Math.log(1 + (distance * size / thrust * 100)) * 150);
    }

    public void setState(StationState state, int timeUntilNext) {
        this.state = state;
        stateTimer = 0;
        maxStateTimer = timeUntilNext;
    }

    /**
     * 1.7 {@code recallPod}: sends a docked, reusable, riderless craft that is ready ({@link StationDock#canRecall}) down
     * to {@code x, z} on the surface of {@code body}, which must be the body this station orbits.
     */
    public boolean recallPod(SolarSystem.Body body, int x, int z) {
        if (!hasStation) return false;
        if (body != orbiting) return false;

        for (TileEntityOrbitalStation port : ports.values()) {
            StationDock rocket = port.getDocked();

            if (rocket == null || !rocket.isReusable()) continue;
            // ensure the rocket has fuel before sending it off
            if (!rocket.canRecall()) continue;
            // and make sure it doesn't have a rider!!
            if (rocket.hasRider()) continue;

            rocket.recall(Orbit.levelOf(body), x, z);
            return true;
        }

        return false;
    }

    // ---------------------------------------------------------------------------------------- engines and ports

    public static void addPropulsion(Propulsion propulsion) {
        BlockEntity te = propulsion.getBlockEntity();
        if (te.getLevel() == null || te.getLevel().getServer() == null) return;
        OrbitalStation station = getStationFromPosition(te.getLevel().getServer(), te.getBlockPos().getX(), te.getBlockPos().getZ());
        station.engines.add(propulsion);
    }

    public static void removePropulsion(Propulsion propulsion) {
        BlockEntity te = propulsion.getBlockEntity();
        if (te.getLevel() == null || te.getLevel().getServer() == null) return;
        OrbitalStation station = getStationFromPosition(te.getLevel().getServer(), te.getBlockPos().getX(), te.getBlockPos().getZ());
        station.engines.remove(propulsion);
    }

    /** The engines registered now (GameTests, the command). */
    public Set<Propulsion> engines() {
        return java.util.Collections.unmodifiableSet(engines);
    }

    public void addPort(TileEntityOrbitalStation port) {
        ports.put(port.getBlockPos(), port);
        if (port.isCore()) mainPort = port;
    }

    public void removePort(TileEntityOrbitalStation port) {
        ports.remove(port.getBlockPos());
    }

    /** 1.7 {@code getPort()}: a free, unreserved port, else the next one round robin; null without ports. */
    public @Nullable TileEntityOrbitalStation getPort() {
        if (ports.size() == 0) return null;

        // First, find any port that's available
        int index = 0;
        for (TileEntityOrbitalStation port : ports.values()) {
            if (!port.hasDocked && !port.isReserved) {
                portIndex = index;
                return port;
            }
            index++;
        }

        // Failing that, round robin on the occupied ports
        portIndex++;
        if (portIndex >= ports.size()) portIndex = 0;

        return ports.values().toArray(new TileEntityOrbitalStation[0])[portIndex];
    }

    /** 1.7 {@code getPort(x, z)}: a port of the station at block {@code x, z} of the orbit level. */
    public static @Nullable TileEntityOrbitalStation getPort(Level level, int x, int z) {
        if (level.getServer() == null) return null;
        return getStationFromPosition(level.getServer(), x, z).getPort();
    }

    // I can't stop pronouncing this as hors d'oeuvre
    private static final Direction[] HOR_DIR = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * 1.7 {@code calculateSize}: the top-down area of the bounding rectangle of everything connected to the core in
     * columns that hold a block above y 1 (or within the 9 x 9 minimum around the core). 1.7 read
     * {@code getHeightValue}, the light-blocking height map; the motion-blocking height map stands in for it.
     */
    public int calculateSize() {
        if (mainPort == null || mainPort.getLevel() == null) return 0;
        Level level = mainPort.getLevel();
        BlockPos core = mainPort.getBlockPos();

        int minX, maxX;
        int minZ, maxZ;
        minX = maxX = core.getX();
        minZ = maxZ = core.getZ();

        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        stack.push(core);

        Set<BlockPos> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            BlockPos pos = stack.pop();
            visited.add(pos);

            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();

            for (Direction dir : HOR_DIR) {
                BlockPos nextPos = pos.relative(dir);
                if (!visited.contains(nextPos) && isInStation(level, core, nextPos)) {
                    stack.push(nextPos);
                }
            }
        }

        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    private static boolean isInStation(Level level, BlockPos core, BlockPos pos) {
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > 1) return true;
        return Math.abs(pos.getX() - core.getX()) < 5 && Math.abs(pos.getZ() - core.getZ()) < 5; // minimum station size
    }

    // ---------------------------------------------------------------------------------------- progress and the sky

    public double getUnscaledProgress(float partialTicks) {
        if (state == StationState.ORBIT) return 0;
        return Mth.clamp(((double) stateTimer + partialTicks) / (double) maxStateTimer, 0, 1);
    }

    public double getTransferProgress(float partialTicks) {
        if (state != StationState.TRANSFER) return 0;
        return easeInOutCirc(getUnscaledProgress(partialTicks));
    }

    /** 1.7 {@code getEclipseAmount}: how far another body hides the sun from the station, cached per server tick. */
    public double getEclipseAmount(long worldTime) {
        if (eclipseAmount > -1) return eclipseAmount;

        double sunSize = SolarSystem.calculateSunSize(orbiting);
        double progress = getTransferProgress(0);
        List<SolarSystem.AstroMetric> metrics;

        // Get our orrery of bodies, this is cached for reuse in sky rendering
        if (state == StationState.ORBIT) {
            double altitude = Orbit.getOrbitalAltitude(orbiting);
            metrics = SolarSystem.calculateMetricsFromSatellite(worldTime, orbiting, altitude);
        } else {
            double fromAlt = Orbit.getOrbitalAltitude(orbiting);
            double toAlt = Orbit.getOrbitalAltitude(target);
            metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(worldTime, orbiting, target, fromAlt, toAlt, progress);

            double sunTargetSize = SolarSystem.calculateSunSize(target);
            sunSize = lerp(progress, sunSize, sunTargetSize);
        }

        eclipseAmount = SpaceSkies.eclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
        return eclipseAmount;
    }

    /** {@code WorldProviderOrbit.getSunPower(x, z)}: the station's sun, lerped during a transfer, less any eclipse. */
    public float getSunPower(long worldTime) {
        double progress = getTransferProgress(0);
        float sunPower = orbiting.getSunPower();
        if (progress > 0) return (float) lerp(progress, sunPower, target.getSunPower());
        double eclipse = getEclipseAmount(worldTime);
        return sunPower * (1 - (float) eclipse);
    }

    private static double easeInOutCirc(double t) {
        return t < 0.5
                ? (1 - Math.sqrt(1 - Math.pow(2 * t, 3))) / 2
                : (Math.sqrt(1 - Math.pow(-2 * t + 2, 3)) + 1) / 2;
    }

    /** {@code BobMathUtil.lerp}. */
    static double lerp(double amount, double a, double b) {
        return a + (b - a) * amount;
    }

    // ---------------------------------------------------------------------------------------- lookup

    /** The grid square of block {@code x} (1.7 {@code MathHelper.floor_float((float) x / STATION_SIZE)}). */
    public static int gridOf(int block) {
        return Mth.floor((float) block / STATION_SIZE);
    }

    /**
     * 1.7 {@code getStationFromPosition}: the station of the square block {@code x, z} lies in, created (orbiting Kerbin,
     * not launched) where none exists, as 1.7's debug fallback did.
     */
    public static OrbitalStation getStationFromPosition(MinecraftServer server, int x, int z) {
        StationData data = StationData.get(server);
        OrbitalStation station = data.getStationFromPosition(x, z);

        // Fallback for when a station doesn't exist (should only occur when using debug wand!)
        if (station == null) {
            station = data.addStation(gridOf(x), gridOf(z), SolarSystem.body("kerbin"));
        }

        return station;
    }

    /** 1.7 {@code getStation(x, z)}: by grid square. */
    public static OrbitalStation getStation(MinecraftServer server, int x, int z) {
        return getStationFromPosition(server, x * STATION_SIZE, z * STATION_SIZE);
    }

    /**
     * 1.7 {@code spawn}: places the station core (5 x 5 docking port, unbreakable) facing north at y 127 of block
     * {@code x, z}, unless it is there already.
     */
    public static void spawn(ServerLevel level, int x, int z) {
        int y = 127;
        BlockPos core = new BlockPos(x, y, z);
        if (level.getBlockState(core).is(StationBlocks.ORBITAL_STATION.get())) return;
        com.hbm.blocks.multiblock.MultiblockHandlerXR.placeAssembled(level, core, StationBlocks.ORBITAL_STATION.get(), Direction.NORTH);
    }

    /** 1.7 {@code addStation(x, z, body)}: marks grid square {@code x, z} as a launched station orbiting {@code body}. */
    public static void addStation(MinecraftServer server, int x, int z, SolarSystem.Body body) {
        StationData data = StationData.get(server);
        OrbitalStation station = data.getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);

        if (station == null) station = data.addStation(x, z, body);

        station.orbiting = station.target = body;
        station.hasStation = true;
        data.setDirty();
    }

    /** 1.7's station id: {@code "0x" + hex(ChunkCoordIntPair(dX, dZ).hashCode())}, upper case. */
    public String identifier() {
        int hash = 1664525 * dX + 1013904223 ^ 1664525 * (dZ ^ -559038737) + 1013904223;
        return "0x" + Integer.toHexString(hash).toUpperCase(java.util.Locale.ROOT);
    }

    // ---------------------------------------------------------------------------------------- sync

    /**
     * 1.7 {@code serialize}: the station state the core sends to the client. Bodies go by name and the state by its
     * name (1.7 wrote the dimension id and the ordinal; runtime packets may carry numbers, saves never do).
     */
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(orbiting.name);
        buf.writeUtf(target.name);
        buf.writeUtf(state.name());
        buf.writeInt(stateTimer);
        buf.writeInt(maxStateTimer);
        buf.writeBoolean(hasEngines);
        buf.writeFloat(gravityMultiplier);
        buf.writeUtf(name);
        buf.writeInt(errorsAt.size());
        for (BlockPos error : errorsAt) buf.writeBlockPos(error);
    }

    public static OrbitalStation deserialize(RegistryFriendlyByteBuf buf) {
        OrbitalStation station = new OrbitalStation(bodyOrKerbin(buf.readUtf()));
        station.target = bodyOrKerbin(buf.readUtf());
        station.state = stateOf(buf.readUtf());
        station.stateTimer = buf.readInt();
        station.maxStateTimer = buf.readInt();
        station.hasEngines = buf.readBoolean();
        station.gravityMultiplier = buf.readFloat();
        station.name = buf.readUtf();
        station.errorsAt = new ArrayList<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) station.errorsAt.add(buf.readBlockPos());
        return station;
    }

    /** 1.7 {@code CelestialBody.getBody(name)}: an unknown name is Kerbin. */
    static SolarSystem.Body bodyOrKerbin(String name) {
        SolarSystem.Body body = name == null ? null : SolarSystem.body(name);
        return body != null ? body : SolarSystem.body("kerbin");
    }

    static StationState stateOf(String name) {
        for (StationState state : StationState.values()) {
            if (state.name().equals(name)) return state;
        }
        return StationState.ORBIT;
    }
}
