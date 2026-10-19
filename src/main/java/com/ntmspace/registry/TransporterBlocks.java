package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.items.MachineObjModel;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.transporter.BlockTransporterRocket;
import com.ntmspace.inventory.ContainerTransporterRocket;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Variants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's cargo transporter: the Rocket Transporter Pad ({@code transporter_rocket}, {@code ModBlocks}: Material.iron,
 * hardness 1, machine tab) and the Transporter Pad Linker ({@code transporter_linker}, {@code ModItems}: stack size 1,
 * control tab). The pad is drawn by its renderer ({@code client/render/transporter}); its item is an OBJ item
 * ({@link #declare()}).
 */
public final class TransporterBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    /** A renderer-drawn multiblock: facing x cell, the shared empty placeholder model. */
    private static final Presets.BlockModel FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    /** 1.7: {@code BlockTransporterRocket(Material.iron)}, hardness 1, machine tab. */
    public static final RegistryObject<BlockTransporterRocket> TRANSPORTER_ROCKET = REG.<BlockTransporterRocket>block("transporter_rocket",
                    BlockTransporterRocket::new,
                    p -> p.strength(1.0F).sound(SoundType.METAL).noOcclusion()
                            .isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> TRANSPORTER_ROCKET_ITEM = REG.blockItem(TRANSPORTER_ROCKET).tab(HbmTab.MACHINES);

    /** 1.7: {@code ItemTransporterLinker}, stack size 1, control tab, texture {@code transporter_linker}. */
    public static final RegistryObject<ItemTransporterLinker> TRANSPORTER_LINKER = REG.item("transporter_linker",
                    ItemTransporterLinker::new, p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.CONTROL);

    /** 1.7 {@code TileMappings}: {@code tileentity_transporter}; the modern id is the block's. */
    public static final RegistryObject<BlockEntityType<TileEntityTransporterRocket>> TRANSPORTER_ROCKET_BE = REG.blockEntity(
            "transporter_rocket", () -> BlockEntities.ticking(TileEntityTransporterRocket::new, TileEntityTransporterRocket::clientTick,
                    TileEntityTransporterRocket::serverTick, TRANSPORTER_ROCKET.get()));

    public static final RegistryObject<MenuType<ContainerTransporterRocket>> TRANSPORTER_ROCKET_MENU =
            REG.menu("transporter_rocket", () -> Menus.typeWithPos(ContainerTransporterRocket::new));

    private TransporterBlocks() {}

    public static void init() {
        ItemTransporterLinker.declarePayload();
    }

    private static boolean declared;

    /**
     * Dejson: the pad's OBJ item, as {@code RenderTransporterRocket.getRenderer} drew it ({@code renderInventory}:
     * scale 3; {@code renderCommon}: translate(0.5, 0, 0), the whole {@code transporter_pad} model).
     */
    public static void declare() {
        if (declared) return;
        declared = true;

        Dejson.item(NtmSpace.MOD_ID, "transporter_rocket")
                .definition(MachineObjModel.of("ntmspace:models/machines/transporter_pad.obj",
                                "ntmspace:textures/models/machines/transporter_pad.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.5, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 0.0, 0.0))
                .oversizedInGui().register();
    }
}
