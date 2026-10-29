package com.ntmspace.world;

import com.ntmspace.orbit.Orbit;
import com.ntmspace.weather.DustStorm;
import dev.golem.core.world.WorldAttributes;
import dev.golem.core.world.TimeOfDay;

/** Vanilla gameplay queries use the same live storm state as the body's sky and daylight. */
public final class SpaceEnvironmentAttributes {
    private SpaceEnvironmentAttributes() {
    }

    public static void init() {
        TimeOfDay.fixedClock(Orbit.ORBIT, 6000L);
        WorldAttributes.MONSTERS_BURN.register(
                (level, unusedPos, burning) -> burning && !DustStorm.obscuresDaylight(level));
        WorldAttributes.BED_SLEEP.register(
                (level, unusedPos, sleeping) -> sleeping || DustStorm.obscuresDaylight(level));
    }
}
