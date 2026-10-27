package com.ntmspace.fork;

import com.hbm.api.data.DepositDataMaps;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.config.SpaceConfig;
import com.ntmspace.rocket.DriveBody;
import java.util.function.DoubleSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A fluid reserve bound to an actual BODY state. ntmspace.cfg supplies independent absolute amounts and ranges.
 * The resolved data-map metadata remains authoritative for fluids, empty blocks and fracking requirements;
 * changes to its yield coefficients apply relative to SpaceDeposits' authored default coefficients.
 */
public final class SpaceReserve implements DepositDataMaps.Reserve {
    private final DepositDataMaps.Deposit metadata;
    private final DriveBody body;
    private final Setting primary;
    private final Setting minimum;
    private final Setting maximum;
    private final @Nullable Setting chance;

    /** A {@code ntmspace.cfg} value and its hbm-space default, which the data-map coefficients are relative to. */
    private record Setting(DoubleSupplier value, double defaultValue) {
        double get() {
            return value.getAsDouble();
        }
    }

    private SpaceReserve(DriveBody body, DepositDataMaps.Deposit metadata, boolean bedrock) {
        this.body = body;
        this.metadata = metadata;
        if (bedrock) {
            var tekto = body == DriveBody.TEKTO;
            primary = tekto ? new Setting(() -> SpaceConfig.tektoBedrockOilPerDeposit, 100)
                    : new Setting(() -> SpaceConfig.bedrockOilPerDeposit, 100);
            minimum = tekto ? new Setting(() -> SpaceConfig.tektoBedrockGasPerDepositMin, 10)
                    : new Setting(() -> SpaceConfig.bedrockGasPerDepositMin, 10);
            maximum = tekto ? new Setting(() -> SpaceConfig.tektoBedrockGasPerDepositMax, 50)
                    : new Setting(() -> SpaceConfig.bedrockGasPerDepositMax, 50);
            chance = null;
            return;
        }
        Setting earthMin = new Setting(() -> SpaceConfig.earthGasPerDepositMin, 100);
        Setting earthMax = new Setting(() -> SpaceConfig.earthGasPerDepositMax, 500);
        switch (body) {
            case DUNA -> {
                primary = new Setting(() -> SpaceConfig.dunaOilPerDeposit, 200);
                minimum = new Setting(() -> SpaceConfig.dunaGasPerDepositMin, 100);
                maximum = new Setting(() -> SpaceConfig.dunaGasPerDepositMax, 500);
                chance = new Setting(() -> SpaceConfig.dunaOilDrainChance, 0.1D);
            }
            case LAYTHE -> {
                primary = new Setting(() -> SpaceConfig.laytheOilPerDeposit, 500);
                minimum = new Setting(() -> SpaceConfig.laytheGasPerDepositMin, 100);
                maximum = new Setting(() -> SpaceConfig.laytheGasPerDepositMax, 500);
                chance = new Setting(() -> SpaceConfig.laytheOilDrainChance, 0.05D);
            }
            case EVE -> {
                primary = new Setting(() -> SpaceConfig.eveGasPerDeposit, 500);
                minimum = new Setting(() -> SpaceConfig.evePetPerDepositMin, 20);
                maximum = new Setting(() -> SpaceConfig.evePetPerDepositMax, 100);
                chance = new Setting(() -> SpaceConfig.eveGasDrainChance, 0.05D);
            }
            case TEKTO -> {
                primary = new Setting(() -> SpaceConfig.tektoOilPerDeposit, 500);
                minimum = new Setting(() -> SpaceConfig.tektoGasPerDepositMin, 100);
                maximum = new Setting(() -> SpaceConfig.tektoGasPerDepositMax, 500);
                chance = new Setting(() -> SpaceConfig.tektoOilDrainChance, 0.05D);
            }
            case MUN -> {
                primary = new Setting(() -> SpaceConfig.munBrinePerDeposit, 300);
                minimum = earthMin; maximum = earthMax;
                chance = new Setting(() -> SpaceConfig.munBrineDrainChance, 0.05D);
            }
            case MINMUS -> {
                primary = new Setting(() -> SpaceConfig.minmusBrinePerDeposit, 300);
                minimum = earthMin; maximum = earthMax;
                chance = new Setting(() -> SpaceConfig.minmusBrineDrainChance, 0.05D);
            }
            case IKE -> {
                primary = new Setting(() -> SpaceConfig.ikeBrinePerDeposit, 300);
                minimum = earthMin; maximum = earthMax;
                chance = new Setting(() -> SpaceConfig.ikeBrineDrainChance, 0.05D);
            }
            default -> {
                primary = new Setting(() -> SpaceConfig.earthOilPerDeposit, 500);
                minimum = earthMin; maximum = earthMax;
                chance = new Setting(() -> SpaceConfig.earthOilDrainChance, 0.05D);
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
        var min = (int) minimum.get();
        var max = (int) maximum.get();
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
