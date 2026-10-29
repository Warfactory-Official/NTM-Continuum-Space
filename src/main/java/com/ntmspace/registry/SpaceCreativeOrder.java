package com.ntmspace.registry;

import com.ntmspace.item.ItemMineralClump;
import com.ntmspace.bedrock.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import java.util.Locale;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.tool.ItemDrone;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.block.BlockBodyOre.Body;
import dev.golem.core.Ids;
import dev.golem.core.registry.Tabs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/** Space's additions to the 1.7 registration-order creative lists. */
public final class SpaceCreativeOrder {
    private SpaceCreativeOrder() {}
    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        // 1.7 tabCombat order: big sword, butter sword, schrabidium sword, consecutive in vanilla combat.
        // One listener keeps the cross-mod order exact; handles resolve when the tab builds.
        Tabs.modifyEntries(CreativeModeTabs.COMBAT, entries -> {
            entries.accept(new ItemStack(ModMeleeKitItems.big_sword.get()));
            entries.accept(new ItemStack(SpaceSpecialItems.BUTTER_SWORD.get()));
            entries.accept(new ItemStack(ModAbilityToolItems.schrabidium_sword.get()));
        });
        // 1.7 vanilla spawn-egg tab: Space's eggs ride before their HBM anchors (upstream VanillaCreativeSpace).
        insertEggs("hbm:entity_mob_nuclear_creeper_spawn_egg", "ntmspace:entity_meme_ufo_spawn_egg",
                "ntmspace:entity_war_behemoth_spawn_egg", "ntmspace:entity_tankbot_spawn_egg",
                "ntmspace:entity_bfa_spawn_egg");
        // 1.7 decorations tab: the crop blocks ride vanilla's Natural tab in 1.7 order.
        Tabs.modifyEntries(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Ids.of("minecraft", "natural_blocks")),
                entries -> {
                    for (var crop : List.of(FloraBlocks.CROP_STRAWBERRY, FloraBlocks.CROP_MINT, FloraBlocks.CROP_PARAFFIN,
                            FloraBlocks.CROP_COFFEE, FloraBlocks.CROP_TEA)) entries.accept(new ItemStack(crop.get().asItem()));
                });
        insertEggs("hbm:entity_mob_hunter_chopper_spawn_egg", "ntmspace:entity_mob_flesh_creeper_spawn_egg");
        insertEggs("hbm:entity_fbi_spawn_egg", "ntmspace:entity_moon_cow_spawn_egg",
                "ntmspace:entity_scutterfish_spawn_egg", "ntmspace:entity_scuttlecrab_spawn_egg",
                "ntmspace:entity_depthsquid_spawn_egg", "ntmspace:entity_scrapfish_spawn_egg",
                "ntmspace:entity_siftereel_spawn_egg", "ntmspace:entity_micro_ufo_spawn_egg");
        // ModBlocks:3750-3756 and ModItems:6138-6139.
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_deuterium_tower", "ntmspace:machine_atmo_tower", "ntmspace:machine_atmo_vent");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_intake", "ntmspace:machine_atmo_emitter");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:plastic_bag", "ntmspace:atmosphere_scanner");
        // Armor modules and mineral forms retain their declared registration neighbours.
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:medal_liquidator", "ntmspace:v1");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:armor_battery_mk3", "ntmspace:flippers", "ntmspace:heavy_boots");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:coke_petroleum", "ntmspace:powder_coke_coal",
                "ntmspace:powder_coke_lignite", "ntmspace:powder_coke_petroleum");
        var clumps = new ArrayList<String>();
        for (var kind : ItemMineralClump.Clump.values())
            clumps.add("ntmspace:mineral_fragment_" + kind.name().toLowerCase(Locale.ROOT));
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:mineral_dust", clumps.toArray(String[]::new));

        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:chlorine_pinwheel", "ntmspace:oxy_pinwheel");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_capacitor_copper", "ntmspace:capacitor_complex");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_soldering_station", "ntmspace:machine_vacuum_circuit");
        // ModBlocks:3556, 3748, 3789-3793, 4016-4028: machines stay beside their authored predecessors.
        // IndustryBlocksSpace's progression machines retain their relative registration order.
        ModTabs.insertAfter(HbmTab.MACHINES, "ntmspace:machine_drive_processor", "ntmspace:machine_alkylation");
        ModTabs.insertAfter(HbmTab.MACHINES, "ntmspace:machine_milk_reformer", "ntmspace:machine_cryo_distill");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_combustion_engine", "ntmspace:dish_control");
        ModTabs.insertAfter(HbmTab.MACHINES, "ntmspace:machine_xenon_thruster", "ntmspace:machine_magma");
        ModTabs.insertAfter(HbmTab.MACHINES, "ntmspace:algae_film", "ntmspace:hydrobay");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_catalytic_reformer", "ntmspace:machine_milk_reformer");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_hephaestus", "ntmspace:machine_solar");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_condenser_powered", "ntmspace:machine_radiator");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:machine_lpw2", "ntmspace:machine_htr3", "ntmspace:machine_htrf4",
                "ntmspace:machine_htrf4neo", "ntmspace:machine_xenon_thruster");
        // The first machine-tab item follows the station, Dyson and orrery block registrations.
        ModTabs.addStacksTo(HbmTab.MACHINES, () -> List.of(new ItemStack(ModEntityItems.drone.get(ItemDrone.EnumDroneType.PATROL))));
        ModTabs.insertBefore(HbmTab.MACHINES, "hbm:drone_patrol", "ntmspace:atmosphere_editor", "ntmspace:transporter_rocket",
                "ntmspace:orbital_station_port", "ntmspace:orbital_station_launcher", "ntmspace:orbital_station_computer",
                "ntmspace:propulsion_creative", "ntmspace:dyson_launcher", "ntmspace:dyson_receiver",
                "ntmspace:dyson_converter_tu", "ntmspace:dyson_converter_he", "ntmspace:dyson_converter_anatmogenesis",
                "ntmspace:orrery");
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:radar_screen", "ntmspace:gas_dock");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:inf_water_mk2", "ntmspace:transporter_linker");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:block_niter", "ntmspace:block_nickel");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:block_steel", "ntmspace:block_osmiridium");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:fallout", "ntmspace:salted_fallout");
        ModTabs.insertAfter(HbmTab.BOMBS, "hbm:det_nuke", "ntmspace:det_salt");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:door_bunker", "ntmspace:fence_gate");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:pellet_rtg_americium", "ntmspace:pellet_rtg_berkelium");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:pellet_rtg_lead", "ntmspace:pellet_rtg_cf251",
                "ntmspace:pellet_rtg_cf252");
        ModTabs.insertBefore(HbmTab.CONTROL, "hbm:rbmk_fuel_ueu", "hbm:laser_crystal_co2",
                "hbm:laser_crystal_bismuth", "hbm:laser_crystal_cmb", "hbm:laser_crystal_bale",
                "hbm:laser_crystal_digamma");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:laser_crystal_cmb", "ntmspace:laser_crystal_iron");

        // ModItems:6202-6343: the rocket parts extend HBM's missile families in registration order.
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:mp_thruster_10_solid", "ntmspace:mp_thruster_10_hydrazine");
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:mp_thruster_20_kerosene_triple", "ntmspace:mp_thruster_20_methalox",
                "ntmspace:mp_thruster_20_methalox_dual", "ntmspace:mp_thruster_20_methalox_triple",
                "ntmspace:mp_thruster_20_hydrogen", "ntmspace:mp_thruster_20_hydrogen_dual", "ntmspace:mp_thruster_20_hydrogen_triple");
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:mp_thruster_20_solid_multier", "ntmspace:mp_thruster_20_hydrazine");
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:mp_fuselage_10_solid", "ntmspace:mp_fuselage_10_hydrazine");
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:mp_c_5", "ntmspace:rp_f_20_12", "ntmspace:rp_f_20_6",
                "ntmspace:rp_f_20_3", "ntmspace:rp_f_20_1", "ntmspace:mp_fuselage_20_hydrazine",
                "ntmspace:rp_l_20", "ntmspace:rp_c_20", "ntmspace:rp_sc_20", "ntmspace:rp_pod_20");
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:soyuz_capsule", "ntmspace:combat_drop");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:part_grip_pvc", "ntmspace:hard_drive");
        var drives = new ArrayList<String>();
        for (var body : DriveBody.values()) drives.add("ntmspace:hard_drive_full_" + body.key());
        drives.add("hbm:pellet_rtg_radium");
        ModTabs.insertFirst(HbmTab.CONTROL, drives.toArray(String[]::new));

        // EnumPlushie/EnumBobble insertions and the corresponding getSubBlocks order.
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:plushie_hundun", "ntmspace:plushie_teto", "ntmspace:plushie_miku", "ntmspace:plushie_neru");
        ModTabs.insertBefore(HbmTab.BLOCKS, "hbm:bobblehead_microwave", "hbm:bobblehead_peep");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:bobblehead_cirno", "ntmspace:bobblehead_gwen", "ntmspace:bobblehead_juice", "ntmspace:bobblehead_jamesh_2");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:bobblehead_mellow", "ntmspace:bobblehead_mrkimkimora");

        // ModBlocks:3244-3300: wild flora and planetary terrain extend the existing block runs.
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:plant_flower_cd1", "ntmspace:plant_flower_strawberry", "ntmspace:plant_flower_mint");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:plant_tall_cd4", "ntmspace:plant_tall_laythe", "ntmspace:laythe_kelp",
                "ntmspace:laythe_seagrass", "ntmspace:laythe_glowgrass", "ntmspace:laythe_coral", "ntmspace:laythe_coral_brain",
                "ntmspace:laythe_coral_bubble", "ntmspace:laythe_coral_fire", "ntmspace:laythe_coral_horn",
                "ntmspace:laythe_coral_block", "ntmspace:laythe_coral_block_brain", "ntmspace:laythe_coral_block_bubble",
                "ntmspace:laythe_coral_block_fire", "ntmspace:laythe_coral_block_horn");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:plant_dead_fern", "ntmspace:rubber_plant", "ntmspace:rubber_plant_fern", "ntmspace:rubber_plant_shrub");
        // 1.7 block tab: reeds follow the rubber run; the first dead plant follows the coral blocks.
        ModTabs.insertAfter(HbmTab.BLOCKS, "ntmspace:rubber_plant_shrub", "hbm:plant_reeds");
        ModTabs.insertAfter(HbmTab.BLOCKS, "ntmspace:laythe_coral_block_horn", "hbm:plant_dead_generic");
        // ModBlocks:2745-2756: body ore displays precede or extend their existing host entries.
        ModTabs.remove(HbmTab.RESOURCES, "hbm:ore_uranium");
        ModTabs.remove(HbmTab.RESOURCES, "hbm:ore_uranium_scorched");
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> List.of(stack("hbm:ore_uranium"), stack("hbm:ore_uranium_scorched")));
        ModTabs.insertBefore(HbmTab.BLOCKS, "hbm:waste_earth", "hbm:ore_uranium", "hbm:ore_uranium_scorched");
        var earlyOres = new ArrayList<String>();
        for (var path : List.of("ore_iron", "ore_gold", "ore_redstone", "ore_lapis", "ore_emerald", "ore_quartz", "ore_diamond"))
            earlyOres.addAll(bodyFamily(path));
        ModTabs.insertBefore(HbmTab.BLOCKS, "hbm:ore_uranium", earlyOres.toArray(String[]::new));
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:ore_uranium", bodyFamily("ore_uranium").toArray(String[]::new));
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:ore_uranium_scorched", bodyFamily("ore_plutonium").toArray(String[]::new));
        // 1.7 tabBlocks: the sellafite set runs slaked, diamond ore, bedrock, emerald ore, after the astroturf.
        ModTabs.insertAfter(HbmTab.BLOCKS, "ntmspace:astroturf", "hbm:sellafield_slaked", "hbm:ore_sellafield_diamond",
                "hbm:sellafield_bedrock", "hbm:ore_sellafield_emerald");
        // Space uses 1.7's block tab for moon turf; the base mod's resource split remains its own declaration.
        ModTabs.remove(HbmTab.RESOURCES, "hbm:moon_turf");
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> List.of(stack("hbm:moon_turf")));
        ModTabs.insertBefore(HbmTab.BLOCKS, "hbm:waste_earth", "hbm:moon_turf");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:moon_turf", "ntmspace:moon_rock", "ntmspace:sapling_vinyl", "ntmspace:sapling_pvc",
                "ntmspace:duna_sands", "ntmspace:duna_cobble", "ntmspace:duna_rock", "ntmspace:dry_ice", "ntmspace:ferric_clay",
                "ntmspace:eve_silt", "ntmspace:eve_rock", "ntmspace:laythe_silt", "ntmspace:laythe_root", "ntmspace:ike_regolith",
                "ntmspace:ike_stone", "ntmspace:dres_rock", "ntmspace:moho_regolith", "ntmspace:moho_stone",
                "ntmspace:minmus_regolith", "ntmspace:minmus_stone", "ntmspace:minmus_smooth");
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:impact_dirt", "ntmspace:tumor", "ntmspace:primed_log", "ntmspace:eu_log",
                "ntmspace:bf_log", "ntmspace:lattice_log", "ntmspace:pvc_log", "ntmspace:rubber_leaves", "ntmspace:rubber_grass",
                "ntmspace:rubber_silt", "ntmspace:rubber_tall", "ntmspace:rubber_tall_bush", "ntmspace:rubber_tall_flower",
                "ntmspace:rubber_farmland", "ntmspace:vinyl_sand", "ntmspace:vinyl_vine", "ntmspace:vinyl_log",
                "ntmspace:pet_leaves", "ntmspace:astroturf");

        // ModItems:5752, 6649-6653, 7177: charge variants and the remaining item anchors.
        ModTabs.displayStacks(HbmTab.CONTROL, SpaceMobs.CORE_ANGEL, () -> SpaceMobs.CORE_ANGEL.get().creativeStacks());
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:cube_power", "ntmspace:core_angel");
        // 1.7 science variants before Gerald: sensor, gerald, chip, dyson relay, coord.
        ModTabs.insertAfter(HbmTab.MISSILES, "hbm:satellite_science_sensor", "hbm:sat_gerald", "hbm:sat_chip",
                "ntmspace:sat_dyson_relay", "hbm:sat_coord");
        // 1.7 ModBlocks:3338/3398: the antimatter bomb precedes fstbmb, the oil barrel follows vitrified.
        ModTabs.insertBefore(HbmTab.BOMBS, "hbm:nuke_fstbmb", "ntmspace:nuke_antimatter");
        ModTabs.insertAfter(HbmTab.BOMBS, "hbm:vitrified_barrel", "ntmspace:oil_barrel");
        // 1.7 ModItems:6109: the angel spawner follows the duck in consumables (the duck lists on weapons).
        ModTabs.addStacksTo(HbmTab.CONSUMABLES, () -> List.of(stack("hbm:spawn_duck")));
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:spawn_duck", "ntmspace:spawn_angel");
        // 1.7 ModBlocks registers gas, vent and scrubber before algae.
        ModTabs.insertBefore(HbmTab.MACHINES, "ntmspace:algae_film", "hbm:gas_explosive");
        ModTabs.insertAfter(HbmTab.MACHINES, "hbm:gas_explosive", "ntmspace:air_vent", "ntmspace:air_scrubber");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:crystal_charred", "ntmspace:swarm_member");
        // Zinc's wire and iridium's plates take their 1.7 slots among HBM's autogen shapes.
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:wire_fine_zirconium", "ntmspace:wire_fine_zinc");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:plate_cast_osmiridium", "ntmspace:plate_cast_iridium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:plate_welded_osmiridium", "ntmspace:plate_welded_iridium");
        // 1.7 Mats file order: Space's bedrock fragments (and the base fragments Space gives the shape late)
        // slot after their 1.7 predecessor; HBM's own fragments already stand in 1.7 order.
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_redstone", "ntmspace:bedrock_ore_fragment_glowstone");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_uranium", "ntmspace:bedrock_ore_fragment_u233");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_u233", "ntmspace:bedrock_ore_fragment_u235");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_thorium", "ntmspace:bedrock_ore_fragment_plutonium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_plutonium", "ntmspace:bedrock_ore_fragment_pu239");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_pu239", "ntmspace:bedrock_ore_fragment_neptunium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_ra226", "ntmspace:bedrock_ore_fragment_co60");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_co60", "ntmspace:bedrock_ore_fragment_au198");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_au198", "ntmspace:bedrock_ore_fragment_pb209");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_pb209", "ntmspace:bedrock_ore_fragment_schrabidium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_schrabidium", "ntmspace:bedrock_ore_fragment_solinium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_solinium", "ntmspace:bedrock_ore_fragment_gh336");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_gh336", "ntmspace:bedrock_ore_fragment_australium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_australium", "ntmspace:bedrock_ore_fragment_tasmanite");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_tasmanite", "ntmspace:bedrock_ore_fragment_ayerite");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_bismuth", "ntmspace:bedrock_ore_fragment_arsenic");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_cobalt", "ntmspace:bedrock_ore_fragment_lapis");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_red_phosphorus", "ntmspace:bedrock_ore_fragment_white_phosphorus");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_cinnabar", "ntmspace:bedrock_ore_fragment_cadmium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_asbestos", "ntmspace:bedrock_ore_fragment_quartz");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_quartz", "ntmspace:bedrock_ore_fragment_nickel_pure");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_nickel_pure", "ntmspace:bedrock_ore_fragment_gallium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_gallium", "ntmspace:bedrock_ore_fragment_zinc");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_zinc", "ntmspace:bedrock_ore_fragment_bromine");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_bromine", "ntmspace:bedrock_ore_fragment_iodine");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_iodine", "ntmspace:bedrock_ore_fragment_hafnium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_hafnium", "ntmspace:bedrock_ore_fragment_semtex");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_semtex", "hbm:bedrock_ore_fragment_rare");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_rare", "ntmspace:bedrock_ore_fragment_polymer");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_polymer", "ntmspace:bedrock_ore_fragment_rubber");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_rubber", "ntmspace:bedrock_ore_fragment_pvc");
        // 1.7 ModItems registration order: every named Space part rides its 1.7 predecessor, and the
        // predecessors chain in 1.7 order (upstream PartsItemsSpace static block, hbmspace: -> ntmspace:).
        // Each rule carries its cluster plus the next link, so the whole run lands as one 1.7-ordered block.
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_am_mix", "ntmspace:ingot_cm242", "ntmspace:ingot_cm243",
                "ntmspace:ingot_cm244", "ntmspace:ingot_cm245", "ntmspace:ingot_cm246", "ntmspace:ingot_cm247",
                "ntmspace:ingot_cm_fuel", "ntmspace:ingot_cm_mix", "ntmspace:ingot_bk247", "ntmspace:ingot_cf251",
                "ntmspace:ingot_cf252", "ntmspace:ingot_es253", "ntmspace:ingot_es255", "hbm:ingot_titanium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_titanium", "ntmspace:ingot_nickel", "hbm:ingot_red_copper");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_red_copper", "ntmspace:billet_red_copper", "hbm:ingot_tungsten");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_tungsten", "ntmspace:ingot_gallium", "ntmspace:ingot_gaas",
                "ntmspace:ingot_tt", "ntmspace:ingot_ttas", "ntmspace:ingot_iridium", "ntmspace:ingot_palladium",
                "hbm:ingot_steel");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_steel", "ntmspace:ingot_stainless", "hbm:ingot_bscco");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_bscco", "ntmspace:ingot_menthol", "hbm:ingot_mud");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_mud", "ntmspace:ingot_magma", "hbm:ingot_gh336");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_gh336", "ntmspace:ingot_cn989", "hbm:ingot_australium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_australium", "ntmspace:ingot_australium_lesser",
                "ntmspace:ingot_australium_greater", "hbm:ingot_electronium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_electronium", "ntmspace:ingot_gwenium", "hbm:ingot_osmiridium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ingot_osmiridium", "ntmspace:ingot_hafnium", "hbm:billet_am_mix");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:billet_am_mix", "ntmspace:billet_bk247", "ntmspace:billet_cm242",
                "ntmspace:billet_cm243", "ntmspace:billet_cm244", "ntmspace:billet_cm245", "ntmspace:billet_cm246",
                "ntmspace:billet_cm247", "ntmspace:billet_cm_fuel", "ntmspace:billet_cm_mix", "ntmspace:billet_cf251",
                "ntmspace:billet_cf252", "ntmspace:billet_es253", "ntmspace:billet_cn989", "hbm:billet_gh336");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:billet_gh336", "ntmspace:billet_gaas", "hbm:billet_nuclear_waste");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:billet_nuclear_waste", "ntmspace:billet_menthol",
                "hbm:briquette_wood");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:briquette_wood", "ntmspace:woodemium_briquette",
                "hbm:powder_cs137_tiny");
        // 1.7 tabParts: sulfur follows the woodemium briquette.
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:woodemium_briquette", "hbm:sulfur");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:powder_cs137_tiny", "ntmspace:powder_cn989", "hbm:powder_titanium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:powder_titanium", "ntmspace:powder_nickel", "hbm:powder_copper");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:powder_copper", "ntmspace:powder_zinc",
                "ntmspace:powder_gallium_tiny", "ntmspace:powder_gallium", "hbm:powder_euphemium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:powder_euphemium", "ntmspace:powder_wd2004_tiny",
                "ntmspace:powder_wd2004", "hbm:powder_cement");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:powder_cement", "ntmspace:nickel_salts", "hbm:cordite");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:cordite", "ntmspace:ammonium_nitrate", "hbm:ball_fireclay");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:ball_fireclay", "ntmspace:ball_ferric_clay");
        var bedrock = new ArrayList<String>();
        bedrock.add("hbm:bedrock_ore_base");
        for (var type : CelestialBedrockOre.SPECS)
            bedrock.add("ntmspace:" + BedrockOreItems.id(Grade.BASE, type));
        bedrock.addAll(List.of("ntmspace:crystal_mineral", "ntmspace:crystal_cleaned", "ntmspace:mineral_dust",
                "hbm:gem_alexandrite"));
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:bedrock_ore_fragment_pvc", bedrock.toArray(String[]::new));
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:gem_alexandrite", "ntmspace:crystal_nickel",
                "ntmspace:crystal_niobium", "ntmspace:crystal_zinc", "hbm:bio_wafer");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:bio_wafer", "ntmspace:flesh", "ntmspace:flesh_wafer",
                "ntmspace:grilled_flesh", "ntmspace:flesh_burger", "ntmspace:stick_pvc", "ntmspace:stick_vinyl",
                "hbm:nugget_am_mix");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:nugget_am_mix", "ntmspace:nugget_bk247", "ntmspace:nugget_cm_mix",
                "ntmspace:nugget_cm242", "ntmspace:nugget_cm243", "ntmspace:nugget_cm244", "ntmspace:nugget_cm245",
                "ntmspace:nugget_cm246", "ntmspace:nugget_cm247", "ntmspace:nugget_cm_fuel", "ntmspace:nugget_cf251",
                "ntmspace:nugget_cf252", "ntmspace:nugget_es253", "ntmspace:nugget_cn989", "hbm:nugget_lead");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:nugget_lead", "ntmspace:nugget_menthol", "hbm:nugget_zirconium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:nugget_zirconium", "ntmspace:nugget_nickel", "ntmspace:nugget_gaas",
                "ntmspace:nugget_zinc", "ntmspace:nugget_gallium", "hbm:nugget_osmiridium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:nugget_osmiridium", "ntmspace:nugget_hafnium",
                "ntmspace:nugget_lanthanium", "hbm:plate_titanium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:plate_titanium", "ntmspace:plate_nickel", "hbm:plate_steel");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:plate_steel", "ntmspace:plate_stainless", "hbm:plate_dineutronium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:plate_dineutronium", "ntmspace:plate_cn989", "hbm:rag_piss");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:rag_piss", "ntmspace:rag_blood", "hbm:photo_panel");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:photo_panel", "ntmspace:beryllium_mirror", "hbm:blade_titanium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:blade_titanium", "ntmspace:blade_syngas", "hbm:turbine_tungsten");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:turbine_tungsten", "ntmspace:turbine_syngas");
        // Post-PIN HBM material (absent from Space 1.7's Mats order): functional for the ilmenite chain
        // but hidden from the parts tab so the autogen families read in PIN order.
        ModTabs.remove(HbmTab.MATERIALS, "hbm:bedrock_ore_fragment_ilmenite");
        // 1.7 ItemCircuit.getSubItems / EnumChunkType / EnumExpensiveType: Space's entries slot right after
        // the 1.7 entry listed before them (HBM's and Space's circuits share the one parts-tab list in 1.7).
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:circuit_bismoid", "ntmspace:circuit_avionics");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_avionics", "ntmspace:circuit_aero");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_aero", "ntmspace:circuit_gaas");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:circuit_controller_quantum",
                "ntmspace:circuit_capacitor_lanthanium");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_capacitor_lanthanium", "ntmspace:circuit_processt1");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_processt1", "ntmspace:circuit_processt2");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_processt2", "ntmspace:circuit_processt3");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_processt3", "ntmspace:circuit_gaschip");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_gaschip", "ntmspace:circuit_hfchip");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:circuit_hfchip", "ntmspace:circuit_molychip");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:chunk_ore_cryolite", "ntmspace:chunk_ore_pentlandite");
        ModTabs.insertAfter(HbmTab.MATERIALS, "hbm:item_expensive_degenerate_matter",
                "ntmspace:item_expensive_stainless_beam");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:item_expensive_stainless_beam",
                "ntmspace:item_expensive_avionics");
        ModTabs.insertAfter(HbmTab.MATERIALS, "ntmspace:item_expensive_avionics", "ntmspace:item_expensive_astro");
        // 1.7 FuelItemsSpace:96-99: Space's pellets extend each of NTM's two watz blocks in enum order.
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:watz_pellet_nqr", "ntmspace:watz_pellet_pu241",
                "ntmspace:watz_pellet_amrg", "ntmspace:watz_pellet_amf", "ntmspace:watz_pellet_cmrg",
                "ntmspace:watz_pellet_cmf", "ntmspace:watz_pellet_bk247", "ntmspace:watz_pellet_cf251",
                "ntmspace:watz_pellet_cf252", "ntmspace:watz_pellet_es253");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:watz_pellet_depleted_nqr", "ntmspace:watz_pellet_depleted_pu241",
                "ntmspace:watz_pellet_depleted_amrg", "ntmspace:watz_pellet_depleted_amf",
                "ntmspace:watz_pellet_depleted_cmrg", "ntmspace:watz_pellet_depleted_cmf",
                "ntmspace:watz_pellet_depleted_bk247", "ntmspace:watz_pellet_depleted_cf251",
                "ntmspace:watz_pellet_depleted_cf252", "ntmspace:watz_pellet_depleted_es253");
        // 1.7 FuelItemsSpace:91-94: the bk247 rod after hea242, the curium rods after the test rod;
        // a pellet lists every stage, so the bk247 stages go before the item 1.7 registered after them.
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:rbmk_fuel_hea242", "ntmspace:rbmk_fuel_bk247");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:rbmk_fuel_test", "ntmspace:rbmk_fuel_lecm",
                "ntmspace:rbmk_fuel_mecm", "ntmspace:rbmk_fuel_hecm");
        ModTabs.displayStacks(HbmTab.CONTROL, SpaceSpecialItems.RBMK_PELLET_BK247, () -> {
            var stages = new ArrayList<ItemStack>();
            for (int meta = 0; meta < 10; meta++) stages.add(SpaceSpecialItems.RBMK_PELLET_BK247.get().of(meta));
            return stages;
        });
        ModTabs.insertBefore(HbmTab.CONTROL, "hbm:rbmk_pellet_men", "ntmspace:rbmk_pellet_bk247");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:pancake", "ntmspace:butter");
        // 1.7 registration order has the gun kits before the pancake row; HBM registers those classes flipped.
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:mucho_mango", "hbm:gun_kit_1", "hbm:gun_kit_2");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "ntmspace:butter", "ntmspace:s_cream", "ntmspace:min_cream");
        // 1.7 registration order has chocolate before the bdcl row; HBM registers those classes flipped.
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:nugget", "hbm:chocolate");
        // 1.7 ModItems:6649-6653: HBM's nugget closes the pancake row.
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "ntmspace:min_cream", "hbm:nugget");
        // 1.7 arcwelder.insert_cmb order: Space's module slots between ghiorsium and HBM's era.
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:insert_ghiorsium", "ntmspace:insert_cmb");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "ntmspace:insert_cmb", "hbm:insert_era");
        ModTabs.insertBefore(HbmTab.CONSUMABLES, "hbm:bobmazon", "ntmspace:chocolate_mint_billet");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:gun_kit_2", "ntmspace:animan");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:chocolate", "ntmspace:flour");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:coin_ufo", "ntmspace:coin_airliner");
        // 1.7 registration order has the coins after flour; HBM registers those classes flipped.
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "ntmspace:flour", "hbm:coin_ufo");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:flask_infusion_shield", "ntmspace:flask_infusion_nitan");
        // The smilk glass is Space's own item: join the tab before taking its anchored place.
        ModTabs.addTo(HbmTab.CONSUMABLES, SpaceItems.GLASS_SMILK);
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:bdcl", "ntmspace:glass_smilk", "ntmspace:glass_empty",
                "ntmspace:powder_coffee", "ntmspace:bean_roast", "ntmspace:cmug_empty", "ntmspace:teacup_empty",
                "ntmspace:teacup", "ntmspace:bottle_honey");
        ModTabs.insertAfter(HbmTab.CONTROL, "hbm:pellet_rtg_depleted_zirconium", "ntmspace:pellet_rtg_depleted_americium");
        ModTabs.insertAfter(HbmTab.TEMPLATES, "hbm:siren_track_razortrain",
                "ntmspace:siren_track_disembodied", "ntmspace:siren_track_suicide");
        // hbm-space WasteItemsSpace: each item lists every meta in the parts tab, HBM's eight classes then
        // Space's four in 1.7 metadata order; HBM's waste otherwise lives only in the control tab.
        for (String family : new String[]{"nuclear_waste_short", "nuclear_waste_short_tiny",
                "nuclear_waste_short_depleted", "nuclear_waste_short_depleted_tiny"}) {
            String batch = family;
            ModTabs.addStacksTo(HbmTab.MATERIALS, () -> List.of(stack("hbm:" + batch + "_uranium235"),
                    stack("hbm:" + batch + "_uranium233"), stack("hbm:" + batch + "_neptunium"),
                    stack("hbm:" + batch + "_plutonium239"), stack("hbm:" + batch + "_plutonium240"),
                    stack("hbm:" + batch + "_plutonium241"), stack("hbm:" + batch + "_americium242"),
                    stack("ntmspace:" + batch + "_americium241"), stack("hbm:" + batch + "_schrabidium"),
                    stack("ntmspace:" + batch + "_berkelium247"), stack("ntmspace:" + batch + "_curium244"),
                    stack("ntmspace:" + batch + "_curium245")));
        }
        ModTabs.insertBefore(HbmTab.CONSUMABLES, "hbm:structure_custommachine", "ntmspace:wand_time");
        ModTabs.insertAfter(HbmTab.CONSUMABLES, "hbm:bomb_caller_abomb", "ntmspace:bomb_caller_airliner");
        ModTabs.remove(HbmTab.CONSUMABLES, "hbm:bottled_cloud");
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow());
    }

    /** Vanilla-tab eggs before their HBM anchor; stacks resolve when the tab builds. */
    private static void insertEggs(String anchor, String... eggs) {
        Tabs.insertBefore(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Ids.of("minecraft", "spawn_eggs")),
                () -> stack(anchor),
                () -> Arrays.stream(eggs).map(SpaceCreativeOrder::stack).toList());
    }

    private static List<String> bodyFamily(String path) {
        var items = new ArrayList<String>();
        for (var body : Body.values())
            items.add("ntmspace:" + path + (body == Body.KERBIN ? "" : "_" + body.getSerializedName()));
        return items;
    }
}
