package com.ntmspace.block;

import com.ntmspace.block.BlockBodyOre.Body;
import dev.golem.core.world.CompatBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * One block item per body state of a per-body ore ({@link BodyOre}): it places its body's state. 1.7 kept the body in the
 * item's damage and named every body's item alike ({@code tile.<ore>.name}); the name here is the block's.
 */
public final class BodyOreItem extends CompatBlockItem {

    private final Body body;

    public BodyOreItem(Block block, Properties properties, Body body) {
        super(block, properties);
        this.body = body;
    }

    public Body body() {
        return body;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getBlock().getDescriptionId());
    }
}
