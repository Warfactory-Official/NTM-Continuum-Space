package com.ntmspace.world;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.items.block.ItemBlockDecoVariant;
import dev.golem.core.world.FishingLoot;
import dev.golem.core.world.Ingredients;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * WorldProviderCelestial's dead-world fish and WorldProviderIke's Yomi catches, without changing other worlds' tables.
 */
public final class SpaceFishing {
    private SpaceFishing() {
    }

    public static void init() {
        FishingLoot.SELECTORS.register(SpaceFishing::select);
    }

    private static @Nullable ItemStack select(ServerLevel level, @Nullable BlockPos origin,
                                              FishingLoot.Category category,
                                              RandomSource random) {
        if (level.dimension().equals(Level.OVERWORLD) || CelestialBodies.of(level) == null) return null;
        if (level.dimension().equals(SpaceDimensions.IKE)) {
            return ItemBlockDecoVariant.stackOf(ModDecoCrateBlocks.plushie.get(),
                    BlockPlushie.PlushieType.YOMI.ordinal());
        }
        return category == FishingLoot.Category.FISH ? deadFish(random) : null;
    }

    /**
     * WorldProviderCelestial.getFish: weights 10/5/2/10/5/1/10, with ten ink sacs and a damaged rod.
     */
    private static ItemStack deadFish(RandomSource random) {
        var roll = random.nextInt(43);
        if (roll < 10) return Ingredients.potionStack("minecraft:potion", "minecraft:water");
        if (roll < 15) return new ItemStack(Items.STRING);
        if (roll < 17) {
            var rod = new ItemStack(Items.FISHING_ROD);
            var limit = (int) (0.9F * rod.getMaxDamage());
            var damage = rod.getMaxDamage() - random.nextInt(random.nextInt(limit) + 1);
            rod.setDamageValue(Math.max(1, Math.min(damage, limit)));
            return rod;
        }
        if (roll < 27) return new ItemStack(Items.BOWL);
        if (roll < 32) return new ItemStack(Items.STICK);
        if (roll < 33) return new ItemStack(Items.INK_SAC, 10);
        return new ItemStack(Items.TRIPWIRE_HOOK);
    }
}
