package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.ItemTag;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.item.Item;

/**
 * The items hbm-space's progression machines use and make (its {@code ModItems} values): the circuits it added to 1.7's
 * {@code ItemCircuit} enum (one item per type, ntm-next's split, id {@code circuit_<type>} as HBM's own circuits, drawn
 * with the 1.7 sprite {@code circuit.<type>}), the pentlandite chunk ({@code chunk_ore} type {@code PENTLANDITE}), the
 * zinc ingot, the mineral items and the zinc and niobium crystals. All in the parts tab, as 1.7.
 */
public final class SpaceMachineItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ITEMS);

    /** hbm-space's additions to {@code EnumCircuitType} (tab places follow 1.7's sub-item order). */
    public static final String[] CIRCUIT_TYPES = {
            "GAAS", "AVIONICS", "AERO", "CAPACITOR_LANTHANIUM", "PROCESST1", "PROCESST2", "PROCESST3", "GASCHIP", "HFCHIP", "MOLYCHIP"
    };

    private static final Map<String, RegistryObject<Item>> CIRCUITS = new LinkedHashMap<>();

    static {
        for (String type : CIRCUIT_TYPES) CIRCUITS.put(type, variant("circuit", type));
    }

    /** {@code chunk_ore} type {@code PENTLANDITE}: generic nickel dust and pentlandite crystal, as 1.7. */
    public static final RegistryObject<Item> CHUNK_ORE_PENTLANDITE =
            variant("chunk_ore", "PENTLANDITE", "c:dusts/nickel", "c:crystals/pentlandite");

    public static final RegistryObject<Item> INGOT_ZINC = plain("ingot_zinc", "ntmspace:items/ingot_zinc");
    /** 1.7 {@code ItemCustomLore}. */
    public static final RegistryObject<Item> CRYSTAL_MINERAL = plain("crystal_mineral", "ntmspace:items/crystal_mineral");
    /** 1.7 {@code ItemCustomLore}, texture {@code powder_mineral}. */
    public static final RegistryObject<Item> MINERAL_DUST = plain("mineral_dust", "ntmspace:items/powder_mineral");
    public static final RegistryObject<Item> CRYSTAL_ZINC = plain("crystal_zinc", "ntmspace:items/crystal_zinc");
    public static final RegistryObject<Item> CRYSTAL_NIOBIUM = plain("crystal_niobium", "ntmspace:items/crystal_niobium");

    private static RegistryObject<Item> variant(String base, String type) {
        String name = type.toLowerCase(Locale.US);
        return REG.item(base + "_" + name).transform(Presets.sprite("ntmspace:items/" + base + "." + name)).tab(HbmTab.MATERIALS);
    }

    private static RegistryObject<Item> variant(String base, String type, String... tags) {
        String name = type.toLowerCase(Locale.US);
        var item = REG.item(base + "_" + name)
                .transform(Presets.sprite("ntmspace:items/" + base + "." + name));
        for (String tag : tags) item.tag(ItemTag.of(tag));
        return item.tab(HbmTab.MATERIALS);
    }

    private static RegistryObject<Item> plain(String id, String texture) {
        return REG.item(id).transform(Presets.sprite(texture)).tab(HbmTab.MATERIALS);
    }

    /** One of hbm-space's circuits by its 1.7 enum name ({@code "PROCESST1"}). */
    public static Item circuit(String type) {
        RegistryObject<Item> item = CIRCUITS.get(type);
        if (item == null) throw new IllegalArgumentException("No hbm-space circuit " + type);
        return item.get();
    }

    private SpaceMachineItems() {}

    public static void init() {}
}
