package com.ntmspace.despaceimport;

import com.hbm.config.StructureConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * DE: Space's {@code hbmspace:wand_loot_scaled} rolls for its served chest tables: the drawn count times the structure
 * loot factor, rounded down. Unlike DE's own scaled rolls it may come to zero.
 */
record WandLootScaled(NumberProvider value) implements NumberProvider {
    static final MapCodec<WandLootScaled> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NumberProviders.CODEC.fieldOf("value").forGetter(WandLootScaled::value)).apply(i, WandLootScaled::new));
    static final LootNumberProviderType TYPE = new LootNumberProviderType(CODEC);

    @Override
    public LootNumberProviderType getType() {
        return TYPE;
    }

    @Override
    public float getFloat(LootContext context) {
        return getInt(context);
    }

    @Override
    public int getInt(LootContext context) {
        return (int) Math.floor(value.getInt(context) * StructureConfig.lootAmountFactor);
    }
}
