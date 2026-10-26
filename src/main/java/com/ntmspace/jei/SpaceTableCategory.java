package com.ntmspace.jei;

import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
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
public class SpaceTableCategory<T extends GenericRecipe> extends JEIUniversalHandler<T> {

    private final GenericRecipes table;
    private final boolean stats;
    private final int shownFluidSteps;

    public SpaceTableCategory(ViewerType<T> type, String title,
                              GenericRecipes table, boolean stats, ItemStack machine, int shownFluidSteps) {
        super(type, Component.literal(title), machine);
        this.table = table;
        this.stats = stats;
        this.shownFluidSteps = shownFluidSteps;
    }

    /** Every row of the table, in its order. */
    public List<T> buildRecipes() {
        table.registerRecipes();
        List<T> rows = new ArrayList<>();
        for (GenericRecipe recipe : table.recipeOrderedList) {
            var row = type().recipeClass().cast(recipe);
            if (!inputs(row).isEmpty() || !outputs(row).isEmpty()) rows.add(row);
        }
        return rows;
    }

    @Override
    protected List<List<ItemStack>> inputs(T recipe) {
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
    protected List<List<ItemStack>> outputs(T recipe) {
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

    /** Real fluid ingredients retain the authored display batch instead of becoming fluid-icon items. */
    @Override
    public void setRecipe(SlotLayout builder, T recipe) {
        if (recipe.inputFluid == null && recipe.outputFluid == null) {
            super.setRecipe(builder, recipe);
            return;
        }
        var inputItems = new ArrayList<List<ItemStack>>();
        if (recipe.inputItem != null) for (var ingredient : recipe.inputItem) {
            var alternatives = ingredient.getDisplayStacks();
            if (!alternatives.isEmpty()) inputItems.add(alternatives);
        }
        var outputItems = new ArrayList<List<ItemStack>>();
        if (recipe.outputItem != null) for (var stack : recipe.outputItem) {
            if (stack != null && !stack.isEmpty()) outputItems.add(List.of(stack.copy()));
        }
        addSlots(builder, Role.INPUT, inputItems, recipe.inputFluid);
        addSlots(builder, Role.OUTPUT, outputItems, recipe.outputFluid);
        builder.addSlot(Role.CRAFTING_STATION, 75, 31).addItemStack(icon());
    }

    private void addSlots(SlotLayout builder, Role role,
                          List<List<ItemStack>> items, FluidStack[] fluids) {
        int count = items.size() + (fluids == null ? 0 : fluids.length);
        var coordinates = role == Role.INPUT ? getInputCoords(count) : getOutputCoords(count);
        for (var i = 0; i < items.size(); i++) {
            builder.addSlot(role, coordinates[i][0], coordinates[i][1]).addItemStacks(items.get(i))
                    .setBackground(slotFrame, -1, -1);
        }
        if (fluids != null) for (var i = 0; i < fluids.length; i++) {
            var fluid = fluids[i];
            var position = coordinates[items.size() + i];
            builder.addSlot(role, position[0], position[1])
                    .addFluidStack(Fluids.toVanilla(fluid.type),
                            Math.multiplyExact((long) fluid.fill, shownFluidSteps))
                    .setBackground(slotFrame, -1, -1);
        }
    }

    @Override
    public void draw(T recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
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
