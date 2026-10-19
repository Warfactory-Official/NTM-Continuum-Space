package com.ntmspace.registry;

import com.hbm.blocks.generic.BlockFallingBase;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.BlockBodyOre;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The terrain blocks of Duna, Ike and Dres and hbm-space's per-body fluid ores, with hbm-space's {@code ModBlocks}
 * values (resistance written as 1.7 stated it, as HBM writes its own) in hbm-space's block tab.
 *
 * <p>The per-body ores ({@link BlockBodyOre}) draw each body's stone under the ore overlay: one model per body, parented
 * on the authored {@code ntmspace:block/ore_overlay}. They have one item (the Kerbin form); the 1.7 item kept the body in
 * its damage, the body form of a placed item is a later step.
 */
public final class DunaBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    // ---- Duna

    /** 1.7: {@code BlockGenericDrop(Material.rock, duna_cobble)}, hardness 1.5, resistance 10; drops Duna cobblestone. */
    public static final RegistryObject<Block> DUNA_ROCK = REG.simpleBlock("duna_rock",
                    p -> p.strength(1.5F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll()).loot(Loot.oreNoFortune("ntmspace:duna_cobble", 1, 1)).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> DUNA_ROCK_ITEM = REG.blockItem(DUNA_ROCK).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockFalling(Material.sand)}, hardness 0.5, sand sound. */
    public static final RegistryObject<BlockFallingBase> DUNA_SANDS = REG.block("duna_sands", BlockFallingBase::new,
                    p -> p.strength(0.5F).sound(SoundType.SAND))
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> DUNA_SANDS_ITEM = REG.blockItem(DUNA_SANDS).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockGeneric(Material.rock)}, hardness 2, resistance 10. */
    public static final RegistryObject<Block> DUNA_COBBLE = REG.simpleBlock("duna_cobble",
                    p -> p.strength(2.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> DUNA_COBBLE_ITEM = REG.blockItem(DUNA_COBBLE).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockGeneric(Material.clay)}, hardness 0.6, gravel sound. */
    public static final RegistryObject<Block> FERRIC_CLAY = REG.simpleBlock("ferric_clay",
                    p -> p.strength(0.6F).sound(SoundType.GRAVEL))
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> FERRIC_CLAY_ITEM = REG.blockItem(FERRIC_CLAY).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockGeneric(Material.ice)}, hardness 0.5, stone sound, texture {@code ice_dry}; an opaque cube. */
    public static final RegistryObject<Block> DRY_ICE = REG.simpleBlock("dry_ice",
                    p -> p.strength(0.5F).sound(SoundType.STONE))
            .transform(Presets.cubeAll("ntmspace:blocks/ice_dry")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> DRY_ICE_ITEM = REG.blockItem(DRY_ICE).tab(HbmTab.BLOCKS);

    /**
     * 1.7: {@code stone_resource} with hbm-space's {@code EnumStoneType.CALCIUM} ("Calcium"), hardness 5, resistance 10,
     * dropping itself; HBM splits {@code stone_resource} into {@code stone_resource_<type>}, this is hbm-space's type.
     */
    public static final RegistryObject<Block> STONE_RESOURCE_CALCIUM = REG.simpleBlock("stone_resource_calcium",
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll("ntmspace:blocks/stone_resource.calcium")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> STONE_RESOURCE_CALCIUM_ITEM = REG.blockItem(STONE_RESOURCE_CALCIUM).tab(HbmTab.RESOURCES);

    // ---- Ike

    /** 1.7: {@code BlockGeneric(Material.rock)}, hardness 1.5, resistance 10. */
    public static final RegistryObject<Block> IKE_STONE = REG.simpleBlock("ike_stone",
                    p -> p.strength(1.5F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> IKE_STONE_ITEM = REG.blockItem(IKE_STONE).tab(HbmTab.BLOCKS);

    /**
     * 1.7: {@code BlockTint(Material.rock, "ike_regolith2", ..., "ike_regolith5")}, hardness 1.5, resistance 10: a
     * per-position pick of its textures ({@code (hash >> 16) & 3}, so the first four), here the renderer's random pool
     * of the same four. Its metadata tint is white for every generated block.
     */
    public static final RegistryObject<Block> IKE_REGOLITH = REG.simpleBlock("ike_regolith",
                    p -> p.strength(1.5F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                            .and("_0", Models.cubeAll("ntmspace:blocks/ike_regolith"))
                            .and("_1", Models.cubeAll("ntmspace:blocks/ike_regolith2"))
                            .and("_2", Models.cubeAll("ntmspace:blocks/ike_regolith3"))
                            .and("_3", Models.cubeAll("ntmspace:blocks/ike_regolith4")))
                    .states(Variants.pool(4)))
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> IKE_REGOLITH_ITEM = REG.blockItem(IKE_REGOLITH)
            .icon(d -> d.models(Models.blockItem("ntmspace:block/ike_regolith_0"))).tab(HbmTab.BLOCKS);

    // ---- Dres

    /** 1.7: {@code BlockGeneric(Material.rock)}, hardness 1.5, resistance 10, texture {@code dresbase}. */
    public static final RegistryObject<Block> DRES_ROCK = REG.simpleBlock("dres_rock",
                    p -> p.strength(1.5F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll("ntmspace:blocks/dresbase")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> DRES_ROCK_ITEM = REG.blockItem(DRES_ROCK).tab(HbmTab.BLOCKS);

    // ---- per-body fluid ores: registry/BodyOreBlocks owns the family (ore_oil, ore_brine, ore_tekto, ore_gas, ...)

    private DunaBlocks() {}

    /** A per-body ore's models and blockstate ({@link com.ntmspace.registry.BodyOreBlocks#bodyOre}). */
    public static dev.golem.core.dejson.Dejson.Declaration bodyOre(dev.golem.core.dejson.Dejson.Declaration d, String overlay) {
        return BodyOreBlocks.bodyOre(d, overlay);
    }

    public static void init() {}
}
