package com.ntmspace.gametest;

import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.machine.DemonLamp;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.RadiationConfig;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.tileentity.machine.BlockEntityDecon;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityStorageDrum;
import com.hbm.util.ContaminationUtil;
import com.ntmspace.config.SpaceConfig;
import com.ntmspace.fork.neutron.NeutronActivation;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's neutron activation ({@link NeutronActivation}): off by default (hbm-space's {@code HAZ_01_disableNeutrons =
 * true}) and, with the switch on, hbm-space's numbers. Expected values are 1.7 float arithmetic on hbm-space's constants
 * ({@code HazardRegistry}, {@code HazardTypeNeutron}, {@code TileEntityReactorResearch}, {@code TileEntityDecon},
 * {@code TileEntityStorageDrum}, {@code EntityEffectHandler}); a carried hazard is applied once per HBM hazard interval
 * ({@code hazardRate} ticks), so a carried dose is 1.7's per-tick dose times {@code hazardRate}.
 *
 * <p>The switch is flipped inside one test callback and restored before it returns, so no other test ever sees it on.
 */
public final class NeutronGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function OFF = TESTS.function("fork_neutron_off_by_default", 20, () -> NeutronGameTests::off);
    public static final GameTests.Function CARRIED = TESTS.function("fork_neutron_carried", 20, () -> NeutronGameTests::carried);
    public static final GameTests.Function REACTOR = TESTS.function("fork_neutron_research_reactor", 200, () -> NeutronGameTests::reactor);
    public static final GameTests.Function CLEANUP = TESTS.function("fork_neutron_decay_decon_drum", 20, () -> NeutronGameTests::cleanup);

    private NeutronGameTests() {}

    public static void init() {}

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (!errors.isEmpty()) {
            String message = String.join("; ", errors);
            com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
            return;
        }
        helper.succeed();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElse(Items.AIR);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= 1e-6 * Math.max(1.0, Math.abs(b));
    }

    private static void expect(List<String> errors, String what, double actual, double expected) {
        if (!near(actual, expected)) errors.add(what + " = " + actual + ", hbm-space " + expected);
    }

    private static boolean mentions(List<Component> lines, String text) {
        for (Component line : lines) if (line.getString().contains(text) || line.toString().contains(text)) return true;
        return false;
    }

    private static List<Component> tooltip(ItemStack stack, Player player) {
        List<Component> lines = new ArrayList<>();
        HazardSystem.addHazardInfo(stack, player, lines);
        return lines;
    }

    /** One carried pass of the stack's hazards, as {@code HazardSystem}'s carried scan does it. */
    private static void carry(Player player, ItemStack stack) {
        HazardSystem.applyHazards(stack, player);
    }

    // ------------------------------------------------------------------------------------------------ off

    private static void off(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        if (!SpaceConfig.disableNeutron) errors.add("the switch must default to off (hbm-space disableNeutron = true)");
        boolean saved = SpaceConfig.disableNeutron;
        try {
            SpaceConfig.disableNeutron = true;
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            ItemStack source = new ItemStack(item("hbm:billet_po210be"));
            ItemStack stone = new ItemStack(Items.STONE, 4);
            player.getInventory().setItem(0, source);
            player.getInventory().setItem(1, stone);

            // the NEUTRON row exists (hbm-space registers it either way) but does nothing and shows nothing
            if (!near(HazardSystem.getHazardLevelFromStack(source, NeutronActivation.NEUTRON), 11.25)) {
                errors.add("billet_po210be NEUTRON " + HazardSystem.getHazardLevelFromStack(source, NeutronActivation.NEUTRON));
            }
            carry(player, source);
            NeutronActivation.irradiate(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), player, 50F);
            for (var listener : DemonLamp.TileEntityDemonLamp.IRRADIATED) {
                listener.irradiated(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), player, 1000F);
            }
            if (NeutronActivation.STACK.has(stone) || NeutronActivation.STACK.has(source)) errors.add("a stack was activated while off");
            if (NeutronActivation.getActivation(player) != 0F) errors.add("the player was activated while off");
            if (HbmLivingProps.getData(player).getNeutrons() != 0D) {
                errors.add("living activation stored while off");
            }
            if (mentions(tooltip(source, player), "trait.ntmspace.neutron")) errors.add("neutron tooltip shown while off");
            if (mentions(tooltip(stone, player), "trait.radioactive")) errors.add("plain stone shows radiation");

            // hbm-space printed "Player radioactivity: 0.0 RAD/s" with the system off too
            List<Component> geiger = new ArrayList<>();
            for (var listener : ContaminationUtil.GEIGER_LINES) listener.lines(player, geiger::add);
            if (!mentions(geiger, "geiger.ntmspace.playerAct") || !mentions(geiger, "0.0 RAD/s")) {
                errors.add("geiger line " + geiger);
            }
        } finally {
            SpaceConfig.disableNeutron = saved;
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ carried

    private static void carried(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        boolean saved = SpaceConfig.disableNeutron;
        try {
            SpaceConfig.disableNeutron = false;
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            ItemStack source = new ItemStack(item("hbm:billet_po210be"));
            ItemStack stone = new ItemStack(Items.STONE, 4);
            ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
            player.getInventory().setItem(0, source);
            player.getInventory().setItem(1, stone);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
            float radMod = (float) ContaminationUtil.calculateRadiationMod(player);

            // HazardRegistry: billet_po210be NEUTRON pobe / 10 * billet = 225 / 10 * 0.5
            float level = 75.0F * 3.0F / 10 * 0.5F;
            expect(errors, "billet_po210be NEUTRON", HazardSystem.getHazardLevelFromStack(source, NeutronActivation.NEUTRON), level);
            // OreDictManager frames: PU240 ingot pu240 / 120, nugget x0.1, block x10; CF252 billet cf252 / 5 x0.5
            expect(errors, "ingot_pu240 NEUTRON", HazardSystem.getHazardLevelFromStack(new ItemStack(item("hbm:ingot_pu240")),
                    NeutronActivation.NEUTRON), 7.5F / 120f);
            expect(errors, "block_pu240 NEUTRON", HazardSystem.getHazardLevelFromStack(new ItemStack(item("hbm:block_pu240")),
                    NeutronActivation.NEUTRON), 7.5F / 120f * 10.0F);
            expect(errors, "billet_cf252 NEUTRON", HazardSystem.getHazardLevelFromStack(new ItemStack(item("ntmspace:billet_cf252")),
                    NeutronActivation.NEUTRON), 15.3F / 5f * 0.5F);
            expect(errors, "demon_core_closed NEUTRON", HazardSystem.getHazardLevelFromStack(new ItemStack(item("hbm:demon_core_closed")),
                    NeutronActivation.NEUTRON), 10_000F);
            expect(errors, "billet_po210be RADIATION untouched", HazardSystem.getHazardLevelFromStack(source, HazardRegistry.RADIATION), 112.5);

            // HazardTypeNeutron.onUpdate: rad = level / 20 * radMod (per tick in 1.7, x hazardRate here); every main and
            // armour stack gets (rad / count) / 10
            float rad = (level * source.getCount() / 20F) * radMod * RadiationConfig.hazardRate;
            carry(player, source);
            expect(errors, "billet activation", NeutronActivation.stackActivation(source), (rad / 1) / 10);
            expect(errors, "stone x4 activation", NeutronActivation.stackActivation(stone), (rad / 4) / 10);
            expect(errors, "helmet activation", NeutronActivation.stackActivation(helmet), (rad / 1) / 10);
            // a mock player is under 200 ticks old: 1.7's contaminate leaves its own activation alone
            if (NeutronActivation.getActivation(player) != 0F) errors.add("a fresh player took living activation");

            // tooltip: [Neutron Radiation] / 11.25RAD/s^2
            List<Component> lines = tooltip(new ItemStack(item("hbm:billet_po210be")), player);
            if (!mentions(lines, "trait.ntmspace.neutron") || !mentions(lines, "11.25RAD/s^2")) errors.add("neutron tooltip " + lines);

            // Activation uses one ordinary per-item gamma entry, including the stack count.
            float a = NeutronActivation.stackActivation(stone);
            expect(errors, "stone RADIATION entry", HazardSystem.getHazardLevelFromStack(stone, HazardRegistry.RADIATION), a);
            String rate = String.valueOf(Math.floor(a * 1000) / 1000);
            if (!mentions(tooltip(stone, player), rate)) errors.add("activated stone tooltip lacks " + rate + ": " + tooltip(stone, player));
            double env = HbmLivingProps.getRadEnv(player);
            carry(player, stone);
            expect(errors, "stone radiation into the environment buffer", HbmLivingProps.getRadEnv(player) - env,
                    ((double) a * 4) / 20D * RadiationConfig.hazardRate);

            // geiger line: activation to one decimal, "RAD/s"
            NeutronActivation.setActivation(player, 12.34F);
            List<Component> geiger = new ArrayList<>();
            for (var listener : ContaminationUtil.GEIGER_LINES) listener.lines(player, geiger::add);
            if (!mentions(geiger, "12.3 RAD/s")) errors.add("geiger line " + geiger);
        } finally {
            SpaceConfig.disableNeutron = saved;
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ reactor

    private static float dose(LivingEntity e, BlockPos source, float rad) {
        double x = source.getX(), y = source.getY(), z = source.getZ();
        Vec3 vec = new Vec3(e.getX() - (x + 0.5), (e.getY() + e.getEyeHeight()) - (y + 0.5), e.getZ() - (z + 0.5));
        double len = vec.length();
        float eRads = rad; // the line runs through air: resistance 1
        eRads /= (float) (len * len);
        return eRads;
    }

    private static void reactor(GameTestHelper helper) {
        // the reactor dose reaches the pig only through the level's entity lookup: on 26.1 the pig at (6, 1, 2) can lie in
        // the chunk next to the test's own, which need not be entity-ticking, and the reactor then does not see it
        com.hbm.gametest.TestPlots.whenTicking(helper, new BlockPos(0, 0, 0), new BlockPos(7, 3, 3), () -> reactorNow(helper));
    }

    private static void reactorNow(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        boolean saved = SpaceConfig.disableNeutron;
        var level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos abs = helper.absolutePos(rel);
        try {
            SpaceConfig.disableNeutron = false;
            helper.setBlock(rel, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
            TileEntityReactorResearch reactor = (TileEntityReactorResearch) level.getBlockEntity(abs);
            var pig = helper.spawn(EntityType.PIG, new BlockPos(6, 1, 2));
            pig.setNoAi(true);
            reactor.rodLevel = 1.0;
            reactor.targetLevel = 1.0;
            reactor.heat = 10_000;
            TileEntityReactorResearch.serverTick(level, abs, level.getBlockState(abs), reactor);
            // 1.7: heat drops by 1 without water before the dose is taken; rad = heat / maxHeat * 50
            float rad = (float) reactor.heat / (float) reactor.maxHeat * 50F;
            if (reactor.heat != 9_999) errors.add("reactor heat " + reactor.heat);
            float expected = dose(pig, abs, rad) * (float) ContaminationUtil.calculateRadiationMod(pig);
            expect(errors, "pig activation by the running reactor", NeutronActivation.getActivation(pig), expected);

            // a player's inventory (the mock player is not in the level, so it gets the reactor's per-entity pass directly)
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            Vec3 c = Vec3.atCenterOf(abs);
            player.setPos(c.x, c.y - player.getEyeHeight(), c.z + 4); // eyes level with the core, four blocks south
            ItemStack stone = new ItemStack(Items.STONE, 4);
            player.getInventory().setItem(3, stone);
            NeutronActivation.irradiate(level, abs, player, 50F);
            expect(errors, "stone x4 four blocks from the core", NeutronActivation.stackActivation(stone), (50F / 16F / 4) / 10);

            // switched off, the same reactor tick leaves the pig alone
            SpaceConfig.disableNeutron = true;
            double before = HbmLivingProps.getData(pig).getNeutrons();
            reactor.heat = 10_000;
            TileEntityReactorResearch.serverTick(level, abs, level.getBlockState(abs), reactor);
            if (HbmLivingProps.getData(pig).getNeutrons() != before) errors.add("the reactor activated the pig while off");
        } finally {
            SpaceConfig.disableNeutron = saved;
            // stop the reactor before the plot keeps ticking it
            if (level.getBlockEntity(abs) instanceof TileEntityReactorResearch r) {
                r.heat = 0;
                r.rodLevel = 0;
                r.targetLevel = 0;
            }
            level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ decay

    private static void cleanup(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        boolean saved = SpaceConfig.disableNeutron;
        var level = helper.getLevel();
        try {
            SpaceConfig.disableNeutron = false;
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

            // EntityEffectHandler: activation * 0.998816 per tick
            NeutronActivation.setActivation(player, 1.0F);
            NeutronActivation.livingTick(player);
            expect(errors, "living decay", NeutronActivation.getActivation(player), 1.0F * 0.998816f);

            // TileEntityDecon.deconNeutron: entity and carried stacks * 0.899916, through HBM's decontaminator hook
            ItemStack stone = new ItemStack(Items.STONE);
            NeutronActivation.STACK.set(stone, 2.0F);
            player.getInventory().setItem(0, stone);
            NeutronActivation.setActivation(player, 1.0F);
            for (var listener : BlockEntityDecon.DECONTAMINATED) listener.decontaminated(level, helper.absolutePos(BlockPos.ZERO), List.of(player));
            expect(errors, "decon living", NeutronActivation.getActivation(player), 1.0F * 0.899916F);
            expect(errors, "decon stack", NeutronActivation.stackActivation(stone), 2.0F * 0.899916F);

            // The source cleanup floor applies after multiplication.
            ItemStack faint = new ItemStack(Items.STONE);
            NeutronActivation.STACK.set(faint, 2e-5F);
            NeutronActivation.decay(faint, 0.1F);
            if (NeutronActivation.STACK.has(faint)) errors.add("decay kept a value below 1e-5");

            // TileEntityStorageDrum: every stored stack * 0.9899916 per tick, through HBM's drum hook
            BlockPos drumRel = new BlockPos(1, 1, 1);
            helper.setBlock(drumRel, com.hbm.blocks.machine.ModMachineBlocks.machine_storage_drum.get().defaultBlockState());
            BlockPos drumAbs = helper.absolutePos(drumRel);
            TileEntityStorageDrum drum = (TileEntityStorageDrum) level.getBlockEntity(drumAbs);
            ItemStack stored = new ItemStack(Items.STONE);
            NeutronActivation.STACK.set(stored, 1.0F);
            drum.inventory.setStackInSlot(0, stored);
            TileEntityStorageDrum.serverTick(level, drumAbs, level.getBlockState(drumAbs), drum);
            expect(errors, "drum decay", NeutronActivation.stackActivation(drum.inventory.getStackInSlot(0)), 1.0F * 0.9899916F);
        } finally {
            SpaceConfig.disableNeutron = saved;
        }
        finish(helper, errors);
    }
}
