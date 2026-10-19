package com.ntmspace.block;

import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.fluid.WorldFluids;

/**
 * hbm-space's world fluids {@code mercury_block} (Eve's seas) and {@code bromine_block} (Ike's lakes): a
 * {@code GenericFluidBlock}, i.e. a flowing {@code BlockFluidClassic} (8 quanta, never makes a source) of
 * {@code Material.water}, here a Golem Core {@link FluidBlock} ({@code com.ntmspace.fluid.SpaceWorldFluids}), as HBM
 * ported its own 1.7 fluid blocks and as Tekto's {@code ccl_block} is.
 *
 * <p><b>Ported oddity:</b> 1.7 gave mercury {@code setDamage(lead, 1F)} and bromine {@code setDamage(acid, 5F)}, but
 * hbm-space's {@code GenericFluidBlock} applies damage only when the block is {@code sulfuric_acid_block}, so touching
 * either does nothing.
 */
public class SpaceFluidBlock extends FluidBlock {

    public SpaceFluidBlock(Properties properties, WorldFluids.Handle fluid) {
        super(properties, fluid);
    }
}
