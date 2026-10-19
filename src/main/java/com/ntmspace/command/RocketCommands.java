package com.ntmspace.command;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.command.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Port-only test conveniences for the rocket family (no 1.7 counterpart), operators only, used by the harness scenario
 * {@code RocketFlight}:
 * <ul>
 *   <li>{@code /ntmrocket place vab|pad <x> <y> <z>}: the VAB or launch pad assembled at that core, facing north;</li>
 *   <li>{@code /ntmrocket fuel <x> <y> <z>}: fills the pad's typed tanks, solid fuel and power;</li>
 *   <li>{@code /ntmrocket drive <body> <x> <z>}: a processed navigation drive to a body ({@code orbit}: a station
 *       square).</li>
 * </ul>
 */
public final class RocketCommands {

    private RocketCommands() {}

    public static void init() {
        CompatCommands.server(RocketCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmrocket")
                .requires(Permissions.gamemasters())
                .then(Commands.literal("place")
                        .then(Commands.argument("machine", StringArgumentType.word())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(RocketCommands::place))))))
                .then(Commands.literal("fuel")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(RocketCommands::fuel)))))
                .then(Commands.literal("drive")
                        .then(Commands.argument("body", StringArgumentType.word())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(RocketCommands::drive))))));
    }

    private static BlockPos pos(CommandContext<CommandSourceStack> context) {
        return new BlockPos(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "y"),
                IntegerArgumentType.getInteger(context, "z"));
    }

    private static int place(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        String machine = StringArgumentType.getString(context, "machine");
        BlockPos core = pos(context);
        switch (machine) {
            case "vab" -> MultiblockHandlerXR.placeAssembled(level, core, RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get(), Direction.NORTH);
            case "pad" -> MultiblockHandlerXR.placeAssembled(level, core, RocketBlocks.LAUNCH_PAD_ROCKET.get(), Direction.NORTH);
            default -> {
                context.getSource().sendFailure(Component.literal("vab or pad"));
                return 0;
            }
        }
        NtmSpace.LOG.info("ntmrocket placed {} at {}", machine, core.toShortString());
        context.getSource().sendSuccess(() -> Component.literal("Placed " + machine), false);
        return 1;
    }

    private static int fuel(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getLevel().getBlockEntity(pos(context)) instanceof TileEntityLaunchPadRocket pad)) {
            context.getSource().sendFailure(Component.literal("no rocket pad core there"));
            return 0;
        }
        for (FluidTankNTM tank : pad.tanks) {
            if (tank.getTankType() != Fluids.NONE) tank.setFill(tank.getMaxFill());
        }
        pad.solidFuel.level = pad.solidFuel.max;
        pad.power = pad.maxPower;
        pad.setChanged();
        NtmSpace.LOG.info("ntmrocket fuelled pad at {}", pad.getBlockPos().toShortString());
        context.getSource().sendSuccess(() -> Component.literal("Fuelled"), false);
        return 1;
    }

    private static int drive(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DriveBody body = DriveBody.byName(StringArgumentType.getString(context, "body"));
        if (body == null) {
            context.getSource().sendFailure(Component.literal("unknown body"));
            return 0;
        }
        ItemStack drive = new ItemStack(RocketItems.drive(body));
        RocketItems.DRIVE.set(drive, DriveData.EMPTY.withCoordinates(IntegerArgumentType.getInteger(context, "x"),
                IntegerArgumentType.getInteger(context, "z")).withProcessed(true));
        if (!player.getInventory().add(drive)) player.drop(drive, false);
        context.getSource().sendSuccess(() -> Component.literal("Drive to " + body.key()), false);
        return 1;
    }
}
