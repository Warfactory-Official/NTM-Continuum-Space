package com.ntmspace.registry;

import com.hbm.items.ModRBMKItems;
import dev.golem.core.dejson.Equipment;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Armors;
import com.hbm.creativetabs.HbmTab;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.ModRtgContent;
import com.hbm.items.armor.ItemModInsert;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemInfiniteFluid;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.item.special.EffectRun;
import com.ntmspace.item.special.ItemAniman;
import com.ntmspace.item.special.ItemAtmosphereScanner;
import com.ntmspace.item.special.ItemButterSword;
import com.ntmspace.item.special.ItemModFlippers;
import com.ntmspace.item.special.ItemModHeavyBoots;
import com.ntmspace.item.special.ItemModV1;
import com.ntmspace.item.special.ItemWandTime;
import dev.golem.core.dejson.Damage;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.ItemModels;
import dev.golem.core.dejson.JukeboxSongs;
import dev.golem.core.dejson.Models;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.Records;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * hbm-space's special items (its {@code ModItems} values), each on HBM's port class where one exists:
 *
 * <ul>
 *   <li>armour modules: {@code flippers}, {@code heavy_boots}, {@code v1} (own classes) and {@code insert_cmb}
 *       ({@code ItemModInsert(9999, 0.7, 0.9, 0.4, 1)}, corrosion protection = the {@code #ntmspace:corrosion_protection}
 *       tag, see {@code AtmosphereContent});</li>
 *   <li>tools and misc: {@code wand_time}, {@code butter_sword}, the records {@code record_gs/gp/el} (DUODEC),
 *       {@code atmosphere_scanner}, {@code oxy_pinwheel} ({@code ItemInfiniteFluid(OXYGEN, 1)}), {@code laser_crystal_iron}
 *       (an X-ray FEL crystal: hbm-space put {@code XRAY} between {@code UV} and {@code GAMMA}: strength 4, GAMMA 5, DRX 6) and
 *       {@code animan} (hbm-space's {@code ItemPill(0)} branch, with its {@code run} effect);</li>
 *   <li>RTG pellets {@code pellet_rtg_berkelium/cf251/cf252} and the depleted americium pellet berkelium decays into;</li>
 *   <li>RBMK: pellets and rods for BK-247 and the three curium enrichments, with hbm-space's {@code SLOW_LINEAR} burn and
 *       {@code CF_SLOPE} depletion functions; and the nine Watz pellets hbm-space added to {@code EnumWatzType}.</li>
 * </ul>
 *
 * Reactor and hazard values are HBM value-map entries ({@code data/ntmspace/value_maps/patches/hbm/item/{rtg_pellet,watz_pellet,hazards}.json},
 * written by {@code tools/gen_special_item_data.py}); the RBMK rods also carry them in code like HBM's own.
 *
 * <p>Not registered: {@code rbmk_pellet_lecf/mecf/hecf}. hbm-space constructs them but never calls
 * {@code GameRegistry.registerItem} for them (and ships no texture), so they never existed in a 1.7 game.
 */
public final class SpaceSpecialItems {

    private static final String NS = NtmSpace.MOD_ID;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(() -> Registries.MOB_EFFECT, NS);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NS);
    private static final Registrar REG = Registrar.of(ITEMS, EFFECTS, SOUNDS);

    // ---------------------------------------------------------------------------------------- registries of HBM's enums

    /** hbm-space {@code EnumBurnFunc.SLOW_LINEAR}: {@code sqrt(2x + 30) / 10 * reactivity / 2.5}. */
    public static final EnumBurnFunc SLOW_LINEAR = EnumBurnFunc.register("SLOW_LINEAR", "trait.rbmx.flux.slow_linear",
            "sqrt(2 * %1$s + 30) / 10 * %2$s / 2.5", (x, e, r, s) -> Math.sqrt(2 * x + 30) / 10 * r / 2.5);
    /** hbm-space {@code EnumDepleteFunc.CF_SLOPE}: californium build-up in curium rods, peaking near 193% at 60% depletion. */
    public static final EnumDepleteFunc CF_SLOPE = EnumDepleteFunc.register("CF_SLOPE", e -> e + (Math.sin(e * Math.PI)) * 1.4);
    /**
     * hbm-space {@code EnumWavelengths.XRAY} (gold text, colour 0xF8E6A7, no beam effect). hbm-space inserted it between UV
     * (3) and GAMMA, so its ordinals, which the SILEX speed and the FEL draw compute with, are X-ray 4, GAMMA 5 and DRX 6
     * (1.7: GAMMA 4, DRX 5). With the addon installed HBM's GAMMA and DRX move up one to match (user rule
     * 2026-10-11: NTM: Space has hbm-space's defaults; plain HBM keeps 1.7's).
     */
    public static final EnumWavelengths XRAY = xray();

    private static EnumWavelengths xray() {
        EnumWavelengths.setStrength(EnumWavelengths.GAMMA, 5D);
        EnumWavelengths.setStrength(EnumWavelengths.DRX, 6D);
        return EnumWavelengths.register("XRAY", 4D, "wavelengths.name.xray", "wavelengths.waveRange.xray",
                0xF8E6A7, 0xF8E6A7, ChatFormatting.GOLD, entity -> {});
    }

    // ---------------------------------------------------------------------------------------- effects, sounds

    /** hbm-space {@code HbmPotion.run} ("GOD WATCHES YOU"). */
    // On the effect register itself, not REG.register(Registries.MOB_EFFECT, ...): that reads Registries in this class's
    // static initialiser, which 1.20.1 cannot do in Dejson's plain JVM.
    public static final RegistryObject<MobEffect> RUN = EFFECTS.register("run", () -> new EffectRun());
    /** hbm-space {@code hbm:player.oil}. */
    public static final RegistryObject<SoundEvent> PLAYER_OIL = REG.sound("player.oil");
    /** hbm-space {@code hbm:music.recordGodSpeed}, {@code recordGoop}, {@code recordEthereal}. */
    public static final RegistryObject<SoundEvent> RECORD_GOD_SPEED = REG.sound("music.record_god_speed");
    public static final RegistryObject<SoundEvent> RECORD_GOOP = REG.sound("music.record_goop");
    public static final RegistryObject<SoundEvent> RECORD_ETHEREAL = REG.sound("music.record_ethereal");

    // ---------------------------------------------------------------------------------------- armour modules

    public static final RegistryObject<ItemModFlippers> FLIPPERS = REG.item("flippers", ItemModFlippers::new, p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemModHeavyBoots> HEAVY_BOOTS = REG.item("heavy_boots", ItemModHeavyBoots::new,
            p -> Armors.equippable(p.stacksTo(1), ArmorSlot.BOOTS,
                    "ntmspace:heavy_boots")).transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemModV1> V1 = REG.item("v1", ItemModV1::new, p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    /** 1.7 {@code ItemModInsert(9999, 0.7F, 0.9F, 0.4F, 1F).withCorrosionProtection()}. */
    public static final RegistryObject<ItemModInsert> INSERT_CMB = REG.item("insert_cmb",
            p -> new ItemModInsert(9999, 0.7F, 0.9F, 0.4F, 1F, p), p -> p)
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);

    // ---------------------------------------------------------------------------------------- tools and misc

    /** 1.7: stack 1, consumables tab, full 3D. */
    public static final RegistryObject<ItemWandTime> WAND_TIME = REG.item("wand_time", ItemWandTime::new, p -> p.stacksTo(1))
            .transform(Presets.handheld()).tab(HbmTab.CONSUMABLES);
    /** 1.7 {@code BSword(ToolMaterial.WOOD)}, vanilla combat tab (HBM lists no vanilla tabs: weapons), texture {@code budder_sword}. */
    public static final RegistryObject<ItemButterSword> BUTTER_SWORD = REG.item("butter_sword", ItemButterSword::new, p -> p)
            .transform(Presets.handheld(NS + ":items/budder_sword")).tab(HbmTab.WEAPONS);

    public static final RegistryObject<Item> RECORD_GS = record("record_gs", "gs", () -> RECORD_GOD_SPEED.get(), 354);
    public static final RegistryObject<Item> RECORD_GP = record("record_gp", "gp", () -> RECORD_GOOP.get(), 241);
    public static final RegistryObject<Item> RECORD_EL = record("record_el", "el", () -> RECORD_ETHEREAL.get(), 198);

    /** 1.7: stack 1, consumables tab. */
    public static final RegistryObject<ItemAtmosphereScanner> ATMOSPHERE_SCANNER = REG.item("atmosphere_scanner",
            ItemAtmosphereScanner::new, p -> p.stacksTo(1)).transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    /** 1.7 {@code ItemInfiniteFluid(Fluids.OXYGEN, 1)}: 1 mB, chance 1, no pressure, no water table; parts tab. */
    public static final RegistryObject<ItemInfiniteFluid> OXY_PINWHEEL = REG.item("oxy_pinwheel",
            p -> new ItemInfiniteFluid(p, () -> Fluids.OXYGEN, 1, 1, false), p -> p)
            .transform(Presets.sprite()).tab(HbmTab.MATERIALS);
    /** 1.7 {@code ItemFELCrystal(EnumWavelengths.XRAY)}, control tab. */
    public static final RegistryObject<ItemFELCrystal> LASER_CRYSTAL_IRON = REG.item("laser_crystal_iron",
            p -> new ItemFELCrystal(p, XRAY, false), p -> p).transform(Presets.sprite()).tab(HbmTab.CONTROL);
    /** 1.7 {@code ItemPill(0)}: food 0, always edible, consumables tab. */
    public static final RegistryObject<ItemAniman> ANIMAN = REG.item("animan", ItemAniman::new,
            p -> CompatConsumables.foodAlwaysEdible(p, 0, 0F, List.of())).transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);

    // ---------------------------------------------------------------------------------------- RTG

    /** 1.7 {@code DepletedRTGMaterial.AMERICIUM} (HBM's enum has no such entry): plate container like the others. */
    public static final RegistryObject<CompatItem> PELLET_RTG_DEPLETED_AMERICIUM = REG.item("pellet_rtg_depleted_americium",
                    CompatItem::new, p -> p.craftRemainder(ModItems.plate_iron.get()))
            .transform(Presets.sprite(NS + ":items/pellet_rtg_depleted.americium")).tab(HbmTab.CONTROL);

    /** Heat 20, decays to depleted americium; lifespan {@code getLifespan(13.8, LONG) * 1.5} (the data map entry rules). */
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_BERKELIUM = rtg("pellet_rtg_berkelium", 20,
            9_936_000_000L, "Berkelium RTG Pellet");
    /** Heat {@code rtgDecay ? 600 : 200}, decays to lead; lifespan {@code getLifespan(1, SHORT) * 2.5}. */
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_CF251 = rtg("pellet_rtg_cf251",
            com.hbm.config.MachineConfig.rtgDecay() ? 600 : 200, 120_000L, "Californium-251 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_CF252 = rtg("pellet_rtg_cf252",
            com.hbm.config.MachineConfig.rtgDecay() ? 600 : 200, 120_000L, "Californium-252 RTG Pellet");

    // ---------------------------------------------------------------------------------------- RBMK

    /** Pellet id to its 1.7 {@code fullName} (also the rod's, 1.7 {@code ItemRBMKRod(pellet)}). */
    public static final Map<String, String> RBMK_NAMES = new LinkedHashMap<>();

    public static final RegistryObject<ItemRBMKPellet> RBMK_PELLET_BK247 = pellet("bk247", "Highly Enriched Berkelium-247");
    public static final RegistryObject<ItemRBMKPellet> RBMK_PELLET_LECM = pellet("lecm", "Low Enriched Curium-245");
    public static final RegistryObject<ItemRBMKPellet> RBMK_PELLET_MECM = pellet("mecm", "Medium Enriched Curium-245");
    public static final RegistryObject<ItemRBMKPellet> RBMK_PELLET_HECM = pellet("hecm", "Highly Enriched Curium-245");

    public static final RegistryObject<ItemRBMKRod> RBMK_FUEL_BK247 = rod("bk247", RBMK_PELLET_BK247,
            r -> r.setYield(100_000_000D).setStats(50).setFunction(EnumBurnFunc.LINEAR).setHeat(2D).setMeltingPoint(2993));
    public static final RegistryObject<ItemRBMKRod> RBMK_FUEL_LECM = rod("lecm", RBMK_PELLET_LECM,
            r -> r.setYield(30_000_000D).setStats(20, 5).setFunction(SLOW_LINEAR).setDepletionFunction(CF_SLOPE)
                    .setHeat(1.25D).setMeltingPoint(1340).setDiffusion(0.4D));
    public static final RegistryObject<ItemRBMKRod> RBMK_FUEL_MECM = rod("mecm", RBMK_PELLET_MECM,
            r -> r.setYield(27_000_000D).setStats(30, 10).setFunction(SLOW_LINEAR).setDepletionFunction(CF_SLOPE)
                    .setHeat(1.25D).setMeltingPoint(1720).setDiffusion(0.3D));
    public static final RegistryObject<ItemRBMKRod> RBMK_FUEL_HECM = rod("hecm", RBMK_PELLET_HECM,
            r -> r.setYield(24_000_000D).setStats(60, 25).setFunction(SLOW_LINEAR).setDepletionFunction(CF_SLOPE)
                    .setHeat(1.3D).setMeltingPoint(1880).setDiffusion(0.3D));

    // ---------------------------------------------------------------------------------------- Watz

    /** hbm-space's {@code EnumWatzType} additions, in enum order (lower case = the 1.7 lang suffix). */
    public static final String[] WATZ_TYPES = {"pu241", "amrg", "amf", "cmrg", "cmf", "bk247", "cf251", "cf252", "es253"};
    public static final Map<String, RegistryObject<ItemWatzPellet>> WATZ_FRESH = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<ItemWatzPellet>> WATZ_DEPLETED = new LinkedHashMap<>();

    static {
        for (String type : WATZ_TYPES) {
            // the type passed is only the item's fallback: every value of these pellets is their hbm:watz_pellet entry
            WATZ_FRESH.put(type, REG.item("watz_pellet_" + type, p -> new ItemWatzPellet(p, EnumWatzType.SCHRABIDIUM, false),
                    p -> p.stacksTo(16)).transform(Presets.sprite()).tab(HbmTab.CONTROL));
            WATZ_DEPLETED.put(type, REG.item("watz_pellet_depleted_" + type, p -> new ItemWatzPellet(p, EnumWatzType.SCHRABIDIUM, true),
                    p -> p.stacksTo(16)).transform(Presets.sprite()).tab(HbmTab.CONTROL));
        }
    }

    // ---------------------------------------------------------------------------------------- helpers

    private static RegistryObject<Item> record(String id, String song, java.util.function.Supplier<SoundEvent> sound, int seconds) {
        // 1.7 ItemModRecord: rare, vanilla misc tab (HBM lists its records in consumables)
        return REG.item(id, p -> Records.disc(p, NS, song, sound, 15, seconds), p -> p.rarity(Rarity.RARE))
                .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    }

    private static RegistryObject<ItemRTGPellet> rtg(String id, int heat, long lifespan, String name) {
        // code fallback only; the hbm:rtg_pellet entry holds the values and the decay item
        return REG.item(id, p -> new ItemRTGPellet(p, heat, lifespan,
                        () -> ModRtgContent.DEPLETED.get(DepletedRTGMaterial.LEAD).get(), name), p -> p.stacksTo(1))
                .transform(Presets.sprite()).tab(HbmTab.CONTROL);
    }

    private static RegistryObject<ItemRBMKPellet> pellet(String type, String name) {
        RBMK_NAMES.put("rbmk_pellet_" + type, name);
        return REG.item("rbmk_pellet_" + type, (Item.Properties p) -> new ItemRBMKPellet(p, name)).tab(HbmTab.CONTROL);
    }

    private static RegistryObject<ItemRBMKRod> rod(String type, RegistryObject<ItemRBMKPellet> pellet, Consumer<ItemRBMKRod> configure) {
        String name = RBMK_NAMES.get("rbmk_pellet_" + type);
        return REG.item("rbmk_fuel_" + type, (Item.Properties p) -> {
            ItemRBMKRod rod = new ItemRBMKRod(p.stacksTo(1), name);
            configure.accept(rod);
            // The pellet link resolves post-flush through ModRBMKItems.PELLETS (see init), like HBM's own rods.
            return rod;
        }).transform(Presets.sprite()).tab(HbmTab.CONTROL);
    }

    private SpaceSpecialItems() {}

    public static void init() {
        // Join HBM's rod-to-pellet linkage so Hazards.init() resolves our recycling pellets to their items.
        ModRBMKItems.PELLETS.put(RBMK_FUEL_BK247, RBMK_PELLET_BK247);
        ModRBMKItems.PELLETS.put(RBMK_FUEL_LECM, RBMK_PELLET_LECM);
        ModRBMKItems.PELLETS.put(RBMK_FUEL_MECM, RBMK_PELLET_MECM);
        ModRBMKItems.PELLETS.put(RBMK_FUEL_HECM, RBMK_PELLET_HECM);
    }

    private static boolean declared;

    /** Dejson: the pellets' depletion/xenon models, the jukebox songs, the run damage type. */
    public static void declare() {
        if (declared) return;
        declared = true;

        Equipment.of(NS, "heavy_boots")
                .layer(Equipment.HUMANOID, NS + ":heavy_boots").register();

        // RBMK pellets: 1.7 ItemRBMKPellet drew the pellet with the enrichment overlay e0..e4 and the xenon overlay
        // (meta 5..9); HBM's model_state property is that 1.7 metadata, spelled as HBM's own pellets
        for (String pellet : RBMK_NAMES.keySet()) {
            ItemModels.Select select = ItemModels.select("hbm:model_state");
            for (int meta = 0; meta < 10; meta++) {
                int e = meta % 5;
                boolean xe = meta >= 5;
                String model = pellet + "_e" + e + (xe ? "_xe" : "");
                Dejson.item(NS, model).models(xe
                        ? Models.generated(NS + ":items/" + pellet, "hbm:items/rbmk_pellet_overlay_e" + e, "hbm:items/rbmk_pellet_overlay_xenon")
                        : Models.generated(NS + ":items/" + pellet, "hbm:items/rbmk_pellet_overlay_e" + e)).register();
                select.when(String.valueOf(meta), ItemModels.model(NS + ":item/" + model));
            }
            Dejson.item(NS, pellet).definition(select.fallback(ItemModels.model(NS + ":item/" + pellet + "_e0"))).register();
        }

        // the records' songs (1.7 ItemModRecord.getRecordResource), lengths off the shipped oggs
        JukeboxSongs.declare(NS, "gs", NS + ":music.record_god_speed", "jukebox_song.ntmspace.gs", 353.152F, 15);
        JukeboxSongs.declare(NS, "gp", NS + ":music.record_goop", "jukebox_song.ntmspace.gp", 240.789F, 15);
        JukeboxSongs.declare(NS, "el", NS + ":music.record_ethereal", "jukebox_song.ntmspace.el", 197.739F, 15);
        JukeboxSongs.discs(NS + ":record_gs", NS + ":record_gp", NS + ":record_el");

        // 1.7 ModDamageSource.run: setDamageIsAbsolute().setDamageBypassesArmor()
        // (the two vanilla tag files are whole-file declarations of AtmosphereContent's Damage.declare, so the membership
        // is contributed member by member)
        Damage.declare(NS, Damage.type("run"));
        dev.golem.core.dejson.Tags.member("damage_type", "minecraft", "bypasses_armor", NS + ":run");
        dev.golem.core.dejson.Tags.member("damage_type", "minecraft", "bypasses_effects", NS + ":run");
    }
}
