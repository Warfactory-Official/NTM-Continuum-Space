package com.ntmspace.client;

import com.ntmspace.tileentity.machine.StardarHeightmap;
import com.hbm.client.model.ObjBlockModels;
/** NTM: Space's client start-up, the same on every loader (each loader's client init calls it). */
public final class SpaceClient {

    private SpaceClient() {}

    public static void init() {
        ObjBlockModels.install();
        MunSky.init();
        SkyWeatherClient.init();
        SkyShaderClient.init();
        SpaceClouds.init();
        OrbitClient.init();
        RocketClient.init();
        TransporterClient.init();
        com.ntmspace.bedrock.BedrockOreClient.install();
        // the recoloured sprites of this mod's autogen shapes (hbm:items/plate_cast_stainless, plate_welded_iridium,
        // wire_fine_zinc ...): on NeoForge/Forge HBM's own call ran at HBM's construction, before these materials existed
        com.hbm.client.AutogenSprites.install();
        // celestial body traits and the oxygen HUD (hbm-space perma-sync and ModEventHandlerRenderer)
        com.ntmspace.trait.TraitSync.initClient();
        com.ntmspace.breathing.OxygenSync.initClient();
        OxygenHud.init();
        SpecialItemsClient.init();
        CelestialOreTooltip.init();
        // the RBMK pellets (and any other ntmspace item) select on HBM's hbm:model_state; below 26.1 that predicate is
        // installed per item namespace, so it must be extended to this one or they always draw their fallback
        dev.golem.core.client.ItemModelProperties.includeItems("hbm", "model_state", com.ntmspace.NtmSpace.MOD_ID);
        AirVentClient.init();
        AtmosphereMachinesClient.init();
        FloraClient.init();
        com.ntmspace.client.render.mob.SpaceMobRenderers.init();
        MachinesClient.init();
        MachinesClientD.init();
        StructureClient.init();
        com.ntmspace.client.render.DropPodRenderers.init();
        DysonClient.init();
        WarClient.init();
        ThatmoSky.init();
        StardarLayers.init();
        StardarHeightmap.initClient();
        LeftoverClient.init();
        SpaceNames.init();
        com.ntmspace.fork.client.ForkClient.init();
        // the droplet sprites of NTM: Space's fluids (ntmspace:items/fluid_icon_<key>), before the first resource load
        com.hbm.client.FluidSprites.install(com.ntmspace.NtmSpace.MOD_ID);
    }
}
