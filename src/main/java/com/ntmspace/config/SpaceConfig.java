package com.ntmspace.config;

import com.hbm.config.CommonConfig;
import com.hbm.config.Configuration;
import java.nio.file.Path;

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

    private SpaceConfig() {}

    /**
     * hbm-space {@code RadiationConfig.disableNeutron} ({@code HAZ_01_disableNeutrons}, default true): the neutron
     * activation system ({@code com.ntmspace.fork.neutron.NeutronActivation}) is off unless this is set to false.
     */
    public static boolean disableNeutron = true;

    private static boolean loaded;

    public static Path file() {
        return dev.golem.core.platform.ConfigFiles.configDir().resolve("ntmspace.cfg");
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        load(new Configuration(file()));
    }

    /** Reads {@code config} (and writes missing keys back); the GameTests use a scratch file. */
    public static void load(Configuration config) {
        config.load();
        // hbm-space RadiationConfig: CATEGORY_HAZ, the same "14_hazard" category as HBM's hazard switches
        disableNeutron = CommonConfig.createConfigBool(config, CommonConfig.CATEGORY_HAZARD, "HAZ_01_disableNeutrons",
                "When turned off, all neutron radiation hazards are disabled", true);
        config.save();
    }
}
