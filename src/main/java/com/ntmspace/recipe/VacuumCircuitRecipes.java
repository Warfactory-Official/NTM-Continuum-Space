package com.ntmspace.recipe;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.ntmspace.NtmSpace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code VacuumCircuitRecipes} as the datapack table {@code ntmspace:vacuum_circuit}
 * ({@code data/ntmspace/recipe/vacuum_circuit/}, converted once from the 1.7 table; GameTest
 * {@code vacuum_circuit_table_matches_17}).
 *
 * <p>A row is a {@link GenericRecipe} plus one field: {@code input_items} lists 1.7's {@code wafer} ingredients first and
 * its {@code pcb} ingredients after them, and {@code "wafers": n} says how many of them are wafers (they go in the
 * machine's slots 0-1, the rest in slots 2-3). Keeping every ingredient in {@code input_items} lets recipe viewers show
 * the whole row. {@code output_items} is the one output, {@code duration} 1.7's duration in ticks, {@code power} 1.7's
 * {@code consumption} in HE per tick. Rows keep 1.7's order ({@code order}); the first row whose ingredients match wins,
 * as 1.7's {@code getRecipe}.
 */
public final class VacuumCircuitRecipes extends DataGenericRecipes<VacuumCircuitRecipes.VacuumCircuitRecipe> {

    public static final VacuumCircuitRecipes INSTANCE = new VacuumCircuitRecipes();

    /** One row; {@link #wafer()} and {@link #pcb()} are 1.7's two ingredient groups. */
    public static final class VacuumCircuitRecipe extends GenericRecipe {

        public int wafers;

        public VacuumCircuitRecipe(String name) {
            super(name);
        }

        public AStack[] wafer() {
            return Arrays.copyOfRange(inputItem, 0, Math.min(wafers, inputItem.length));
        }

        public AStack[] pcb() {
            return Arrays.copyOfRange(inputItem, Math.min(wafers, inputItem.length), inputItem.length);
        }

        @Override
        public VacuumCircuitRecipe inputItems(AStack... wafer) {
            return groups(wafer, inputItem == null ? new AStack[0] : pcb());
        }

        public VacuumCircuitRecipe setPcb(AStack... pcb) {
            return groups(inputItem == null ? new AStack[0] : wafer(), pcb);
        }

        private VacuumCircuitRecipe groups(AStack[] wafer, AStack[] pcb) {
            wafers = wafer.length;
            inputItem = Arrays.copyOf(wafer, wafer.length + pcb.length);
            System.arraycopy(pcb, 0, inputItem, wafer.length, pcb.length);
            return this;
        }

        public ItemStack output() {
            return outputItem[0];
        }

        public Item previewItem() {
            return outputItem[0].getItem();
        }

        public ItemStack output(RandomSource random) {
            return rollOutput(0, random);
        }
    }

    private volatile List<VacuumCircuitRecipe> rows = List.of();
    /** 1.7's static {@code wafer} and {@code pcb} sets: every ingredient of either group (slot validity). */
    private volatile List<AStack> wafer = List.of();
    private volatile List<AStack> pcb = List.of();

    private VacuumCircuitRecipes() {
        super(NtmSpace.MOD_ID, "vacuum_circuit", () -> GenericRecipeCodec.codec(VacuumCircuitRecipe::new,
                Codec.INT.fieldOf("wafers"), recipe -> recipe.wafers, (recipe, wafers) -> recipe.wafers = wafers));
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    @Override
    protected void rebuilt() {
        List<VacuumCircuitRecipe> valid = new ArrayList<>();
        List<AStack> wafers = new ArrayList<>(), pcbs = new ArrayList<>();
        for (VacuumCircuitRecipe recipe : typedRowsUnchecked()) {
            if (recipe.inputItem == null || recipe.wafers < 0 || recipe.wafers > recipe.inputItem.length
                    || recipe.outputItem == null || recipe.outputItem.length != 1) {
                NtmSpace.LOG.warn("Vacuum solderer recipe {} needs its wafer count within input_items and one output; ignored",
                        recipe.getInternalName());
                continue;
            }
            valid.add(recipe);
            wafers.addAll(Arrays.asList(recipe.wafer()));
            pcbs.addAll(Arrays.asList(recipe.pcb()));
        }
        rows = valid;
        wafer = wafers;
        pcb = pcbs;
    }

    @SuppressWarnings("unchecked")
    private List<VacuumCircuitRecipe> typedRowsUnchecked() {
        List<VacuumCircuitRecipe> out = new ArrayList<>();
        for (GenericRecipe recipe : recipeOrderedList) out.add((VacuumCircuitRecipe) recipe);
        return out;
    }

    /** The rows in 1.7 order. */
    public List<VacuumCircuitRecipe> rows() {
        registerRecipes();
        return rows;
    }

    /** 1.7 {@code getRecipe(inputs)}: slots 0-1 against the wafers, 2-3 against the PCBs. */
    public @Nullable VacuumCircuitRecipe getRecipe(ItemStack[] inputs) {
        for (VacuumCircuitRecipe recipe : rows()) {
            if (matchesIngredients(new ItemStack[] {inputs[0], inputs[1]}, recipe.wafer())
                    && matchesIngredients(new ItemStack[] {inputs[2], inputs[3]}, recipe.pcb())) return recipe;
        }
        return null;
    }

    public boolean isWafer(ItemStack stack) {
        registerRecipes();
        for (AStack t : wafer) if (t.matchesRecipe(stack, true)) return true;
        return false;
    }

    public boolean isPcb(ItemStack stack) {
        registerRecipes();
        for (AStack t : pcb) if (t.matchesRecipe(stack, true)) return true;
        return false;
    }

    /**
     * 1.7 {@code SerializableRecipe.matchesIngredients}: every non-empty input takes one unused ingredient it matches with
     * enough items, and every ingredient is taken.
     */
    public static boolean matchesIngredients(ItemStack[] inputs, AStack[] recipe) {
        List<AStack> recipeList = new ArrayList<>(Arrays.asList(recipe));
        for (ItemStack input : inputs) {
            if (input == null || input.isEmpty()) continue;
            boolean hasMatch = false;
            for (AStack recipeStack : recipeList) {
                if (recipeStack.matchesRecipe(input, true) && input.getCount() >= recipeStack.stacksize) {
                    hasMatch = true;
                    recipeList.remove(recipeStack);
                    break;
                }
            }
            if (!hasMatch) return false;
        }
        return recipeList.isEmpty();
    }
}
