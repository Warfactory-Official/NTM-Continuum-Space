package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ISatChip;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.saveddata.SatelliteSavedData;
import com.mojang.serialization.JsonOps;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.orbit.NavDrives;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import com.ntmspace.orbit.StationDock;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketTarget;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The rocket family's GameTests. The fuel, mass and height expectations come from hbm-space's own
 * {@code RocketStruct} mass and fuel code and {@code SolarSystem.getCostBetween}, copied verbatim into a program run
 * outside the game with the parts' 1.7 make-call values (scratchpad {@code rocket/oracle/RocketOracle.java}, on top of
 * the orbit family's {@code orbit/oracle/Oracle17.java}):
 *
 * <pre>
 * R1 rp_c_20 | rp_f_20_12 + rp_l_20 + mp_thruster_20_kerosene: launch 14400 kg, wet 30400, height 17.9, tank 64000,
 *    Kerbin->Mun 26600 mB, Kerbin->Kerbin orbit 12000, Kerbin->Duna 26700
 * R2 rp_c_20 | rp_f_20_6 + rp_l_20 + mp_thruster_20_methalox | 3 x rp_f_20_12 + 3 x mp_thruster_20_kerosene:
 *    launch 39700 / 12500 kg, wet 87700, height 25.5, tank 192000, Kerbin->Mun 71800 (stage 0), Mun->Kerbin 7400 (stage 1)
 * R3 rp_c_20 | rp_f_20_3 + rp_l_20 + mp_thruster_20_hydrogen: launch 11800 kg, tank 16000, Kerbin->Mun 22300
 * R5 satellite_spy | rp_f_20_12 + rp_l_20 + mp_thruster_20_kerosene: launch 22400 kg, Kerbin->Kerbin 31600, height 21.4
 * </pre>
 */
public final class RocketGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** hbm-space's validation rules and issue lines, on valid and invalid stacks. */
    public static final GameTests.Function VALIDATION = TESTS.function("rocket_assembly_validation", 20,
            () -> RocketGameTests::validation);

    /** Masses, heights, fuel capacity, fuel needed and fill requirements against the 1.7 oracle. */
    public static final GameTests.Function FUEL = TESTS.function("rocket_fuel_matches_17", 20,
            () -> RocketGameTests::fuel);

    /** hbm-space stacks fuselages and thrusters (HBM's through PART_PROPERTIES), warheads and fins stay single. */
    public static final GameTests.Function STACKING = TESTS.function("rocket_part_stacking", 20,
            () -> RocketGameTests::stacking);

    /** The VAB reads its stages, raises its platform and spire, builds the rocket item and takes it apart again. */
    public static final GameTests.Function VAB = TESTS.function("rocket_assembly_vab", 60, 8,
            () -> RocketGameTests::vab);

    /** The pad takes a fuelled rocket and a processed drive, fills its tanks, launches and empties. */
    public static final GameTests.Function PAD = TESTS.function("rocket_launch_pad", 60, 8,
            () -> RocketGameTests::pad);

    /** A satellite rocket launches at once, climbs past y 900 and puts its satellite in orbit (no dimension change). */
    public static final GameTests.Function SATELLITE_FLIGHT = TESTS.function("rocket_satellite_flight", 1200, 4,
            () -> RocketGameTests::satelliteFlight);

    /** A capsule with landing legs coming down lands upright (LANDED) on the ground. */
    public static final GameTests.Function LANDING = TESTS.function("rocket_landing", 600, 4,
            () -> RocketGameTests::landing);

    /** A station core arriving at its orbit marks the drive's station launched, orbiting the launch body. */
    public static final GameTests.Function STATION_CORE = TESTS.function("rocket_station_core_launch", 20,
            () -> RocketGameTests::stationCore);

    /** A pod held against a free station port docks (needs fuel), refuels, and is stowed into the port's slots. */
    public static final GameTests.Function DOCKING = TESTS.function("rocket_station_docking", 60, 8,
            () -> RocketGameTests::docking);

    /** The drive: one item per body, processing levels, NavDrives hooks, station target written onto the stack. */
    public static final GameTests.Function DRIVES = TESTS.function("rocket_nav_drives", 20,
            () -> RocketGameTests::drives);

    /** Rockets, drives and rocket entities save by name, never by numeric id. */
    public static final GameTests.Function SAVES = TESTS.function("rocket_saves_by_name", 20,
            () -> RocketGameTests::saves);

    /** The rocket's look overlay (1.7 printHook) through HBM's IEntityLookOverlay. */
    public static final GameTests.Function LOOK_OVERLAY = TESTS.function("rocket_look_overlay", 20,
            () -> RocketGameTests::lookOverlay);

    private RocketGameTests() {}

    public static void init() {}

    // ---------------------------------------------------------------------------------------- rockets

    private static RocketPart part(String id) {
        RocketPart part = RocketParts.get(id);
        if (part == null) throw new IllegalStateException("no rocket part " + id);
        return part;
    }

    static RocketStruct r1() {
        RocketStruct rocket = new RocketStruct(part(RocketParts.CAPSULE));
        rocket.addStage(part("ntmspace:rp_f_20_12"), part("ntmspace:rp_l_20"), part("hbm:mp_thruster_20_kerosene"), 1, 1);
        return rocket;
    }

    static RocketStruct r2() {
        RocketStruct rocket = new RocketStruct(part(RocketParts.CAPSULE));
        rocket.addStage(part("ntmspace:rp_f_20_6"), part("ntmspace:rp_l_20"), part("ntmspace:mp_thruster_20_methalox"), 1, 1);
        rocket.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_kerosene"), 3, 3);
        return rocket;
    }

    static RocketStruct r3() {
        RocketStruct rocket = new RocketStruct(part(RocketParts.CAPSULE));
        rocket.addStage(part("ntmspace:rp_f_20_3"), part("ntmspace:rp_l_20"), part("ntmspace:mp_thruster_20_hydrogen"), 1, 1);
        return rocket;
    }

    static RocketStruct r5() {
        RocketStruct rocket = new RocketStruct(part("hbm:satellite_spy"));
        rocket.addStage(part("ntmspace:rp_f_20_12"), part("ntmspace:rp_l_20"), part("hbm:mp_thruster_20_kerosene"), 1, 1);
        return rocket;
    }

    private static SolarSystem.Body body(String name) {
        return SolarSystem.body(name);
    }

    private static boolean hasIssue(List<Component> issues, String text) {
        for (Component issue : issues) if (issue.getString().equals(text)) return true;
        return false;
    }

    private static void validation(GameTestHelper helper) {
        helper.assertTrue(r1().validate() && r2().validate() && r3().validate() && r5().validate(), "R1, R2, R3, R5 must be valid");

        // fuel mismatch: a kerosene fuselage on a hydrogen thruster
        RocketStruct mismatch = new RocketStruct(part(RocketParts.CAPSULE));
        mismatch.addStage(part("hbm:mp_fuselage_15_kerosene"), null, part("ntmspace:mp_thruster_20_hydrogen"), 1, 1);
        helper.assertTrue(!mismatch.validate(), "kerosene fuselage + hydrogen thruster must be invalid");
        helper.assertTrue(hasIssue(mismatch.findIssues(0, null, null, false, false), "Stage 1 fuel mismatch"), "fuel mismatch issue");

        // too many and uneven thrusters
        RocketStruct tooMany = new RocketStruct(part(RocketParts.CAPSULE));
        tooMany.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_kerosene"), 1, 2);
        helper.assertTrue(!tooMany.validate() && hasIssue(tooMany.findIssues(0, null, null, false, false), "Stage 1 too many thrusters"),
                "1 fuselage, 2 thrusters: too many");
        RocketStruct uneven = new RocketStruct(part(RocketParts.CAPSULE));
        uneven.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_kerosene"), 3, 2);
        helper.assertTrue(!uneven.validate() && hasIssue(uneven.findIssues(0, null, null, false, false), "Stage 1 uneven thrusters"),
                "3 fuselages, 2 thrusters: uneven");

        // no payload / no stages / a missile warhead payload
        RocketStruct noStages = new RocketStruct(part(RocketParts.CAPSULE));
        helper.assertTrue(!noStages.validate(), "a capsule alone is no rocket");
        RocketStruct warhead = new RocketStruct(part("hbm:mp_warhead_15_he"));
        warhead.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_kerosene"), 1, 1);
        helper.assertTrue(!warhead.validate() && hasIssue(warhead.findIssues(0, null, null, false, false), "Invalid Capsule/Satellite"),
                "an HE warhead is no payload");

        // the upper stage without legs warns, the stats lines lead
        List<Component> issues = r2().findIssues(0, body("kerbin"), body("mun"), false, false);
        helper.assertTrue(issues.get(0).getString().equals("Dry mass: 39700kg") && issues.get(1).getString().equals("Wet mass: 87700kg")
                        && issues.get(2).getString().equals("Thrust: 4608000N") && issues.get(3).getString().equals("ISP: 308s"),
                "R2 stage 0 stats lines, got " + issues);
        helper.assertTrue(hasIssue(issues, "Trip possible! 192000/71800mB"), "R2 Kerbin->Mun trip possible, got " + issues);
        helper.assertTrue(hasIssue(r1().findIssues(0, null, null, false, false), "Dry mass: 14400kg"), "R1 dry mass line");
        RocketStruct legless = new RocketStruct(part(RocketParts.CAPSULE));
        legless.addStage(part("ntmspace:rp_f_20_6"), null, part("ntmspace:mp_thruster_20_methalox"), 1, 1);
        legless.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_kerosene"), 1, 1);
        helper.assertTrue(hasIssue(legless.findIssues(0, null, null, false, false), "Stage 2 lacks landing legs"), "upper stage without legs");
        helper.assertTrue(hasIssue(r3().findIssues(0, body("kerbin"), body("mun"), false, false), "Insufficient fuel: 16000/22300mB"),
                "R3 insufficient fuel line");

        // the payload slot: capsules and satellites, not the pod
        helper.assertTrue(TileEntityMachineRocketAssembly.isCapsule(new ItemStack(RocketItems.RP_C_20.get()))
                && TileEntityMachineRocketAssembly.isCapsule(new ItemStack(RocketItems.RP_SC_20.get()))
                && TileEntityMachineRocketAssembly.isCapsule(ItemSatellite.make(ItemSatellite.EnumSatType.SPY))
                && !TileEntityMachineRocketAssembly.isCapsule(new ItemStack(RocketItems.RP_POD_20.get()))
                && !TileEntityMachineRocketAssembly.isCapsule(new ItemStack(ModCustomMissileItems.mp_warhead_15_he.get())),
                "payload slot: capsule, station core, satellite yes; pod, HE warhead no");
        helper.succeed();
    }

    private static void fuel(GameTestHelper helper) {
        RocketStruct r1 = r1(), r2 = r2(), r3 = r3(), r5 = r5();
        SolarSystem.Body kerbin = body("kerbin"), mun = body("mun"), duna = body("duna");

        helper.assertTrue(r1.getLaunchMass(0) == 14400 && r1.getWetMass(0) == 30400 && Math.abs(r1.getHeight() - 17.9) < 1e-9
                && r1.getFuelCapacity(0) == 64000, "R1 masses/height/tank");
        helper.assertTrue(r1.getFuelRequired(0, kerbin, mun, false, false) == 26600, "R1 Kerbin->Mun 26600, got " + r1.getFuelRequired(0, kerbin, mun, false, false));
        helper.assertTrue(r1.getFuelRequired(0, kerbin, kerbin, false, true) == 12000, "R1 Kerbin->orbit 12000");
        helper.assertTrue(r1.getFuelRequired(0, kerbin, duna, false, false) == 26700, "R1 Kerbin->Duna 26700");

        helper.assertTrue(r2.getLaunchMass(0) == 39700 && r2.getLaunchMass(1) == 12500 && r2.getWetMass(0) == 87700
                && Math.abs(r2.getHeight() - 25.5) < 1e-9 && r2.getFuelCapacity(0) == 192000, "R2 masses/height/tank");
        helper.assertTrue(r2.getFuelRequired(0, kerbin, mun, false, false) == 71800, "R2 Kerbin->Mun 71800");
        helper.assertTrue(r2.getFuelRequired(1, mun, kerbin, false, false) == 7400, "R2 Mun->Kerbin 7400 (upper stage)");
        helper.assertTrue(r2.stages.get(0).getCluster() == 3 && r2.stages.get(0).getStack() == 1, "R2 3 x 1 cluster");

        helper.assertTrue(r3.getLaunchMass(0) == 11800 && r3.getFuelRequired(0, kerbin, mun, false, false) == 22300, "R3 22300");
        helper.assertTrue(r5.getLaunchMass(0) == 22400 && r5.getFuelRequired(0, kerbin, kerbin, false, false) == 31600
                && Math.abs(r5.getHeight() - 21.4) < 1e-9, "R5 22400 kg, 31600 mB, 21.4 high");

        helper.assertTrue(r1.hasSufficientFuel(kerbin, mun, false, false) && !r3.hasSufficientFuel(kerbin, mun, false, false),
                "R1 reaches the Mun, R3 does not");

        // pods and bare capsules by rule
        RocketStruct pod = new RocketStruct(part(RocketParts.POD));
        helper.assertTrue(pod.hasSufficientFuel(kerbin, kerbin, true, false) && pod.hasSufficientFuel(kerbin, kerbin, false, true)
                && !pod.hasSufficientFuel(kerbin, kerbin, false, false) && !pod.hasSufficientFuel(kerbin, mun, true, false),
                "pods fall to and return from their own body's orbit only");
        RocketStruct capsule = new RocketStruct(part(RocketParts.CAPSULE));
        helper.assertTrue(capsule.hasSufficientFuel(kerbin, kerbin, true, false) && !capsule.hasSufficientFuel(kerbin, kerbin, false, true),
                "a bare capsule only returns from orbit");

        // fill requirements: kerosene/peroxide for the kerosene stage, natural gas/oxygen for methalox
        Map<FluidType, Integer> fill1 = r1.getFillRequirement();
        helper.assertTrue(fill1.size() == 2 && fill1.get(Fluids.KEROSENE) == 64000 && fill1.get(Fluids.PEROXIDE) == 64000, "R1 fill " + fill1);
        Map<FluidType, Integer> fill2 = r2.getFillRequirement();
        helper.assertTrue(fill2.get(Fluids.KEROSENE) == 192000 && fill2.get(Fluids.PEROXIDE) == 192000 && fill2.get(Fluids.GAS) == 32000
                && fill2.get(Fluids.OXYGEN) == 32000, "R2 fill " + fill2);
        RocketStruct hydrazine = new RocketStruct(part(RocketParts.CAPSULE));
        hydrazine.addStage(part("ntmspace:mp_fuselage_20_hydrazine"), null, part("ntmspace:mp_thruster_20_hydrazine"), 1, 1);
        Map<FluidType, Integer> fill3 = hydrazine.getFillRequirement();
        helper.assertTrue(fill3.size() == 1 && fill3.get(SpaceFluids.HYDRAZINE) == 12500, "hydrazine needs no oxidiser " + fill3);
        RocketStruct solid = new RocketStruct(part(RocketParts.CAPSULE));
        solid.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_solid"), 1, 1);
        helper.assertTrue(solid.getFillRequirement().get(Fluids.NONE) == 64000, "solid fuel is the NONE entry");
        helper.succeed();
    }

    private static void stacking(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(ModCustomMissileItems.mp_fuselage_15_kerosene.get()).getMaxStackSize() == 64
                && new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene.get()).getMaxStackSize() == 64,
                "HBM's fuselages and thrusters stack to 64 with NTM: Space");
        helper.assertTrue(new ItemStack(ModCustomMissileItems.mp_warhead_15_he.get()).getMaxStackSize() == 1
                && new ItemStack(ModCustomMissileItems.mp_stability_15_flat.get()).getMaxStackSize() == 1
                && new ItemStack(ModCustomMissileItems.mp_chip_1.get()).getMaxStackSize() == 1, "warheads, fins, chips stay single");
        helper.assertTrue(new ItemStack(RocketItems.RP_F_20_12.get()).getMaxStackSize() == 64
                && new ItemStack(RocketItems.MP_THRUSTER_20_METHALOX.get()).getMaxStackSize() == 64
                && new ItemStack(RocketItems.RP_L_20.get()).getMaxStackSize() == 1
                && new ItemStack(RocketItems.RP_C_20.get()).getMaxStackSize() == 1, "NTM: Space's parts as hbm-space");
        helper.assertTrue(RocketParts.all().size() == 147, "147 rocket parts, found " + RocketParts.all().size());
        int missing = 0;
        for (RocketPart part : RocketParts.all()) if (part.item() == net.minecraft.world.item.Items.AIR) missing++;
        helper.assertTrue(missing == 0, missing + " rocket parts have no registered item");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------- machines

    private static void vab(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 3));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get(), Direction.NORTH);
        TileEntityMachineRocketAssembly vab = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineRocketAssembly.class);
        helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.offset(4, -2, 4))), "a leg belongs to the VAB");

        vab.setItem(0, new ItemStack(RocketItems.RP_C_20.get()));
        // stage 1 in the second slot group: the VAB must pull it down into the empty first stage
        vab.setItem(4, new ItemStack(RocketItems.RP_F_20_12.get()));
        vab.setItem(5, new ItemStack(RocketItems.RP_L_20.get()));
        vab.setItem(6, new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene.get()));

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(vab.getItem(1).getItem() == RocketItems.RP_F_20_12.get() && vab.getItem(4).isEmpty(),
                    "an empty stage takes the one above it");
            helper.assertTrue(vab.rocket.validate() && vab.rocket.stages.size() == 1, "the VAB reads a valid one-stage rocket, issues "
                    + vab.rocket.findIssues(0, null, null, false, false));
            // platform at the top of the 12 m fuselage, spire under it
            BlockPos deck = core.offset(1, 12, 0);
            helper.assertTrue(helper.getLevel().getBlockState(deck).getBlock() == RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get()
                    && core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), deck)), "platform cell at y + 12");
            helper.assertTrue(helper.getLevel().getBlockState(core.offset(0, 6, 0)).getBlock() == RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get()
                    && helper.getLevel().getBlockState(core.offset(4, 6, 4)).getBlock() == RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get(),
                    "central and corner spires");

            vab.construct();
            ItemStack built = vab.getItem(TileEntityMachineRocketAssembly.SLOT_RESULT);
            RocketStruct rocket = ItemCustomRocket.get(built);
            helper.assertTrue(rocket != null && rocket.sameAs(r1()) && vab.getItem(0).isEmpty() && vab.getItem(1).isEmpty(),
                    "construct: rocket_custom in the result slot, parts consumed");
            helper.assertTrue(!ItemCustomRocket.hasFuel(built) && vab.canDeconstruct(), "a new rocket is unfuelled and can be taken apart");

            vab.deconstruct();
            helper.assertTrue(vab.getItem(0).getItem() == RocketItems.RP_C_20.get() && vab.getItem(1).getItem() == RocketItems.RP_F_20_12.get()
                    && vab.getItem(2).getItem() == RocketItems.RP_L_20.get()
                    && vab.getItem(3).getItem() == ModCustomMissileItems.mp_thruster_20_kerosene.get()
                    && vab.getItem(TileEntityMachineRocketAssembly.SLOT_RESULT).isEmpty(), "deconstruct returns the parts");

            // breaking a platform cell tears the whole VAB down, platforms and spires included
            helper.getLevel().removeBlock(deck, false);
            helper.assertTrue(helper.getLevel().getBlockState(core).isAir() && helper.getLevel().getBlockState(core.offset(0, 6, 0)).isAir()
                    && helper.getLevel().getBlockState(core.offset(4, -2, 4)).isAir(), "teardown removes deck, spire and legs");
            helper.succeed();
        });
    }

    private static ItemStack driveTo(DriveBody body, int x, int z) {
        ItemStack drive = new ItemStack(RocketItems.drive(body));
        RocketItems.DRIVE.set(drive, DriveData.EMPTY.withCoordinates(x, z).withProcessed(true));
        return drive;
    }

    private static void pad(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, RocketBlocks.LAUNCH_PAD_ROCKET.get(), Direction.NORTH);
        TileEntityLaunchPadRocket pad = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityLaunchPadRocket.class);

        pad.setItem(0, ItemCustomRocket.build(r1(), true));
        pad.setItem(1, driveTo(DriveBody.MUN, 100, 100));
        pad.power = pad.maxPower;

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(pad.tanks[0].getTankType() == Fluids.KEROSENE || pad.tanks[0].getTankType() == Fluids.PEROXIDE,
                    "the pad's tanks take the rocket's fuels, got " + pad.tanks[0].getTankType());
            boolean full = true;
            int typed = 0;
            for (var tank : pad.tanks) {
                if (tank.getTankType() == Fluids.NONE) continue;
                typed++;
                full &= tank.getFill() == 64000 && tank.getMaxFill() == 64000;
            }
            helper.assertTrue(typed == 2 && full, "a fuelled rocket fills kerosene and peroxide, 64000 mB each");
            helper.assertTrue(!ItemCustomRocket.hasFuel(pad.getItem(0)), "the pad takes the fuel flag off the rocket");
            helper.assertTrue(pad.rocket != null && pad.height == 15, "rocket on the pad, tower height 15 (floor(17.9 - 3.5 + 1)), got " + pad.height);
            helper.assertTrue(pad.findIssues().isEmpty() || pad.canLaunch(), "issues " + pad.findIssues());
            helper.assertTrue(pad.canLaunch(), "a fuelled R1 with a processed Mun drive and 100 kHE can launch: " + pad.findIssues());

            pad.launch(null);
            helper.assertTrue(pad.getItem(0).isEmpty() && pad.getItem(1).isEmpty() && pad.power == 25_000
                    && pad.tanks[0].getTankType() == Fluids.NONE, "launch empties the pad and takes 75 kHE");
            List<EntityRideableRocket> rockets = helper.getLevel().getEntitiesOfClass(EntityRideableRocket.class,
                    new net.minecraft.world.phys.AABB(core).inflate(2, 6, 2));
            helper.assertTrue(rockets.size() == 1 && rockets.get(0).getState() == RocketState.AWAITING
                    && Math.abs(rockets.get(0).getY() - (core.getY() + 3)) < 1e-6, "the rocket stands 3 up, awaiting its pilot");
            rockets.get(0).discard();
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------------------------------- flight

    private static void satelliteFlight(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        int freq = 424242;
        ItemStack stack = ItemCustomRocket.build(r5());
        ISatChip.setFreqS(stack, freq);
        EntityRideableRocket rocket = EntityRideableRocket.create(helper.getLevel(), base.getX() + 0.5F, base.getY(), base.getZ() + 0.5F, stack)
                .withProgram(driveTo(DriveBody.KERBIN, base.getX(), base.getZ()));
        helper.assertTrue(!rocket.canRide(), "a satellite rocket carries no crew");
        // The rocket pitches over to 45 degrees towards +X (yaw -90) and passes y 900 about 760 blocks east of the pad
        // (1.7's flight; about 425 ticks). Its own ticket asks for the next chunk only at the border, which under a busy
        // batch is not entity-ticking in time, so the rocket froze in mid-air ("still flying at y 210 on tick 1202").
        // The whole corridor is held, and the rocket starts once all of it is entity-ticking.
        int[] corridor = {-16, 840, -16, 16};
        Runnable release = PlotChunks.force(helper, corridor[0], corridor[1], corridor[2], corridor[3]);
        SatelliteSavedData data = SatelliteSavedData.get(helper.getLevel());
        data.sats.remove(freq);
        boolean[] launched = {false};

        helper.succeedWhen(() -> {
            if (!launched[0]) {
                String waiting = PlotChunks.notTicking(helper, corridor[0], corridor[1], corridor[2], corridor[3]);
                helper.assertTrue(waiting == null, "flight corridor chunk " + waiting + " is not entity-ticking yet");
                launched[0] = true;
                helper.getLevel().addFreshEntity(rocket);
            }
            helper.assertTrue(rocket.isRemoved(), "still flying at y " + (int) rocket.getY() + " in " + rocket.getState());
            helper.assertTrue(data.sats.containsKey(freq), "the satellite reached orbit on frequency " + freq);
            data.sats.remove(freq);
            release.run();
        });
    }

    private static void landing(GameTestHelper helper) {
        BlockPos ground = helper.absolutePos(new BlockPos(2, 1, 2));
        EntityRideableRocket rocket = EntityRideableRocket.create(helper.getLevel(), ground.getX() + 0.5F, ground.getY() + 20F,
                ground.getZ() + 0.5F, ItemCustomRocket.build(r1()));
        rocket.setState(RocketState.LANDING);
        Runnable release = PlotChunks.force(helper, 16);
        helper.getLevel().addFreshEntity(rocket);
        int floor = helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, ground.getX(), ground.getZ());

        helper.succeedWhen(() -> {
            helper.assertTrue(rocket.getState() == RocketState.LANDED, "still " + rocket.getState() + " at y " + rocket.getY());
            helper.assertTrue(Math.abs(rocket.getY() - floor) < 1e-6, "landed on the ground (" + floor + "), at " + rocket.getY());
            rocket.discard();
            release.run();
        });
    }

    private static void stationCore(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        StationData data = StationData.get(server);
        int gx = 31, gz = -29;
        data.removeStation(gx, gz);

        RocketStruct core = new RocketStruct(part(RocketParts.STATION_CORE));
        core.addStage(part("ntmspace:rp_f_20_12"), null, part("hbm:mp_thruster_20_kerosene"), 1, 1);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        EntityRideableRocket rocket = EntityRideableRocket.create(helper.getLevel(), at.getX(), at.getY(), at.getZ(), ItemCustomRocket.build(core))
                .withProgram(driveTo(DriveBody.ORBIT, gx, gz));
        rocket.setDrive(rocket.navDrive);

        // the station drive's target: a station square not yet launched, orbiting the launch body (Kerbin)
        RocketTarget to = ItemVOTVdrive.getTarget(rocket.navDrive, helper.getLevel());
        helper.assertTrue(to.inOrbit() && !to.isValid() && to.body() == body("kerbin"), "an unlaunched station over Kerbin");

        rocket.beginLandingSequence(RocketTarget.of(helper.getLevel(), at.getX(), at.getZ()), to);
        OrbitalStation station = data.getStationFromPosition(gx * OrbitalStation.STATION_SIZE, gz * OrbitalStation.STATION_SIZE);
        helper.assertTrue(station != null && station.hasStation && station.orbiting == body("kerbin"), "the station is launched over Kerbin");
        helper.assertTrue(rocket.isRemoved(), "an uncrewed station core is spent on arrival");
        helper.succeed();
    }

    private static void docking(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, StationBlocks.ORBITAL_STATION_PORT.get(), Direction.NORTH);
        TileEntityOrbitalStation port = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityOrbitalStation.class);

        ItemStack pod = new ItemStack(RocketItems.RP_POD_20.get());
        helper.assertTrue(StationDock.CRAFT_ITEMS.invoker().test(pod)
                && !StationDock.CRAFT_ITEMS.invoker().test(ItemCustomRocket.build(r1(), false))
                && StationDock.CRAFT_ITEMS.invoker().test(ItemCustomRocket.build(r1(), true)), "pods, capsules and fuelled rockets dock");
        StationDock dock = StationDock.SPAWNERS.invoker().spawn(port, pod);
        helper.assertTrue(dock instanceof EntityRideableRocket, "a held pod becomes a docked craft");
        port.dockRocket(dock);
        EntityRideableRocket rocket = (EntityRideableRocket) dock;
        helper.assertTrue(rocket.isReusable() && rocket.getState() == RocketState.NEEDSFUEL, "a pod docks needing fuel");
        helper.assertTrue(Math.abs(rocket.getY() + rocket.getBbHeight() - (core.getY() + 1.5)) < 1e-6, "the pod hangs under the port");

        rocket.fuelled(true);
        helper.assertTrue(rocket.getState() == RocketState.LANDED, "fuelled without a drive: landed");
        rocket.navDrive = driveTo(DriveBody.KERBIN, 0, 0);
        rocket.setState(RocketState.NEEDSFUEL);
        rocket.fuelled(true);
        helper.assertTrue(rocket.getState() == RocketState.AWAITING && rocket.canRecall(), "fuelled with a drive: awaiting, recallable");

        List<ItemStack> stowed = rocket.stow();
        helper.assertTrue(stowed.size() == 2 && stowed.get(0).getItem() instanceof ItemVOTVdrive
                && stowed.get(1).getItem() == RocketItems.RP_POD_20.get() && rocket.isRemoved(), "stowing gives the drive, then the pod");
        port.undockRocket();
        helper.succeed();
    }

    private static void drives(GameTestHelper helper) {
        helper.assertTrue(DriveBody.values().length == 11, "orbit + 10 bodies");
        for (DriveBody body : DriveBody.values()) {
            helper.assertTrue(RocketItems.drive(body) instanceof ItemVOTVdrive drive && drive.body() == body,
                    "hard_drive_full_" + body.key() + " is the " + body + " drive");
        }
        SolarSystem.Body kerbin = body("kerbin"), duna = body("duna");
        helper.assertTrue(DriveBody.MUN.getProcessingLevel(kerbin) == 0 && DriveBody.DUNA.getProcessingLevel(kerbin) == 1
                && DriveBody.EVE.getProcessingLevel(kerbin) == 2 && DriveBody.LAYTHE.getProcessingLevel(kerbin) == 3
                && DriveBody.KERBIN.getProcessingLevel(kerbin) == 0 && DriveBody.IKE.getProcessingLevel(duna) == 0
                && DriveBody.DRES.getProcessingLevel(duna) == 2 && DriveBody.ORBIT.getProcessingLevel(duna) == 0,
                "1.7 processing levels (Eve/Dres 2, Laythe/Tekto 3 at least)");

        ItemStack drive = driveTo(DriveBody.DUNA, 120, -40);
        NavDrives.Destination destination = NavDrives.destination(drive);
        helper.assertTrue(destination != null && destination.body().equals("duna") && destination.x() == 120 && destination.z() == -40,
                "NavDrives reads the drive");
        helper.assertTrue(NavDrives.destination(new ItemStack(RocketItems.HARD_DRIVE.get())) == null, "an empty drive is no destination");

        ItemStack fetched = NavDrives.FACTORIES.invoker().create(5, 6, "Zvezda");
        NavDrives.Destination orbit = NavDrives.destination(fetched);
        helper.assertTrue(orbit != null && orbit.isOrbit() && orbit.x() == 5 && orbit.z() == 6 && ItemVOTVdrive.getProcessed(fetched)
                && "Zvezda".equals(ItemVOTVdrive.data(fetched).stationName()), "the station drive factory");

        RocketTarget target = ItemVOTVdrive.getTarget(drive, helper.getLevel());
        helper.assertTrue(target.body() == duna && !target.inOrbit() && target.isValid(), "a body drive targets the body");
        RocketTarget none = ItemVOTVdrive.getTarget(ItemStack.EMPTY, helper.getLevel());
        helper.assertTrue(none.body() == null && !none.isValid(), "no drive, no target");
        helper.succeed();
    }

    private static void saves(GameTestHelper helper) {
        RocketStruct r2 = r2();
        var json = RocketStruct.CODEC.encodeStart(JsonOps.INSTANCE, r2).result().orElseThrow();
        String text = json.toString();
        helper.assertTrue(text.contains("\"ntmspace:rp_c_20\"") && text.contains("\"hbm:mp_thruster_20_kerosene\"")
                && text.contains("\"fc\":3"), "rockets save part ids, got " + text);
        RocketStruct back = RocketStruct.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
        helper.assertTrue(back.sameAs(r2), "rocket round trip");

        var driveJson = DriveData.CODEC.encodeStart(JsonOps.INSTANCE, new DriveData(3, 4, true, false, java.util.Optional.empty(),
                java.util.Optional.empty(), "Mir", "duna", true)).result().orElseThrow().toString();
        helper.assertTrue(driveJson.contains("\"station_body\":\"duna\""), "drives name the station's body, got " + driveJson);

        // the rocket item carries the design by id, the fuel flag as a boolean
        ItemStack item = ItemCustomRocket.build(r2, true);
        helper.assertTrue(ItemCustomRocket.get(item).sameAs(r2) && ItemCustomRocket.hasFuel(item), "rocket item round trip");
        helper.succeed();
    }

    private static void lookOverlay(GameTestHelper helper) {
        var player = GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        EntityRideableRocket rocket = EntityRideableRocket.create(helper.getLevel(), 0, 100, 0, ItemCustomRocket.build(r1()));
        var data = com.hbm.interfaces.IEntityLookOverlay.of(rocket, player);
        helper.assertTrue(data != null && data.title().equals("Rocket") && data.lines().contains("Interact to enter"),
                "an empty rocket: Rocket / Interact to enter, got " + (data == null ? null : data.lines()));
        rocket.setState(RocketState.LAUNCHING);
        helper.assertTrue(com.hbm.interfaces.IEntityLookOverlay.of(rocket, player) == null, "no overlay in flight");
        EntityRideableRocket capsule = EntityRideableRocket.create(helper.getLevel(), 0, 100, 0,
                ItemCustomRocket.build(new RocketStruct(part(RocketParts.CAPSULE))));
        helper.assertTrue(com.hbm.interfaces.IEntityLookOverlay.of(capsule, player) == null, "a bare capsule on a surface has none");
        EntityRideableRocket pod = EntityRideableRocket.create(helper.getLevel(), 0, 100, 0,
                ItemCustomRocket.build(new RocketStruct(part(RocketParts.POD))));
        pod.setState(RocketState.NEEDSFUEL);
        var podData = com.hbm.interfaces.IEntityLookOverlay.of(pod, player);
        helper.assertTrue(podData != null && podData.lines().get(0).endsWith("Rocket has no fuel!"), "an unfuelled pod says so");
        helper.succeed();
    }
}
