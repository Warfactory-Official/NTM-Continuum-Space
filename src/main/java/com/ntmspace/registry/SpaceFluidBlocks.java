package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.SpaceFluidBlock;
import com.ntmspace.fluid.SpaceWorldFluids;
import dev.golem.core.fluid.WorldFluids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;

/**
 * hbm-space's placeable world fluids and their buckets ({@code ModBlocks}/{@code ModItems}): {@code mercury_block}
 * (Eve's seas), {@code bromine_block} (Ike's lakes) and the bucket of Tekto's {@code ccl_block}
 * ({@link LaytheTektoBlocks#CCL_BLOCK}). The buckets are vanilla buckets of the {@link SpaceWorldFluids}; their network
 * fluids ({@code hbm:mercury}, {@code ntmspace:bromine}, {@code ntmspace:ccl}) come from the fluid container data map.
 */
public final class SpaceFluidBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS);

    /** 1.7: {@code GenericFluidBlock(mercury_fluid, Material.water, "mercury_still", "mercury_flowing")}, resistance 500. */
    public static final RegistryObject<SpaceFluidBlock> MERCURY_BLOCK = REG.block("mercury_block",
                    p -> new SpaceFluidBlock(p, SpaceWorldFluids.MERCURY),
                    p -> SpaceWorldFluids.properties(p, SpaceWorldFluids.MERCURY))
            .transform(SpaceWorldFluids.blockstate("mercury_still"));

    /** 1.7: {@code GenericFluidBlock(bromine_fluid, Material.water, "bromine_still", "bromine_flowing")}, resistance 500. */
    public static final RegistryObject<SpaceFluidBlock> BROMINE_BLOCK = REG.block("bromine_block",
                    p -> new SpaceFluidBlock(p, SpaceWorldFluids.BROMINE),
                    p -> SpaceWorldFluids.properties(p, SpaceWorldFluids.BROMINE))
            .transform(SpaceWorldFluids.blockstate("bromine_still"));

    /** 1.7: {@code ItemModBucket(mercury_block)}, container item the bucket, in the blocks tab. */
    public static final RegistryObject<BucketItem> BUCKET_MERCURY = bucket("bucket_mercury", SpaceWorldFluids.MERCURY);
    public static final RegistryObject<BucketItem> BUCKET_BROMINE = bucket("bucket_bromine", SpaceWorldFluids.BROMINE);
    public static final RegistryObject<BucketItem> BUCKET_CCL = bucket("bucket_ccl", SpaceWorldFluids.CCL);

    private SpaceFluidBlocks() {}

    public static void init() {}

    private static RegistryObject<BucketItem> bucket(String name, WorldFluids.Handle fluid) {
        return REG.item(name, p -> WorldFluids.bucket(fluid, p), p -> p.stacksTo(1).craftRemainder(Items.BUCKET))
                .transform(Presets.sprite()).tab(HbmTab.BLOCKS);
    }
}
