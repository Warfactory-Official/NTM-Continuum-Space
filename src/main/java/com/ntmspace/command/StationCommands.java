package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.NavDrives;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.SpaceTeleporter;
import com.ntmspace.orbit.StationData;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.command.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * hbm-space's {@code CommandStations} ({@code /ntmstations}) and {@code CommandTotalTime} ({@code /ntmtotaltime}):
 *
 * <ul>
 *   <li>{@code launch}: marks the held station drive's square as a launched station orbiting the body you stand on;</li>
 *   <li>{@code tp}: teleports to the held drive's destination (a station: its square's centre at y 130, placing the
 *       core; a body: its x/z, on the ground);</li>
 *   <li>{@code list}: the launched stations, {@code 0x<id> - <name>};</li>
 *   <li>{@code fetch <id|name>}: a processed drive for that station;</li>
 *   <li>{@code /ntmtotaltime set|add <ticks>}: the world's total time, which the solar system turns with.</li>
 * </ul>
 * The drive is the rocket port's item ({@link NavDrives}); until it exists, two port-only forms take the grid square
 * directly: {@code launch <x> <z> [body]} and {@code tp <x> <z>}. Operators only (1.7: permission level 2).
 */
public final class StationCommands {

    private StationCommands() {}

    public static void init() {
        CompatCommands.server(StationCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmstations")
                .requires(Permissions.gamemasters())
                .then(Commands.literal("launch")
                        .executes(context -> launchHeld(context))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> launch(context, null))
                                        .then(Commands.argument("body", StringArgumentType.word())
                                                .executes(context -> launch(context, StringArgumentType.getString(context, "body")))))))
                .then(Commands.literal("tp")
                        .executes(StationCommands::tpHeld)
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> tpStation(context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "z"))))))
                .then(Commands.literal("list").executes(StationCommands::list))
                .then(Commands.literal("fetch")
                        .executes(context -> message(context.getSource(), "commands.station.invalid_station", true))
                        .then(Commands.argument("station", StringArgumentType.greedyString())
                                .executes(context -> fetch(context, StringArgumentType.getString(context, "station"))))));

        dispatcher.register(Commands.literal("ntmtotaltime")
                .requires(Permissions.gamemasters())
                .then(Commands.literal("set").then(Commands.argument("time", IntegerArgumentType.integer(0))
                        .executes(context -> setTime(context, IntegerArgumentType.getInteger(context, "time"), false))))
                .then(Commands.literal("add").then(Commands.argument("time", IntegerArgumentType.integer(0))
                        .executes(context -> setTime(context, IntegerArgumentType.getInteger(context, "time"), true)))));
    }

    private static int message(CommandSourceStack source, String key, boolean isError) {
        Component message = Component.translatable(key).withStyle(isError ? ChatFormatting.RED : ChatFormatting.GREEN);
        if (isError) source.sendFailure(message);
        else source.sendSuccess(() -> message, false);
        return isError ? 0 : 1;
    }

    private static NavDrives.Destination heldDestination(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack held = source.getPlayerOrException().getMainHandItem();
        return NavDrives.destination(held);
    }

    private static int launchHeld(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NavDrives.Destination destination = heldDestination(context.getSource());
        if (destination == null) return message(context.getSource(), "commands.station.invalid_drive", true);
        ServerPlayer player = context.getSource().getPlayerOrException();
        OrbitalStation.addStation(context.getSource().getServer(), destination.x(), destination.z(), Orbit.bodyOf(player.level()));
        return message(context.getSource(), "commands.station.launched", false);
    }

    private static int launch(CommandContext<CommandSourceStack> context, String bodyName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SolarSystem.Body body = bodyName == null ? Orbit.bodyOf(player.level()) : SolarSystem.body(bodyName);
        if (body == null || !Orbit.isLandable(body)) {
            context.getSource().sendFailure(Component.literal("Unknown body: " + bodyName));
            return 0;
        }
        OrbitalStation.addStation(context.getSource().getServer(), IntegerArgumentType.getInteger(context, "x"),
                IntegerArgumentType.getInteger(context, "z"), body);
        return message(context.getSource(), "commands.station.launched", false);
    }

    private static int tpHeld(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NavDrives.Destination destination = heldDestination(context.getSource());
        if (destination == null) return message(context.getSource(), "commands.station.invalid_drive", true);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (destination.isOrbit()) return tpStation(player, destination.x(), destination.z());

        SolarSystem.Body body = SolarSystem.body(destination.body());
        ServerLevel level = context.getSource().getServer().getLevel(Orbit.levelOf(body));
        if (level == null) return 0;
        player.stopRiding();
        int x = destination.x(), z = destination.z();
        if (!player.level().dimension().equals(level.dimension())) {
            SpaceTeleporter.teleport(player, level.dimension(), x + 0.5D, 300.0D, z + 0.5D, true);
        } else {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            player.teleportTo(x + 0.5D, y + 1, z + 0.5D);
        }
        return message(context.getSource(), "commands.station.teleported", false);
    }

    /** 1.7 {@code tp} for a station: the square's centre, y 130, and the core placed under it. */
    private static int tpStation(ServerPlayer player, int gridX, int gridZ) {
        ServerLevel orbit = player.level().getServer().getLevel(Orbit.ORBIT);
        if (orbit == null) {
            player.sendSystemMessage(Component.literal("The orbit dimension is not loaded"));
            return 0;
        }
        int x = gridX * OrbitalStation.STATION_SIZE + (OrbitalStation.STATION_SIZE / 2);
        int z = gridZ * OrbitalStation.STATION_SIZE + (OrbitalStation.STATION_SIZE / 2);

        player.stopRiding();
        if (!player.level().dimension().equals(Orbit.ORBIT)) {
            SpaceTeleporter.teleport(player, Orbit.ORBIT, x + 0.5D, 130.0D, z + 0.5D, false);
        } else {
            player.teleportTo(x + 0.5D, 130.0D, z + 0.5D);
        }

        OrbitalStation.spawn(orbit, x, z);
        player.sendSystemMessage(Component.translatable("commands.station.teleported").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        StationData data = StationData.get(context.getSource().getServer());
        boolean hasAnyStations = false;
        for (OrbitalStation station : data.getStations()) {
            if (!station.hasStation) continue;
            String messageText = station.identifier();
            if (!station.name.isEmpty()) messageText += " - " + station.name;
            String text = messageText;
            context.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GREEN), false);
            NtmSpace.LOG.info("ntmstations list: {} grid {},{} orbiting {}", text, station.dX, station.dZ, station.orbiting.name);
            hasAnyStations = true;
        }
        if (!hasAnyStations) return message(context.getSource(), "commands.station.no_stations", true);
        return 1;
    }

    private static int fetch(CommandContext<CommandSourceStack> context, String toMatch) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        toMatch = toMatch.trim();
        for (OrbitalStation station : StationData.get(context.getSource().getServer()).getStations()) {
            if (station.name.trim().equalsIgnoreCase(toMatch) || station.identifier().equalsIgnoreCase(toMatch)) {
                ItemStack drive = NavDrives.FACTORIES.invoker().create(station.dX, station.dZ, station.name);
                if (drive.isEmpty()) {
                    context.getSource().sendFailure(Component.literal("No station drive item is registered"));
                    return 0;
                }
                player.getInventory().add(drive);
                return message(context.getSource(), "commands.station.drive_created", false);
            }
        }
        return message(context.getSource(), "commands.station.no_match", true);
    }

    /**
     * 1.7 {@code CommandTotalTime}: {@code incrementTotalWorldTime} is a setter despite its name, so "set" sets the
     * total time and "add" sets it to the current total plus the amount. The levels share the overworld's clock here.
     */
    private static int setTime(CommandContext<CommandSourceStack> context, int time, boolean add) {
        var data = context.getSource().getServer().getWorldData().overworldData();
        long total = add ? context.getSource().getServer().overworld().getGameTime() + time : time;
        data.setGameTime(total);
        String key = add ? "commands.time.added" : "commands.time.set";
        context.getSource().sendSuccess(() -> Component.translatable(key, time), true);
        return 1;
    }
}
