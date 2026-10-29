package com.ntmspace.command;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ntmspace.NtmSpace;
import com.ntmspace.satellite.SatelliteWar;
import com.ntmspace.trait.CBT_Invasion;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.command.Permissions;

import java.util.Map;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Port-only test conveniences for the war and invasion family (hbm-space has no command for either: an invasion starts
 * only with the angel's death, a war only through a railgun), operators only, used by the harness scenario
 * {@code WarInvasion}. Every command logs one {@code [war] ...} line.
 * <ul>
 *   <li>{@code /ntmwar invasion start <body> [countdown]}: 1.7's {@code CBT_Invasion(0, countdown, false)} (default 122,
 *       the angel's);</li>
 *   <li>{@code /ntmwar invasion kills <body> <n>}: adds kills to the running wave;</li>
 *   <li>{@code /ntmwar invasion stop <body>}: removes the invasion;</li>
 *   <li>{@code /ntmwar projectile <body> <type> [travel]}: launches a war projectile at the body (railgun values: size 20,
 *       damage 50, position (0, 55, 20), travel 100), starting the war ({@code CBT_War(100, 0)}) if there is none;</li>
 *   <li>{@code /ntmwar railgun <frequency> fire}: arms the war satellite on that frequency (1.7 {@code onClick}, which
 *       nothing in 1.7 called) so it fires at the body it is aimed at and plays its flash;</li>
 *   <li>{@code /ntmwar railgun <frequency> orbit|remove}: launches a railgun ({@code sat_war}) on that frequency over the
 *       executor's position the way a launched chip does, or takes it out of orbit;</li>
 *   <li>{@code /ntmwar controller <x> <y> <z> <frequency> <body>}: a war controller there with that body's drive in
 *       slot 1 and a chip on the frequency in slot 2;</li>
 *   <li>{@code /ntmwar clear <body>}: removes the body's war, destruction and invasion;</li>
 *   <li>{@code /ntmwar status <body>}: the body's war and invasion state.</li>
 * </ul>
 */
public final class WarCommands {

    private WarCommands() {
    }

    public static void init() {
        CompatCommands.server(WarCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmwar")
                                    .requires(Permissions.gamemasters())
                                    .then(Commands.literal("invasion")
                                                  .then(Commands.literal("start")
                                                                .then(Commands.argument("body",
                                                                                      StringArgumentType.word())
                                                                              .executes(ctx -> start(ctx, 122))
                                                                              .then(Commands.argument("countdown",
                                                                                                    IntegerArgumentType.integer(0))
                                                                                            .executes(ctx -> start(ctx,
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            ctx,
                                                                                                            "countdown"))))))
                                                  .then(Commands.literal("kills")
                                                                .then(Commands.argument("body",
                                                                                      StringArgumentType.word())
                                                                              .then(Commands.argument("n",
                                                                                                    IntegerArgumentType.integer(0))
                                                                                            .executes(
                                                                                                    WarCommands::kills))))
                                                  .then(Commands.literal("stop")
                                                                .then(Commands.argument("body",
                                                                                      StringArgumentType.word())
                                                                              .executes(WarCommands::stop))))
                                    .then(Commands.literal("projectile")
                                                  .then(Commands.argument("body", StringArgumentType.word())
                                                                .then(Commands.argument("type",
                                                                                      StringArgumentType.word())
                                                                              .executes(ctx -> projectile(ctx, 100))
                                                                              .then(Commands.argument("travel",
                                                                                                    IntegerArgumentType.integer(0))
                                                                                            .executes(ctx -> projectile(
                                                                                                    ctx,
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            ctx,
                                                                                                            "travel")))))))
                                    .then(Commands.literal("railgun")
                                                  .then(Commands.argument("frequency", IntegerArgumentType.integer())
                                                                .then(Commands.literal("fire")
                                                                              .executes(WarCommands::fire))
                                                                .then(Commands.literal("orbit")
                                                                              .executes(WarCommands::orbit))
                                                                .then(Commands.literal("remove")
                                                                              .executes(WarCommands::remove))))
                                    .then(Commands.literal("controller")
                                                  .then(Commands.argument("x", IntegerArgumentType.integer())
                                                                .then(Commands.argument("y",
                                                                                      IntegerArgumentType.integer())
                                                                              .then(Commands.argument("z",
                                                                                                    IntegerArgumentType.integer())
                                                                                            .then(Commands.argument(
                                                                                                                  "frequency",
                                                                                                                  IntegerArgumentType.integer())
                                                                                                          .then(Commands.argument(
                                                                                                                                "body",
                                                                                                                                StringArgumentType.word())
                                                                                                                        .executes(
                                                                                                                                WarCommands::controller)))))))
                                    .then(Commands.literal("clear")
                                                  .then(Commands.argument("body", StringArgumentType.word())
                                                                .executes(WarCommands::clear)))
                                    .then(Commands.literal("status")
                                                  .then(Commands.argument("body", StringArgumentType.word())
                                                                .executes(WarCommands::status))));
    }

    private static String body(CommandContext<CommandSourceStack> ctx) {
        String body = StringArgumentType.getString(ctx, "body");
        if (!CelestialTraits.allBodies().contains(body)) {
            ctx.getSource()
               .sendFailure(Component.literal("unknown body " + body + ", one of " + CelestialTraits.allBodies()));
            return null;
        }
        CelestialTraits.attach(ctx.getSource().getLevel());
        return body;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int countdown) {
        String body = body(ctx);
        if (body == null) return 0;
        CelestialTraits.modifyTraits(body, new CBT_Invasion(0, countdown, false));
        return ok(ctx, "invasion started on " + body + ", countdown " + countdown);
    }

    private static int kills(CommandContext<CommandSourceStack> ctx) {
        String body = body(ctx);
        if (body == null) return 0;
        CBT_Invasion invasion = CelestialTraits.getTrait(body, CBT_Invasion.class, false);
        if (invasion == null) {
            ctx.getSource().sendFailure(Component.literal("no invasion on " + body));
            return 0;
        }
        invasion.kills += IntegerArgumentType.getInteger(ctx, "n");
        CelestialTraits.modifyTraits(body, invasion);
        return ok(ctx,
                "invasion on " + body + ": wave " + invasion.wave + ", kills " + invasion.kills + "/" + invasion.killreq);
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        String body = body(ctx);
        if (body == null) return 0;
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = CelestialTraits.getTraits(body);
        traits.remove(CBT_Invasion.class);
        CelestialTraits.setTraits(body, traits);
        return ok(ctx, "invasion stopped on " + body);
    }

    private static int projectile(CommandContext<CommandSourceStack> ctx, int travel) {
        String body = body(ctx);
        if (body == null) return 0;
        CBT_War.ProjectileType type;
        try {
            type = CBT_War.ProjectileType.valueOf(
                    StringArgumentType.getString(ctx, "type").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(
                    Component.literal("type: one of " + java.util.Arrays.toString(CBT_War.ProjectileType.values())));
            return 0;
        }
        CBT_War war = CelestialTraits.getTrait(body, CBT_War.class, false);
        if (war == null) war = new CBT_War(100, 0);
        ResourceKey<Level> key = CelestialTraits.levelKeyOf(body);
        String target = key == null ? "minecraft:overworld" : dev.golem.core.Ids.keyId(key).toString();
        war.launchProjectile(travel, 20, 50, 0F, 55, 20, type, target);
        CelestialTraits.modifyTraits(body, war);
        return ok(ctx, "projectile " + type + " launched at " + body + ", travel " + travel + "; health " + war.health
                + ", " + war.projectiles.size() + " in flight");
    }

    private static int fire(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int freq = IntegerArgumentType.getInteger(ctx, "frequency");
        BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
        Satellite sat = SatelliteSavedData.get(level, pos).getSatFromFreq(freq);
        if (!(sat instanceof SatelliteWar war)) {
            ctx.getSource().sendFailure(Component.literal("no war satellite on " + freq));
            return 0;
        }
        war.onClick(level, pos.getX(), pos.getZ());
        return ok(ctx, "railgun " + freq + " fired");
    }

    private static int orbit(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int freq = IntegerArgumentType.getInteger(ctx, "frequency");
        net.minecraft.world.phys.Vec3 at = ctx.getSource().getPosition();
        if (SatelliteSavedData.get(level, BlockPos.containing(at)).isFreqTaken(freq)) {
            ctx.getSource().sendFailure(Component.literal("frequency " + freq + " is taken"));
            return 0;
        }
        net.minecraft.world.item.ItemStack chip = new net.minecraft.world.item.ItemStack(
                com.ntmspace.satellite.SpaceSatellites.SAT_WAR.get());
        Satellite.orbit(level, chip, freq, at.x, at.y, at.z);
        return ok(ctx, "railgun " + freq + " in orbit: " + (SatelliteSavedData.get(level, BlockPos.containing(at))
                                                                              .getSatFromFreq(freq) != null));
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int freq = IntegerArgumentType.getInteger(ctx, "frequency");
        SatelliteSavedData data = SatelliteSavedData.get(level, BlockPos.containing(ctx.getSource().getPosition()));
        boolean removed = data.descend(freq);
        return ok(ctx, "railgun " + freq + " removed: " + removed);
    }

    private static int controller(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = new BlockPos(IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"),
                IntegerArgumentType.getInteger(ctx, "z"));
        String bodyName = StringArgumentType.getString(ctx, "body");
        com.ntmspace.rocket.DriveBody drive = com.ntmspace.rocket.DriveBody.byName(bodyName);
        if (drive == null) {
            ctx.getSource().sendFailure(Component.literal("no drive for " + bodyName));
            return 0;
        }
        level.setBlock(pos, com.ntmspace.registry.WarContent.WAR_CONTROLLER.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof com.ntmspace.tileentity.war.TileEntityMachineWarController te)) {
            ctx.getSource().sendFailure(Component.literal("no war controller at " + pos));
            return 0;
        }
        net.minecraft.world.item.ItemStack chip = new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModSatelliteItems.sat_chip.get());
        com.hbm.items.ISatChip.setFreqS(chip, IntegerArgumentType.getInteger(ctx, "frequency"));
        te.setItem(1, new net.minecraft.world.item.ItemStack(com.ntmspace.registry.RocketItems.drive(drive)));
        te.setItem(2, chip);
        return ok(ctx, "war controller at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " aiming "
                + IntegerArgumentType.getInteger(ctx, "frequency") + " at " + bodyName);
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        String body = body(ctx);
        if (body == null) return 0;
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = CelestialTraits.getTraits(body);
        traits.remove(CBT_War.class);
        traits.remove(CBT_Invasion.class);
        traits.remove(com.ntmspace.trait.CBT_Destroyed.class);
        CelestialTraits.setTraits(body, traits);
        return ok(ctx, "war, destruction and invasion cleared on " + body);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        String body = body(ctx);
        if (body == null) return 0;
        CBT_War war = CelestialTraits.getTrait(body, CBT_War.class, false);
        CBT_Invasion invasion = CelestialTraits.getTrait(body, CBT_Invasion.class, false);
        boolean destroyed = CelestialTraits.hasTrait(body, com.ntmspace.trait.CBT_Destroyed.class, false);
        String text = "status " + body + ": war " + (war == null ? "none" : "health " + war.health + " shield " + war.shield
                + " projectiles " + war.projectiles.size())
                + "; invasion " + (invasion == null ? "none" : "wave " + invasion.wave + " invading " + invasion.isInvading
                + " countdown " + invasion.waveTime + " kills " + invasion.kills + "/" + invasion.killreq)
                + "; destroyed " + destroyed;
        return ok(ctx, text);
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String text) {
        NtmSpace.LOG.info("[war] {}", text);
        ctx.getSource().sendSuccess(() -> Component.literal(text), true);
        return 1;
    }
}
