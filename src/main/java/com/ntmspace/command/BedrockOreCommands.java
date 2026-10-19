package com.ntmspace.command;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.mojang.brigadier.CommandDispatcher;
import com.ntmspace.NtmSpace;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.command.Permissions;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Port-only test commands for the per-body bedrock ores (harness scenario {@code BedrockSatellites}), operators only:
 * {@code /ntmbedrock sample} gives the raw bedrock ore the excavator would collect in the player's column (HBM's
 * {@code ItemBedrockOreBase.setOreAmount(level, ...)}, drill fortune 0) and logs {@code bedrock_sample <dimension>
 * <origin> <type>=<amount> ...}; {@code /ntmbedrock scan} logs the column's field ({@code bedrock_field ...}) as the ore
 * density scanner reads it.
 */
public final class BedrockOreCommands {

    private BedrockOreCommands() {}

    public static void init() {
        CompatCommands.server(BedrockOreCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmbedrock")
                .requires(Permissions.gamemasters())
                .then(Commands.literal("sample").executes(context -> sample(context.getSource())))
                .then(Commands.literal("scan").executes(context -> scan(context.getSource()))));
    }

    private static int sample(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        ServerLevel level = source.getLevel();
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        ItemStack stack = new ItemStack(ModOreBlocks.bedrock_ore_base.get());
        ItemBedrockOreBase.setOreAmount(level, stack, x, z, 1D);
        StringBuilder line = new StringBuilder("bedrock_sample ").append(dev.golem.core.Ids.keyId(level.dimension()))
                .append(' ').append(ItemBedrockOreBase.COMPOSITION.get(stack).map(ItemBedrockOreBase.Composition::origin)
                        .orElse("-"));
        for (BedrockOreType type : ItemBedrockOreBase.types(stack)) {
            line.append(' ').append(type.name()).append('=').append(ItemBedrockOreBase.getOreAmount(stack, type));
        }
        NtmSpace.LOG.info(line.toString());
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        source.sendSuccess(() -> Component.literal(line.toString()), false);
        return 1;
    }

    private static int scan(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        ServerLevel level = source.getLevel();
        ItemBedrockOreBase.OreField field = ItemBedrockOreBase.field(level, (int) Math.floor(player.getX()),
                (int) Math.floor(player.getZ()));
        StringBuilder line = new StringBuilder("bedrock_field ").append(dev.golem.core.Ids.keyId(level.dimension()))
                .append(' ').append(field.origin()).append(" average=").append(field.average())
                .append(" fluid=").append(field.boreFluid() == null ? "-"
                        : field.boreFluid().type.getName() + "x" + field.boreFluid().fill);
        for (Map.Entry<BedrockOreType, Double> entry : field.levels().entrySet()) {
            line.append(' ').append(entry.getKey().name()).append('=').append(entry.getValue());
        }
        NtmSpace.LOG.info(line.toString());
        source.sendSuccess(() -> Component.literal(line.toString()), false);
        return 1;
    }
}
