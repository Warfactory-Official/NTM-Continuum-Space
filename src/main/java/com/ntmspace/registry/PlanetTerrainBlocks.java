package com.ntmspace.registry;

import com.hbm.blocks.generic.BlockFallingBase;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.block.DecoBlockItem;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The terrain blocks of Eve, Moho and Minmus, from hbm-space's {@code ModBlocks} (values as 1.7 stated them, the
 * texture names of 1.7's {@code setBlockTextureName}), in HBM's blocks tab as 1.7 listed them in its block tab. Eve's
 * natural gas deposit ({@code ore_gas}) is one of hbm-space's per-body fluid ores, registered with that family.
 */
public final class PlanetTerrainBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    // ---------------------------------------------------------------- Eve

    /** 1.7: {@code BlockFalling(Material.sand)}, hardness 0.5, sand sound. */
    public static final RegistryObject<BlockFallingBase> EVE_SILT = REG.block("eve_silt", BlockFallingBase::new,
                    p -> p.strength(0.5F).sound(SoundType.SAND))
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockItem> EVE_SILT_ITEM = REG.blockItem(EVE_SILT).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BlockGeneric(Material.rock)}, hardness 1.5, resistance 10, stone sound; texture {@code eve_stone_2}. */
    public static final RegistryObject<Block> EVE_ROCK = REG.simpleBlock("eve_rock", PlanetTerrainBlocks::rock)
            .transform(Presets.cubeAll("ntmspace:blocks/eve_stone_2")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> EVE_ROCK_ITEM = REG.blockItem(EVE_ROCK).tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- Moho

    /** 1.7: {@code BlockGeneric(Material.rock)}, hardness 1.5, resistance 10, stone sound. */
    public static final RegistryObject<Block> MOHO_REGOLITH = REG.simpleBlock("moho_regolith", PlanetTerrainBlocks::rock)
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> MOHO_REGOLITH_ITEM = REG.blockItem(MOHO_REGOLITH).tab(HbmTab.BLOCKS);

    public static final RegistryObject<Block> MOHO_STONE = REG.simpleBlock("moho_stone", PlanetTerrainBlocks::rock)
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> MOHO_STONE_ITEM = REG.blockItem(MOHO_STONE).tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- Minmus

    public static final RegistryObject<Block> MINMUS_REGOLITH = REG.simpleBlock("minmus_regolith", PlanetTerrainBlocks::rock)
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> MINMUS_REGOLITH_ITEM = REG.blockItem(MINMUS_REGOLITH).tab(HbmTab.BLOCKS);

    public static final RegistryObject<Block> MINMUS_STONE = REG.simpleBlock("minmus_stone", PlanetTerrainBlocks::rock)
            .transform(Presets.cubeAll()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> MINMUS_STONE_ITEM = REG.blockItem(MINMUS_STONE).tab(HbmTab.BLOCKS);

    /**
     * 1.7: texture {@code minflat_regolith}, registered with {@code ItemBlockLore}, whose tooltip is the
     * {@code .desc} translation (grey, as 1.7 drew every tooltip line after the name).
     */
    public static final RegistryObject<Block> MINMUS_SMOOTH = REG.simpleBlock("minmus_smooth", PlanetTerrainBlocks::rock)
            .transform(Presets.cubeAll("ntmspace:blocks/minflat_regolith")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    private static final Supplier<Component> MINMUS_SMOOTH_DESC =
            () -> Component.translatable("block.ntmspace.minmus_smooth.desc").withStyle(ChatFormatting.GRAY);
    public static final RegistryObject<DecoBlockItem> MINMUS_SMOOTH_ITEM = REG.item("minmus_smooth",
                    (net.minecraft.world.item.Item.Properties p) -> new DecoBlockItem(MINMUS_SMOOTH.get(),
                            dev.golem.core.world.Compat.blockDescription(p), List.of(MINMUS_SMOOTH_DESC)))
            .tab(HbmTab.BLOCKS);

    private PlanetTerrainBlocks() {}

    /** {@code BlockGeneric(Material.rock)}, hardness 1.5, resistance 10, stone sound. */
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties rock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.strength(1.5F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static void init() {}
}
