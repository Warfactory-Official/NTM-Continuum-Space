package com.ntmspace.atmosphere;

import com.hbm.items.armor.ArmorDNT;
import com.hbm.items.armor.ItemModInsert;
import com.ntmspace.breathing.Breathing;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Atmosphere capabilities shown by the same tags and armor classes that govern real breathing and corrosion. */
public final class AtmosphereItemLore {
    private AtmosphereItemLore() {}

    public static void armor(ItemStack stack, Consumer<Component> adder) {
        if (stack.getItem() instanceof ArmorDNT) {
            adder.accept(Component.translatable("armor.canBreathe").withStyle(ChatFormatting.BLUE));
        } else if (Breathing.canSeal(stack)) {
            adder.accept(Component.translatable("armor.canSeal").withStyle(ChatFormatting.BLUE));
        }
    }

    public static void insert(ItemStack stack, Consumer<Component> adder) {
        if (stack.getItem() instanceof ItemModInsert && Breathing.isCorrosionProtection(stack)) {
            adder.accept(Component.translatable("desc.ntmspace.insert.corrosionProtection").withStyle(ChatFormatting.GOLD));
        }
    }
}
