package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import dev.golem.core.world.Chunks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import com.ntmspace.tileentity.machine.TileEntityMachineSolarPanel;
import com.ntmspace.registry.SpaceMachinesC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineSolar} (Solar Panel): a 1x1x5 strip along the placement direction ({@code getDimensions}
 * {0, 0, 2, 2, 0, 0}, offset 0), core in the middle. 1.7 stamped no {@code makeExtra} cells, so only the core touches
 * the networks, on all six faces (its {@code tryProvide} at every neighbour). No GUI.
 */
public class MachineSolar extends BlockMultiblockMachine implements IEnergyConnectorBlock, ICapabilityBlock {
    private static final MachineCaps CAPS = MachineCaps.of(SpaceMachinesC.SOLAR_BE).powerOut().fe();

    @Override
    public MachineCaps caps() {
        return CAPS;
    }

    public MachineSolar(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSolarPanel(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 2, 2, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /**
     * The six visual connectors, using the same native contact query as the panel renderer.
     */
    public static int powerConnectionMask(Level level, BlockPos core) {
        var mask = 0;
        for (var side : Direction.values()) {
            var neighbour = core.relative(side);
            if (Chunks.loaded(level, neighbour.getX() >> 4, neighbour.getZ() >> 4) != null
                    && Library.canConnect(level, neighbour, ForgeDirection.getOrientation(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return mask;
    }

    /**
     * The core provides on every face (1.7 {@code ForgeDirection.VALID_DIRECTIONS}).
     */
    @Override
    public int coreMask() {
        return MASK_ALL;
    }
}
