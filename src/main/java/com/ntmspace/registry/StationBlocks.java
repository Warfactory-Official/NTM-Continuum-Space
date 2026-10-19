package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.station.BlockOrbitalStation;
import com.ntmspace.block.station.BlockOrbitalStationComputer;
import com.ntmspace.block.station.BlockStationPropulsionCreative;
import com.ntmspace.block.station.MachineHTR3;
import com.ntmspace.block.station.MachineHTRF4;
import com.ntmspace.block.station.MachineHTRFNeo;
import com.ntmspace.block.station.MachineXenonThruster;
import com.ntmspace.inventory.ContainerOrbitalStationComputer;
import com.ntmspace.tileentity.station.TileEntityMachineHTR3;
import com.ntmspace.tileentity.station.TileEntityMachineHTRF4;
import com.ntmspace.tileentity.station.TileEntityMachineHTRNeo;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationComputer;
import com.ntmspace.tileentity.station.TileEntityStationPropulsionCreative;
import com.ntmspace.tileentity.station.TileEntityXenonThruster;
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
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The orbital station's blocks and the station thrusters, hbm-space's {@code ModBlocks} values: the station core
 * ({@code orbital_station}: unbreakable, no tab, never placed by hand) and its extra docking ports, the station computer,
 * the creative engine, and the LPW-3N (HTR3), HTR-F4, HTR-F4 NEO and xenon thrusters (hardness 5, resistance 100). The
 * multiblocks are drawn by their renderers ({@code client/render/station}); their items are OBJ items
 * ({@link StationItemDeclarations}).
 *
 * <p>Not here: {@code orbital_station_launcher}, which builds and launches rockets (the rocket part of the port).
 */
public final class StationBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    /** A renderer-drawn multiblock: facing x cell, the shared empty placeholder model. */
    private static final Presets.BlockModel FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    private static BlockBehaviour.Properties dummyable(BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false).noOcclusion();
    }

    /** 1.7: {@code setBlockUnbreakable().setResistance(Float.POSITIVE_INFINITY)}, no creative tab. */
    public static final RegistryObject<BlockOrbitalStation> ORBITAL_STATION = REG.<BlockOrbitalStation>block("orbital_station",
                    p -> new BlockOrbitalStation(p, true),
                    p -> dummyable(p.strength(-1.0F, 3_600_000.0F).sound(SoundType.METAL).noLootTable()))
            .transform(FOLDED);
    /** 1.7: hardness 1. */
    public static final RegistryObject<BlockOrbitalStation> ORBITAL_STATION_PORT = REG.<BlockOrbitalStation>block("orbital_station_port",
                    p -> new BlockOrbitalStation(p, false),
                    p -> dummyable(p.strength(1.0F).sound(SoundType.METAL)))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** 1.7: hardness 1. */
    public static final RegistryObject<BlockOrbitalStationComputer> ORBITAL_STATION_COMPUTER = REG.<BlockOrbitalStationComputer>block(
                    "orbital_station_computer", BlockOrbitalStationComputer::new,
                    p -> dummyable(p.strength(1.0F).sound(SoundType.METAL)))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** 1.7: {@code BlockStationPropulsionCreative}, hardness 1, the steel block texture. */
    public static final RegistryObject<BlockStationPropulsionCreative> PROPULSION_CREATIVE = REG.<BlockStationPropulsionCreative>block(
                    "propulsion_creative", BlockStationPropulsionCreative::new, p -> p.strength(1.0F).sound(SoundType.METAL))
            .transform(Presets.cubeAll("hbm:blocks/block_steel")).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineHTR3> MACHINE_HTR3 = REG.<MachineHTR3>block("machine_htr3", MachineHTR3::new,
                    p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineHTRF4> MACHINE_HTRF4 = REG.<MachineHTRF4>block("machine_htrf4", MachineHTRF4::new,
                    p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineHTRFNeo> MACHINE_HTRF4NEO = REG.<MachineHTRFNeo>block("machine_htrf4neo", MachineHTRFNeo::new,
                    p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineXenonThruster> MACHINE_XENON_THRUSTER = REG.<MachineXenonThruster>block(
                    "machine_xenon_thruster", MachineXenonThruster::new,
                    p -> dummyable(p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    // items: 1.7 registered an ItemBlock for each; the core's has no tab
    public static final RegistryObject<BlockItem> ORBITAL_STATION_ITEM = REG.blockItem(ORBITAL_STATION);
    public static final RegistryObject<BlockItem> ORBITAL_STATION_PORT_ITEM = REG.blockItem(ORBITAL_STATION_PORT).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> ORBITAL_STATION_COMPUTER_ITEM = REG.blockItem(ORBITAL_STATION_COMPUTER).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> PROPULSION_CREATIVE_ITEM = REG.blockItem(PROPULSION_CREATIVE).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_HTR3_ITEM = REG.blockItem(MACHINE_HTR3).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_HTRF4_ITEM = REG.blockItem(MACHINE_HTRF4).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_HTRF4NEO_ITEM = REG.blockItem(MACHINE_HTRF4NEO).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_XENON_THRUSTER_ITEM = REG.blockItem(MACHINE_XENON_THRUSTER).tab(HbmTab.MACHINES);

    // block entities
    public static final RegistryObject<BlockEntityType<TileEntityOrbitalStation>> ORBITAL_STATION_BE = REG.blockEntity("orbital_station",
            () -> BlockEntities.ticking(TileEntityOrbitalStation::new, TileEntityOrbitalStation::clientTick,
                    TileEntityOrbitalStation::serverTick, ORBITAL_STATION.get(), ORBITAL_STATION_PORT.get()));
    public static final RegistryObject<BlockEntityType<TileEntityOrbitalStationComputer>> ORBITAL_STATION_COMPUTER_BE = REG.blockEntity(
            "orbital_station_computer", () -> BlockEntities.serverTicking(TileEntityOrbitalStationComputer::new,
                    TileEntityOrbitalStationComputer::serverTick, ORBITAL_STATION_COMPUTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityStationPropulsionCreative>> PROPULSION_CREATIVE_BE = REG.blockEntity(
            "propulsion_creative", () -> BlockEntities.serverTicking(TileEntityStationPropulsionCreative::new,
                    TileEntityStationPropulsionCreative::serverTick, PROPULSION_CREATIVE.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineHTR3>> HTR3_BE = REG.blockEntity("machine_htr3",
            () -> BlockEntities.ticking(TileEntityMachineHTR3::new, TileEntityMachineHTR3::clientTick,
                    TileEntityMachineHTR3::serverTick, MACHINE_HTR3.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineHTRF4>> HTRF4_BE = REG.blockEntity("machine_htrf4",
            () -> BlockEntities.ticking(TileEntityMachineHTRF4::new, TileEntityMachineHTRF4::clientTick,
                    TileEntityMachineHTRF4::serverTick, MACHINE_HTRF4.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineHTRNeo>> HTRF4_NEO_BE = REG.blockEntity("machine_htrf4neo",
            () -> BlockEntities.ticking(TileEntityMachineHTRNeo::new, TileEntityMachineHTRNeo::clientTick,
                    TileEntityMachineHTRNeo::serverTick, MACHINE_HTRF4NEO.get()));
    public static final RegistryObject<BlockEntityType<TileEntityXenonThruster>> XENON_THRUSTER_BE = REG.blockEntity(
            "machine_xenon_thruster", () -> BlockEntities.ticking(TileEntityXenonThruster::new, TileEntityXenonThruster::clientTick,
                    TileEntityXenonThruster::serverTick, MACHINE_XENON_THRUSTER.get()));

    public static final RegistryObject<MenuType<ContainerOrbitalStationComputer>> ORBITAL_STATION_COMPUTER_MENU =
            REG.menu("orbital_station_computer", () -> Menus.typeWithPos(ContainerOrbitalStationComputer::new));

    private StationBlocks() {}

    public static void init() {}
}
