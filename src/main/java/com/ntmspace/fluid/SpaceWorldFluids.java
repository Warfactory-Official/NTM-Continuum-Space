package com.ntmspace.fluid;

import com.hbm.registration.Presets;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.LaytheTektoBlocks;
import com.ntmspace.registry.SpaceFluidBlocks;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.fluid.WorldFluids;
import dev.golem.core.fluid.WorldFluids.Physics;
import dev.golem.core.fluid.WorldFluids.Spec;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * hbm-space's placeable world fluids ({@code ModBlocks} 2649-2666): the Forge {@code Fluid}s it registered for Eve's
 * mercury seas, Ike's bromine lakes and Tekto's carbon tetrachloride sea, as Golem Core {@link WorldFluids}, the way
 * HBM declares its own ({@code com.hbm.blocks.fluid.ModWorldFluids}). All three are a {@code GenericFluid} (only the
 * density, viscosity and temperature setters) carried by a {@code GenericFluidBlock}: a {@code BlockFluidClassic}
 * with Forge's defaults (8 quanta, tick rate viscosity / 200, render pass 1, no light) of {@code Material.water}
 * (water physics, so the {@code minecraft:water} fluid tag).
 *
 * <p>The fluids take 1.7's Forge fluid names in this namespace ({@code ntmspace:mercury_fluid}, flowing
 * {@code ntmspace:flowing_mercury_fluid}), which no network fluid ({@code ntmspace:<FluidType key>},
 * {@code hbm:mercury}) uses. The block ids stay the 1.7 block names. These are world blocks, not duct fluids: the
 * buckets' 1.7 {@code FluidContainerRegistry} link to {@code hbm:mercury}, {@code ntmspace:bromine} and
 * {@code ntmspace:ccl} is unchanged ({@code SpaceFluidContainers}).
 */
public final class SpaceWorldFluids {

    private SpaceWorldFluids() {}

    private static final String NS = NtmSpace.MOD_ID;

    /** 1.7 {@code GenericFluid("mercury_fluid").setDensity(1840).setViscosity(1000).setTemperature(273)}. */
    public static final WorldFluids.Handle MERCURY = WorldFluids.declare(NS, Spec.of("mercury_fluid")
                    .name("fluid.ntmspace.mercury_fluid", "Mercury")
                    .sprites("ntmspace:blocks/mercury_still", "ntmspace:blocks/mercury_flowing")
                    .density(1840).viscosity(1000).temperature(273).physics(Physics.WATER),
            () -> SpaceFluidBlocks.MERCURY_BLOCK.get(), () -> SpaceFluidBlocks.BUCKET_MERCURY.get());

    /** 1.7 {@code GenericFluid("bromine_fluid").setDensity(3000).setViscosity(3000).setTemperature(273)}: tick 15. */
    public static final WorldFluids.Handle BROMINE = WorldFluids.declare(NS, Spec.of("bromine_fluid")
                    .name("fluid.ntmspace.bromine_fluid", "Alkyl Bromide")
                    .sprites("ntmspace:blocks/bromine_still", "ntmspace:blocks/bromine_flowing")
                    .density(3000).viscosity(3000).temperature(273).physics(Physics.WATER),
            () -> SpaceFluidBlocks.BROMINE_BLOCK.get(), () -> SpaceFluidBlocks.BUCKET_BROMINE.get());

    /**
     * 1.7 {@code GenericFluid("ccl_fluid").setDensity(1840).setViscosity(1000).setTemperature(273)}; its block's
     * sprites are {@code ccl} (still) and {@code ccl_flowing}.
     */
    public static final WorldFluids.Handle CCL = WorldFluids.declare(NS, Spec.of("ccl_fluid")
                    .name("fluid.ntmspace.ccl_fluid", "CCL")
                    .sprites("ntmspace:blocks/ccl", "ntmspace:blocks/ccl_flowing")
                    .density(1840).viscosity(1000).temperature(273).physics(Physics.WATER),
            () -> LaytheTektoBlocks.CCL_BLOCK.get(), () -> SpaceFluidBlocks.BUCKET_CCL.get());

    /** Loads the declarations; called from {@code SpaceEntry.declare} before the blocks that carry them. */
    public static void init() {
        WorldFluids.enableMilk();
        Fluids.registerAlias(MERCURY::source, () -> Fluids.MERCURY);
        Fluids.registerAlias(BROMINE::source, () -> SpaceFluids.BROMINE);
        Fluids.registerAlias(CCL::source, () -> SpaceFluids.CCL);
    }

    /**
     * Golem Core's world-fluid block properties (1.7 {@code BlockFluidBase}: no collision or shape, hardness 100, no
     * drops), {@code GenericFluidBlock}'s {@code setResistance(500F)} and {@code Material.water}'s replaceability.
     */
    public static BlockBehaviour.Properties properties(BlockBehaviour.Properties p, WorldFluids.Handle fluid) {
        return FluidBlock.properties(p, fluid).strength(100.0F, 500.0F).replaceable();
    }

    /**
     * A world fluid's blockstate: every level shows one model that only names the particle sprite (the fluid renderer
     * draws the fluid itself, as vanilla's water model does); no block item (1.7 had none).
     */
    public static Presets.BlockModel blockstate(String stillSprite) {
        return Presets.shape(Models.parented("minecraft:block/block",
                        Slots.of("particle", "ntmspace:blocks/" + stillSprite)))
                .noItem();
    }
}
