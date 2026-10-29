package com.ntmspace.registry;

import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.flora.BlockRubberFarmland;
import com.ntmspace.block.flora.BlockSpaceCrop;
import com.ntmspace.block.flora.BlockSpaceLeaves;
import com.ntmspace.block.flora.BlockSpaceSapling;
import com.ntmspace.block.flora.CropKind;
import com.ntmspace.block.flora.ItemSpaceSeedFood;
import com.ntmspace.block.terrain.BlockGeysirChloric;
import com.ntmspace.block.terrain.BlockGeysirElectric;
import com.ntmspace.block.terrain.BlockRubberTall;
import com.ntmspace.block.terrain.BlockVinylVine;
import com.ntmspace.block.terrain.PlantType;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.ItemTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import dev.golem.core.world.CompatItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * NTM: Space's flora and the generic blocks around it that hbm-space added (its {@code ModBlocks}/{@code ModItems}
 * entries; values, ids, textures and tabs as 1.7 stated them). The flora that Laythe's and Tekto's terrain places is
 * registered in {@link LaytheTektoBlocks}; this class holds what the flora needs besides (its items, rubber farmland, the
 * crops, the saplings), the vinyl and PVC woods, the joke logs, astroturf, the discharge geyser, and the block entities
 * of the tall rubber plant and both geysers.
 *
 * <p>Item ids are the 1.7 registry names ({@code getUnlocalizedName}) but for the two leaf items, whose 1.7 names
 * ({@code rubber_leaves}, {@code pet_leaves}) are the leaf blocks' here: they keep their field names
 * ({@code leaf_rubber}, {@code leaf_pet}).
 */
public final class FloraBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES);

    private static final String CUTOUT = "minecraft:cutout";

    // ---------------------------------------------------------------- items (hbm-space ModItems)

    /** 1.7: {@code ItemLemon(2, 0.5F, false)} "Saltleaf", parts tab; furnace fuel 200 ticks ({@code FuelHandler}). */
    public static final RegistryObject<CompatItem> SALTLEAF = food("saltleaf", "salt_leaf", 2, 0.5F, HbmTab.MATERIALS);
    /** 1.7 {@code leaf_rubber}: plain item "Rubber Leaves" (1.7 name {@code rubber_leaves}), parts tab. */
    public static final RegistryObject<Item> LEAF_RUBBER = plain("leaf_rubber", "rubber_leaves", HbmTab.MATERIALS);
    /** 1.7 {@code leaf_pet}: plain item "Plastic Leaves" (1.7 name {@code pet_leaves}), parts tab. */
    public static final RegistryObject<Item> LEAF_PET = plain("leaf_pet", "pet_leaves", HbmTab.MATERIALS);
    /** 1.7: {@code ItemCustomLore} "PVC Stick", parts tab. */
    public static final RegistryObject<Item> STICK_PVC = plain("stick_pvc", "stick_pvc", HbmTab.MATERIALS, ItemTag.of("c:rods/wooden"));
    /** 1.7: {@code ItemCustomLore} "Vinyl Stick", parts tab. */
    public static final RegistryObject<Item> STICK_VINYL = plain("stick_vinyl", "stick_vinyl", HbmTab.MATERIALS, ItemTag.of("c:rods/wooden"));
    /** 1.7: {@code ItemCustomLore} "Rubber Powder" (ore dictionary {@code dustRubber}), parts tab. */
    public static final RegistryObject<Item> POWDER_RUBBER = plain("powder_rubber", "powder_rubber", HbmTab.MATERIALS, ItemTag.of("c:dusts/rubber"));
    /** 1.7: {@code ItemCustomLore} "PVC Powder" (ore dictionary {@code dustPVC}), parts tab. */
    public static final RegistryObject<Item> POWDER_PVC = plain("powder_pvc", "powder_pvc", HbmTab.MATERIALS, ItemTag.of("c:dusts/pvc"));
    /** 1.7: plain item "Balefire Infused Woodemium Briquette", parts tab. */
    public static final RegistryObject<Item> WOODEMIUM_BRIQUETTE = plain("woodemium_briquette", "briquette_woodemium", HbmTab.MATERIALS);

    /** 1.7: {@code ItemSeedFood(3, 0.4F, crop_strawberry, farmland)} "Strawberry" (ore dictionary {@code cropStrawberry}), consumables tab. */
    public static final RegistryObject<ItemSpaceSeedFood> STRAWBERRY = seed("strawberry", "strawberry", 3, 0.4F, CropKind.STRAWBERRY, ItemTag.of("c:crops/strawberry"));
    /** 1.7: {@code ItemSeedFood(3, 0.4F, crop_mint, farmland)} "Mint Leaves", consumables tab. */
    public static final RegistryObject<ItemSpaceSeedFood> MINT_LEAVES = seed("mint_leaves", "mint_leaves", 3, 0.4F, CropKind.MINT, null);
    /** 1.7: {@code ItemSeedFood(1, 0.2F, crop_coffee, farmland)} "Raw Coffee Bean", consumables tab. */
    public static final RegistryObject<ItemSpaceSeedFood> BEAN_RAW = seed("bean_raw", "coffeebeanraw", 1, 0.2F, CropKind.COFFEE, null);
    /** 1.7: plain item "Roasted Coffee Bean", consumables tab. */
    public static final RegistryObject<Item> BEAN_ROAST = plain("bean_roast", "coffeebeanroast", HbmTab.CONSUMABLES);
    /** 1.7: {@code ItemSeedFood(0, 0, crop_tea, farmland)} "Tea Seeds" (edible, for nothing), consumables tab. */
    public static final RegistryObject<ItemSpaceSeedFood> TEASEEDS = seed("teaseeds", "teaseeds", 0, 0.0F, CropKind.TEA, null);
    /** 1.7: plain item "Tea Leaf", consumables tab. */
    public static final RegistryObject<Item> TEA_LEAF = plain("tea_leaf", "tea_leaf", HbmTab.CONSUMABLES);
    /** 1.7: {@code ItemSeedFood(1, 0.2F, crop_paraffin, rubber_farmland)} "Chlorinated Seeds", consumables tab. */
    public static final RegistryObject<ItemSpaceSeedFood> PARAFFIN_SEEDS = seed("paraffin_seeds", "paraffin_seeds", 1, 0.2F, CropKind.PARAFFIN, null);

    // ---------------------------------------------------------------- farmland and crops

    /** 1.7: {@code BlockRubberFarm} "Tekto Farmland", hardness 1, resistance 1, gravel sound, blocks tab; drops rubber silt. */
    public static final RegistryObject<BlockRubberFarmland> RUBBER_FARMLAND = REG.block("rubber_farmland",
                    p -> new BlockRubberFarmland(p, LaytheTektoBlocks.RUBBER_SILT, LaytheTektoBlocks.CCL_BLOCK),
                    p -> p.strength(1.0F, 1.0F).sound(SoundType.GRAVEL).randomTicks())
            .model(d -> d.models(Models.group()
                            .and("", farmland("ntmspace:blocks/rubber_farmland"))
                            .and("_moist", farmland("ntmspace:blocks/rubber_farmland_moist")))
                    .states(Variants.select("moisture", "0", "", "1", "_moist"))
                    .item(Models.blockItem("ntmspace:block/rubber_farmland")))
            .loot(Loot.only("ntmspace:rubber_silt")).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> RUBBER_FARMLAND_ITEM = REG.blockItem(RUBBER_FARMLAND).tab(HbmTab.BLOCKS);

    /** 1.7 {@code BlockCrop}s: hardness 0, grass sound; each crop has its own block item like upstream. */
    private static final Map<CropKind, RegistryObject<BlockSpaceCrop>> CROPS = new EnumMap<>(CropKind.class);

    /** Every crop's block item, in 1.7 order (the vanilla Natural tab lists them after its own crops). */
    public static final List<RegistryObject<BlockItem>> CROP_ITEMS = new ArrayList<>(5);

    public static final RegistryObject<BlockSpaceCrop> CROP_STRAWBERRY = crop(CropKind.STRAWBERRY);
    public static final RegistryObject<BlockSpaceCrop> CROP_MINT = crop(CropKind.MINT);
    public static final RegistryObject<BlockSpaceCrop> CROP_COFFEE = crop(CropKind.COFFEE);
    public static final RegistryObject<BlockSpaceCrop> CROP_TEA = crop(CropKind.TEA);
    public static final RegistryObject<BlockSpaceCrop> CROP_PARAFFIN = crop(CropKind.PARAFFIN);

    // ---------------------------------------------------------------- saplings

    /**
     * 1.7 {@code BlockNTSapling} block name {@code sapling}, split per type the way upstream splits it: the vinyl
     * sapling ("Vinyl Sapling", {@code sapling_vinyl}) and the PVC one ("Plastic Sapling", {@code sapling_pvc}),
     * blocks tab. Each split keeps its own type (placing, drops and pick-block are always its own).
     */
    public static final RegistryObject<BlockSpaceSapling> SAPLING_VINYL = REG.block("sapling_vinyl",
                    p -> new BlockSpaceSapling(p, PlantType.Sapling.VINYL, () -> saplingItem("sapling_vinyl")),
                    p -> p.strength(0.0F).sound(SoundType.GRASS).noOcclusion().noCollision().randomTicks())
            .model(d -> d.models(Models.group().and("_vinyl",
                            Models.renderType(CUTOUT, Models.cross("ntmspace:blocks/sapling_vinyl"))))
                    .states(Variants.select("type", "vinyl", "_vinyl", "pvc", "_vinyl"))
                    .item(Models.generated("ntmspace:blocks/sapling_vinyl")))
            .loot(Loot.nothing());
    public static final RegistryObject<BlockItem> SAPLING_VINYL_ITEM = REG.blockItem(SAPLING_VINYL).tab(HbmTab.BLOCKS);
    public static final RegistryObject<BlockSpaceSapling> SAPLING_PVC = REG.block("sapling_pvc",
                    p -> new BlockSpaceSapling(p, PlantType.Sapling.PVC, () -> saplingItem("sapling_pvc")),
                    p -> p.strength(0.0F).sound(SoundType.GRASS).noOcclusion().noCollision().randomTicks())
            .model(d -> d.models(Models.group().and("_pvc",
                            Models.renderType(CUTOUT, Models.cross("ntmspace:blocks/sapling_pvc"))))
                    .states(Variants.select("type", "vinyl", "_pvc", "pvc", "_pvc"))
                    .item(Models.generated("ntmspace:blocks/sapling_pvc")))
            .loot(Loot.nothing());
    public static final RegistryObject<BlockItem> SAPLING_PVC_ITEM = REG.blockItem(SAPLING_PVC).tab(HbmTab.BLOCKS);

    /** A split sapling's item by id (its drop and pick-block; avoids the item's forward reference). */
    private static Item saplingItem(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.of(NtmSpace.MOD_ID, id));
    }

    // ---------------------------------------------------------------- woods

    /** 1.7: {@code BlockGeneric(Material.wood)} "Vinyl Planks", hardness 1, resistance 1, wood sound (ore dictionary {@code plankWood}). */
    public static final RegistryObject<net.minecraft.world.level.block.Block> VINYL_PLANKS = planks("vinyl_planks");
    public static final RegistryObject<BlockItem> VINYL_PLANKS_ITEM = REG.blockItem(VINYL_PLANKS).tag(ItemTag.of("minecraft:planks")).tab(HbmTab.BLOCKS);
    /** 1.7: {@code BlockGeneric(Material.wood)} "PVC Planks", hardness 1, resistance 1, wood sound (ore dictionary {@code plankWood}). */
    public static final RegistryObject<net.minecraft.world.level.block.Block> PVC_PLANKS = planks("pvc_planks");
    public static final RegistryObject<BlockItem> PVC_PLANKS_ITEM = REG.blockItem(PVC_PLANKS).tag(ItemTag.of("minecraft:planks")).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockLogNT(rad_log_side, rad_log_top)} "Latticed Log", hardness 0.5, resistance 2.5, wood sound. */
    public static final RegistryObject<RotatedPillarBlock> LATTICE_LOG = log("lattice_log", "rad_log");
    public static final RegistryObject<BlockItem> LATTICE_LOG_ITEM = REG.blockItem(LATTICE_LOG).tab(HbmTab.BLOCKS);
    /** 1.7: {@code BlockLogNT} "Balefire Log"; nine woodemium briquettes. */
    public static final RegistryObject<RotatedPillarBlock> BF_LOG = log("bf_log", "bf_log");
    public static final RegistryObject<BlockItem> BF_LOG_ITEM = REG.blockItem(BF_LOG).tab(HbmTab.BLOCKS);
    /** 1.7: {@code BlockLogNT} "Primed Log". */
    public static final RegistryObject<RotatedPillarBlock> PRIMED_LOG = log("primed_log", "primed_log");
    public static final RegistryObject<BlockItem> PRIMED_LOG_ITEM = REG.blockItem(PRIMED_LOG).tab(HbmTab.BLOCKS);
    /** 1.7: {@code BlockLogNT} "Anti-Mass Log" (the cyclotron's euphemium and a balefire log). */
    public static final RegistryObject<RotatedPillarBlock> EU_LOG = log("eu_log", "eu_log");
    public static final RegistryObject<BlockItem> EU_LOG_ITEM = REG.blockItem(EU_LOG).tab(HbmTab.BLOCKS);

    /**
     * 1.7: {@code BlockAstroturf(Material.grass)} (a {@code BlockNoSpawn}) "Astroturf", hardness 0.6, grass sound: no mob
     * spawns on it; top {@code astroturf_top}, sides {@code astroturf_side}, bottom vanilla dirt.
     */
    public static final RegistryObject<net.minecraft.world.level.block.Block> ASTROTURF = REG.simpleBlock("astroturf",
                    p -> p.strength(0.6F).sound(SoundType.GRASS).isValidSpawn((state, level, pos, type) -> false))
            .transform(Presets.cubeBottomTop("minecraft:block/dirt", "ntmspace:blocks/astroturf_side", "ntmspace:blocks/astroturf_top"))
            .loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> ASTROTURF_ITEM = REG.blockItem(ASTROTURF).tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- geysers

    /**
     * 1.7: {@code BlockVolcanoV2(Material.rock)} "Discharge Geyser", light 1.0, hardness 2, stone sound, drawn as HBM's
     * basalt; Eve's electric volcanoes. Not in a creative tab (1.7 set none).
     */
    public static final RegistryObject<BlockGeysirElectric> GEYSIR_ELECTRIC = REG.block("geysir_electric", BlockGeysirElectric::new,
                    p -> p.strength(2.0F).sound(SoundType.STONE).lightLevel(state -> 15).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll("hbm:blocks/basalt")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> GEYSIR_ELECTRIC_ITEM = REG.blockItem(GEYSIR_ELECTRIC);

    // ---------------------------------------------------------------- block entities (1.7 TileMappings ids in the comments)

    /** The tall rubber plant's renderer anchor (1.7 drew it in the chunk pass; no tile entity). */
    public static final RegistryObject<BlockEntityType<BlockRubberTall.Plant>> RUBBER_TALL_BE = REG.blockEntity("rubber_tall",
            () -> BlockEntities.type((pos, state) -> new BlockRubberTall.Plant(FloraBlocks.RUBBER_TALL_BE.get(), pos, state),
                    LaytheTektoBlocks.RUBBER_TALL.get()));
    /** 1.7 {@code tileentity_dcm}. */
    public static final RegistryObject<BlockEntityType<BlockGeysirChloric.Vent>> GEYSIR_CHLORIC_BE = REG.blockEntity("geysir_chloric",
            () -> BlockEntities.ticking((pos, state) -> new BlockGeysirChloric.Vent(FloraBlocks.GEYSIR_CHLORIC_BE.get(), pos, state),
                    BlockGeysirChloric.Vent::clientTick, BlockGeysirChloric.Vent::serverTick, LaytheTektoBlocks.GEYSIR_CHLORIC.get()));
    /** 1.7 {@code tileentity_electric_volcano}. */
    public static final RegistryObject<BlockEntityType<BlockGeysirElectric.Discharge>> GEYSIR_ELECTRIC_BE = REG.blockEntity("geysir_electric",
            () -> BlockEntities.ticking((pos, state) -> new BlockGeysirElectric.Discharge(FloraBlocks.GEYSIR_ELECTRIC_BE.get(), pos, state),
                    BlockGeysirElectric.Discharge::clientTick, BlockGeysirElectric.Discharge::serverTick, GEYSIR_ELECTRIC.get()));

    private FloraBlocks() {}

    private static boolean started;

    /** Class loading plus the run-time hooks (vinyl vine shears drops). */
    public static synchronized void init() {
        if (started) return;
        started = true;
        BlockVinylVine.shearDrops(LaytheTektoBlocks.VINYL_VINE);
        BlockSpaceLeaves.watchLogRemovals();
    }

    /**
     * The flora's furnace fuels, by item id: 1.7's {@code FuelHandler} gave saltleaf 200 ticks, and every
     * {@code Material.wood} block 300 ({@code TileEntityFurnace.getItemBurnTime}); the planks burn through
     * {@code #minecraft:planks}. One table for every loader: NeoForge's {@code neoforge:furnace_fuels} data map is
     * declared from it ({@link #declare}), Fabric and Forge register it at start-up ({@link #burnTimes}, Golem's
     * {@code Fuels}), as HBM's {@code FuelDataMapDeclarations} does.
     */
    public static final Map<String, Integer> BURN_TIMES = burnTimeTable();

    private static Map<String, Integer> burnTimeTable() {
        Map<String, Integer> t = new LinkedHashMap<>();
        t.put("ntmspace:saltleaf", 200);
        // hbm-space FuelHandler: powder_coke (ItemEnumMulti, every type) = single * 16 (fork-content family)
        for (String coke : new String[] {"powder_coke_coal", "powder_coke_lignite", "powder_coke_petroleum"}) {
            t.put("ntmspace:" + coke, 200 * 16);
        }
        for (String log : new String[] {"pvc_log", "vinyl_log", "lattice_log", "bf_log", "primed_log", "eu_log", "laythe_root"}) {
            t.put("ntmspace:" + log, 300);
        }
        return java.util.Collections.unmodifiableMap(t);
    }

    private static boolean declared;

    /** Dejson: the NeoForge furnace-fuel data map of {@link #BURN_TIMES}. */
    public static void declare() {
        if (declared) return;
        declared = true;
        var fuels = dev.golem.core.dejson.DataMaps.furnaceFuels();
        BURN_TIMES.forEach(fuels::burnTime);
        fuels.register();
    }

    /** {@link #BURN_TIMES} resolved for Golem's {@code Fuels.register} (Fabric, Forge; a no-op on NeoForge). */
    public static Map<Supplier<? extends Item>, Integer> burnTimes() {
        Map<Supplier<? extends Item>, Integer> fuels = new LinkedHashMap<>();
        BURN_TIMES.forEach((id, ticks) -> fuels.put(
                () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.parse(id)).orElseThrow(),
                ticks));
        return fuels;
    }

    /** The crop block of {@code kind}. */
    public static BlockSpaceCrop cropBlock(CropKind kind) {
        return CROPS.get(kind).get();
    }

    // ---------------------------------------------------------------- helpers

    private static RegistryObject<Item> plain(String id, String texture, HbmTab tab, ItemTag... tags) {
        Registrar.Handle<Item> item = REG.simpleItem(id, p -> p).transform(Presets.sprite("ntmspace:items/" + texture));
        for (ItemTag tag : tags) item.tag(tag);
        return item.tab(tab);
    }

    private static RegistryObject<CompatItem> food(String id, String texture, int nutrition, float saturation, HbmTab tab) {
        return REG.item(id, CompatItem::new, p -> CompatConsumables.food(p, nutrition, saturation, List.of()))
                .transform(Presets.sprite("ntmspace:items/" + texture)).tab(tab);
    }

    private static RegistryObject<ItemSpaceSeedFood> seed(String id, String texture, int nutrition, float saturation,
                                                          CropKind crop, ItemTag tag) {
        Registrar.Handle<ItemSpaceSeedFood> item = REG.item(id, p -> new ItemSpaceSeedFood(p, CROPS.get(crop).get(), id),
                        p -> CompatConsumables.food(p, nutrition, saturation, List.of()))
                .transform(Presets.sprite("ntmspace:items/" + texture));
        if (tag != null) item.tag(tag);
        return item.tab(HbmTab.CONSUMABLES);
    }

    /** A {@code BlockCrop}: eight cross models over the 1.7 stage textures, by age; its own block item. */
    private static RegistryObject<BlockSpaceCrop> crop(CropKind kind) {        RegistryObject<BlockSpaceCrop> crop = REG.block(kind.id(), p -> new BlockSpaceCrop(p, kind),
                        p -> p.strength(0.0F).sound(SoundType.GRASS).noOcclusion().noCollision().randomTicks())
                .model(d -> {
                    Models.Group group = Models.group();
                    for (int age = 0; age <= 7; age++) {
                        group = group.and("_" + age, Models.renderType(CUTOUT,
                                Models.cross("ntmspace:blocks/" + kind.texture + "_" + CropKind.textureStage(age))));
                    }
                    return d.models(group).states(Variants.intRange("age", 0, 7))
                            .item(Models.generated("ntmspace:blocks/" + kind.texture + "_" + CropKind.textureStage(0)));
                })
                .loot(Loot.nothing()).tag(BlockTag.of("minecraft:maintains_farmland"));
        CROPS.put(kind, crop);
        // 1.7 ModBlocks:1912-1919/3222-3226: each crop's block item lives in vanilla Decorations
        // (Natural on 26.1, see SpaceCreativeOrder), not the HBM blocks tab.
        CROP_ITEMS.add(REG.blockItem(crop));
        return crop;
    }

    /** vanilla's farmland shape with rubber silt for dirt. */
    private static dev.golem.core.dejson.Shape farmland(String top) {
        return Models.parented("minecraft:block/template_farmland",
                Slots.of("dirt", "ntmspace:blocks/rubber_silt", "top", top));
    }

    private static RegistryObject<net.minecraft.world.level.block.Block> planks(String id) {
        return REG.simpleBlock(id, p -> p.strength(1.0F, 1.0F).sound(SoundType.WOOD))
                .transform(Presets.cubeAll("ntmspace:blocks/" + id)).loot(Loot.self()).tag(BlockTag.AXE);
    }

    /** {@code BlockLogNT}: a log column ({@code <texture>_top} ends, {@code <texture>_side}) that holds leaves up. */
    private static RegistryObject<RotatedPillarBlock> log(String id, String texture) {
        return REG.block(id, RotatedPillarBlock::new, FloraBlocks::logProperties)
                .transform(Presets.column("ntmspace:blocks/" + texture + "_top", "ntmspace:blocks/" + texture + "_side")
                        .states(Variants.axis()))
                .loot(Loot.self()).tag(BlockTag.AXE).tag(BlockTag.of("minecraft:logs"));
    }

    private static BlockBehaviour.Properties logProperties(BlockBehaviour.Properties p) {
        return p.strength(0.5F, 2.5F).sound(SoundType.WOOD);
    }
}
