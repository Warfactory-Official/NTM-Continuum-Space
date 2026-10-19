package com.ntmspace.block;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.tileentity.ModBlockEntities;
import com.ntmspace.atmosphere.BlockSealable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * HBM's fan as hbm-space has it: the same block, sealable-aware ({@code MachineFan implements IBlockSealable}, never
 * sealing: air passes through a fan). Substituted for {@code hbm:fan} in {@code SpaceEntry.substitute}; its other
 * hbm-space change, moving nothing without air, is HBM's own fan reading {@code Environment.pressure}.
 */
public class SpaceMachineFan extends MachineFan implements BlockSealable {

    public SpaceMachineFan(Properties properties) {
        super(properties, () -> ModBlockEntities.MACHINE_FAN.get());
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos) {
        return false;
    }
}
