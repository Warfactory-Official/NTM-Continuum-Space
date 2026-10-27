package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.items.block.DecoBlockItem;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.machine.MachineHydroponic;
import com.ntmspace.block.machine.HydroponicLampCell;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.ntmspace.block.machine.MachineVacuumCircuit;
import com.ntmspace.inventory.machine.ContainerHydroponic;
import com.ntmspace.inventory.machine.ContainerVacuumCircuit;
import com.ntmspace.recipe.VacuumCircuitRecipes;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
import dev.golem.core.dejson.Variants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's progression machines, part B (see {@link SpaceMachines}): the Industrial Greenhouse ({@code hydrobay}) and
 * the Vacuum Solderer ({@code machine_vacuum_circuit}), both hardness 5, resistance 10, machine tab, with 1.7's
 * {@code addStandardInfo} description lines on their items ({@code block.ntmspace.<id>.desc.N}, yellow, always shown as
 * HBM's own {@code DescBlockItem}s are).
 */
public final class SpaceMachinesB {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    public static final RegistryObject<MachineHydroponic> HYDROBAY = REG.<MachineHydroponic>block(
                    "hydrobay", MachineHydroponic::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** 1.7 dummy_beam: no item or own loot; breaking it uses the owner's shared-cell path. */
    public static final RegistryObject<HydroponicLampCell> HYDROBAY_LAMP = REG.block(
                    "hydrobay_lamp", HydroponicLampCell::new,
                    p -> SpaceMachines.machine(BlockMultiblockCell.rigid(p), 10.0F, 12.0F).lightLevel(state -> 15))
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.nothing()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineVacuumCircuit> MACHINE_VACUUM_CIRCUIT = REG.<MachineVacuumCircuit>block(
                    "machine_vacuum_circuit", MachineVacuumCircuit::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<DecoBlockItem> HYDROBAY_ITEM = REG.<MachineHydroponic, DecoBlockItem>blockItem(HYDROBAY,
            (block, p) -> new DecoBlockItem(block, p, desc("hydrobay", 2)), Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<DecoBlockItem> MACHINE_VACUUM_CIRCUIT_ITEM = REG.<MachineVacuumCircuit, DecoBlockItem>blockItem(
            MACHINE_VACUUM_CIRCUIT, (block, p) -> new DecoBlockItem(block, p, desc("machine_vacuum_circuit", 1)),
            Compat::blockDescription).tab(HbmTab.MACHINES);

    public static final RegistryObject<BlockEntityType<TileEntityHydroponic>> HYDROBAY_BE = REG.blockEntity(
            "hydrobay", () -> BlockEntities.ticking(TileEntityHydroponic::new,
                    TileEntityHydroponic::clientTick, TileEntityHydroponic::serverTick, HYDROBAY.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineVacuumCircuit>> VACUUM_CIRCUIT_BE = REG.blockEntity(
            "machine_vacuum_circuit", () -> BlockEntities.ticking(TileEntityMachineVacuumCircuit::new,
                    TileEntityMachineVacuumCircuit::clientTick, TileEntityMachineVacuumCircuit::serverTick, MACHINE_VACUUM_CIRCUIT.get()));

    public static final RegistryObject<MenuType<ContainerHydroponic>> HYDROBAY_MENU =
            REG.menu("hydrobay", () -> Menus.typeWithPos(ContainerHydroponic::new));
    public static final RegistryObject<MenuType<ContainerVacuumCircuit>> VACUUM_CIRCUIT_MENU =
            REG.menu("machine_vacuum_circuit", () -> Menus.typeWithPos(ContainerVacuumCircuit::new));

    /** 1.7 {@code addStandardInfo}: {@code tile.<id>.desc} split at {@code $}, yellow. */
    private static List<Supplier<Component>> desc(String id, int lines) {
        List<Supplier<Component>> out = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            String key = "block.ntmspace." + id + ".desc." + i;
            out.add(() -> Component.translatable(key).withStyle(ChatFormatting.YELLOW));
        }
        return out;
    }

    private SpaceMachinesB() {}

    public static void init() {
        VacuumCircuitRecipes.declare();
    }
}
