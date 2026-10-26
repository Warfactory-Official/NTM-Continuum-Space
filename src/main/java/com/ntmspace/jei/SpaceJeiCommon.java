package com.ntmspace.jei;

//? if jei {
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.ntmspace.NtmSpace;
import com.ntmspace.recipe.AlkylationRecipes;
import com.ntmspace.recipe.AtmosphereRecipes;
import com.ntmspace.recipe.CryoRecipes;
import com.ntmspace.recipe.VacuumCircuitRecipes;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.registry.SpaceMachinesB;
import dev.golem.core.jei.JeiViewer;
import dev.golem.core.viewer.ViewerPlugin;
import dev.golem.core.viewer.ViewerType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.ItemStack;

/**
 * NTM: Space's JEI integration, the loader-neutral body (the loaders' {@code @JeiPlugin} entrypoints delegate here):
 * hbm-space's {@code NEIRegistry} handlers for the addon's own machine tables - {@code ntmspace:cryo_distill},
 * {@code ntmspace:alkylation}, {@code ntmspace:vacuum_circuit} and {@code ntmspace:atmosphere} (see
 * {@link SpaceTableCategory}). The addon's rows in HBM's tables show in HBM's categories already.
 *
 * <p>The categories are Golem Core recipe-viewer categories (as HBM's are since its REI support), declared once in
 * {@link #VIEWER} and handed to JEI by {@link JeiViewer}.
 */
public final class SpaceJeiCommon {

    public static final ViewerType<GenericRecipe> CRYO_DISTILL = ViewerType.create(NtmSpace.MOD_ID, "cryo_distill", GenericRecipe.class);
    public static final ViewerType<GenericRecipe> ALKYLATION = ViewerType.create(NtmSpace.MOD_ID, "alkylation", GenericRecipe.class);
    public static final ViewerType<VacuumCircuitRecipes.VacuumCircuitRecipe> VACUUM_CIRCUIT = ViewerType.create(NtmSpace.MOD_ID, "vacuum_circuit", VacuumCircuitRecipes.VacuumCircuitRecipe.class);
    public static final ViewerType<GenericRecipe> ATMOSPHERE = ViewerType.create(NtmSpace.MOD_ID, "atmosphere", GenericRecipe.class);

    public static final ViewerType<AtmosphereExtractionCategory.Recipe> ATMOSPHERE_EXTRACTION =
            ViewerType.create(NtmSpace.MOD_ID, "atmosphere_extraction", AtmosphereExtractionCategory.Recipe.class);

    public static final ViewerType<DairyCategory.Recipe> DAIRY = ViewerType.create(NtmSpace.MOD_ID, "dairy", DairyCategory.Recipe.class);

    private static SpaceTableCategory<GenericRecipe> cryo;
    private static SpaceTableCategory<GenericRecipe> alkylation;
    private static SpaceTableCategory<VacuumCircuitRecipes.VacuumCircuitRecipe> vacuumCircuit;
    private static SpaceTableCategory<GenericRecipe> atmosphere;
    private static DairyCategory dairy;
    private static AtmosphereExtractionCategory atmosphereExtraction;

    /** The addon's recipe-viewer declarations. */
    public static final ViewerPlugin VIEWER = new ViewerPlugin() {
        @Override
        public void categories(Categories reg) {
            cryo = new SpaceTableCategory<>(CRYO_DISTILL, "Cryogenic Distillation", CryoRecipes.INSTANCE, false,
                    new ItemStack(SpaceMachines.MACHINE_CRYO_DISTILL.get()), 10);
            alkylation = new SpaceTableCategory<>(ALKYLATION, "Alkylation", AlkylationRecipes.INSTANCE, false,
                    new ItemStack(SpaceMachines.MACHINE_ALKYLATION.get()), 10);
            vacuumCircuit = new SpaceTableCategory<>(VACUUM_CIRCUIT, "Vacuum Solderer", VacuumCircuitRecipes.INSTANCE, true,
                    new ItemStack(SpaceMachinesB.MACHINE_VACUUM_CIRCUIT.get()), 1);
            atmosphere = new SpaceTableCategory<>(ATMOSPHERE, "Atmospheric Chemistry", AtmosphereRecipes.INSTANCE, false,
                    new ItemStack(AtmosphereMachines.ATMO_EMITTER.get()), 1);
            dairy = new DairyCategory();
            atmosphereExtraction = new AtmosphereExtractionCategory();
            reg.add(cryo, alkylation, vacuumCircuit, atmosphere, dairy, atmosphereExtraction);
        }

        @Override
        public void recipes(Recipes reg) {
            reg.add(CRYO_DISTILL, cryo.buildRecipes());
            reg.add(ALKYLATION, alkylation.buildRecipes());
            reg.add(VACUUM_CIRCUIT, vacuumCircuit.buildRecipes());
            reg.add(ATMOSPHERE, atmosphere.buildRecipes());
            reg.add(DAIRY, dairy.rows());
            reg.add(ATMOSPHERE_EXTRACTION, atmosphereExtraction.rows());
        }

        @Override
        public void stations(Stations reg) {
            reg.add(CRYO_DISTILL, new ItemStack(SpaceMachines.MACHINE_CRYO_DISTILL.get()));
            reg.add(ALKYLATION, new ItemStack(SpaceMachines.MACHINE_ALKYLATION.get()));
            reg.add(VACUUM_CIRCUIT, new ItemStack(SpaceMachinesB.MACHINE_VACUUM_CIRCUIT.get()));
            reg.add(ATMOSPHERE, new ItemStack(AtmosphereMachines.ATMO_EMITTER.get()));
            reg.add(DAIRY, new ItemStack(SpaceMachines.MACHINE_MILK_REFORMER.get()));
            reg.add(ATMOSPHERE_EXTRACTION, new ItemStack(AtmosphereMachines.ATMO_VENT.get()));
        }
    };

    private SpaceJeiCommon() {}

    public static void registerCategories(IRecipeCategoryRegistration reg) {
        JeiViewer.categories(reg, VIEWER);
    }

    public static void registerRecipes(IRecipeRegistration reg) {
        JeiViewer.recipes(reg, VIEWER);
    }

    public static void registerCatalysts(IRecipeCatalystRegistration reg) {
        JeiViewer.stations(reg, VIEWER);
    }
}
//?}
