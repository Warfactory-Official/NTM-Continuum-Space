package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.ItemTag;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** Raw acquisition forms use their own tags, keeping all existing ore processing tags on the Silk Touch blocks. */
public final class RawItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ITEMS);

    public static final RegistryObject<Item> NICKEL = raw("nickel", "nickel_pure");
    public static final RegistryObject<Item> ZINC = raw("zinc", "zinc");
    public static final RegistryObject<Item> AUSTRALIUM = raw("australium", "australium");
    public static final RegistryObject<Item> NIOBIUM = raw("niobium", "niobium");
    public static final RegistryObject<Item> LANTHANIUM = raw("lanthanium", "lanthanum");

    private RawItems() {}

    private static RegistryObject<Item> raw(String id, String material) {
        return REG.item("raw_" + id, Item::new)
                .transform(Presets.sprite("ntmspace:items/raw_" + id))
                .tag(ItemTag.of("c:raw_materials/" + material))
                .tag(ItemTag.of("c:raw_materials"))
                .tab(HbmTab.MATERIALS);
    }

    public static void init() {}
}
