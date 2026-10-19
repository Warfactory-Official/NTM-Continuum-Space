package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.CloudLayers17;
import dev.golem.core.command.CompatCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Test commands for the bodies' clouds (operators only; the harness scenario {@code SkiesWeather} uses them):
 * <ul>
 *   <li>{@code /ntmspace clouds air <body> <atm> <fluid>}: the body's air becomes {@code atm} of {@code fluid} (a
 *       terraformed body; synced to clients like any trait change). Logs {@code space_clouds_air body=.. fluid=.. atm=..}.</li>
 *   <li>{@code /ntmspace clouds reset <body>}: the body's default air again. Logs {@code space_clouds_reset body=..}.</li>
 * </ul>
 */
public final class CloudCommands {

    private CloudCommands() {}

    public static void init() {
        CompatCommands.server(CloudCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("clouds")
                        .then(Commands.literal("air")
                                .then(Commands.argument("body", StringArgumentType.word())
                                        .then(Commands.argument("atm", DoubleArgumentType.doubleArg(0))
                                                .then(Commands.argument("fluid", StringArgumentType.greedyString())
                                                        .executes(CloudCommands::air)))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("body", StringArgumentType.word())
                                        .executes(CloudCommands::reset)))));
    }

    private static int air(CommandContext<CommandSourceStack> context) {
        String body = StringArgumentType.getString(context, "body");
        double atm = DoubleArgumentType.getDouble(context, "atm");
        String fluid = StringArgumentType.getString(context, "fluid").trim();
        com.hbm.inventory.fluid.FluidType type = com.hbm.inventory.fluid.Fluids.byRegistryName(fluid);
        if (type == com.hbm.inventory.fluid.Fluids.NONE) {
            context.getSource().sendFailure(Component.literal("unknown fluid " + fluid));
            return 0;
        }
        CelestialTraits.modifyTraits(body, new CBT_Atmosphere(type, atm));
        return report(context.getSource(), "space_clouds_air body=" + body + " fluid=" + type.getRegistryName() + " atm=" + atm
                + " layers=" + CloudLayers17.layerCount(java.util.List.of(new Atmospheres.Entry(type.getRegistryName(), atm))));
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        String body = StringArgumentType.getString(context, "body");
        CBT_Atmosphere air = CelestialTraits.getDefaultTrait(body, CBT_Atmosphere.class);
        if (air == null) {
            java.util.Map<Class<? extends com.ntmspace.trait.CelestialBodyTrait>, com.ntmspace.trait.CelestialBodyTrait> traits =
                    CelestialTraits.getTraits(body);
            traits.remove(CBT_Atmosphere.class);
            CelestialTraits.setTraits(body, traits);
        } else {
            CelestialTraits.modifyTraits(body, air.copy());
        }
        return report(context.getSource(), "space_clouds_reset body=" + body);
    }

    private static int report(CommandSourceStack source, String message) {
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
