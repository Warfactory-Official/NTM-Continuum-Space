package com.ntmspace.fluid;

import com.ntmspace.NtmSpace;
import dev.golem.core.dejson.Tags;

/** Conventional token-fluid memberships and the water physics of Space's substituted Schrabidic acid. */
public final class SpaceFluidTags {
    static {
        Tags.member("fluid", "minecraft", "water", "hbm:schrabidic_fluid");
        Tags.member("fluid", "minecraft", "water", "hbm:flowing_schrabidic_fluid");
        Tags.member("fluid", "c", "milk", NtmSpace.MOD_ID + ":milk");
        Tags.member("fluid", "c", "honey", NtmSpace.MOD_ID + ":honey");
    }

    private SpaceFluidTags() {}
}
