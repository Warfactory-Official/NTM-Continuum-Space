package com.ntmspace.jei;

//? if jei {
import com.hbm.handler.jei.JEIUniversalHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's {@code NEIUniversalHandler}s for NTM: Space's own machine tables, on HBM's {@link JEIUniversalHandler}
 * (the {@code gui_nei} background, the count-based slot tables, the arrow-over-machine pill): {@code CryoHandler}
 * ("Cryogenic Distillation", the input fluid to its four fractions), {@code AlkylationHandler} ("Alkylation", input and
 * secondary fluid to two products), {@code VacuumCircuitHandler} ("Vacuum Solderer", the wafers and boards to the
 * circuit, with its {@code drawExtras} duration and consumption at the right edge) and {@code AtmosphereRecipeHandler}
 * ("Atmospheric Chemistry", gases to a gas). 1.7 built each list from its table's {@code getRecipes()} map; here every
 * row of the datapack table: items first, then fluids as droplets, in the row's order.
 */
public class SpaceTableCategory extends JEIUniversalHandler<GenericRecipe> {

    private final GenericRecipes table;
    private final boolean stats;

    public SpaceTableCategory(ViewerType<GenericRecipe> type, String title,
                              GenericRecipes table, boolean stats, ItemStack machine) {
        super(type, Component.literal(title), machine);
        this.table = table;
        this.stats = stats;
    }

    /** Every row of the table, in its order. */
    public List<GenericRecipe> buildRecipes() {
        table.registerRecipes();
        List<GenericRecipe> rows = new ArrayList<>();
        for (GenericRecipe recipe : table.recipeOrderedList) {
            if (!inputs(recipe).isEmpty() || !outputs(recipe).isEmpty()) rows.add(recipe);
        }
        return rows;
    }

    @Override
    protected List<List<ItemStack>> inputs(GenericRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        if (recipe.inputItem != null) for (AStack stack : recipe.inputItem) {
            List<ItemStack> variants = stack.getDisplayStacks();
            if (!variants.isEmpty()) list.add(variants);
        }
        if (recipe.inputFluid != null) for (FluidStack fluid : recipe.inputFluid) {
            ItemStack icon = ItemFluidIcon.make(fluid);
            if (!icon.isEmpty()) list.add(Collections.singletonList(icon));
        }
        return list;
    }

    @Override
    protected List<List<ItemStack>> outputs(GenericRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        if (recipe.outputItem != null) for (ItemStack stack : recipe.outputItem) {
            if (stack != null && !stack.isEmpty()) list.add(Collections.singletonList(stack.copy()));
        }
        if (recipe.outputFluid != null) for (FluidStack fluid : recipe.outputFluid) {
            ItemStack icon = ItemFluidIcon.make(fluid);
            if (!icon.isEmpty()) list.add(Collections.singletonList(icon));
        }
        return list;
    }

    @Override
    public void draw(GenericRecipe recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        if (!stats) return;
        // VacuumCircuitHandler.drawExtras
        String duration = String.format(Locale.US, "%,d", recipe.duration) + " ticks";
        String consumption = String.format(Locale.US, "%,d", recipe.power) + " HE/t";
        text(g, duration, 160 - textWidth(duration), 43);
        text(g, consumption, 160 - textWidth(consumption), 55);
    }
}
//?}
