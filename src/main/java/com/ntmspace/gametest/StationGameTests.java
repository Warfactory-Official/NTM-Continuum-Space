package com.ntmspace.gametest;

import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.CapRole;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineTeleporter;
import com.hbm.tileentity.machine.fusion.FusionLink;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitChunkGenerator;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.tileentity.station.TileEntityMachineHTR3;
import com.ntmspace.tileentity.station.TileEntityMachineHTRNeo;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.tileentity.station.TileEntityStationPropulsionCreative;
import com.ntmspace.tileentity.station.TileEntityXenonThruster;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The orbit and station GameTests. Expected numbers come from hbm-space's own code run outside the game (its
 * {@code SolarSystem} delta-v and fuel methods, {@code WorldProviderOrbit.getOrbitalAltitude},
 * {@code OrbitalStation.calculateTransferTime}, copied verbatim into an oracle with the 1.7 body constants; scratchpad
 * {@code orbit/oracle/Oracle17.java}). A GameTest world has no datapack dimensions, so the orbit level itself is the
 * harness's ({@code StationOrbit}).
 */
public final class StationGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** Stations save by name: bodies, state; a round trip keeps every field; old saves without "gravity" read 1. */
    public static final GameTests.Function SAVED_DATA = TESTS.function("station_saved_data_round_trip", 20,
            () -> StationGameTests::savedData);

    /** Delta-v, fuel, orbital altitude, transfer time and the port's fill requirement against the 1.7 oracle. */
    public static final GameTests.Function DELTA_V = TESTS.function("station_delta_v_matches_17", 20,
            () -> StationGameTests::deltaV);

    /** Each thruster's burn check, fuel and power use, thrust and timings against 1.7's values. */
    public static final GameTests.Function THRUSTERS = TESTS.function("station_thruster_burns", 40,
            () -> StationGameTests::thrusters);

    /** A station with one engine travels Kerbin -> Duna through LEAVING, TRANSFER, ARRIVING; without engines it stays. */
    public static final GameTests.Function TRAVEL = TESTS.function("station_travel_states", 100,
            () -> StationGameTests::travel);

    /** The orbit generator is void: base height 0, every column air, 256 high from y 0. */
    public static final GameTests.Function VOID = TESTS.function("orbit_void_generator", 20,
            () -> StationGameTests::voidGenerator);

    /** The station core's 5 x 5 port with its twelve rim cells, its block entity and hydrazine tank. */
    public static final GameTests.Function CORE = TESTS.function("station_core_structure", 40, 8,
            () -> StationGameTests::coreStructure);

    /** 1.7's layout: the NEO's plasma end touches the torus's edge and the torus counts it as that arm's receiver. */
    public static final GameTests.Function NEO_TORUS = TESTS.function("station_neo_fed_by_touching_torus", 40, 32,
            () -> StationGameTests::neoFedByTouchingTorus);

    /** The xenon thruster's ports are 1.7's {@code getPorts}: the core row, taking power and xenon at the back. */
    public static final GameTests.Function XENON_PORTS = TESTS.function("station_xenon_thruster_ports", 20, 12,
            () -> StationGameTests::xenonPorts);

    /** The LPW-3N's port is 1.7's {@code getPorts}: the propellant cell at the end of its long axis. */
    public static final GameTests.Function HTR3_PORTS = TESTS.function("station_htr3_ports", 20, 12,
            () -> StationGameTests::htr3Ports);

    /** The docking port's rim is 1.7's twelve ports and takes hydrazine with nothing docked. */
    public static final GameTests.Function RIM = TESTS.function("station_rim_takes_fuel_undocked", 40, 8,
            () -> StationGameTests::rimTakesFuelUndocked);

    /** Space's teleporter takes N-MASS at all six faces (1.7 {@code setupFluidPorts} and {@code updatePortFIFO}). */
    public static final GameTests.Function TELEPORTER = TESTS.function("teleporter_takes_nmass_from_ducts", 40,
            () -> StationGameTests::teleporterTakesNmass);

    /** A bread item sent through Space's teleporter leaves a slime at the exit; another item leaves none. */
    public static final GameTests.Function TELEPORTER_BREAD = TESTS.function("teleporter_bread_leaves_slime", 20, 8,
            () -> StationGameTests::teleporterBread);

    private StationGameTests() {}

    public static void init() {}

    /** A 1.7 port: one cell at an offset from the core, open on one face. */
    record Port(int dx, int dy, int dz, ForgeDirection out) {}

    static void assertPorts(GameTestHelper helper, String what, BlockPos core, PortDef[] ports, Port... expected) {
        helper.assertTrue(ports.length == expected.length, what + ": " + ports.length + " ports, 1.7 has " + expected.length);
        for (int i = 0; i < expected.length; i++) {
            Port port = expected[i];
            BlockPos cell = core.offset(port.dx, port.dy, port.dz);
            PortDef def = ports[i];
            helper.assertTrue(def.portPositions.length == 1 && def.portPositions[0].equals(cell),
                    what + " port " + i + " is not the cell " + port);
            helper.assertTrue(def.portConnections.length == 1 && def.portConnections[0].getDir() == port.out
                    && def.portConnections[0].getPos().equals(cell.relative(port.out.toDirection())),
                    what + " port " + i + " is not open " + port.out);
        }
    }

    /** Each port is a cell of the placed structure (or its core), open on its face for {@code roles} and no other. */
    static void assertOpen(GameTestHelper helper, String what, BlockPos core, PortDef[] ports, int roles) {
        ServerLevel level = helper.getLevel();
        BlockState state = level.getBlockState(core);
        BlockMultiblockCore block = (BlockMultiblockCore) state.getBlock();
        Direction facing = state.getValue(BlockMultiblockCore.FACING);
        for (DirPos con : PortDef.connections(ports)) {
            Direction out = con.getDir().toDirection();
            BlockPos cell = con.getPos().relative(out.getOpposite());
            boolean atCore = cell.equals(core);
            helper.assertTrue(atCore || core.equals(MultiblockSurface.coreOfCell(level, cell)),
                    what + ": the port cell " + cell + " is not part of the structure");
            for (CapRole role : CapRole.VALUES) {
                boolean open = block.isOpenAt(facing, cell.getX() - core.getX(), cell.getY() - core.getY(),
                        cell.getZ() - core.getZ(), out, BlockMultiblockCore.PASSIVE_NONE, role);
                // the core answers for every role
                helper.assertTrue(open == (atCore || (roles & role.bit) != 0),
                        what + ": " + cell + " open " + out + " for " + role + " is " + open);
            }
        }
    }

    /** Places {@code block} at the padded centre and answers its core's block entity. */
    static <T extends BlockEntity> T placed(GameTestHelper helper, BlockMultiblockCore block, Direction facing, Class<T> type) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(12, 5));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, block, facing);
        return GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), type);
    }

    /** Whether a cable beyond {@code port} reaches {@code te} as a power receiver. */
    static boolean powered(ServerLevel level, Object te, BlockPos core, Port port) {
        BlockPos outside = core.offset(port.dx, port.dy, port.dz).relative(port.out.toDirection());
        return PortNodes.isPorted(level, te, true)
                && PortNodes.allows(level, outside.asLong(), port.out, te, PowerNetProvider.THE_PROVIDER, true);
    }

    /** Whether a duct beyond {@code port} reaches {@code te} as a receiver of {@code tank}'s fluid. */
    static boolean takes(ServerLevel level, Object te, BlockPos core, Port port, FluidTankNTM tank) {
        if (!PortNodes.isPorted(level, te, false)) return false;
        BlockPos outside = core.offset(port.dx, port.dy, port.dz).relative(port.out.toDirection());
        return fluidPort(level, te, new DirPos(outside, port.out), tank, true);
    }

    /** Whether {@code te} has a port for {@code tank}'s fluid aimed at {@code con}, receiving or providing. */
    private static boolean fluidPort(ServerLevel level, Object te, DirPos con, FluidTankNTM tank, boolean receiving) {
        for (FluidType type : tank.connectionTypes()) {
            if (PortNodes.allows(level, con.getPos().asLong(), con.getDir(), te, FluidNetProvider.of(type), receiving)) return true;
        }
        return false;
    }

    private static void savedData(GameTestHelper helper) {
        StationData data = new StationData();
        OrbitalStation a = data.addStation(3, -7, SolarSystem.body("duna"));
        a.name = "Zvezda";
        a.hasStation = true;
        a.target = SolarSystem.body("ike");
        a.state = OrbitalStation.StationState.TRANSFER;
        a.stateTimer = 37;
        a.maxStateTimer = 1330;
        a.gravityMultiplier = 0;
        OrbitalStation b = data.addStation(-1, 0, SolarSystem.body("kerbin"));

        Tag tag = StationData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
        String text = tag.toString();
        helper.assertTrue(text.contains("\"TRANSFER\"") || text.contains("'TRANSFER'") || text.contains("TRANSFER"),
                "the state must be saved by name: " + text);
        helper.assertTrue(text.contains("duna") && text.contains("ike") && text.contains("kerbin"), "bodies must be saved by name: " + text);

        StationData back = StationData.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
        OrbitalStation a2 = back.getStationFromPosition(3 * OrbitalStation.STATION_SIZE, -7 * OrbitalStation.STATION_SIZE + 5);
        OrbitalStation b2 = back.getStationFromPosition(-1, 1023);
        helper.assertTrue(a2 != null && b2 != null, "both stations must come back by grid square");
        helper.assertTrue(a2.dX == 3 && a2.dZ == -7 && a2.name.equals("Zvezda") && a2.hasStation
                        && a2.orbiting == SolarSystem.body("duna") && a2.target == SolarSystem.body("ike")
                        && a2.state == OrbitalStation.StationState.TRANSFER && a2.stateTimer == 37 && a2.maxStateTimer == 1330
                        && a2.gravityMultiplier == 0,
                "station fields did not survive the round trip");
        helper.assertTrue(b2.dX == -1 && b2.dZ == 0 && !b2.hasStation && b2.gravityMultiplier == 1 && b2.name.isEmpty(),
                "a default station did not survive the round trip");

        // 1.7's tag without "gravity" (and without "name") reads gravity 1
        CompoundTag old = new CompoundTag();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putInt("x", 5);
        entry.putInt("z", 6);
        entry.putString("orbiting", "mun");
        entry.putString("target", "mun");
        entry.putString("state", "ORBIT");
        entry.putInt("stateTimer", 0);
        entry.putInt("maxStateTimer", 100);
        entry.putBoolean("hasStation", true);
        list.add(entry);
        old.put("stations", list);
        StationData legacy = StationData.CODEC.parse(NbtOps.INSTANCE, old).result().orElseThrow();
        OrbitalStation c = legacy.getStationFromPosition(5 * 1024, 6 * 1024);
        helper.assertTrue(c != null && c.gravityMultiplier == 1 && c.orbiting == SolarSystem.body("mun"), "a tag without gravity must read 1");

        // the grid: 1.7 floor_float(x / 1024)
        helper.assertTrue(OrbitalStation.gridOf(-1) == -1 && OrbitalStation.gridOf(0) == 0 && OrbitalStation.gridOf(1023) == 0
                && OrbitalStation.gridOf(1024) == 1, "grid squares are 1024 wide, floored");
        // 1.7 id: "0x" + hex(ChunkCoordIntPair.hashCode()); ChunkCoordIntPair(0, 0) hashes to 1013904223 ^ (1664525 * -559038737 + 1013904223)
        OrbitalStation zero = new OrbitalStation(SolarSystem.body("kerbin"), 0, 0);
        int expected = 1013904223 ^ (1664525 * -559038737 + 1013904223);
        helper.assertTrue(zero.identifier().equals("0x" + Integer.toHexString(expected).toUpperCase(java.util.Locale.ROOT)),
                "station id: " + zero.identifier());
        helper.succeed();
    }

    private static void near(GameTestHelper helper, double actual, double expected, double tolerance, String what) {
        helper.assertTrue(Math.abs(actual - expected) <= tolerance, what + ": " + actual + ", 1.7 gives " + expected);
    }

    private static void deltaV(GameTestHelper helper) {
        SolarSystem.Body kerbin = SolarSystem.body("kerbin"), mun = SolarSystem.body("mun"), duna = SolarSystem.body("duna");
        SolarSystem.Body ike = SolarSystem.body("ike"), eve = SolarSystem.body("eve");
        // Oracle17 (hbm-space SolarSystem): exact doubles; the port's float/double steps are the same, so 1e-9
        near(helper, SolarSystem.getDeltaVBetween(kerbin, duna), 1668.6843246049402, 1e-9, "dV Kerbin -> Duna");
        near(helper, SolarSystem.getDeltaVBetween(kerbin, mun), 1127.740111244644, 1e-9, "dV Kerbin -> Mun");
        near(helper, SolarSystem.getDeltaVBetween(mun, kerbin), 1127.740111244644, 1e-9, "dV Mun -> Kerbin");
        near(helper, SolarSystem.getDeltaVBetween(mun, ike), 1342.671875734652, 1e-9, "dV Mun -> Ike");
        near(helper, SolarSystem.getDeltaVBetween(kerbin, eve), 2426.5806736761347, 1e-9, "dV Kerbin -> Eve");

        double dv = SolarSystem.getDeltaVBetween(kerbin, duna);
        helper.assertTrue(SolarSystem.getFuelCost(dv, 200_000, 4200) == 4200, "xenon fuel Kerbin -> Duna");
        helper.assertTrue(SolarSystem.getFuelCost(dv, 200_000, 900) == 20900, "superheated hydrogen fuel Kerbin -> Duna");
        helper.assertTrue(SolarSystem.getFuelCost(dv, 200_000, 12000) == 1500, "D-T plasma fuel Kerbin -> Duna");
        helper.assertTrue(SolarSystem.getFuelCost(dv, 200_000, 250) == 97500, "NEO plasma energy Kerbin -> Duna");
        helper.assertTrue(SolarSystem.getFuelCost(dv, 24561, 4200) == 600, "xenon share of a two-engine station");
        helper.assertTrue(Math.round(200_000 * (1_400_000F / (1_400_000F + 10_000_000F))) == 24561, "xenon mass share");

        // TileEntityOrbitalStation.getFillRequirement: hydrazine (ISP 210, 600 kN), 4 t, from the body's surface to orbit
        helper.assertTrue(SolarSystem.getCostBetween(kerbin, kerbin, 4_000, 600_000, 210, false, true, 1.0, 1.0) == 5800, "port fill at Kerbin");
        helper.assertTrue(SolarSystem.getCostBetween(mun, mun, 4_000, 600_000, 210, false, true, 0, 0) == 700, "port fill at the Mun");
        helper.assertTrue(SolarSystem.getCostBetween(duna, duna, 4_000, 600_000, 210, false, true, 0.1, 0.1) == 1300, "port fill at Duna");
        helper.assertTrue(Orbit.pressureOf(duna) == 0.1 && Orbit.pressureOf(kerbin) == 1.0, "body pressures for the drag");

        helper.assertTrue(Orbit.getOrbitalAltitude(kerbin) == 1667670.8F, "orbital altitude at Kerbin: " + Orbit.getOrbitalAltitude(kerbin));
        helper.assertTrue(Orbit.getOrbitalAltitude(mun) == 440609.28F, "orbital altitude at the Mun: " + Orbit.getOrbitalAltitude(mun));
        helper.assertTrue(OrbitalStation.calculateTransferTime(1.0e7, 81, 11_400_000F) == 1330, "transfer time 1");
        helper.assertTrue(OrbitalStation.calculateTransferTime(6_500_000.0, 81, 10_000_000F) == 1285, "transfer time 2");
        helper.succeed();
    }

    private static void thrusters(GameTestHelper helper) {
        SolarSystem.Body kerbin = SolarSystem.body("kerbin"), mun = SolarSystem.body("mun"), duna = SolarSystem.body("duna");
        double toDuna = SolarSystem.getDeltaVBetween(kerbin, duna);
        double toMun = SolarSystem.getDeltaVBetween(kerbin, mun);

        TileEntityXenonThruster xenon = core(helper, StationBlocks.MACHINE_XENON_THRUSTER.get(), new BlockPos(1, 2, 1), Direction.WEST,
                TileEntityXenonThruster.class);
        helper.assertTrue(xenon.getThrust() == 1_400_000F && xenon.isFacingPrograde(), "xenon: 1.4 MN, prograde facing west");
        xenon.tanks[0].setFill(4000);
        xenon.power = TileEntityXenonThruster.maxPower;
        helper.assertTrue(!xenon.canPerformBurn(200_000, toDuna) && xenon.fuelCost == 4200, "xenon cannot reach Duna on 4,000 mB");
        List<Component> errors = new ArrayList<>();
        xenon.addErrors(errors);
        // 4,200 mB also costs 21 MHE, over the 20 MHE buffer: both 1.7 lines
        helper.assertTrue(errors.size() == 2 && errors.get(0).getString().contains("needs 21.0MHE")
                && errors.get(1).getString().contains("needs 4200mB"), "xenon errors: " + errors);
        helper.assertTrue(xenon.canPerformBurn(200_000, toMun) && xenon.fuelCost == 2800, "xenon reaches the Mun with 2,800 mB");
        helper.assertTrue(xenon.startBurn() == 200 && xenon.tanks[0].getFill() == 1200 && xenon.power == 20_000_000L - 2800L * 5000L,
                "xenon start: 200 ticks, 2,800 mB and 14 MHE used");
        helper.assertTrue(xenon.endBurn() == 200 && !xenon.isOn, "xenon stop: 200 ticks");

        TileEntityMachineHTR3 htr3 = core(helper, StationBlocks.MACHINE_HTR3.get(), new BlockPos(3, 2, 1), Direction.SOUTH,
                TileEntityMachineHTR3.class);
        helper.assertTrue(htr3.tanks[0].getTankType() == SpaceFluids.SUPERHEATED_HYDROGEN && htr3.tanks[0].getMaxFill() == 1_280_000,
                "LPW-3N: 1,280,000 mB of superheated hydrogen");
        htr3.tanks[0].setFill(1_280_000);
        helper.assertTrue(htr3.canPerformBurn(200_000, toDuna) && htr3.fuelCost == 20900 && htr3.getThrust() == 800_000_000F,
                "LPW-3N: 20,900 mB to Duna (ISP 900), 800 MN");
        helper.assertTrue(htr3.startBurn() == 80 && htr3.tanks[0].getFill() == 1_280_000 - 20900 && htr3.endBurn() == 80, "LPW-3N burn");

        helper.assertTrue(BuiltInRegistries.BLOCK.getOptional(Ids.of("ntmspace", "machine_htrf4")).isEmpty(),
                "machine_htrf4 is not registered");

        TileEntityMachineHTRNeo neo = core(helper, StationBlocks.MACHINE_HTRF4NEO.get(), new BlockPos(7, 2, 1), Direction.SOUTH,
                TileEntityMachineHTRNeo.class);
        neo.power = TileEntityMachineHTRNeo.maxPower;
        neo.temperature = TileEntityMachineHTRNeo.temperature_target;
        neo.plasmaEnergySync = 97_499;
        helper.assertTrue(!neo.canPerformBurn(200_000, toDuna) && neo.fuelCost == 97500, "NEO needs 97,500 TU to Duna");
        neo.plasmaEnergySync = 97_500;
        helper.assertTrue(neo.canPerformBurn(200_000, toDuna), "NEO burns with 97,500 TU, full power, cooled");
        helper.assertTrue(neo.startBurn() == 180 && neo.power == 0 && neo.endBurn() == 180, "NEO start spends all its power");

        TileEntityStationPropulsionCreative creative = core(helper, StationBlocks.PROPULSION_CREATIVE.get(), new BlockPos(9, 2, 1), null,
                TileEntityStationPropulsionCreative.class);
        helper.assertTrue(creative.canPerformBurn(Integer.MAX_VALUE, 1e12) && creative.getThrust() == 10_000_000F
                && creative.startBurn() == 20 && creative.endBurn() == 20, "creative: always, 10 MN, 20 ticks");
        helper.succeed();
    }

    private static <T extends BlockEntity> T core(GameTestHelper helper, Block block, BlockPos relative, Direction facing,
                              Class<T> type) {
        BlockState state = block.defaultBlockState();
        if (facing != null) state = state.setValue(BlockMultiblockCore.FACING, facing);
        helper.setBlock(relative, state);
        return GameTestHelpers.blockEntity(helper, relative, type);
    }

    /**
     * The travel test's own station square. A station is one 1024-block square, which a test plot shares with the plots
     * around it, and engines of neighbouring tests join its station: hbm-space's HTR-NEO registers on its first server
     * tick anywhere, not only in orbit (its 1.7 {@code updateEntity} has no {@code inOrbit} check, unlike the other
     * thrusters), so the NEO of {@code station_thruster_burns} placed in the same square a tick earlier made this journey
     * fail its burn check (4 of 24 full runs on 1.21.1 Fabric, 2026-10-09). Far from any station another test or a plot uses:
     * those squares are within 31 of the origin (1.7's 32,000-block limit), 3,000..3,999 on the diagonal, or the
     * plots' own.
     */
    private static final int TRAVEL_SQUARE_X = -20_011, TRAVEL_SQUARE_Z = 20_017;

    private static void travel(GameTestHelper helper) {
        SolarSystem.Body kerbin = SolarSystem.body("kerbin"), duna = SolarSystem.body("duna");
        var server = helper.getLevel().getServer();
        StationData data = StationData.get(server);
        data.removeStation(TRAVEL_SQUARE_X, TRAVEL_SQUARE_Z);
        // the creative engine's block entity, standing in the middle of that square (an engine registers by its position)
        BlockPos pos = new BlockPos(TRAVEL_SQUARE_X * OrbitalStation.STATION_SIZE + 512, 64,
                TRAVEL_SQUARE_Z * OrbitalStation.STATION_SIZE + 512);
        TileEntityStationPropulsionCreative engine = new TileEntityStationPropulsionCreative(pos,
                StationBlocks.PROPULSION_CREATIVE.get().defaultBlockState());
        engine.setLevel(helper.getLevel());
        OrbitalStation station = OrbitalStation.getStationFromPosition(server, pos.getX(), pos.getZ());
        helper.assertTrue(station.dX == TRAVEL_SQUARE_X && station.dZ == TRAVEL_SQUARE_Z && station.engines().isEmpty(),
                "a fresh station in the test's own square");
        station.orbiting = station.target = kerbin;
        station.setState(OrbitalStation.StationState.ORBIT, 0);

        // no engine registered: no journey
        station.travelTo(helper.getLevel(), duna);
        helper.assertTrue(station.state == OrbitalStation.StationState.ORBIT, "a station without engines must not leave");

        engine.registerPropulsion();
        try {
            helper.assertTrue(station.engines().contains(engine), "the engine registered with its square's station");
            station.travelTo(helper.getLevel(), duna);
            helper.assertTrue(station.state == OrbitalStation.StationState.LEAVING && station.maxStateTimer == 20
                    && station.target == duna, "LEAVING for the slowest engine's spool-up (20); state " + station.state
                    + " for " + station.maxStateTimer + ", engines " + station.engines().size());

            List<OrbitalStation.StationState> seen = new ArrayList<>();
            for (int i = 0; i < 200 && !(station.state == OrbitalStation.StationState.ORBIT && seen.size() > 1); i++) {
                station.update(helper.getLevel());
                if (seen.isEmpty() || seen.get(seen.size() - 1) != station.state) seen.add(station.state);
            }
            // no core port registered: 1.7 getTransferTime answers -1, so TRANSFER ends at once
            helper.assertTrue(seen.contains(OrbitalStation.StationState.TRANSFER) && seen.contains(OrbitalStation.StationState.ARRIVING)
                    && station.state == OrbitalStation.StationState.ORBIT, "states: " + seen);
            helper.assertTrue(station.orbiting == duna, "the station orbits Duna after arriving");
        } finally {
            engine.unregisterPropulsion();
            data.removeStation(TRAVEL_SQUARE_X, TRAVEL_SQUARE_Z);
        }
        helper.succeed();
    }

    private static void voidGenerator(GameTestHelper helper) {
        OrbitChunkGenerator generator = new OrbitChunkGenerator(helper.getLevel().getChunkSource().getGenerator().getBiomeSource());
        for (int[] column : new int[][] {{0, 0}, {512, 512}, {-30000, 12345}}) {
            helper.assertTrue(generator.getBaseHeight(column[0], column[1], net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                    helper.getLevel(), null) == 0, "base height 0");
            NoiseColumn noise = generator.getBaseColumn(column[0], column[1], helper.getLevel(), null);
            for (int y = 0; y < 256; y++) {
                helper.assertTrue(noise.getBlock(y).isAir(), "orbit column has " + noise.getBlock(y) + " at y " + y);
            }
        }
        helper.assertTrue(generator.getGenDepth() == 256 && generator.minimumY() == 0, "0 to 255, as 1.7");
        helper.succeed();
    }

    private static void coreStructure(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, StationBlocks.ORBITAL_STATION.get(), Direction.NORTH);
        TileEntityOrbitalStation te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityOrbitalStation.class);
        helper.assertTrue(te.isCore() && te.tanks[0].getTankType() == SpaceFluids.HYDRAZINE && te.tanks[0].getMaxFill() == 16_000
                && te.getContainerSize() == 16, "core: 16 slots, 16,000 mB hydrazine");
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos cell = core.offset(dx, dy, dz);
                    if (cell.equals(core)) continue;
                    helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), cell)),
                            "cell " + dx + "," + dy + "," + dz + " does not belong to the core");
                }
            }
        }
        // the rim takes hydrazine from outside, the inner top does not
        com.ntmspace.block.station.BlockOrbitalStation block = StationBlocks.ORBITAL_STATION.get();
        BlockPos rim = core.offset(0, 1, 2);
        helper.assertTrue(block.canConnect(SpaceFluids.HYDRAZINE, helper.getLevel(), rim.getX(), rim.getY(), rim.getZ(),
                com.hbm.lib.ForgeDirection.SOUTH), "the south rim takes fluid from the south");
        // the port is not placeable by hand (1.7 canPlaceBlockAt false for the core)
        helper.assertTrue(!block.canPlaceAt(helper.getLevel(), core.offset(0, 0, 6), Direction.NORTH), "the core must not be placeable");
        helper.succeed();
    }

    private static void xenonPorts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // facing north: dir is (0, 0, -1) and dir.getRotation(UP) is east
        TileEntityXenonThruster xenon = placed(helper, StationBlocks.MACHINE_XENON_THRUSTER.get(), Direction.NORTH,
                TileEntityXenonThruster.class);
        BlockPos core = xenon.getBlockPos();
        Port[] row = {new Port(-1, 0, 0, ForgeDirection.SOUTH), new Port(0, 0, 0, ForgeDirection.SOUTH),
                new Port(1, 0, 0, ForgeDirection.SOUTH)};
        assertPorts(helper, "xenon thruster", core, xenon.getPorts(), row);
        assertOpen(helper, "xenon thruster", core, xenon.getPorts(), BlockMultiblockCore.ROLE_POWER_IN | BlockMultiblockCore.ROLE_FLUID_IN);

        // 1.7 updates the ports only in orbit; a GameTest world is not the orbit
        xenon.updatePorts();
        for (Port port : row) {
            helper.assertTrue(takes(level, xenon, core, port, xenon.tanks[0]), "the thruster takes no xenon at " + port);
            helper.assertTrue(powered(level, xenon, core, port), "the thruster takes no power at " + port);
        }
        helper.succeed();
    }

    private static void htr3Ports(GameTestHelper helper) {
        // facing north: 1.7's dir, the facing turned about UP, is east
        TileEntityMachineHTR3 htr3 = placed(helper, StationBlocks.MACHINE_HTR3.get(), Direction.NORTH, TileEntityMachineHTR3.class);
        assertPorts(helper, "LPW-3N", htr3.getBlockPos(), htr3.getPorts(), new Port(-5, 0, 0, ForgeDirection.WEST));
        assertOpen(helper, "LPW-3N", htr3.getBlockPos(), htr3.getPorts(), BlockMultiblockCore.ROLE_FLUID_IN);
        helper.succeed();
    }

    private static void rimTakesFuelUndocked(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, StationBlocks.ORBITAL_STATION.get(), Direction.NORTH);
        TileEntityOrbitalStation te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityOrbitalStation.class);
        Port[] rim = {
                new Port(-1, 1, 2, ForgeDirection.SOUTH), new Port(0, 1, 2, ForgeDirection.SOUTH), new Port(1, 1, 2, ForgeDirection.SOUTH),
                new Port(-1, 1, -2, ForgeDirection.NORTH), new Port(0, 1, -2, ForgeDirection.NORTH), new Port(1, 1, -2, ForgeDirection.NORTH),
                new Port(2, 1, -1, ForgeDirection.EAST), new Port(2, 1, 0, ForgeDirection.EAST), new Port(2, 1, 1, ForgeDirection.EAST),
                new Port(-2, 1, -1, ForgeDirection.WEST), new Port(-2, 1, 0, ForgeDirection.WEST), new Port(-2, 1, 1, ForgeDirection.WEST)};
        assertPorts(helper, "docking port", core, te.getPorts(), rim);

        te.updatePorts();
        helper.assertTrue(te.getDocked() == null && te.getReceivingTanks() == te.tanks, "nothing docked, the tank still receives");
        for (Port port : rim) {
            helper.assertTrue(takes(helper.getLevel(), te, core, port, te.tanks[0]), "the rim takes no hydrazine at " + port);
        }
        helper.assertTrue(te.transferFluid(SpaceFluids.HYDRAZINE, 0, 1000) == 0 && te.tanks[0].getFill() == 1000,
                "the tank takes hydrazine with nothing docked");
        helper.succeed();
    }

    private static void teleporterTakesNmass(GameTestHelper helper) {
        BlockPos pad = new BlockPos(1, 2, 1);
        helper.setBlock(pad, ModRadarUtilityBlocks.machine_teleporter.get().defaultBlockState());
        TileEntityMachineTeleporter tele = GameTestHelpers.blockEntity(helper, pad, TileEntityMachineTeleporter.class);
        FluidTankNTM tank = tele.getCrossingTank();
        helper.assertTrue(tank != null && tank.getTankType() == SpaceFluids.NMASS && tank.getMaxFill() == 16_000
                && tele.getCrossingCost() == 100, "16,000 mB of N-MASS, 100 mB a crossing");
        BlockPos at = helper.absolutePos(pad);
        helper.succeedWhen(() -> {
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                helper.assertTrue(takes(helper.getLevel(), tele, at, new Port(0, 0, 0, side), tank), "no N-MASS from " + side);
            }
        });
    }

    private static void teleporterBread(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pad = GameTests.paddedCentre(8, 2);
        helper.setBlock(pad, ModRadarUtilityBlocks.machine_teleporter.get().defaultBlockState());
        TileEntityMachineTeleporter tele = GameTestHelpers.blockEntity(helper, pad, TileEntityMachineTeleporter.class);
        BlockPos from = helper.absolutePos(pad);
        BlockPos exit = from.offset(0, 0, 3);
        // 1.7 checks the item after either kind of send, so the same level will do
        tele.setTarget(GlobalPos.of(level.dimension(), exit));
        AABB around = new AABB(exit).inflate(2);

        tele.power = TileEntityMachineTeleporter.maxPower;
        ItemEntity stone = onPad(level, from, Items.STONE);
        tele.teleport(stone);
        helper.assertTrue(level.getEntitiesOfClass(Slime.class, around).isEmpty(), "a stone left a slime");

        tele.power = TileEntityMachineTeleporter.maxPower;
        ItemEntity bread = onPad(level, from, Items.BREAD);
        tele.teleport(bread);
        List<Slime> slimes = level.getEntitiesOfClass(Slime.class, around);
        helper.assertTrue(slimes.size() == 1, "bread leaves one slime, got " + slimes.size());
        Slime slime = slimes.get(0);
        helper.assertTrue(slime.position().distanceTo(bread.position()) < 1e-6
                && Math.abs(slime.getY() - (exit.getY() + 1.5)) < 1e-6, "the slime is not at the exit: " + slime.position());
        helper.assertTrue(slime.getSize() == 1 || slime.getSize() == 2 || slime.getSize() == 4, "1.7 sizes are 1, 2, 4: " + slime.getSize());
        stone.discard();
        bread.discard();
        slime.discard();
        helper.succeed();
    }

    private static ItemEntity onPad(ServerLevel level, BlockPos pad, Item item) {
        ItemEntity entity = new ItemEntity(level, pad.getX() + 0.5, pad.getY() + 1.0, pad.getZ() + 0.5, new ItemStack(item));
        entity.setNoGravity(true);
        level.addFreshEntity(entity);
        return entity;
    }

    private static void neoFedByTouchingTorus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ForgeDirection rot = ForgeDirection.getOrientation(Direction.SOUTH).getRotation(ForgeDirection.UP);
        BlockPos base = helper.absolutePos(new BlockPos(0, 4, 0));
        BlockPos neoCore = base.offset(rot.offsetX * 11, 0, rot.offsetZ * 11);
        BlockPos end = neoCore.offset(rot.offsetX * -9, 0, rot.offsetZ * -9);
        BlockPos torusCore = neoCore.offset(rot.offsetX * -17, -2, rot.offsetZ * -17);
        MultiblockHandlerXR.placeAssembled(level, neoCore, StationBlocks.MACHINE_HTRF4NEO.get(), Direction.SOUTH);
        MultiblockHandlerXR.placeAssembled(level, torusCore, ModMachineBlocks.fusion_torus.get(), Direction.NORTH);
        helper.assertTrue(neoCore.equals(MultiblockSurface.coreOfCell(level, end)), "the NEO's plasma end is not at core - 9");
        helper.assertTrue(level.getBlockEntity(torusCore) instanceof TileEntityFusionTorus, "no torus");
        helper.assertTrue(level.getBlockEntity(neoCore) instanceof TileEntityMachineHTRNeo, "the torus replaced the NEO");

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(level.getBlockEntity(neoCore) instanceof TileEntityMachineHTRNeo,
                    "no NEO at its core, the block there is " + level.getBlockState(neoCore));
            TileEntityMachineHTRNeo neo = (TileEntityMachineHTRNeo) level.getBlockEntity(neoCore);
            TileEntityFusionTorus torus = (TileEntityFusionTorus) level.getBlockEntity(torusCore);
            helper.assertTrue(FusionLink.at(level, end, MachineCaps.PLASMA_IN) == neo, "the torus beam's cell is not the NEO's");
            boolean linked = false;
            for (boolean arm : torus.connections) linked |= arm;
            helper.assertTrue(linked, "the torus sees no receiver on any arm");

            // facing south: dir is (0, 0, 1) and dir.getRotation(UP) is west
            assertPorts(helper, "HTR-F4 NEO", neoCore, neo.getPorts(), new Port(5, -2, -2, ForgeDirection.NORTH),
                    new Port(-1, -2, -2, ForgeDirection.NORTH), new Port(5, -2, 2, ForgeDirection.SOUTH),
                    new Port(-1, -2, 2, ForgeDirection.SOUTH));
            assertOpen(helper, "HTR-F4 NEO", neoCore, neo.getPorts(), BlockMultiblockCore.ROLE_POWER_IN | BlockMultiblockCore.ROLE_FLUID);

            helper.assertTrue(PortNodes.isPorted(level, neo, true) && PortNodes.isPorted(level, neo, false), "the NEO has no ports");
            for (DirPos con : PortDef.connections(neo.getPorts())) {
                helper.assertTrue(PortNodes.allows(level, con.getPos().asLong(), con.getDir(), neo, PowerNetProvider.THE_PROVIDER, true),
                        "no power port at " + con.getPos());
                helper.assertTrue(fluidPort(level, neo, con, neo.coolantTanks[0], true) && fluidPort(level, neo, con, neo.coolantTanks[1], false),
                        "cold coolant in and warm coolant out at " + con.getPos());
            }
            helper.succeed();
        });
    }
}
