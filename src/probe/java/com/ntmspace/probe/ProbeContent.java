package com.ntmspace.probe;

import com.hbm.api.environment.Environment;
import dev.golem.core.fluid.TokenFluids;
import dev.golem.core.hook.Hook;
import dev.golem.core.hook.Verdict;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * What the probe adds, each through the same extension points a real addon uses:
 * <ul>
 *   <li>a plain vanilla fluid, {@code ntmprobe:probe_gas}, registered with Golem Core only: HBM never hears of it, so
 *       it is a foreign fluid that HBM's open fluid model must still carry;</li>
 *   <li>a "sealed room" answer to HBM's environment hooks, in its own phase ordered before NTM: Space's: a sealed
 *       position breathes at 1 atm even on the Mun, where NTM: Space says there is no air
 *       ({@code /ntmprobe seal <x y z>});</li>
 *   <li>a datapack (src/probe/resources) adding a chemical plant recipe that makes the foreign fluid and switching off
 *       HBM's {@code chem.cobble}.</li>
 * </ul>
 */
public final class ProbeContent {

    public static final String SEALED_PHASE = "ntmprobe:sealed";

    /** Sealed positions, by dimension id and packed position. */
    private static final Set<String> SEALED = ConcurrentHashMap.newKeySet();

    private ProbeContent() {}

    public static void init() {
        TokenFluids.register(ProbeEntry.MOD_ID, List.of(new TokenFluids.Spec("probe_gas", "fluid.ntmprobe.probe_gas",
                "Probe Gas", "minecraft:block/water_still", 0xFFC0FFC0, 300, -1000, 1000, true)));

        dev.golem.core.command.CompatCommands.server(ProbeContent::register);

        Environment.BREATHING.addPhaseOrdering(SEALED_PHASE, Hook.DEFAULT_PHASE);
        Environment.BREATHING.register(SEALED_PHASE, (level, pos, amount) -> sealed(level, pos) ? Verdict.ALLOW
                : ProbeMarks.at("breathing", level, pos) instanceof Verdict marked ? marked : Verdict.PASS);
        Environment.PRESSURE.addPhaseOrdering(SEALED_PHASE, Hook.DEFAULT_PHASE);
        Environment.PRESSURE.register(SEALED_PHASE, (level, pos) -> sealed(level, pos) ? Environment.EARTH_PRESSURE
                : ProbeMarks.at("pressure", level, pos) instanceof Number marked ? marked.doubleValue() : Double.NaN);
    }

    private static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal(ProbeEntry.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(net.minecraft.commands.Commands.literal("seal")
                        .then(net.minecraft.commands.Commands.argument("pos",
                                        net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                .executes(context -> {
                                    BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument
                                            .getLoadedBlockPos(context, "pos");
                                    seal(context.getSource().getLevel(), pos);
                                    ProbeEntry.LOG.info("probe_seal {} {}",
                                            dev.golem.core.Ids.keyId(context.getSource().getLevel().dimension()),
                                            pos.toShortString());
                                    return 1;
                                })))
                .then(net.minecraft.commands.Commands.literal("ping")
                        .then(net.minecraft.commands.Commands.argument("value",
                                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int value = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value");
                                    boolean sent = ProbeNet.send(context.getSource().getPlayerOrException(), value);
                                    ProbeEntry.LOG.info("probe_ping {} sent={}", value, sent);
                                    return sent ? 1 : 0;
                                }))));
    }

    public static void seal(Level level, BlockPos pos) {
        SEALED.add(key(level, pos));
    }

    public static boolean sealed(Level level, BlockPos pos) {
        return !SEALED.isEmpty() && SEALED.contains(key(level, pos));
    }

    private static String key(Level level, BlockPos pos) {
        return dev.golem.core.Ids.keyId(level.dimension()) + "@" + pos.asLong();
    }
}
