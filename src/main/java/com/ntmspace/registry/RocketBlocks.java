package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.rocket.LaunchPadRocket;
import com.ntmspace.block.rocket.MachineRocketAssembly;
import com.ntmspace.inventory.rocket.ContainerLaunchPadRocket;
import com.ntmspace.inventory.rocket.ContainerMachineRocketAssembly;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
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
 * The rocket family's machines, hbm-space's {@code ModBlocks} values: the Vehicle Assembly Building
 * ({@code machine_rocket_assembly}) and the rocket launch pad ({@code launch_pad_rocket}), both hardness 5, resistance
 * 10, on the missile tab, drawn by their block entity renderers ({@code client/render/rocket}), OBJ items
 * ({@link RocketItemDeclarations}).
 */
public final class RocketBlocks {

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

    public static final RegistryObject<MachineRocketAssembly> MACHINE_ROCKET_ASSEMBLY = REG.<MachineRocketAssembly>block(
                    "machine_rocket_assembly", MachineRocketAssembly::new,
                    p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<LaunchPadRocket> LAUNCH_PAD_ROCKET = REG.<LaunchPadRocket>block(
                    "launch_pad_rocket", LaunchPadRocket::new,
                    p -> dummyable(p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> MACHINE_ROCKET_ASSEMBLY_ITEM = REG.blockItem(MACHINE_ROCKET_ASSEMBLY).tab(HbmTab.MISSILES);
    public static final RegistryObject<BlockItem> LAUNCH_PAD_ROCKET_ITEM = REG.blockItem(LAUNCH_PAD_ROCKET).tab(HbmTab.MISSILES);

    public static final RegistryObject<BlockEntityType<TileEntityMachineRocketAssembly>> ROCKET_ASSEMBLY_BE = REG.blockEntity(
            "machine_rocket_assembly", () -> BlockEntities.serverTicking(TileEntityMachineRocketAssembly::new,
                    TileEntityMachineRocketAssembly::serverTick, MACHINE_ROCKET_ASSEMBLY.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLaunchPadRocket>> LAUNCH_PAD_ROCKET_BE = REG.blockEntity(
            "launch_pad_rocket", () -> BlockEntities.ticking(TileEntityLaunchPadRocket::new, TileEntityLaunchPadRocket::clientTick,
                    TileEntityLaunchPadRocket::serverTick, LAUNCH_PAD_ROCKET.get()));

    public static final RegistryObject<MenuType<ContainerMachineRocketAssembly>> ROCKET_ASSEMBLY_MENU =
            REG.menu("machine_rocket_assembly", () -> Menus.typeWithPos(ContainerMachineRocketAssembly::new));
    public static final RegistryObject<MenuType<ContainerLaunchPadRocket>> LAUNCH_PAD_ROCKET_MENU =
            REG.menu("launch_pad_rocket", () -> Menus.typeWithPos(ContainerLaunchPadRocket::new));

    private RocketBlocks() {}

    public static void init() {}
}
