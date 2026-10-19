package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.ModFuelItems;
import com.ntmspace.block.station.BlockOrbitalStationLauncher;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.StationLauncherBlocks;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationLauncher;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The orbital station launcher's GameTests (hbm-space {@code TileEntityOrbitalStationLauncher}). A GameTest world is not
 * the orbit, where the launcher does nothing (1.7 {@code if(!CelestialBody.inOrbit(worldObj)) return}), so the tests
 * run its server update directly; launching from it then starts from Kerbin's ground, which the R1 rocket of
 * {@link RocketGameTests} (rp_c_20 | rp_f_20_12 + rp_l_20 + mp_thruster_20_kerosene, 64,000 mB kerosene and peroxide)
 * can leave for the Mun.
 */
public final class StationLauncherGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** The 5 x 5 port shape with its rim, the launcher core, 29 slots, ten idle 64,000 mB tanks, stack limit 8. */
    public static final GameTests.Function STRUCTURE = TESTS.function("station_launcher_structure", 40, 8,
            () -> StationLauncherGameTests::structure);

    /** Stages read from the slots (an empty stage pulls the next one down) and 1.7's issue lines for drive and tanks. */
    public static final GameTests.Function BUILD = TESTS.function("station_launcher_build_and_issues", 40, 8,
            () -> StationLauncherGameTests::build);

    /** Tanks take the rocket's fill requirement; solid rocket fuel loads 250 kg per item up to the stages' capacity. */
    public static final GameTests.Function TANKS = TESTS.function("station_launcher_fuel_tanks", 40, 8,
            () -> StationLauncherGameTests::tanks);

    /** Launch spawns the rideable rocket under the port with the drive, docked; it re-docks to the launcher on load. */
    public static final GameTests.Function LAUNCH = TESTS.function("station_launcher_launch", 60, 8,
            () -> StationLauncherGameTests::launch);

    /** Saved by name: tank fluids by id, solid fuel, slots; a round trip reads them back. */
    public static final GameTests.Function SAVES = TESTS.function("station_launcher_saves_by_name", 40, 8,
            () -> StationLauncherGameTests::saves);

    private StationLauncherGameTests() {}

    public static void init() {}

    private static TileEntityOrbitalStationLauncher place(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, StationLauncherBlocks.ORBITAL_STATION_LAUNCHER.get(), Direction.NORTH);
        return GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityOrbitalStationLauncher.class);
    }

    private static ItemStack drive(DriveBody body, int x, int z, boolean processed) {
        ItemStack drive = new ItemStack(RocketItems.drive(body));
        RocketItems.DRIVE.set(drive, DriveData.EMPTY.withCoordinates(x, z).withProcessed(processed));
        return drive;
    }

    /** R1's parts in the launcher: payload, and the stage in the given stage group (0 = slots 4..6). */
    private static void loadR1(TileEntityOrbitalStationLauncher te, int group) {
        int o = TileEntityOrbitalStationLauncher.SLOT_STAGES + group * 3;
        te.setItem(TileEntityOrbitalStationLauncher.SLOT_PAYLOAD, new ItemStack(RocketItems.RP_C_20.get()));
        te.setItem(o, new ItemStack(RocketItems.RP_F_20_12.get()));
        te.setItem(o + 1, new ItemStack(RocketItems.RP_L_20.get()));
        te.setItem(o + 2, new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene.get()));
    }

    private static void fill(TileEntityOrbitalStationLauncher te) {
        for (FluidTankNTM tank : te.tanks) if (tank.getTankType() != Fluids.NONE) tank.setFill(tank.getMaxFill());
        te.solidFuel.level = te.solidFuel.max;
    }

    private static boolean hasIssue(List<Component> issues, String text) {
        for (Component issue : issues) if (issue.getString().equals(text)) return true;
        return false;
    }

    private static void structure(GameTestHelper helper) {
        TileEntityOrbitalStationLauncher te = place(helper);
        BlockPos core = te.getBlockPos();
        helper.assertTrue(te.getContainerSize() == 29 && TileEntityOrbitalStationLauncher.SLOT_DRIVES == 19 && te.getMaxStackSize() == 8,
                "29 slots (drive, fuel in/out, payload, 15 stage parts, 10 ghost drives), stack limit 8");
        boolean idle = te.tanks.length == 10;
        for (FluidTankNTM tank : te.tanks) idle &= tank.getTankType() == Fluids.NONE && tank.getMaxFill() == 64_000;
        helper.assertTrue(idle, "ten idle 64,000 mB tanks");
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos cell = core.offset(dx, dy, dz);
                    if (cell.equals(core)) continue;
                    helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), cell)),
                            "cell " + dx + "," + dy + "," + dz + " does not belong to the launcher");
                }
            }
        }
        BlockOrbitalStationLauncher block = StationLauncherBlocks.ORBITAL_STATION_LAUNCHER.get();
        BlockPos rim = core.offset(0, 1, 2);
        helper.assertTrue(block.canConnect(Fluids.KEROSENE, helper.getLevel(), rim.getX(), rim.getY(), rim.getZ(),
                com.hbm.lib.ForgeDirection.SOUTH), "the south rim takes fluid from the south");
        helper.assertTrue(te.isItemValidForSlot(TileEntityOrbitalStationLauncher.SLOT_DRIVE, drive(DriveBody.MUN, 0, 0, true))
                && !te.isItemValidForSlot(TileEntityOrbitalStationLauncher.SLOT_DRIVE, new ItemStack(RocketItems.RP_C_20.get()))
                && te.isItemValidForSlot(TileEntityOrbitalStationLauncher.SLOT_FUEL_IN, new ItemStack(ModFuelItems.rocket_fuel.get()))
                && !te.isItemValidForSlot(TileEntityOrbitalStationLauncher.SLOT_FUEL_IN, new ItemStack(RocketItems.RP_C_20.get())),
                "slot 0 takes drives, slot 1 rocket fuel (1.7 isItemValidForSlot)");
        helper.assertTrue(StationLauncherBlocks.ORBITAL_STATION_LAUNCHER.get().defaultDestroyTime() == 1.0F, "hardness 1");
        helper.succeed();
    }

    private static void build(GameTestHelper helper) {
        TileEntityOrbitalStationLauncher te = place(helper);

        te.updateServer();
        helper.assertTrue(!te.rocket.validate() && te.findIssues().isEmpty() && te.getReceivingTanks().length == 0,
                "an empty launcher has no rocket, no issues and takes no fluid");

        // stage in the second slot group: the launcher must pull it down into the empty first stage
        loadR1(te, 1);
        te.updateServer();
        helper.assertTrue(te.getItem(4).getItem() == RocketItems.RP_F_20_12.get() && te.getItem(7).isEmpty()
                && te.getItem(9).isEmpty(), "an empty stage takes the one above it");
        helper.assertTrue(te.rocket.validate() && te.rocket.sameAs(RocketGameTests.r1()), "the launcher reads R1, issues "
                + te.rocket.findIssues(0, null, null, false, false));

        List<Component> issues = te.findIssues();
        helper.assertTrue(issues.size() == 3 && hasIssue(issues, "0/64000mB Kerosene") && hasIssue(issues, "0/64000mB Peroxide")
                && hasIssue(issues, "No destination drive installed"), "tanks and missing drive, got " + issues);
        helper.assertTrue(te.getReceivingTanks().length == 2, "a valid rocket opens its two typed tanks to ducts");

        te.setItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE, drive(DriveBody.MUN, 100, 100, false));
        helper.assertTrue(hasIssue(te.findIssues(), "Destination drive needs processing"), "unprocessed drive: " + te.findIssues());

        te.setItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE, drive(DriveBody.MUN, 100, 100, true));
        issues = te.findIssues();
        helper.assertTrue(issues.size() == 2 && !te.canLaunch(), "a processed Mun drive leaves only the empty tanks, got " + issues);

        // a payload alone is no rocket (1.7 validate: no stages)
        for (int i = TileEntityOrbitalStationLauncher.SLOT_STAGES; i < TileEntityOrbitalStationLauncher.SLOT_DRIVES; i++) te.setItem(i, ItemStack.EMPTY);
        te.updateServer();
        helper.assertTrue(!te.rocket.validate() && te.findIssues().isEmpty() && !te.canLaunch(), "a capsule without stages cannot launch");
        helper.succeed();
    }

    private static void tanks(GameTestHelper helper) {
        TileEntityOrbitalStationLauncher te = place(helper);
        loadR1(te, 0);
        te.updateServer();
        int typed = 0;
        boolean sized = true;
        for (FluidTankNTM tank : te.tanks) {
            if (tank.getTankType() == Fluids.NONE) continue;
            typed++;
            sized &= tank.getMaxFill() == 64_000 && tank.getFill() == 0
                    && (tank.getTankType() == Fluids.KEROSENE || tank.getTankType() == Fluids.PEROXIDE);
        }
        helper.assertTrue(typed == 2 && sized && te.solidFuel.max == 0, "R1: kerosene and peroxide, 64,000 mB each, empty");

        // a solid rocket: rp_c_20 | mp_fuselage_10_solid + mp_thruster_10_solid
        for (int i = TileEntityOrbitalStationLauncher.SLOT_STAGES; i < TileEntityOrbitalStationLauncher.SLOT_DRIVES; i++) te.setItem(i, ItemStack.EMPTY);
        te.setItem(4, new ItemStack(ModCustomMissileItems.mp_fuselage_10_solid.get()));
        te.setItem(6, new ItemStack(ModCustomMissileItems.mp_thruster_10_solid.get()));
        te.setItem(TileEntityOrbitalStationLauncher.SLOT_FUEL_IN, new ItemStack(ModFuelItems.rocket_fuel.get(), 2));
        te.updateServer();
        Integer solid = te.rocket.getFillRequirement().get(Fluids.NONE);
        helper.assertTrue(te.rocket.validate() && solid != null && te.solidFuel.max == solid, "solid capacity " + solid + ", max " + te.solidFuel.max);
        helper.assertTrue(te.solidFuel.level == Math.min(250, solid) && te.getItem(TileEntityOrbitalStationLauncher.SLOT_FUEL_IN).getCount() == 1,
                "one rocket fuel item gives 250 kg, got " + te.solidFuel.level);
        boolean noFluids = true;
        for (FluidTankNTM tank : te.tanks) noFluids &= tank.getTankType() == Fluids.NONE;
        helper.assertTrue(noFluids, "a solid rocket needs no fluid tanks");
        te.updateServer();
        helper.assertTrue(te.solidFuel.level == Math.min(500, solid) && te.getItem(TileEntityOrbitalStationLauncher.SLOT_FUEL_IN).isEmpty(),
                "the second item gives 500 kg, got " + te.solidFuel.level);
        helper.succeed();
    }

    private static void launch(GameTestHelper helper) {
        TileEntityOrbitalStationLauncher te = place(helper);
        BlockPos core = te.getBlockPos();
        loadR1(te, 0);
        te.setItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE, drive(DriveBody.MUN, 100, 100, true));
        te.setItem(TileEntityOrbitalStationLauncher.SLOT_DRIVES, drive(DriveBody.KERBIN, 0, 0, true)); // a ghost stage drive
        te.updateServer();
        helper.assertTrue(!te.canLaunch(), "empty tanks: no launch");
        te.launch(null);
        helper.assertTrue(te.getDocked() == null && !te.getItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE).isEmpty(), "a refused launch keeps everything");

        fill(te);
        helper.assertTrue(te.canLaunch() && te.findIssues().size() == 2, "full tanks and a processed Mun drive: " + te.findIssues());
        te.launch(null);

        EntityRideableRocket rocket = te.getDocked();
        helper.assertTrue(rocket != null && !rocket.isRemoved() && rocket.getRocket().sameAs(RocketGameTests.r1()), "the launched rocket is docked");
        // 1.7 passed the position as floats: GameTest plots sit near x = 14.6 million, where a float steps by 1
        helper.assertTrue(Math.abs(rocket.getY() + rocket.getBbHeight() - (core.getY() + 1.5)) < 1e-3
                && Math.abs(rocket.getX() - (core.getX() + 0.5)) <= 1.0, "the rocket hangs under the port: y " + rocket.getY()
                + " + " + rocket.getBbHeight() + " vs " + (core.getY() + 1.5) + ", x " + rocket.getX() + " vs " + (core.getX() + 0.5));
        helper.assertTrue(rocket.getState() == RocketState.AWAITING && rocket.navDrive != null
                && ItemVOTVdrive.getBody(rocket.navDrive) == DriveBody.MUN, "awaiting its pilot, programmed for the Mun");
        boolean empty = true;
        for (int i = 0; i < te.getContainerSize(); i++) if (i != 1 && i != 2) empty &= te.getItem(i).isEmpty();
        boolean idle = true;
        for (FluidTankNTM tank : te.tanks) idle &= tank.getTankType() == Fluids.NONE && tank.getMaxFill() == 64_000;
        helper.assertTrue(empty && idle && te.solidFuel.max == 0, "launch takes drive, parts and ghost drives and resets the tanks");
        te.updateServer();
        helper.assertTrue(te.hasDocked && !te.hasRider, "synced: docked, no rider");

        // 1.7 sizeSet: the launcher forgets its rocket (a reload); the rocket's first tick docks it again
        te.undockRocket();
        Runnable release = PlotChunks.force(helper, 16);
        // checked once the rocket has ticked (its first tick is the one that re-docks): the plot is held forced so it
        // ticks at all (PlotChunks), and the check waits for that tick instead of assuming it within 3 ticks
        afterFirstTick(helper, rocket, 40, () -> {
            helper.assertTrue(te.getDocked() == rocket, "the parked rocket re-docked to the launcher (DOCK_TARGETS) on its first tick");
            rocket.setState(RocketState.UNDOCKING);
            te.updateServer();
            helper.assertTrue(te.getDocked() == null && !te.hasDocked, "an undocking rocket leaves the launcher");
            List<EntityRideableRocket> rockets = helper.getLevel().getEntitiesOfClass(EntityRideableRocket.class, new AABB(core).inflate(3, 30, 3));
            rockets.forEach(EntityRideableRocket::discard);
            release.run();
            helper.succeed();
        });
    }

    /** Runs {@code then} on the GameTest tick after {@code entity}'s first tick (within {@code ticks}), or fails. */
    private static void afterFirstTick(GameTestHelper helper, net.minecraft.world.entity.Entity entity, int ticks, Runnable then) {
        helper.runAfterDelay(1, () -> {
            if (entity.tickCount > 0) then.run();
            else if (ticks <= 1) helper.fail("the rocket never ticked");
            else afterFirstTick(helper, entity, ticks - 1, then);
        });
    }

    private static void saves(GameTestHelper helper) {
        TileEntityOrbitalStationLauncher te = place(helper);
        loadR1(te, 0);
        te.setItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE, drive(DriveBody.MUN, 100, 100, true));
        te.updateServer();
        for (FluidTankNTM tank : te.tanks) if (tank.getTankType() != Fluids.NONE) tank.setFill(1234);
        te.solidFuel.level = 7;
        te.solidFuel.max = 9;

        CompoundTag saved = BlockEntities.saveWithoutMetadata(te, helper.getLevel().registryAccess());
        String text = saved.toString();
        helper.assertTrue(text.contains("hbm:kerosene") && text.contains("hbm:peroxide") && text.contains("ntmspace:rp_c_20"),
                "tank fluids and items are saved by name: " + text);

        TileEntityOrbitalStationLauncher copy = new TileEntityOrbitalStationLauncher(te.getBlockPos(), te.getBlockState());
        GameTestHelpers.copyPersistentData(te, copy);
        boolean tanks = true;
        for (int i = 0; i < te.tanks.length; i++) {
            tanks &= copy.tanks[i].getTankType() == te.tanks[i].getTankType() && copy.tanks[i].getFill() == te.tanks[i].getFill()
                    && copy.tanks[i].getMaxFill() == te.tanks[i].getMaxFill();
        }
        helper.assertTrue(tanks && copy.solidFuel.level == 7 && copy.solidFuel.max == 9, "tanks and solid fuel read back");
        helper.assertTrue(copy.getItem(TileEntityOrbitalStationLauncher.SLOT_PAYLOAD).getItem() == RocketItems.RP_C_20.get()
                && ItemVOTVdrive.getBody(copy.getItem(TileEntityOrbitalStationLauncher.SLOT_DRIVE)) == DriveBody.MUN, "slots read back");
        helper.succeed();
    }
}
