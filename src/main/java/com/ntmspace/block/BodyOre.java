package com.ntmspace.block;

import com.ntmspace.block.BlockBodyOre.Body;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A per-body ore block ({@link BlockBodyOre}, {@link BlockBodyOreOutgas}): what its states share. Each body state has its
 * own block item ({@link BodyOreItem}, registered by {@code registry/BodyOreBlocks}); the Kerbin state's is the block's
 * own id and is its {@code asItem()}.
 */
public interface BodyOre {

    /** The block items per body of each per-body ore block, by block id (filled at registration). */
    Map<String, Map<Body, Supplier<? extends Item>>> ITEMS = new HashMap<>();

    static void register(String blockId, Body body, Supplier<? extends Item> item) {
        ITEMS.computeIfAbsent(blockId, b -> new EnumMap<>(Body.class)).put(body, item);
    }

    /** The item of {@code state}'s body; the block's own item if that body has none. */
    static Item item(Block block, Body body) {
        Map<Body, Supplier<? extends Item>> items = ITEMS.get(BuiltInRegistries.BLOCK.getKey(block).toString());
        Supplier<? extends Item> item = items == null ? null : items.get(body);
        return item != null ? item.get() : block.asItem();
    }

    static Item item(BlockState state) {
        var block = state.getBlock();
        return block instanceof BodyOre ? item(block, state.getValue(BlockBodyOre.BODY)) : block.asItem();
    }

    static ItemStack stack(Block block, BlockState state) {
        return new ItemStack(item(block, state.getValue(BlockBodyOre.BODY)));
    }

    /** The state an item places: its body's ({@link BodyOreItem}), Kerbin's for the block's own item. */
    static BlockState placement(Block block, BlockPlaceContext context) {
        Body body = context.getItemInHand().getItem() instanceof BodyOreItem item ? item.body() : Body.KERBIN;
        return block.defaultBlockState().setValue(BlockBodyOre.BODY, body);
    }

    /** The loot table's drops with the block's own item (the Kerbin form) made the broken state's item. */
    static List<ItemStack> drops(Block block, BlockState state, List<ItemStack> drops) {
        Item own = block.asItem();
        Item body = item(block, state.getValue(BlockBodyOre.BODY));
        if (own == body) return drops;
        List<ItemStack> out = new ArrayList<>(drops.size());
        for (ItemStack stack : drops) {
            out.add(stack.is(own) ? new ItemStack(body, stack.getCount()) : stack);
        }
        return out;
    }
}
