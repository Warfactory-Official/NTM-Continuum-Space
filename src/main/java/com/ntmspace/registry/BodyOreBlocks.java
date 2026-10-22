package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.block.BlockBodyOre.Body;
import com.ntmspace.block.BlockBodyOreOutgas;
import com.ntmspace.block.BodyOre;
import com.ntmspace.block.BodyOreItem;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.ItemTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's per-body ores: every {@code BlockOre} a body's world generator places (hbm-space made base HBM's
 * {@code BlockOre} a multipass block whose metadata is the body it was found on) and the per-body fluid ores
 * ({@code BlockOreFluid}) with the empty blocks they leave. Values are hbm-space's {@code ModBlocks}: hardness and
 * resistance as 1.7 wrote them, {@code BlockOre.getItemDropped}/{@code quantityDropped} as the loot (fortune multiplies
 * only an ore that drops something other than itself, the 1.7 {@code quantityDroppedWithBonus}).
 *
 * <p>Each is one block with a {@code body} state ({@link BlockBodyOre}) and one block item per state ({@link BodyOreItem}):
 * {@code ntmspace:<ore>} is the Kerbin form (1.7 metadata 0 and 1, plain stone), {@code ntmspace:<ore>_<body>} the
 * others. The loot names the Kerbin item and the block makes it the broken state's ({@link BodyOre#drops}), as 1.7 kept
 * the body in the item's damage. On Kerbin itself hbm-space's ore was HBM's (or vanilla's) own block:
 * {@link #state(String, Body)} answers that block for the Kerbin form, so a world generator never places the duplicate.
 */
public final class BodyOreBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    /** The ores by 1.7 path, and the block standing for each one's Kerbin form (null: the ore's own Kerbin state). */
    private static final Map<String, RegistryObject<? extends Block>> ORES = new LinkedHashMap<>();
    private static final Map<String, String> KERBIN = new LinkedHashMap<>();

    // ---------------------------------------------------------------- vanilla reproductions (BlockOre(rock, vanilla))

    public static final RegistryObject<BlockBodyOre> ORE_IRON = ore("ore_iron", 3.0F, 5.0F, Loot.self(), "minecraft:iron_ore", "minecraft:iron_ores");
    public static final RegistryObject<BlockBodyOre> ORE_GOLD = ore("ore_gold", 3.0F, 5.0F, Loot.self(), "minecraft:gold_ore", "minecraft:gold_ores");
    public static final RegistryObject<BlockBodyOre> ORE_REDSTONE = ore("ore_redstone", 3.0F, 5.0F, Loot.oreMultiplied("minecraft:redstone", 4, 5), "minecraft:redstone_ore", "minecraft:redstone_ores");
    public static final RegistryObject<BlockBodyOre> ORE_QUARTZ = ore("ore_quartz", 3.0F, 5.0F, Loot.oreMultiplied("minecraft:quartz", 1, 1), "minecraft:nether_quartz_ore", "c:ores/quartz");
    public static final RegistryObject<BlockBodyOre> ORE_DIAMOND = ore("ore_diamond", 3.0F, 5.0F, Loot.oreMultiplied("minecraft:diamond", 1, 1), "minecraft:diamond_ore", "minecraft:diamond_ores");

    // ---------------------------------------------------------------- HBM's ores

    /** {@code BlockOreOutgas(rock, true, 5, true)}: radon. */
    public static final RegistryObject<BlockBodyOreOutgas> ORE_URANIUM = outgas("ore_uranium", 5.0F, 10.0F, Loot.self(), true, "hbm:ore_uranium", "c:ores/uranium");
    public static final RegistryObject<BlockBodyOre> ORE_THORIUM = ore("ore_thorium", 5.0F, 10.0F, Loot.self(), "hbm:ore_thorium", "c:ores/thorium");
    public static final RegistryObject<BlockBodyOre> ORE_TITANIUM = ore("ore_titanium", 5.0F, 10.0F, Loot.self(), "hbm:ore_titanium", "c:ores/titanium");
    public static final RegistryObject<BlockBodyOre> ORE_SULFUR = ore("ore_sulfur", 5.0F, 10.0F, Loot.oreMultiplied("hbm:sulfur", 2, 4), "hbm:ore_sulfur", "c:ores/sulfur");
    /** Light 4/15; drops 1-3 glowstone dust. */
    public static final RegistryObject<BlockBodyOre> ORE_GLOWSTONE = ore("ore_glowstone", 5.0F, 10.0F, p -> p.lightLevel(s -> 4), Loot.oreMultiplied("minecraft:glowstone_dust", 1, 3), null, "c:ores/glowstone");
    public static final RegistryObject<BlockBodyOre> ORE_NITER = ore("ore_niter", 5.0F, 10.0F, Loot.oreMultiplied("hbm:niter", 2, 4), "hbm:ore_niter", "c:ores/niter");
    public static final RegistryObject<BlockBodyOre> ORE_COPPER = ore("ore_copper", 5.0F, 10.0F, Loot.self(), "minecraft:copper_ore", "c:ores/copper");
    public static final RegistryObject<BlockBodyOre> ORE_NICKEL = ore("ore_nickel", 5.0F, 10.0F, Loot.self(), null, "c:ores/nickel");
    public static final RegistryObject<BlockBodyOre> ORE_MINERAL = ore("ore_mineral", 5.0F, 10.0F, Loot.self(), null, "c:ores/mineral");
    public static final RegistryObject<BlockBodyOre> ORE_ZINC = ore("ore_zinc", 5.0F, 10.0F, Loot.self(), null, "c:ores/zinc");
    public static final RegistryObject<BlockBodyOre> ORE_LITHIUM = ore("ore_lithium", 5.0F, 10.0F, Loot.self(), null, "c:ores/lithium");
    public static final RegistryObject<BlockBodyOre> ORE_NIOBIUM = ore("ore_niobium", 5.0F, 10.0F, Loot.self(), null, "c:ores/niobium");
    public static final RegistryObject<BlockBodyOre> ORE_IODINE = ore("ore_iodine", 5.0F, 10.0F, Loot.self(), null, "c:ores/iodine");
    public static final RegistryObject<BlockBodyOre> ORE_TUNGSTEN = ore("ore_tungsten", 5.0F, 10.0F, Loot.self(), "hbm:ore_tungsten", "c:ores/tungsten");
    public static final RegistryObject<BlockBodyOre> ORE_ALUMINIUM = ore("ore_aluminium", 5.0F, 10.0F, Loot.self(), "hbm:ore_aluminium", "c:ores/aluminium");
    public static final RegistryObject<BlockBodyOre> ORE_FLUORITE = ore("ore_fluorite", 5.0F, 10.0F, Loot.oreMultiplied("hbm:fluorite", 2, 4), "hbm:ore_fluorite", "c:ores/fluorite");
    public static final RegistryObject<BlockBodyOre> ORE_LEAD = ore("ore_lead", 5.0F, 10.0F, Loot.self(), "hbm:ore_lead", "c:ores/lead");
    public static final RegistryObject<BlockBodyOre> ORE_SCHRABIDIUM = ore("ore_schrabidium", 15.0F, 600.0F, Loot.self(), "hbm:ore_schrabidium", "c:ores/schrabidium");
    public static final RegistryObject<BlockBodyOre> ORE_BERYLLIUM = ore("ore_beryllium", 5.0F, 15.0F, Loot.self(), "hbm:ore_beryllium", "c:ores/beryllium");
    /** {@code BlockOreOutgas(rock, true, 5, true)}: asbestos dust. */
    public static final RegistryObject<BlockBodyOreOutgas> ORE_ASBESTOS = outgas("ore_asbestos", 5.0F, 15.0F, Loot.oreMultiplied("hbm:ingot_asbestos", 1, 1), false, "hbm:ore_asbestos", "c:ores/asbestos");
    /** Drops red phosphorus one time in ten, else fire powder ({@code getItemDropped}); fortune is not applied here. */
    public static final RegistryObject<BlockBodyOre> ORE_FIRE = ore("ore_fire", 0.4F, 10.0F, Loot.oreOrRare("hbm:powder_fire", 1, 1, "hbm:ingot_phosphorus", 0.1D), null, "c:ores/phosphorus");
    public static final RegistryObject<BlockBodyOre> ORE_SHALE = ore("ore_shale", 1.5F, 10.0F, Loot.self(), null, "c:ores/shale");
    public static final RegistryObject<BlockBodyOre> ORE_AUSTRALIUM = ore("ore_australium", 5.0F, 10.0F, Loot.self(), "hbm:ore_australium", "c:ores/australium");
    public static final RegistryObject<BlockBodyOre> ORE_RARE = ore("ore_rare", 5.0F, 10.0F, Loot.oreMultiplied("hbm:chunk_ore_rare", 1, 1), "hbm:ore_rare", "c:ores/rare");
    public static final RegistryObject<BlockBodyOre> ORE_COBALT = ore("ore_cobalt", 5.0F, 10.0F, Loot.oreMultiplied("hbm:fragment_cobalt", 4, 9), "hbm:ore_cobalt", "c:ores/cobalt");
    public static final RegistryObject<BlockBodyOre> ORE_CINNEBAR = ore("ore_cinnebar", 5.0F, 10.0F, Loot.oreMultiplied("hbm:cinnabar", 1, 1), "hbm:ore_cinnabar", "c:ores/cinnabar");
    public static final RegistryObject<BlockBodyOre> ORE_COLTAN = ore("ore_coltan", 15.0F, 10.0F, Loot.oreMultiplied("hbm:fragment_coltan", 1, 1), "hbm:ore_coltan", "c:ores/coltan");
    public static final RegistryObject<BlockBodyOre> ORE_LANTHANIUM = ore("ore_lanthanium", 15.0F, 10.0F, Loot.self(), null, "c:ores/lanthanum");

    /** {@code BlockCluster}: drops its crystal. */
    public static final RegistryObject<BlockBodyOre> CLUSTER_IRON = ore("cluster_iron", 5.0F, 15.0F, Loot.oreMultiplied("hbm:crystal_iron", 1, 1), "hbm:cluster_iron", null);
    public static final RegistryObject<BlockBodyOre> CLUSTER_TITANIUM = ore("cluster_titanium", 5.0F, 15.0F, Loot.oreMultiplied("hbm:crystal_titanium", 1, 1), "hbm:cluster_titanium", null);
    public static final RegistryObject<BlockBodyOre> CLUSTER_ALUMINIUM = ore("cluster_aluminium", 5.0F, 15.0F, Loot.oreMultiplied("hbm:crystal_aluminium", 1, 1), "hbm:cluster_aluminium", null);
    public static final RegistryObject<BlockBodyOre> CLUSTER_COPPER = ore("cluster_copper", 5.0F, 15.0F, Loot.oreMultiplied("hbm:crystal_copper", 1, 1), "hbm:cluster_copper", null);

    // ---------------------------------------------------------------- fluid ores (BlockOreFluid) and their empties

    /** The empty rock a drained fluid ore leaves ({@code BlockOre}, texture {@code ore_oil_empty}); drops itself. */
    public static final RegistryObject<BlockBodyOre> ORE_OIL_EMPTY = fluidOre("ore_oil_empty", "ntmspace:blocks/ore_oil_empty", Loot.self(), "hbm:ore_oil_empty");
    public static final RegistryObject<BlockBodyOre> ORE_GAS_EMPTY = fluidOre("ore_gas_empty", "ntmspace:blocks/ore_oil_empty", Loot.self(), null);
    public static final RegistryObject<BlockBodyOre> ORE_BRINE_EMPTY = fluidOre("ore_brine_empty", "ntmspace:blocks/ore_oil_empty", Loot.self(), null);
    public static final RegistryObject<BlockBodyOre> ORE_TEKTO_EMPTY = fluidOre("ore_tekto_empty", "ntmspace:blocks/ore_oil_empty", Loot.self(), null);

    /**
     * {@code BlockOreFluid(rock, ore_oil_empty, OIL)}. Kerbin's is HBM's own {@code hbm:ore_oil}. Drops crude oil tar
     * ({@code canSilkHarvest} false; fortune multiplies); its oil reservoir comes with the drills.
     */
    public static final RegistryObject<BlockBodyOre> ORE_OIL = fluidOre("ore_oil", "ntmspace:blocks/ore_oil", Loot.dropsMultiplied("hbm:oil_tar_crude", 1, 1), "hbm:ore_oil");
    /** {@code BlockOreFluid(rock, ore_gas_empty, GAS)}, "Natural Gas Deposit": drops the empty (its Kerbin form, damage 0). */
    public static final RegistryObject<BlockBodyOre> ORE_GAS = fluidOre("ore_gas", "ntmspace:blocks/ore_gas", Loot.dropsMultiplied("ntmspace:ore_gas_empty", 1, 1), null);
    /** {@code BlockOreFluid(rock, ore_brine_empty, BRINE)}: drops the empty. */
    public static final RegistryObject<BlockBodyOre> ORE_BRINE = fluidOre("ore_brine", "ntmspace:blocks/ore_brine", Loot.dropsMultiplied("ntmspace:ore_brine_empty", 1, 1), null);
    /** {@code BlockOreFluid(rock, ore_tekto_empty, OIL)}, "Tektonic Crude Deposit": drops the empty. */
    public static final RegistryObject<BlockBodyOre> ORE_TEKTO = fluidOre("ore_tekto", "ntmspace:blocks/ore_tekto", Loot.dropsMultiplied("ntmspace:ore_tekto_empty", 1, 1), null);

    private BodyOreBlocks() {}

    public static void init() {}

    // ---------------------------------------------------------------- lookups for the world generators

    /**
     * The block state of hbm-space ore {@code path} on {@code body}: {@code ntmspace:<path>[body=<body>]}, or, for the
     * Kerbin form of an ore that HBM or vanilla has, that block. Null for an ore not registered here.
     */
    public static BlockState state(String path, Body body) {
        RegistryObject<? extends Block> ore = ORES.get(path);
        if (ore == null) return null;
        if (body == Body.KERBIN && KERBIN.get(path) != null) {
            return BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(KERBIN.get(path)))
                    .map(Block::defaultBlockState).orElse(null);
        }
        return ore.get().defaultBlockState().setValue(BlockBodyOre.BODY, body);
    }

    /** The registered ore paths. */
    public static List<String> paths() {
        return new ArrayList<>(ORES.keySet());
    }

    // ---------------------------------------------------------------- registration

    private static RegistryObject<BlockBodyOre> ore(String path, float hardness, float resistance, Loot loot,
                                                    String kerbin, String oreTag) {
        return ore(path, hardness, resistance, p -> p, loot, kerbin, oreTag);
    }

    private static RegistryObject<BlockBodyOre> ore(String path, float hardness, float resistance,
                                                    UnaryOperator<BlockBehaviour.Properties> extra, Loot loot,
                                                    String kerbin, String oreTag) {
        Registrar.Handle<BlockBodyOre> block = REG.block(path, BlockBodyOre::new,
                        p -> extra.apply(p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops()))
                .model(d -> bodyOre(d, "ntmspace:blocks/" + path)).loot(loot).tag(BlockTag.PICKAXE);
        if (oreTag != null) block.tag(BlockTag.of(oreTag));
        family(path, block, kerbin, oreTag);
        return block;
    }

    private static RegistryObject<BlockBodyOreOutgas> outgas(String path, float hardness, float resistance, Loot loot,
                                                             boolean radon, String kerbin, String oreTag) {
        Registrar.Handle<BlockBodyOreOutgas> block = REG.<BlockBodyOreOutgas>block(path,
                        p -> new BlockBodyOreOutgas(p, radon ? BlockBodyOreOutgas::radon : BlockBodyOreOutgas::asbestos),
                        p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks())
                .model(d -> bodyOre(d, "ntmspace:blocks/" + path)).loot(loot).tag(BlockTag.PICKAXE);
        if (oreTag != null) block.tag(BlockTag.of(oreTag));
        family(path, block, kerbin, oreTag);
        return block;
    }

    private static RegistryObject<BlockBodyOre> fluidOre(String path, String overlay, Loot loot, String kerbin) {
        RegistryObject<BlockBodyOre> block = REG.block(path, BlockBodyOre::new,
                        p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
                .model(d -> bodyOre(d, overlay)).loot(loot).tag(BlockTag.PICKAXE);
        family(path, block, kerbin, null);
        return block;
    }

    /**
     * The family's block items: {@code <path>_<body>} for every body but Kerbin, then {@code <path>} for Kerbin last, so
     * the block's own item ({@code asItem}, the loot's) is the Kerbin form on every loader. Each joins the ore's item
     * tags ({@code ntmspace:ores/<path>}, which the smelting recipes read, and the common ore tag HBM's machines read).
     */
    private static void family(String path, RegistryObject<? extends Block> block, String kerbin, String oreTag) {
        ORES.put(path, block);
        KERBIN.put(path, kerbin);
        List<Body> order = new ArrayList<>(List.of(Body.values()));
        order.remove(Body.KERBIN);
        order.add(Body.KERBIN);
        for (Body body : order) {
            String id = body == Body.KERBIN ? path : path + "_" + body.getSerializedName();
            Function<net.minecraft.world.item.Item.Properties, BodyOreItem> factory = p -> new BodyOreItem(block.get(), p, body);
            Registrar.Handle<BodyOreItem> item = REG.item(id, factory, Compat::blockDescription)
                    .icon(d -> d.shares(NtmSpace.MOD_ID + ":block/" + path + "_" + body.getSerializedName()))
                    .tag(ItemTag.of(NtmSpace.MOD_ID + ":ores/" + path))
                    .tab(HbmTab.RESOURCES);
            if (oreTag != null) item.tag(ItemTag.of(oreTag));
            BodyOre.register(NtmSpace.MOD_ID + ":" + path, body, item);
        }
    }

    /**
     * A per-body ore's models and blockstate: per body, {@code <block>_<body>} = the body's stone under {@code overlay}
     * (parent {@code ntmspace:block/ore_overlay}, cutout).
     */
    public static Dejson.Declaration bodyOre(Dejson.Declaration d, String overlay) {
        Models.Group group = Models.group();
        List<String> select = new ArrayList<>();
        for (Body body : Body.values()) {
            String name = body.getSerializedName();
            group = group.and("_" + name, Models.renderType("minecraft:cutout", Models.parented("ntmspace:block/ore_overlay",
                    Slots.of("stone", body.stoneTexture(), "overlay", overlay).and("particle", body.stoneTexture()))));
            select.add(name);
            select.add("_" + name);
        }
        return d.models(group).states(Variants.select("body", select.toArray(new String[0])));
    }
}
