package com.ntmspace.orbit;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * hbm-space's {@code api.hbm.tile.IPropulsion}: an engine that moves the orbital station it stands in. A station travels
 * only when every one of its registered engines can perform the burn; the thrust of all of them sets the travel time.
 *
 * <p>An engine registers itself ({@link #registerPropulsion}) on its first server tick in orbit (only when it faces
 * prograde, for the directional thrusters) and unregisters when removed or unloaded, as 1.7 did. Engines of other mods
 * implement this on their block entity the same way.
 */
public interface Propulsion {

    /** Registers this engine with the station whose grid square it stands in. Server, orbit level only. */
    default void registerPropulsion() {
        OrbitalStation.addPropulsion(this);
    }

    /** And removes it again (block removed, chunk unloaded). */
    default void unregisterPropulsion() {
        OrbitalStation.removePropulsion(this);
    }

    /** The block entity this engine is. */
    BlockEntity getBlockEntity();

    /** Is the engine fuelled to change the velocity of {@code shipMass} kg by {@code deltaV} m/s. */
    boolean canPerformBurn(int shipMass, double deltaV);

    /** Why {@link #canPerformBurn} failed, one line each (the computer's look overlay lists them). */
    void addErrors(List<Component> errors);

    /** Thrust in newtons; higher means faster travel. */
    float getThrust();

    /** Turns the engine on (consuming its fuel); the ticks before it provides thrust. */
    int startBurn();

    /** Turns the engine off; the ticks before another journey can start. */
    int endBurn();
}
