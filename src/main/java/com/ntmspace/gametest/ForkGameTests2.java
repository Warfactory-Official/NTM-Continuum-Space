package com.ntmspace.gametest;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.explosion.ExplosionBalefire;
import com.ntmspace.fork.ForkContent;
import com.ntmspace.fork.antimatter.AntimatterBalefire;
import com.ntmspace.fork.antimatter.TileEntityAntimatter;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.hook.Verdict;
import com.ntmspace.block.BlockBodyOre;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Part 2 of the fork-content tests ({@code scratchpad/m2/fork-content-triage.md}, "Part 2 list"): expected values are
 * hbm-space's ({@code ExplosionBalefire}, {@code EntityBalefire}, {@code NukeCustom}, {@code TileEntityNukeCustom},
 * {@code TileEntityAntimatter}, {@code ItemDrop}, {@code EntityNukeTorex.startFacAnti}).
 */
public final class ForkGameTests2 {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function AMAT_CRATER =
            TESTS.function("fork_antimatter_crater", 20, 8, () -> ForkGameTests2::antimatterCrater);
    public static final GameTests.Function AMAT_RADIATE =
            TESTS.function("fork_antimatter_radiation", 20, 8, () -> ForkGameTests2::antimatterRadiation);
    public static final GameTests.Function AMAT_HOOKS =
            TESTS.function("fork_antimatter_hooks", 20, 8, () -> ForkGameTests2::antimatterHooks);
    public static final GameTests.Function AMAT_BOMB =
            TESTS.function("fork_antimatter_bomb", 20, 8, () -> ForkGameTests2::antimatterBomb);

    public static final GameTests.Function LASER =
            TESTS.function("fork_fluid_ore_laser", 20, 8, () -> ForkGameTests2::laser);
    public static final GameTests.Function DETECTOR =
            TESTS.function("fork_oil_detector", 20, 8, () -> ForkGameTests2::detector);
    public static final GameTests.Function TEKTO_BEDROCK =
            TESTS.function("fork_tekto_bedrock_oil", 20, () -> ForkGameTests2::tektoBedrock);
    public static final GameTests.Function SETTINGS =
            TESTS.function("fork_part2_settings", 20, () -> ForkGameTests2::settings);

    public static final GameTests.Function BOBBLES =
            TESTS.function("fork_bobbleheads", 20, 8, () -> ForkGameTests2::bobbles);
    public static final GameTests.Function GWEN_SHREDDER =
            TESTS.function("fork_gwen_shredder", 40, 8, () -> ForkGameTests2::gwenShredder);

    public static final GameTests.Function SLIPPERY =
            TESTS.function("fork_slippery_nitan_hazmat", 20, 8, () -> ForkGameTests2::slippery);

    public static final GameTests.Function BOMBERS =
            TESTS.function("fork_bombers", 20, 8, () -> ForkGameTests2::bombers);
    public static final GameTests.Function CASSETTES =
            TESTS.function("fork_cassettes", 20, () -> ForkGameTests2::cassettes);

    private ForkGameTests2() {}

    public static void init() {}

    // ------------------------------------------------------------------------------------------------ antimatter

    /**
     * hbm-space {@code breakColumn} with {@code antimatter}, through the registered variant: snow and ice stay (gas flame
     * instead of removal), other blocks go; no balefire and no schrabidium-cluster stop; a floor layer of a dist-1 column
     * always gets volcanic lava at the crater floor ({@code nextInt(1) == 0}), never at the layer itself. (The whole
     * spiral is HBM's and is covered by HBM's {@code addon_hook_balefire_variant}; GameTest plots report no usable
     * surface heightmap, so the crater is not run here.)
     */
    private static void antimatterCrater(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        ExplosionBalefire.Variant variant = ExplosionBalefire.variant(AntimatterBalefire.ID);
        if (variant == null) {
            errors.add("ntmspace:antimatter is not a registered balefire variant");
            finish(helper, errors);
            return;
        }
        BlockPos centre = helper.absolutePos(new BlockPos(3, 2, 3));
        ExplosionBalefire exp = new ExplosionBalefire(centre.getX(), centre.getY(), centre.getZ(), level, 2);
        exp.variant = variant;
        if (variant.balefire()) errors.add("the antimatter crater must not place balefire or stop at clusters");
        for (Block keep : new Block[] {Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE}) {
            if (!variant.keep(exp, centre, keep.defaultBlockState())) errors.add(keep + " must stay (1.7 snow/ice material)");
        }
        for (Block go : new Block[] {Blocks.STONE, Blocks.DIRT, Blocks.WATER}) {
            if (variant.keep(exp, centre, go.defaultBlockState())) errors.add(go + " must be removed");
        }
        BlockPos floor = centre.offset(1, 0, 0);
        try {
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(floor.below(), Blocks.STONE.defaultBlockState(), 2);
            variant.floor(exp, floor.getX(), floor.getY(), floor.getZ(), 1, floor.getY() - 1);
            Block lava = com.hbm.blocks.ModFluidNetBlocks.volcanic_lava_block.get();
            if (!level.getBlockState(floor).is(lava)) errors.add("dist 1: no volcanic lava at the floor, got " + level.getBlockState(floor));
            if (!level.getBlockState(floor.below()).is(Blocks.STONE)) errors.add("lava went to the layer instead of the floor");
            // a dist-1000 column: the 1-in-1000 chance, 200 tries => expect well under 20 lava blocks
            int hits = 0;
            for (int t = 0; t < 200; t++) {
                level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
                variant.floor(exp, floor.getX(), floor.getY(), floor.getZ(), 1000, floor.getY());
                if (level.getBlockState(floor).is(lava)) hits++;
            }
            if (hits > 20) errors.add("dist 1000 placed lava " + hits + " times in 200");
        } finally {
            level.setBlock(floor, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(floor.below(), Blocks.AIR.defaultBlockState(), 2);
        }
        finish(helper, errors);
    }

    /**
     * hbm-space {@code EntityBalefire.radiate}: dose = rads / max(1, summed resistance) / len^2 through air
     * ({@code CREATIVE} contamination), 500 or more kills, and it counts {@code age} up once.
     */
    private static void antimatterRadiation(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        EntityBalefire bf = new EntityBalefire(com.hbm.entity.ModNtmEntities.BALEFIRE.get(), level);
        bf.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        bf.variant = AntimatterBalefire.ID;
        LivingEntity near = spawnPig(helper, new BlockPos(1, 3, 2));
        LivingEntity far = spawnPig(helper, new BlockPos(6, 3, 1));
        double lenNear = distanceToEyes(bf, near), lenFar = distanceToEyes(bf, far);
        // choose rads so that the far pig gets exactly 100 RAD (survives) and the near one far more than 500 (dies)
        float rads = (float) (100.0 * lenFar * lenFar);
        double before = com.hbm.capability.HbmLivingProps.getRadiation(far);
        int age = bf.age;
        AntimatterBalefire.radiate(level, bf, rads, 10);
        double got = com.hbm.capability.HbmLivingProps.getRadiation(far) - before;
        double expected = rads / (float) (lenFar * lenFar);
        if (Math.abs(got - expected) > 0.5) errors.add("far pig got " + got + " RAD, hbm-space " + expected);
        if (rads / (lenNear * lenNear) < 500) errors.add("test setup: the near pig is not in the lethal band");
        if (near.isAlive() && !near.isRemoved()) errors.add("a pig over 500 RAD survived");
        if (!far.isAlive()) errors.add("a pig under 500 RAD died");
        if (bf.age != age + 1) errors.add("radiate must count age up once (hbm-space), got " + (bf.age - age));
        far.discard();
        near.discard();
        finish(helper, errors);
    }

    private static LivingEntity spawnPig(GameTestHelper helper, BlockPos rel) {
        var pig = helper.spawn(EntityType.PIG, rel);
        pig.setNoAi(true);
        return pig;
    }

    private static double distanceToEyes(Entity from, LivingEntity to) {
        double dx = to.getX() - from.getX(), dy = to.getY() + to.getEyeHeight() - from.getY(), dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * The hooks NTM: Space answers: custom nuke antimatter floor 0 (hbm-space commented {@code nuke < 50} out), dropped
     * antimatter cell 5 (original 3), the custom nuke's antimatter stage spawns an antimatter balefire plus a type-2
     * Torex of scale {@code clamp(amat * 0.01, 0.5, 5)}, at the 1.7 positions.
     */
    private static void antimatterHooks(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        if (com.hbm.tileentity.bomb.TileEntityNukeCustom.amatNukeFloor != 0F) errors.add("custom nuke amat floor is not 0");
        if (com.hbm.items.special.ItemCell.antimatterDropStrength != 5F) errors.add("antimatter cell drop strength is not 5");
        if (!com.hbm.tileentity.machine.storage.TileEntityBarrel.ANTIMATTER_BLAST.iterator().hasNext()) {
            errors.add("no barrel antimatter blast listener");
        }
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        double x = at.getX() + 0.5, y = at.getY() + 0.5, z = at.getZ() + 0.5;
        com.hbm.explosion.ExplosionNukeCustom.explodeCustom(level, x, y, z, 0F, 0F, 0F, 120F, 0F, 0F, 0F);
        AABB box = new AABB(at).inflate(8);
        List<EntityBalefire> fires = level.getEntitiesOfClass(EntityBalefire.class, box);
        List<EntityNukeTorex> clouds = level.getEntitiesOfClass(EntityNukeTorex.class, box);
        if (fires.size() != 1) {
            errors.add("custom nuke amat spawned " + fires.size() + " balefires");
        } else {
            EntityBalefire bf = fires.get(0);
            if (!AntimatterBalefire.ID.equals(bf.variant)) errors.add("custom nuke balefire is not antimatter: " + bf.variant);
            if (bf.destructionRange != 120) errors.add("custom nuke range " + bf.destructionRange + ", expected 120");
            if (Math.abs(bf.getX() - (x + 0.5)) > 1e-6 || Math.abs(bf.getY() - (y + 0.5)) > 1e-6) errors.add("balefire position");
        }
        if (clouds.size() != 1) {
            errors.add("custom nuke amat spawned " + clouds.size() + " clouds");
        } else {
            EntityNukeTorex cloud = clouds.get(0);
            if (cloud.getTorexType() != 2) errors.add("cloud type " + cloud.getTorexType() + ", startFacAnti is 2");
            if (Math.abs(cloud.getScale() - 1.2F) > 1e-6) errors.add("cloud scale " + cloud.getScale() + ", expected 1.2");
            if (Math.abs(cloud.getY() - (y + 5)) > 1e-6) errors.add("cloud height");
        }
        fires.forEach(Entity::discard);
        clouds.forEach(Entity::discard);
        finish(helper, errors);
    }

    /**
     * {@code nuke_antimatter}: loaded with two lead particles, two hafnium ingots and an antimatter pellet; the timer
     * field sets seconds; detonation removes the block and spawns a 150-block antimatter balefire at the centre and a
     * type-2 cloud of scale 5 ({@code startFacAnti(..., 1000)}) five blocks up.
     */
    private static void antimatterBomb(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(at, ForkContent.NUKE_ANTIMATTER.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(at) instanceof TileEntityAntimatter bomb)) {
            errors.add("nuke_antimatter has no TileEntityAntimatter");
            finish(helper, errors);
            return;
        }
        if (bomb.timer != 18000) errors.add("default timer " + bomb.timer);
        bomb.setItem(0, new ItemStack(com.hbm.items.ModItems.particle_lead.get()));
        bomb.setItem(1, new ItemStack(com.hbm.items.ModItems.particle_lead.get()));
        bomb.setItem(3, new ItemStack(com.ntmspace.registry.SpaceItems.INGOT_HAFNIUM.get()));
        if (bomb.isLoaded()) errors.add("loaded without the core and the second hafnium ingot");
        bomb.setItem(4, new ItemStack(com.ntmspace.registry.SpaceItems.INGOT_HAFNIUM.get()));
        bomb.setItem(2, new ItemStack(com.hbm.items.ModSingularityItems.pellet_antimatter.get()));
        if (!bomb.isLoaded()) errors.add("not loaded with every part");
        bomb.handleButtonPacket(5, 1);
        if (bomb.timer != 100) errors.add("timer 5 s gave " + bomb.timer + " ticks");
        bomb.handleButtonPacket(0, 0);
        if (!bomb.started) errors.add("start did not start");
        if (!"ntmspace:nuke_antimatter".equals(BuiltInRegistries.ITEM.getKey(ForkContent.NUKE_ANTIMATTER_ITEM.get()).toString())) {
            errors.add("item id");
        }
        bomb.explode();
        if (!level.getBlockState(at).isAir()) errors.add("the bomb block was not removed");
        AABB box = new AABB(at).inflate(8);
        List<EntityBalefire> fires = level.getEntitiesOfClass(EntityBalefire.class, box);
        List<EntityNukeTorex> clouds = level.getEntitiesOfClass(EntityNukeTorex.class, box);
        if (fires.size() != 1 || fires.get(0).destructionRange != 150 || !AntimatterBalefire.ID.equals(fires.get(0).variant)) {
            errors.add("expected one 150-block antimatter balefire, got " + fires.size());
        }
        if (clouds.size() != 1 || clouds.get(0).getTorexType() != 2 || Math.abs(clouds.get(0).getScale() - 5.0) > 1e-6
                || Math.abs(clouds.get(0).getY() - (at.getY() + 5)) > 1e-6) {
            errors.add("expected one type-2 cloud of scale 5 five blocks up");
        }
        fires.forEach(Entity::discard);
        clouds.forEach(Entity::discard);
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ fluid ores

    /**
     * hbm-space {@code TileEntityMachineMiningLaser.suckDrops}: a mined fluid ore item fills the tank by
     * {@code getBlockFluidAmount} (oil 250, gas 100, brine 0 - consumed all the same) with the body's primary fluid (a new fluid empties the tank first, as 1.7's setTankType did); the
     * nullifier destroys the tag list (tektite included) and nothing else; anything else is buffered.
     */
    private static void laser(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        var laser = new com.hbm.tileentity.machine.TileEntityMachineMiningLaser(at,
                com.hbm.blocks.machine.ModMachineBlocks.machine_mining_laser.get().defaultBlockState());
        laser.tank.setFill(0);
        check(errors, helper, laser, "ntmspace:ore_gas_eve", false, Verdict.ALLOW, "hbm:gas", 100);
        check(errors, helper, laser, "ntmspace:ore_oil_laythe", false, Verdict.ALLOW, "hbm:oil_ds", 250);
        check(errors, helper, laser, "hbm:ore_oil", false, Verdict.ALLOW, "hbm:oil", 250);
        check(errors, helper, laser, "ntmspace:ore_brine_mun", false, Verdict.ALLOW, "hbm:oil", 250);
        check(errors, helper, laser, "hbm:tektite", true, Verdict.ALLOW, "hbm:oil", 250);
        check(errors, helper, laser, "hbm:tektite", false, Verdict.DENY, "hbm:oil", 250);
        check(errors, helper, laser, "minecraft:netherrack", true, Verdict.DENY, "hbm:oil", 250);
        check(errors, helper, laser, "minecraft:cobblestone", true, Verdict.ALLOW, "hbm:oil", 250);
        finish(helper, errors);
    }

    private static void check(List<String> errors, GameTestHelper helper, com.hbm.tileentity.machine.TileEntityMachineMiningLaser laser,
                              String item, boolean nullifier, Verdict expected, String fluid, int fill) {
        var stack = new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.parse(item)).orElseThrow());
        var entity = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, stack);
        Verdict verdict = com.ntmspace.fork.FluidOres.pickUp(laser, entity, nullifier);
        if (verdict != expected) errors.add(item + (nullifier ? " (nullifier)" : "") + ": " + verdict + ", expected " + expected);
        if (expected == Verdict.ALLOW && !entity.isRemoved()) errors.add(item + " was not consumed");
        if (laser.tank.getFill() != fill) errors.add("after " + item + " the tank holds " + laser.tank.getFill() + " mB, expected " + fill);
        String type = laser.tank.getTankType().getRegistryName();
        if (!fluid.equals(type)) errors.add("after " + item + " the tank holds " + type + ", expected " + fluid);
    }

    /**
     * hbm-space {@code ItemOilDetector}: HBM's {@code oil_detector} is substituted; a deposit in the player's column is a
     * bullseye of its reserve type, one in the ring is "detected"; the 5-ring comes before the diagonals.
     */
    private static void detector(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        if (!(com.hbm.items.ModOilItems.oil_detector.get() instanceof com.ntmspace.fork.SpaceOilDetector)) {
            errors.add("hbm:oil_detector is not NTM: Space's");
        }
        BlockPos player = helper.absolutePos(new BlockPos(6, 10, 6));
        BlockPos below = player.below(3);
        BlockPos ring = player.offset(5, -4, 0);
        BlockPos diagonal = player.offset(-5, -2, -5);
        try {
            level.setBlock(below, com.ntmspace.registry.BodyOreBlocks.state("ore_gas", BlockBodyOre.Body.EVE), 2);
            var direct = com.ntmspace.fork.SpaceOilDetector.searchDirect(level, player.getX(), player.getY(), player.getZ());
            if (direct == null || com.ntmspace.fork.FluidOres.reserve(direct) != com.ntmspace.fork.FluidOres.Reserve.GAS) {
                errors.add("gas below the player is not a gas bullseye: " + direct);
            }
            level.setBlock(below, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(diagonal, com.ntmspace.registry.BodyOreBlocks.state("ore_oil", BlockBodyOre.Body.DUNA), 2);
            level.setBlock(ring, com.ntmspace.registry.BodyOreBlocks.state("ore_brine", BlockBodyOre.Body.MUN), 2);
            var near = com.ntmspace.fork.SpaceOilDetector.search(level, player.getX(), player.getY(), player.getZ());
            if (near == null || com.ntmspace.fork.FluidOres.reserve(near) != com.ntmspace.fork.FluidOres.Reserve.BRINE) {
                errors.add("the 5-ring brine must win over the diagonal oil: " + near);
            }
        } finally {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(ring, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(diagonal, Blocks.AIR.defaultBlockState(), 2);
        }
        finish(helper, errors);
    }

    /**
     * hbm-space {@code BlockOreFluid} with no empty block and Tekto's metadata: Tekto crude + hydrogen-rich gas at
     * {@code tektoBedrockOilPerDeposit} 100 and 10..50 over the fracking tower's 500 and 100..500 (yield 0.2 / 0.1),
     * fracking only; Tekto's primer places it ({@code Block17.ORE_BEDROCK_OIL} meta 10), Kerbin's stays HBM's.
     */
    private static void tektoBedrock(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var state = ForkContent.ORE_BEDROCK_OIL.get().defaultBlockState().setValue(BlockBodyOre.BODY, BlockBodyOre.Body.TEKTO);
        var deposit = com.hbm.api.data.DepositDataMaps.deposit(state);
        if (deposit == null) {
            errors.add("no deposit for ntmspace:ore_bedrock_oil[body=tekto]");
        } else {
            if (!deposit.primary().equals("ntmspace:tcrude") || !deposit.secondary().equals("ntmspace:hgas")) {
                errors.add("Tekto bedrock fluids " + deposit.primary() + " / " + deposit.secondary());
            }
            if (deposit.primaryAmount(500) != 100 || deposit.secondaryAmount(100) != 10 || deposit.secondaryAmount(500) != 50) {
                errors.add("Tekto bedrock amounts " + deposit.primaryAmount(500) + ", " + deposit.secondaryAmount(100) + ".."
                        + deposit.secondaryAmount(500));
            }
            if (!deposit.frackingOnly() || deposit.emptyBlock() != null) errors.add("Tekto bedrock must be fracking-only and never drain");
        }
        var block17 = com.ntmspace.world.gen.legacy.Block17.ORE_BEDROCK_OIL;
        if (!"ntmspace:ore_bedrock_oil[body=tekto]".equals(block17.id(10))) errors.add("Tekto meta maps to " + block17.id(10));
        if (!"hbm:ore_bedrock_oil".equals(block17.id(1))) errors.add("Kerbin meta maps to " + block17.id(1));
        finish(helper, errors);
    }

    /**
     * Small fork rules NTM: Space switches on: HBM's damaged tank and refinery list their repair without a blowtorch, Tom's
     * lava within 500 blocks, coffee's mug, the plane crash effect and the filing cabinet's Soyuz blast.
     */
    private static void settings(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        if (!com.hbm.blocks.machine.MachineExplosionDamage.alwaysShowRepair) errors.add("repair overlay rule not set");
        if (com.hbm.explosion.ExplosionTom.lavaRadius != 500) errors.add("Tom lava radius " + com.hbm.explosion.ExplosionTom.lavaRadius);
        net.minecraft.world.item.Item mug = null;
        for (var listener : com.hbm.items.special.ItemEnergy.EMPTY) {
            mug = listener.apply("coffee");
            if (mug != null) break;
        }
        if (mug == null || !BuiltInRegistries.ITEM.getKey(mug).toString().equals("ntmspace:cmug_empty")) errors.add("coffee leaves " + mug);
        if (!com.hbm.entity.logic.EntityPlaneBase.CRASH.iterator().hasNext()) errors.add("no plane crash effect listener");
        if (!com.hbm.tileentity.machine.storage.TileEntityFileCabinet.TICK.iterator().hasNext()) errors.add("no filing cabinet listener");
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ bobbleheads

    /**
     * hbm-space's GWEN, JUICE, JAMESH_2 and MRKIMKIMORA: one item each on {@code ntmspace:bobblehead}, the type kept by the
     * placed block, the 1.7 credits verbatim; hazards: hbm-space metadata 22 (JUICE) asbestos 1, 25 (MICROWAVE) glitch 1,
     * {@code sat_gerald} autism 1.
     */
    private static void bobbles(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        var types = com.ntmspace.fork.bobble.BlockSpaceBobble.Type.VALUES;
        String[] ids = {"ntmspace:bobblehead_gwen", "ntmspace:bobblehead_juice", "ntmspace:bobblehead_jamesh_2", "ntmspace:bobblehead_mrkimkimora"};
        for (int i = 0; i < ids.length; i++) {
            String id = BuiltInRegistries.ITEM.getKey(ForkContent.BOBBLEHEAD_ITEMS.get(i).get()).toString();
            if (!id.equals(ids[i])) errors.add("bobble item " + i + " is " + id + ", expected " + ids[i]);
            if (ForkContent.BOBBLEHEAD_ITEMS.get(i).get().variant() != i) errors.add(id + " has variant " + ForkContent.BOBBLEHEAD_ITEMS.get(i).get().variant());
        }
        if (!types[0].contribution.equals("Numero Uno Homie") || !types[2].inscription.equals("COME ON AND SLAM")
                || !types[3].label.equals("MrKimkimora") || types[3].skinLayers || !types[1].skinLayers) {
            errors.add("bobble credits differ from hbm-space");
        }
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack juice = new ItemStack(ForkContent.BOBBLEHEAD_ITEMS.get(1).get());
        level.setBlock(at, ForkContent.BOBBLEHEAD.get().defaultBlockState(), 3);
        ForkContent.BOBBLEHEAD.get().setPlacedBy(level, at, level.getBlockState(at), player, juice);
        if (!(level.getBlockEntity(at) instanceof com.ntmspace.fork.bobble.BlockSpaceBobble.Bobble bobble)
                || bobble.type() != com.ntmspace.fork.bobble.BlockSpaceBobble.Type.JUICE) {
            errors.add("placing bobblehead_juice did not make a JUICE figure");
        }
        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        hazard(errors, "ntmspace:bobblehead_juice", com.hbm.hazard.HazardRegistry.ASBESTOS, 1F);
        hazard(errors, "hbm:bobblehead_microwave", com.ntmspace.fork.bobble.ForkHazards.GLITCH, 1F);
        hazard(errors, "hbm:sat_gerald", com.ntmspace.fork.bobble.ForkHazards.AUTISM, 1F);
        finish(helper, errors);
    }

    private static void hazard(List<String> errors, String id, com.hbm.hazard.type.IHazardType type, float expected) {
        var item = BuiltInRegistries.ITEM.getOptional(Ids.parse(id));
        if (item.isEmpty()) {
            errors.add(id + " is not registered");
            return;
        }
        double level = com.hbm.hazard.HazardSystem.getHazardLevelFromStack(new ItemStack(item.get()), type);
        if (Math.abs(level - expected) > 1e-4) errors.add(id + " " + type + " = " + level + ", hbm-space " + expected);
    }

    /**
     * hbm-space {@code TileEntityMachineShredder.canProcess}: a Gwen bobblehead among the inputs breaks the shredder and
     * explodes; any other input is shredded (Juicy_Lad to the blank-board plastic scrap).
     */
    private static void gwenShredder(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
        Block shredderBlock = block("hbm:machine_shredder");
        level.setBlock(at, shredderBlock.defaultBlockState(), 3);
        if (!(level.getBlockEntity(at) instanceof com.hbm.tileentity.machine.TileEntityMachineShredder shredder)) {
            errors.add("no shredder block entity");
            finish(helper, errors);
            return;
        }
        ItemStack blade = new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.parse("hbm:blades_steel")).orElseThrow());
        shredder.setItem(27, blade.copy());
        shredder.setItem(28, blade.copy());
        shredder.setItem(0, new ItemStack(ForkContent.BOBBLEHEAD_ITEMS.get(1).get()));
        if (!shredder.canProcess()) errors.add("a Juicy_Lad bobblehead does not shred");
        var out = com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(new ItemStack(ForkContent.BOBBLEHEAD_ITEMS.get(1).get()));
        if (!BuiltInRegistries.ITEM.getKey(out.getItem()).toString().equals("hbm:scrap_plastic_board_blank")) {
            errors.add("Juicy_Lad shreds to " + out);
        }
        shredder.setItem(0, new ItemStack(ForkContent.BOBBLEHEAD_ITEMS.get(0).get()));
        boolean can = shredder.canProcess();
        if (can) errors.add("a Gwen bobblehead was shredded");
        if (level.getBlockState(at).is(shredderBlock)) errors.add("the shredder survived Gwen");
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ effects

    /**
     * Slippery's corrected idle rule preserves rest (0 -> 0); moving retains the authored pull (0.2 -> 0.24).
     * Nitan counts as hazmat.
     */
    private static void slippery(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 2, 2));
        pig.setNoAi(true);
        pig.zza = 0F;
        pig.xxa = 0F;
        pig.setDeltaMovement(0, 0, 0);
        com.ntmspace.fork.Slippery.slide(pig);
        if (Math.abs(pig.getDeltaMovement().x) > 1e-12 || Math.abs(pig.getDeltaMovement().z) > 1e-12) {
            errors.add("slide at rest gave " + pig.getDeltaMovement());
        }
        pig.zza = 1F;
        pig.setDeltaMovement(0.2, 0, 0);
        com.ntmspace.fork.Slippery.slide(pig);
        if (Math.abs(pig.getDeltaMovement().x - 0.24) > 1e-9) errors.add("slide moving gave " + pig.getDeltaMovement());
        if (com.hbm.handler.ArmorUtil.checkForHazmat(pig)) errors.add("a bare pig counts as hazmat");
        dev.golem.core.world.Effects.addRegistered(pig, ForkContent.NITAN_EFFECT.get(), 100, 0);
        if (!com.hbm.handler.ArmorUtil.checkForHazmat(pig)) errors.add("nitan does not count as hazmat");
        pig.discard();
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ bombers, cassettes

    /**
     * hbm-space {@code statFacCV} (timer 200, drops 75..125 every 90000 ticks, i.e. never) and {@code statFacCarpetJet}
     * (timer 150, 10..70 every 3, 1.5 times the speed), each with its airframe, saved and loaded by name; the airliner
     * designator's text; the jet bomblet registered.
     */
    private static void bombers(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        var airliner = com.ntmspace.fork.ForkBombers.AIRLINER.create(level, at.getX(), at.getY(), at.getZ());
        if (!java.util.Arrays.equals(airliner.timing(), new int[] {75, 125, 90000}) || airliner.timer != 200
                || !airliner.getSkin().equals(com.ntmspace.fork.ForkBombers.AIRLINER_SKIN)) {
            errors.add("airliner " + java.util.Arrays.toString(airliner.timing()) + " timer " + airliner.timer + " skin " + airliner.getSkin());
        }
        if (Math.abs(airliner.getY() - (at.getY() + 16 + 50)) > 1e-6) errors.add("airliner height " + airliner.getY());
        var jet = com.ntmspace.fork.ForkBombers.CARPET_JET.create(level, at.getX(), at.getY(), at.getZ());
        double speed = Math.sqrt(jet.getDeltaMovement().x * jet.getDeltaMovement().x + jet.getDeltaMovement().z * jet.getDeltaMovement().z);
        double base = com.hbm.config.GeneralConfig.enableBomberShortMode ? 1D : 2D;
        if (!java.util.Arrays.equals(jet.timing(), new int[] {10, 70, 3}) || jet.timer != 150 || Math.abs(speed - 1.5 * base) > 1e-6
                || !jet.getSkin().equals(com.ntmspace.fork.ForkBombers.B2X_SKIN)) {
            errors.add("carpet jet " + java.util.Arrays.toString(jet.timing()) + " timer " + jet.timer + " speed " + speed);
        }
        if (com.hbm.entity.projectile.EntityBombletZeta.KINDS.get("ntmspace:jet") == null) errors.add("no ntmspace:jet bomblet");
        if (!jet.payload().equals("ntmspace:carpet_jet")) errors.add("carpet jet payload " + jet.payload());
        var caller = ForkContent.BOMB_CALLER_AIRLINER.get();
        if (!caller.isFoil(new ItemStack(caller))) errors.add("the airliner designator does not glint");
        airliner.discard();
        jet.discard();
        finish(helper, errors);
    }

    /** hbm-space {@code TrackType.DISEMBODIED} ("Ducc", loop, 0xb3a8c1, 50) and {@code SUICIDE} ("Xbox Live", loop, 70). */
    private static void cassettes(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var ducc = com.hbm.items.machine.ItemCassette.TrackType.byName("ntmspace:DISEMBODIED");
        var mama = com.hbm.items.machine.ItemCassette.TrackType.byName("ntmspace:SUICIDE");
        if (ducc == null || !ducc.getTrackTitle().equals("Ducc") || ducc.getVolume() != 50 || ducc.getColor() != 0xb3a8c1
                || ducc.getType() != com.hbm.items.machine.ItemCassette.SoundType.LOOP) errors.add("DISEMBODIED track");
        if (mama == null || !mama.getTrackTitle().equals("Xbox Live") || mama.getVolume() != 70) errors.add("SUICIDE track");
        if (ForkContent.SIREN_TRACK_DISEMBODIED.get().track != ducc || ForkContent.SIREN_TRACK_SUICIDE.get().track != mama) {
            errors.add("cassette items carry the wrong tracks");
        }
        if (BuiltInRegistries.SOUND_EVENT.getOptional(Ids.of("ntmspace", "alarm.ducc")).isEmpty()) errors.add("no ntmspace:alarm.ducc sound");
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElseThrow();
    }

    static void finish(GameTestHelper helper, List<String> errors) {
        if (!errors.isEmpty()) {
            String message = String.join("; ", errors);
            com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
            return;
        }
        helper.succeed();
    }
}
