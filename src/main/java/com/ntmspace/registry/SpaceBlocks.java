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
import net.minecraft.world.level.block.Block;

/**
 * NTM: Space's blocks, registered through Golem Core's registration chain under {@code ntmspace:} and listed in HBM's
 * creative tabs, as hbm-space listed them in 1.7's ({@code MainRegistry.blockTab}).
 *
 * <p>Values are hbm-space's {@code ModBlocks} ones; resistance is written as 1.7 stated it, as HBM writes its own.
 */
public final class SpaceBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    /** 1.7: {@code BlockGeneric(Material.rock)}, hardness 1.5, resistance 10, stone sound. */
    public static final RegistryObject<Block> MOON_ROCK = REG.simpleBlock("moon_rock",
                    p -> p.strength(1.5F, 10.0F).sound(net.minecraft.world.level.block.SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> MOON_ROCK_ITEM = REG.blockItem(MOON_ROCK).tab(HbmTab.BLOCKS);

    // ore_brine: registry/BodyOreBlocks owns the per-body ore family

    private SpaceBlocks() {}

    public static void init() {}
}
