package com.ntmspace.registry;

import com.hbm.blocks.generic.BlockResourceStone;
import com.hbm.blocks.generic.BlockStalagmite;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The blocks hbm-space's populate places that no other part of the port registers: Laythe's tumor caves (and their
 * flesh spikes) and the conglomerate layers of the Mun, Ike and Minmus. Values are hbm-space's {@code ModBlocks}.
 */
public final class PopulateBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    /**
     * 1.7: {@code BlockTint(Material.clay, "tumor_base2", "tumor_base3", "fleshv2")}, texture {@code tumor_base}, hardness
     * 1, snow sound: a per-position pick of its four textures ({@code (hash >> 16) & 3}), here the renderer's random pool.
     */
    public static final RegistryObject<Block> TUMOR = REG.simpleBlock("tumor", p -> p.strength(1.0F).sound(SoundType.SNOW))
            .model(d -> d.models(Models.group()
                            .and("_0", Models.cubeAll("ntmspace:blocks/tumor_base"))
                            .and("_1", Models.cubeAll("ntmspace:blocks/tumor_base2"))
                            .and("_2", Models.cubeAll("ntmspace:blocks/tumor_base3"))
                            .and("_3", Models.cubeAll("ntmspace:blocks/fleshv2")))
                    .states(Variants.pool(4)))
            .loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> TUMOR_ITEM = REG.blockItem(TUMOR)
            .icon(d -> d.models(Models.blockItem("ntmspace:block/tumor_0"))).tab(HbmTab.BLOCKS);

    /**
     * 1.7: {@code stone_resource} with hbm-space's {@code EnumStoneType.CONGLOMERATE}, hardness 5, resistance 10; HBM
     * splits {@code stone_resource} into {@code stone_resource_<type>} ({@link BlockResourceStone}).
     */
    public static final RegistryObject<BlockResourceStone> STONE_RESOURCE_CONGLOMERATE = REG.block("stone_resource_conglomerate",
                    p -> new BlockResourceStone(p, "conglomerate"),
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable())
            .transform(Presets.cubeAll("ntmspace:blocks/stone_resource.conglomerate")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> STONE_RESOURCE_CONGLOMERATE_ITEM = REG.blockItem(STONE_RESOURCE_CONGLOMERATE,
            Compat::blockDescription).tab(HbmTab.RESOURCES);

    /** 1.7: {@code stalagmite} / {@code stalactite} metadata 2 ({@code EnumStalagmiteType.FLESH}): drops flesh. */
    public static final RegistryObject<BlockStalagmite> STALAGMITE_FLESH = spike("stalagmite_flesh", Direction.DOWN);
    public static final RegistryObject<BlockStalagmite> STALACTITE_FLESH = spike("stalactite_flesh", Direction.UP);

    private PopulateBlocks() {}

    public static void init() {}

    private static RegistryObject<BlockStalagmite> spike(String id, Direction anchor) {
        String texture = "ntmspace:blocks/" + id.replace("_flesh", ".flesh");
        RegistryObject<BlockStalagmite> block = REG.block(id, p -> new BlockStalagmite(p, anchor, PopulateBlocks::flesh),
                        p -> p.strength(0.5F, 2.0F).sound(SoundType.STONE).noOcclusion().noCollision().noLootTable())
                .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross(texture))).states(Variants.simple()))
                .tag(BlockTag.PICKAXE);
        REG.blockItem(block, Compat::blockDescription).icon(d -> d.models(Models.generated(texture))).tab(HbmTab.RESOURCES);
        return block;
    }

    /** hbm-space's {@code flesh} item, when its port registers it ({@code ntmspace:flesh}); nothing until then. */
    private static ItemStack flesh() {
        return BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.parse("ntmspace:flesh"))
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
