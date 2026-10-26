package com.ntmspace.registry;

import com.hbm.registration.CarriedDrops;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.machine.MachineAlkylation;
import com.ntmspace.block.machine.MachineCryoDistill;
import com.ntmspace.block.machine.MachineMilkReformer;
import com.ntmspace.inventory.machine.ContainerMachineCryoDistill;
import com.ntmspace.inventory.machine.ContainerMachineMilkReformer;
import com.ntmspace.recipe.AlkylationRecipes;
import com.ntmspace.recipe.CryoRecipes;
import com.ntmspace.tileentity.machine.TileEntityMachineAlkylation;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
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
 * hbm-space's progression machines (its {@code ModBlocks} values), part one: the cryogenic distiller, the alkylation
 * unit and the milk reformer (hardness 5, resistance 10, machine tab). The multiblocks are drawn by their renderers
 * ({@code client/render/machine}); their items are OBJ items ({@link MachineItemDeclarations}). The other machines of
 * the family are in {@link SpaceMachinesB} and {@link SpaceMachinesC}.
 */
public final class SpaceMachines {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);

    /** A renderer-drawn multiblock: facing x cell, the shared empty placeholder model. */
    public static final Presets.BlockModel FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    /** The properties every folded machine shares (cells must not suffocate or block the view). */
    public static BlockBehaviour.Properties machine(BlockBehaviour.Properties p, float hardness, float resistance) {
        return p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false).noOcclusion();
    }

    public static final RegistryObject<MachineCryoDistill> MACHINE_CRYO_DISTILL = REG.<MachineCryoDistill>block(
                    "machine_cryo_distill", MachineCryoDistill::new, p -> machine(p, 5.0F, 10.0F))
            .transform(FOLDED).loot(CarriedDrops.ALWAYS).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineAlkylation> MACHINE_ALKYLATION = REG.<MachineAlkylation>block(
                    "machine_alkylation", MachineAlkylation::new, p -> machine(p, 5.0F, 10.0F))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineMilkReformer> MACHINE_MILK_REFORMER = REG.<MachineMilkReformer>block(
                    "machine_milk_reformer", MachineMilkReformer::new, p -> machine(p, 5.0F, 10.0F))
            .transform(FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> MACHINE_CRYO_DISTILL_ITEM = REG.blockItem(MACHINE_CRYO_DISTILL).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_ALKYLATION_ITEM = REG.blockItem(MACHINE_ALKYLATION).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_MILK_REFORMER_ITEM = REG.blockItem(MACHINE_MILK_REFORMER).tab(HbmTab.MACHINES);

    public static final RegistryObject<BlockEntityType<TileEntityMachineCryoDistill>> CRYO_DISTILL_BE = REG.blockEntity(
            "machine_cryo_distill", () -> BlockEntities.ticking(TileEntityMachineCryoDistill::new,
                    TileEntityMachineCryoDistill::clientTick, TileEntityMachineCryoDistill::serverTick, MACHINE_CRYO_DISTILL.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineAlkylation>> ALKYLATION_BE = REG.blockEntity(
            "machine_alkylation", () -> BlockEntities.ticking(TileEntityMachineAlkylation::new,
                    TileEntityMachineAlkylation::clientTick, TileEntityMachineAlkylation::serverTick, MACHINE_ALKYLATION.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineMilkReformer>> MILK_REFORMER_BE = REG.blockEntity(
            "machine_milk_reformer", () -> BlockEntities.ticking(TileEntityMachineMilkReformer::new,
                    TileEntityMachineMilkReformer::clientTick, TileEntityMachineMilkReformer::serverTick, MACHINE_MILK_REFORMER.get()));

    public static final RegistryObject<MenuType<ContainerMachineCryoDistill>> CRYO_DISTILL_MENU =
            REG.menu("machine_cryo_distill", () -> Menus.typeWithPos(ContainerMachineCryoDistill::new));
    public static final RegistryObject<MenuType<ContainerMachineMilkReformer>> MILK_REFORMER_MENU =
            REG.menu("machine_milk_reformer", () -> Menus.typeWithPos(ContainerMachineMilkReformer::new));

    private SpaceMachines() {}

    public static void init() {
        CryoRecipes.declare();
        AlkylationRecipes.declare();
    }
}
