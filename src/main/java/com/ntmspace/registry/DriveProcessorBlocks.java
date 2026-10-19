package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.items.MachineObjModel;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.rocket.MachineDriveProcessor;
import com.ntmspace.inventory.rocket.ContainerDriveProcessor;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's drive processor ({@code ModBlocks.machine_drive_processor}: hardness 5, resistance 10, machine tab),
 * its block entity, menu and OBJ item ({@code RenderDriveProcessor.getRenderer}: {@code renderInventory} scale 7,
 * {@code renderCommon} translate (0.5, 0, 0), {@code Base} and {@code Drive}).
 */
public final class DriveProcessorBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    public static final RegistryObject<MachineDriveProcessor> MACHINE_DRIVE_PROCESSOR = REG.<MachineDriveProcessor>block(
                    "machine_drive_processor", MachineDriveProcessor::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> MACHINE_DRIVE_PROCESSOR_ITEM = REG.blockItem(MACHINE_DRIVE_PROCESSOR).tab(HbmTab.MACHINES);

    public static final RegistryObject<BlockEntityType<TileEntityMachineDriveProcessor>> DRIVE_PROCESSOR_BE = REG.blockEntity(
            "machine_drive_processor", () -> BlockEntities.ticking(TileEntityMachineDriveProcessor::new,
                    TileEntityMachineDriveProcessor::clientTick, TileEntityMachineDriveProcessor::serverTick, MACHINE_DRIVE_PROCESSOR.get()));

    public static final RegistryObject<MenuType<ContainerDriveProcessor>> DRIVE_PROCESSOR_MENU =
            REG.menu("machine_drive_processor", () -> Menus.typeWithPos(ContainerDriveProcessor::new));

    private static boolean declared;

    private DriveProcessorBlocks() {}

    public static void init() {}

    /** The OBJ item, run by Dejson at build time. */
    public static void declare() {
        if (declared) return;
        declared = true;

        Dejson.item(NtmSpace.MOD_ID, "machine_drive_processor")
                .definition(MachineObjModel.of("ntmspace:models/machines/drive_processor.obj", "ntmspace:textures/models/machines/drive_processor.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(7.0).rawLegacyTransform()
                        .itemTranslate(0.5, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 0.0, 0.0)
                        .parts("Base", "Drive"))
                .oversizedInGui().register();
    }
}
