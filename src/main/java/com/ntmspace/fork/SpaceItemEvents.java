package com.ntmspace.fork;

import com.hbm.Constants;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.handler.BobmazonSignHandler;
import dev.golem.core.world.BlockUses;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Deaths;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeverBlock;
import org.apache.logging.log4j.LogManager;

/** Space's death reward and interaction logging, with the actual interaction actor retained. */
public final class SpaceItemEvents {
    private static final UUID MELLOW = UUID.fromString("99b9efa5-8c38-4b22-86c2-baf97789d2fb");
    private SpaceItemEvents() {}

    public static void init() {
        Deaths.onDeath((entity, source) -> {
            if (entity.getUUID().equals(MELLOW) && entity.level() instanceof ServerLevel level) {
                Compat.spawnAtLocation(entity, level,
                        new ItemStack(ModBlocks.block_meteor.get(), 1 + entity.getRandom().nextInt(10)), 0F);
            }
        });
        BobmazonSignHandler.pickupDelay = 1;
        BobmazonSignHandler.OBTAINED.register((player, pos) ->
                LogManager.getLogger(Constants.MOD_NAME).fatal("THE HIDDENCAT HAS BEEN OBTAINED  x: {} /  y: {} / z: {} by {}!",
                        pos.getX(), pos.getY(), pos.getZ(), player.getDisplayName().getString()));
        BlockUses.AFTER.register((level, pos, state, player) -> {
            if (GeneralConfig.enableExtendedLogging && state.getBlock() instanceof LeverBlock) {
                LogManager.getLogger(Constants.MOD_NAME).info("[DET] pulled lever at {} / {} / {} by {}!",
                        pos.getX(), pos.getY(), pos.getZ(), player.getDisplayName().getString());
            }
        });
    }
}
