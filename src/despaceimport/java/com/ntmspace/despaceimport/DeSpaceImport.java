package com.ntmspace.despaceimport;

import com.ntmspace.NtmSpace;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NTM DE: Space worlds convert with NTM: Decentralized Edition's, in HBM's importer: this root adds the addon's tables,
 * hooks and steps while mods are constructed, before the registry events read its aliases.
 */
@EventBusSubscriber(modid = NtmSpace.MOD_ID)
public final class DeSpaceImport {
    private DeSpaceImport() {}

    /** A mod-bus event; NeoForge routes it here by its type. */
    @SubscribeEvent
    public static void construct(FMLConstructModEvent event) {
        try {
            SpaceAddon.install();
        } catch (LinkageError e) {
            // an HBM whose importer takes no addons: the game still starts, and DE: Space worlds do not convert
            NtmSpace.LOG.error("This NTM - Continuum has no DE importer hooks for addons; DE: Space worlds will not convert", e);
        }
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.LOOT_NUMBER_PROVIDER_TYPE, ResourceLocation.fromNamespaceAndPath("ntmspace_de", "wand_loot_scaled"),
                () -> WandLootScaled.TYPE);
    }
}
