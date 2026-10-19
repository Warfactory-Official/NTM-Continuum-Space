package com.ntmspace.block.terrain;

import com.ntmspace.fluid.SpaceWorldFluids;
import dev.golem.core.fluid.FluidBlock;

/**
 * hbm-space's {@code ccl_block} ("CCL"), Tekto's carbon tetrachloride sea: a {@code GenericFluidBlock}, i.e. a flowing
 * {@code BlockFluidClassic} (8 quanta, never makes a source) of {@code Material.water}, here a Golem Core
 * {@link FluidBlock} ({@link SpaceWorldFluids#CCL}).
 *
 * <p><b>Ported oddity:</b> 1.7 gave it {@code setDamage(lead, 1F)}, but {@code GenericFluidBlock} only applied damage
 * when the block was {@code sulfuric_acid_block}, so touching it does nothing.
 */
public class BlockCcl extends FluidBlock {

    public BlockCcl(Properties properties) {
        super(properties, SpaceWorldFluids.CCL);
    }
}
