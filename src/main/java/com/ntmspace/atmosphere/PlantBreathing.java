package com.ntmspace.atmosphere;

import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.Ids;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code IPlantableBreathing}: plants that live on another air than the default (a plant otherwise needs
 * oxygen above 0.01 atm or earth air above 0.1). Rules are kept by block id, so a block registered by another family or
 * another mod gets its rule as soon as it exists; {@link #register} adds one.
 */
public final class PlantBreathing {

    private PlantBreathing() {}

    /** Whether the plant can live in {@code atmosphere} (null: no air at all). */
    @FunctionalInterface
    public interface Rule {
        boolean canBreathe(@Nullable CBT_Atmosphere atmosphere);
    }

    private static final Map<String, Rule> BY_ID = new LinkedHashMap<>();
    private static final Map<Block, Rule> RESOLVED = new ConcurrentHashMap<>();
    private static final Rule NONE = atmosphere -> true;

    static {
        // hbm-space ModBlocks: BlockCrop(..., atmospherePredicate); a crop with no air at all cannot breathe
        Rule earthy = atmosphere -> atmosphere != null
                && (atmosphere.hasFluid(SpaceFluids.EARTHAIR, 0.1) || atmosphere.hasFluid(Fluids.OXYGEN, 0.1));
        Rule tekto = atmosphere -> atmosphere != null
                && (atmosphere.hasFluid(SpaceFluids.TEKTOAIR, 0.1) || atmosphere.hasFluid(Fluids.CHLORINE, 0.1));
        register("ntmspace:crop_strawberry", earthy);
        register("ntmspace:crop_mint", earthy);
        register("ntmspace:crop_coffee", earthy);
        register("ntmspace:crop_tea", earthy);
        register("ntmspace:crop_paraffin", tekto);
        // BlockNTSapling splits (1.7 block name "sapling"): Tekto air or chlorine
        register("ntmspace:sapling_vinyl", tekto);
        register("ntmspace:sapling_pvc", tekto);
    }

    /** Gives the block with id {@code blockId} its own air rule. */
    public static synchronized void register(String blockId, Rule rule) {
        BY_ID.put(blockId, rule);
        RESOLVED.clear();
    }

    /** The rule of {@code block}, or null for the default rule. */
    public static @Nullable Rule of(Block block) {
        Rule rule = RESOLVED.computeIfAbsent(block, PlantBreathing::resolve);
        return rule == NONE ? null : rule;
    }

    private static synchronized Rule resolve(Block block) {
        for (Map.Entry<String, Rule> entry : BY_ID.entrySet()) {
            var found = BuiltInRegistries.BLOCK.getOptional(Ids.parse(entry.getKey()));
            if (found.isPresent() && found.get() == block) return entry.getValue();
        }
        return NONE;
    }
}
