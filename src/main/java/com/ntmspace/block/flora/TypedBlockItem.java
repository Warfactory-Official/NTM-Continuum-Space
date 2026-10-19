package com.ntmspace.block.flora;

import dev.golem.core.world.CompatBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * One type's block item of a typed flora block ({@link TypedItems}): it places that type. 1.7 kept the type in the
 * item's damage; most families named every type alike ({@code tile.<block>.name}), the saplings per type
 * ({@code nameKey}).
 */
public final class TypedBlockItem extends CompatBlockItem {

    private final String value;
    private final @Nullable String nameKey;
    private final boolean mustSurvive;

    public TypedBlockItem(Block block, Properties properties, String value, @Nullable String nameKey, boolean mustSurvive) {
        super(block, properties);
        this.value = value;
        this.nameKey = nameKey;
        this.mustSurvive = mustSurvive;
    }

    /** The type's serialized state value. */
    public String value() {
        return value;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(nameKey != null ? nameKey : getBlock().getDescriptionId());
    }

    /**
     * Whether placement asks {@code canSurvive}. The saplings place where 1.7's {@code canPlaceBlockOnSide} allowed
     * (their {@code getStateForPlacement} says so), which is more than their {@code canBlockStay} keeps.
     */
    @Override
    protected boolean mustSurvive() {
        return mustSurvive;
    }
}
