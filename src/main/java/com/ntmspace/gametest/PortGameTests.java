package com.ntmspace.gametest;

import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericCompressor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
import com.ntmspace.tileentity.machine.TileEntityMachineAlkylation;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import com.ntmspace.tileentity.machine.TileEntityMachineMagma;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
import com.ntmspace.tileentity.machine.TileEntityRadiator;
import com.ntmspace.tileentity.station.TileEntityMachineHTR3;
import com.ntmspace.tileentity.station.TileEntityMachineHTRF4;
import com.ntmspace.tileentity.station.TileEntityMachineLPW2Space;
import com.ntmspace.tileentity.station.TileEntityXenonThruster;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Every 1.7 connection point of the progression machines ends up on one of the machine's ports, in 1.7's role: a duct
 * there reaches the machine for power or for the named fluid, and the machine is no longer found by its faces. The
 * station machines tick only in orbit, so for them only the geometry is checked: a port of the structure aims at each
 * point.
 */
public final class PortGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    private record Role(String what, INetworkProvider type, boolean subscribe,
                        BiFunction<BlockPos, ForgeDirection, DirPos[]> points) {}

    static {
        INetworkProvider power = PowerNetProvider.THE_PROVIDER;
        machine("ports_alkylation", SpaceMachines.MACHINE_ALKYLATION::get,
                new Role("power", power, true, TileEntityMachineAlkylation::conPos));
        machine("ports_cryo_distill", SpaceMachines.MACHINE_CRYO_DISTILL::get,
                new Role("power", power, true, (c, d) -> TileEntityMachineCryoDistill.ports(c, d).power()),
                new Role("earth air", FluidNetProvider.of(SpaceFluids.EARTHAIR), true, (c, d) -> TileEntityMachineCryoDistill.ports(c, d).input()));
        machine("ports_milk_reformer", SpaceMachines.MACHINE_MILK_REFORMER::get,
                new Role("power", power, true, (c, d) -> TileEntityMachineMilkReformer.conPos(c)));
        machine("ports_vacuum_circuit", SpaceMachinesB.MACHINE_VACUUM_CIRCUIT::get,
                new Role("power", power, true, (c, d) -> TileEntityMachineVacuumCircuit.conPos(c)));
        machine("ports_hydroponic", SpaceMachinesB.HYDROBAY::get,
                new Role("power", power, true, (c, d) -> TileEntityHydroponic.ports(c, d).power()),
                new Role("CO2", FluidNetProvider.of(Fluids.CARBONDIOXIDE), true, (c, d) -> TileEntityHydroponic.ports(c, d).fluid()),
                new Role("oxygen", FluidNetProvider.of(Fluids.OXYGEN), false, (c, d) -> TileEntityHydroponic.ports(c, d).fluid()));
        machine("ports_magma", SpaceMachinesC.MACHINE_MAGMA::get,
                new Role("power", power, true, TileEntityMachineMagma::conPos));
        machine("ports_radiator", SpaceMachinesC.MACHINE_RADIATOR::get,
                new Role("spent steam", FluidNetProvider.of(Fluids.SPENTSTEAM), true, TileEntityRadiator::conPos),
                new Role("water", FluidNetProvider.of(Fluids.WATER), false, TileEntityRadiator::conPos));
        machine("ports_atmo_vent", AtmosphereMachines.ATMO_VENT::get,
                new Role("power", power, true, TileEntityAtmosphericCompressor::conPos));
        machine("ports_atmo_emitter", AtmosphereMachines.ATMO_EMITTER::get,
                new Role("power", power, true, TileEntityAtmosphericEmitter::conPos));
        machine("ports_dyson_launcher", DysonContent.DYSON_LAUNCHER::get,
                new Role("power", power, true, TileEntityDysonLauncher::conPos));
        machine("ports_dyson_converter_he", DysonContent.DYSON_CONVERTER_HE::get,
                new Role("power out", power, false, (c, d) -> new DirPos[] {TileEntityDysonConverterHE.output(c, d)}));
        geometry("ports_htr3", StationBlocks.MACHINE_HTR3::get, TileEntityMachineHTR3::conPos);
        geometry("ports_htrf4", StationBlocks.MACHINE_HTRF4::get, TileEntityMachineHTRF4::conPos);
        geometry("ports_xenon_thruster", StationBlocks.MACHINE_XENON_THRUSTER::get, TileEntityXenonThruster::conPos);
        geometry("ports_lpw2", ModPowerGenBlocks.MACHINE_LPW2::get, TileEntityMachineLPW2Space::conPos);
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
                    if (!reaches(level, point.getPos(), te, role)) missing.add(role.what + " at " + point.getPos());
                }
            }
            helper.assertTrue(missing.isEmpty(), "1.7 points without a port: " + missing);
        });
    }

    private static void geometry(String name, Supplier<? extends BlockMultiblockCore> block,
                                 BiFunction<BlockPos, ForgeDirection, DirPos[]> points) {
        TESTS.function(name, 20, 12, () -> helper -> {
            BlockPos core = helper.absolutePos(GameTests.paddedCentre(12, 2));
            MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, block.get(), Direction.NORTH);
            BlockState state = helper.getLevel().getBlockState(core);
            DirPos[] expected = points.apply(core, MachineCore.facing(state));
            PortDef[] ports = MachinePorts.ports(helper.getLevel(), state, core, expected);
            List<String> wrong = new ArrayList<>();
            for (DirPos point : expected) {
                boolean found = false;
                for (DirPos con : PortDef.connections(ports)) found |= con.getPos().equals(point.getPos());
                if (!found) wrong.add("no port aimed at " + point.getPos());
            }
            if (ports.length == 0) wrong.add("no ports");
            helper.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
            helper.succeed();
        });
    }

    private static boolean reaches(ServerLevel level, BlockPos point, Object te, Role role) {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (PortNodes.allows(level, point.asLong(), side, te, role.type, role.subscribe)) return true;
        }
        return false;
    }
}
