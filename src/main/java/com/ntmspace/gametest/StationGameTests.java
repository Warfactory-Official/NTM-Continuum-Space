package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitChunkGenerator;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.tileentity.station.TileEntityMachineHTR3;
import com.ntmspace.tileentity.station.TileEntityMachineHTRF4;
import com.ntmspace.tileentity.station.TileEntityMachineHTRNeo;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.tileentity.station.TileEntityStationPropulsionCreative;
import com.ntmspace.tileentity.station.TileEntityXenonThruster;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;

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

    private StationGameTests() {}

    public static void init() {}

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

        TileEntityMachineHTRF4 htrf4 = core(helper, StationBlocks.MACHINE_HTRF4.get(), new BlockPos(5, 2, 1), Direction.SOUTH,
                TileEntityMachineHTRF4.class);
        htrf4.tanks[0].setFill(64000);
        htrf4.power = TileEntityMachineHTRF4.maxPower;
        helper.assertTrue(htrf4.canPerformBurn(200_000, toDuna) && htrf4.fuelCost == 1500 && htrf4.getThrust() == 1_600_000_000F,
                "HTR-F4: 1,500 mB of D-T plasma (ISP 12,000), 1.6 GN");
        helper.assertTrue(htrf4.startBurn() == 180 && htrf4.power == 1_000_000_000L - 1500L * 250_000L && htrf4.tanks[0].getFill() == 62500,
                "HTR-F4 start: 375 MHE and 1,500 mB");
        helper.assertTrue(htrf4.getReceivingTanks().length == 0, "HTR-F4 takes no plasma from ducts (1.7)");

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

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T core(GameTestHelper helper, net.minecraft.world.level.block.Block block, BlockPos relative, Direction facing,
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
}
