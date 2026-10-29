package com.ntmspace.fork;

import com.hbm.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.ntmspace.world.SpaceSkies;

/**
 * Space's BlockPedestal interprets NEW_MOON as an eclipse under a high sun on celestial worlds.
 */
public final class SpacePedestalConditions {

    public static final PedestalExtraCondition NEW_MOON = PedestalExtraCondition.register(
            "ntmspace:new_moon", PedestalExtraCondition.NEW_MOON.icon, SpacePedestalConditions::newMoon);

    private SpacePedestalConditions() {
    }

    private static boolean newMoon(PedestalExtraCondition.Site site) {
        var level = site.level();
        if (SpaceSkies.body(level) == null) return PedestalExtraCondition.NEW_MOON.holds(site);
        var angle = SpaceSkies.angle(level, 0.0F);
        return (angle <= 0.15F || angle >= 0.85F) && SpaceSkies.eclipse(level) > 0.0D;
    }

    public static void init() {
    }
}
