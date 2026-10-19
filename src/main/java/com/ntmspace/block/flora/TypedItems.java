package com.ntmspace.block.flora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The 1.7 metadata families of NTM: Space's flora ({@code BlockEnumMulti}, {@code BlockNTSapling}) as one block with a
 * named {@code type} state and one block item per type (HBM's rule for 1.7 block families), the way
 * {@code registry/BodyOreBlocks} does it per body: {@code ntmspace:<block>} is the first type's item (the block's own
 * {@code asItem}), {@code ntmspace:<block>_<type>} the others' ({@link TypedBlockItem}). An item places its type; a
 * broken block drops, and pick-block gives, its type's item.
 */
public final class TypedItems {

    /** The block items per type value of each typed block, by block id (filled at registration). */
    private static final Map<String, Map<String, Supplier<? extends Item>>> ITEMS = new HashMap<>();

    private TypedItems() {}

    public static void register(String blockId, String value, Supplier<? extends Item> item) {
        ITEMS.computeIfAbsent(blockId, b -> new HashMap<>()).put(value, item);
    }

    /** The item of {@code value}; the block's own item if that type has none. */
    public static Item item(Block block, String value) {
        Map<String, Supplier<? extends Item>> items = ITEMS.get(BuiltInRegistries.BLOCK.getKey(block).toString());
        Supplier<? extends Item> item = items == null ? null : items.get(value);
        return item != null ? item.get() : block.asItem();
    }

    public static <E extends Enum<E> & StringRepresentable> ItemStack stack(BlockState state, EnumProperty<E> property) {
        return new ItemStack(item(state.getBlock(), state.getValue(property).getSerializedName()));
    }

    /** The state an item places: its type's ({@link TypedBlockItem}), the default type for the block's own item. */
    public static <E extends Enum<E> & StringRepresentable> BlockState placement(Block block, EnumProperty<E> property,
                                                                                 BlockPlaceContext context) {
        BlockState state = block.defaultBlockState();
        if (context.getItemInHand().getItem() instanceof TypedBlockItem typed) {
            return property.getValue(typed.value()).map(v -> state.setValue(property, v)).orElse(state);
        }
        return state;
    }

    /** The drops with the block's own item (the first type's) made the broken state's type item, as 1.7's damageDropped. */
    public static <E extends Enum<E> & StringRepresentable> List<ItemStack> drops(BlockState state, EnumProperty<E> property,
                                                                                 List<ItemStack> drops) {
        Item own = state.getBlock().asItem();
        Item typed = item(state.getBlock(), state.getValue(property).getSerializedName());
        if (own == typed) return drops;
        List<ItemStack> out = new ArrayList<>(drops.size());
        for (ItemStack stack : drops) {
            out.add(stack.is(own) ? new ItemStack(typed, stack.getCount()) : stack);
        }
        return out;
    }
}
