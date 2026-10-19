package com.ntmspace.block.terrain;

import com.ntmspace.block.flora.FloraDrops;
import dev.golem.core.world.BlockBreaks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;

/**
 * hbm-space's {@code vinyl_vine} ("Vinyl Vines", {@code BlockVinylVine}): vanilla's vine with its own texture and no
 * foliage tint (its colour methods answer white). It keeps vanilla's vine rules: hbm-space's own
 * {@code canPlaceBlockOn(Block)} never overrode vanilla's {@code func_150093_a}, and its {@code canBlockStay} is not
 * consulted by vine placement or vine support, so the 1.7 block behaved exactly as a vine does. Like 1.7's vine it drops
 * nothing (its loot table) unless sheared, when it drops itself ({@code IShearable}; {@link #shearDrops}).
 */
public class BlockVinylVine extends VineBlock {

    public BlockVinylVine(Properties properties) {
        super(properties);
    }

    /** Shears drop the vine (Golem's break notification: vanilla's {@code playerWillDestroy} changed shape across eras). */
    public static void shearDrops(java.util.function.Supplier<? extends BlockVinylVine> vine) {
        BlockBreaks.onBreak((player, level, pos, state) -> {
            if (state.is(vine.get()) && FloraDrops.shearing(player)) {
                Block.popResource(level, pos, new ItemStack(vine.get()));
            }
        });
    }
}
