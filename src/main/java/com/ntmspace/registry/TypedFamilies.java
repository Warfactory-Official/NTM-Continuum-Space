package com.ntmspace.registry;

import com.ntmspace.NtmSpace;
import com.ntmspace.block.flora.TypedBlockItem;
import com.ntmspace.block.flora.TypedItems;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.TabTarget;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

/**
 * Registers the block items of a typed flora family ({@link TypedItems}): {@code <block>_<type>} for every type but the
 * first, then {@code <block>} for the first, last, so the block's own item ({@code asItem}, the one its declaration's
 * item model is written for) is the first type's on every loader.
 */
final class TypedFamilies {

    private TypedFamilies() {}

    /**
     * @param nameKey     a type's item name key, or null for the block's name
     * @param mustSurvive whether placing asks {@code canSurvive} (not for the saplings, see {@link TypedBlockItem})
     * @param icon        the item model of a type other than the first (the first's comes with the block's declaration)
     */
    static <E extends Enum<E> & StringRepresentable> List<RegistryObject<TypedBlockItem>> items(
            Registrar reg, RegistryObject<? extends Block> block, String path, E[] types,
            Function<String, String> nameKey, boolean mustSurvive,
            Function<String, UnaryOperator<Dejson.ItemDeclaration>> icon, TabTarget tab) {
        java.util.Map<E, Registrar.Handle<TypedBlockItem>> byType = new java.util.EnumMap<>(types[0].getDeclaringClass());
        List<E> order = new ArrayList<>(List.of(types));
        E first = order.remove(0);
        order.add(first);
        for (E type : order) {
            String value = type.getSerializedName();
            String id = type == first ? path : path + "_" + value;
            String key = nameKey == null ? null : nameKey.apply(value);
            Registrar.Handle<TypedBlockItem> item = reg.item(id,
                    p -> new TypedBlockItem(block.get(), p, value, key, mustSurvive), Compat::blockDescription);
            if (type != first) item.icon(icon.apply(value));
            TypedItems.register(NtmSpace.MOD_ID + ":" + path, value, item);
            byType.put(type, item);
        }
        // the creative tab in 1.7's metadata order
        List<RegistryObject<TypedBlockItem>> items = new ArrayList<>();
        for (E type : types) {
            byType.get(type).tab(tab);
            items.add(byType.get(type));
        }
        return items;
    }
}
