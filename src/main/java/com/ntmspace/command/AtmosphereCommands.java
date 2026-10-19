package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ntmspace.NtmSpace;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.breathing.Breathing;
import com.ntmspace.tileentity.TileEntityAirPump;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.command.CompatCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator tools for the atmosphere (hbm-space's debug wand printed {@code CBT_Atmosphere} the same way):
 * {@code /ntmspace atmosphere} logs {@code atmosphere_check body=<b> oxygen=<n> breathable=<bool> pressure=<atm>
 * room=<bool> air=[...]} for the air at the player's eyes (the harness reads it), and {@code /ntmspace vent fill x y z}
 * fills an air vent's tank (test fixtures).
 */
public final class AtmosphereCommands {

    private AtmosphereCommands() {}

    public static void init() {
        CompatCommands.server(AtmosphereCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("atmosphere").executes(context -> atmosphere(context.getSource())))
                .then(Commands.literal("vent").then(Commands.literal("fill")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(AtmosphereCommands::fillVent)))))));
    }

    private static int atmosphere(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        CBT_Atmosphere air = AtmosphereHandler.getAtmosphere(player);
        StringBuilder fluids = new StringBuilder();
        if (air != null) {
            for (CBT_Atmosphere.FluidEntry entry : air.fluids) {
                if (fluids.length() > 0) fluids.append(',');
                fluids.append(entry.fluid.getRegistryName()).append('=').append(entry.pressure);
            }
        }
        String message = "atmosphere_check body=" + CelestialTraits.bodyOf(level) + " oxygen=" + Breathing.getOxy(player)
                + " breathable=" + AtmosphereHandler.canBreathe(air) + " pressure=" + (air == null ? 0 : air.getPressure())
                + " room=" + AtmosphereHandler.hasAtmosphere(level, eyes) + " air=[" + fluids + "]";
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int fillVent(CommandContext<CommandSourceStack> context) {
        BlockPos pos = new BlockPos(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "y"),
                IntegerArgumentType.getInteger(context, "z"));
        if (!(context.getSource().getLevel().getBlockEntity(pos) instanceof TileEntityAirPump vent)) {
            context.getSource().sendFailure(Component.literal("no air vent at " + pos.toShortString()));
            return 0;
        }
        vent.tank.setFill(vent.tank.getMaxFill());
        vent.setChanged();
        String message = "vent_fill " + pos.toShortString() + " " + vent.tank.getTankType().getRegistryName() + "=" + vent.tank.getFill();
        NtmSpace.LOG.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
