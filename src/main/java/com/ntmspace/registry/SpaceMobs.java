package com.ntmspace.registry;

import com.ntmspace.entity.mob.MobWeaponsSpace;
import dev.golem.core.dejson.BiomeModifiers;
import dev.golem.core.dejson.Tags;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.special.ItemChopper;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.entity.mob.EntityBFAngel;
import com.ntmspace.entity.mob.EntityCreeperFlesh;
import com.ntmspace.entity.mob.EntityDepthSquid;
import com.ntmspace.entity.mob.EntityFish;
import com.ntmspace.entity.mob.EntityMoonCow;
import com.ntmspace.entity.mob.EntityScrapFish;
import com.ntmspace.entity.mob.EntityScutterfish;
import com.ntmspace.entity.mob.EntityScuttlecrab;
import com.ntmspace.entity.mob.EntitySifterEel;
import com.ntmspace.entity.mob.EntityTankbot;
import com.ntmspace.entity.mob.EntityWarBehemoth;
import com.ntmspace.entity.mob.ai.EntityAITankshell;
import com.ntmspace.entity.mob.siege.EntitySiegeUFO;
import dev.golem.core.dejson.Tints;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.EntityTag;
import dev.golem.core.registry.LivingAttributes;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Tabs;
import dev.golem.core.world.SpawnEggs;
import dev.golem.core.world.Spawns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's mobs (History §19fb-mobs): the eleven entity types its {@code EntityMappings} added with {@code addMob}
 * (ids kept, under {@code ntmspace:}), their 1.7 sizes and categories, spawn eggs in the 1.7 colours, the three items
 * the mobs drop or are summoned by, the three sounds hbm-space added for them, and the entity tag
 * {@code ntmspace:suffocation_immune} (1.7 {@code ISuffocationImmune}) for the atmosphere system.
 *
 * <p>1.7 registered every mob through {@code ModEntityList.registerEntity} with tracking range 80 (5 chunks) and
 * update interval 3. Natural spawns are the planet biomes' own spawner lists (authored biome JSON); the placements here
 * are 1.7 {@code SpawnerAnimals}' (land mobs on the ground, water creatures in water over a liquid).
 */
public final class SpaceMobs {

    public static final DeferredRegister.EntityTypes ENTITY_TYPES = DeferredRegister.createEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ENTITY_TYPES, ITEMS, SOUNDS);

    /**
     * 1.7 {@code ISuffocationImmune}: breathes no air. Read by the atmosphere system.
     */
    public static final EntityTag SUFFOCATION_IMMUNE = EntityTag.of(NtmSpace.MOD_ID + ":suffocation_immune");
    private static final EntityTag RADIATION_IMMUNE = EntityTag.of("hbm:radiation_immune");

    // ---------------------------------------------------------------------------------------- sounds

    /**
     * {@code entity.beep} (hbm-space {@code hbm:entity.beep}): the war machines' step beep.
     */
    public static final RegistryObject<SoundEvent> BEEP = REG.sound("entity.beep");
    /**
     * {@code entity.bfatalk}: the angel's voice line while its beam charges.
     */
    public static final RegistryObject<SoundEvent> BFA_TALK = REG.sound("entity.bfatalk");
    /**
     * {@code entity.bfashoot}: the angel's (and the mini UFO's) beam.
     */
    public static final RegistryObject<SoundEvent> BFA_SHOOT = REG.sound("entity.bfashoot");

    // ---------------------------------------------------------------------------------------- entities

    public static final Registrar.EntityHandle<EntityMoonCow> MOON_COW = REG.entity("entity_moon_cow",
                                                                                    key -> Entities.build(
                                                                                            EntityType.Builder.<EntityMoonCow>of(EntityMoonCow::new, MobCategory.CREATURE)
                                                                                                              .sized(0.9F, 1.3F).clientTrackingRange(5).updateInterval(3), key))
                                                                            .tag(SUFFOCATION_IMMUNE)
                                                                            .tag(RADIATION_IMMUNE);
    public static final Registrar.EntityHandle<EntityScutterfish> SCUTTERFISH = REG.entity("entity_scutterfish",
            key -> Entities.build(
                    EntityType.Builder.<EntityScutterfish>of(EntityScutterfish::new, MobCategory.WATER_CREATURE)
                                      .sized(2.0F, 2.0F).clientTrackingRange(5).updateInterval(3), key));
    public static final Registrar.EntityHandle<EntityScuttlecrab> SCUTTLECRAB = REG.entity("entity_scuttlecrab",
            key -> Entities.build(
                    EntityType.Builder.<EntityScuttlecrab>of(EntityScuttlecrab::new, MobCategory.WATER_CREATURE)
                                      .sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final Registrar.EntityHandle<EntityDepthSquid> DEPTH_SQUID = REG.entity("entity_depthsquid",
            key -> Entities.build(
                    EntityType.Builder.<EntityDepthSquid>of(EntityDepthSquid::new, MobCategory.WATER_CREATURE)
                                      .sized(0.95F, 0.95F).clientTrackingRange(5).updateInterval(3), key));
    public static final Registrar.EntityHandle<EntityScrapFish> SCRAPFISH = REG.entity("entity_scrapfish",
            key -> Entities.build(
                    EntityType.Builder.<EntityScrapFish>of(EntityScrapFish::new, MobCategory.WATER_CREATURE)
                                      .sized(2.0F, 2.0F).clientTrackingRange(5).updateInterval(3), key));
    public static final Registrar.EntityHandle<EntitySifterEel> SIFTER_EEL = REG.entity("entity_siftereel",
            key -> Entities.build(
                    Entities.noLoot(
                                    EntityType.Builder.<EntitySifterEel>of(EntitySifterEel::new, MobCategory.WATER_CREATURE))
                            .sized(2.0F, 2.0F).clientTrackingRange(5).updateInterval(3), key));
    public static final Registrar.EntityHandle<EntityCreeperFlesh> CREEPER_FLESH = REG.entity(
            "entity_mob_flesh_creeper", key -> Entities.build(
                    EntityType.Builder.<EntityCreeperFlesh>of(EntityCreeperFlesh::new, MobCategory.MONSTER)
                                      .sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3), key));
    public static final Registrar.EntityHandle<EntityWarBehemoth> WAR_BEHEMOTH = REG.entity("entity_war_behemoth",
                                                                                            key -> Entities.build(
                                                                                                    Entities.noLoot(
                                                                                                                    EntityType.Builder.<EntityWarBehemoth>of(EntityWarBehemoth::new, MobCategory.CREATURE))
                                                                                                            .sized(1.75F, 6.35F).clientTrackingRange(5).updateInterval(3), key))
                                                                                    .tag(SUFFOCATION_IMMUNE);
    public static final Registrar.EntityHandle<EntityTankbot> TANKBOT = REG.entity("entity_tankbot",
                                                                                   key -> Entities.build(
                                                                                           Entities.noLoot(EntityType.Builder.<EntityTankbot>of(EntityTankbot::new, MobCategory.CREATURE))
                                                                                                   .sized(2.45F, 3.25F).fireImmune().clientTrackingRange(5).updateInterval(3), key))
                                                                           .tag(SUFFOCATION_IMMUNE)
                                                                           .tag(RADIATION_IMMUNE);
    public static final Registrar.EntityHandle<EntityBFAngel> BF_ANGEL = REG.entity("entity_bfa", key -> Entities.build(
                                                                                    EntityType.Builder.<EntityBFAngel>of(EntityBFAngel::new, MobCategory.MONSTER)
                                                                                                      .sized(5.0F, 4.0F).fireImmune().clientTrackingRange(5).updateInterval(3), key))
                                                                            .tag(SUFFOCATION_IMMUNE)
                                                                            .tag(RADIATION_IMMUNE);
    /**
     * 1.7 {@code EntityUFOBase} is {@code ISuffocationImmune} in hbm-space, so the siege saucer is too.
     */
    public static final Registrar.EntityHandle<EntitySiegeUFO> SIEGE_UFO = REG.entity("entity_meme_ufo",
                                                                                      key -> Entities.build(
                                                                                              EntityType.Builder.<EntitySiegeUFO>of(EntitySiegeUFO::new, MobCategory.MONSTER)
                                                                                                                .sized(1.5F, 1.0F).clientTrackingRange(5).updateInterval(3), key))
                                                                              .tag(SUFFOCATION_IMMUNE)
                                                                              .tag(RADIATION_IMMUNE);

    static {
        // The upstream Space bodies have their own complete spawn lists; Host additions remain in other biomes.
        Tags.of("worldgen/biome", NtmSpace.MOD_ID, "celestial_biomes").values(
                "ntmspace:dres_canyon", "ntmspace:dres_plains", "ntmspace:duna_hills", "ntmspace:duna_lowlands",
                "ntmspace:duna_plains", "ntmspace:duna_polar", "ntmspace:duna_polar_hills",
                "ntmspace:eve_mountains", "ntmspace:eve_ocean", "ntmspace:eve_plains", "ntmspace:eve_river",
                "ntmspace:eve_seismic_plains", "ntmspace:ike", "ntmspace:laythe_coast", "ntmspace:laythe_islands",
                "ntmspace:laythe_ocean", "ntmspace:laythe_polar", "ntmspace:minmus_basins", "ntmspace:minmus_hills",
                "ntmspace:moho_basalt", "ntmspace:moho_crag", "ntmspace:moho_lava_sea", "ntmspace:moho_plateau",
                "ntmspace:mun", "ntmspace:orbit", "ntmspace:tekto_forest", "ntmspace:tekto_halogen_hills",
                "ntmspace:tekto_polyvinyl_plains", "ntmspace:tekto_tetrachloric_river", "ntmspace:tekto_vinyl_sands",
                "ntmspace:thatmo").register();
        BiomeModifiers.removeSpawns(NtmSpace.MOD_ID, "remove_host_creepers",
                "#ntmspace:celestial_biomes", List.of("hbm:entity_mob_phosgene_creeper",
                        "hbm:entity_mob_volatile_creeper", "hbm:entity_mob_gold_creeper"));
    }

    static {
        // hbm-space marks these HBM mobs ISuffocationImmune too (History: its space-hook rows for entity/mob/*)
        for (String id : new String[]{"hbm:entity_cyber_crab", "hbm:entity_tesla_crab", "hbm:entity_taint_crab",
                "hbm:entity_test_dummy", "hbm:entity_fbi", "hbm:entity_mob_hunter_chopper", "hbm:entity_mob_mask_man",
                "hbm:entity_parasite_maggot", "hbm:entity_plastic_bag", "hbm:entity_radiation_blaze", "hbm:entity_ufo",
                "hbm:entity_fbi_drone", "hbm:entity_glyphid", "hbm:entity_glyphid_behemoth", "hbm:entity_glyphid_brenda",
                "hbm:entity_glyphid_scout", "hbm:entity_glyphid_nuclear", "hbm:entity_glyphid_blaster",
                "hbm:entity_glyphid_bombardier", "hbm:entity_glyphid_brawler", "hbm:entity_glyphid_digger"}) {
            dev.golem.core.dejson.Tags.member("entity_type", SUFFOCATION_IMMUNE.namespace(), SUFFOCATION_IMMUNE.path(),
                    id);
        }
    }

    // ---------------------------------------------------------------------------------------- items

    /**
     * 1.7: {@code Item}, parts tab; the scutterfish's drop.
     */
    public static final RegistryObject<Item> SCUTTERTAIL = REG.simpleItem("scuttertail", p -> p)
                                                              .transform(Presets.sprite()).tab(HbmTab.MATERIALS);
    /**
     * 1.7: {@code ItemBattery(16,000,000, 16,000,000, 800,000)}, stack 1, control tab, the angel's drop. 1.7 drew it
     * with {@code ItemRenderAngelCore} (the angel's body mesh); here it keeps its 1.7 icon {@code battery_creative_new}.
     */
    public static final RegistryObject<ItemBattery> CORE_ANGEL = REG.<ItemBattery>item("core_angel",
                                                                            p -> new ItemBattery(p, 16_000_000L, 16_000_000L, 800_000L), p -> p.stacksTo(1))
                                                                    .transform(Presets.sprite(
                                                                            "hbm:items/battery_creative_new"))
                                                                    .tab(HbmTab.CONTROL);
    /**
     * 1.7: {@code ItemChopper}, stack 1, consumables tab: summons the angel 25 blocks up.
     */
    public static final RegistryObject<ItemChopper> SPAWN_ANGEL = REG.<ItemChopper>item("spawn_angel",
                                                                             p -> new ItemChopper(p, () -> BF_ANGEL.get(), 25, mob -> {
                                                                             }), p -> p.stacksTo(1))
                                                                     .transform(Presets.sprite())
                                                                     .tab(HbmTab.CONSUMABLES);

    // ---------------------------------------------------------------------------------------- spawn eggs

    /**
     * Every egg, in hbm-space's {@code addMob} order.
     */
    private static final List<RegistryObject<Item>> EGGS = new ArrayList<>();

    static {
        egg("entity_meme_ufo", () -> SIEGE_UFO.get(), 0x303030, 0x800000);
        egg("entity_war_behemoth", () -> WAR_BEHEMOTH.get(), 0x204131, 0x75CE00);
        egg("entity_tankbot", () -> TANKBOT.get(), 0x204131, 0x75CE00);
        egg("entity_bfa", () -> BF_ANGEL.get(), 0x204131, 0x75CE00);
        egg("entity_mob_flesh_creeper", () -> CREEPER_FLESH.get(), 0xFF0000, 0xD65337);
        egg("entity_moon_cow", () -> MOON_COW.get(), 0xECD376, 0x262A44);
        egg("entity_scutterfish", () -> SCUTTERFISH.get(), 0xC8C9CD, 0x858894);
        egg("entity_scuttlecrab", () -> SCUTTLECRAB.get(), 0xF17951, 0xEDDABB);
        egg("entity_depthsquid", () -> DEPTH_SQUID.get(), 0x00B4DF, 0x016085);
        egg("entity_scrapfish", () -> SCRAPFISH.get(), 0xDF9835, 0x510E13);
        egg("entity_siftereel", () -> SIFTER_EEL.get(), 0x5B963E, 0xC0B286);
    }

    /**
     * As HBM's {@code ModSpawnEggs}: {@code <entity id>_spawn_egg}, the two-layer egg tinted with the 1.7 colours.
     */
    private static void egg(String entity, Supplier<EntityType<? extends Mob>> type, int primary, int secondary) {
        EGGS.add(REG.<Item>item(entity + "_spawn_egg", p -> SpawnEggs.create(type, primary, secondary, p))
                    .transform(Presets.sprite("hbm:items/spawn_egg", "hbm:items/spawn_egg_overlay")
                                      .tints(Tints.constant(0xFF000000 | primary),
                                              Tints.constant(0xFF000000 | secondary))));
    }

    /**
     * The eggs, read-only, in hbm-space's order.
     */
    public static List<RegistryObject<Item>> eggs() {
        return Collections.unmodifiableList(EGGS);
    }

    private SpaceMobs() {
    }

    public static void init() {
        MobWeaponsSpace.init();
        LivingAttributes.register(MOON_COW, Cow::createAttributes);
        LivingAttributes.register(SCUTTERFISH, EntityFish::createAttributes);
        LivingAttributes.register(SCRAPFISH, EntityFish::createAttributes);
        LivingAttributes.register(SIFTER_EEL, EntityFish::createAttributes);
        LivingAttributes.register(SCUTTLECRAB, EntityScuttlecrab::createAttributes);
        LivingAttributes.register(DEPTH_SQUID, Squid::createAttributes);
        LivingAttributes.register(CREEPER_FLESH, Creeper::createAttributes);
        LivingAttributes.register(WAR_BEHEMOTH, EntityWarBehemoth::createAttributes);
        LivingAttributes.register(TANKBOT, EntityTankbot::createAttributes);
        LivingAttributes.register(BF_ANGEL, EntityBFAngel::createAttributes);
        LivingAttributes.register(SIEGE_UFO, EntitySiegeUFO::createAttributes);

        // 1.7 SpawnerAnimals: creatures on a solid floor, water creatures in water over a liquid. The 1.7 per-mob
        // rules (grass and light for the cow, darkness for the war machines, y 45..63 for the squid, tumor for the
        // flesh creeper) are each mob's checkSpawnRules; 1.7's chunk-generation spawning asked none of them.
        Spawns.placement(() -> MOON_COW.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.GENERATION_ANIMAL);
        Spawns.placement(() -> TANKBOT.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.GENERATION_MONSTER);
        Spawns.placement(() -> WAR_BEHEMOTH.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.GENERATION_MONSTER);
        Spawns.placement(() -> CREEPER_FLESH.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.MONSTER);
        Spawns.placement(() -> SCUTTERFISH.get(), Spawns.Placement.IN_WATER, Spawns.Rule.LIQUID_BELOW);
        Spawns.placement(() -> SCRAPFISH.get(), Spawns.Placement.IN_WATER, Spawns.Rule.LIQUID_BELOW);
        Spawns.placement(() -> SIFTER_EEL.get(), Spawns.Placement.IN_WATER, Spawns.Rule.LIQUID_BELOW);
        Spawns.placement(() -> SCUTTLECRAB.get(), Spawns.Placement.IN_WATER, Spawns.Rule.LIQUID_BELOW);
        Spawns.placement(() -> DEPTH_SQUID.get(), Spawns.Placement.IN_WATER, Spawns.Rule.LIQUID_BELOW);

        EntityAITankshell.registerCasing();

        // The vanilla spawn-egg tab by id, as HBM's eggs (ModSpawnEggs.init)
        Tabs.modifyEntries(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                        dev.golem.core.Ids.of("minecraft", "spawn_eggs")), entries -> {
                    for (RegistryObject<Item> egg : EGGS) entries.accept(new ItemStack(egg.get()));
                });
    }
}
