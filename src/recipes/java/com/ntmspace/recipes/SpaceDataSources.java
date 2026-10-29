package com.ntmspace.recipes;

import com.ntmspace.recipes.tables.*;
import dev.golem.core.world.DataSource;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.world.DataSources;

import java.util.ArrayList;
import java.util.List;

/**
 * NTM: Space's machine recipe rows, generated into {@code data/ntmspace/recipe/<table>/} at build time
 * ({@code :26.1-neoforge:runGenerateData}): its own tables, and its rows for HBM's (hbm-space's changes to them; the HBM
 * rows they replace are listed in {@code data/ntmspace/recipe_removals}). Each source transliterates hbm-space's recipe code.
 */
public final class SpaceDataSources implements DataSources {

    @Override
    public String namespace() {
        return "ntmspace";
    }

    @Override
    public List<DataSource<?>> sources() {
        List<DataSource<?>> sources = new ArrayList<>(List.of(
                // NTM: Space's own tables
                new AlkylationTable(),
                new AtmosphereTable(),
                new CryoDistillTable(),
                new VacuumCircuitTable(),
                // per-body bedrock ores (hbm-space CelestialBedrockOre) in HBM's tables
                new BedrockOreArcFurnaceTable(),
                new BedrockOreCentrifugeTable(),
                new BedrockOreCombinationTable(),
                new BedrockOreCrystallizerTable(),
                new BedrockOreElectrolyserMetalTable(),
                new BedrockOreShredderTable(),
                // hbm-space's changes to HBM's tables (the replaced HBM rows are in recipe_removals)
                new AssemblyMachineTable(),
                new ChemicalPlantTable(),
                new MixerTable(),
                new PurexTable(),
                new PlasmaForgeTable(),
                new OutgasserTable(),
                new CrucibleTable(),
                new PseudoFluidTable(),
                new BlastFurnaceTable(),
                new DiFurnaceTable(),
                new SILEXTable(),
                new ShredderTable(),
                new CrystallizerTable(),
                new CentrifugeTable(),
                new StorageDrumTable(),
                new ArcWelderTable(),
                new AnvilConstructionTable(),
                new AnvilSmithingTable(),
                new CrackingTable(),
                new LiquefactionTable(),
                new SawmillTable(),
                new PedestalTable(),
                new SolderingTable(),
                new ElectrolyserFluidTable(),
                new MaterialDistributionTable(),
                new PressTable(),
                new SolidificationTable(),
                new AnnihilatorTable(),
                new FractionTable(),
                new HydrotreatingTable(),
                new CokerTable(),
                new ReformingTable(),
                new CyclotronTable(),
                new GasCentrifugeTable(),
                new MagicTable(),
                new PyrolysisTable(),
                new RotaryFurnaceTable(),
                new VacuumDistillTable()));
        // data folders (fluid properties, data maps, item pools, removal lists, ...)
        sources.addAll(com.ntmspace.recipes.data.D5Sources.sources());
        sources.addAll(com.ntmspace.recipes.data.D4Sources.sources());
        return sources;
    }
}
