package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.machine.MachineDishControl;
import com.ntmspace.block.machine.MachineStardar;
import com.ntmspace.block.machine.StardarSensorLink;
import com.ntmspace.inventory.machine.ContainerStardar;
import com.ntmspace.item.MachineDescItem;
import com.ntmspace.tileentity.machine.TileEntityDishControl;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's progression machines, part D: the StarDar ({@code machine_stardar}) and its remote access console
 * ({@code dish_control}), hardness 5, resistance 10, machine tab, both drawn by their renderers
 * ({@code client/render/machine/MachineRenderersD}); their items are OBJ items ({@link MachineItemDeclarationsD}) with
 * 1.7's {@code addStandardInfo} description lines. The reactor sensor links them through HBM's
 * {@code ItemReactorSensor.USE} hook ({@link StardarSensorLink}).
 */
public final class SpaceMachinesD {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    public static final RegistryObject<MachineStardar> MACHINE_STARDAR = REG.<MachineStardar>block(
                    "machine_stardar", MachineStardar::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDishControl> DISH_CONTROL = REG.<MachineDishControl>block(
                    "dish_control", MachineDishControl::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineDescItem> MACHINE_STARDAR_ITEM = REG.<MachineStardar, MachineDescItem>blockItem(
            MACHINE_STARDAR, MachineDescItem::new, Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<MachineDescItem> DISH_CONTROL_ITEM = REG.<MachineDishControl, MachineDescItem>blockItem(
            DISH_CONTROL, MachineDescItem::new, Compat::blockDescription).tab(HbmTab.MACHINES);

    public static final RegistryObject<BlockEntityType<TileEntityMachineStardar>> STARDAR_BE = REG.blockEntity(
            "machine_stardar", () -> BlockEntities.ticking(TileEntityMachineStardar::new,
                    TileEntityMachineStardar::clientTick, TileEntityMachineStardar::serverTick, MACHINE_STARDAR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDishControl>> DISH_CONTROL_BE = REG.blockEntity(
            "dish_control", () -> BlockEntities.ticking(TileEntityDishControl::new,
                    TileEntityDishControl::clientTick, TileEntityDishControl::serverTick, DISH_CONTROL.get()));

    public static final RegistryObject<MenuType<ContainerStardar>> STARDAR_MENU =
            REG.menu("machine_stardar", () -> Menus.typeWithPos(ContainerStardar::new));

    private SpaceMachinesD() {}

    public static void init() {
        StardarSensorLink.register();
    }
}
