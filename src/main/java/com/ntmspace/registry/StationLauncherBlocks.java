package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.items.MachineObjModel;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.station.BlockOrbitalStationLauncher;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.inventory.station.ContainerOrbitalStationLauncher;
import com.ntmspace.item.MachineDescItem;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationLauncher;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Variants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's orbital station launcher ({@code orbital_station_launcher}, "Orbital Station Launching Port"): hardness 1,
 * the machine tab, its block entity and menu. Drawn by its renderer ({@code client/render/station/StationLauncherRenderer});
 * its item is the docking port OBJ with the launcher skin (1.7 {@code RenderOrbitalStation}'s item renderer).
 *
 * <p>Also registers the launcher as a {@link EntityRideableRocket#DOCK_TARGETS} entry: a parked rocket re-docks to the
 * launcher it stands on after a reload (1.7 {@code EntityRideableRocket.sizeSet}).
 */
public final class StationLauncherBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    /** A renderer-drawn multiblock: facing x cell, the shared empty placeholder model. */
    private static final Presets.BlockModel FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    /** 1.7: {@code setHardness(1.0F)}, machine tab. */
    public static final RegistryObject<BlockOrbitalStationLauncher> ORBITAL_STATION_LAUNCHER = REG.<BlockOrbitalStationLauncher>block(
                    "orbital_station_launcher", BlockOrbitalStationLauncher::new,
                    p -> p.strength(1.0F).sound(SoundType.METAL)
                            .isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false).noOcclusion())
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    /** 1.7 {@code ITooltipProvider.addStandardInfo}: the {@code .desc} line. */
    public static final RegistryObject<MachineDescItem> ORBITAL_STATION_LAUNCHER_ITEM = REG.<BlockOrbitalStationLauncher, MachineDescItem>blockItem(
            ORBITAL_STATION_LAUNCHER, MachineDescItem::new, Compat::blockDescription).tab(HbmTab.MACHINES);

    public static final RegistryObject<BlockEntityType<TileEntityOrbitalStationLauncher>> ORBITAL_STATION_LAUNCHER_BE = REG.blockEntity(
            "orbital_station_launcher", () -> BlockEntities.ticking(TileEntityOrbitalStationLauncher::new,
                    TileEntityOrbitalStationLauncher::clientTick, TileEntityOrbitalStationLauncher::serverTick, ORBITAL_STATION_LAUNCHER.get()));

    public static final RegistryObject<MenuType<ContainerOrbitalStationLauncher>> ORBITAL_STATION_LAUNCHER_MENU =
            REG.menu("orbital_station_launcher", () -> Menus.typeWithPos(ContainerOrbitalStationLauncher::new));

    private static boolean initialised;
    private static boolean declared;

    private StationLauncherBlocks() {}

    public static void init() {
        if (initialised) return;
        initialised = true;
        // 1.7 sizeSet: a parked rocket on a launcher docks to it again
        EntityRideableRocket.DOCK_TARGETS.add((level, core, rocket) -> {
            if (level.getBlockEntity(core) instanceof TileEntityOrbitalStationLauncher launcher) launcher.dockRocket(rocket);
        });
    }

    /** The item: {@code RenderOrbitalStation}'s item renderer, inv translate(0, 2, 0) scale(2), cull off, launcher skin. */
    public static void declare() {
        if (declared) return;
        declared = true;
        Dejson.item(NtmSpace.MOD_ID, "orbital_station_launcher")
                .definition(MachineObjModel.of("ntmspace:models/machines/docking_port.obj", "ntmspace:textures/models/machines/docking_port_launcher.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(2.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 2.0, 0.0))
                .oversizedInGui().register();
    }
}
