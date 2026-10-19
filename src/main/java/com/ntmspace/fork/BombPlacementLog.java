package com.ntmspace.fork;

import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.blocks.bomb.BlockDynamite;
import com.hbm.blocks.bomb.BlockFireworks;
import com.hbm.blocks.bomb.BlockFissureBomb;
import com.hbm.blocks.bomb.BlockSemtex;
import com.hbm.blocks.bomb.BlockTNT;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.blocks.bomb.BombFlameWar;
import com.hbm.blocks.bomb.BombFloat;
import com.hbm.blocks.bomb.BombMulti;
import com.hbm.blocks.bomb.BombThermo;
import com.hbm.blocks.bomb.ExplosiveCharge;
import com.hbm.blocks.bomb.Landmine;
import com.hbm.blocks.bomb.NukeBalefire;
import com.hbm.blocks.bomb.NukeBoy;
import com.hbm.blocks.bomb.NukeFleija;
import com.hbm.blocks.bomb.NukeGadget;
import com.hbm.blocks.bomb.NukeMan;
import com.hbm.blocks.bomb.NukeMike;
import com.hbm.blocks.bomb.NukeN2;
import com.hbm.blocks.bomb.NukePrototype;
import com.hbm.blocks.bomb.NukeSolinium;
import com.hbm.blocks.bomb.NukeTsar;
import com.hbm.config.GeneralConfig;
import dev.golem.core.world.BlockPlaces;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

/**
 * hbm-space's {@code [BOMBPL]} admin log: the 24 bomb blocks it gave an {@code onBlockPlacedBy} log line (22 bomb
 * classes, {@code ExplosiveCharge} and its own {@code NukeAntimatter}) write "{@code [BOMBPL]<name> placed at x / y / z! by <player>}" when
 * {@code 1.18_enableExtendedLogging} is on (default off). One Golem {@link BlockPlaces} listener instead of 23 edits.
 */
public final class BombPlacementLog {

    private BombPlacementLog() {}

    /** The hbm-space classes with the log line (each also covers its HBM subclasses, as 1.7's override did). */
    static final List<Class<? extends Block>> LOGGED = List.of(BlockChargeBase.class, BlockDynamite.class, BlockFireworks.class,
            BlockFissureBomb.class, BlockSemtex.class, BlockTNT.class, BlockVolcano.class, BombFlameWar.class, BombFloat.class,
            BombMulti.class, BombThermo.class, Landmine.class, NukeBalefire.class, NukeBoy.class, NukeFleija.class,
            NukeGadget.class, NukeMan.class, NukeMike.class, NukeN2.class, NukePrototype.class, NukeSolinium.class,
            NukeTsar.class, ExplosiveCharge.class, com.ntmspace.fork.antimatter.NukeAntimatter.class);

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        BlockPlaces.onPlace((placer, level, pos, state) -> {
            if (GeneralConfig.enableExtendedLogging && logged(state.getBlock())) {
                com.hbm.Constants.LOG.info(message(state.getBlock(), pos, placer));
            }
        });
    }

    public static boolean logged(Block block) {
        for (Class<? extends Block> type : LOGGED) if (type.isInstance(block)) return true;
        return false;
    }

    /** 1.7: {@code "[BOMBPL]" + getLocalizedName() + " placed at " + x + " / " + y + " / " + z + "! " + "by " + name}. */
    public static String message(Block block, BlockPos pos, Entity placer) {
        String name = placer == null ? "null" : placer.getName().getString();
        return "[BOMBPL]" + block.getName().getString() + " placed at " + pos.getX() + " / " + pos.getY() + " / " + pos.getZ()
                + "! " + "by " + name;
    }
}
