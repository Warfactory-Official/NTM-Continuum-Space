package com.ntmspace.fork.neutron;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ntmspace.NtmSpace;
import com.ntmspace.config.SpaceConfig;
import dev.golem.core.command.CompatCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /ntmspace neutron on|off|check} (operators): flips {@link SpaceConfig#disableNeutron} for the running game
 * (not written to {@code ntmspace.cfg}) and prints the caller's activation and every activated stack it carries, as a
 * log line for the harness. hbm-space had no such command (it was a config restart); this is a test aid.
 */
final class NeutronCommands {

    private NeutronCommands() {}

    static void init() {
        CompatCommands.server(NeutronCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("neutron")
                        .then(Commands.literal("on").executes(c -> set(c, false)))
                        .then(Commands.literal("off").executes(c -> set(c, true)))
                        .then(Commands.literal("check").executes(NeutronCommands::check))));
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean disable) {
        SpaceConfig.disableNeutron = disable;
        return report(context.getSource(), "space_neutron enabled=" + NeutronActivation.enabled());
    }

    private static int check(CommandContext<CommandSourceStack> context) {
        StringBuilder line = new StringBuilder("space_neutron_check enabled=").append(NeutronActivation.enabled());
        if (context.getSource().getEntity() instanceof Player player) {
            line.append(" activation=").append(NeutronActivation.getActivation(player));
            NeutronActivation.forInventory(player, stack -> {
                if (stack.isEmpty()) return;
                float a = NeutronActivation.stackActivation(stack);
                line.append(' ').append(id(stack)).append('x').append(stack.getCount()).append('=').append(a);
            });
        }
        return report(context.getSource(), line.toString());
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static int report(CommandSourceStack source, String message) {
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
