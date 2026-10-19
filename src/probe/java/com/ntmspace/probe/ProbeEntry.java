package com.ntmspace.probe;

import dev.golem.core.platform.ModEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The probe addon: a dev-only second mod built like any HBM addon, which uses every extension point NTM: Space uses
 * (scratchpad m2/probe-hook-families.md), always in a way that meets NTM: Space's use: the same hook (in a phase before
 * or after NTM: Space's), the same registry, a second patch to the same data map, fluid or recipe table, a second claim
 * on what NTM: Space owns. Its GameTests ({@code ntmprobe:*}) and the harness scenario {@code ProbeCoexistence} prove
 * the two addons' contributions apply together under the documented ordering and merge rules. Every probe answer is
 * inert until a test marks it ({@link ProbeMarks}), so a dev run plays as without the probe.
 */
public final class ProbeEntry implements ModEntry {

    public static final String MOD_ID = "ntmprobe";
    public static final Logger LOG = LoggerFactory.getLogger("NTM Probe");

    /** The HBM item the probe substitutes (NTM: Space substitutes {@code hbm:fan} and {@code hbm:oil_detector}). */
    public static final String SUBSTITUTED_ITEM = "hbm:hs_vault";

    /**
     * With {@code -Dntmprobe.doubleSubstitution=true} (Gradle {@code -Pprobe.doubleSubstitution=true}) the probe also
     * substitutes {@code hbm:fan}, which NTM: Space already does: start-up must then fail, naming both mods. Off by
     * default, or no dev run would start.
     */
    @Override
    public void substitute() {
        // a different HBM id than NTM: Space's: both substitutions apply
        dev.golem.core.registry.Substitutions.item(MOD_ID, SUBSTITUTED_ITEM, ProbeSubstitutedItem::new);
        if (Boolean.getBoolean("ntmprobe.doubleSubstitution")) {
            dev.golem.core.registry.Substitutions.block(MOD_ID, "hbm:fan",
                    p -> new com.hbm.blocks.machine.MachineFan(p, () -> com.hbm.tileentity.ModBlockEntities.MACHINE_FAN.get()));
        }
    }

    @Override
    public void declare() {
        LOG.info("Probe addon declaring");
        ProbeContent.init();
        ProbeRecords.init();
        ProbeHooks.init();
        ProbeWorld.init();
        ProbeNet.init();
        ProbeGameTests.init();
    }

    @Override
    public void registered() {
        ProbeWorld.registered();
    }

    /** The probe's subclass of HBM's plain item, built from HBM's own registration properties. */
    public static final class ProbeSubstitutedItem extends net.minecraft.world.item.Item {
        public ProbeSubstitutedItem(Properties properties) {
            super(properties);
        }
    }
}
