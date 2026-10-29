package com.ntmspace.registry;

import dev.golem.core.world.Compat;
import com.hbm.creativetabs.HbmTab;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.items.special.MaterialShapeItem;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.material.SpaceAlloyMaterials;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import com.ntmspace.item.ItemMineralClump;
import com.ntmspace.item.SpaceDrinkItem;
import dev.golem.core.dejson.Tags;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.ItemTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Remainders;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

/**
 * hbm-space's remaining {@code ModItems} that no feature family owns: the material sets of its new metals and
 * actinides, its parts, foods and drinks, the three {@code item_expensive} types and the coke powders (one item per
 * 1.7 metadata, HBM's split), the mineral fragments, and the nickel and osmiridium storage blocks. Ids are the 1.7
 * unlocalized names; textures are hbm-space's ({@code ntmspace:items/<texture>}); 1.7 ore-dictionary names are the
 * {@code c:} tags HBM spells for its own materials ({@code ingotNickelPure} -> {@code c:ingots/nickel_pure},
 * the material name; the generic nickel tags are other mods' nickel, which Space's pentlandite chunk joins).
 * {@code ItemCustomLore} items with no {@code .desc} key are plain items with their 1.7 rarity.
 *
 * <p>Materials are {@link SpaceAlloyMaterials} / {@link SpaceOreMaterials}; the autogen plates of stainless steel and
 * iridium and zinc's fine wire are registered here through HBM's {@link ModAutogenItems#registerAutogen}.
 */
public final class SpaceItems {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    static {
        SpaceAlloyMaterials.register();
    }

    public static final RegistryObject<Item> INGOT_NICKEL = plain("ingot_nickel", "ingot_nickel", HbmTab.MATERIALS,
            null, "c:ingots/nickel_pure");
    public static final RegistryObject<Item> NUGGET_NICKEL = plain("nugget_nickel", "nugget_nickel", HbmTab.MATERIALS,
            null, "c:nuggets/nickel_pure");
    public static final RegistryObject<Item> POWDER_NICKEL = plain("powder_nickel", "powder_nickel", HbmTab.MATERIALS,
            null, "c:dusts/nickel_pure");
    public static final RegistryObject<Item> PLATE_NICKEL = plain("plate_nickel", "plate_nickel", HbmTab.MATERIALS,
            null, "c:plates/nickel_pure");
    public static final RegistryObject<Item> CRYSTAL_NICKEL = plain("crystal_nickel", "crystal_nickel",
            HbmTab.MATERIALS, null, "c:crystals/nickel");
    public static final RegistryObject<Item> NUGGET_ZINC = plain("nugget_zinc", "nugget_zinc", HbmTab.MATERIALS, null,
            "c:nuggets/zinc");
    public static final RegistryObject<Item> POWDER_ZINC = plain("powder_zinc", "powder_zinc", HbmTab.MATERIALS, null,
            "c:dusts/zinc");
    public static final RegistryObject<Item> INGOT_GALLIUM = plain("ingot_gallium", "ingot_gallium", HbmTab.MATERIALS,
            null, "c:ingots/gallium");
    public static final RegistryObject<Item> NUGGET_GALLIUM = plain("nugget_gallium", "nugget_gallium",
            HbmTab.MATERIALS, null, "c:nuggets/gallium");
    public static final RegistryObject<Item> POWDER_GALLIUM = plain("powder_gallium", "powder_gallium",
            HbmTab.MATERIALS, null, "c:dusts/gallium");
    public static final RegistryObject<Item> POWDER_GALLIUM_TINY = plain("powder_gallium_tiny", "powder_gallium_tiny",
            HbmTab.MATERIALS, null, "c:dusts_tiny/gallium");
    public static final RegistryObject<Item> INGOT_GAAS = plain("ingot_gaas", "ingot_gaas1", HbmTab.MATERIALS,
            Rarity.RARE, "c:ingots/gaas");
    public static final RegistryObject<Item> NUGGET_GAAS = plain("nugget_gaas", "nugget_gaas", HbmTab.MATERIALS, null,
            "c:nuggets/gaas");
    public static final RegistryObject<Item> BILLET_GAAS = plain("billet_gaas", "billet_gaas1", HbmTab.MATERIALS, null,
            "c:billets/gaas");
    public static final RegistryObject<Item> INGOT_HAFNIUM = plain("ingot_hafnium", "ingot_hafnium", HbmTab.MATERIALS,
            null, "c:ingots/hafnium");
    public static final RegistryObject<Item> NUGGET_HAFNIUM = plain("nugget_hafnium", "nugget_hafnium",
            HbmTab.MATERIALS, null, "c:nuggets/hafnium");
    public static final RegistryObject<Item> INGOT_IRIDIUM = plain("ingot_iridium", "ingot_iridium", HbmTab.MATERIALS,
            Rarity.RARE, "c:ingots/iridium");
    public static final RegistryObject<Item> INGOT_STAINLESS = plain("ingot_stainless", "ingot_stainless",
            HbmTab.MATERIALS, null, "c:ingots/stainless");
    public static final RegistryObject<Item> PLATE_STAINLESS = plain("plate_stainless", "plate_stainless",
            HbmTab.MATERIALS, null, "c:plates/stainless");
    public static final RegistryObject<Item> INGOT_MENTHOL = plain("ingot_menthol", "ingot_menthol", HbmTab.MATERIALS,
            null, "c:ingots/menthol");
    public static final RegistryObject<Item> NUGGET_MENTHOL = plain("nugget_menthol", "nugget_menthol",
            HbmTab.MATERIALS, null, "c:nuggets/menthol");
    public static final RegistryObject<Item> BILLET_MENTHOL = plain("billet_menthol", "billet_menthol",
            HbmTab.MATERIALS, null, "c:billets/menthol");
    public static final RegistryObject<Item> INGOT_CM242 = plain("ingot_cm242", "ingot_cm242", HbmTab.MATERIALS, null,
            "c:ingots/cm242");
    public static final RegistryObject<Item> INGOT_CM243 = plain("ingot_cm243", "ingot_cm243", HbmTab.MATERIALS, null,
            "c:ingots/cm243");
    public static final RegistryObject<Item> INGOT_CM244 = plain("ingot_cm244", "ingot_cm244", HbmTab.MATERIALS, null,
            "c:ingots/cm244");
    public static final RegistryObject<Item> INGOT_CM245 = plain("ingot_cm245", "ingot_cm245", HbmTab.MATERIALS, null,
            "c:ingots/cm245");
    public static final RegistryObject<Item> INGOT_CM246 = plain("ingot_cm246", "ingot_cm246", HbmTab.MATERIALS, null,
            "c:ingots/cm246");
    public static final RegistryObject<Item> INGOT_CM247 = plain("ingot_cm247", "ingot_cm247", HbmTab.MATERIALS, null,
            "c:ingots/cm247");
    public static final RegistryObject<Item> INGOT_CM_MIX = plain("ingot_cm_mix", "ingot_cm_mix", HbmTab.MATERIALS,
            null, "c:ingots/cm_mix");
    public static final RegistryObject<Item> INGOT_CM_FUEL = plain("ingot_cm_fuel", "ingot_cm_fuel", HbmTab.MATERIALS,
            null, "c:ingots/cm_fuel");
    public static final RegistryObject<Item> INGOT_BK247 = plain("ingot_bk247", "ingot_bk247", HbmTab.MATERIALS, null,
            "c:ingots/bk247");
    public static final RegistryObject<Item> INGOT_CF251 = plain("ingot_cf251", "ingot_cf251", HbmTab.MATERIALS, null,
            "c:ingots/cf251");
    public static final RegistryObject<Item> INGOT_CF252 = plain("ingot_cf252", "ingot_cf252", HbmTab.MATERIALS, null,
            "c:ingots/cf252");
    public static final RegistryObject<Item> INGOT_ES253 = plain("ingot_es253", "ingot_es253", HbmTab.MATERIALS, null,
            "c:ingots/es253");
    public static final RegistryObject<Item> INGOT_ES255 = plain("ingot_es255", "ingot_es255", HbmTab.MATERIALS, null,
            "c:ingots/es255");
    public static final RegistryObject<Item> NUGGET_CM242 = plain("nugget_cm242", "nugget_cm242", HbmTab.MATERIALS,
            null, "c:nuggets/cm242");
    public static final RegistryObject<Item> NUGGET_CM243 = plain("nugget_cm243", "nugget_cm243", HbmTab.MATERIALS,
            null, "c:nuggets/cm243");
    public static final RegistryObject<Item> NUGGET_CM244 = plain("nugget_cm244", "nugget_cm244", HbmTab.MATERIALS,
            null, "c:nuggets/cm244");
    public static final RegistryObject<Item> NUGGET_CM245 = plain("nugget_cm245", "nugget_cm245", HbmTab.MATERIALS,
            null, "c:nuggets/cm245");
    public static final RegistryObject<Item> NUGGET_CM246 = plain("nugget_cm246", "nugget_cm246", HbmTab.MATERIALS,
            null, "c:nuggets/cm246");
    public static final RegistryObject<Item> NUGGET_CM247 = plain("nugget_cm247", "nugget_cm247", HbmTab.MATERIALS,
            null, "c:nuggets/cm247");
    public static final RegistryObject<Item> NUGGET_CM_MIX = plain("nugget_cm_mix", "nugget_cm_mix", HbmTab.MATERIALS,
            null, "c:nuggets/cm_mix");
    public static final RegistryObject<Item> NUGGET_CM_FUEL = plain("nugget_cm_fuel", "nugget_cm_fuel",
            HbmTab.MATERIALS, null, "c:nuggets/cm_fuel");
    public static final RegistryObject<Item> NUGGET_BK247 = plain("nugget_bk247", "nugget_bk247", HbmTab.MATERIALS,
            null, "c:nuggets/bk247");
    public static final RegistryObject<Item> NUGGET_CF251 = plain("nugget_cf251", "nugget_cf251", HbmTab.MATERIALS,
            null, "c:nuggets/cf251");
    public static final RegistryObject<Item> NUGGET_CF252 = plain("nugget_cf252", "nugget_cf252", HbmTab.MATERIALS,
            null, "c:nuggets/cf252");
    public static final RegistryObject<Item> NUGGET_ES253 = plain("nugget_es253", "nugget_es253", HbmTab.MATERIALS,
            null, "c:nuggets/es253");
    public static final RegistryObject<Item> BILLET_CM242 = plain("billet_cm242", "billet_cm242", HbmTab.MATERIALS,
            null, "c:billets/cm242");
    public static final RegistryObject<Item> BILLET_CM243 = plain("billet_cm243", "billet_cm243", HbmTab.MATERIALS,
            null, "c:billets/cm243");
    public static final RegistryObject<Item> BILLET_CM244 = plain("billet_cm244", "billet_cm244", HbmTab.MATERIALS,
            null, "c:billets/cm244");
    public static final RegistryObject<Item> BILLET_CM245 = plain("billet_cm245", "billet_cm245", HbmTab.MATERIALS,
            null, "c:billets/cm245");
    public static final RegistryObject<Item> BILLET_CM246 = plain("billet_cm246", "billet_cm246", HbmTab.MATERIALS,
            null, "c:billets/cm246");
    public static final RegistryObject<Item> BILLET_CM247 = plain("billet_cm247", "billet_cm247", HbmTab.MATERIALS,
            null, "c:billets/cm247");
    public static final RegistryObject<Item> BILLET_CM_MIX = plain("billet_cm_mix", "billet_cm_mix", HbmTab.MATERIALS,
            null, "c:billets/cm_mix");
    public static final RegistryObject<Item> BILLET_CM_FUEL = plain("billet_cm_fuel", "billet_cm_fuel",
            HbmTab.MATERIALS, null, "c:billets/cm_fuel");
    public static final RegistryObject<Item> BILLET_BK247 = plain("billet_bk247", "billet_bk247", HbmTab.MATERIALS,
            null, "c:billets/bk247");
    public static final RegistryObject<Item> BILLET_CF251 = plain("billet_cf251", "billet_cf251", HbmTab.MATERIALS,
            null, "c:billets/cf251");
    public static final RegistryObject<Item> BILLET_CF252 = plain("billet_cf252", "billet_cf252", HbmTab.MATERIALS,
            null, "c:billets/cf252");
    public static final RegistryObject<Item> BILLET_ES253 = plain("billet_es253", "billet_es253", HbmTab.MATERIALS,
            null, "c:billets/es253");
    public static final RegistryObject<Item> INGOT_CN989 = plain("ingot_cn989", "ingot_cn989", HbmTab.MATERIALS,
            Rarity.EPIC, "c:ingots/cn989");
    public static final RegistryObject<Item> NUGGET_CN989 = plain("nugget_cn989", "nugget_cn989", HbmTab.MATERIALS,
            Rarity.EPIC, "c:nuggets/cn989");
    public static final RegistryObject<Item> BILLET_CN989 = plain("billet_cn989", "billet_cn989", HbmTab.MATERIALS,
            Rarity.EPIC, "c:billets/cn989");
    public static final RegistryObject<Item> POWDER_CN989 = plain("powder_cn989", "powder_cn989", HbmTab.MATERIALS,
            Rarity.EPIC, "c:dusts/cn989");
    public static final RegistryObject<Item> PLATE_CN989 = plain("plate_cn989", "plate_cn989", HbmTab.MATERIALS,
            Rarity.EPIC, "c:plates/cn989");
    public static final RegistryObject<Item> INGOT_AUSTRALIUM_GREATER = plain("ingot_australium_greater",
            "ingot_australium_greater", HbmTab.MATERIALS, Rarity.UNCOMMON, "c:ingots/australium_greater",
            "c:ingots/ayerite");
    public static final RegistryObject<Item> INGOT_AUSTRALIUM_LESSER = plain("ingot_australium_lesser",
            "ingot_australium_lesser", HbmTab.MATERIALS, Rarity.UNCOMMON, "c:ingots/australium_lesser",
            "c:ingots/tasmanite");
    public static final RegistryObject<Item> BILLET_RED_COPPER = plain("billet_red_copper", "billet_red_copper",
            HbmTab.MATERIALS, null, "c:billets/red_copper");
    public static final RegistryObject<Item> NUGGET_LANTHANIUM = plain("nugget_lanthanium", "nugget_lanthanium",
            HbmTab.MATERIALS, null, "c:nuggets/lanthanium");
    public static final RegistryObject<Item> INGOT_MAGMA = plain("ingot_magma", "ingot_magma", HbmTab.MATERIALS, null,
            "c:ingots/rich_magma");
    public static final RegistryObject<Item> AMMONIUM_NITRATE = plain("ammonium_nitrate", "ammonium_nitrate",
            HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> BALL_FERRIC_CLAY = plain("ball_ferric_clay", "ferric_clay",
            HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> BERYLLIUM_MIRROR = plain("beryllium_mirror", "beryllium_mirror",
            HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> BLADE_SYNGAS = plain("blade_syngas", "blade_syngas", HbmTab.MATERIALS,
            null);
    public static final RegistryObject<Item> TURBINE_SYNGAS = plain("turbine_syngas", "turbine_syngas",
            HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> NICKEL_SALTS = plain("nickel_salts", "nickel_salts", HbmTab.MATERIALS,
            null);
    public static final RegistryObject<Item> RAG_BLOOD = plain("rag_blood", "rag_blood", HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> FLESH = plain("flesh", "flesh", HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> CRYSTAL_CLEANED = plain("crystal_cleaned", "crystal_mineralcf",
            HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> POWDER_WD2004 = plain("powder_wd2004", "powder_wd2004", HbmTab.MATERIALS,
            Rarity.EPIC);
    public static final RegistryObject<Item> POWDER_WD2004_TINY = plain("powder_wd2004_tiny", "powder_wd2004_tiny",
            HbmTab.MATERIALS, Rarity.EPIC);
    public static final RegistryObject<Item> INGOT_GWENIUM = plain("ingot_gwenium", "ingot_gwenium", HbmTab.MATERIALS,
            null);
    public static final RegistryObject<Item> INGOT_PALLADIUM = plain("ingot_palladium", "ingot_palladium",
            HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> INGOT_TT = plain("ingot_tt", "ingot_techtactium", HbmTab.MATERIALS,
            Rarity.UNCOMMON);
    public static final RegistryObject<Item> INGOT_TTAS = plain("ingot_ttas", "ingot_techtactium_as", HbmTab.MATERIALS,
            Rarity.RARE);
    public static final RegistryObject<Item> COIN_AIRLINER = plain("coin_airliner", "coin_airliner", HbmTab.CONSUMABLES,
            Rarity.UNCOMMON);
    public static final RegistryObject<Item> POWDER_COFFEE = plain("powder_coffee", "powder_coffee", HbmTab.CONSUMABLES,
            null);
    public static final RegistryObject<Item> BUTTER = plain("butter", "ingot_butter", HbmTab.CONSUMABLES, null,
            "c:foods/butter");
    public static final RegistryObject<Item> FLOUR = plain("flour", "flour", HbmTab.CONSUMABLES, null, "c:foods/flour");
    public static final RegistryObject<Item> GLASS_EMPTY = plain("glass_empty", "glass_empty", HbmTab.CONSUMABLES,
            null);
    public static final RegistryObject<Item> CMUG_EMPTY = plain("cmug_empty", "mug_empty", HbmTab.CONSUMABLES, null);
    public static final RegistryObject<Item> TEACUP_EMPTY = plain("teacup_empty", "teacup", HbmTab.CONSUMABLES, null);
    public static final RegistryObject<Item> FLESH_BURGER = food("flesh_burger", "blood_burger", 7, 1.0F,
            HbmTab.MATERIALS, List::of);
    public static final RegistryObject<Item> FLESH_WAFER = food("flesh_wafer", "blood_wafer", 5, 0.6F, HbmTab.MATERIALS,
            List::of);
    public static final RegistryObject<Item> GRILLED_FLESH = food("grilled_flesh", "blood_patty", 6, 0.8F,
            HbmTab.MATERIALS, List::of);
    public static final RegistryObject<Item> CHOCOLATE_MINT_BILLET = food("chocolate_mint_billet",
            "chocolate_mint_billet", 5, 5F, HbmTab.CONSUMABLES, List::of, "minecraft:wolf_food");
    public static final RegistryObject<Item> MIN_CREAM = food("min_cream", "ice_cream_min", 10, 1.0F,
            HbmTab.CONSUMABLES, List::of);
    public static final RegistryObject<Item> S_CREAM = food("s_cream", "ice_cream_s", 8, 1.0F, HbmTab.CONSUMABLES,
            () -> List.of(Effects.instance(MobEffects.REGENERATION, 20 * 20, 2)), "c:foods/strawberry_ice_cream");
    public static final RegistryObject<SpaceDrinkItem> GLASS_SMILK = drink("glass_smilk", "glass_smilk",
            SpaceDrinkItem.Kind.STRAWBERRY_MILKSHAKE, GLASS_EMPTY, null, "c:foods/strawberry_milkshake");
    public static final RegistryObject<SpaceDrinkItem> TEACUP = drink("teacup", "teacup_full", SpaceDrinkItem.Kind.TEA,
            TEACUP_EMPTY, HbmTab.CONSUMABLES);
    public static final RegistryObject<SpaceDrinkItem> BOTTLE_HONEY = drink("bottle_honey", "bottle_honey",
            SpaceDrinkItem.Kind.HONEY, () -> net.minecraft.world.item.Items.GLASS_BOTTLE, HbmTab.CONSUMABLES);
    // ---------------------------------------------------------------- split metadata items

    /**
     * 1.7 {@code item_expensive} types hbm-space added ({@code EnumExpensiveType.STAINLESS_BEAM, AVIONICS, ASTRO}).
     */
    public static final RegistryObject<Item> ITEM_EXPENSIVE_STAINLESS_BEAM = plain("item_expensive_stainless_beam",
            "item_expensive.stainless_beam", HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> ITEM_EXPENSIVE_AVIONICS = plain("item_expensive_avionics",
            "item_expensive.avionics", HbmTab.MATERIALS, null);
    public static final RegistryObject<Item> ITEM_EXPENSIVE_ASTRO = plain("item_expensive_astro",
            "item_expensive.astro", HbmTab.MATERIALS, null);

    /**
     * 1.7 {@code powder_coke = ItemEnumMulti(EnumCokeType.class, true, true)}: {@code COALCOKE/PETCOKE/LIGCOKE.dust()}.
     */
    public static final RegistryObject<Item> POWDER_COKE_COAL = lore("powder_coke_coal", "powder_coke.coal",
            HbmTab.MATERIALS, null, "c:dusts/coal_coke");
    public static final RegistryObject<Item> POWDER_COKE_LIGNITE = lore("powder_coke_lignite", "powder_coke.lignite",
            HbmTab.MATERIALS, null, "c:dusts/lignite_coke");
    public static final RegistryObject<Item> POWDER_COKE_PETROLEUM = lore("powder_coke_petroleum",
            "powder_coke.petroleum",
            HbmTab.MATERIALS, null, "c:dusts/pet_coke");

    /**
     * 1.7 {@code mineral_fragment = ItemMineralOre}, six metas named {@code clump_<type>} (the acid a mineral dust was
     * crystallised with). The coloured tooltip line is lang {@code .desc.0}.
     */
    public static final RegistryObject<ItemMineralClump> MINERAL_FRAGMENT_PEROXIDE = clump("mineral_fragment_peroxide",
            "clump_peroxide", ItemMineralClump.Clump.PEROXIDE);
    public static final RegistryObject<ItemMineralClump> MINERAL_FRAGMENT_NITRIC = clump("mineral_fragment_nitric",
            "clump_nitric", ItemMineralClump.Clump.NITRIC);
    public static final RegistryObject<ItemMineralClump> MINERAL_FRAGMENT_SULFURIC = clump("mineral_fragment_sulfuric",
            "clump_sulfuric", ItemMineralClump.Clump.SULFURIC);
    public static final RegistryObject<ItemMineralClump> MINERAL_FRAGMENT_SOLVENT = clump("mineral_fragment_solvent",
            "clump_solvent", ItemMineralClump.Clump.SOLVENT);
    public static final RegistryObject<ItemMineralClump> MINERAL_FRAGMENT_HYDROCHLORIC = clump(
            "mineral_fragment_hydrochloric", "clump_hydrochloric", ItemMineralClump.Clump.HYDROCHLORIC);
    public static final RegistryObject<ItemMineralClump> MINERAL_FRAGMENT_SCHRABIDIC = clump(
            "mineral_fragment_schrabidic", "clump_schrabidic", ItemMineralClump.Clump.SCHRABIDIC);

    // ---------------------------------------------------------------- blocks

    /**
     * 1.7 {@code BlockBeaconable(Material.iron)}, hardness 5, resistance 10, metal sound, blocks tab.
     */
    public static final RegistryObject<Block> BLOCK_NICKEL = storage("block_nickel", "c:storage_blocks/nickel_pure");
    public static final RegistryObject<BlockItem> BLOCK_NICKEL_ITEM = REG.blockItem(BLOCK_NICKEL)
                                                                         .tag(ItemTag.of(
                                                                                 "c:storage_blocks/nickel_pure"))
                                                                         .tab(HbmTab.BLOCKS);
    public static final RegistryObject<Block> BLOCK_OSMIRIDIUM = storage("block_osmiridium",
            "c:storage_blocks/osmiridium");
    public static final RegistryObject<BlockItem> BLOCK_OSMIRIDIUM_ITEM = REG.blockItem(BLOCK_OSMIRIDIUM)
                                                                             .tag(ItemTag.of(
                                                                                     "c:storage_blocks/osmiridium"))
                                                                             .tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- autogen shapes

    static {
        ModAutogenItems.registerAutogen(ITEMS, SpaceAlloyMaterials.STAINLESS, MaterialShapes.CASTPLATE,
                MaterialShapes.WELDEDPLATE);
        ModAutogenItems.registerAutogen(ITEMS, SpaceAlloyMaterials.IRIDIUM, MaterialShapes.CASTPLATE,
                MaterialShapes.WELDEDPLATE);
        ModAutogenItems.registerAutogen(ITEMS, SpaceOreMaterials.ZINC, MaterialShapes.WIRE);
        SpaceAlloyMaterials.registerBaseShapes();
    }

    private SpaceItems() {
    }

    public static void init() {
        Remainders.add(ModConsumableTailItems.coffee, CMUG_EMPTY);
    }

    /**
     * Source ore-dictionary names for the live neutron-producing material shapes.
     */
    private static void declareNeutronMaterialTags() {
        for (var names : new String[][]{
                {"pu_mix", "plutonium_rg"}, {"pu240", "plutonium_240"},
                {"cm243", "curium_243"}, {"cm244", "curium_244"},
                {"cm245", "curium_245"}, {"cm246", "curium_246"},
                {"cm_mix", "curium_rg"}, {"cm_fuel", "curium_fuel"},
                {"cf252", "californium_252"}, {"es253", "einsteinium_253"}
        }) {
            var nativeName = names[0];
            var sourceName = names[1];
            for (var shape : new String[]{"nuggets", "ingots"}) {
                Tags.item("c", shape + "/" + sourceName, "#c:" + shape + "/" + nativeName);
            }
            Tags.item("hbm", "billets/" + sourceName, "#c:billets/" + nativeName);
            if (nativeName.equals("pu_mix") || nativeName.equals("pu240")) {
                Tags.item("c", "storage_blocks/" + sourceName, "#c:storage_blocks/" + nativeName);
            }
        }
    }

    private static boolean declared;

    /**
     * Dejson: the tags that are not one item's membership. 1.7 {@code ANY_COKE.dust()} (every coke powder),
     * {@code ANY_COAL_COKE} (hbm-space's {@code DictGroup("AnyCoalCoke", ANY_COKE, COAL)}: any coke or coal), the food
     * key whose member is another mod's item ({@code KEY_CHEESE} = HBM's cheese), and the existing items joining the
     * material tags of this family (the zinc ingot and crystal of {@code SpaceMachineItems}).
     */
    public static void declare() {
        if (declared) return;
        declared = true;
        SpaceAlloyMaterials.register();
        declareNeutronMaterialTags();
        Tags.of("item", "c", "dusts/any_coke")
            .values("#c:dusts/coal_coke", "#c:dusts/lignite_coke", "#c:dusts/pet_coke").register();
        Tags.of("item", "c", "dusts/any_coal_coke").values("#c:dusts/any_coke", "#c:dusts/coal").register();
        // 1.7's namespaced group tag for the black-dye rows (the crystallizer tests address it, not c:).
        Tags.of("item", NtmSpace.MOD_ID, "dusts/any_coal_coke").values("#c:dusts/any_coal_coke").register();
        Tags.of("item", "c", "gems/any_coal_coke").values("#c:cokes", "#c:gems/coal").register();
        Tags.member("item", "c", "foods/cheese", "hbm:cheese");
        Tags.member("item", "c", "ingots/zinc", "ntmspace:ingot_zinc");
        Tags.member("item", "c", "crystals/zinc", "ntmspace:crystal_zinc");
        // 1.7 full material names (upstream DictFrames): our short ids keep their tags above and join these.
        Tags.member("item", "c", "ingots/curium_242", "ntmspace:ingot_cm242");
        Tags.member("item", "c", "nuggets/curium_242", "ntmspace:nugget_cm242");
        Tags.member("item", "c", "billets/curium_242", "ntmspace:billet_cm242");
        Tags.member("item", "c", "ingots/curium_243", "ntmspace:ingot_cm243");
        Tags.member("item", "c", "nuggets/curium_243", "ntmspace:nugget_cm243");
        Tags.member("item", "c", "billets/curium_243", "ntmspace:billet_cm243");
        Tags.member("item", "c", "ingots/curium_244", "ntmspace:ingot_cm244");
        Tags.member("item", "c", "nuggets/curium_244", "ntmspace:nugget_cm244");
        Tags.member("item", "c", "billets/curium_244", "ntmspace:billet_cm244");
        Tags.member("item", "c", "ingots/curium_245", "ntmspace:ingot_cm245");
        Tags.member("item", "c", "nuggets/curium_245", "ntmspace:nugget_cm245");
        Tags.member("item", "c", "billets/curium_245", "ntmspace:billet_cm245");
        Tags.member("item", "c", "ingots/curium_246", "ntmspace:ingot_cm246");
        Tags.member("item", "c", "nuggets/curium_246", "ntmspace:nugget_cm246");
        Tags.member("item", "c", "billets/curium_246", "ntmspace:billet_cm246");
        Tags.member("item", "c", "ingots/curium_247", "ntmspace:ingot_cm247");
        Tags.member("item", "c", "nuggets/curium_247", "ntmspace:nugget_cm247");
        Tags.member("item", "c", "billets/curium_247", "ntmspace:billet_cm247");
        Tags.member("item", "c", "ingots/curium_rg", "ntmspace:ingot_cm_mix");
        Tags.member("item", "c", "nuggets/curium_rg", "ntmspace:nugget_cm_mix");
        Tags.member("item", "c", "billets/curium_rg", "ntmspace:billet_cm_mix");
        Tags.member("item", "c", "ingots/curium_fuel", "ntmspace:ingot_cm_fuel");
        Tags.member("item", "c", "nuggets/curium_fuel", "ntmspace:nugget_cm_fuel");
        Tags.member("item", "c", "billets/curium_fuel", "ntmspace:billet_cm_fuel");
        Tags.member("item", "c", "ingots/berkelium_247", "ntmspace:ingot_bk247");
        Tags.member("item", "c", "nuggets/berkelium_247", "ntmspace:nugget_bk247");
        Tags.member("item", "c", "billets/berkelium_247", "ntmspace:billet_bk247");
        Tags.member("item", "c", "ingots/californium_251", "ntmspace:ingot_cf251");
        Tags.member("item", "c", "nuggets/californium_251", "ntmspace:nugget_cf251");
        Tags.member("item", "c", "billets/californium_251", "ntmspace:billet_cf251");
        Tags.member("item", "c", "ingots/californium_252", "ntmspace:ingot_cf252");
        Tags.member("item", "c", "nuggets/californium_252", "ntmspace:nugget_cf252");
        Tags.member("item", "c", "billets/californium_252", "ntmspace:billet_cf252");
        Tags.member("item", "c", "ingots/einsteinium_253", "ntmspace:ingot_es253");
        Tags.member("item", "c", "nuggets/einsteinium_253", "ntmspace:nugget_es253");
        Tags.member("item", "c", "billets/einsteinium_253", "ntmspace:billet_es253");
        Tags.member("item", "c", "ingots/einsteinium_255", "ntmspace:ingot_es255");
        Tags.member("item", "c", "ingots/chinesium_989", "ntmspace:ingot_cn989");
        Tags.member("item", "c", "nuggets/chinesium_989", "ntmspace:nugget_cn989");
        Tags.member("item", "c", "billets/chinesium_989", "ntmspace:billet_cn989");
        Tags.member("item", "c", "dusts/chinesium_989", "ntmspace:powder_cn989");
        // (menthol keeps its short tags only: full name == short name.)
        Tags.member("item", "c", "ingots/gallium_arsenide", "ntmspace:ingot_gaas");
        Tags.member("item", "c", "nuggets/gallium_arsenide", "ntmspace:nugget_gaas");
        Tags.member("item", "c", "billets/gallium_arsenide", "ntmspace:billet_gaas");
        Tags.member("item", "c", "ingots/stainless_steel", "ntmspace:ingot_stainless");
        Tags.member("item", "c", "plates/stainless_steel", "ntmspace:plate_stainless");
        Tags.member("item", "c", "plates/chinesium_989", "ntmspace:plate_cn989");
        Tags.member("item", "c", "billets/ayerite", "hbm:billet_australium_greater");
        Tags.member("item", "c", "nuggets/ayerite", "hbm:nugget_australium_greater");
        Tags.member("item", "c", "billets/tasmanite", "hbm:billet_australium_lesser");
        Tags.member("item", "c", "nuggets/tasmanite", "hbm:nugget_australium_lesser");
        Tags.member("item", "c", "ingots/semtex", "hbm:ingot_semtex");
        Tags.member("item", "c", "storage_blocks/semtex", "hbm:block_semtex");
        Tags.member("item", "c", "dusts/glowstone", "minecraft:glowstone_dust");
        // (nickel shapes declare their nickel_pure tags at registration above.)
        // the autogen shapes' 1.7 ore names (plateCastStainlessSteel ...): HBM's own JSON names them as material+shape,
        // but rows that spell them as tags (and other mods) must find the items too
        for (String mat : new String[]{"stainless", "iridium"}) {
            Tags.member("item", "c", "plates_cast/" + mat, "ntmspace:plate_cast_" + mat);
            Tags.member("item", "c", "plates_welded/" + mat, "ntmspace:plate_welded_" + mat);
        }
        Tags.member("item", "c", "wires/zinc", "ntmspace:wire_fine_zinc");
        // 1.7 plateNickel: the RTG pellet rows take the generic plate tag, which carries our plate.
        Tags.member("item", "c", "plates/nickel", "ntmspace:plate_nickel");
        // hbm-space OreDictManager: the planets' stones, cobbles and sands join KEY_STONE / KEY_COBBLESTONE / KEY_SAND
        for (String stone : new String[]{"duna_rock", "ike_stone", "moho_stone", "minmus_smooth"}) {
            Tags.member("item", "c", "stones", "ntmspace:" + stone);
        }
        for (String cobble : new String[]{"duna_cobble", "dres_rock", "ike_regolith", "eve_rock", "moho_regolith",
                "moon_rock", "minmus_regolith"}) {
            Tags.member("item", "c", "cobblestones", "ntmspace:" + cobble);
        }
        for (String sand : new String[]{"ntmspace:duna_sands", "ntmspace:laythe_silt", "ntmspace:eve_silt",
                "hbm:moon_turf", "ntmspace:rubber_silt", "ntmspace:vinyl_sand"}) {
            Tags.member("item", "c", "sands", sand);
        }
        // The vanilla members of the three, as NeoForge and Fabric define them from 1.21 on. The 1.20.1 loaders have no
        // c:stones/c:cobblestones/c:sands (Forge spells them forge:stone, ...), so without these the KEY_ loops of the
        // shredder and the stone/cobble rules found no vanilla block there; where the loader has them they merge as
        // duplicates. TODO(Golem): a Golem Core convention-tag backport would serve every mod.
        for (String stone : new String[]{"stone", "andesite", "diorite", "granite", "tuff", "deepslate"}) {
            Tags.member("item", "c", "stones", "minecraft:" + stone);
        }
        for (String cobble : new String[]{"cobblestone", "infested_cobblestone", "mossy_cobblestone", "cobbled_deepslate"}) {
            Tags.member("item", "c", "cobblestones", "minecraft:" + cobble);
        }
        for (String sand : new String[]{"sand", "red_sand"}) {
            Tags.member("item", "c", "sands", "minecraft:" + sand);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static RegistryObject<Item> plain(String id, String texture, HbmTab tab, Rarity rarity, String... tags) {
        Registrar.Handle<Item> item = REG.<Item>item(id, p -> MaterialShapeItem.fromTags(p, tags),
                                                 p -> rarity == null ? p : p.rarity(rarity))
                                         .transform(Presets.sprite("ntmspace:items/" + texture));
        for (String tag : tags) item.tag(ItemTag.of(tag));
        return tab == null ? item : item.tab(tab);
    }

    /**
     * A lore item with tags but no material shape. 1.7's enum-multi splits (one registration covering several
     * variants) stay out of the named-shape roster, like upstream's plain-{@code Item} families; only 1.7's named
     * {@code ModItems} entries take a shape.
     */
    private static RegistryObject<Item> lore(String id, String texture, HbmTab tab, Rarity rarity, String... tags) {
        Registrar.Handle<Item> item = REG.<Item>item(id, ItemCustomLore::new,
                                                 p -> rarity == null ? p : p.rarity(rarity))
                                         .transform(Presets.sprite("ntmspace:items/" + texture));
        for (String tag : tags) item.tag(ItemTag.of(tag));
        return tab == null ? item : item.tab(tab);
    }

    private static RegistryObject<ItemMineralClump> clump(String id, String texture, ItemMineralClump.Clump clump) {
        return REG.<ItemMineralClump>item(id, properties -> new ItemMineralClump(properties, clump),
                          properties -> properties)
                  .transform(Presets.sprite("ntmspace:items/" + texture)).tab(HbmTab.MATERIALS);
    }

    /**
     * 1.7 {@code ItemLemon(hunger, saturation, wolf)}; tooltips from lang {@code .desc.N} (flesh_burger's "juicy!.").
     */
    private static RegistryObject<Item> food(String id, String texture, int nutrition, float saturation, HbmTab tab,
                                             Supplier<List<MobEffectInstance>> effects, String... tags) {
        Registrar.Handle<Item> item = REG.<Item>item(id, ItemCustomLore::new,
                                                 p -> CompatConsumables.food(p, nutrition, saturation, effects.get()))
                                         .transform(Presets.sprite("ntmspace:items/" + texture));
        for (String tag : tags) item.tag(ItemTag.of(tag));
        return tab == null ? item : item.tab(tab);
    }

    private static RegistryObject<SpaceDrinkItem> drink(String id, String texture, SpaceDrinkItem.Kind kind,
                                                        Supplier<? extends Item> container, HbmTab tab,
                                                        String... tags) {
        Registrar.Handle<SpaceDrinkItem> item = REG.item(id,
                                                           p -> new SpaceDrinkItem(Compat.craftingRemainder(p, container.get()), kind,
                                                                   container))
                                                   .transform(Presets.sprite("ntmspace:items/" + texture));
        for (String tag : tags) item.tag(ItemTag.of(tag));
        return tab == null ? item : item.tab(tab);
    }

    private static RegistryObject<Block> storage(String id, String tag) {
        return REG.simpleBlock(id, p -> p.strength(5.0F, 6.0F).requiresCorrectToolForDrops()
                                         .sound(net.minecraft.world.level.block.SoundType.METAL))
                  .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE)
                  .tag(BlockTag.of("minecraft:beacon_base_blocks")).tag(BlockTag.of(tag));
    }
}
