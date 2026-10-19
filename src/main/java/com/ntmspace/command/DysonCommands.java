package com.ntmspace.command;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterTU;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.dyson.TileEntityDysonReceiver;
import com.ntmspace.trait.CBT_Dyson;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.command.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port-only test conveniences for the Dyson swarm (no 1.7 counterpart), operators only, used by the harness scenario
 * {@code DysonSwarm}. Each logs one {@code dyson_*} line:
 * <ul>
 *   <li>{@code /ntmdyson swarm <id> <members>}: sets the star's swarm {@code id} to that many members;</li>
 *   <li>{@code /ntmdyson count <id>}: logs {@code dyson_swarm id=<id> members=<n> total=<all swarms>};</li>
 *   <li>{@code /ntmdyson place launcher|receiver|he|tu|anatmogenesis <x> <y> <z> <north|east|south|west>}: the machine
 *       assembled at that core;</li>
 *   <li>{@code /ntmdyson chip <x> <y> <z> <freq>}: a satellite ID-chip on {@code freq} into the launcher or receiver whose
 *       part is at x y z;</li>
 *   <li>{@code /ntmdyson charge <x> <y> <z>}: fills the launcher's buffer (20 MHE) and, if empty, its member slot (four
 *       swarm members) - a network's job, run every tick by the scenario while the launcher spins up;</li>
 *   <li>{@code /ntmdyson state <x> <y> <z>}: logs the machine's state ({@code dyson_state ...}).</li>
 * </ul>
 */
public final class DysonCommands {

    private DysonCommands() {}

    public static void init() {
        CompatCommands.server(DysonCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmdyson")
                .requires(Permissions.gamemasters())
                .then(Commands.literal("swarm")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.argument("members", IntegerArgumentType.integer(0))
                                        .executes(DysonCommands::swarm))))
                .then(Commands.literal("count")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(DysonCommands::count)))
                .then(Commands.literal("place")
                        .then(Commands.argument("machine", StringArgumentType.word())
                                .then(xyz(Commands.argument("facing", StringArgumentType.word()).executes(DysonCommands::place)))))
                .then(Commands.literal("chip")
                        .then(xyz(Commands.argument("freq", IntegerArgumentType.integer(0)).executes(DysonCommands::chip))))
                .then(Commands.literal("charge")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer()).executes(DysonCommands::charge)))))
                .then(Commands.literal("state")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer()).executes(DysonCommands::state))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> xyz(
            com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> last) {
        return Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer()).then(last)));
    }

    private static BlockPos pos(CommandContext<CommandSourceStack> context) {
        return new BlockPos(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "y"),
                IntegerArgumentType.getInteger(context, "z"));
    }

    private static void log(CommandContext<CommandSourceStack> context, String line) {
        NtmSpace.LOG.info(line);
        context.getSource().sendSuccess(() -> Component.literal(line), false);
    }

    private static int swarm(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int id = IntegerArgumentType.getInteger(context, "id");
        int members = IntegerArgumentType.getInteger(context, "members");
        CBT_Dyson.set(level, id, members);
        log(context, "dyson_swarm_set id=" + id + " members=" + CBT_Dyson.count(level, id));
        return 1;
    }

    private static int count(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int id = IntegerArgumentType.getInteger(context, "id");
        CBT_Dyson dyson = com.ntmspace.trait.CelestialTraits.getTrait(com.ntmspace.trait.CelestialTraits.starOf(level), CBT_Dyson.class, false);
        log(context, "dyson_swarm id=" + id + " members=" + CBT_Dyson.count(level, id) + " total=" + (dyson == null ? 0 : dyson.size()));
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos core = pos(context);
        Direction facing = Direction.byName(StringArgumentType.getString(context, "facing"));
        if (facing == null || facing.getAxis().isVertical()) {
            context.getSource().sendFailure(Component.literal("facing: north, east, south or west"));
            return 0;
        }
        BlockMultiblockCore block = switch (StringArgumentType.getString(context, "machine")) {
            case "launcher" -> DysonContent.DYSON_LAUNCHER.get();
            case "receiver" -> DysonContent.DYSON_RECEIVER.get();
            case "he" -> DysonContent.DYSON_CONVERTER_HE.get();
            case "tu" -> DysonContent.DYSON_CONVERTER_TU.get();
            case "anatmogenesis" -> DysonContent.DYSON_CONVERTER_ANATMOGENESIS.get();
            default -> null;
        };
        if (block == null) {
            context.getSource().sendFailure(Component.literal("launcher, receiver, he, tu or anatmogenesis"));
            return 0;
        }
        MultiblockHandlerXR.placeAssembled(level, core, block, facing);
        log(context, "dyson_placed " + block.getDescriptionId() + " at " + core.toShortString() + " facing " + facing.getName());
        return 1;
    }

    private static int chip(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockEntity te = MultiblockSurface.coreEntity(level, pos(context));
        ItemStack chip = new ItemStack(ModSatelliteItems.sat_chip.get());
        ISatChip.setFreqS(chip, IntegerArgumentType.getInteger(context, "freq"));
        if (te instanceof TileEntityDysonLauncher launcher) {
            launcher.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_CHIP, chip);
        } else if (te instanceof TileEntityDysonReceiver receiver) {
            receiver.inventory.setStackInSlot(0, chip);
        } else {
            context.getSource().sendFailure(Component.literal("no Dyson launcher or receiver there"));
            return 0;
        }
        log(context, "dyson_chip freq=" + ISatChip.getFreqS(chip) + " at " + te.getBlockPos().toShortString());
        return 1;
    }

    private static int charge(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        if (!(MultiblockSurface.coreEntity(level, pos(context)) instanceof TileEntityDysonLauncher launcher)) {
            context.getSource().sendFailure(Component.literal("no Dyson launcher there"));
            return 0;
        }
        launcher.power = TileEntityDysonLauncher.MAX_POWER;
        if (launcher.inventory.getStackInSlot(TileEntityDysonLauncher.SLOT_MEMBERS).isEmpty()) {
            launcher.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_MEMBERS,
                    new ItemStack(DysonContent.SWARM_MEMBER.get(), TileEntityDysonLauncher.MEMBERS_PER_LAUNCH));
        }
        return 1;
    }

    private static int state(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockEntity te = MultiblockSurface.coreEntity(level, pos(context));
        String line;
        if (te instanceof TileEntityDysonLauncher l) {
            line = "dyson_state launcher id=" + l.swarmId + " swarm=" + l.swarmCount + " power=" + l.power + " operating="
                    + l.isOperating + " spinDown=" + l.isSpinningDown + " members=" + l.inventory.getStackInSlot(0).getCount();
        } else if (te instanceof TileEntityDysonReceiver r) {
            line = "dyson_state receiver id=" + r.swarmId + " swarm=" + r.swarmCount + " consumers=" + r.swarmConsumers
                    + " receiving=" + r.isReceiving + " beam=" + r.beamLength;
        } else if (te instanceof TileEntityDysonConverterHE he) {
            line = "dyson_state he power=" + he.power + " converting=" + he.isConverting;
        } else if (te instanceof TileEntityDysonConverterTU tu) {
            line = "dyson_state tu heat=" + tu.heatEnergy;
        } else if (te instanceof com.ntmspace.tileentity.dyson.TileEntityDysonConverterAnatmogenesis a) {
            line = "dyson_state anatmogenesis fluid=" + a.fluid.getName() + " emitting=" + a.isEmitting + " converting=" + a.isConverting;
        } else {
            context.getSource().sendFailure(Component.literal("no Dyson machine there"));
            return 0;
        }
        log(context, line);
        return 1;
    }
}
