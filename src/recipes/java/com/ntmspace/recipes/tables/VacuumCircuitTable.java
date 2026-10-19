package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.ntmspace.recipe.VacuumCircuitRecipes;
import com.ntmspace.recipe.VacuumCircuitRecipes.VacuumCircuitRecipe;
import com.ntmspace.registry.SpaceMachineItems;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code VacuumCircuitRecipes.registerDefaults}: one row per {@code recipes.add(new VacuumCircuitRecipe(output,
 * duration, consumption, wafer, pcb))}, in registration order, named {@code vacuum.<output circuit type>}:
 * {@code input_items} is the wafers followed by the PCB ingredients and {@code wafers} says how many are wafers, as
 * {@link VacuumCircuitRecipes} reads it. hbm-space's circuit types are NTM: Space's ({@link SpaceMachineItems}). Replaces
 * the authored rows of {@code data/ntmspace/recipe/vacuum_circuit/}.
 */
public final class VacuumCircuitTable extends GenericTableSource<VacuumCircuitRecipe> {

    public VacuumCircuitTable() {
        super(VacuumCircuitRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        //t0 you can go to the mun when you got all the shit. :)
        add("PROCESST1", 200, 250,
                new AStack[] {
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 8)},
                new AStack[] {
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 4)}
        );

        add("PROCESST2", 400, 1_000,
                new AStack[] {
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 16)},
                new AStack[] {
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 32)}
        );

        add("PROCESST3", 800, 25_000,
                new AStack[] {
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 48),
                        new ComparableStack(SpaceMachineItems.circuit("HFCHIP"), 16)},
                new AStack[] {
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 8),
                        new ComparableStack(SpaceMachineItems.circuit("CAPACITOR_LANTHANIUM"), 64)}
        );
    }

    /**
     * 1.7 {@code recipes.add(new VacuumCircuitRecipe(new ItemStack(ModItems.circuit, 1, EnumCircuitType.X.ordinal()),
     * duration, consumption, wafer, pcb))}, the output given by its circuit type {@code X}.
     */
    private void add(String output, int duration, long consumption, AStack[] wafer, AStack[] pcb) {
        VacuumCircuitRecipe recipe = new VacuumCircuitRecipe("vacuum." + output.toLowerCase(Locale.ROOT));
        recipe.wafers = wafer.length;
        recipe.setup(duration, consumption)
                .inputItems(Stream.concat(Arrays.stream(wafer), Arrays.stream(pcb)).toArray(AStack[]::new))
                .outputItems(new ItemStack(SpaceMachineItems.circuit(output), 1));
        register(recipe);
    }
}
