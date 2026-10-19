package com.ntmspace.bedrock;

import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * One flattened variant of hbm-space's {@code ItemBedrockOreNew} ({@code bedrock_ore}, metadata
 * {@code grade << 8 | type.index}): {@code ntmspace:bedrock_ore_<grade>_<body>_<suffix>}. HBM's graded item with
 * hbm-space's name (the type and, in brackets, the body it was mined on) and tooltip (the traits, and for the BASE grade
 * the four processing outputs).
 */
public class ItemCelestialBedrockOre extends ItemBedrockOreGraded {

    public final String body;

    public ItemCelestialBedrockOre(Properties properties, Grade grade, BedrockOreType type, String body) {
        super(properties, grade, type);
        this.body = body;
    }

    /** 1.7: {@code grade.<grade>.name} formatted with {@code type.<suffix>.name}, then {@code " (" + body + ")"}. */
    @Override
    public Component getName(ItemStack stack) {
        Component typeName = Component.translatableWithFallback("item.bedrock_ore.type." + type.suffix + ".name",
                type.displayName);
        return Component.translatableWithFallback("item.bedrock_ore.grade." + grade.name().toLowerCase(Locale.ROOT) + ".name",
                        "%s Bedrock Ore", typeName)
                .append(" (").append(Component.translatable("body." + body)).append(")");
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);
        if (grade == Grade.BASE) {
            tooltip.add(Component.literal("Processing outputs").withStyle(ChatFormatting.DARK_GRAY));
            line(tooltip, "Main: ", type.primary1);
            line(tooltip, "Sulfuric: ", type.byproductAcid1);
            line(tooltip, "Solvent: ", type.byproductSolvent1);
            line(tooltip, "HPS: ", type.byproductRad1);
        }
    }

    private static void line(List<Component> tooltip, String label, BedrockOreType.BedrockOreOutput output) {
        tooltip.add(Component.literal(label).withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(output.material().getDisplayName()).withStyle(ChatFormatting.GRAY)));
    }
}
