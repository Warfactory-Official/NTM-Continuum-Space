package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.machine.ItemReactorSensor;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.tileentity.machine.TileEntityDishControl;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests of the progression machines, part D: the StarDar and its remote access console. Expected values are
 * hbm-space's {@code processDrive}, {@code updateDriveCoords}, the structure of {@code MachineStardar.fillSpace} and
 * the console's link/save, worked by hand from the 1.7 source.
 */
public final class MachineGameTestsD {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function STRUCTURE = TESTS.function("stardar_structure", 20, 4,
            () -> MachineGameTestsD::structure);
    public static final GameTests.Function PROGRAM_BODY = TESTS.function("stardar_programs_body_drive", 20, 4,
            () -> MachineGameTestsD::programBody);
    public static final GameTests.Function PROGRAM_ORBIT = TESTS.function("stardar_programs_orbit_drive", 20, 4,
            () -> MachineGameTestsD::programOrbit);
    public static final GameTests.Function DRIVE_COORDS = TESTS.function("stardar_drive_coords", 20, 4,
            () -> MachineGameTestsD::driveCoords);
    public static final GameTests.Function HEIGHTMAP = TESTS.function("stardar_heightmap_scan", 1200, 4,
            () -> MachineGameTestsD::heightmap);
    public static final GameTests.Function DISH_LINK = TESTS.function("stardar_dish_console_link", 20, 4,
            () -> MachineGameTestsD::dishLink);

    private MachineGameTestsD() {}

    public static void init() {}

    private static TileEntityMachineStardar placeStardar(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(4, 4));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachinesD.MACHINE_STARDAR.get(), Direction.NORTH);
        return GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineStardar.class);
    }

    /** 1.7 {@code fillSpace}: platform, eight legs three down, and the rack beside the front leg (north: x -1/+1). */
    private static void structure(GameTestHelper helper) {
        TileEntityMachineStardar te = placeStardar(helper);
        BlockPos core = te.getBlockPos();
        List<String> errors = new ArrayList<>();
        for (BlockPos cell : new BlockPos[] {core.offset(2, 0, 2), core.offset(-2, 0, -2), core.offset(1, 0, -1),
                core.offset(2, -3, 2), core.offset(0, -3, -2), core.offset(-2, -3, 0)}) {
            if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), cell))) errors.add("no cell at " + core.subtract(cell));
        }
        // dir NORTH (0, -1), rot = NORTH.getRotation(UP); the rack is core - rot + dir * 2 at y -2 and -3
        com.hbm.lib.ForgeDirection rot = com.hbm.lib.ForgeDirection.NORTH.getRotation(com.hbm.lib.ForgeDirection.UP);
        BlockPos rack = core.offset(-rot.offsetX, -2, -2 - rot.offsetZ);
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), rack))) errors.add("no rack cell");
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), rack.below()))) errors.add("no lower rack cell");
        if (!helper.getLevel().getBlockState(core.offset(1, -1, 1)).isAir()) errors.add("the space between the legs is filled");
        MachineGameTests.finish(helper, errors);
    }

    /**
     * {@code processDrive}: an empty drive becomes the body's drive; with the StarDar's position (the current body) it is
     * processed with that approximate destination; without one it is a bare unprocessed drive; a full drive is left
     * alone.
     */
    private static void programBody(GameTestHelper helper) {
        TileEntityMachineStardar te = placeStardar(helper);
        List<String> errors = new ArrayList<>();

        te.setItem(0, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.processDrive("kerbin", 120, -340);
        ItemStack drive = te.getItem(0);
        DriveData data = ItemVOTVdrive.data(drive);
        if (ItemVOTVdrive.getBody(drive) != DriveBody.KERBIN || !(drive.getItem() instanceof ItemVOTVdrive)) errors.add("not a kerbin drive: " + drive);
        if (!data.processed()) errors.add("kerbin drive not processed");
        if (data.ax().orElse(0) != 120 || data.az().orElse(0) != -340) errors.add("approximate destination " + data.ax() + ", " + data.az());
        if (data.x() != 0 || data.z() != 0) errors.add("landing coordinates were set: " + data.x() + ", " + data.z());

        // a full drive is not reprogrammed
        te.processDrive("mun", 0, 0);
        if (ItemVOTVdrive.getBody(te.getItem(0)) != DriveBody.KERBIN) errors.add("a full drive was reprogrammed");

        te.setItem(0, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.processDrive("mun", 0, 0);
        drive = te.getItem(0);
        data = ItemVOTVdrive.data(drive);
        if (ItemVOTVdrive.getBody(drive) != DriveBody.MUN) errors.add("not a mun drive: " + drive);
        if (data.processed() || data.ax().isPresent()) errors.add("1.7 left the remote body's drive bare: " + data);

        te.setItem(0, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.processDrive("thatmo", 0, 0);
        if (!te.getItem(0).is(RocketItems.HARD_DRIVE.get())) errors.add("a body without a drive was programmed");

        MachineGameTests.finish(helper, errors);
    }

    /** {@code processDrive(orbit)} off a station: a processed orbit drive for a free grid square within reach. */
    private static void programOrbit(GameTestHelper helper) {
        TileEntityMachineStardar te = placeStardar(helper);
        List<String> errors = new ArrayList<>();

        te.setItem(0, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.processDrive(DriveBody.ORBIT.key(), 0, 0);
        ItemStack drive = te.getItem(0);
        DriveData data = ItemVOTVdrive.data(drive);
        int size = Orbit.MAX_STATION_DISTANCE / OrbitalStation.STATION_SIZE;
        if (ItemVOTVdrive.getBody(drive) != DriveBody.ORBIT || !(drive.getItem() instanceof ItemVOTVdrive)) errors.add("not an orbit drive: " + drive);
        if (!data.processed()) errors.add("orbit drive not processed");
        if (data.x() < -size || data.x() >= size || data.z() < -size || data.z() >= size) errors.add("grid square " + data.x() + ", " + data.z());
        if (data.ax().isPresent()) errors.add("orbit drive got an approximate destination");
        MachineGameTests.finish(helper, errors);
    }

    /** {@code updateDriveCoords}: ax + px - 128, az + pz - 128 (1.7 does not process the drive here). */
    private static void driveCoords(GameTestHelper helper) {
        TileEntityMachineStardar te = placeStardar(helper);
        List<String> errors = new ArrayList<>();

        ItemStack drive = new ItemStack(RocketItems.drive(DriveBody.DUNA));
        RocketItems.DRIVE.set(drive, DriveData.EMPTY.withApproximate(1000, -500));
        te.setItem(0, drive);
        te.updateDriveCoords(130, 10);
        DriveData data = ItemVOTVdrive.data(te.getItem(0));
        if (data.x() != 1002 || data.z() != -618) errors.add("coordinates " + data.x() + ", " + data.z() + ", 1.7 1002, -618");
        if (data.processed()) errors.add("updateDriveCoords processed the drive");

        // control packet path: px/pz
        net.minecraft.nbt.CompoundTag control = new net.minecraft.nbt.CompoundTag();
        control.putInt("px", 0);
        control.putInt("pz", 255);
        te.receiveControl(control);
        data = ItemVOTVdrive.data(te.getItem(0));
        if (data.x() != 872 || data.z() != -373) errors.add("control coordinates " + data.x() + ", " + data.z() + ", 1.7 872, -373");

        // a drive without an approximate destination rolls one within the probe distance first
        ItemStack fresh = new ItemStack(RocketItems.drive(DriveBody.MUN));
        te.setItem(0, fresh);
        te.updateDriveCoords(128, 128);
        data = ItemVOTVdrive.data(te.getItem(0));
        if (data.ax().isEmpty() || Math.abs(data.ax().get()) > Orbit.MAX_PROBE_DISTANCE) errors.add("no approximate destination rolled");
        else if (data.x() != data.ax().get() || data.z() != data.az().get()) errors.add("centre pixel is not the approximate destination");

        MachineGameTests.finish(helper, errors);
    }

    /** The background height-map scan: 16 x 16 chunks, each column the level's MOTION_BLOCKING height. */
    private static void heightmap(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        int chunkX = origin.getX() >> 4, chunkZ = origin.getZ() >> 4;
        TileEntityMachineStardar.HeightmapScan scan = new TileEntityMachineStardar.HeightmapScan(helper.getLevel(), chunkX, chunkZ);
        boolean[] finished = {false};
        helper.succeedWhen(() -> {
            if (!finished[0]) finished[0] = scan.tick();
            if (!finished[0]) helper.fail("height map not complete");
            // map pixel (8 * 16 + x & 15, 8 * 16 + z & 15) is the origin's column
            int px = 128 + (origin.getX() & 15), pz = 128 + (origin.getZ() & 15);
            int expected = helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, origin.getX(), origin.getZ());
            if (scan.map[pz * 256 + px] != expected) helper.fail("height " + scan.map[pz * 256 + px] + ", level " + expected);
            int cornerX = (chunkX - 8) * 16, cornerZ = (chunkZ - 8) * 16;
            // read from the chunk itself: below 26.1 Level.getHeight answers the minimum height for a chunk that is
            // not loaded (the scan released its claim on the far corner), 26.1 loads it
            int corner = helper.getLevel().getChunk(cornerX >> 4, cornerZ >> 4)
                    .getHeight(Heightmap.Types.MOTION_BLOCKING, cornerX & 15, cornerZ & 15) + 1;
            if (scan.map[0] != corner) helper.fail("corner height " + scan.map[0] + ", level " + corner);
        });
    }

    /**
     * The reactor sensor through HBM's {@code ItemReactorSensor.USE} hook: used on a StarDar cell it stores that
     * position, used on the console it links the console to the StarDar's core; the link survives a save round trip.
     */
    private static void dishLink(GameTestHelper helper) {
        TileEntityMachineStardar stardar = placeStardar(helper);
        BlockPos core = stardar.getBlockPos();
        BlockPos consolePos = helper.absolutePos(GameTests.paddedCentre(4, 1).offset(-4, 0, -4));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), consolePos, SpaceMachinesD.DISH_CONTROL.get(), Direction.NORTH);
        if (!(helper.getLevel().getBlockEntity(consolePos) instanceof TileEntityDishControl console)) {
            helper.fail("no console block entity");
            return;
        }
        List<String> errors = new ArrayList<>();

        Player player = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        ItemStack sensor = new ItemStack(ModNuclearMiscItems.REACTOR_SENSOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, sensor);

        // a leg cell, not the core: the console finds the core itself
        BlockPos leg = core.offset(2, -2, 2);
        sensor.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(leg), Direction.UP, leg, false)));
        BlockPos stored = ItemReactorSensor.getTarget(player.getItemInHand(InteractionHand.MAIN_HAND));
        if (!leg.equals(stored)) errors.add("sensor stored " + stored + ", expected " + leg);

        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(consolePos), Direction.UP, consolePos, false)));
        if (!console.isLinked) errors.add("console not linked");
        if (!core.equals(console.linkPosition)) errors.add("console linked to " + console.linkPosition + ", core " + core);
        if (console.getLinkedDish() != stardar) errors.add("linked dish is not the StarDar");

        TileEntityDishControl copy = new TileEntityDishControl(console.getBlockPos(), console.getBlockState());
        GameTestHelpers.copyPersistentData(console, copy);
        if (!copy.isLinked || !core.equals(copy.linkPosition)) errors.add("link lost in the save: " + copy.isLinked + " " + copy.linkPosition);

        // a sensor that never saw a StarDar does not link
        TileEntityDishControl other = new TileEntityDishControl(console.getBlockPos(), console.getBlockState());
        other.setLevel(helper.getLevel());
        other.tryLink(new ItemStack(ModNuclearMiscItems.REACTOR_SENSOR.get()));
        if (other.isLinked) errors.add("an empty sensor linked");

        MachineGameTests.finish(helper, errors);
    }
}
