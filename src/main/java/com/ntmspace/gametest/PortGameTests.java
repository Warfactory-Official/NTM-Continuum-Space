package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.AtmosphereContent;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericCompressor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
import com.ntmspace.tileentity.machine.TileEntityMachineAlkylation;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import com.ntmspace.tileentity.machine.TileEntityMachineMagma;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import com.ntmspace.tileentity.machine.TileEntityRadiator;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * Every 1.7 connection point of the progression machines ends up on one of the machine's ports, in 1.7's role: a duct
 * there reaches the machine for power or for the named fluid, and the machine is no longer found by its faces. The
 * station machines tick only in orbit; {@code StationGameTests} and {@code Lpw2GameTests} pin the thrusters' ports. The
 * vacuum circuit, whose 1.7 ports leave half its faces dead, is in {@code MachineGameTestsB}.
 */
public final class PortGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** {@code absent}: the points must not reach the machine in this role. */
    private record Role(String what, INetworkProvider type, boolean subscribe,
                        BiFunction<BlockPos, ForgeDirection, DirPos[]> points, boolean absent) {
        Role(String what, INetworkProvider type, boolean subscribe, BiFunction<BlockPos, ForgeDirection, DirPos[]> points) {
            this(what, type, subscribe, points, false);
        }
    }

    static {
        INetworkProvider power = PowerNetProvider.THE_PROVIDER;
        machine("ports_alkylation", SpaceMachines.MACHINE_ALKYLATION::get,
                new Role("power", power, true, (c, d) -> firstOnCell(TileEntityMachineAlkylation.ports(c, d), true)),
                new Role("power", power, true, (c, d) -> firstOnCell(TileEntityMachineAlkylation.ports(c, d), false), true),
                new Role("chloromethane", FluidNetProvider.of(SpaceFluids.CHLOROMETHANE), true, TileEntityMachineAlkylation::conPos));
        machine("ports_cryo_distill", SpaceMachines.MACHINE_CRYO_DISTILL::get,
                new Role("power", power, true, (c, d) -> TileEntityMachineCryoDistill.ports(c, d).power()),
                new Role("earth air", FluidNetProvider.of(SpaceFluids.EARTHAIR), true, (c, d) -> TileEntityMachineCryoDistill.ports(c, d).input()));
        machine("ports_milk_reformer", SpaceMachines.MACHINE_MILK_REFORMER::get,
                new Role("power", power, true, (c, d) -> firstOnCell(TileEntityMachineMilkReformer.ports(c), true)),
                new Role("power", power, true, (c, d) -> firstOnCell(TileEntityMachineMilkReformer.ports(c), false), true),
                new Role("enriched milk", FluidNetProvider.of(SpaceFluids.EMILK), false, (c, d) -> TileEntityMachineMilkReformer.conPos(c)));
        machine("ports_hydroponic", SpaceMachinesB.HYDROBAY::get,
                new Role("power", power, true, (c, d) -> PortDef.connections(TileEntityHydroponic.powerPorts(c))),
                new Role("CO2", FluidNetProvider.of(Fluids.CARBONDIOXIDE), true, (c, d) -> PortDef.connections(TileEntityHydroponic.fluidPorts(c, d))),
                new Role("oxygen", FluidNetProvider.of(Fluids.OXYGEN), false, (c, d) -> PortDef.connections(TileEntityHydroponic.fluidPorts(c, d))));
        machine("ports_magma", SpaceMachinesC.MACHINE_MAGMA::get,
                new Role("power", power, true, (c, d) -> PortDef.connections(TileEntityMachineMagma.ports(c, d))));
        machine("ports_radiator", SpaceMachinesC.MACHINE_RADIATOR::get,
                new Role("spent steam", FluidNetProvider.of(Fluids.SPENTSTEAM), true, TileEntityRadiator::conPos),
                new Role("water", FluidNetProvider.of(Fluids.WATER), false, TileEntityRadiator::conPos));
        machine("ports_atmo_vent", AtmosphereMachines.ATMO_VENT::get,
                new Role("power", power, true, TileEntityAtmosphericCompressor::conPos));
        machine("ports_atmo_emitter", AtmosphereMachines.ATMO_EMITTER::get,
                new Role("power", power, true, (c, d) -> firstOnCell(TileEntityAtmosphericEmitter.ports(c, d), true)),
                new Role("power", power, true, (c, d) -> firstOnCell(TileEntityAtmosphericEmitter.ports(c, d), false), true));
        TESTS.function("ports_fluid_only", 40, 8, () -> PortGameTests::fluidOnly);
        machine("ports_dyson_launcher", DysonContent.DYSON_LAUNCHER::get,
                new Role("power", power, true, (c, d) -> PortDef.connections(TileEntityDysonLauncher.ports(c, d))));
        machine("ports_dyson_converter_he", DysonContent.DYSON_CONVERTER_HE::get,
                new Role("power out", power, false, (c, d) -> PortDef.connections(TileEntityDysonConverterHE.ports(c, d))));
    }

    private PortGameTests() {}

    public static void init() {}

    private static void machine(String name, Supplier<? extends BlockMultiblockCore> block, Role... roles) {
        TESTS.function(name, 40, 8, () -> helper -> check(helper, block.get(), roles));
    }

    private static void check(GameTestHelper helper, BlockMultiblockCore block, Role[] roles) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, block, Direction.NORTH);
        ServerLevel level = helper.getLevel();
        ForgeDirection dir = MachineCore.facing(level.getBlockState(core));
        helper.succeedWhen(() -> {
            Object te = level.getBlockEntity(core);
            List<String> missing = new ArrayList<>();
            for (Role role : roles) {
                if (!PortNodes.isPorted(level, te, role.type == PowerNetProvider.THE_PROVIDER)) missing.add(role.what + " has no ports");
                for (DirPos point : role.points.apply(core, dir)) {
                    if (reaches(level, point.getPos(), te, role) == role.absent) {
                        missing.add((role.absent ? "unexpected " : "") + role.what + " at " + point.getPos());
                    }
                }
            }
            helper.assertTrue(missing.isEmpty(), "1.7 points without a port: " + missing);
        });
    }

    /**
     * The connections of the ports that are first ({@code first}) or not first on their cell. 1.7 gives a power port the
     * node already on its cell, so only a cell's first port is connected.
     */
    private static DirPos[] firstOnCell(PortDef[] ports, boolean first) {
        List<DirPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (PortDef port : ports) {
            if (seen.add(port.portPositions[0]) == first) out.addAll(Arrays.asList(port.portConnections));
        }
        return out.toArray(new DirPos[0]);
    }

    /** 1.7 gives the gas dock and the air vent a fluid port manager only: no power node on their cells. */
    private static void fluidOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos dock = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(level, dock, SpaceMachinesC.GAS_DOCK.get(), Direction.NORTH);
        BlockPos vent = dock.offset(3, 0, 0);
        level.setBlock(vent, AtmosphereContent.AIR_VENT.get().defaultBlockState(), 3);
        helper.succeedWhen(() -> {
            for (BlockPos pos : List.of(dock, vent)) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof TileEntityLoadedBase te && te.fluidInPorts != null
                        && te.powerPorts == null, "fluid ports only at " + pos);
            }
        });
    }

    private static boolean reaches(ServerLevel level, BlockPos point, Object te, Role role) {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (PortNodes.allows(level, point.asLong(), side, te, role.type, role.subscribe)) return true;
        }
        return false;
    }
}
