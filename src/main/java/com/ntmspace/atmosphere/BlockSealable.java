package com.ntmspace.atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code IBlockSealable}: a block the sealed-room atmosphere system asks whether it closes a room off. A
 * block without it seals when it is a full cube. The flood fill that reads it arrives with the atmosphere system
 * (hbm-space {@code handler/atmosphere}); until then the answers are only declared.
 */
public interface BlockSealable {

    boolean isSealed(Level level, BlockPos pos);
}
