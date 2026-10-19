package com.ntmspace.item;

import com.hbm.items.special.ItemNuclearWaste;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * One of hbm-space's four extra short-lived storage-drum waste classes ({@code ItemWasteShort.WasteClass} AMERICIUM241,
 * BERKELIUM247, CURIUM244, CURIUM245; 1.7 metadata 7, 9, 10, 11) in one of the four short families
 * ({@code nuclear_waste_short}, {@code _tiny}, {@code _depleted}, {@code _depleted_tiny}). HBM's own classes are
 * {@code ItemWasteStorage} items indexed into a closed enum, so the addon's are their own items, named as HBM names its
 * split ({@code <family>_<class>}); the dropped item keeps HBM's waste invariants (no despawn, no damage) through
 * {@link ItemNuclearWaste}. Decay, fuel, hazards, SILEX and the 1-to-9 recipes are data (tools/gen_space_waste.py).
 */
public class ItemSpaceWaste extends ItemNuclearWaste {

    private final String className;

    public ItemSpaceWaste(Properties properties, String className) {
        super(properties);
        this.className = className;
    }

    /** The waste class's 1.7 name, e.g. "Americium-241" (the italic tooltip line). */
    public String className() {
        return className;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal(className).withStyle(ChatFormatting.ITALIC));
    }
}
