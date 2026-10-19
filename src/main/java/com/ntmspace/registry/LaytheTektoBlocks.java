package com.ntmspace.registry;

import com.hbm.blocks.generic.BlockFallingBase;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.block.flora.BlockRubberSilt;
import com.ntmspace.block.flora.BlockSpaceLeaves;
import com.ntmspace.block.flora.TypedBlockItem;
import com.ntmspace.block.flora.TypedItems;
import com.ntmspace.block.terrain.BlockCcl;
import com.ntmspace.block.terrain.BlockGeysirChloric;
import com.ntmspace.block.terrain.BlockLaytheCoral;
import com.ntmspace.block.terrain.BlockLaytheCoralBlock;
import com.ntmspace.block.terrain.BlockLaytheKelp;
import com.ntmspace.block.terrain.BlockLaytheTallPlant;
import com.ntmspace.block.terrain.BlockRubberGrass;
import com.ntmspace.block.terrain.BlockRubberPlant;
import com.ntmspace.block.terrain.BlockRubberTall;
import com.ntmspace.block.terrain.BlockVinylVine;
import com.ntmspace.block.terrain.BlockWaterPlant;
import com.ntmspace.block.terrain.PlantType;
import com.ntmspace.fluid.SpaceWorldFluids;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The blocks Laythe's and Tekto's 1.7 terrain and decoration place, from hbm-space's {@code ModBlocks} (values as 1.7
 * stated them, ids its block names, textures its {@code setBlockTextureName}s), in HBM's blocks tab as 1.7 listed them.
 * Thatmo's terrain uses only HBM's and vanilla's blocks.
 *
 * <p>The flora's behaviour is hbm-space's (drops, leaf decay, the coral's attachment, the tall rubber plant's OBJ, one
 * block item per type through {@link TypedFamilies}); what it needs besides (its items, rubber farmland, crops,
 * saplings, the block entities) is in {@link FloraBlocks}.
 */
public final class LaytheTektoBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    private static final String CUTOUT = "minecraft:cutout";

    // ---------------------------------------------------------------- Laythe terrain

    /** 1.7: {@code BlockFallingTint(Material.sand)}, hardness 0.5, sand sound ("Laythe Silt"; its tint is white at metadata 0). */
    public static final RegistryObject<BlockFallingBase> LAYTHE_SILT = REG.block("laythe_silt", BlockFallingBase::new,
                    p -> p.strength(0.5F).sound(SoundType.SAND))
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> LAYTHE_SILT_ITEM = REG.blockItem(LAYTHE_SILT).tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- Tekto terrain

    /** 1.7: {@code BlockGeneric(Material.sand)}, hardness 1, resistance 1, gravel sound ("Tekto Silt"); a hoe tills it. */
    public static final RegistryObject<BlockRubberSilt> RUBBER_SILT = REG.block("rubber_silt",
                    p -> new BlockRubberSilt(p, FloraBlocks.RUBBER_FARMLAND), p -> p.strength(1.0F, 1.0F).sound(SoundType.GRAVEL))
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> RUBBER_SILT_ITEM = REG.blockItem(RUBBER_SILT).tab(HbmTab.BLOCKS);

    /** 1.7: {@code RubberGrass(Material.grass)}, hardness 0.5, resistance 2.5, grass sound; drops itself; a hoe tills it. */
    public static final RegistryObject<BlockRubberGrass> RUBBER_GRASS = REG.block("rubber_grass",
                    p -> new BlockRubberGrass(p, RUBBER_SILT, FloraBlocks.RUBBER_FARMLAND), p -> p.strength(0.5F, 2.5F).sound(SoundType.GRASS))
            .transform(Presets.cubeBottomTop("ntmspace:blocks/rubber_silt", "ntmspace:blocks/rubber_grass_side",
                    "ntmspace:blocks/rubber_grass_top"))
            .loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> RUBBER_GRASS_ITEM = REG.blockItem(RUBBER_GRASS).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockFalling(Material.sand)}, hardness 1, resistance 1, sand sound, texture {@code sand_vinyl}. */
    public static final RegistryObject<BlockFallingBase> VINYL_SAND = REG.block("vinyl_sand", BlockFallingBase::new,
                    p -> p.strength(1.0F, 1.0F).sound(SoundType.SAND))
            .transform(Presets.cubeAll("ntmspace:blocks/sand_vinyl")).loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> VINYL_SAND_ITEM = REG.blockItem(VINYL_SAND).tab(HbmTab.BLOCKS);

    /** 1.7: {@code GenericFluidBlock(ccl_fluid, Material.water, "ccl", "ccl_flowing")}, resistance 500, Tekto's sea ("CCL"). */
    public static final RegistryObject<BlockCcl> CCL_BLOCK = REG.block("ccl_block", BlockCcl::new,
                    p -> SpaceWorldFluids.properties(p, SpaceWorldFluids.CCL))
            .transform(SpaceWorldFluids.blockstate("ccl"));

    // ore_tekto: registry/BodyOreBlocks owns the per-body ore family


    /**
     * 1.7: {@code BlockGeysierDCM(Material.rock)} ("DCM Geysir"), light 1.0, hardness 2, stone sound, drawn as HBM's
     * basalt; the core of Tekto's vinyl volcanoes. Its block entity is {@link FloraBlocks#GEYSIR_CHLORIC_BE}.
     */
    public static final RegistryObject<BlockGeysirChloric> GEYSIR_CHLORIC = REG.block("geysir_chloric", BlockGeysirChloric::new,
                    p -> p.strength(2.0F).sound(SoundType.STONE).lightLevel(state -> 15).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll("hbm:blocks/basalt")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> GEYSIR_CHLORIC_ITEM = REG.blockItem(GEYSIR_CHLORIC).tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- Laythe flora

    /** 1.7: {@code BlockWaterPlant} ("Laythe Seagrass"), hardness 0, grass sound; drops 0-3 saltleaves (in code). */
    public static final RegistryObject<BlockWaterPlant> LAYTHE_SEAGRASS = REG.block("laythe_seagrass", BlockWaterPlant::new,
                    LaytheTektoBlocks::plant)
            .transform(crop("ntmspace:blocks/laythe_seagrass")).loot(Loot.nothing());
    public static final RegistryObject<BlockItem> LAYTHE_SEAGRASS_ITEM = REG.blockItem(LAYTHE_SEAGRASS).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockWaterPlant} ("Laythe Glowgrass"), light 1.0; drops 0-3 saltleaves (in code). */
    public static final RegistryObject<BlockWaterPlant> LAYTHE_GLOWGRASS = REG.block("laythe_glowgrass", BlockWaterPlant::new,
                    p -> plant(p).lightLevel(state -> 15))
            .transform(crop("ntmspace:blocks/laythe_glowgrass")).loot(Loot.nothing());
    public static final RegistryObject<BlockItem> LAYTHE_GLOWGRASS_ITEM = REG.blockItem(LAYTHE_GLOWGRASS).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockTallPlantWater} ("Saltgrass"), on laythe silt; drops 0-3 saltleaves per half (in code). */
    public static final RegistryObject<BlockLaytheTallPlant> PLANT_TALL_LAYTHE = REG.block("plant_tall_laythe",
                    p -> new BlockLaytheTallPlant(p, LAYTHE_SILT), LaytheTektoBlocks::plant)
            .model(d -> d.models(Models.group()
                            .and("_lower", Models.renderType(CUTOUT, crop17("ntmspace:blocks/plant_tall_laythe.laythe.lower")))
                            .and("_upper", Models.renderType(CUTOUT, crop17("ntmspace:blocks/plant_tall_laythe.laythe.upper"))))
                    .states(Variants.select("half", "lower", "_lower", "upper", "_upper"))
                    .item(Models.generated("ntmspace:blocks/plant_tall_laythe.laythe.lower")))
            .loot(Loot.nothing());
    public static final RegistryObject<BlockItem> PLANT_TALL_LAYTHE_ITEM = REG.blockItem(PLANT_TALL_LAYTHE).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockKelp} ("Laythe Kelp"), on laythe silt or kelp; drops 0-3 saltleaves (in code). */
    public static final RegistryObject<BlockLaytheKelp> LAYTHE_KELP = REG.block("laythe_kelp",
                    p -> new BlockLaytheKelp(p, LAYTHE_SILT), LaytheTektoBlocks::plant)
            .model(d -> d.models(Models.group()
                            .and("", Models.renderType(CUTOUT, Models.cross("ntmspace:blocks/laythe_kelp")))
                            .and("_top", Models.renderType(CUTOUT, Models.cross("ntmspace:blocks/laythe_kelp_top"))))
                    .states(Variants.flag("top", "_top"))
                    .item(Models.generated("ntmspace:blocks/laythe_kelp_top")))
            .loot(Loot.nothing());
    public static final RegistryObject<BlockItem> LAYTHE_KELP_ITEM = REG.blockItem(LAYTHE_KELP).tab(HbmTab.BLOCKS);

    /**
     * 1.7: {@code BlockCoral} ("Laythe Coral"), five types; drops HBM's calcium powder. The cross leans away from its
     * support ({@code facing}, 1.7 {@code RenderCoral}): the blockstate rotates vanilla's cross, drawn pointing up.
     */
    public static final RegistryObject<BlockLaytheCoral> LAYTHE_CORAL = REG.block("laythe_coral", BlockLaytheCoral::new,
                    LaytheTektoBlocks::plant)
            .model(d -> {
                Models.Group group = Models.group();
                List<String> select = new ArrayList<>();
                for (PlantType.Coral type : PlantType.Coral.values()) {
                    String name = type.getSerializedName();
                    group = group.and("_" + name, Models.renderType(CUTOUT, Models.cross("ntmspace:blocks/laythe_coral." + name)));
                    select.add(name);
                    select.add("_" + name);
                }
                return d.models(group)
                        .states(Variants.product(Variants.select("type", select.toArray(new String[0])), Variants.facingUp("facing")))
                        .item(Models.generated("ntmspace:blocks/laythe_coral.tube"));
            })
            .loot(Loot.only("hbm:powder_calcium"));
    public static final List<RegistryObject<TypedBlockItem>> LAYTHE_CORAL_ITEMS = TypedFamilies.items(REG, LAYTHE_CORAL,
            "laythe_coral", PlantType.Coral.values(), null, true,
            type -> d -> d.models(Models.generated("ntmspace:blocks/laythe_coral." + type)), HbmTab.BLOCKS);

    /** 1.7: {@code BlockEnumMulti(Material.coral, EnumCoral)} ("Block of Laythe Coral"), hardness 0.5, grass sound. */
    public static final RegistryObject<BlockLaytheCoralBlock> LAYTHE_CORAL_BLOCK = REG.block("laythe_coral_block",
                    BlockLaytheCoralBlock::new, p -> p.strength(0.5F).sound(SoundType.GRASS))
            .model(d -> typedCubes(d, PlantType.Coral.values(), type -> "ntmspace:blocks/laythe_coral_block." + type))
            .loot(Loot.self());
    public static final List<RegistryObject<TypedBlockItem>> LAYTHE_CORAL_BLOCK_ITEMS = TypedFamilies.items(REG,
            LAYTHE_CORAL_BLOCK, "laythe_coral_block", PlantType.Coral.values(), null, true,
            type -> d -> d.models(Models.blockItem("ntmspace:block/laythe_coral_block_" + type)), HbmTab.BLOCKS);

    // ---------------------------------------------------------------- Tekto flora

    /** 1.7: {@code BlockRubberPlant} ("Short Rubber Plant"), three types, on rubber grass, silt or farmland; drops nothing. */
    public static final RegistryObject<BlockRubberPlant> RUBBER_PLANT = REG.block("rubber_plant",
                    p -> new BlockRubberPlant(p, below -> below.is(RUBBER_GRASS.get()) || below.is(RUBBER_SILT.get())
                            || below.is(FloraBlocks.RUBBER_FARMLAND.get())),
                    LaytheTektoBlocks::plant)
            .model(d -> typed(d, PlantType.RubberPlant.values(), type -> Models.renderType(CUTOUT,
                    Models.cross("ntmspace:blocks/rubber_plant." + type)), "ntmspace:blocks/rubber_plant.grass"))
            .loot(Loot.nothing());
    public static final List<RegistryObject<TypedBlockItem>> RUBBER_PLANT_ITEMS = TypedFamilies.items(REG, RUBBER_PLANT,
            "rubber_plant", PlantType.RubberPlant.values(), null, true,
            type -> d -> d.models(Models.generated("ntmspace:blocks/rubber_plant." + type)), HbmTab.BLOCKS);

    /**
     * 1.7: {@code BlockRubberCacti(Material.grass)} as {@code spike_cacti}, block name {@code rubber_tall} ("Tall Rubber
     * Plant"), three types, on vinyl sand, rubber grass, silt or farmland; drops itself, the flower chlorinated seeds (in
     * code). Drawn by its block entity's renderer (the OBJ is three blocks tall); its models only carry the particle.
     */
    public static final RegistryObject<BlockRubberTall> RUBBER_TALL = REG.block("rubber_tall",
                    p -> new BlockRubberTall(p, below -> below.is(VINYL_SAND.get()) || below.is(RUBBER_GRASS.get())
                            || below.is(RUBBER_SILT.get()) || below.is(FloraBlocks.RUBBER_FARMLAND.get())),
                    LaytheTektoBlocks::plant)
            .model(d -> typed(d, PlantType.RubberTall.values(), type -> Models.parented("hbm:block/machine_dummy_empty",
                    Slots.of("particle", "ntmspace:blocks/rubber_tall." + type)), "ntmspace:blocks/rubber_tall.cact"))
            .loot(Loot.nothing());
    public static final List<RegistryObject<TypedBlockItem>> RUBBER_TALL_ITEMS = TypedFamilies.items(REG, RUBBER_TALL,
            "rubber_tall", PlantType.RubberTall.values(), null, true,
            type -> d -> d.models(Models.generated("ntmspace:blocks/rubber_tall." + type)), HbmTab.BLOCKS);

    /** 1.7: {@code BlockLogNT} ("PVC Log"), hardness 0.5, resistance 2.5, wood sound. */
    public static final RegistryObject<RotatedPillarBlock> PVC_LOG = REG.block("pvc_log", RotatedPillarBlock::new,
                    LaytheTektoBlocks::log)
            .transform(Presets.column("ntmspace:blocks/pvc_log_top", "ntmspace:blocks/pvc_log_side").states(Variants.axis()))
            .loot(Loot.self()).tag(BlockTag.AXE).tag(BlockTag.of("minecraft:logs"));
    public static final RegistryObject<BlockItem> PVC_LOG_ITEM = REG.blockItem(PVC_LOG).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockLogNT} ("Hardened Vinyl Log"), hardness 0.5, resistance 2.5, wood sound. */
    public static final RegistryObject<RotatedPillarBlock> VINYL_LOG = REG.block("vinyl_log", RotatedPillarBlock::new,
                    LaytheTektoBlocks::log)
            .transform(Presets.column("ntmspace:blocks/vinyl_log_top", "ntmspace:blocks/vinyl_log_side").states(Variants.axis()))
            .loot(Loot.self()).tag(BlockTag.AXE).tag(BlockTag.of("minecraft:logs"));
    public static final RegistryObject<BlockItem> VINYL_LOG_ITEM = REG.blockItem(VINYL_LOG).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockRubberLeaves} ("Rubber Leaves"), hardness 0.5, resistance 2.5; drops rubber leaves and PVC saplings. */
    public static final RegistryObject<BlockSpaceLeaves> RUBBER_LEAVES = REG.block("rubber_leaves",
                    p -> new BlockSpaceLeaves(p, FloraBlocks.LEAF_RUBBER, () -> TypedItems.item(FloraBlocks.SAPLING.get(), "pvc")),
                    LaytheTektoBlocks::leaves)
            .transform(Presets.shape(Models.renderType("minecraft:cutout_mipped", Models.cubeAll("ntmspace:blocks/rubber_leaves"))))
            .loot(Loot.nothing()).tag(BlockTag.HOE).tag(BlockTag.of("minecraft:leaves"));
    public static final RegistryObject<BlockItem> RUBBER_LEAVES_ITEM = REG.blockItem(RUBBER_LEAVES).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockRubberLeaves} ("Plastic Leaves"), hardness 0.5, resistance 2.5; drops plastic leaves and vinyl saplings. */
    public static final RegistryObject<BlockSpaceLeaves> PET_LEAVES = REG.block("pet_leaves",
                    p -> new BlockSpaceLeaves(p, FloraBlocks.LEAF_PET, () -> TypedItems.item(FloraBlocks.SAPLING.get(), "vinyl")),
                    LaytheTektoBlocks::leaves)
            .transform(Presets.shape(Models.renderType("minecraft:cutout_mipped", Models.cubeAll("ntmspace:blocks/pet_leaves"))))
            .loot(Loot.nothing()).tag(BlockTag.HOE).tag(BlockTag.of("minecraft:leaves"));
    public static final RegistryObject<BlockItem> PET_LEAVES_ITEM = REG.blockItem(PET_LEAVES).tab(HbmTab.BLOCKS);

    /**
     * 1.7: {@code BlockVinylVine} ("Vinyl Vines"), hardness 1, resistance 1, grass sound, texture {@code vine}; drops only
     * when sheared. Its multipart blockstate and model are authored ({@code gradle/dejson-authored.txt}).
     */
    public static final RegistryObject<BlockVinylVine> VINYL_VINE = REG.block("vinyl_vine", BlockVinylVine::new,
                    p -> p.strength(1.0F, 1.0F).sound(SoundType.VINE).noCollision().noOcclusion().replaceable().randomTicks())
            .loot(Loot.nothing());
    public static final RegistryObject<BlockItem> VINYL_VINE_ITEM = REG.blockItem(VINYL_VINE)
            .icon(d -> d.models(Models.generated("ntmspace:blocks/vine"))).tab(HbmTab.BLOCKS);

    private LaytheTektoBlocks() {}

    public static void init() {}

    // ---------------------------------------------------------------- helpers

    /** The 1.7 plants: hardness 0, grass sound, no collision. */
    private static BlockBehaviour.Properties plant(BlockBehaviour.Properties p) {
        return p.strength(0.0F).sound(SoundType.GRASS).noOcclusion().noCollision();
    }

    private static BlockBehaviour.Properties log(BlockBehaviour.Properties p) {
        return p.strength(0.5F, 2.5F).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties leaves(BlockBehaviour.Properties p) {
        return p.strength(0.5F, 2.5F).sound(SoundType.GRASS).noOcclusion()
                .isSuffocating(LaytheTektoBlocks::never).isViewBlocking(LaytheTektoBlocks::never);
    }

    private static boolean never(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return false;
    }

    /** 1.7 render type 6 (the crops shape): vanilla's {@code crop} model. */
    private static Shape crop17(String texture) {
        return Models.parented("minecraft:block/crop", Slots.of("crop", texture));
    }

    /** A crops-shaped cutout plant whose item is its flat sprite. */
    private static Preset crop(String texture) {
        return handle -> handle.model(d -> d.models(Models.renderType(CUTOUT, crop17(texture)))
                .states(Variants.simple()).item(Models.generated(texture)));
    }

    /** A block with a {@code type} state, one model {@code <block>_<type>} each; the item is {@code itemTexture}'s sprite. */
    private static <E extends Enum<E> & StringRepresentable> dev.golem.core.dejson.Dejson.Declaration typed(
            dev.golem.core.dejson.Dejson.Declaration d, E[] types, java.util.function.Function<String, Shape> model,
            String itemTexture) {
        Models.Group group = Models.group();
        List<String> select = new ArrayList<>();
        for (E type : types) {
            String name = type.getSerializedName();
            group = group.and("_" + name, model.apply(name));
            select.add(name);
            select.add("_" + name);
        }
        return d.models(group).states(Variants.select("type", select.toArray(new String[0])))
                .item(Models.generated(itemTexture));
    }

    /** {@link #typed} for full cubes; the item is the first type's block model. */
    private static <E extends Enum<E> & StringRepresentable> dev.golem.core.dejson.Dejson.Declaration typedCubes(
            dev.golem.core.dejson.Dejson.Declaration d, E[] types, java.util.function.Function<String, String> texture) {
        Models.Group group = Models.group();
        List<String> select = new ArrayList<>();
        for (E type : types) {
            String name = type.getSerializedName();
            group = group.and("_" + name, Models.cubeAll(texture.apply(name)));
            select.add(name);
            select.add("_" + name);
        }
        return d.models(group).states(Variants.select("type", select.toArray(new String[0])))
                .item(Models.blockItem(d.baseModel() + "_" + types[0].getSerializedName()));
    }
}
