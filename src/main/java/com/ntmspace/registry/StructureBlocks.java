package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The blocks and items hbm-space's planet structures need that no other part of the port registers. Values are
 * hbm-space's {@code ModBlocks} / {@code ModItems}.
 */
public final class StructureBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    /**
     * 1.7: {@code BlockDecoCT(Material.iron)}, hardness 5, resistance 10, blocks tab; the stainless panels of the Mun,
     * Minmus and Moho bases, the Duna comms station and the Laythe submarine. Its connected texture
     * ({@code deco_stainless_ct}) is Golem Core's {@code ConnectedTextures} ({@code client/StructureClient}).
     */
    public static final RegistryObject<Block> DECO_STAINLESS = REG.simpleBlock("deco_stainless",
                    p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> DECO_STAINLESS_ITEM = REG.blockItem(DECO_STAINLESS).tab(HbmTab.BLOCKS);

    /**
     * 1.7: a plain {@code Item}, no creative tab. Ike's populate puts one on every pedestal of a freshly generated chunk
     * ({@code WorldGeneratorIke}: the artifact's pedestal), and the angel's spawn item is crafted from it.
     */
    public static final RegistryObject<Item> DIVINE_SHARD = REG.simpleItem("divine_shard", p -> p)
            .transform(Presets.sprite());

    private StructureBlocks() {}

    public static void init() {}
}
