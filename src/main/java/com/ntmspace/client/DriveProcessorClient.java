package com.ntmspace.client;

import com.ntmspace.client.gui.rocket.GUIMachineDriveProcessor;
import com.ntmspace.client.render.rocket.RenderDriveProcessor;
import com.ntmspace.registry.DriveProcessorBlocks;
import dev.golem.core.client.ClientRegistrations;

/** The drive processor's client start-up (hbm-space {@code ClientProxy}): its screen and block entity renderer. */
public final class DriveProcessorClient {

    private DriveProcessorClient() {}

    public static void init() {
        ClientRegistrations.screens(screens -> screens.register(DriveProcessorBlocks.DRIVE_PROCESSOR_MENU.get(), GUIMachineDriveProcessor::new));
        ClientRegistrations.renderers(r -> r.blockEntity(DriveProcessorBlocks.DRIVE_PROCESSOR_BE.get(), context -> new RenderDriveProcessor()));
    }
}
