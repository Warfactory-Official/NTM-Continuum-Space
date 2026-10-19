package com.ntmspace.block.flora;

import com.ntmspace.registry.FloraBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;

/** The drop rules NTM: Space's flora share with each other. */
public final class FloraDrops {

    private FloraDrops() {}

    /** The level's random, which 1.7's {@code quantityDropped}/{@code getDrops} drew from ({@code world.rand}). */
    public static RandomSource random(LootParams.Builder params) {
        return params.getLevel().getRandom();
    }

    /**
     * The Laythe water plants' {@code getItemDropped = saltleaf} with {@code quantityDropped = random.nextInt(4)}:
     * zero to three saltleaves, no fortune ({@code BlockWaterPlant}, {@code BlockKelp}, {@code BlockTallPlantWater}).
     */
    public static List<ItemStack> saltleaf(RandomSource random) {
        int count = random.nextInt(4);
        List<ItemStack> drops = new ArrayList<>(1);
        if (count > 0) drops.add(new ItemStack(FloraBlocks.SALTLEAF.get(), count));
        return drops;
    }

    /** Whether a player breaks with shears in the main hand, not in creative (1.7 {@code IShearable} via {@code ItemShears}). */
    public static boolean shearing(Player player) {
        return !player.isCreative() && player.getMainHandItem().is(Items.SHEARS);
    }
}
