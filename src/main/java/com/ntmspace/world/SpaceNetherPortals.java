package com.ntmspace.world;

import com.ntmspace.config.SpaceConfig;
import com.ntmspace.orbit.Orbit;
import dev.golem.core.world.BlockPlaces;
import dev.golem.core.world.Portals;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

/** hbm-space {@code ModEventHandler.onBlockPlaced}: fire a player places on a celestial level or in orbit lights a portal. */
public final class SpaceNetherPortals {

    private SpaceNetherPortals() {
    }

    public static void init() {
        BlockPlaces.onPlace((placer, level, pos, placed) -> {
            if (!SpaceConfig.allowNetherPortals || !(placer instanceof Player)) return;
            if (!(placed.getBlock() instanceof BaseFireBlock) || !(level.getBlockState(pos).getBlock() instanceof BaseFireBlock)) return;
            if (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension() == Level.END)
                return;
            if (CelestialBodies.of(level) != null || Orbit.inOrbit(level)) {
                Portals.tryCreate(level, pos, Direction.Axis.X);
            }
        });
    }
}
