package com.ntmspace.gametest;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.api.data.MachineDataMaps;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.IHazardType;
import com.ntmspace.fork.BombPlacementLog;
import com.ntmspace.fork.ForkContent;
import com.ntmspace.fork.ForkEffects;
import com.ntmspace.fork.Nitan;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemUses;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's non-space fork content (package {@code com.ntmspace.fork} and its data rows) against the hbm-space source:
 * expected values are written from {@code HazardRegistry} / {@code OreDictManager}, {@code ItemFlask}, {@code HbmPotion},
 * {@code ItemLemon}, {@code BobmazonOfferFactory}, {@code TileEntityNukeCustom}, {@code FluidContainerRegistry} and
 * {@code FuelHandler}.
 */
public final class ForkGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function HAZARDS = TESTS.function("fork_isotope_hazards", 20, () -> ForkGameTests::hazards);
    public static final GameTests.Function NITAN = TESTS.function("fork_nitan_flask", 20, () -> ForkGameTests::nitan);
    public static final GameTests.Function EFFECTS = TESTS.function("fork_effects", 20, () -> ForkGameTests::effects);
    public static final GameTests.Function BOBMAZON = TESTS.function("fork_bobmazon_offer", 20, () -> ForkGameTests::bobmazon);
    public static final GameTests.Function DATA = TESTS.function("fork_data_rows", 20, () -> ForkGameTests::data);
    public static final GameTests.Function BOMBPL = TESTS.function("fork_bomb_placement_log", 20, () -> ForkGameTests::bombpl);
    public static final GameTests.Function DEPOSITS = TESTS.function("fork_fluid_deposits", 40, () -> ForkGameTests::deposits);
    public static final GameTests.Function DAMAGED = TESTS.function("fork_damaged_machines", 40, () -> ForkGameTests::damaged);
    public static final GameTests.Function SALTED = TESTS.function("fork_salted_charge", 20, () -> ForkGameTests::salted);
    public static final GameTests.Function TWEAKS = TESTS.function("fork_machine_tweaks", 20, () -> ForkGameTests::tweaks);

    private ForkGameTests() {}

    public static void init() {
        ForkGameTests2.init(); // part 2 of the fork content
        ForkGameTests3.init(); // part 3: small base-HBM changes
        NeutronGameTests.init(); // neutron activation
    }

    // ------------------------------------------------------------------------------------------------ hazards

    private static void hazards(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // OreDictManager: X.rad(r).hot(h) on nugget (0.1) / billet (0.5) / ingot (1) / dust (3) / plate (1)
        level(errors, "ntmspace:nugget_cm242", HazardRegistry.RADIATION, 9.3F * 0.1F);
        level(errors, "ntmspace:billet_cm243", HazardRegistry.RADIATION, 5.6F * 0.5F);
        level(errors, "ntmspace:ingot_cm244", HazardRegistry.RADIATION, 2.0F);
        level(errors, "ntmspace:ingot_cm_mix", HazardRegistry.RADIATION, 6.0F);
        level(errors, "ntmspace:ingot_cm_fuel", HazardRegistry.RADIATION, 2.2F);
        level(errors, "ntmspace:ingot_bk247", HazardRegistry.RADIATION, 10.5F);
        level(errors, "ntmspace:ingot_cf251", HazardRegistry.RADIATION, 14.3F);
        level(errors, "ntmspace:ingot_cf251", HazardRegistry.HOT, 2.0F);
        level(errors, "ntmspace:ingot_cf252", HazardRegistry.RADIATION, 15.3F);
        level(errors, "ntmspace:ingot_cf252", HazardRegistry.HOT, 3.0F);
        level(errors, "ntmspace:billet_es253", HazardRegistry.RADIATION, 18.3F * 0.5F);
        level(errors, "ntmspace:billet_es253", HazardRegistry.HOT, 4.0F * 0.5F);
        level(errors, "ntmspace:ingot_es255", HazardRegistry.RADIATION, 19.3F);
        level(errors, "ntmspace:ingot_es255", HazardRegistry.HOT, 0F);
        level(errors, "ntmspace:powder_cn989", HazardRegistry.RADIATION, 89.0F * 3F);
        level(errors, "ntmspace:plate_cn989", HazardRegistry.HOT, 4.0F);
        // HazardRegistry.registerItem rows
        level(errors, "ntmspace:ammonium_nitrate", HazardRegistry.EXPLOSIVE, 4.0F);
        level(errors, "ntmspace:powder_wd2004", HazardRegistry.DIGAMMA, 1.0F);
        level(errors, "ntmspace:powder_wd2004_tiny", HazardRegistry.DIGAMMA, 0.05F);
        finish(helper, errors);
    }

    private static void level(List<String> errors, String id, IHazardType type, float expected) {
        ItemStack stack = stack(id);
        if (stack == null) {
            errors.add(id + " is not registered");
            return;
        }
        double level = HazardSystem.getHazardLevelFromStack(stack, type);
        if (Math.abs(level - expected) > 1e-4) errors.add(id + " " + type + " = " + level + ", hbm-space " + expected);
    }

    // ------------------------------------------------------------------------------------------------ nitan

    private static void nitan(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack flask = new ItemStack(ForkContent.FLASK_INFUSION_NITAN.get(), 2);
        flask.finishUsingItem(helper.getLevel(), player);
        if (flask.getCount() != 1) errors.add("drinking used " + (2 - flask.getCount()) + " flasks, 1.7 used 1");
        if (Nitan.count(player) != 1) errors.add("NITAN count " + Nitan.count(player) + " after one flask, expected 1");
        MobEffectInstance nitan = Effects.getRegistered(player, ForkContent.NITAN_EFFECT.get());
        if (nitan == null || nitan.getDuration() != 3600 || nitan.getAmplifier() != 0) errors.add("nitan effect " + nitan);
        MobEffectInstance resistance = Effects.get(player, Effects.resistance());
        if (resistance == null || resistance.getDuration() != 3600 || resistance.getAmplifier() != 3) {
            errors.add("resistance " + resistance + ", 1.7 gave resistance IV (amplifier 3) for 3600 ticks");
        }

        // a count of 2 is harmless, exactly 3 kills
        Nitan.setCount(player, 2);
        float health = player.getHealth();
        ForkEffects.Nitan.tick(player);
        if (player.getHealth() != health) errors.add("nitan hurt at count 2");
        Nitan.setCount(player, 3);
        ForkEffects.Nitan.tick(player);
        if (!player.isDeadOrDying()) errors.add("nitan at count 3 did not kill (health " + player.getHealth() + ")");

        // ipecac clears nitan and the count; a plain food does not
        Player other = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        Nitan.setCount(other, 2);
        Effects.addRegistered(other, ForkContent.NITAN_EFFECT.get(), 100, 0);
        ItemUses.finished(other, new ItemStack(Items.BREAD));
        if (Nitan.count(other) != 2) errors.add("bread reset the NITAN count");
        ItemUses.finished(other, stack("hbm:med_ipecac"));
        if (Nitan.count(other) != 0 || Effects.hasRegistered(other, ForkContent.NITAN_EFFECT.get())) {
            errors.add("ipecac did not clear nitan (count " + Nitan.count(other) + ")");
        }
        if (!Nitan.clearsNitan(stack("hbm:med_ptsd"))) errors.add("PTSD medication does not clear nitan");
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ effects

    private static void effects(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // HbmPotion colours and "bad effect" flags (all three: not bad)
        check(errors, "nitan", ForkContent.NITAN_EFFECT.get().getColor(), 8388736);
        check(errors, "flashbang", ForkContent.FLASHBANG_EFFECT.get().getColor(), 0xD0D0D0);
        check(errors, "slippery", ForkContent.SLIPPERY_EFFECT.get().getColor(), 0xD0D0D0);
        if (ForkContent.NITAN_EFFECT.get().getCategory() != net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) errors.add("nitan is not beneficial (1.7 isBadEffect false)");
        finish(helper, errors);
    }

    private static void check(List<String> errors, String what, int actual, int expected) {
        if (actual != expected) errors.add(what + " colour " + Integer.toHexString(actual) + ", hbm-space " + Integer.toHexString(expected));
    }

    // ------------------------------------------------------------------------------------------------ bobmazon

    private static void bobmazon(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        List<BobmazonOfferFactory.Offer> special = BobmazonOfferFactory.special();
        int index = -1;
        for (int i = 0; i < special.size(); i++) {
            if (id(special.get(i).offer).equals("hbm:pellet_charged")) index = i;
        }
        if (index < 0) {
            errors.add("no pellet_charged special offer");
        } else {
            BobmazonOfferFactory.Offer offer = special.get(index);
            if (offer.cost != 16 || offer.requirement != BobmazonOfferFactory.Requirement.STEEL) {
                errors.add("pellet_charged offer cost " + offer.cost + " / " + offer.requirement + ", hbm-space 16 / STEEL");
            }
            if (index == 0 || !id(special.get(index - 1).offer).equals("hbm:crucible")) {
                errors.add("pellet_charged is not right after the crucible (1.7 order)");
            }
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ data rows

    private static void data(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // TileEntityNukeCustom: particle_amat 5, pellet_antimatter 45 AMAT; the balefire eggs are no AMAT entries
        nuke(errors, "hbm:particle_amat", 5F);
        nuke(errors, "hbm:pellet_antimatter", 45F);
        for (String egg : new String[] {"hbm:egg_balefire", "hbm:egg_balefire_shard"}) {
            if (MachineDataMaps.CUSTOM_NUKE.get(item(egg)) != null) errors.add(egg + " is still a custom nuke entry");
        }
        // FluidContainerRegistry: the drinks and the dry-ice block
        container(errors, "ntmspace:glass_smilk", "ntmspace:smilk", 100, "ntmspace:glass_empty");
        container(errors, "hbm:coffee", "ntmspace:coffee", 100, "ntmspace:cmug_empty");
        container(errors, "ntmspace:teacup", "ntmspace:tea", 100, "ntmspace:teacup_empty");
        container(errors, "ntmspace:bottle_honey", "ntmspace:honey", 100, "minecraft:glass_bottle");
        container(errors, "ntmspace:dry_ice", "hbm:carbondioxide", 1000, null);
        // FuelHandler: powder_coke = single (200) * 16
        for (String coke : new String[] {"powder_coke_coal", "powder_coke_lignite", "powder_coke_petroleum"}) {
            int burn = Compat.burnDuration(helper.getLevel(), stack("ntmspace:" + coke));
            if (burn != 3200) errors.add(coke + " burns " + burn + " ticks, hbm-space 3200");
        }
        finish(helper, errors);
    }

    private static void nuke(List<String> errors, String id, float value) {
        MachineDataMaps.NukeEntry entry = MachineDataMaps.CUSTOM_NUKE.get(item(id));
        if (entry == null || !entry.type().name().equals("AMAT") || entry.value() != value) {
            errors.add(id + " custom nuke entry " + entry + ", hbm-space AMAT " + value);
        }
    }

    private static void container(List<String> errors, String id, String fluid, int amount, String empty) {
        FluidDataMaps.Container c = FluidDataMaps.FLUID_CONTAINER.get(item(id));
        if (c == null || !c.fluid().equals(fluid) || c.amount() != amount || !c.empty().equals(java.util.Optional.ofNullable(empty))) {
            errors.add(id + " container " + c + ", hbm-space " + fluid + " " + amount + " -> " + empty);
        }
    }

    // ------------------------------------------------------------------------------------------------ [BOMBPL]

    private static void bombpl(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (String id : new String[] {"hbm:nuke_boy", "hbm:nuke_tsar", "hbm:det_nuke", "hbm:det_charge", "hbm:semtex",
                "hbm:mine_ap", "hbm:nuke_fstbmb"}) {
            Block block = block(id);
            if (block == Blocks.AIR) errors.add(id + " is not registered");
            else if (!BombPlacementLog.logged(block)) errors.add(id + " would not be logged");
        }
        for (String id : new String[] {"minecraft:tnt", "hbm:machine_press"}) {
            if (BombPlacementLog.logged(block(id))) errors.add(id + " would be logged (not one of hbm-space's bomb classes)");
        }
        String message = BombPlacementLog.message(block("hbm:nuke_boy"), new BlockPos(1, 2, 3), null);
        if (!message.startsWith("[BOMBPL]") || !message.endsWith(" placed at 1 / 2 / 3! by null")) {
            errors.add("message \"" + message + "\"");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ fluid ores

    /**
     * hbm-space {@code BlockOreFluid} through HBM's derrick and pumpjack: per-body fluids and amounts (WorldConfig
     * defaults), and a drained ore keeps its body.
     */
    private static void deposits(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        // body -> block, primary fluid, primary amount (derrick), secondary fluid, gas min, gas max
        Object[][] rows = {
                {"duna", "ntmspace:ore_oil", "hbm:oil", 200, "hbm:gas", 100, 500},
                {"laythe", "ntmspace:ore_oil", "hbm:oil_ds", 500, "hbm:gas", 100, 500},
                {"tekto", "ntmspace:ore_tekto", "ntmspace:tcrude", 500, "ntmspace:hgas", 100, 500},
                {"eve", "ntmspace:ore_gas", "hbm:gas", 500, "hbm:petroleum", 20, 100},
                {"mun", "ntmspace:ore_brine", "ntmspace:brine", 300, null, 0, 0},
                {"ike", "ntmspace:ore_brine", "ntmspace:brine", 300, null, 0, 0},
        };
        for (Object[] row : rows) {
            var state = bodyState((String) row[1], (String) row[0]);
            if (state == null) {
                errors.add(row[1] + "[body=" + row[0] + "] does not exist");
                continue;
            }
            var well = new com.hbm.tileentity.machine.oil.TileEntityMachineOilWell(helper.absolutePos(new BlockPos(3, 1, 1)),
                    com.hbm.blocks.machine.ModMachineBlocks.machine_well.get().defaultBlockState());
            well.setLevel(level);
            level.setBlock(pos, state, 3);
            if (!well.canSuckState(state)) errors.add("the derrick does not reach " + row[1] + "[" + row[0] + "]");
            well.doSuck(pos);
            String what = row[1] + "[" + row[0] + "]";
            if (!well.tanks[0].getTankType().getRegistryName().equals(row[2]) || well.tanks[0].getFill() != (int) row[3]) {
                errors.add(what + " gave " + well.tanks[0].getFill() + " " + well.tanks[0].getTankType().getRegistryName()
                        + ", hbm-space " + row[3] + " " + row[2]);
            }
            if (row[4] != null) {
                int gas = well.tanks[1].getFill();
                if (!well.tanks[1].getTankType().getRegistryName().equals(row[4]) || gas < (int) row[5] || gas > (int) row[6]) {
                    errors.add(what + " secondary " + gas + " " + well.tanks[1].getTankType().getRegistryName() + ", hbm-space "
                            + row[5] + ".." + row[6] + " " + row[4]);
                }
            } else if (well.tanks[1].getFill() != 0) {
                errors.add(what + " gave a secondary fluid");
            }
        }
        // the pumpjack: 1.5x the oil, 0.5x the gas (Duna: 300 oil, 50..250 gas)
        var pumpjack = new com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack(helper.absolutePos(new BlockPos(3, 1, 3)),
                com.hbm.blocks.machine.ModMachineBlocks.machine_pumpjack.get().defaultBlockState());
        pumpjack.setLevel(level);
        level.setBlock(pos, bodyState("ntmspace:ore_oil", "duna"), 3);
        pumpjack.doSuck(pos);
        if (pumpjack.tanks[0].getFill() != 300 || pumpjack.tanks[1].getFill() < 50 || pumpjack.tanks[1].getFill() > 250) {
            errors.add("pumpjack on Duna oil gave " + pumpjack.tanks[0].getFill() + " / " + pumpjack.tanks[1].getFill()
                    + ", hbm-space 300 / 50..250");
        }
        // the fracking tower takes the deposit's own amounts: Earth oil 500 + 100..500 gas, bedrock oil 100 + 10..50
        if (com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower.oilPerDeposit != 500
                || com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower.drainChance != 0.05D) {
            errors.add("fracking tower amounts are not hbm-space's (500, 0.05)");
        }
        var bedrock = com.hbm.api.data.DepositDataMaps.deposit(block("hbm:ore_bedrock_oil"));
        if (bedrock == null || bedrock.primaryAmount(500) != 100 || bedrock.secondaryAmount(100) != 10
                || bedrock.secondaryAmount(500) != 50 || !bedrock.frackingOnly()) {
            errors.add("bedrock oil deposit " + bedrock + ", hbm-space 100 + 10..50 fracking only");
        }
        // draining keeps the body; the derrick searches through the empty
        var duna = bodyState("ntmspace:ore_oil", "duna");
        var empty = com.hbm.api.data.DepositDataMaps.emptyState(duna, block("ntmspace:ore_oil_empty"));
        if (!empty.getBlock().equals(block("ntmspace:ore_oil_empty")) || !empty.toString().contains("body=duna")) {
            errors.add("drained Duna oil becomes " + empty);
        }
        if (!com.hbm.api.data.DepositDataMaps.isEmptyDeposit(block("ntmspace:ore_gas_empty"))) {
            errors.add("ore_gas_empty is not a drained deposit the drills search through");
        }
        finish(helper, errors);
    }

    static BlockState bodyState(String id, String body) {
        Block block = block(id);
        if (block == Blocks.AIR) return null;
        var prop = block.getStateDefinition().getProperty("body");
        if (prop == null) return null;
        return withValue(block.defaultBlockState(), prop, body);
    }

    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState withValue(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.state.properties.Property<T> prop,
            String value) {
        return prop.getValue(value).map(v -> state.setValue(prop, v)).orElse(null);
    }

    // ------------------------------------------------------------------------------------------------ damaged machines

    /**
     * hbm-space's damaged Leviathan and powered condenser: the substitutions are in place, a structure-placed machine
     * is damaged and idle, explosions damage then remove it, a blowtorch repair takes the 1.7 materials.
     */
    private static void damaged(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        Block chungusBlock = block("hbm:machine_chungus");
        Block condenserBlock = block("hbm:machine_condenser_powered");
        if (!(chungusBlock instanceof com.ntmspace.fork.machine.SpaceMachineChungus)) errors.add("hbm:machine_chungus is not substituted");
        if (!(condenserBlock instanceof com.ntmspace.fork.machine.SpaceMachineCondenserPowered)) {
            errors.add("hbm:machine_condenser_powered is not substituted");
        }
        BlockPos core = helper.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(core, chungusBlock.defaultBlockState(), 3);
        if (!(level.getBlockEntity(core) instanceof com.ntmspace.fork.machine.TileEntityChungusSpace turbine)) {
            errors.add("the Leviathan core has no TileEntityChungusSpace: " + level.getBlockEntity(core));
            finish(helper, errors);
            return;
        }
        // structure placement wrecks it; a wreck does not run
        turbine.transformTE(level, 0);
        turbine.tanks[0].setTankType(com.hbm.inventory.fluid.Fluids.STEAM);
        turbine.tanks[0].setFill(1000);
        com.ntmspace.fork.machine.TileEntityChungusSpace.serverTick(level, core, level.getBlockState(core), turbine);
        if (turbine.tanks[0].getFill() != 1000 || turbine.powerBuffer != 0) errors.add("a damaged Leviathan ran");
        turbine.setDamaged(false);
        com.ntmspace.fork.machine.TileEntityChungusSpace.serverTick(level, core, level.getBlockState(core), turbine);
        if (turbine.tanks[0].getFill() >= 1000) errors.add("a repaired Leviathan did not take steam");

        // blowtorch repair: nothing without the materials, everything taken with them
        turbine.setDamaged(true);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        if (com.ntmspace.fork.machine.DamagedMachines.repair(level, core, player)) errors.add("repaired without materials");
        for (var material : turbine.getRepairMaterials()) {
            ItemStack give = material.getDisplayStacks().get(0).copy();
            give.setCount(material.stacksize);
            player.getInventory().add(give);
        }
        if (!com.ntmspace.fork.machine.DamagedMachines.repair(level, core, player) || turbine.isDamaged()) {
            errors.add("not repaired with the 1.7 materials");
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) errors.add("repair left " + player.getInventory().getItem(i));
        }
        if (turbine.getRepairMaterials().size() != 5) errors.add("Leviathan repair has " + turbine.getRepairMaterials().size() + " materials, 1.7 five");

        // explosions: the first damages, the same one again does nothing more, a second one removes the core
        Object first = new Object(), second = new Object();
        com.ntmspace.fork.machine.DamagedMachines.hit(level, core, first);
        com.ntmspace.fork.machine.DamagedMachines.hit(level, core, first);
        if (!turbine.isDamaged() || level.getBlockState(core).getBlock() != chungusBlock) errors.add("the first explosion did not just damage");
        com.ntmspace.fork.machine.DamagedMachines.hit(level, core, second);
        if (level.getBlockState(core).getBlock() == chungusBlock) errors.add("a second explosion did not remove the damaged core");

        // the powered condenser
        BlockPos cond = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(cond, condenserBlock.defaultBlockState(), 3);
        if (level.getBlockEntity(cond) instanceof com.ntmspace.fork.machine.TileEntityCondenserPoweredSpace condenser) {
            condenser.transformTE(level, 0);
            if (!condenser.isDamaged() || condenser.getRepairMaterials().size() != 3) errors.add("condenser wreck / repair list");
        } else {
            errors.add("the condenser core has no TileEntityCondenserPoweredSpace");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ det_salt

    /** {@code det_salt}: a nuclear charge (det_nuke's blast), its 1.7 recipe; {@code salted_fallout} adds 50 RAD per tick. */
    private static void salted(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        if (!(block("ntmspace:det_salt") instanceof com.hbm.blocks.bomb.ExplosiveCharge charge)
                || charge.kind != com.hbm.blocks.bomb.ExplosiveCharge.Kind.NUKE) {
            errors.add("ntmspace:det_salt is not a nuclear ExplosiveCharge");
        }
        // CraftingManager: "PIP", "DCD", "PIP"; P cobalt ingot, I cast TCA plate, D GaAs billet, C det_nuke
        ItemStack p = stack("hbm:ingot_cobalt"), i = stack("hbm:plate_cast_tcalloy"), d = stack("ntmspace:billet_gaas"),
                c = stack("hbm:det_nuke");
        if (p == null || i == null || d == null || c == null) {
            errors.add("a det_salt ingredient is not registered");
        } else {
            var result = dev.golem.core.world.Recipes.craftingResult(helper.getLevel(), 3, 3,
                    java.util.Arrays.asList(p, i, p, d, c, d, p, i, p));
            if (result.isEmpty() || !id(result.get()).equals("ntmspace:det_salt")) errors.add("det_salt is not crafted: " + result);
        }
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        Block fallout = block("ntmspace:salted_fallout");
        if (!(fallout instanceof com.ntmspace.fork.SaltedFallout salt)) {
            errors.add("ntmspace:salted_fallout is not a SaltedFallout");
        } else {
            double before = com.hbm.handler.radiation.ChunkRadiationManager.proxy.getRadiation(level, pos);
            salt.randomTick(fallout.defaultBlockState(), level, pos, level.getRandom());
            double after = com.hbm.handler.radiation.ChunkRadiationManager.proxy.getRadiation(level, pos);
            if (Math.abs(after - before - 50) > 1e-6) errors.add("salted fallout added " + (after - before) + " RAD, 1.7 50");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ small tweaks

    /** The rag above 500 RAD, the deuterium extractor's power use. */
    private static void tweaks(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        if (!id(com.hbm.items.special.ItemRag.product(player)).equals("hbm:rag_piss")) errors.add("a clean player's rag is not rag_piss");
        com.hbm.capability.HbmLivingProps.setRadiation(player, 501);
        if (!id(com.hbm.items.special.ItemRag.product(player)).equals("ntmspace:rag_blood")) errors.add("above 500 RAD the rag is not rag_blood");
        com.hbm.capability.HbmLivingProps.setRadiation(player, 500);
        if (!id(com.hbm.items.special.ItemRag.product(player)).equals("hbm:rag_piss")) errors.add("at 500 RAD the rag is not rag_piss (1.7: > 500)");

        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        Block extractorBlock = block("hbm:machine_deuterium_extractor");
        level.setBlock(pos, extractorBlock.defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof com.hbm.tileentity.machine.TileEntityDeuteriumExtractor extractor) {
            extractor.tanks[0].setFill(extractor.tanks[0].getMaxFill());
            extractor.power = 200; // 1.7 hbm-space: needs more than 200 HE
            com.hbm.tileentity.machine.TileEntityDeuteriumExtractor.serverTick(level, pos, level.getBlockState(pos), extractor);
            if (extractor.power != 200) errors.add("the extractor ran at 200 HE");
            extractor.power = 201;
            com.hbm.tileentity.machine.TileEntityDeuteriumExtractor.serverTick(level, pos, level.getBlockState(pos), extractor);
            long used = 201 - extractor.power;
            if (used != extractor.getMaxPower() / 100) errors.add("the extractor used " + used + " HE, hbm-space maxPower / 100");
        } else {
            errors.add("no deuterium extractor block entity");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElse(Items.AIR);
    }

    static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElse(Blocks.AIR);
    }

    private static ItemStack stack(String id) {
        Item item = item(id);
        return item == Items.AIR ? null : new ItemStack(item);
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (!errors.isEmpty()) {
            String message = String.join("; ", errors);
            com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
            return;
        }
        helper.succeed();
    }
}
