package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.ntmspace.NtmSpace;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.world.TimeOfDay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * {@code /ntmspace time [ticks]}: reads (or sets) the time of day of the level the command runs in, a body's own clock
 * (Golem {@code TimeOfDay}). Logs {@code space_time dim=<id> time=<ticks> own=<bool>} for the harness. Operators only.
 *
 * <p>1.7's {@code /time set} and {@code /time add} reached every body's clock too ({@code CommandTime} looped over all
 * worlds' providers); vanilla's {@code /time} now changes vanilla's clock only, so a body's is set here.
 */
public final class SpaceTimeCommand {

    private SpaceTimeCommand() {}

    public static void init() {
        CompatCommands.server(SpaceTimeCommand::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("time")
                        .executes(context -> report(context.getSource()))
                        .then(Commands.argument("ticks", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    TimeOfDay.set(context.getSource().getLevel(), LongArgumentType.getLong(context, "ticks"));
                                    return report(context.getSource());
                                }))));
    }

    private static int report(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        String message = "space_time dim=" + dev.golem.core.Ids.keyId(level.dimension()) + " time=" + TimeOfDay.get(level)
                + " own=" + TimeOfDay.hasOwnClock(level);
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
