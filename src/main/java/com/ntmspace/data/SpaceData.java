package com.ntmspace.data;

import dev.golem.core.Ids;
import dev.golem.core.data.DataGroup;
import dev.golem.core.data.DataGroups;
import dev.golem.core.registry.DatapackRegistries;
import com.ntmspace.NtmSpace;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;

/** 1.7 SpaceConfig, WorldConfig, and GeneralConfig values in Space's server datapack, read at use. */
public final class SpaceData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(Ids.of(NtmSpace.MOD_ID, "space_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    // 1.7: SpaceConfig:103, 118-119, 123
    private static final DataGroups.Group TRAVEL = group("travel");
    public static final DataGroups.Param<Boolean> ALLOW_NETHER_PORTALS = TRAVEL.bool("allow_nether_portals", false);
    // 1.7 guarded nothing; probes draw nextInt(2 * distance)
    public static final DataGroups.Param<Integer> MAX_PROBE_DISTANCE =
            TRAVEL.bounded("max_probe_distance", 32_000, 1, Integer.MAX_VALUE / 2);
    public static final DataGroups.Param<Integer> MAX_STATION_DISTANCE =
            TRAVEL.bounded("max_station_distance", 32_000, 1024, Integer.MAX_VALUE);
    public static final DataGroups.Param<Boolean> COMBAT_POD_DESPAWN = TRAVEL.bool("combat_pod_despawn", false);

    // 1.7: SpaceConfig:120
    private static final DataGroups.Group WORLDGEN = group("worldgen");
    public static final DataGroups.Param<Boolean> ENABLE_VOLCANO_GEN = WORLDGEN.bool("enable_volcano_gen", true);

    // 1.7: MobConfig 12.D03_enableMunCows
    private static final DataGroups.Group MOBS = group("mobs");
    public static final DataGroups.Param<Boolean> ENABLE_MUN_COWS = MOBS.bool("enable_mun_cows", true);
    // 1.7: GeneralConfig 1.43_serverSafety - repeated kills cull Quackos
    public static final DataGroups.Param<Boolean> SERVER_SAFETY = MOBS.bool("server_safety", false);

    // 1.7: WorldConfig:51-93 - what a drill sucks from one fluid-ore block, and the chance it runs dry
    private static final DataGroups.Group RESERVES = group("reserves");
    public static final DataGroups.Param<Integer> BEDROCK_OIL_PER_DEPOSIT =
            RESERVES.integer("bedrock_oil_per_deposit", 100);
    public static final DataGroups.Param<Integer> BEDROCK_GAS_MIN = RESERVES.integer("bedrock_gas_per_deposit_min", 10);
    public static final DataGroups.Param<Integer> BEDROCK_GAS_MAX = RESERVES.integer("bedrock_gas_per_deposit_max", 50);
    public static final DataGroups.Param<Integer> EARTH_OIL_PER_DEPOSIT =
            RESERVES.integer("earth_oil_per_deposit", 500);
    public static final DataGroups.Param<Integer> EARTH_GAS_MIN = RESERVES.integer("earth_gas_per_deposit_min", 100);
    public static final DataGroups.Param<Integer> EARTH_GAS_MAX = RESERVES.integer("earth_gas_per_deposit_max", 500);
    public static final DataGroups.Param<Double> EARTH_OIL_DRAIN = RESERVES.chance("earth_oil_drain_chance", 0.05D);
    public static final DataGroups.Param<Integer> DUNA_OIL_PER_DEPOSIT = RESERVES.integer("duna_oil_per_deposit", 200);
    public static final DataGroups.Param<Integer> DUNA_GAS_MIN = RESERVES.integer("duna_gas_per_deposit_min", 100);
    public static final DataGroups.Param<Integer> DUNA_GAS_MAX = RESERVES.integer("duna_gas_per_deposit_max", 500);
    public static final DataGroups.Param<Double> DUNA_OIL_DRAIN = RESERVES.chance("duna_oil_drain_chance", 0.1D);
    public static final DataGroups.Param<Integer> LAYTHE_OIL_PER_DEPOSIT =
            RESERVES.integer("laythe_oil_per_deposit", 500);
    public static final DataGroups.Param<Integer> LAYTHE_GAS_MIN = RESERVES.integer("laythe_gas_per_deposit_min", 100);
    public static final DataGroups.Param<Integer> LAYTHE_GAS_MAX = RESERVES.integer("laythe_gas_per_deposit_max", 500);
    public static final DataGroups.Param<Double> LAYTHE_OIL_DRAIN = RESERVES.chance("laythe_oil_drain_chance", 0.05D);
    public static final DataGroups.Param<Integer> TEKTO_OIL_PER_DEPOSIT =
            RESERVES.integer("tekto_oil_per_deposit", 500);
    public static final DataGroups.Param<Integer> TEKTO_GAS_MIN = RESERVES.integer("tekto_gas_per_deposit_min", 100);
    public static final DataGroups.Param<Integer> TEKTO_GAS_MAX = RESERVES.integer("tekto_gas_per_deposit_max", 500);
    public static final DataGroups.Param<Double> TEKTO_OIL_DRAIN = RESERVES.chance("tekto_oil_drain_chance", 0.05D);
    public static final DataGroups.Param<Integer> TEKTO_BEDROCK_OIL_PER_DEPOSIT =
            RESERVES.integer("tekto_bedrock_oil_per_deposit", 100);
    public static final DataGroups.Param<Integer> TEKTO_BEDROCK_GAS_MIN =
            RESERVES.integer("tekto_bedrock_gas_per_deposit_min", 10);
    public static final DataGroups.Param<Integer> TEKTO_BEDROCK_GAS_MAX =
            RESERVES.integer("tekto_bedrock_gas_per_deposit_max", 50);
    public static final DataGroups.Param<Integer> EVE_GAS_PER_DEPOSIT = RESERVES.integer("eve_gas_per_deposit", 500);
    public static final DataGroups.Param<Integer> EVE_PET_MIN = RESERVES.integer("eve_petroleum_per_deposit_min", 20);
    public static final DataGroups.Param<Integer> EVE_PET_MAX = RESERVES.integer("eve_petroleum_per_deposit_max", 100);
    public static final DataGroups.Param<Double> EVE_GAS_DRAIN = RESERVES.chance("eve_gas_drain_chance", 0.05D);
    public static final DataGroups.Param<Integer> MUN_BRINE_PER_DEPOSIT =
            RESERVES.integer("mun_brine_per_deposit", 300);
    public static final DataGroups.Param<Double> MUN_BRINE_DRAIN = RESERVES.chance("mun_brine_drain_chance", 0.05D);
    public static final DataGroups.Param<Integer> MINMUS_BRINE_PER_DEPOSIT =
            RESERVES.integer("minmus_brine_per_deposit", 300);
    public static final DataGroups.Param<Double> MINMUS_BRINE_DRAIN =
            RESERVES.chance("minmus_brine_drain_chance", 0.05D);
    public static final DataGroups.Param<Integer> IKE_BRINE_PER_DEPOSIT =
            RESERVES.integer("ike_brine_per_deposit", 300);
    public static final DataGroups.Param<Double> IKE_BRINE_DRAIN = RESERVES.chance("ike_brine_drain_chance", 0.05D);

    // 1.7: RadiationConfig:18, 55 - 1 = 1 rad/s under the Mun's open sky
    private static final DataGroups.Group RADIATION = group("radiation");
    public static final DataGroups.Param<Double> CELESTIAL_RAD_MULTIPLIER =
            RADIATION.real("celestial_rad_multiplier", 0.25D);

    // 1.7: TileEntityRadiator's IConfigurableMachine "radiator"
    private static final DataGroups.Group RADIATOR = group("radiator");
    public static final DataGroups.Param<Integer> RADIATOR_INPUT_TANK_SIZE =
            RADIATOR.integer("input_tank_capacity", 500);
    public static final DataGroups.Param<Integer> RADIATOR_OUTPUT_TANK_SIZE =
            RADIATOR.integer("output_tank_capacity", 500);

    static {
        DatapackRegistries.registerSynced(REGISTRY, DataGroup.CODEC);
        DataGroups.install(REGISTRY, GROUPS);
    }

    public static void init() {}

    private SpaceData() {
    }

    private static DataGroups.Group group(String name) {
        return DataGroups.group(GROUPS, Ids.of(NtmSpace.MOD_ID, name));
    }

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
