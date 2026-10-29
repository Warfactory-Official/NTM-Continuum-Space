package com.ntmspace.config;

import com.hbm.config.CommonConfig;
import com.hbm.config.Configuration;
import com.ntmspace.world.gen.legacy.MapGenVolcano17;

import java.nio.file.Path;

import net.minecraft.util.Mth;

/**
 * NTM: Space's own settings file, {@code config/ntmspace.cfg}, read the way HBM reads {@code hbm.cfg}: HBM's 1.7-format
 * {@link Configuration} and {@link CommonConfig#createConfigBool}, then {@code save()}, which writes every missing key
 * at its default. The keys are hbm-space's own (its {@code RadiationConfig} etc.), under the category hbm-space put them
 * in, so a 1.7 NTM: Space config can be copied across key by key.
 *
 * <p>Loaded once, the first time a system that reads it starts ({@link #load()} is idempotent). The fields are plain
 * statics like HBM's config classes; GameTests flip them and restore them.
 */
public final class SpaceConfig {

    private SpaceConfig() {
    }

    /**
     * hbm-space {@code RadiationConfig.disableNeutron} ({@code HAZ_01_disableNeutrons}, default true): the neutron
     * activation system ({@code com.ntmspace.fork.neutron.NeutronActivation}) is off unless this is set to false.
     */
    public static boolean disableNeutron = true;
    public static boolean showOreLocations = true;

    // hbm-space SpaceConfig
    public static int maxProbeDistance = 32_000;
    public static final String CATEGORY_DIMENSIONS = "17_dims";
    public static boolean allowNetherPortals = false;
    public static int maxStationDistance = 32_000;
    public static boolean combatPodDespawn = false;

    // hbm-space MobConfig and GeneralConfig
    public static boolean enableMunCows = true;
    public static boolean serverSafety = false;

    /**
     * hbm-space RadiationConfig: 1 = 1 rad/s under the Mun's open sky.
     */
    public static double celestialRadMultiplier = 0.25D;

    // hbm-space WorldConfig: what a drill sucks from one fluid-ore block, and the chance it runs dry
    public static int bedrockOilPerDeposit = 100;
    public static int bedrockGasPerDepositMin = 10;
    public static int bedrockGasPerDepositMax = 50;
    public static int earthOilPerDeposit = 500;
    public static int earthGasPerDepositMin = 100;
    public static int earthGasPerDepositMax = 500;
    public static double earthOilDrainChance = 0.05D;
    public static int dunaOilPerDeposit = 200;
    public static int dunaGasPerDepositMin = 100;
    public static int dunaGasPerDepositMax = 500;
    public static double dunaOilDrainChance = 0.1D;
    public static int laytheOilPerDeposit = 500;
    public static int laytheGasPerDepositMin = 100;
    public static int laytheGasPerDepositMax = 500;
    public static double laytheOilDrainChance = 0.05D;
    public static int tektoOilPerDeposit = 500;
    public static int tektoGasPerDepositMin = 100;
    public static int tektoGasPerDepositMax = 500;
    public static double tektoOilDrainChance = 0.05D;
    public static int tektoBedrockOilPerDeposit = 100;
    public static int tektoBedrockGasPerDepositMin = 10;
    public static int tektoBedrockGasPerDepositMax = 50;
    public static int eveGasPerDeposit = 500;
    public static int evePetPerDepositMin = 20;
    public static int evePetPerDepositMax = 100;
    public static double eveGasDrainChance = 0.05D;
    public static int munBrinePerDeposit = 300;
    public static double munBrineDrainChance = 0.05D;
    public static int minmusBrinePerDeposit = 300;
    public static double minmusBrineDrainChance = 0.05D;
    public static int ikeBrinePerDeposit = 300;
    public static double ikeBrineDrainChance = 0.05D;

    private static boolean loaded;
    private static Configuration configuration;

    /**
     * The declarations and defaults captured by the last successful native configuration load.
     */
    public static Configuration configuration() {
        return configuration;
    }

    public static Path file() {
        return dev.golem.core.platform.ConfigFiles.configDir().resolve("ntmspace.cfg");
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        load(new Configuration(file()));
    }

    /**
     * Reads {@code config} (and writes missing keys back); the GameTests use a scratch file.
     */
    public static void load(Configuration config) {
        config.load();
        // hbm-space RadiationConfig: CATEGORY_HAZ, the same "14_hazard" category as HBM's hazard switches
        disableNeutron = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_HAZARD, "HAZ_01_disableNeutrons",
                "When turned off, all neutron radiation hazards are disabled", true);
        showOreLocations = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_GENERAL, "1.93_showOreLocations",
                "Should ores indicate which planets they can be found on.", true);
        loadSpace(config);
        loadReserves(config);
        config.save();
        configuration = config;
    }

    /**
     * hbm-space SpaceConfig, MobConfig, GeneralConfig and RadiationConfig keys, under their 1.7 categories.
     */
    private static void loadSpace(Configuration config) {
        allowNetherPortals = CommonConfig.createConfigBool(config, CATEGORY_DIMENSIONS, "17.00_allowNetherPortals",
                "Should Nether portals function on other celestial bodies?", false);
        maxProbeDistance = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_GENERAL, "1.90_maxProbeDistance",
                "How far from the center of the dimension can probes generate landing coordinates", maxProbeDistance);
        // port: probes draw nextInt(2 * distance), which overflows above half the int range
        maxProbeDistance = Mth.clamp(maxProbeDistance, 1, Integer.MAX_VALUE / 2);
        maxStationDistance = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_GENERAL,
                "1.93_maxStationDistance",
                "How far from the center of the dimension can orbital stations be generated", maxStationDistance);
        maxStationDistance = Math.max(maxStationDistance, 1024);
        MapGenVolcano17.enableVolcanoGen = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_GENERAL,
                "1.91_enableVolcanoGen",
                "Should volcanoes be active when spawning, disabling will prevent natural volcanoes from spewing lava and growing",
                MapGenVolcano17.enableVolcanoGen);
        combatPodDespawn = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_GENERAL, "1.94_combatPodDespawn",
                "wether combat pods should despawn after a certian amount of time.", combatPodDespawn);
        enableMunCows = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_MOBS, "12.D03_enableMunCows",
                "Whether Mun Cows should spawn on the Mun", enableMunCows);
        serverSafety = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_GENERAL, "1.43_serverSafety",
                "Allows for automated entity culling to function properly.", serverSafety);
        celestialRadMultiplier = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_RADIATION,
                "CELESTIAL_00_radMultiplier",
                "Radiation multiplier on celestial bodies, 1 = 1 rad/s on the Mun", celestialRadMultiplier);
    }

    private static void loadReserves(Configuration config) {
        bedrockOilPerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O00_bedrockOilPerDeposit",
                "Oil extracted per bedrock oil block suck", bedrockOilPerDeposit);
        bedrockGasPerDepositMin = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O01_bedrockGasPerDepositMin",
                "Minimum natural gas extracted per bedrock oil block suck", bedrockGasPerDepositMin);
        bedrockGasPerDepositMax = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O02_bedrockGasPerDepositMax",
                "Maximum natural gas extracted per bedrock oil block suck", bedrockGasPerDepositMax);
        earthOilPerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O03_earthOilPerDeposit",
                "Oil extracted per Earth oil block suck", earthOilPerDeposit);
        earthGasPerDepositMin = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O04_earthGasPerDepositMin",
                "Minimum natural gas extracted per Earth oil block suck", earthGasPerDepositMin);
        earthGasPerDepositMax = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O05_earthGasPerDepositMax",
                "Maximum natural gas extracted per Earth oil block suck", earthGasPerDepositMax);
        earthOilDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O06_earthOilDrainChance",
                "Chance for an Earth oil block to become empty on suck", earthOilDrainChance);
        dunaOilPerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES, "2.O07_dunaOilPerDeposit",
                "Oil extracted per Duna oil block suck", dunaOilPerDeposit);
        dunaGasPerDepositMin = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O08_dunaGasPerDepositMin",
                "Minimum natural gas extracted per Duna oil block suck", dunaGasPerDepositMin);
        dunaGasPerDepositMax = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O09_dunaGasPerDepositMax",
                "Maximum natural gas extracted per Duna oil block suck", dunaGasPerDepositMax);
        dunaOilDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O10_dunaOilDrainChance",
                "Chance for a Duna oil block to become empty on suck", dunaOilDrainChance);
        laytheOilPerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O11_laytheOilPerDeposit",
                "Desulfurized Oil extracted per Laythe oil block suck", laytheOilPerDeposit);
        laytheGasPerDepositMin = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O12_laytheGasPerDepositMin",
                "Minimum natural gas extracted per Laythe oil block suck", laytheGasPerDepositMin);
        laytheGasPerDepositMax = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O13_laytheGasPerDepositMax",
                "Maximum natural gas extracted per Laythe oil block suck", laytheGasPerDepositMax);
        laytheOilDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O14_laytheOilDrainChance",
                "Chance for a Laythe oil block to become empty on suck", laytheOilDrainChance);
        eveGasPerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES, "2.O15_eveGasPerDeposit",
                "Natural Gas extracted per Eve gas block suck", eveGasPerDeposit);
        evePetPerDepositMin = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O16_evePetPerDepositMin",
                "Minimum petroleum gas extracted per Eve oil block suck", evePetPerDepositMin);
        evePetPerDepositMax = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O17_evePetPerDepositMax",
                "Maximum petroleum gas extracted per Eve oil block suck", evePetPerDepositMax);
        eveGasDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O18_eveGasDrainChance",
                "Chance for an Eve gas block to become empty on suck", eveGasDrainChance);
        munBrinePerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O19_munBrinePerDeposit",
                "Brine extracted per Mun brine block suck", munBrinePerDeposit);
        munBrineDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O20_munBrineDrainChance",
                "Chance for an Mun brine block to become empty on suck", munBrineDrainChance);
        minmusBrinePerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O21_minmusBrinePerDeposit",
                "Brine extracted per Minmus brine block suck", minmusBrinePerDeposit);
        minmusBrineDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O22_minmusBrineDrainChance",
                "Chance for an Minmus brine block to become empty on suck", minmusBrineDrainChance);
        ikeBrinePerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O23_ikeBrinePerDeposit",
                "Brine extracted per Ike brine block suck", ikeBrinePerDeposit);
        ikeBrineDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O24_ikeBrineDrainChance",
                "Chance for an Ike brine block to become empty on suck", ikeBrineDrainChance);
        tektoOilPerDeposit = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O25_tektoOilPerDeposit",
                "Oil extracted per Tekto oil block suck", tektoOilPerDeposit);
        tektoGasPerDepositMin = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O26_tektoGasPerDepositMin",
                "Minimum natural gas extracted per Tekto oil block suck", tektoGasPerDepositMin);
        tektoGasPerDepositMax = CommonConfig.createConfigInt(config, CommonConfig.CATEGORY_ORES,
                "2.O27_tektoGasPerDepositMax",
                "Maximum natural gas extracted per Tekto oil block suck", tektoGasPerDepositMax);
        tektoOilDrainChance = CommonConfig.createConfigDouble(config, CommonConfig.CATEGORY_ORES,
                "2.O28_tektoOilDrainChance",
                "Chance for a Tekto oil block to become empty on suck", tektoOilDrainChance);
    }
}
