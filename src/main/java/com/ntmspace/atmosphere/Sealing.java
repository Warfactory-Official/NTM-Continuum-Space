package com.ntmspace.atmosphere;

import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which blocks seal a room, and telling the rooms when that changes: hbm-space's {@code IBlockSealable} for blocks that
 * are not NTM: Space's own.
 *
 * <ul>
 *   <li>{@code #ntmspace:unsealable} (block tag): blocks hbm-space made {@code IBlockSealable} only to answer "never seals"
 *       although they are full blocks - HBM's brick furnace, alloy furnace and diesel generator (the fan is NTM: Space's
 *       substitute, {@code SpaceMachineFan}). Any pack can add to it.</li>
 *   <li>HBM's doors ({@code BlockDoorGeneric}): a door cell seals while the door's moving part is not in it
 *       (1.7 {@code !getBlocksMovement}), and when a door finishes opening or closing
 *       ({@link TileEntityDoorGeneric#MOVED}) every cell of it tells the rooms around (1.7
 *       {@code TileEntityDoorGeneric -> IBlockSealable.updateSealedState}).</li>
 *   <li>{@code #ntmspace:suffocation_immune} (entity type tag): 1.7's {@code ISuffocationImmune} mobs.</li>
 *   <li>{@code #ntmspace:sealed_armor} (item tag): the armour pieces 1.7 built with {@code ArmorFSB.setSealed(true)}.</li>
 *   <li>{@code #ntmspace:corrosion_protection} (item tag): 1.7's corrosion-proof inserts.</li>
 * </ul>
 */
public final class Sealing {

    private Sealing() {}

    public static final TagKey<Block> UNSEALABLE = TagKey.create(Registries.BLOCK, Ids.of(NtmSpace.MOD_ID, "unsealable"));
    public static final TagKey<EntityType<?>> SUFFOCATION_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, Ids.of(NtmSpace.MOD_ID, "suffocation_immune"));
    public static final TagKey<Item> SEALED_ARMOR = TagKey.create(Registries.ITEM, Ids.of(NtmSpace.MOD_ID, "sealed_armor"));
    /** 1.7 {@code ItemModInsert.withCorrosionProtection()}: chestplate inserts that stop a corrosive atmosphere. */
    public static final TagKey<Item> CORROSION_PROTECTION =
            TagKey.create(Registries.ITEM, Ids.of(NtmSpace.MOD_ID, "corrosion_protection"));

    public static void init() {
        TileEntityDoorGeneric.MOVED.register((level, core, state) -> updateSealedState(level, core));
    }

    /**
     * 1.7 {@code IBlockSealable.updateSealedState}: a sealable block at {@code pos} changed; for one of HBM's multiblocks
     * every cell of it (the 1.7 {@code BlockDummyable} box around its core) is reported.
     */
    public static void updateSealedState(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        List<AtmosphereBlob> nearbyBlobs = AtmosphereHandler.getBlobsWithinRadius(level, pos);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockMultiblockCore block && state.hasProperty(BlockMultiblockCore.FACING)) {
            Direction facing = state.getValue(BlockMultiblockCore.FACING);
            int[] rot = MultiblockHandlerXR.rotate(block.getDimensions(), facing);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            for (int a = x - rot[4]; a <= x + rot[5]; a++) {
                for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                    for (int c = z - rot[2]; c <= z + rot[3]; c++) {
                        AtmosphereHandler.onSealableChange(server, new BlockPos(a, b, c), nearbyBlobs);
                    }
                }
            }
        } else {
            AtmosphereHandler.onSealableChange(server, pos, nearbyBlobs);
        }
    }

    /** Whether {@code state} is one of HBM's doors (its cells answer for themselves). */
    public static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof BlockDoorGeneric;
    }
}
