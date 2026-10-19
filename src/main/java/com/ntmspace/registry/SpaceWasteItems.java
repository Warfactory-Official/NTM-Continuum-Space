package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.item.ItemSpaceWaste;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;

/**
 * hbm-space's extra short-lived waste classes ({@code ItemWasteShort.WasteClass}): americium-241, berkelium-247,
 * curium-244 and curium-245, each in the four short families. Items {@code ntmspace:<family>_<class>} (16), drawn with
 * HBM's family sprites like HBM's own classes, in the control tab like 1.7's {@code nuclear_waste_short}. The tiny forms
 * run their full form's SILEX row (1.7 {@code tinyWasteTranslation}, HBM {@link SILEXRecipes#addTinyWaste}).
 */
public final class SpaceWasteItems {

    private static final String NS = NtmSpace.MOD_ID;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    private static final Registrar REG = Registrar.of(ITEMS);

    /** The four families, as HBM spells them (and their 1.7 sprite). */
    public static final String[] FAMILIES = {"nuclear_waste_short", "nuclear_waste_short_tiny", "nuclear_waste_short_depleted",
            "nuclear_waste_short_depleted_tiny"};

    /** hbm-space's classes after HBM's (id suffix -> 1.7 name), in 1.7 metadata order (7, 9, 10, 11). */
    public static final Map<String, String> CLASSES = new LinkedHashMap<>();

    static {
        CLASSES.put("americium241", "Americium-241");
        CLASSES.put("berkelium247", "Berkelium-247");
        CLASSES.put("curium244", "Curium-244");
        CLASSES.put("curium245", "Curium-245");
    }

    public static final List<RegistryObject<ItemSpaceWaste>> ALL = new ArrayList<>();

    static {
        for (String family : FAMILIES) {
            for (Map.Entry<String, String> type : CLASSES.entrySet()) {
                String name = type.getValue();
                ALL.add(REG.item(family + "_" + type.getKey(), (Item.Properties p) -> new ItemSpaceWaste(p, name), p -> p)
                        .transform(Presets.sprite("hbm:items/" + family)).tab(HbmTab.CONTROL));
            }
        }
    }

    private SpaceWasteItems() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        for (String type : CLASSES.keySet()) {
            SILEXRecipes.addTinyWaste(NS + ":nuclear_waste_short_tiny_" + type, NS + ":nuclear_waste_short_" + type);
            SILEXRecipes.addTinyWaste(NS + ":nuclear_waste_short_depleted_tiny_" + type, NS + ":nuclear_waste_short_depleted_" + type);
        }
    }
}
