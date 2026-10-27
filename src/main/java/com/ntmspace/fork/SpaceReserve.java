package com.ntmspace.fork;

import com.hbm.api.data.DepositDataMaps;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.data.SpaceData;
import com.ntmspace.rocket.DriveBody;
import dev.golem.core.data.DataGroups;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A fluid reserve bound to an actual BODY state. SpaceData supplies independent absolute amounts and ranges.
 * The resolved data-map metadata remains authoritative for fluids, empty blocks and fracking requirements;
 * changes to its yield coefficients apply relative to SpaceDeposits' authored default coefficients.
 */
public final class SpaceReserve implements DepositDataMaps.Reserve {
    private final DepositDataMaps.Deposit metadata;
    private final DriveBody body;
    private final DataGroups.Param<Integer> primary;
    private final DataGroups.Param<Integer> minimum;
    private final DataGroups.Param<Integer> maximum;
    private final DataGroups.@Nullable Param<Double> chance;

    private SpaceReserve(DriveBody body, DepositDataMaps.Deposit metadata, boolean bedrock) {
        this.body = body;
        this.metadata = metadata;
        if (bedrock) {
            primary = body == DriveBody.TEKTO ? SpaceData.TEKTO_BEDROCK_OIL_PER_DEPOSIT : SpaceData.BEDROCK_OIL_PER_DEPOSIT;
            minimum = body == DriveBody.TEKTO ? SpaceData.TEKTO_BEDROCK_GAS_MIN : SpaceData.BEDROCK_GAS_MIN;
            maximum = body == DriveBody.TEKTO ? SpaceData.TEKTO_BEDROCK_GAS_MAX : SpaceData.BEDROCK_GAS_MAX;
            chance = null;
            return;
        }
        switch (body) {
            case DUNA -> {
                primary = SpaceData.DUNA_OIL_PER_DEPOSIT;
                minimum = SpaceData.DUNA_GAS_MIN; maximum = SpaceData.DUNA_GAS_MAX; chance = SpaceData.DUNA_OIL_DRAIN;
            }
            case LAYTHE -> {
                primary = SpaceData.LAYTHE_OIL_PER_DEPOSIT;
                minimum = SpaceData.LAYTHE_GAS_MIN; maximum = SpaceData.LAYTHE_GAS_MAX; chance = SpaceData.LAYTHE_OIL_DRAIN;
            }
            case EVE -> {
                primary = SpaceData.EVE_GAS_PER_DEPOSIT;
                minimum = SpaceData.EVE_PET_MIN; maximum = SpaceData.EVE_PET_MAX; chance = SpaceData.EVE_GAS_DRAIN;
            }
            case TEKTO -> {
                primary = SpaceData.TEKTO_OIL_PER_DEPOSIT;
                minimum = SpaceData.TEKTO_GAS_MIN; maximum = SpaceData.TEKTO_GAS_MAX; chance = SpaceData.TEKTO_OIL_DRAIN;
            }
            case MUN -> {
                primary = SpaceData.MUN_BRINE_PER_DEPOSIT;
                minimum = SpaceData.EARTH_GAS_MIN; maximum = SpaceData.EARTH_GAS_MAX; chance = SpaceData.MUN_BRINE_DRAIN;
            }
            case MINMUS -> {
                primary = SpaceData.MINMUS_BRINE_PER_DEPOSIT;
                minimum = SpaceData.EARTH_GAS_MIN; maximum = SpaceData.EARTH_GAS_MAX; chance = SpaceData.MINMUS_BRINE_DRAIN;
            }
            case IKE -> {
                primary = SpaceData.IKE_BRINE_PER_DEPOSIT;
                minimum = SpaceData.EARTH_GAS_MIN; maximum = SpaceData.EARTH_GAS_MAX; chance = SpaceData.IKE_BRINE_DRAIN;
            }
            default -> {
                primary = SpaceData.EARTH_OIL_PER_DEPOSIT;
                minimum = SpaceData.EARTH_GAS_MIN; maximum = SpaceData.EARTH_GAS_MAX; chance = SpaceData.EARTH_OIL_DRAIN;
            }
        }
    }

    public DriveBody body() { return body; }
    @Override public DepositDataMaps.Deposit deposit() { return metadata; }

    @Override
    public int primaryAmount() {
        return (int) Math.round(primary.get() * metadata.yield() / (primary.defaultValue() / 500D));
    }

    @Override
    public int secondaryAmount(RandomSource random) {
        if (secondaryFluid() == null) return 0;
        var min = minimum.get();
        var max = maximum.get();
        // Space Reserve.between / BobMathUtil.randIntBetween has an exclusive upper bound.
        var sampled = min >= max ? min : min + random.nextInt(max - min);
        var configuredScale = metadata.secondaryYield().orElse(metadata.yield()) / (minimum.defaultValue() / 100D);
        return (int) Math.round(sampled * configuredScale);
    }

    @Override
    public double drainChance() {
        return chance == null ? 0D : chance.get() * metadata.drain() / (chance.defaultValue() / 0.05D);
    }

    /** Unknown states keep the host's normal multiplicative data-map behavior. */
    public static @Nullable SpaceReserve bind(BlockState state, DepositDataMaps.Deposit metadata) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return switch (id) {
            case "hbm:ore_oil", "hbm:ore_bedrock_oil" -> new SpaceReserve(DriveBody.KERBIN, metadata, id.endsWith(":ore_bedrock_oil"));
            case "ntmspace:ore_oil", "ntmspace:ore_gas", "ntmspace:ore_brine", "ntmspace:ore_tekto", "ntmspace:ore_bedrock_oil" ->
                    new SpaceReserve(DriveBody.valueOf(state.getValue(BlockBodyOre.BODY).name()), metadata, id.endsWith(":ore_bedrock_oil"));
            default -> null;
        };
    }
}
