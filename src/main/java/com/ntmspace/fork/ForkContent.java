package com.ntmspace.fork;

import com.hbm.creativetabs.HbmTab;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Damage;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's non-space fork content that is not a single feature family of its own (fork-content triage,
 * {@code scratchpad/m2/fork-content-triage.md}): the {@code nitan}, {@code flashbang} and {@code slippery} effects,
 * the NITAN flask, the {@code pellet_charged} Bobmazon offer and the {@code [BOMBPL]} placement log. Values are from
 * the hbm-space source; base-HBM behaviour changes go through HBM/Golem hooks and data maps, so they exist only while
 * NTM: Space is installed.
 */
public final class ForkContent {

    private static final String NS = NtmSpace.MOD_ID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NS);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(() -> Registries.MOB_EFFECT, NS);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NS);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NS);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, EFFECTS, BLOCK_ENTITIES, MENUS);

    // Effects go on EFFECTS directly: REG.register(Registries.MOB_EFFECT, ...) would read Registries in this static
    // initialiser, which 1.20.1 cannot do in Dejson's plain JVM (AGENTS.md section 8).
    /** hbm-space {@code HbmPotion.nitan} ("Ambrosia"). */
    public static final RegistryObject<MobEffect> NITAN_EFFECT = EFFECTS.register("nitan", () -> new ForkEffects.Nitan());
    /** hbm-space {@code HbmPotion.flashbang}. */
    public static final RegistryObject<MobEffect> FLASHBANG_EFFECT = EFFECTS.register("flashbang", () -> new ForkEffects.Flashbang());
    /** hbm-space {@code HbmPotion.slippery} ("Butterfingers"). */
    public static final RegistryObject<MobEffect> SLIPPERY_EFFECT = EFFECTS.register("slippery", () -> new ForkEffects.Slippery());

    /** hbm-space {@code flask_infusion} metadata NITAN, consumables tab, texture {@code flask_nitan}. */
    public static final RegistryObject<ItemFlaskNitan> FLASK_INFUSION_NITAN = REG.item("flask_infusion_nitan",
            ItemFlaskNitan::new, p -> p).transform(Presets.sprite(NS + ":items/flask_nitan")).tab(HbmTab.CONSUMABLES);

    /**
     * hbm-space {@code det_salt} ("Salted Nuclear Charge", {@code ExplosiveCharge}, iron, hardness 0.1, resistance 0, nuke
     * tab): it detonates exactly like {@code det_nuke} - hbm-space's {@code statFacSalted} sets an MK5 {@code salted} flag
     * that nothing reads (its fallout rain never learns it), so no salted fallout falls. Top and bottom
     * {@code det_cobalt_top}, sides {@code det_nuke}.
     */
    public static final RegistryObject<com.hbm.blocks.bomb.ExplosiveCharge> DET_SALT = REG.block("det_salt",
                    p -> new com.hbm.blocks.bomb.ExplosiveCharge(p, com.hbm.blocks.bomb.ExplosiveCharge.Kind.NUKE),
                    p -> p.strength(0.1F, 0.0F).requiresCorrectToolForDrops())
            .transform(Presets.column(NS + ":blocks/det_cobalt_top", "hbm:blocks/det_nuke"))
            .loot(dev.golem.core.registry.Loot.self()).tag(dev.golem.core.registry.BlockTag.PICKAXE);
    public static final RegistryObject<net.minecraft.world.item.BlockItem> DET_SALT_ITEM = REG.blockItem(DET_SALT).tab(HbmTab.BOMBS);

    /**
     * hbm-space {@code salted_fallout} (a {@code BlockFallout}, texture {@code ash}, blocks tab): random ticks add 50 RAD
     * to the chunk ({@link SaltedFallout}). Nothing places it in hbm-space (see {@link #DET_SALT}); creative only.
     */
    public static final RegistryObject<SaltedFallout> SALTED_FALLOUT = REG.block("salted_fallout", SaltedFallout::new,
                    p -> p.strength(0.1F).sound(net.minecraft.world.level.block.SoundType.SAND).noOcclusion().replaceable()
                            .forceSolidOn().randomTicks().isValidSpawn((state, level, pos, type) -> false))
            .transform(Presets.statesOnly("hbm:block/fallout", dev.golem.core.dejson.Variants.simple()))
            .icon(d -> d.models(dev.golem.core.dejson.Models.blockItem("hbm:block/fallout")))
            .loot(dev.golem.core.registry.Loot.only("hbm:falloutitem")).tag(dev.golem.core.registry.BlockTag.SHOVEL);
    public static final RegistryObject<net.minecraft.world.item.BlockItem> SALTED_FALLOUT_ITEM = REG.blockItem(SALTED_FALLOUT)
            .tab(HbmTab.BLOCKS);

    /**
     * hbm-space {@code nuke_antimatter} ("Antimatter Bomb", {@code NukeAntimatter}, iron, hardness 5, resistance 6000,
     * nuke tab): drawn by its renderer, facing 2..5 like HBM's Balefire Bomb.
     */
    public static final RegistryObject<com.ntmspace.fork.antimatter.NukeAntimatter> NUKE_ANTIMATTER = REG.block("nuke_antimatter",
                    com.ntmspace.fork.antimatter.NukeAntimatter::new,
                    p -> p.strength(5.0F, 6000.0F).sound(net.minecraft.world.level.block.SoundType.METAL)
                            .requiresCorrectToolForDrops().noOcclusion())
            .transform(Presets.rendered(dev.golem.core.dejson.Variants.uniformRange("facing", 2, 5)))
            .loot(dev.golem.core.registry.Loot.self()).tag(dev.golem.core.registry.BlockTag.PICKAXE);
    public static final RegistryObject<net.minecraft.world.item.BlockItem> NUKE_ANTIMATTER_ITEM = REG.blockItem(NUKE_ANTIMATTER)
            .tab(HbmTab.BOMBS);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.ntmspace.fork.antimatter.TileEntityAntimatter>>
            NUKE_ANTIMATTER_BE = REG.blockEntity("nuke_antimatter", () -> dev.golem.core.registry.BlockEntities.serverTicking(
                    com.ntmspace.fork.antimatter.TileEntityAntimatter::new, com.ntmspace.fork.antimatter.TileEntityAntimatter::serverTick,
                    NUKE_ANTIMATTER.get()));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<com.ntmspace.fork.antimatter.ContainerNukeAntimatter>>
            NUKE_ANTIMATTER_MENU = REG.menu("nuke_antimatter",
                    () -> dev.golem.core.gui.Menus.typeWithPos(com.ntmspace.fork.antimatter.ContainerNukeAntimatter::new));

    /**
     * hbm-space {@code ore_bedrock_oil} off Kerbin ({@link BlockBedrockOilBody}): Tekto's bedrock oil (Tekto crude +
     * hydrogen-rich gas). Unbreakable like HBM's {@code hbm:ore_bedrock_oil}, no item, no drops.
     */
    public static final RegistryObject<BlockBedrockOilBody> ORE_BEDROCK_OIL = REG.block("ore_bedrock_oil", BlockBedrockOilBody::new,
                    p -> p.strength(-1.0F, 3600000.0F).noLootTable())
            .transform(Presets.cubeAll("hbm:blocks/ore_bedrock_oil"));

    /** hbm-space's new bobbleheads ({@link com.ntmspace.fork.bobble.BlockSpaceBobble}): iron, hardness 0, drawn by its renderer. */
    public static final RegistryObject<com.ntmspace.fork.bobble.BlockSpaceBobble> BOBBLEHEAD = REG.block("bobblehead",
                    com.ntmspace.fork.bobble.BlockSpaceBobble::new,
                    p -> p.strength(0.0F, 0.0F).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion())
            .transform(Presets.rendered(dev.golem.core.dejson.Variants.uniformRange("rotation", 0, 15)))
            .loot(dev.golem.core.registry.Loot.nothing());
    /** One item per type, index = type ordinal (HBM's variant-family order: 1..n-1, then 0 = the block's own item). */
    public static final List<RegistryObject<com.hbm.items.block.ItemBlockDecoVariant>> BOBBLEHEAD_ITEMS = bobbleItems();
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.ntmspace.fork.bobble.BlockSpaceBobble.Bobble>>
            BOBBLEHEAD_BE = REG.blockEntity("bobblehead", () -> dev.golem.core.registry.BlockEntities.type(
                    (pos, state) -> new com.ntmspace.fork.bobble.BlockSpaceBobble.Bobble(ForkContent.BOBBLEHEAD_BE.get(), pos, state),
                    BOBBLEHEAD.get()));

    private static List<RegistryObject<com.hbm.items.block.ItemBlockDecoVariant>> bobbleItems() {
        var types = com.ntmspace.fork.bobble.BlockSpaceBobble.Type.VALUES;
        @SuppressWarnings("unchecked")
        RegistryObject<com.hbm.items.block.ItemBlockDecoVariant>[] items = new RegistryObject[types.length];
        for (int k = 1; k <= types.length; k++) {
            int variant = k % types.length;
            items[variant] = REG.item(types[variant].id(), p -> new com.hbm.items.block.ItemBlockDecoVariant(BOBBLEHEAD.get(), p, variant,
                            i -> net.minecraft.network.chat.Component.translatable("block.hbm.bobblehead"), null),
                    dev.golem.core.world.Compat::blockDescription);
        }
        List<RegistryObject<com.hbm.items.block.ItemBlockDecoVariant>> members = List.of(items);
        com.hbm.items.block.ItemBlockDecoVariant.Families.record(BOBBLEHEAD, members);
        return members;
    }

    /** hbm-space {@code ItemCassette.TrackType.DISEMBODIED} ("Ducc", {@code alarm.ducc}, loop, 0xb3a8c1, range 50). */
    public static final com.hbm.items.machine.ItemCassette.TrackType TRACK_DISEMBODIED = com.hbm.items.machine.ItemCassette.TrackType.register(
            NS + ":DISEMBODIED", "Ducc", NS + ":alarm.ducc", com.hbm.items.machine.ItemCassette.SoundType.LOOP, 0xb3a8c1, 50);
    /** hbm-space {@code TrackType.SUICIDE} ("Xbox Live", {@code alarm.mama}, loop, 0xb3a8c1, range 70). */
    public static final com.hbm.items.machine.ItemCassette.TrackType TRACK_SUICIDE = com.hbm.items.machine.ItemCassette.TrackType.register(
            NS + ":SUICIDE", "Xbox Live", NS + ":alarm.mama", com.hbm.items.machine.ItemCassette.SoundType.LOOP, 0xb3a8c1, 70);
    public static final RegistryObject<com.hbm.items.machine.ItemCassette> SIREN_TRACK_DISEMBODIED = cassette("siren_track_disembodied", TRACK_DISEMBODIED);
    public static final RegistryObject<com.hbm.items.machine.ItemCassette> SIREN_TRACK_SUICIDE = cassette("siren_track_suicide", TRACK_SUICIDE);

    /** HBM's cassette registration ({@code ModPowerGenItems}): the plain cassette, its overlay tinted with the track colour. */
    private static RegistryObject<com.hbm.items.machine.ItemCassette> cassette(String id, com.hbm.items.machine.ItemCassette.TrackType track) {
        return REG.item(id, p -> new com.hbm.items.machine.ItemCassette(p, track), p -> p.stacksTo(1))
                .transform(Presets.sprite("hbm:items/cassette", "hbm:items/cassette_overlay")
                        .tints(dev.golem.core.dejson.Tints.constant(-1), dev.golem.core.dejson.Tints.customModelData(0, track.getColor())))
                .tab(HbmTab.TEMPLATES);
    }

    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NS);
    private static final Registrar SOUND_REG = Registrar.of(SOUNDS);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> ALARM_DUCC = SOUND_REG.sound("alarm.ducc");
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> ALARM_MAMA = SOUND_REG.sound("alarm.mama");

    /** hbm-space {@code bomb_caller} metadata 8: "Civilian Airliner." (foil like every designator from the A-bomb on). */
    public static final RegistryObject<com.hbm.items.tool.ItemBombCaller> BOMB_CALLER_AIRLINER = REG.item("bomb_caller_airliner",
                    p -> new com.hbm.items.tool.ItemBombCaller(p, NS + ":airliner", "Civilian Airliner.", true, "Rerouted Civilian Traffic!"),
                    p -> p)
            .transform(Presets.handheld("hbm:items/bomb_caller")).tab(HbmTab.CONSUMABLES);

    private ForkContent() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        Nitan.init();
        BombPlacementLog.init();
        com.ntmspace.fork.antimatter.AntimatterBalefire.init();
        FluidOres.init();
        com.ntmspace.fork.bobble.SpaceBobbles.init();
        Slippery.init();
        ForkBombers.init();
        ForkTweaks.init();
        ForkBaseChanges.init(); // part 3: the rest of hbm-space's small base-HBM changes
        com.ntmspace.fork.neutron.NeutronActivation.init(); // hbm-space neutron activation (config/ntmspace.cfg, default off)
        // hbm-space IRepairable.addGenericOverlay: HBM's damaged tank and refinery list their repair without a blowtorch too
        com.hbm.blocks.machine.MachineExplosionDamage.alwaysShowRepair = true;
        // hbm-space's fracking tower takes each deposit's own amounts (BlockOreFluid), i.e. the derrick's: 500 oil,
        // 100..500 gas, drain 0.05 (1.7: 1000, 100..500, 0.02); bedrock oil keeps 100 + 10..50 through its
        // fluid_deposit yields 0.2 / 0.1 (data/ntmspace/value_maps/patches/hbm/block/fluid_deposit.json of this mod)
        com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower.oilPerDeposit = 500;
        com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower.drainChance = 0.05D;
        // hbm-space TileEntityDeuteriumExtractor: power use maxPower / 100, and only above 200 HE (1.7: / 20)
        com.hbm.tileentity.machine.TileEntityDeuteriumExtractor.powerDivisor = 100;
        com.hbm.tileentity.machine.TileEntityDeuteriumExtractor.powerFloorExclusive = 200;
        // hbm-space ItemRag: above 500 RAD a used rag becomes rag_blood instead of rag_piss
        com.hbm.items.special.ItemRag.USE.register(player -> com.hbm.capability.HbmLivingProps.getRadiation(player) > 500
                ? BuiltInRegistries.ITEM.getOptional(Ids.of(NS, "rag_blood")).map(ItemStack::new).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY);
        // hbm-space BobmazonOfferFactory: special.add(pellet_charged, STEEL, 16), right after the empty crucible
        BobmazonOfferFactory.OFFERS.register((standard, special) -> {
            ItemStack pellet = BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", "pellet_charged"))
                    .map(ItemStack::new).orElse(ItemStack.EMPTY);
            if (pellet.isEmpty()) return;
            special.add(indexAfter(special, "crucible"),
                    new BobmazonOfferFactory.Offer(pellet, BobmazonOfferFactory.Requirement.STEEL, 16, 0));
        });
    }

    /** The position after the first offer of {@code hbm:<path>}, or the end of the list. */
    static int indexAfter(List<BobmazonOfferFactory.Offer> offers, String path) {
        for (int i = 0; i < offers.size(); i++) {
            ItemStack stack = offers.get(i).offer;
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Ids.of("hbm", path))) return i + 1;
        }
        return offers.size();
    }

    private static boolean declared;

    /** Dejson: the {@code nitan} damage type (absolute, bypasses armour, allowed in creative). */
    public static void declare() {
        if (declared) return;
        declared = true;
        Damage.declare(NS, Damage.type("nitan"));
        // hbm-space ItemRenderLibrary bobblehead, per type (client RenderSpaceBobble.Item, "ntmspace:bobblehead")
        for (var type : com.ntmspace.fork.bobble.BlockSpaceBobble.Type.VALUES) {
            dev.golem.core.dejson.Dejson.item(NS, type.id()).shares("hbm:item/machine_obj_context_base").register();
            com.google.gson.JsonObject model = new com.google.gson.JsonObject();
            model.addProperty("type", "hbm:custom_item");
            model.addProperty("renderer", NS + ":bobblehead");
            model.addProperty("frame", "machine");
            dev.golem.core.dejson.Dejson.itemDefinition(NS + ":item/" + type.id()).add("model", model);
            dev.golem.core.dejson.Dejson.itemDefinition(NS + ":item/" + type.id()).addProperty("oversized_in_gui", true);
        }
        // hbm-space ItemRenderLibrary nuke_antimatter: inv translate(0, 7, 0) scale(3.25) rotate(180 about X); common
        // blomb_tex on blomb.obj, smooth shading
        dev.golem.core.dejson.Dejson.item(NS, "nuke_antimatter")
                .definition(com.hbm.items.MachineObjModel.of(NS + ":models/bombs/blomb.obj", NS + ":textures/models/bombs/blomb_tex.png")
                        .scale(1.0).ty(0.0).yaw(0.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 7.0, 0.0).guiScale(3.25).guiRotateStep(180.0, 0.0, 0.0))
                .oversizedInGui().register();
        dev.golem.core.dejson.Tags.member("damage_type", "minecraft", "bypasses_armor", NS + ":nitan");
        dev.golem.core.dejson.Tags.member("damage_type", "minecraft", "bypasses_effects", NS + ":nitan");
        dev.golem.core.dejson.Tags.member("damage_type", "minecraft", "bypasses_invulnerability", NS + ":nitan");
    }
}
