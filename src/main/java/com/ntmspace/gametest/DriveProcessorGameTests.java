package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModEntityItems;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.DriveProcessorBlocks;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.SpaceMachineItems;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The drive processor (hbm-space {@code MachineDriveProcessor}, {@code TileEntityMachineDriveProcessor}): processing
 * needs the circuit tier hbm-space's {@code CelestialBody.getProcessingLevel} asks from the machine's body (here Kerbin,
 * the overworld) and 100 ticks at 75% charge; cloning marks both drives copied; the save names its items.
 */
public final class DriveProcessorGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** Tier 0 refuses a Duna drive, tier 1 processes it in 100 ticks on a creative battery; two blocks, one core. */
    public static final GameTests.Function PROCESSES = TESTS.function("drive_processor_processes_with_tier", 300, 8,
            () -> DriveProcessorGameTests::processes);

    /** The hand-computed tier table from Kerbin, and the machine refusing one tier short for Eve and Laythe or below 75% charge. */
    public static final GameTests.Function TIERS = TESTS.function("drive_processor_tier_requirements", 40, 8,
            () -> DriveProcessorGameTests::tiers);

    /** Cloning needs an empty hard drive in slot 1, then both drives are marked copied and carry the same data. */
    public static final GameTests.Function CLONE = TESTS.function("drive_processor_clone_marks_copy", 40, 8,
            () -> DriveProcessorGameTests::cloneDrive);

    /** The save names its items and fields; it reads back whole. */
    public static final GameTests.Function SAVES = TESTS.function("drive_processor_saves_by_name", 40, 8,
            () -> DriveProcessorGameTests::saves);

    private DriveProcessorGameTests() {}

    public static void init() {}

    private static TileEntityMachineDriveProcessor place(GameTestHelper helper) {
        BlockPos relative = GameTests.paddedCentre(8, 2);
        BlockPos core = helper.absolutePos(relative);
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, DriveProcessorBlocks.MACHINE_DRIVE_PROCESSOR.get(), Direction.NORTH);
        return GameTestHelpers.blockEntity(helper, relative, TileEntityMachineDriveProcessor.class);
    }

    private static ItemStack drive(DriveBody body, int x, int z, boolean processed) {
        ItemStack stack = new ItemStack(RocketItems.drive(body));
        RocketItems.DRIVE.set(stack, DriveData.EMPTY.withCoordinates(x, z).withProcessed(processed));
        return stack;
    }

    private static ItemStack circuit(int tier) {
        return new ItemStack(SpaceMachineItems.circuit("PROCESST" + tier));
    }

    private static CompoundTag control(String key, boolean value) {
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, value);
        return data;
    }

    private static void processes(GameTestHelper helper) {
        TileEntityMachineDriveProcessor te = place(helper);
        BlockPos core = te.getBlockPos();

        // getDimensions {0, 0, 0, 0, 0, 1}: the core and one cell
        int[] cells = {0};
        MultiblockHandlerXR.visitBox(core, new int[] {0, 0, 0, 0, 0, 1}, Direction.NORTH, (pos, f) -> {
            if (core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), pos.immutable()))) cells[0]++;
        });
        helper.assertTrue(cells[0] == 1, "one cell besides the core, got " + cells[0]);

        te.setItem(3, new ItemStack(ModEntityItems.battery_creative.get()));
        te.setItem(0, drive(DriveBody.DUNA, 120, -40, false));
        te.power = te.maxPower;

        te.receiveControl(control("process", true));
        helper.assertTrue(!te.isProcessing, "no circuit (tier 0) does not process a Duna drive (tier 1 from Kerbin)");

        te.setItem(2, circuit(1));
        te.receiveControl(control("process", true));
        helper.assertTrue(te.isProcessing, "a tier 1 circuit processes a Duna drive");
        long start = helper.getTick();

        helper.succeedWhen(() -> {
            ItemStack stack = te.getItem(0);
            helper.assertTrue(ItemVOTVdrive.getProcessed(stack), "the drive is processed");
            long took = helper.getTick() - start;
            helper.assertTrue(took >= 99 && took <= 101, "processing takes 100 ticks, took " + took);
            helper.assertTrue(!te.isProcessing && te.progress == 0, "processing stopped");
            helper.assertTrue(te.status.contains("Done!"), "status Done!, got " + te.status);
            helper.assertTrue(te.hasDrive, "hasDrive for the renderer");
            helper.assertTrue(te.power >= te.maxPower * 0.75, "the creative battery keeps it charged");
            DriveData data = ItemVOTVdrive.data(stack);
            helper.assertTrue(data.x() == 120 && data.z() == -40 && !data.copied(), "coordinates kept, not copied");
        });
    }

    private static void tiers(GameTestHelper helper) {
        TileEntityMachineDriveProcessor te = place(helper);
        SolarSystem.Body kerbin = Orbit.bodyOf(helper.getLevel());
        helper.assertTrue(kerbin == SolarSystem.body("kerbin"), "the GameTest level is Kerbin");

        // hbm-space CelestialBody.getProcessingLevel from Kerbin: itself and its moons 0, the rest 1, raised to the
        // minimum (Eve, Dres 2; Laythe, Tekto 3); the orbit drive 0
        Object[][] expected = {
                {DriveBody.ORBIT, 0}, {DriveBody.KERBIN, 0}, {DriveBody.MUN, 0}, {DriveBody.MINMUS, 0},
                {DriveBody.DUNA, 1}, {DriveBody.IKE, 1}, {DriveBody.MOHO, 1}, {DriveBody.EVE, 2}, {DriveBody.DRES, 2},
                {DriveBody.LAYTHE, 3}, {DriveBody.TEKTO, 3},
        };
        for (Object[] row : expected) {
            DriveBody body = (DriveBody) row[0];
            int tier = ItemVOTVdrive.getProcessingTier(drive(body, 1, 1, false), kerbin);
            helper.assertTrue(tier == (int) row[1], body + " from Kerbin needs tier " + row[1] + ", got " + tier);
        }
        // and from Duna: its moon Ike 0, Kerbin and the Mun 1
        SolarSystem.Body duna = SolarSystem.body("duna");
        helper.assertTrue(DriveBody.IKE.getProcessingLevel(duna) == 0 && DriveBody.KERBIN.getProcessingLevel(duna) == 1
                && DriveBody.MUN.getProcessingLevel(duna) == 1, "from Duna: Ike 0, Kerbin 1, Mun 1");

        te.power = te.maxPower;
        int[][] cases = {{2, 1, 0}, {2, 2, 1}, {3, 2, 0}, {3, 3, 1}}; // {required, circuit, accepted}
        DriveBody[] bodies = {DriveBody.EVE, DriveBody.EVE, DriveBody.LAYTHE, DriveBody.LAYTHE};
        for (int i = 0; i < cases.length; i++) {
            te.isProcessing = false;
            te.setItem(0, drive(bodies[i], 5, 5, false));
            te.setItem(2, circuit(cases[i][1]));
            te.receiveControl(control("process", true));
            helper.assertTrue(te.isProcessing == (cases[i][2] == 1), bodies[i] + " with tier " + cases[i][1] + ": "
                    + (cases[i][2] == 1 ? "accepted" : "refused"));
        }

        // turning it off
        te.receiveControl(control("process", false));
        helper.assertTrue(!te.isProcessing, "process=false stops it");

        // an already processed drive is not processed again
        te.setItem(0, drive(DriveBody.LAYTHE, 5, 5, true));
        te.receiveControl(control("process", true));
        helper.assertTrue(!te.isProcessing, "a processed drive is left alone");

        // below 75% charge (1,500 HE) nothing starts
        te.setItem(0, drive(DriveBody.LAYTHE, 5, 5, false));
        te.power = 1_499;
        te.receiveControl(control("process", true));
        helper.assertTrue(!te.isProcessing, "1,499 HE is too little");
        te.power = 1_500;
        te.receiveControl(control("process", true));
        helper.assertTrue(te.isProcessing, "1,500 HE is enough");
        te.isProcessing = false;

        // a tier 2 circuit and a Laythe drive: the tick reports "Low tier"
        te.power = te.maxPower;
        te.setItem(3, new ItemStack(ModEntityItems.battery_creative.get()));
        te.setItem(2, circuit(2));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(te.status.contains("Low tier") && !te.isProcessing, "status Low tier, got " + te.status);
            helper.succeed();
        });
    }

    private static void cloneDrive(GameTestHelper helper) {
        TileEntityMachineDriveProcessor te = place(helper);
        te.setItem(0, drive(DriveBody.MUN, 5, 7, true));

        // no power: nothing
        te.power = 0;
        te.setItem(1, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.receiveControl(control("clone", true));
        helper.assertTrue(te.getItem(1).getItem() == RocketItems.HARD_DRIVE.get() && !ItemVOTVdrive.wasCopied(te.getItem(0)),
                "without charge nothing is cloned");

        // no target
        te.power = te.maxPower;
        te.setItem(1, ItemStack.EMPTY);
        te.receiveControl(control("clone", true));
        helper.assertTrue(te.getItem(1).isEmpty() && te.status.contains("No target"), "no target: " + te.status);
        helper.assertTrue(!ItemVOTVdrive.wasCopied(te.getItem(0)), "the drive is not marked without a target");

        // a full drive is not a target either (1.7 asks for hard_drive)
        te.setItem(1, drive(DriveBody.DUNA, 1, 1, false));
        te.receiveControl(control("clone", true));
        helper.assertTrue(te.getItem(1).getItem() == RocketItems.drive(DriveBody.DUNA), "a full drive is not overwritten");

        te.setItem(1, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.receiveControl(control("clone", true));
        ItemStack original = te.getItem(0);
        ItemStack copy = te.getItem(1);
        helper.assertTrue(copy.getItem() == RocketItems.drive(DriveBody.MUN) && copy.getCount() == 1, "slot 1 holds a Mun drive");
        helper.assertTrue(ItemVOTVdrive.wasCopied(original) && ItemVOTVdrive.wasCopied(copy), "both drives are marked copied");
        helper.assertTrue(ItemVOTVdrive.data(copy).equals(ItemVOTVdrive.data(original)) && ItemVOTVdrive.data(copy).x() == 5
                && ItemVOTVdrive.data(copy).z() == 7 && ItemVOTVdrive.getProcessed(copy), "the copy carries the same data");
        helper.assertTrue(copy != original, "the copy is its own stack");
        helper.assertTrue(te.status.contains("Drive cloned"), "status Drive cloned, got " + te.status);
        helper.succeed();
    }

    private static void saves(GameTestHelper helper) {
        TileEntityMachineDriveProcessor te = place(helper);
        te.setItem(0, drive(DriveBody.DUNA, 33, 44, false));
        te.setItem(1, new ItemStack(RocketItems.HARD_DRIVE.get()));
        te.setItem(2, circuit(1));
        te.power = 1_800;
        te.isProcessing = true;
        te.progress = 42;
        te.status = "Processing  ";

        CompoundTag saved = BlockEntities.saveWithoutMetadata(te, helper.getLevel().registryAccess());
        String text = saved.toString();
        helper.assertTrue(text.contains("ntmspace:hard_drive_full_duna") && text.contains("ntmspace:hard_drive")
                && text.contains("ntmspace:circuit_processt1"), "items are saved by id: " + text);
        helper.assertTrue(text.contains("ntmspace:drive") && !text.contains("Damage") && !text.contains("dimension"),
                "the drive's data is its named component: " + text);

        TileEntityMachineDriveProcessor copy = new TileEntityMachineDriveProcessor(te.getBlockPos(), te.getBlockState());
        GameTestHelpers.copyPersistentData(te, copy);
        helper.assertTrue(copy.power == 1_800 && copy.isProcessing && copy.progress == 42 && copy.status.equals("Processing  "),
                "fields read back");
        helper.assertTrue(copy.getItem(0).getItem() == RocketItems.drive(DriveBody.DUNA)
                && ItemVOTVdrive.data(copy.getItem(0)).x() == 33 && ItemVOTVdrive.data(copy.getItem(0)).z() == 44
                && copy.getProcessingTier() == 1, "drive and circuit read back");
        helper.succeed();
    }
}
