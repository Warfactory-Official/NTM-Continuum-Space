package com.ntmspace.gametest;

import com.hbm.api.computer.ComputerComponents;
import com.hbm.blocks.generic.BlockMetalFence;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.block.generic.BlockAlgaeFilm;
import com.ntmspace.block.generic.BlockFenceGate;
import com.ntmspace.block.generic.BlockSpacePlushie;
import com.ntmspace.block.generic.BrownBarrel;
import com.ntmspace.registry.LeftoverContent;
import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.tileentity.machine.StardarComputer;
import com.ntmspace.tileentity.machine.TileEntityAlgaeFilm;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GameTests of the M3 leftovers against hbm-space: registrations and the two assembly rows ({@code ass.orrery},
 * {@code ass.algaefilm}); the orrery's squeezed orbit (Moho's, worked by hand from 1.7's formula); the fence gate's
 * shapes and HBM's chain-link fence joining it; the algae film's step and ports; the oil barrel's drops and oil spill;
 * the plushies (butter turns Teto into FAT TETO, drops by type); the wild flowers' drops, ground and bone meal; the
 * StarDar computer component's three methods.
 */
public final class LeftoverGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function REGISTERED = TESTS.function("leftover_registered_and_recipes", 20,
            () -> LeftoverGameTests::registered);
    public static final GameTests.Function ORRERY = TESTS.function("leftover_orrery_orbits", 20, () -> LeftoverGameTests::orrery);
    public static final GameTests.Function FENCE_GATE = TESTS.function("leftover_fence_gate", 20, 2, () -> LeftoverGameTests::fenceGate);
    public static final GameTests.Function ALGAE_FILM = TESTS.function("leftover_algae_film", 20, 2, () -> LeftoverGameTests::algaeFilm);
    public static final GameTests.Function OIL_BARREL = TESTS.function("leftover_oil_barrel", 60, 6, () -> LeftoverGameTests::oilBarrel);
    public static final GameTests.Function PLUSHIE = TESTS.function("leftover_plushie_butter", 20, 2, () -> LeftoverGameTests::plushie);
    public static final GameTests.Function FLOWERS = TESTS.function("leftover_wild_flowers", 20, 2, () -> LeftoverGameTests::flowers);
    public static final GameTests.Function FLOWER_WORLDGEN = TESTS.function("leftover_wild_flower_worldgen", 20, 10,
            () -> LeftoverGameTests::flowerWorldgen);
    public static final GameTests.Function STARDAR = TESTS.function("leftover_stardar_computer", 20, 4, () -> LeftoverGameTests::stardar);

    private LeftoverGameTests() {}

    public static void init() {}

    // ---------------------------------------------------------------------------------------------------------------

    private static void registered(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (String id : new String[] {"orrery", "fence_gate", "oil_barrel", "algae_film", "plushie", "plant_flower_strawberry",
                "plant_flower_mint"}) {
            if (BuiltInRegistries.BLOCK.getOptional(Ids.of("ntmspace", id)).isEmpty()) errors.add("block ntmspace:" + id);
        }
        for (String id : new String[] {"orrery", "fence_gate", "oil_barrel", "algae_film", "plushie_teto", "plushie_miku",
                "plushie_neru", "plushie_fato", "plant_flower_strawberry", "plant_flower_mint", "beryllium_mirror"}) {
            if (BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", id)).isEmpty()) errors.add("item ntmspace:" + id);
        }
        if (LeftoverContent.PLUSHIE.get().asItem() != LeftoverContent.PLUSHIE_ITEMS.get(0).get()) errors.add("plushie asItem is not Teto");

        AssemblyMachineRecipes table = AssemblyMachineRecipes.INSTANCE;
        table.registerRecipes();
        // hbm-space AssemblyMachineRecipes: ass.orrery setup(400, 100), 16 any glass, 12 advanced circuits, 2,000 mB tritium
        row(errors, table.getRecipe("ass.orrery"), 400, 100, "ntmspace:orrery", new int[] {16, 12}, 2_000);
        // ass.algaefilm setup(100, 100): 8 aluminium plates, 16 saltleaf, a fan, 4 steel beams, 2 chain-link fences
        row(errors, table.getRecipe("ass.algaefilm"), 100, 100, "ntmspace:algae_film", new int[] {8, 16, 1, 4, 2}, -1);
        // ass.dysonmember (the Dyson leftover): it exists once swarm members, beryllium mirrors and stainless plates do
        if (table.getRecipe("ass.dysonmember") == null) errors.add("ass.dysonmember missing");
        MachineGameTests.finish(helper, errors);
    }

    private static void row(List<String> errors, GenericRecipe row, int duration, long power, String output, int[] counts, int fluid) {
        if (row == null) {
            errors.add(output + " assembly row missing");
            return;
        }
        ItemStack out = row.outputItem != null && row.outputItem.length == 1 ? row.outputItem[0] : ItemStack.EMPTY;
        int[] got = Arrays.stream(row.normalInputItems()).mapToInt(s -> s.stacksize).toArray();
        int gotFluid = row.inputFluid == null || row.inputFluid.length == 0 ? -1 : row.inputFluid[0].fill;
        if (row.duration != duration || row.power != power || !output.equals(id(out)) || !Arrays.equals(got, counts) || gotFluid != fluid) {
            errors.add(row.getInternalName() + ": " + row.duration + "/" + row.power + " -> " + id(out) + " inputs "
                    + Arrays.toString(got) + " fluid " + gotFluid);
        }
    }

    /**
     * 1.7 {@code calculatePositionsRecursiveOrrery} for Kerbol's first satellite, Moho: distance = min(261,600, 20,000)
     * * 1.42 + 2,000 * ((1 + 0.2f) * 2) * 1.42 = 35,216.000271 km; the first path point is the periapsis at that
     * semi-major axis, |p| = a * (1 - 0.2f) = 28,172.800112 (rotations keep the length). 31 bodies besides the star.
     */
    private static void orrery(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        List<SolarSystem.OrreryMetric> metrics = SolarSystem.calculatePositionsOrrery(0, 0F, SolarSystem.KERBOL);
        if (metrics.size() != 31) errors.add("metrics " + metrics.size());
        SolarSystem.OrreryMetric moho = metrics.isEmpty() ? null : metrics.get(0);
        if (moho == null || !"moho".equals(moho.body.name)) {
            errors.add("first metric " + (moho == null ? "none" : moho.body.name));
        } else {
            double[] p = moho.orbitalPath[0];
            double length = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            if (Math.abs(length - 28172.800111722947) > 1e-6) errors.add("moho periapsis " + length);
            if (moho.orbitalPath.length != 32) errors.add("path points " + moho.orbitalPath.length);
            // its own position lies on its own (squeezed) orbit: between periapsis and apoapsis
            double r = Math.sqrt(moho.position[0] * moho.position[0] + moho.position[1] * moho.position[1] + moho.position[2] * moho.position[2]);
            if (r < 28172.8 || r > 35216.000270843506 * 1.2000000476837158 + 1e-6) errors.add("moho radius " + r);
        }
        // moons sit around their parent: Mun's first point is Kerbin's position plus a periapsis of its own
        SolarSystem.OrreryMetric kerbin = find(metrics, "kerbin");
        SolarSystem.OrreryMetric mun = find(metrics, "mun");
        if (kerbin == null || mun == null || metrics.indexOf(mun) != metrics.indexOf(kerbin) + 1) errors.add("mun does not follow kerbin");
        MachineGameTests.finish(helper, errors);
    }

    private static SolarSystem.OrreryMetric find(List<SolarSystem.OrreryMetric> metrics, String name) {
        for (SolarSystem.OrreryMetric metric : metrics) {
            if (metric.body.name.equals(name)) return metric;
        }
        return null;
    }

    /** 1.7 {@code BlockFenceGate.func_150011_b}: the closed panel across the doorway, no collision open, the fence joins it. */
    private static void fenceGate(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos lower = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState closed = LeftoverContent.FENCE_GATE.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
        helper.getLevel().setBlock(lower, closed, 3);
        helper.getLevel().setBlock(lower.above(), closed.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);

        VoxelShape shape = closed.getShape(helper.getLevel(), lower, CollisionContext.empty());
        AABB box = shape.bounds();
        if (Math.abs(box.minX - 0.46875) > 1e-9 || Math.abs(box.maxX - 0.53125) > 1e-9 || box.minZ != 0 || box.maxZ != 1) {
            errors.add("east panel " + box);
        }
        BlockState north = closed.setValue(DoorBlock.FACING, Direction.NORTH);
        AABB nbox = north.getShape(helper.getLevel(), lower, CollisionContext.empty()).bounds();
        if (Math.abs(nbox.minZ - 0.46875) > 1e-9 || nbox.minX != 0 || nbox.maxX != 1) errors.add("north panel " + nbox);
        if (closed.getCollisionShape(helper.getLevel(), lower).isEmpty()) errors.add("closed gate has no collision");

        BlockState open = closed.setValue(DoorBlock.OPEN, true);
        if (!open.getCollisionShape(helper.getLevel(), lower).isEmpty()) errors.add("open gate collides");
        if (!open.getShape(helper.getLevel(), lower, CollisionContext.empty()).bounds().equals(box)) errors.add("open outline is not the closed panel");

        // HBM's chain-link fence joins the gate (hbm-space BlockMetalFence.canConnectFenceTo, tag hbm:metal_fence_connects)
        Block fence = BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", "fence_metal")).orElse(Blocks.AIR);
        if (!(fence instanceof BlockMetalFence metal)) {
            errors.add("hbm:fence_metal is not a BlockMetalFence");
        } else if (!metal.canConnectFenceTo(helper.getLevel(), lower, Direction.EAST)) {
            errors.add("chain-link fence does not join the gate");
        }
        if (!closed.is(BlockMetalFence.Connects.TAG)) errors.add("gate not in hbm:metal_fence_connects");
        if (!(closed.getBlock() instanceof BlockFenceGate)) errors.add("not a BlockFenceGate");
        MachineGameTests.finish(helper, errors);
    }

    /** hbm-space {@code TileEntityAlgaeFilm.updateEntity}: one mB CO2 to one mB oxygen per lucky orbit tick; side ports. */
    private static void algaeFilm(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, LeftoverContent.ALGAE_FILM.get().defaultBlockState().setValue(BlockAlgaeFilm.FACING, Direction.NORTH), 3);
        TileEntityAlgaeFilm film = GameTestHelpers.blockEntity(helper, new BlockPos(1, 1, 1), TileEntityAlgaeFilm.class);
        film.tanks[0].setFill(1000);
        film.step(helper.getLevel(), false, true);
        if (film.canOperate || film.tanks[0].getFill() != 1000 || film.tanks[1].getFill() != 0) errors.add("worked outside orbit");
        film.step(helper.getLevel(), true, false);
        if (!film.canOperate || film.tanks[0].getFill() != 1000) errors.add("worked on an unlucky tick");
        film.step(helper.getLevel(), true, true);
        if (film.tanks[0].getFill() != 999 || film.tanks[1].getFill() != 1) {
            errors.add("orbit step " + film.tanks[0].getFill() + "/" + film.tanks[1].getFill());
        }
        film.tanks[1].setFill(film.tanks[1].getMaxFill());
        film.step(helper.getLevel(), true, true);
        if (film.tanks[0].getFill() != 999) errors.add("converted into a full oxygen tank");
        // NORTH (2).getRotation(UP) = EAST (5, 1.7 ROTATION_MATRIX[UP]), and WEST: the two sides beside the front
        ForgeDirection[] ports = TileEntityAlgaeFilm.ports(film.getBlockState());
        if (ports[0] != ForgeDirection.EAST || ports[1] != ForgeDirection.WEST) errors.add("ports " + Arrays.toString(ports));
        if (film.receivesOn(ForgeDirection.NORTH) || !film.receivesOn(ForgeDirection.EAST) || film.providesOn(ForgeDirection.UP)) {
            errors.add("port roles");
        }
        MachineGameTests.finish(helper, errors);
    }

    /** hbm-space {@code BrownBarrel}: drops itself; every 20 ticks {@code OilSpot.generateOilSpot(x, z, 4, 10)} soils grass. */
    private static void oilBarrel(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos centre = helper.absolutePos(GameTests.paddedCentre(6, 1));
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                // a floor under the grass: dead and oily dirt are falling blocks, and below 26.1 the padded test
                // template's bottom layer is air, so a soiled block fell out of the scanned layer
                helper.getLevel().setBlock(centre.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
                helper.getLevel().setBlock(centre.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }
        BlockPos barrel = centre.above();
        helper.getLevel().setBlock(barrel, LeftoverContent.OIL_BARREL.get().defaultBlockState(), 3);
        List<ItemStack> drops = Block.getDrops(LeftoverContent.OIL_BARREL.get().defaultBlockState(), helper.getLevel(), barrel, null,
                null, new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.minecraft("iron_pickaxe")).orElseThrow()));
        if (drops.size() != 1 || !drops.get(0).is(LeftoverContent.OIL_BARREL_ITEM.get())) errors.add("drops " + drops);
        // ten Gaussian picks (sigma 4) per 20 ticks: after 40 ticks some grass in the 13x13 is dead or oily dirt
        helper.runAfterDelay(45, () -> {
            int soiled = 0;
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    Block b = helper.getLevel().getBlockState(centre.offset(dx, 0, dz)).getBlock();
                    String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
                    if (path.equals("dirt_dead") || path.equals("dirt_oily")) soiled++;
                }
            }
            BrownBarrel.Barrel be = helper.getLevel().getBlockEntity(barrel) instanceof BrownBarrel.Barrel b ? b : null;
            if (soiled == 0) {
                errors.add("no soiled ground after 45 ticks (age " + (be == null ? "no block entity" : be.age) + ", surface "
                        + helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        centre.getX() + 2, centre.getZ() + 2) + " at y " + centre.getY() + ")");
            }
            MachineGameTests.finish(helper, errors);
        });
    }

    /** hbm-space {@code BlockPlushie.onBlockActivated}: butter on Teto makes FAT TETO (one butter used); drops by type. */
    private static void plushie(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, LeftoverContent.PLUSHIE.get().defaultBlockState(), 3);
        BlockSpacePlushie.Plushie plushie = GameTestHelpers.blockEntity(helper, new BlockPos(1, 1, 1), BlockSpacePlushie.Plushie.class);
        plushie.setTypeOrdinal(BlockSpacePlushie.Type.MIKU.ordinal());
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        Item butterItem = BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", "butter")).orElse(null);
        if (butterItem == null) {
            errors.add("ntmspace:butter missing");
        } else {
            ItemStack butter = new ItemStack(butterItem, 2);
            if (BlockSpacePlushie.feed(helper.getLevel(), pos, player, butter)) errors.add("Miku took butter");
            plushie.setTypeOrdinal(BlockSpacePlushie.Type.TETO.ordinal());
            if (!BlockSpacePlushie.feed(helper.getLevel(), pos, player, butter)) errors.add("Teto refused butter");
            if (plushie.type() != BlockSpacePlushie.Type.FATO) errors.add("type " + plushie.type());
            if (butter.getCount() != 1) errors.add("butter left " + butter.getCount());
            if (BlockSpacePlushie.feed(helper.getLevel(), pos, player, butter)) errors.add("FAT TETO took butter");
        }
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, plushie, player, ItemStack.EMPTY);
        if (drops.size() != 1 || !drops.get(0).is(LeftoverContent.PLUSHIE_ITEMS.get(BlockSpacePlushie.Type.FATO.ordinal()).get())) {
            errors.add("drops " + drops);
        }
        MachineGameTests.finish(helper, errors);
    }

    /** hbm-space {@code BlockNTMFlower} STRAWBERRY/MINT: their harvest, the flower ground, bone meal drops the harvest. */
    private static void flowers(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        Item strawberry = BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", "strawberry")).orElseThrow();
        Item mint = BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", "mint_leaves")).orElseThrow();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        BlockPos pos = ground.above();
        for (Object[] row : new Object[][] {{LeftoverContent.PLANT_FLOWER_STRAWBERRY.get(), strawberry},
                {LeftoverContent.PLANT_FLOWER_MINT.get(), mint}}) {
            Block flower = (Block) row[0];
            Item harvest = (Item) row[1];
            List<ItemStack> drops = Block.getDrops(flower.defaultBlockState(), helper.getLevel(), pos, null, null, ItemStack.EMPTY);
            if (drops.size() != 1 || !drops.get(0).is(harvest) || drops.get(0).getCount() != 1) errors.add(flower + " drops " + drops);
            if (!flower.defaultBlockState().canSurvive(helper.getLevel(), pos)) errors.add(flower + " refuses grass");
            if (flower.defaultBlockState().canSurvive(helper.getLevel(), ground.above(3))) errors.add(flower + " stays on air");
        }
        // bone meal: 1.7 dropBlockAsItem(meta) - one strawberry beside the flower, the flower stays
        helper.getLevel().setBlock(pos, LeftoverContent.PLANT_FLOWER_STRAWBERRY.get().defaultBlockState(), 3);
        LeftoverContent.PLANT_FLOWER_STRAWBERRY.get().performBonemeal(helper.getLevel(), helper.getLevel().getRandom(), pos,
                helper.getLevel().getBlockState(pos));
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2));
        if (items.size() != 1 || !items.get(0).getItem().is(strawberry)) errors.add("bone meal dropped " + items);
        if (!helper.getLevel().getBlockState(pos).is(LeftoverContent.PLANT_FLOWER_STRAWBERRY.get())) errors.add("bone meal removed the flower");
        for (ItemEntity item : items) item.discard();
        MachineGameTests.finish(helper, errors);
    }

    /**
     * The wild flowers' Earth generation: the plains carry {@code ntmspace:wild_strawberry} then {@code ntmspace:wild_mint}
     * at vegetal decoration (the biome modifier ran), and the strawberry feature (HBM's {@code legacy_flowers}, 1.7
     * {@code WorldGenFlowers}: 64 tries around the origin) plants wild strawberries on a grass field.
     */
    private static void flowerWorldgen(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(GameTests.paddedCentre(10, 1));
        var biome = level.getBiome(origin);
        String biomeId = biome.unwrapKey().map(k -> Ids.keyId(k).toString()).orElse("?");
        net.minecraft.core.Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> strawberry = null;
        List<net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.placement.PlacedFeature>> steps =
                biome.value().getGenerationSettings().features();
        List<String> vegetal = new ArrayList<>();
        if (steps.size() > 9) {
            for (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> holder : steps.get(9)) {
                String id = holder.unwrapKey().map(k -> Ids.keyId(k).toString()).orElse("");
                vegetal.add(id);
                if (id.equals("ntmspace:wild_strawberry")) strawberry = holder;
            }
        }
        boolean plains = biomeId.equals("minecraft:plains") || biomeId.equals("minecraft:sunflower_plains");
        if (plains) {
            int s = vegetal.indexOf("ntmspace:wild_strawberry"), m = vegetal.indexOf("ntmspace:wild_mint");
            if (s < 0 || m < 0 || m < s) errors.add("plains vegetal decoration lacks the wild flowers in order: " + vegetal);
        } else if (vegetal.contains("ntmspace:wild_strawberry")) {
            errors.add("the wild flowers grow in " + biomeId);
        }
        if (strawberry != null) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) level.setBlock(origin.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
            strawberry.value().feature().value().place(level, level.getChunkSource().getGenerator(),
                    net.minecraft.util.RandomSource.create(4815162342L), origin.above());
            int planted = 0;
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (level.getBlockState(origin.offset(dx, 1, dz)).is(LeftoverContent.PLANT_FLOWER_STRAWBERRY.get())) planted++;
                }
            }
            if (planted == 0) errors.add("the strawberry feature planted nothing");
        } else if (plains) {
            errors.add("no ntmspace:wild_strawberry feature in " + biomeId);
        }
        com.ntmspace.NtmSpace.LOG.info("leftover_wild_flower_worldgen: test biome {}", biomeId);
        MachineGameTests.finish(helper, errors);
    }

    /**
     * hbm-space {@code TileEntityMachineStardar}'s component {@code ntm_stardar}, through HBM's {@code ComputerComponents}
     * (no CC needed): Duna seen from Kerbin, Kerbin's moons, the current body, the not-found pair.
     */
    private static void stardar(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(4, 4));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachinesD.MACHINE_STARDAR.get(), Direction.NORTH);
        TileEntityMachineStardar stardar = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineStardar.class);
        ComputerComponents.Component<?> component = ComputerComponents.of(stardar);
        if (component == null || !StardarComputer.NAME.equals(component.name())) {
            errors.add("no ntm_stardar component");
            MachineGameTests.finish(helper, errors);
            return;
        }
        if (!List.copyOf(component.methods().keySet()).equals(List.of("getPlanetStats", "getCurrentPlanet", "getSatellites"))) {
            errors.add("methods " + component.methods().keySet());
        }
        if (!ComputerComponents.carries(SpaceMachinesD.MACHINE_STARDAR.get())) errors.add("stardar block not carried");

        Object[] duna = component.call(stardar, "getPlanetStats", "duna");
        // name, parent, star, tidally locked to, tilt, landable, mass, processing level from Kerbin, radius, semi-major
        // axis; then sun power (1 / (20,726,155 / 13,599,840)^2), gravity (G * m / r^2) and the two periods
        if (duna.length != 14 || !"duna".equals(duna[0]) || !"kerbol".equals(duna[1]) || !"kerbol".equals(duna[2])
                || !"ike".equals(duna[3]) || !Float.valueOf(0F).equals(duna[4]) || !Boolean.TRUE.equals(duna[5])
                || !Float.valueOf(4.515e+21F).equals(duna[6]) || !Integer.valueOf(1).equals(duna[7])
                || !Float.valueOf(320.0F).equals(duna[8]) || !Float.valueOf(20726155.0F).equals(duna[9])) {
            errors.add("duna " + Arrays.toString(duna));
        } else {
            float sun = (Float) duna[10];
            float gravity = (Float) duna[11];
            if (Math.abs(sun - 0.4305F) > 1e-3F) errors.add("duna sun power " + sun);
            if (Math.abs(gravity - 2.9428F) > 1e-3F) errors.add("duna gravity " + gravity);
        }
        Object[] moons = component.call(stardar, "getSatellites", "kerbin");
        if (!Arrays.equals(moons, new Object[] {"mun", "minmus"})) errors.add("kerbin satellites " + Arrays.toString(moons));
        Object[] here = component.call(stardar, "getCurrentPlanet");
        if (!Arrays.equals(here, new Object[] {"kerbin"})) errors.add("current " + Arrays.toString(here));
        Object[] none = component.call(stardar, "getPlanetStats", "earth");
        if (none.length != 2 || none[0] != null || !"No body with that name found.".equals(none[1])) errors.add("unknown " + Arrays.toString(none));
        try {
            component.call(stardar, "getSatellites");
            errors.add("missing argument accepted");
        } catch (IllegalArgumentException expected) {
            // 1.7 checkString(0)
        }
        MachineGameTests.finish(helper, errors);
    }

    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
