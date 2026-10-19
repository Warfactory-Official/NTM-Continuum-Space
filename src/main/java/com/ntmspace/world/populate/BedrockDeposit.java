package com.ntmspace.world.populate;

import com.hbm.inventory.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** What hbm-space's {@code BedrockOre.generate} wrote into each {@code ore_bedrock} tile entity it placed. */
public record BedrockDeposit(ItemStack resource, @Nullable FluidStack acid, int color, int tier) {}
