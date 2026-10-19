package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CBT_Destroyed;
import com.ntmspace.trait.CBT_Impact;
import com.ntmspace.trait.CBT_Lights;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.NukeShocks;
import dev.golem.core.command.CompatCommands;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Test commands for the shader-drawn sky layers (operators only; the harness scenario {@code SkiesWeather} uses them).
 * Each sets what 1.7 gameplay sets, on any body, and logs {@code space_skytrait body=.. trait=..}:
 * <ul>
 *   <li>{@code /ntmspace skytrait lights <body> <lights>}: the body's {@code CBT_Lights} sum (above 400 / 2,000 / 10,000:
 *       city light textures 1 / 2 / 3).</li>
 *   <li>{@code /ntmspace skytrait impact <body>}: a {@code CBT_Impact} now (Tom's impact).</li>
 *   <li>{@code /ntmspace skytrait destroyed <body>}: {@code CBT_Destroyed} (a destroyed Kerbol is the black hole).</li>
 *   <li>{@code /ntmspace skytrait infected <body>}: {@code CBT_COMPROMISED} (the flesh sky).</li>
 *   <li>{@code /ntmspace skytrait nuke <body> <radius>}: a nuclear blast of that radius on the body at the caller's x / z
 *       ({@code CelestialNukeShockHandler.trigger}).</li>
 *   <li>{@code /ntmspace skytrait battle <body>}: {@code CBT_BATTLEFIELD} (Thatmo's battle sky and strikes; the Thatmo level
 *       reads Kerbin's traits, as in 1.7, so it is Kerbin's trait that shows there).</li>
 *   <li>{@code /ntmspace skytrait clear <body> <trait>}: removes the trait (1.7 id: lights, impact, destroyed, infected,
 *       battle).</li>
 * </ul>
 */
public final class SkyTraitCommands {

    private SkyTraitCommands() {}

    public static void init() {
        CompatCommands.server(SkyTraitCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("skytrait")
                        .then(Commands.literal("lights").then(body().then(Commands.argument("lights", IntegerArgumentType.integer(0))
                                .executes(c -> set(c.getSource(), body(c), "lights",
                                        new CBT_Lights(IntegerArgumentType.getInteger(c, "lights")))))))
                        .then(Commands.literal("impact").then(body().executes(c -> set(c.getSource(), body(c), "impact",
                                new CBT_Impact(c.getSource().getLevel().getGameTime())))))
                        .then(Commands.literal("destroyed").then(body().executes(c -> set(c.getSource(), body(c), "destroyed",
                                new CBT_Destroyed()))))
                        .then(Commands.literal("infected").then(body().executes(c -> set(c.getSource(), body(c), "infected",
                                new CelestialBodyTrait.CBT_Compromised()))))
                        .then(Commands.literal("battle").then(body().executes(c -> set(c.getSource(), body(c), "battle",
                                new CelestialBodyTrait.CBT_Battlefield()))))
                        .then(Commands.literal("nuke").then(body().then(Commands.argument("radius", FloatArgumentType.floatArg(0))
                                .executes(c -> {
                                    String body = body(c);
                                    float radius = FloatArgumentType.getFloat(c, "radius");
                                    var position = c.getSource().getPosition();
                                    NukeShocks.trigger(body, c.getSource().getServer(), c.getSource().getLevel().getGameTime(),
                                            position.x, position.z, radius);
                                    return report(c.getSource(), "space_skytrait body=" + body + " trait=nuke radius=" + radius);
                                }))))
                        .then(Commands.literal("clear").then(body().then(Commands.argument("trait", StringArgumentType.word())
                                .executes(c -> clear(c.getSource(), body(c), StringArgumentType.getString(c, "trait"))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> body() {
        return Commands.argument("body", StringArgumentType.word());
    }

    private static String body(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "body");
    }

    private static int set(CommandSourceStack source, String body, String id, CelestialBodyTrait trait) {
        CelestialTraits.modifyTraits(body, trait);
        return report(source, "space_skytrait body=" + body + " trait=" + id);
    }

    private static int clear(CommandSourceStack source, String body, String id) {
        CelestialBodyTrait.Type<?> type = CelestialBodyTrait.byId(id);
        if (type == null) {
            source.sendFailure(Component.literal("unknown trait " + id));
            return 0;
        }
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = CelestialTraits.getTraits(body);
        traits.remove(type.type);
        CelestialTraits.setTraits(body, traits);
        return report(source, "space_skytrait body=" + body + " trait=" + id + " cleared");
    }

    private static int report(CommandSourceStack source, String message) {
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
