package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterAnatmogenesis;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterTU;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.dyson.TileEntityDysonReceiver;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CBT_Dyson;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * GameTests of the Dyson swarm, each against hbm-space's own numbers:
 * <ul>
 *   <li>{@code dyson_energy_formula}: {@code getEnergyOutput} against a table printed by a verbatim copy of 1.7's
 *       method run in a plain JVM (scratchpad {@code dyson/Oracle.java});</li>
 *   <li>{@code dyson_swarm_decay}: {@code attenuate} - a swarm of 102,400 (chance 1) loses exactly one member a tick, an
 *       empty swarm none, and 10,240 members lose about a tenth of a member a tick;</li>
 *   <li>{@code dyson_launcher_launch}: chip, power and four members spin up for 132 ticks at 151,515 HE a tick, launch
 *       four members into the star's swarm and spin down for 68 ticks; overdrive 38/12 ticks at 526,315 HE; one member a
 *       tick is pulled from a chest nine blocks behind;</li>
 *   <li>{@code dyson_receiver_he}, {@code dyson_receiver_tu}, {@code dyson_receiver_anatmogenesis}: a receiver beam into
 *       each converter's intake gives the HE converter {@code getEnergyOutput(members) / consumers} as power (and back
 *       out of its rear port), the heat converter as heat, the terraformer {@code energy / 28,800,000} mB of its gas in
 *       the body's air (emitted, then captured back).</li>
 * </ul>
 */
public final class DysonGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function FORMULA = TESTS.function("dyson_energy_formula", 20, () -> DysonGameTests::formula);
    public static final GameTests.Function DECAY = TESTS.function("dyson_swarm_decay", 20, () -> DysonGameTests::decay);
    public static final GameTests.Function LAUNCH = TESTS.function("dyson_launcher_launch", 40, 12, () -> DysonGameTests::launch);
    public static final GameTests.Function HE = TESTS.function("dyson_receiver_he", 40, 24, () -> DysonGameTests::receiverHe);
    public static final GameTests.Function TU = TESTS.function("dyson_receiver_tu", 40, 24, () -> DysonGameTests::receiverTu);
    public static final GameTests.Function ANATMOGENESIS = TESTS.function("dyson_receiver_anatmogenesis", 40, 24,
            () -> DysonGameTests::receiverAnatmogenesis);

    /** {members, HE per tick} from the 1.7 oracle (verbatim getEnergyOutput in a plain JVM). */
    private static final long[][] ORACLE = {
            {0, 584L}, {1, 608L}, {2, 633L}, {4, 686L}, {64, 7094L}, {512, 2563117562L}, {1024, 7522635302327L},
            {2048, 4282016788862789L}, {4096, 38653933893607379L}, {10000, 46112336583769024L},
            {20000, 46116860170406278L}, {100000, 46116860184273881L},
    };

    private DysonGameTests() {}

    public static void init() {}

    private static void formula(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (long[] row : ORACLE) {
            long actual = TileEntityDysonReceiver.getEnergyOutput((int) row[0]);
            if (actual != row[1]) errors.add(row[0] + " members: " + actual + " != 1.7 " + row[1]);
        }
        MachineGameTests.finish(helper, errors);
    }

    private static void decay(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        CBT_Dyson full = CBT_Dyson.of(7, 102_400);
        if (!full.attenuate() || full.members(7) != 102_399) errors.add("102,400 members: chance 1, expected 102,399, got " + full.members(7));

        CBT_Dyson empty = CBT_Dyson.of(7, 0);
        if (empty.attenuate() || empty.members(7) != 0) errors.add("an empty swarm decayed");

        CBT_Dyson tenth = CBT_Dyson.of(7, 10_240);
        for (int i = 0; i < 1000; i++) tenth.attenuate();
        int lost = 10_240 - tenth.members(7);
        // expected ~95.5 (chance 0.1 shrinking with the swarm), sd ~9.5
        if (lost < 55 || lost > 140) errors.add("10,240 members over 1000 ticks lost " + lost + ", expected ~95");
        MachineGameTests.finish(helper, errors);
    }

    private static ItemStack chip(int freq) {
        ItemStack chip = new ItemStack(ModSatelliteItems.sat_chip.get());
        ISatChip.setFreqS(chip, freq);
        return chip;
    }

    private static <T extends BlockEntity> T core(GameTestHelper helper, BlockPos pos, Block block, Class<T> type) {
        Level level = helper.getLevel();
        level.setBlock(pos, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH), 3);
        BlockEntity te = level.getBlockEntity(pos);
        if (!type.isInstance(te)) throw new IllegalStateException("no " + type.getSimpleName() + " at " + pos + ": " + te);
        return type.cast(te);
    }

    private static void launch(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        int id = 31_001;
        CBT_Dyson.set(level, id, 0);

        BlockPos pos = helper.absolutePos(GameTests.paddedCentre(12, 2)).north(4);
        TileEntityDysonLauncher te = core(helper, pos, DysonContent.DYSON_LAUNCHER.get(), TileEntityDysonLauncher.class);
        te.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_CHIP, chip(id));
        te.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_MEMBERS, new ItemStack(DysonContent.SWARM_MEMBER.get(), 4));

        // facing north: the inventory is nine blocks behind, to the south
        BlockPos chestPos = pos.south(9);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        Container chest = (Container) level.getBlockEntity(chestPos);
        chest.setItem(3, new ItemStack(DysonContent.SWARM_MEMBER.get(), 2));

        // the network keeps the buffer full; each spin-up tick draws 20 MHE / 132 = 151,515 HE
        for (int i = 0; i < 132; i++) powered(te, level);
        if (CBT_Dyson.count(level, id) != 0) errors.add("launched before tick 133: " + CBT_Dyson.count(level, id));
        if (te.power != TileEntityDysonLauncher.MAX_POWER - 151_515L) errors.add("spin-up draw " + (TileEntityDysonLauncher.MAX_POWER - te.power) + " != 151,515");
        if (!te.isOperating || te.operatingTime != 132) errors.add("spin-up state: operating " + te.isOperating + ", time " + te.operatingTime);

        powered(te, level);
        if (CBT_Dyson.count(level, id) != 4) errors.add("after tick 133 the swarm holds " + CBT_Dyson.count(level, id) + ", expected 4");
        if (!te.isSpinningDown) errors.add("not spinning down after the launch");
        if (!te.inventory.getStackInSlot(TileEntityDysonLauncher.SLOT_MEMBERS).isEmpty()) errors.add("members left after launch");

        // spinning down 68 ticks; the chest refills slot 0 one member a tick meanwhile
        for (int i = 0; i < 68; i++) powered(te, level);
        if (!te.isSpinningDown) errors.add("spin-down ended before 69 ticks");
        if (te.inventory.getStackInSlot(TileEntityDysonLauncher.SLOT_MEMBERS).getCount() != 2) {
            errors.add("pulled " + te.inventory.getStackInSlot(TileEntityDysonLauncher.SLOT_MEMBERS).getCount() + " from the chest, expected 2");
        }
        powered(te, level);
        if (te.isSpinningDown) errors.add("still spinning down after 69 ticks");

        // without enough power for a tick the spin-up starts over (1.7 resets operatingTime)
        te.power = 151_514;
        te.step(level);
        if (te.isOperating || te.operatingTime != 0) errors.add("ran on 151,514 HE");

        // sunset overdrive: 38 up at 20 MHE / 38 = 526,315 HE a tick, 12 down
        te.sunsetOverdrive = true;
        for (int i = 0; i < 38; i++) powered(te, level);
        if (te.power != TileEntityDysonLauncher.MAX_POWER - 526_315L) errors.add("overdrive draw " + (TileEntityDysonLauncher.MAX_POWER - te.power) + " != 526,315");
        if (CBT_Dyson.count(level, id) != 4) errors.add("overdrive launched early");
        powered(te, level);
        if (CBT_Dyson.count(level, id) != 6) errors.add("overdrive launch: swarm " + CBT_Dyson.count(level, id) + ", expected 6");
        for (int i = 0; i < 12; i++) powered(te, level);
        if (!te.isSpinningDown) errors.add("overdrive spin-down shorter than 13 ticks");
        powered(te, level);
        if (te.isSpinningDown) errors.add("overdrive spin-down longer than 13 ticks");

        CBT_Dyson.set(level, id, 0);
        te.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_CHIP, ItemStack.EMPTY);
        MachineGameTests.finish(helper, errors);
    }

    private static void powered(TileEntityDysonLauncher te, Level level) {
        te.power = TileEntityDysonLauncher.MAX_POWER;
        te.step(level);
    }

    /**
     * A receiver core facing north at the centre (its beam runs south from nine blocks on) with {@code members} in swarm
     * {@code id}, and a converter placed so its intake is the beam's eleventh block; the beam runs on the next tick, once
     * the swarm has counted the receiver as its consumer.
     */
    private static void beam(GameTestHelper helper, int id, int members, Block converter, int converterOffset, Check check) {
        Level level = helper.getLevel();
        CBT_Dyson.set(level, id, members);
        BlockPos pos = helper.absolutePos(GameTests.paddedCentre(24, 2)).north(10);
        TileEntityDysonReceiver receiver = core(helper, pos, DysonContent.DYSON_RECEIVER.get(), TileEntityDysonReceiver.class);
        BlockPos converterCore = pos.south(converterOffset);
        MultiblockHandlerXR.placeAssembled(level, converterCore, (BlockMultiblockCore) converter, Direction.NORTH);
        BlockEntity te = level.getBlockEntity(converterCore);

        receiver.receive(level, id, true, false); // registers the consumer
        helper.runAfterDelay(1, () -> {
            List<String> errors = new ArrayList<>();
            receiver.receive(level, id, true, false);
            if (!receiver.isReceiving) errors.add("not receiving (members " + receiver.swarmCount + ", consumers " + receiver.swarmConsumers + ")");
            if (receiver.swarmConsumers != 1) errors.add("consumers " + receiver.swarmConsumers + ", expected 1");
            if (receiver.beamLength != 11) errors.add("beam " + receiver.beamLength + " long, expected 11 (the converter's intake)");
            if (MultiblockSurface.coreEntity(level, pos.south(11).above()) != te) errors.add("the beam blew up the converter's intake");
            long energy = TileEntityDysonReceiver.getEnergyOutput(receiver.swarmCount) / receiver.swarmConsumers;
            check.check(level, te, energy, receiver.swarmCount, errors);
            CBT_Dyson.set(level, id, 0);
            MachineGameTests.finish(helper, errors);
        });
    }

    @FunctionalInterface
    private interface Check {
        void check(Level level, BlockEntity converter, long energy, int members, List<String> errors);
    }

    private static void receiverHe(GameTestHelper helper) {
        beam(helper, 31_002, 4096, DysonContent.DYSON_CONVERTER_HE.get(), 15, (level, te, energy, members, errors) -> {
            TileEntityDysonConverterHE he = (TileEntityDysonConverterHE) te;
            if (he.power != energy) errors.add("HE converter holds " + he.power + ", expected " + energy + " (" + members + " members)");
            if (he.getMaxPower() != he.power) errors.add("HE converter's max power is not its power (1.7)");
            if (!he.provideEnergy(0, 0, 0, 5)) {
                if (he.power != energy) errors.add("a refused beam changed the HE buffer");
            } else {
                errors.add("the HE converter took a beam outside its intake");
            }
        });
    }

    private static void receiverTu(GameTestHelper helper) {
        beam(helper, 31_003, 64, DysonContent.DYSON_CONVERTER_TU.get(), 17, (level, te, energy, members, errors) -> {
            TileEntityDysonConverterTU tu = (TileEntityDysonConverterTU) te;
            if (tu.heatEnergy != energy) errors.add("TU converter heat " + tu.heatEnergy + ", expected " + energy);
            BlockPos core = tu.getBlockPos();
            if (!BlockMultiblockCore.vendsHeatAt(tu, core.above(1)) || !BlockMultiblockCore.vendsHeatAt(tu, core.above(2))
                    || BlockMultiblockCore.vendsHeatAt(tu, core.north())) {
                errors.add("heat cells are not the two above the core");
            }
            tu.provideEnergy(core.getX(), core.getY() + 1, core.getZ() - 6, Long.MAX_VALUE);
            if (tu.heatEnergy != Integer.MAX_VALUE) errors.add("TU heat does not saturate at Integer.MAX_VALUE: " + tu.heatEnergy);
        });
    }

    private static void receiverAnatmogenesis(GameTestHelper helper) {
        beam(helper, 31_004, 512, DysonContent.DYSON_CONVERTER_ANATMOGENESIS.get(), 16, (level, te, energy, members, errors) -> {
            TileEntityDysonConverterAnatmogenesis anat = (TileEntityDysonConverterAnatmogenesis) te;
            long volume = energy / TileEntityDysonConverterAnatmogenesis.HE_TO_MB;
            if (anat.fluid != Fluids.OXYGEN || !anat.isEmitting) errors.add("terraformer is not an oxygen emitter by default");
            if (anat.gasProduced != volume) errors.add("gas produced " + anat.gasProduced + " mB, expected " + volume);
            if (volume <= 0) errors.add("no gas for " + members + " members (energy " + energy + ")");

            // the beam emitted `volume` mB of oxygen into the body's air; capture it back through the converter
            double before = oxygen(level);
            BlockPos core = anat.getBlockPos();
            anat.isEmitting = false;
            anat.provideEnergy(core.getX(), core.getY() + 1, core.getZ() - 5, energy);
            double captured = before - oxygen(level);
            double expected = volume / CelestialTraits.MB_PER_ATM;
            if (Math.abs(captured - expected) > expected * 1.0E-3) errors.add("captured " + captured + " atm, expected " + expected);
            anat.isEmitting = true;
        });
    }

    private static double oxygen(Level level) {
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        return atmosphere == null ? 0 : atmosphere.getPressure(Fluids.OXYGEN);
    }
}
