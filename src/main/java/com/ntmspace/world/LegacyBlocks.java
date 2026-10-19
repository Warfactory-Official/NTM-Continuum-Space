package com.ntmspace.world;

import com.ntmspace.world.gen.legacy.Block17;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block state a 1.7 primer entry becomes. Every entry names a registered block by id; where the 1.7 metadata chose
 * the block or its state, {@code plant_dead} (split by type in the port, {@code hbm:plant_dead_<type>}) and the blocks
 * with {@link Block17#hasMetaIds() per-metadata ids} (body ores, sellafite shades, terracotta colours), the metadata
 * picks it.
 */
final class LegacyBlocks {

    private static final String[] DEAD_PLANTS = {"generic", "grass", "flower", "bigflower", "fern"};
    private static final Map<Block17, BlockState> STATES = new IdentityHashMap<>();

    private LegacyBlocks() {}

    static BlockState state(Block17 block, byte meta) {
        if (block == Block17.PLANT_DEAD) {
            return lookup("hbm:plant_dead_" + DEAD_PLANTS[Math.floorMod(meta, DEAD_PLANTS.length)].toLowerCase(Locale.ROOT));
        }
        if (block.hasMetaIds()) {
            // a block whose 1.7 metadata picks the modern block or state (body ores, shades, terracotta colours)
            synchronized (META_STATES) {
                BlockState[] states = META_STATES.computeIfAbsent(block, b -> new BlockState[256]);
                int m = meta & 255;
                if (states[m] == null) states[m] = lookup(block.id(m));
                return states[m];
            }
        }
        synchronized (STATES) {
            return STATES.computeIfAbsent(block, b -> lookup(b.id));
        }
    }

    private static final Map<Block17, BlockState[]> META_STATES = new IdentityHashMap<>();

    /** A block id, or a state string {@code id[property=value,...]}. */
    private static BlockState lookup(String id) {
        int bracket = id.indexOf('[');
        String name = bracket < 0 ? id : id.substring(0, bracket);
        Block block = BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(name))
                .orElseThrow(() -> new IllegalStateException("1.7 terrain names an unregistered block: " + id));
        if (bracket < 0) return block.defaultBlockState();
        BlockState state = com.hbm.util.SavedIds.stateFromName(id, null);
        if (state == null || state.getBlock() != block) throw new IllegalStateException("1.7 terrain names an unknown state: " + id);
        return state;
    }
}
