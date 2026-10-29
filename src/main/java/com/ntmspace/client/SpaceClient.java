package com.ntmspace.client;

import com.hbm.client.AutogenSprites;
import com.hbm.client.FluidSprites;
import com.hbm.client.model.ObjBlockModels;
import com.ntmspace.inventory.material.SpaceAlloyMaterials;
import dev.golem.core.render.Sprites;
import com.ntmspace.NtmSpace;
import com.ntmspace.bedrock.BedrockOreClient;
import com.ntmspace.breathing.OxygenSync;
import com.ntmspace.client.render.DropPodRenderers;
import com.ntmspace.client.render.mob.SpaceMobRenderers;
import com.ntmspace.fork.client.ForkClient;
import com.ntmspace.tileentity.machine.StardarHeightmap;
import com.ntmspace.trait.TraitSync;
import dev.golem.core.client.ItemModelProperties;
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
        BedrockOreClient.install();
        // the recoloured sprites of this mod's autogen shapes (hbm:items/plate_cast_stainless, plate_welded_iridium,
        // wire_fine_zinc ...): on NeoForge/Forge HBM's own call ran at HBM's construction, before these materials existed
        AutogenSprites.install();
        // This recipe-only form is outside iridium's autogen set; 1.7 draws the base art with its molten tint.
        var iridium = SpaceAlloyMaterials.IRIDIUM;
        Sprites.remap(Sprites.Atlas.ITEMS).texture("hbm:items/plate_welded")
               .tint(iridium.name, iridium.moltenColor).declare();
        // celestial body traits and the oxygen HUD (hbm-space perma-sync and ModEventHandlerRenderer)
        TraitSync.initClient();
        OxygenSync.initClient();
        OxygenHud.init();
        SpecialItemsClient.init();
        CelestialOreTooltip.init();
        // the RBMK pellets (and any other ntmspace item) select on HBM's hbm:model_state; below 26.1 that predicate is
        // installed per item namespace, so it must be extended to this one or they always draw their fallback
        ItemModelProperties.includeItems("hbm", "model_state", NtmSpace.MOD_ID);
        AirVentClient.init();
        AtmosphereMachinesClient.init();
        FloraClient.init();
        SpaceMobRenderers.init();
        MachinesClient.init();
        MachinesClientD.init();
        StructureClient.init();
        DropPodRenderers.init();
        DysonClient.init();
        WarClient.init();
        ThatmoSky.init();
        StardarLayers.init();
        StardarHeightmap.initClient();
        LeftoverClient.init();
        SpaceNames.init();
        ForkClient.init();
        // the droplet sprites of NTM: Space's fluids (ntmspace:items/fluid_icon_<key>), before the first resource load
        FluidSprites.install(NtmSpace.MOD_ID);
    }
}
