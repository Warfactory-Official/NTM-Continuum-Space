package com.ntmspace.gametest;

import com.ntmspace.block.flora.BlockRubberFarmland;
import com.ntmspace.block.flora.BlockSpaceCrop;
import com.ntmspace.block.flora.BlockSpaceLeaves;
import com.ntmspace.block.flora.BlockSpaceSapling;
import com.ntmspace.block.flora.CropKind;
import com.ntmspace.block.flora.RubberSoil;
import com.ntmspace.block.terrain.BlockGeysirElectric;
import com.ntmspace.block.terrain.BlockLaytheCoral;
import com.ntmspace.block.terrain.BlockLaytheCoralBlock;
import com.ntmspace.block.terrain.BlockRubberPlant;
import com.ntmspace.block.terrain.BlockRubberTall;
import com.ntmspace.block.terrain.PlantType;
import com.ntmspace.registry.FloraBlocks;
import com.ntmspace.registry.LaytheTektoBlocks;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NTM: Space's flora against hbm-space's rules: the drops of every flora block (counts, types, fortune where 1.7 had
 * it), crop growth stages (random tick and bonemeal), both saplings growing their 1.7 {@code TTree} on rubber grass,
 * leaf decay (1.7's four-step search, the flag, the decay), the coral's attachment, rubber farmland's moisture and
 * silting, hoe tilling, and both geysers' damage columns.
 */
public final class FloraGameTests {

    private FloraGameTests() {}

    public static void init() {}

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function REGISTERED = TESTS.function("flora_registered", 20, () -> FloraGameTests::registered);
    public static final GameTests.Function WATER_PLANT_DROPS = TESTS.function("flora_saltleaf_drops", 20, 2,
            () -> FloraGameTests::waterPlantDrops);
    public static final GameTests.Function TYPED_DROPS = TESTS.function("flora_typed_drops", 20, 2, () -> FloraGameTests::typedDrops);
    public static final GameTests.Function CROP_DROPS = TESTS.function("flora_crop_drops", 20, () -> FloraGameTests::cropDrops);
    public static final GameTests.Function CROP_GROWTH = TESTS.function("flora_crop_growth", 20, 4, () -> FloraGameTests::cropGrowth);
    public static final GameTests.Function SAPLING_PVC = TESTS.function("flora_sapling_grows_pvc_tree", 40, 12,
            () -> helper -> sapling(helper, PlantType.Sapling.PVC));
    public static final GameTests.Function SAPLING_VINYL = TESTS.function("flora_sapling_grows_vinyl_tree", 40, 12,
            () -> helper -> sapling(helper, PlantType.Sapling.VINYL));
    public static final GameTests.Function LEAF_DECAY = TESTS.function("flora_leaf_decay", 20, 12, () -> FloraGameTests::leafDecay);
    public static final GameTests.Function LEAF_DROPS = TESTS.function("flora_leaf_drops", 20, () -> FloraGameTests::leafDrops);
    public static final GameTests.Function CORAL_FACING = TESTS.function("flora_coral_attachment", 20, 4, () -> FloraGameTests::coral);
    public static final GameTests.Function FARMLAND = TESTS.function("flora_rubber_farmland", 20, 8, () -> FloraGameTests::farmland);
    public static final GameTests.Function HOE = TESTS.function("flora_hoe_tills_rubber_soil", 20, 4, () -> FloraGameTests::hoe);
    public static final GameTests.Function GEYSERS = TESTS.function("flora_geyser_columns", 80, 8, () -> FloraGameTests::geysers);

    // ---------------------------------------------------------------------------------------------------------------

    private static void registered(GameTestHelper helper) {
        String[] blocks = {"rubber_farmland", "crop_strawberry", "crop_mint", "crop_coffee", "crop_tea", "crop_paraffin", "sapling_vinyl",
                "sapling_pvc", "vinyl_planks", "pvc_planks", "lattice_log", "bf_log", "primed_log", "eu_log", "astroturf", "geysir_electric"};
        String[] items = {"saltleaf", "leaf_rubber", "leaf_pet", "paraffin_seeds", "strawberry", "mint_leaves", "bean_raw",
                "bean_roast", "teaseeds", "tea_leaf", "stick_pvc", "stick_vinyl", "powder_rubber", "powder_pvc",
                "woodemium_briquette", "sapling_vinyl", "sapling_pvc", "laythe_coral", "laythe_coral_horn", "laythe_coral_block_fire",
                "rubber_plant_shrub", "rubber_tall", "rubber_tall_flower"};
        for (String id : blocks) {
            helper.assertTrue(BuiltInRegistries.BLOCK.getOptional(Ids.of("ntmspace", id)).isPresent(), "block ntmspace:" + id);
        }
        for (String id : items) {
            helper.assertTrue(BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", id)).isPresent(), "item ntmspace:" + id);
        }
        // the block's own item is the first type's
        helper.assertTrue(LaytheTektoBlocks.LAYTHE_CORAL.get().asItem() == item("laythe_coral"), "laythe_coral asItem");
        helper.assertTrue(FloraBlocks.SAPLING_VINYL.get().asItem() == item("sapling_vinyl"), "sapling_vinyl asItem");
        helper.succeed();
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", path)).orElseThrow();
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, ItemStack tool) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        return Block.getDrops(state, helper.getLevel(), pos, null, null, tool);
    }

    /** 1.7 {@code quantityDropped = random.nextInt(4)} of saltleaf, for the seagrass, glowgrass, kelp and saltgrass. */
    private static void waterPlantDrops(GameTestHelper helper) {
        Block[] plants = {LaytheTektoBlocks.LAYTHE_SEAGRASS.get(), LaytheTektoBlocks.LAYTHE_GLOWGRASS.get(),
                LaytheTektoBlocks.LAYTHE_KELP.get(), LaytheTektoBlocks.PLANT_TALL_LAYTHE.get()};
        for (Block plant : plants) {
            boolean[] seen = new boolean[4];
            for (int i = 0; i < 400; i++) {
                List<ItemStack> drops = drops(helper, plant.defaultBlockState(), ItemStack.EMPTY);
                int count = 0;
                for (ItemStack stack : drops) {
                    helper.assertTrue(stack.is(FloraBlocks.SALTLEAF.get()), plant + " drops only saltleaf, got " + stack);
                    count += stack.getCount();
                }
                helper.assertTrue(count <= 3, plant + " drops at most three saltleaves, got " + count);
                seen[count] = true;
            }
            helper.assertTrue(seen[0] && seen[1] && seen[2] && seen[3], plant + " drops 0, 1, 2 and 3 saltleaves");
        }
        helper.succeed();
    }

    private static void typedDrops(GameTestHelper helper) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        List<ItemStack> coral = drops(helper, LaytheTektoBlocks.LAYTHE_CORAL.get().defaultBlockState()
                .setValue(BlockLaytheCoral.TYPE, PlantType.Coral.FIRE), ItemStack.EMPTY);
        helper.assertTrue(coral.size() == 1 && coral.get(0).getCount() == 1
                        && coral.get(0).is(BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", "powder_calcium")).orElseThrow()),
                "laythe coral drops one calcium powder, got " + coral);
        List<ItemStack> block = drops(helper, LaytheTektoBlocks.LAYTHE_CORAL_BLOCK.get().defaultBlockState()
                .setValue(BlockLaytheCoralBlock.TYPE, PlantType.Coral.BRAIN), pick);
        helper.assertTrue(block.size() == 1 && block.get(0).is(item("laythe_coral_block_brain")),
                "a brain coral block drops its own item, got " + block);
        BlockState tall = LaytheTektoBlocks.RUBBER_TALL.get().defaultBlockState();
        helper.assertTrue(only(drops(helper, tall, ItemStack.EMPTY), item("rubber_tall")), "the rubber cactus drops itself");
        helper.assertTrue(only(drops(helper, tall.setValue(BlockRubberTall.TYPE, PlantType.RubberTall.BUSH), ItemStack.EMPTY),
                item("rubber_tall_bush")), "the rubber bush drops itself");
        helper.assertTrue(only(drops(helper, tall.setValue(BlockRubberTall.TYPE, PlantType.RubberTall.FLOWER), ItemStack.EMPTY),
                FloraBlocks.PARAFFIN_SEEDS.get()), "the rubber flower drops chlorinated seeds");
        helper.assertTrue(drops(helper, LaytheTektoBlocks.RUBBER_PLANT.get().defaultBlockState()
                .setValue(BlockRubberPlant.TYPE, PlantType.RubberPlant.FERN), ItemStack.EMPTY).isEmpty(), "rubber plants drop nothing");
        BlockState sapling = FloraBlocks.SAPLING_PVC.get().defaultBlockState();
        helper.assertTrue(only(drops(helper, sapling, ItemStack.EMPTY), item("sapling_pvc")), "the PVC sapling drops itself");
        helper.assertTrue(only(drops(helper, FloraBlocks.RUBBER_FARMLAND.get().defaultBlockState(), ItemStack.EMPTY),
                LaytheTektoBlocks.RUBBER_SILT.get().asItem()), "rubber farmland drops rubber silt");
        helper.succeed();
    }

    private static boolean only(List<ItemStack> drops, Item item) {
        return drops.size() == 1 && drops.get(0).is(item) && drops.get(0).getCount() == 1;
    }

    private static int count(List<ItemStack> drops, Item item) {
        int n = 0;
        for (ItemStack stack : drops) if (stack.is(item)) n += stack.getCount();
        return n;
    }

    /** 1.7 {@code quantityDropped}: ripe 4, else half the stage; tea and wax extra rolls only when ripe. */
    private static void cropDrops(GameTestHelper helper) {
        RandomSource random = RandomSource.create(1234L);
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 4};
        for (int age = 0; age <= 7; age++) {
            List<ItemStack> strawberry = BlockSpaceCrop.drops(CropKind.STRAWBERRY, age, 0, random);
            helper.assertTrue(count(strawberry, FloraBlocks.STRAWBERRY.get()) == expected[age] && strawberry.size() == expected[age],
                    "strawberry crop at stage " + age + " drops " + expected[age] + ", got " + strawberry);
            List<ItemStack> tea = BlockSpaceCrop.drops(CropKind.TEA, age, 0, random);
            if (age < 7) {
                helper.assertTrue(count(tea, FloraBlocks.TEASEEDS.get()) == expected[age] && tea.size() == expected[age],
                        "unripe tea at stage " + age + " drops " + expected[age] + " tea seeds, got " + tea);
            }
        }
        Item wax = com.hbm.items.ModOilItems.tar(com.hbm.items.ItemEnums.EnumTarType.WAX);
        int seeds = 0;
        int rolls = 0;
        for (int i = 0; i < 2000; i++) {
            List<ItemStack> tea = BlockSpaceCrop.drops(CropKind.TEA, 7, 0, random);
            helper.assertTrue(count(tea, FloraBlocks.TEA_LEAF.get()) == 4, "ripe tea drops four tea leaves");
            int extra = count(tea, FloraBlocks.TEASEEDS.get());
            helper.assertTrue(extra <= 3, "ripe tea rolls three times for seeds, got " + extra);
            seeds += extra;
            rolls += 3;
            List<ItemStack> paraffin = BlockSpaceCrop.drops(CropKind.PARAFFIN, 7, 2, random);
            int pairs = count(paraffin, wax);
            helper.assertTrue(count(paraffin, FloraBlocks.PARAFFIN_SEEDS.get()) == 4 + pairs && pairs <= 5,
                    "ripe wax drops four seeds plus one seed per wax (up to 3 + fortune 2), got " + paraffin);
        }
        double rate = seeds / (double) rolls;
        helper.assertTrue(Math.abs(rate - 8.0 / 15.0) < 0.03, "each ripe tea roll gives a seed 8 times in 15, got " + rate);
        helper.succeed();
    }

    private static void cropGrowth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos soil = helper.absolutePos(GameTests.paddedCentre(4, 1));
        BlockPos pos = soil.above();
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, FloraBlocks.CROP_MINT.get().defaultBlockState(), 3);
        BlockSpaceCrop crop = FloraBlocks.CROP_MINT.get();
        crop.randomTick(level.getBlockState(pos), level, pos, level.getRandom());
        helper.assertTrue(level.getBlockState(pos).getValue(BlockSpaceCrop.AGE) == 1, "a random tick grows the crop one stage");
        RandomSource random = RandomSource.create(7L);
        for (int i = 0; i < 50; i++) {
            BlockState young = crop.defaultBlockState().setValue(BlockSpaceCrop.AGE, 1);
            level.setBlock(pos, young, 3);
            crop.performBonemeal(level, random, pos, young);
            int age = level.getBlockState(pos).getValue(BlockSpaceCrop.AGE);
            helper.assertTrue(age >= 3 && age <= 6, "bonemeal grows 2 to 5 stages, got 1 -> " + age);
        }
        BlockState ripe = crop.defaultBlockState().setValue(BlockSpaceCrop.AGE, 7);
        level.setBlock(pos, ripe, 3);
        helper.assertTrue(!crop.canBonemeal(level, pos, ripe), "a ripe crop takes no bonemeal");
        crop.randomTick(ripe, level, pos, random);
        helper.assertTrue(level.getBlockState(pos).getValue(BlockSpaceCrop.AGE) == 7, "stage 7 is the last");
        // the seed food plants only on farmland's top with air above
        helper.assertTrue(com.ntmspace.block.flora.ItemSpaceSeedFood.isSeedGround(Blocks.FARMLAND.defaultBlockState())
                && com.ntmspace.block.flora.ItemSpaceSeedFood.isSeedGround(FloraBlocks.RUBBER_FARMLAND.get().defaultBlockState())
                && !com.ntmspace.block.flora.ItemSpaceSeedFood.isSeedGround(Blocks.DIRT.defaultBlockState()),
                "seeds plant on farmland and rubber farmland only");
        // a crop stays on dirt (1.7 Plains) but not on stone
        level.setBlock(soil, Blocks.DIRT.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).is(crop), "a crop stays on dirt");
        level.setBlock(soil, Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).isAir(), "a crop on stone breaks");
        helper.succeed();
    }

    private static void sapling(GameTestHelper helper, PlantType.Sapling type) {
        ServerLevel level = helper.getLevel();
        // 1.7's TTree and World17 keep to heights 1..255: build well above the test floor (y -57 on 26.1)
        BlockPos centre = helper.absolutePos(GameTests.paddedCentre(12, 80));
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlock(centre.offset(dx, 0, dz), LaytheTektoBlocks.RUBBER_GRASS.get().defaultBlockState(), 2);
                for (int dy = 1; dy <= 14; dy++) level.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        BlockPos pos = centre.above();
        BlockState sapling = (type == PlantType.Sapling.PVC ? FloraBlocks.SAPLING_PVC : FloraBlocks.SAPLING_VINYL).get().defaultBlockState();
        level.setBlock(pos, sapling, 2);
        BlockSpaceSapling.grow(level, pos, sapling, RandomSource.create(42L));
        boolean pvc = type == PlantType.Sapling.PVC;
        Block log = pvc ? LaytheTektoBlocks.PVC_LOG.get() : LaytheTektoBlocks.VINYL_LOG.get();
        Block leaves = pvc ? LaytheTektoBlocks.RUBBER_LEAVES.get() : LaytheTektoBlocks.PET_LEAVES.get();
        // TTree heights: PVC 7 + nextInt(5), vinyl 5 + nextInt(4)
        int height = 0;
        while (level.getBlockState(pos.above(height)).is(log)) height++;
        int min = pvc ? 7 : 5;
        int max = pvc ? 11 : 8;
        helper.assertTrue(height >= min && height <= max, type + " sapling grows a log column " + min + ".." + max + " tall, got " + height);
        int leafCount = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 14, 4))) {
            if (level.getBlockState(p).is(leaves)) leafCount++;
        }
        helper.assertTrue(leafCount > 10, type + " sapling's tree has its leaves, got " + leafCount);
        // on rubber farmland the tree cannot stand: the sapling stays
        BlockPos other = pos.offset(4, 0, 4);
        level.setBlock(other.below(), FloraBlocks.RUBBER_FARMLAND.get().defaultBlockState(), 2);
        for (int dy = 0; dy <= 14; dy++) level.setBlock(other.above(dy), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(other, sapling, 2);
        BlockSpaceSapling.grow(level, other, sapling, RandomSource.create(42L));
        helper.assertTrue(level.getBlockState(other).is(sapling.getBlock()),
                "a sapling on rubber farmland keeps its type");
        helper.succeed();
    }

    private static void leafDecay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(GameTests.paddedCentre(12, 2));
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-6, -1, -6), centre.offset(6, 4, 6))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        BlockState leaf = LaytheTektoBlocks.RUBBER_LEAVES.get().defaultBlockState();
        // a log with a row of five leaves east of it: four steps reach, the fifth does not
        level.setBlock(centre, LaytheTektoBlocks.PVC_LOG.get().defaultBlockState(), 2);
        for (int i = 1; i <= 5; i++) level.setBlock(centre.east(i), leaf, 2);
        for (int i = 1; i <= 4; i++) {
            helper.assertTrue(BlockSpaceLeaves.connected(level, centre.east(i)), "a leaf " + i + " steps from a log is connected");
        }
        helper.assertTrue(!BlockSpaceLeaves.connected(level, centre.east(5)), "a leaf 5 steps from a log is not");
        // generated leaves are unflagged and do not tick
        helper.assertTrue(!leaf.isRandomlyTicking(), "an unflagged leaf has no random tick");
        // removing the log flags the leaf beside it; the flagged leaf decays on its random tick
        level.setBlock(centre, Blocks.AIR.defaultBlockState(), 3);
        BlockState first = level.getBlockState(centre.east());
        helper.assertTrue(first.getValue(BlockSpaceLeaves.CHECK_DECAY), "a leaf whose neighbour goes away is flagged");
        first.randomTick(level, centre.east(), level.getRandom());
        helper.assertTrue(level.getBlockState(centre.east()).isAir(), "a flagged leaf with no log in reach decays");
        // its removal flags the next one (1.7 BlockLeaves.breakBlock)
        helper.assertTrue(level.getBlockState(centre.east(2)).getValue(BlockSpaceLeaves.CHECK_DECAY), "a decayed leaf flags its neighbours");
        // a flagged leaf that still reaches a log clears its flag
        level.setBlock(centre.east(3), LaytheTektoBlocks.PVC_LOG.get().defaultBlockState(), 2);
        BlockState kept = level.getBlockState(centre.east(2));
        kept.randomTick(level, centre.east(2), level.getRandom());
        helper.assertTrue(level.getBlockState(centre.east(2)).is(LaytheTektoBlocks.RUBBER_LEAVES.get())
                && !level.getBlockState(centre.east(2)).getValue(BlockSpaceLeaves.CHECK_DECAY), "a connected leaf clears its flag");
        helper.succeed();
    }

    /** 1.7: 1/20 the leaf (1/10 with fortune 3), then 30 % another leaf and of those 50 % a sapling of the tree's type. */
    private static void leafDrops(GameTestHelper helper) {
        BlockSpaceLeaves rubber = LaytheTektoBlocks.RUBBER_LEAVES.get();
        Item leaf = FloraBlocks.LEAF_RUBBER.get();
        Item pvcSapling = item("sapling_pvc");
        RandomSource random = RandomSource.create(99L);
        int trials = 20000;
        int leaves = 0, saplings = 0, fortuneLeaves = 0;
        for (int i = 0; i < trials; i++) {
            List<ItemStack> drops = rubber.drops(0, random);
            for (ItemStack stack : drops) {
                helper.assertTrue(stack.is(leaf) || stack.is(pvcSapling), "rubber leaves drop rubber leaves and PVC saplings, got " + stack);
            }
            leaves += count(drops, leaf);
            saplings += count(drops, pvcSapling);
            fortuneLeaves += count(rubber.drops(3, random), leaf);
        }
        double leafRate = leaves / (double) trials;
        double saplingRate = saplings / (double) trials;
        double fortuneRate = fortuneLeaves / (double) trials;
        helper.assertTrue(Math.abs(leafRate - 0.35) < 0.02, "leaf items per break 1/20 + 0.3, got " + leafRate);
        helper.assertTrue(Math.abs(saplingRate - 0.15) < 0.015, "saplings per break 0.3 * 0.5, got " + saplingRate);
        helper.assertTrue(Math.abs(fortuneRate - 0.40) < 0.02, "fortune 3: 1/10 + 0.3 leaf items, got " + fortuneRate);
        List<ItemStack> pet = new ArrayList<>();
        for (int i = 0; i < 200; i++) pet.addAll(LaytheTektoBlocks.PET_LEAVES.get().drops(0, random));
        for (ItemStack stack : pet) {
            helper.assertTrue(stack.is(FloraBlocks.LEAF_PET.get()) || stack.is(item("sapling_vinyl")),
                    "plastic leaves drop plastic leaves and vinyl saplings, got " + stack);
        }
        helper.succeed();
    }

    private static void coral(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(GameTests.paddedCentre(4, 2));
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
        }
        helper.assertTrue(BlockLaytheCoral.facing(level, pos) == Direction.UP, "coral with no solid neighbour stands upright");
        level.setBlock(pos.below(), LaytheTektoBlocks.LAYTHE_SILT.get().defaultBlockState(), 2);
        helper.assertTrue(BlockLaytheCoral.facing(level, pos) == Direction.UP, "coral on silt stands upright");
        level.setBlock(pos.west(), LaytheTektoBlocks.LAYTHE_CORAL_BLOCK.get().defaultBlockState(), 2);
        helper.assertTrue(BlockLaytheCoral.facing(level, pos) == Direction.EAST, "coral on a block to the west leans east (west before down)");
        level.setBlock(pos.north(), LaytheTektoBlocks.LAYTHE_CORAL_BLOCK.get().defaultBlockState(), 2);
        helper.assertTrue(BlockLaytheCoral.facing(level, pos) == Direction.SOUTH, "north is checked first");
        // the state follows a neighbour change
        level.setBlock(pos, LaytheTektoBlocks.LAYTHE_CORAL.get().defaultBlockState(), 2);
        level.setBlock(pos.north(), Blocks.WATER.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).getValue(BlockLaytheCoral.FACING) == Direction.EAST,
                "coral recomputes its lean when a neighbour changes, got " + level.getBlockState(pos));
        helper.succeed();
    }

    private static void farmland(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(GameTests.paddedCentre(8, 2));
        BlockRubberFarmland block = FloraBlocks.RUBBER_FARMLAND.get();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-5, 0, -5), pos.offset(5, 2, 5))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(pos, block.defaultBlockState(), 2);
        // vanilla water does not moisten it
        level.setBlock(pos.east(), Blocks.WATER.defaultBlockState(), 2);
        boolean raining = level.isRainingAt(pos.above());
        block.randomTick(level.getBlockState(pos), level, pos, level.getRandom());
        helper.assertTrue(raining || level.getBlockState(pos).getValue(BlockRubberFarmland.MOISTURE) == 0,
                "rubber farmland stays dry beside water");
        level.setBlock(pos.offset(4, 1, -4), LaytheTektoBlocks.CCL_BLOCK.get().defaultBlockState(), 2);
        block.randomTick(level.getBlockState(pos), level, pos, level.getRandom());
        helper.assertTrue(level.getBlockState(pos).getValue(BlockRubberFarmland.MOISTURE) == 1,
                "carbon tetrachloride four blocks across and one up moistens it");
        level.setBlock(pos.offset(4, 1, -4), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).is(LaytheTektoBlocks.RUBBER_SILT.get()), "a solid block above turns it into rubber silt");
        helper.succeed();
    }

    private static void hoe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(GameTests.paddedCentre(4, 1));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        for (Block soil : new Block[] {LaytheTektoBlocks.RUBBER_GRASS.get(), LaytheTektoBlocks.RUBBER_SILT.get()}) {
            level.setBlock(pos, soil.defaultBlockState(), 2);
            RubberSoil.till(level, pos, player, InteractionHand.MAIN_HAND, player.getMainHandItem(), FloraBlocks.RUBBER_FARMLAND);
            helper.assertTrue(level.getBlockState(pos).is(FloraBlocks.RUBBER_FARMLAND.get()), soil + " tills into rubber farmland");
        }
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 2, "each tilling wears the hoe by one");
        level.setBlock(pos, LaytheTektoBlocks.RUBBER_SILT.get().defaultBlockState(), 2);
        RubberSoil.till(level, pos, player, InteractionHand.MAIN_HAND, new ItemStack(Items.STICK), FloraBlocks.RUBBER_FARMLAND);
        helper.assertTrue(level.getBlockState(pos).is(LaytheTektoBlocks.RUBBER_SILT.get()), "only a hoe tills");
        helper.succeed();
    }

    /** Both geysers hurt what lives in the column above them; the discharge only when its charge runs out. */
    private static void geysers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chloric = helper.absolutePos(GameTests.paddedCentre(8, 1));
        BlockPos electric = chloric.east(4);
        for (BlockPos p : BlockPos.betweenClosed(chloric.offset(-2, 0, -2), electric.offset(2, 6, 2))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(chloric, LaytheTektoBlocks.GEYSIR_CHLORIC.get().defaultBlockState(), 3);
        level.setBlock(electric, FloraBlocks.GEYSIR_ELECTRIC.get().defaultBlockState(), 3);
        // each pig stands in a one-block glass well above its geyser, inside the damage column
        for (BlockPos geyser : new BlockPos[] {chloric, electric}) {
            level.setBlock(geyser.above(2), Blocks.GLASS.defaultBlockState(), 2);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                for (int dy = 3; dy <= 5; dy++) level.setBlock(geyser.above(dy).relative(side), Blocks.GLASS.defaultBlockState(), 2);
            }
        }
        net.minecraft.world.entity.Mob acidVictim = helper.spawn(EntityType.PIG, GameTests.relativePos(helper, chloric.above(3)));
        net.minecraft.world.entity.Mob sparkVictim = helper.spawn(EntityType.PIG, GameTests.relativePos(helper, electric.above(3)));
        // put each pig in its well (the helper's spawn position is not the well's on every node), and keep it there
        for (net.minecraft.world.entity.Mob pig : java.util.List.of(acidVictim, sparkVictim)) pig.setNoAi(true);
        acidVictim.teleportTo(chloric.getX() + 0.5, chloric.getY() + 3, chloric.getZ() + 0.5);
        sparkVictim.teleportTo(electric.getX() + 0.5, electric.getY() + 3, electric.getZ() + 0.5);
        float max = acidVictim.getMaxHealth();
        BlockGeysirElectric.Discharge discharge = (BlockGeysirElectric.Discharge) level.getBlockEntity(electric);
        helper.assertTrue(discharge != null, "the discharge geyser has its block entity");
        helper.assertTrue(level.getBlockEntity(chloric) != null, "the DCM geyser has its block entity");
        discharge.charge = 40;
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(acidVictim.getHealth() < max, "the DCM geyser's acid hurts a pig above it");
            helper.assertTrue(sparkVictim.getHealth() == sparkVictim.getMaxHealth(), "the discharge geyser waits for its charge");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(sparkVictim.isDeadOrDying() || sparkVictim.getHealth() < sparkVictim.getMaxHealth(),
                    "the discharge hurts a pig above it");
            helper.succeed();
        });
    }
}
