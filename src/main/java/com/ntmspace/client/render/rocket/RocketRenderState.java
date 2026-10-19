package com.ntmspace.client.render.rocket;

import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.rocket.RocketStruct;
import dev.golem.core.render.CompatEntityRenderState;
import org.jspecify.annotations.Nullable;

/** What a frame of the rideable rocket needs, taken from the entity before it is drawn. */
public final class RocketRenderState extends CompatEntityRenderState {
    public float yaw;
    public float pitch;
    public @Nullable RocketStruct rocket;
    public RocketState state = RocketState.AWAITING;
    public int stateTimer;
    public float interp;
    public boolean inOrbit;
    public int decoupleTimer;
    public int shroudTimer;
    public double motionY;
    public boolean preview;
}
