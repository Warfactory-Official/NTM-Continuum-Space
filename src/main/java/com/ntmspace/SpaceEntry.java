package com.ntmspace;

import com.ntmspace.fluid.SpaceWorldFluids;
import dev.golem.core.platform.ModEntry;

/**
 * NTM: Space's common start-up in Golem Core's three phases: the Fabric {@code golemcore:entry}, and what the NeoForge
 * and Forge {@code @Mod} classes start. It is the same path HBM's own {@code HbmEntry} takes.
 */
public final class SpaceEntry implements ModEntry {

    @Override
    public void substitute() {
        // hbm-space's fan is sealable-aware (IBlockSealable); same id, HBM's own registration properties
        dev.golem.core.registry.Substitutions.block(NtmSpace.MOD_ID, "hbm:fan", com.ntmspace.block.SpaceMachineFan::new);
        // hbm-space's LPW-2 is an orbital-station engine: its block and block entity type, HBM's renderer (animationTime)
        com.ntmspace.block.station.SpaceMachineLPW2.substitute();
        // hbm-space's damageable Leviathan turbine and powered condenser (fork content: blown open / structure wrecks, blowtorch repair)
        com.ntmspace.fork.machine.SpaceMachineChungus.substitute();
        com.ntmspace.fork.machine.SpaceMachineCondenserPowered.substitute();
        // hbm-space's oil detector finds every body's oil, gas, brine and bedrock oil and names the reserve (fork content)
        dev.golem.core.registry.Substitutions.item(NtmSpace.MOD_ID, "hbm:oil_detector", com.ntmspace.fork.SpaceOilDetector::new);
    }

    @Override
    public void declare() {
        NtmSpace.LOG.info("NTM: Space declaring its content");
        com.ntmspace.fluid.SpaceFluids.init();
        SpaceWorldFluids.init();
        com.ntmspace.registry.SpaceBlocks.init();
        com.ntmspace.registry.DunaBlocks.init();
        com.ntmspace.registry.PlanetTerrainBlocks.init();
        com.ntmspace.registry.LaytheTektoBlocks.init();
        com.ntmspace.registry.FloraBlocks.init();
        com.ntmspace.registry.BodyOreBlocks.init();
        com.ntmspace.registry.PopulateBlocks.init();
        com.ntmspace.registry.SpaceFluidBlocks.init();
        // planet structures: their blocks/items, NBT spawns and structure types (meteor dungeons, bases, tomb, spaceship)
        com.ntmspace.registry.StructureBlocks.init();
        com.ntmspace.world.structure.SpaceStructureTypes.init();
        com.ntmspace.command.StructureCommands.init();
        com.ntmspace.world.SpaceDimensions.init();
        // hbm-space's "The Martian" world type: players start on Duna (Golem WorldTypes + first join)
        com.ntmspace.world.MartianWorld.init();
        com.ntmspace.command.SpaceTimeCommand.init();
        com.ntmspace.registry.StationBlocks.init();
        com.ntmspace.registry.TransporterBlocks.init();
        com.ntmspace.registry.StationSounds.init();
        com.ntmspace.rocket.RocketContent.init();
        com.ntmspace.orbit.Orbit.init();
        com.ntmspace.command.StationCommands.init();
        com.ntmspace.command.SpaceCommands.init();
        com.ntmspace.command.DunaIkeDresCommands.init();
        com.ntmspace.command.PlanetCommands.init();
        com.ntmspace.command.LaytheTektoThatmoCommands.init();
        com.ntmspace.command.AtmosphereCommands.init();
        com.ntmspace.command.CloudCommands.init();
        // celestial body traits, sealed rooms and breathing (before SpaceEnvironment, which registers their hooks)
        com.ntmspace.registry.AtmosphereContent.init();
        // atmosphere machines: tower, compressor, emitter, CO2 scrubber, editor; the atmospheric chemistry table
        com.ntmspace.registry.AtmosphereMachines.init();
        com.ntmspace.registry.SpaceMobs.init();
        // progression machines (cryo distill ... troll disturber), their items and recipe tables
        com.ntmspace.registry.SpaceItems.init();
        com.ntmspace.registry.SpaceMachineItems.init();
        // hbm-space special items: armour modules, tools, records, RTG/RBMK/Watz fuels (recipe-change family)
        com.ntmspace.registry.SpaceSpecialItems.init();
        // hbm-space's extra short-lived waste classes (americium-241, berkelium-247, curium-244/245)
        com.ntmspace.registry.SpaceWasteItems.init();
        // hbm-space non-space fork content: nitan/flashbang/slippery, NITAN flask, Bobmazon offer, [BOMBPL] log
        com.ntmspace.fork.ForkContent.init();
        com.ntmspace.registry.SpaceMachines.init();
        com.ntmspace.registry.SpaceMachinesB.init();
        com.ntmspace.registry.SpaceMachinesC.init();
        com.ntmspace.registry.SpaceMachinesD.init();
        com.ntmspace.registry.DropPodContent.init();
        // war between bodies and invasions (war controller, siege craft, invasion spawns/kills, railgun firing/sky sync)
        com.ntmspace.registry.WarContent.init();
        // per-body bedrock ores (hbm-space CelestialBedrockOre) and satellites across the bodies
        com.ntmspace.registry.BedrockOreItems.init();
        com.ntmspace.satellite.SpaceSatellites.init();
        // the Dyson swarm: spinlauncher, receiver, converters, swarm members
        com.ntmspace.registry.DysonContent.init();
        // leftovers: orrery, fence gate, oil barrel, algae film, vocaloid plushies, wild flowers; StarDar computer; JEI tables
        com.ntmspace.registry.LeftoverContent.init();
        com.ntmspace.trait.TraitSync.init();
        com.ntmspace.breathing.OxygenSync.init();
        com.ntmspace.atmosphere.AtmosphereHandler.init();
        com.ntmspace.world.SpaceEnvironment.init();
        com.ntmspace.world.SpaceSkies.init();
        // the bodies' weather on their levels, Duna's dust storms, satellites and stations in the sky
        com.ntmspace.registry.SkyWeatherContent.init();
        // the shader sky layers' nuke shocks (CelestialNukeShockHandler) and their test commands
        com.ntmspace.world.NukeShocks.init();
        com.ntmspace.command.SkyTraitCommands.init();
    }

    @Override
    public void registered() {
        NtmSpace.LOG.info("NTM: Space registered");
        // furnace fuels on Fabric and Forge (NeoForge: the authored neoforge:furnace_fuels data map)
        dev.golem.core.registry.Fuels.register(com.ntmspace.registry.FloraBlocks.burnTimes());
    }
}
