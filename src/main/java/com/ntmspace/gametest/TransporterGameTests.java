package com.ntmspace.gametest;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.ntmspace.block.transporter.BlockTransporterRocket;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.item.ItemTransporterLinker.TransporterInfo;
import com.ntmspace.registry.TransporterBlocks;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The cargo transporter (hbm-space {@code BlockTransporterRocket}, {@code TileEntityTransporterRocket},
 * {@code ItemTransporterLinker}): the folded structure and its duct faces, linking two pads (the linker's saved list
 * and the pair link it makes), one shipment of items and fluid with the fuel cost of hbm-space's formula, and saves by
 * name.
 */
public final class TransporterGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** The three 1.7 fillSpace boxes: 17 cells around the core; the six other cells of getDimensions stay air. */
    public static final GameTests.Function STRUCTURE = TESTS.function("transporter_structure", 40, 8,
            () -> TransporterGameTests::structure);

    /**
     * 1.7's two subscriptions: hydrogen and oxygen reaching the cargo faces fill the cargo tanks that hold them, never
     * the fuel tanks (the pad's own receiving tanks are the cargo input tanks).
     */
    public static final GameTests.Function CARGO_FACES = TESTS.function("transporter_cargo_faces_skip_fuel", 100, 8,
            () -> TransporterGameTests::cargoFaces);

    /** The fuel faces reach the separate fuel receiver: hydrogen and oxygen fill the fuel tanks, not the cargo tanks. */
    public static final GameTests.Function FUEL_FACES = TESTS.function("transporter_fuel_faces_fill_fuel", 100, 8,
            () -> TransporterGameTests::fuelFaces);

    /** The linker saves both pads by level id; a link is made and broken for both pads at once. */
    public static final GameTests.Function LINK = TESTS.function("transporter_link_with_linker", 40, 8,
            () -> TransporterGameTests::link);

    /** One shipment: 10 ingots and 5,000 mB of water arrive; 800 mB of hydrogen and of oxygen are burned. */
    public static final GameTests.Function TRANSFER = TESTS.function("transporter_transfer_items_and_fluid", 100, 8,
            () -> TransporterGameTests::transfer);

    /** The pad's save names its link's level and its fluids; it reads back whole. */
    public static final GameTests.Function SAVES = TESTS.function("transporter_saves_by_name", 40, 8,
            () -> TransporterGameTests::saves);

    private TransporterGameTests() {}

    public static void init() {}

    private static TileEntityTransporterRocket place(GameTestHelper helper, BlockPos relative, Direction facing) {
        BlockPos core = helper.absolutePos(relative);
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, TransporterBlocks.TRANSPORTER_ROCKET.get(), facing);
        return GameTestHelpers.blockEntity(helper, relative, TileEntityTransporterRocket.class);
    }

    private static void structure(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(8, 2);
        TileEntityTransporterRocket pad = place(helper, centre, Direction.NORTH);
        BlockPos core = pad.getBlockPos();
        helper.assertTrue(pad.getContainerSize() == 22 && pad.tanks.length == 10, "22 slots and 10 tanks");
        helper.assertTrue(pad.tanks[0].getMaxFill() == 128_000 && pad.tanks[7].getMaxFill() == 128_000
                        && pad.tanks[8].getTankType() == Fluids.HYDROGEN && pad.tanks[8].getMaxFill() == 64_000
                        && pad.tanks[9].getTankType() == Fluids.OXYGEN && pad.tanks[9].getMaxFill() == 64_000,
                "cargo tanks 128,000 mB, fuel hydrogen and oxygen 64,000 mB");

        // the dims box {1, 0, 1, 1, 1, 2}: 24 cells; the three fillSpace boxes claim 18 of them (core included)
        int[] claimed = {0};
        int[] air = {0};
        MultiblockHandlerXR.visitBox(core, new int[] {1, 0, 1, 1, 1, 2}, Direction.NORTH, (pos, facingCore) -> {
            BlockPos at = pos.immutable();
            if (core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), at))) claimed[0]++;
            else if (helper.getLevel().getBlockState(at).isAir()) air[0]++;
        });
        helper.assertTrue(claimed[0] == 17 && air[0] == 6, "17 cells and 6 air in the dims box, got " + claimed[0] + " / " + air[0]);

        // ducts: a fuel face reaches the fuel receiver, a cargo face the pad itself (IFluidMultiUserMK2)
        BlockTransporterRocket block = TransporterBlocks.TRANSPORTER_ROCKET.get();
        ForgeDirection dir = ForgeDirection.NORTH;
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos fuelCell = core.offset(dir.offsetX - rot.offsetX * 2, 0, dir.offsetZ - rot.offsetZ * 2);
        BlockPos fuelPoint = core.offset(dir.offsetX - rot.offsetX * 3, 0, dir.offsetZ - rot.offsetZ * 3);
        helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), fuelCell)), "the fuel cell belongs to the pad");
        helper.assertTrue(helper.getLevel().getBlockState(fuelPoint).isAir(), "the fuel point is outside the pad");
        helper.assertTrue(block.canConnect(Fluids.HYDROGEN, helper.getLevel(), fuelCell.getX(), fuelCell.getY(), fuelCell.getZ(), rot.getOpposite()),
                "the fuel face takes hydrogen");
        helper.assertTrue(pad.fluidUserAt(fuelCell, rot.getOpposite().toDirection()) == pad.fuelReceiver(),
                "the fuel face reaches the fuel receiver");
        BlockPos cargoCell = core.offset(dir.offsetX - rot.offsetX, 0, dir.offsetZ - rot.offsetZ);
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), cargoCell.getX(), cargoCell.getY(), cargoCell.getZ(), dir),
                "a cargo face takes water");
        helper.assertTrue(pad.fluidUserAt(cargoCell, dir.toDirection()) == null && pad.fluidUserAt(cargoCell, Direction.UP) == null,
                "a cargo face reaches the pad itself");
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), cargoCell.getX(), cargoCell.getY(), cargoCell.getZ(), ForgeDirection.UP),
                "the floor corner opens upwards too (1.7 getConPos y + 1)");
        helper.succeed();
    }

    private static void cargoFaces(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(8, 2);
        TileEntityTransporterRocket pad = place(helper, centre, Direction.NORTH);
        pad.tanks[0].setTankType(Fluids.HYDROGEN);
        pad.tanks[1].setTankType(Fluids.OXYGEN);
        DirPos[] cargo = TileEntityTransporterRocket.conPos(pad.getBlockPos(), ForgeDirection.NORTH);
        // conPos[0] and [2]: the points in front of and behind the pad, on opposite ends
        feed(helper, cargo[0], Fluids.HYDROGEN);
        feed(helper, cargo[2], Fluids.OXYGEN);

        helper.succeedWhen(() -> {
            helper.assertTrue(pad.tanks[0].getFill() > 0 && pad.tanks[1].getFill() > 0,
                    "the cargo tanks fill: " + pad.tanks[0].getFill() + " / " + pad.tanks[1].getFill());
            helper.assertTrue(pad.tanks[8].getFill() == 0 && pad.tanks[9].getFill() == 0,
                    "the fuel tanks stay empty: " + pad.tanks[8].getFill() + " / " + pad.tanks[9].getFill());
        });
    }

    private static void fuelFaces(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(8, 2);
        TileEntityTransporterRocket pad = place(helper, centre, Direction.NORTH);
        // cargo tanks that could take the fuel: 1.7 still kept them off the fuel points
        pad.tanks[0].setTankType(Fluids.HYDROGEN);
        pad.tanks[1].setTankType(Fluids.OXYGEN);
        DirPos[] fuel = TileEntityTransporterRocket.tankPos(pad.getBlockPos(), ForgeDirection.NORTH);
        feed(helper, fuel[0], Fluids.HYDROGEN);
        feed(helper, fuel[1], Fluids.OXYGEN);

        helper.succeedWhen(() -> {
            helper.assertTrue(pad.tanks[8].getFill() > 0 && pad.tanks[9].getFill() > 0,
                    "the fuel tanks fill: " + pad.tanks[8].getFill() + " / " + pad.tanks[9].getFill());
            helper.assertTrue(pad.tanks[0].getFill() == 0 && pad.tanks[1].getFill() == 0,
                    "the cargo tanks stay empty: " + pad.tanks[0].getFill() + " / " + pad.tanks[1].getFill());
        });
    }

    /** A duct of {@code type} on a 1.7 connection point, fed by a full output-mode barrel one block further out. */
    private static void feed(GameTestHelper helper, DirPos point, FluidType type) {
        var level = helper.getLevel();
        BlockPos at = point.getPos();
        helper.assertTrue(level.getBlockState(at).isAir(), "the point " + at + " is outside the pad");
        level.setBlock(at, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(at)).setFluidType(type);
        ForgeDirection out = point.getDir().getOpposite();
        BlockPos source = at.offset(out.offsetX, out.offsetY, out.offsetZ);
        level.setBlock(source, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel barrel = (TileEntityBarrel) level.getBlockEntity(source);
        barrel.tank.setTankType(type);
        barrel.tank.setFill(barrel.tank.getMaxFill());
        barrel.mode = 2;
    }

    private static void link(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(8, 2);
        TileEntityTransporterRocket a = place(helper, centre.offset(-5, 0, 0), Direction.NORTH);
        TileEntityTransporterRocket b = place(helper, centre.offset(4, 0, 0), Direction.NORTH);
        b.receiveControl(name("Beta"));
        a.receiveControl(name("Alpha"));

        ItemStack linker = new ItemStack(TransporterBlocks.TRANSPORTER_LINKER.get());
        ItemTransporterLinker.addTransporter(linker, helper.getLevel(), a);
        ItemTransporterLinker.addTransporter(linker, helper.getLevel(), b);
        ItemTransporterLinker.addTransporter(linker, helper.getLevel(), a); // a set: no duplicate
        List<TransporterInfo> saved = ItemTransporterLinker.getTransporters(helper.getLevel().getServer(), linker);
        helper.assertTrue(saved.size() == 2 && saved.get(0).name.equals("Alpha") && saved.get(1).name.equals("Beta"),
                "the linker lists Alpha and Beta once each: " + saved.size());
        String dimension = ItemTransporterLinker.dimensionOf(helper.getLevel());
        helper.assertTrue(saved.get(0).dimension.equals(dimension) && saved.get(0).pos().equals(a.getBlockPos()),
                "saved by level id and position");
        helper.assertTrue(saved.get(0).planet.getPath().equals("textures/misc/space/kerbin.png"), "the overworld shows Kerbin");

        // the stack names the level, never a number
        var ops = net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        Tag tag = dev.golem.core.world.ItemStacks.optionalCodec().encodeStart(ops, linker).result().orElseThrow();
        helper.assertTrue(tag.toString().contains(dimension) && !tag.toString().contains("dimensionId"),
                "the linker saves the level by id: " + tag);

        // the screen's link click: Alpha -> Beta, and Beta learns it too
        a.receiveControl(linkTo(dimension, b.getBlockPos()));
        helper.assertTrue(a.getLinkedTransporter() != null && a.getLinkedTransporter().pos().equals(b.getBlockPos()), "Alpha links Beta");
        helper.assertTrue(b.getLinkedTransporter() != null && b.getLinkedTransporter().pos().equals(a.getBlockPos()), "Beta links Alpha");
        helper.assertTrue(a.hasRocket && !b.hasRocket, "the linking pad holds the rocket (1.7 hasConnected)");
        List<TransporterInfo> after = ItemTransporterLinker.getTransporters(helper.getLevel().getServer(), linker);
        helper.assertTrue(after.get(0).linkedTo != null && after.get(0).linkedTo.equals(after.get(1)), "the list shows the link");

        // unlink from Beta: both forget
        CompoundTag unlink = new CompoundTag();
        unlink.putBoolean("unlink", true);
        b.receiveControl(unlink);
        helper.assertTrue(a.getLinkedTransporter() == null && b.getLinkedTransporter() == null, "unlink clears both pads");
        helper.succeed();
    }

    private static void transfer(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(8, 2);
        TileEntityTransporterRocket a = place(helper, centre.offset(-5, 0, 0), Direction.NORTH);
        TileEntityTransporterRocket b = place(helper, centre.offset(4, 0, 0), Direction.NORTH);
        String dimension = ItemTransporterLinker.dimensionOf(helper.getLevel());
        a.receiveControl(linkTo(dimension, b.getBlockPos()));

        a.setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        a.tanks[0].setTankType(Fluids.WATER);
        a.tanks[0].setFill(5_000);
        a.tanks[8].setFill(64_000);
        a.tanks[9].setFill(64_000);
        // skip the wait: the rocket has sat 20 ticks on Alpha and has been gone 100 from Beta
        a.launchTicks = -20;
        b.launchTicks = 100;

        // Hand computation (hbm-space SolarSystem.getCostBetween, Kerbin -> Kerbin, neither in orbit, 1 atm both ends):
        // mass = (10 items + 5,000 mB / 1,000) * 100 = 1,500 kg; hydrogen FT_Rocket: ISP 380, thrust 700,000 N.
        // g = G M / r^2 = 6.6743015e-11 * 5.292e22 / 600,000^2 = 9.8114 m/s^2 (floats); orbital dV = sqrt(G M / r) = 2426.26;
        // TWR = 700,000 / (1,500 g) = 47.56; a = 46.56 g; t = 2426.26 / a = 5.31 s; losses = 2 g t = 104.22;
        // drag = ln(2) / 10 = 0.0693; launch = 2426.26 + 104.22 * 1.0693 = 2537.70; landing = 104.22 * 0.9307 = 96.99;
        // dV = 2634.68 m/s; mass fraction = 1 - exp(-dV / (380 * 9.81)) = 0.5069; total mass = 1,500 / 0.4931 = 3,041.6;
        // propellant = 1,541.6 kg, half each = 770.8 -> ceil(7.708) * 100 = 800 mB of each tank
        int expected = 800;
        FT_Rocket hydrogen = Fluids.HYDROGEN.getTrait(FT_Rocket.class);
        helper.assertTrue(hydrogen != null && hydrogen.getISP() == 380 && hydrogen.getThrust() == 700_000, "hydrogen is rocket fuel (380 s, 700 kN)");
        helper.assertTrue(a.sendCost(b, 15, hydrogen) == expected, "send cost for 15: " + a.sendCost(b, 15, hydrogen));
        // the 1.7 numbers the station tests use, through the same sum
        helper.assertTrue(SolarSystem.getCostBetween(SolarSystem.body("kerbin"), SolarSystem.body("kerbin"), 1500, 700_000, 380,
                false, false, 1.0, 1.0) == expected, "getCostBetween(1,500 kg) = 800");

        helper.succeedWhen(() -> {
            helper.assertTrue(!a.hasRocket && b.hasRocket, "the rocket has flown to Beta");
            helper.assertTrue(a.getItem(0).isEmpty(), "Alpha's input slot is empty");
            ItemStack arrived = b.getItem(8);
            helper.assertTrue(arrived.is(Items.IRON_INGOT) && arrived.getCount() == 10, "10 ingots in Beta's first output slot: " + arrived);
            helper.assertTrue(b.tanks[4].getTankType() == Fluids.WATER && b.tanks[4].getFill() == 5_000 && a.tanks[0].getFill() == 0,
                    "5,000 mB of water in Beta's first output tank");
            helper.assertTrue(a.tanks[8].getFill() == 64_000 - expected && a.tanks[9].getFill() == 64_000 - expected,
                    "800 mB of hydrogen and oxygen burned: " + a.tanks[8].getFill() + " / " + a.tanks[9].getFill());
        });
    }

    private static void saves(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(8, 2);
        TileEntityTransporterRocket a = place(helper, centre.offset(-5, 0, 0), Direction.NORTH);
        TileEntityTransporterRocket b = place(helper, centre.offset(4, 0, 0), Direction.NORTH);
        String dimension = ItemTransporterLinker.dimensionOf(helper.getLevel());
        a.receiveControl(name("Depot"));
        a.receiveControl(linkTo(dimension, b.getBlockPos()));
        CompoundTag threshold = new CompoundTag();
        threshold.putDouble("threshold", 4);
        a.receiveControl(threshold);
        a.tanks[1].setTankType(Fluids.OIL);
        a.tanks[1].setFill(1234);

        CompoundTag saved = BlockEntities.saveWithoutMetadata(a, helper.getLevel().registryAccess());
        String text = saved.toString();
        helper.assertTrue(text.contains(dimension) && !text.contains("dimensionId"), "the link's level is saved by id: " + text);
        helper.assertTrue(text.contains("hbm:oil") && text.contains("hbm:hydrogen") && text.contains("hbm:oxygen"),
                "tank fluids are saved by name: " + text);
        helper.assertTrue(a.getThreshold() == 8, "threshold 4 -> x8 (2^(n-1))");

        // read it back into a fresh pad
        TileEntityTransporterRocket copy = new TileEntityTransporterRocket(a.getBlockPos(), a.getBlockState());
        GameTestHelpers.copyPersistentData(a, copy);
        helper.assertTrue(copy.getTransporterName().equals("Depot") && copy.threshold == 4 && copy.hasRocket,
                "name, threshold and rocket read back");
        helper.assertTrue(copy.getLinkedTransporter() != null && copy.getLinkedTransporter().pos().equals(b.getBlockPos())
                && copy.getLinkedTransporter().dimension.equals(dimension), "the link reads back");
        helper.assertTrue(copy.tanks[1].getTankType() == Fluids.OIL && copy.tanks[1].getFill() == 1234
                && copy.tanks[8].getTankType() == Fluids.HYDROGEN, "tanks read back");
        helper.succeed();
    }

    private static CompoundTag name(String name) {
        CompoundTag data = new CompoundTag();
        data.putString("name", name);
        return data;
    }

    private static CompoundTag linkTo(String dimension, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putString("dimension", dimension);
        data.putIntArray("linkedTo", new int[] {pos.getX(), pos.getY(), pos.getZ()});
        return data;
    }
}
