package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.atmosphere.AtmoTower;
import com.ntmspace.block.atmosphere.AtmosphereEditorItem;
import com.ntmspace.block.atmosphere.BlockAirScrubber;
import com.ntmspace.block.atmosphere.BlockAtmosphereEditor;
import com.ntmspace.block.atmosphere.BlockAtmosphericCompressor;
import com.ntmspace.block.atmosphere.MachineAtmosphericEmitter;
import com.ntmspace.block.machine.StandardInfoBlockItem;
import com.ntmspace.recipe.AtmosphereRecipes;
import com.ntmspace.tileentity.atmosphere.TileEntityAirScrubber;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmoTower;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphereEditor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericCompressor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's atmosphere machines (its {@code ModBlocks}, machine tab): the Atmospheric Liquefaction Tower
 * ({@code machine_atmo_tower}), the Atmospheric Compressor ({@code machine_atmo_vent}; hardness 10, resistance 20 both),
 * the Atmospheric Emitter ({@code machine_atmo_emitter}, 10/20), the CO2 Scrubber ({@code air_scrubber}, 5/10) and the
 * creative Atmosphere Editor ({@code atmosphere_editor}, hardness 1). None has a GUI in 1.7. Also the PLSS's breathing
 * loop ({@code player.plss_breathing}) and the atmospheric chemistry table ({@link AtmosphereRecipes}).
 * The air vent ({@code air_vent}) is {@link AtmosphereContent}'s.
 */
public final class AtmosphereMachines {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, SOUNDS);

    public static final RegistryObject<AtmoTower> ATMO_TOWER = REG.<AtmoTower>block(
                    "machine_atmo_tower", AtmoTower::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockAtmosphericCompressor> ATMO_VENT = REG.<BlockAtmosphericCompressor>block(
                    "machine_atmo_vent", BlockAtmosphericCompressor::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineAtmosphericEmitter> ATMO_EMITTER = REG.<MachineAtmosphericEmitter>block(
                    "machine_atmo_emitter", MachineAtmosphericEmitter::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockAirScrubber> AIR_SCRUBBER = REG.<BlockAirScrubber>block(
                    "air_scrubber", BlockAirScrubber::new, p -> SpaceMachines.machine(p, 5.0F, 10.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** 1.7 {@code BlockAtmosphereEditor(Material.iron)}, hardness 1 (1.7's resistance floor 5), texture {@code atmosphere_editor}. */
    public static final RegistryObject<BlockAtmosphereEditor> ATMOSPHERE_EDITOR = REG.<BlockAtmosphereEditor>block(
                    "atmosphere_editor", BlockAtmosphereEditor::new,
                    p -> p.strength(1.0F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll("ntmspace:blocks/atmosphere_editor")).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> ATMO_TOWER_ITEM = REG.blockItem(ATMO_TOWER).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> ATMO_VENT_ITEM = REG.blockItem(ATMO_VENT).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> ATMO_EMITTER_ITEM = REG.blockItem(ATMO_EMITTER).tab(HbmTab.MACHINES);
    /** 1.7 {@code BlockAirScrubber} is an {@code ITooltipProvider} ({@code addStandardInfo}). */
    public static final RegistryObject<StandardInfoBlockItem> AIR_SCRUBBER_ITEM = REG.<BlockAirScrubber, StandardInfoBlockItem>blockItem(
            AIR_SCRUBBER, (b, p) -> new StandardInfoBlockItem(b, p, "air_scrubber"), Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<AtmosphereEditorItem> ATMOSPHERE_EDITOR_ITEM = REG.<BlockAtmosphereEditor, AtmosphereEditorItem>blockItem(
            ATMOSPHERE_EDITOR, AtmosphereEditorItem::new, p -> p).tab(HbmTab.MACHINES);

    /** 1.7 {@code TileMappings}: {@code tileentity_atmospheric_tower}; the modern id is the block's. */
    public static final RegistryObject<BlockEntityType<TileEntityAtmoTower>> ATMO_TOWER_BE = REG.blockEntity(
            "machine_atmo_tower", () -> BlockEntities.ticking(TileEntityAtmoTower::new, null,
                    TileEntityAtmoTower::serverTick, ATMO_TOWER.get()));
    /** {@code tileentity_atmospheric_vent}. */
    public static final RegistryObject<BlockEntityType<TileEntityAtmosphericCompressor>> ATMO_VENT_BE = REG.blockEntity(
            "machine_atmo_vent", () -> BlockEntities.ticking(TileEntityAtmosphericCompressor::new,
                    TileEntityAtmosphericCompressor::clientTick, TileEntityAtmosphericCompressor::serverTick, ATMO_VENT.get()));
    /** {@code tileentity_atmo_emitter}. */
    public static final RegistryObject<BlockEntityType<TileEntityAtmosphericEmitter>> ATMO_EMITTER_BE = REG.blockEntity(
            "machine_atmo_emitter", () -> BlockEntities.ticking(TileEntityAtmosphericEmitter::new,
                    TileEntityAtmosphericEmitter::clientTick, TileEntityAtmosphericEmitter::serverTick, ATMO_EMITTER.get()));
    /** {@code tileentity_air_scrubber}. */
    public static final RegistryObject<BlockEntityType<TileEntityAirScrubber>> AIR_SCRUBBER_BE = REG.blockEntity(
            "air_scrubber", () -> BlockEntities.ticking(TileEntityAirScrubber::new,
                    TileEntityAirScrubber::clientTick, TileEntityAirScrubber::serverTick, AIR_SCRUBBER.get()));
    /** {@code tileentity_atmosphere_editor}. */
    public static final RegistryObject<BlockEntityType<TileEntityAtmosphereEditor>> ATMOSPHERE_EDITOR_BE = REG.blockEntity(
            "atmosphere_editor", () -> BlockEntities.ticking(TileEntityAtmosphereEditor::new, null,
                    TileEntityAtmosphereEditor::serverTick, ATMOSPHERE_EDITOR.get()));

    /** hbm-space {@code sounds.json} {@code player.plss_breathing} (file {@code player/plss_breathing}). */
    public static final RegistryObject<SoundEvent> PLSS_BREATHING = REG.sound("player.plss_breathing");

    private AtmosphereMachines() {}

    public static void init() {
        AtmosphereRecipes.declare();
    }
}
