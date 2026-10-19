package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.machine.MachineGasDock;
import com.ntmspace.block.machine.MachineMagma;
import com.ntmspace.block.machine.MachineRadiator;
import com.ntmspace.block.machine.MachineSolar;
import com.ntmspace.block.machine.MachineTrollDisturber;
import com.ntmspace.block.machine.StandardInfoBlockItem;
import com.ntmspace.inventory.material.SpaceMaterials;
import com.ntmspace.tileentity.machine.SunPower;
import com.ntmspace.tileentity.machine.TileEntityMachineGasDock;
import com.ntmspace.tileentity.machine.TileEntityMachineMagma;
import com.ntmspace.tileentity.machine.TileEntityMachineSolarPanel;
import com.ntmspace.tileentity.machine.TileEntityRadiator;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's progression machines, part C (see {@link SpaceMachines}): the Magma Drill (hardness 5, resistance 100),
 * the Solar Panel, the Steam Radiator (machine tab), the Gas Giant Harvester (missile tab; all hardness 5, resistance
 * 10) and the creative-only {@code troll_disturber} (hardness 5, resistance 200, no tab). None has a GUI.
 */
public final class SpaceMachinesC {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    public static final RegistryObject<MachineMagma> MACHINE_MAGMA = REG.<MachineMagma>block(
                    "machine_magma", MachineMagma::new, p -> SpaceMachines.machine(p, 5.0F, 100.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineSolar> MACHINE_SOLAR = REG.<MachineSolar>block(
                    "machine_solar", MachineSolar::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineRadiator> MACHINE_RADIATOR = REG.<MachineRadiator>block(
                    "machine_radiator", MachineRadiator::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineGasDock> GAS_DOCK = REG.<MachineGasDock>block(
                    "gas_dock", MachineGasDock::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** 1.7 texture {@code field_disturber}; {@code tickRandomly}. */
    public static final RegistryObject<MachineTrollDisturber> TROLL_DISTURBER = REG.<MachineTrollDisturber>block(
                    "troll_disturber", MachineTrollDisturber::new,
                    p -> p.strength(5.0F, 200.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().randomTicks())
            .transform(Presets.cubeAll("ntmspace:blocks/field_disturber")).loot(Loot.self()).tag(BlockTag.PICKAXE);

    /** 1.7 {@code MachineMagma} is an {@code ITooltipProvider} ({@code addStandardInfo}). */
    public static final RegistryObject<StandardInfoBlockItem> MACHINE_MAGMA_ITEM = REG.<MachineMagma, StandardInfoBlockItem>blockItem(
            MACHINE_MAGMA, (b, p) -> new StandardInfoBlockItem(b, p, "machine_magma"), Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_SOLAR_ITEM = REG.blockItem(MACHINE_SOLAR).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_RADIATOR_ITEM = REG.blockItem(MACHINE_RADIATOR).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> GAS_DOCK_ITEM = REG.blockItem(GAS_DOCK).tab(HbmTab.MISSILES);
    /** {@code setCreativeTab(null)}: the item exists, in no tab. */
    public static final RegistryObject<BlockItem> TROLL_DISTURBER_ITEM = REG.blockItem(TROLL_DISTURBER);

    public static final RegistryObject<BlockEntityType<TileEntityMachineMagma>> MAGMA_BE = REG.blockEntity(
            "machine_magma", () -> BlockEntities.ticking(TileEntityMachineMagma::new,
                    TileEntityMachineMagma::clientTick, TileEntityMachineMagma::serverTick, MACHINE_MAGMA.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineSolarPanel>> SOLAR_BE = REG.blockEntity(
            "machine_solar", () -> BlockEntities.ticking(TileEntityMachineSolarPanel::new,
                    null, TileEntityMachineSolarPanel::serverTick, MACHINE_SOLAR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadiator>> RADIATOR_BE = REG.blockEntity(
            "machine_radiator", () -> BlockEntities.ticking(TileEntityRadiator::new,
                    null, TileEntityRadiator::serverTick, MACHINE_RADIATOR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineGasDock>> GAS_DOCK_BE = REG.blockEntity(
            "gas_dock", () -> BlockEntities.ticking(TileEntityMachineGasDock::new,
                    TileEntityMachineGasDock::clientTick, TileEntityMachineGasDock::serverTick, GAS_DOCK.get()));

    private SpaceMachinesC() {}

    public static void init() {
        SpaceMaterials.register();
        SunPower.init();
    }
}
