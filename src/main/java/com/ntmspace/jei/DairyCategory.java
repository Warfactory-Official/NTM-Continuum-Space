package com.ntmspace.jei;

//? if jei {
import com.hbm.handler.jei.HbmRecipeCategory;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.NtmSpace;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import dev.golem.core.Ids;
import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** The milk reformer's fixed batch, presented as native recipe-viewer fluid ingredients. */
public final class DairyCategory extends HbmRecipeCategory<DairyCategory.Recipe> {

    private final Drawable slotFrame = Drawable.texture(tex("gui_nei.png"), 5, 87, 18, 18);
    private final Drawable machinePill = Drawable.texture(tex("gui_nei.png"), 59, 87, 18, 36);

    public DairyCategory() {
        super(SpaceJeiCommon.DAIRY, SpaceMachines.MACHINE_MILK_REFORMER.get().getName(),
                new ItemStack(SpaceMachines.MACHINE_MILK_REFORMER.get()), "gui_nei.png");
    }

    public Identifier id() {
        return Ids.parse(type().id());
    }

    public List<Recipe> rows() {
        return List.of(new Recipe(Ids.of(NtmSpace.MOD_ID, "dairy/milk"), Fluids.toVanilla(SpaceFluids.MILK),
                TileEntityMachineMilkReformer.INPUT,
                List.of(Fluids.toVanilla(SpaceFluids.EMILK), Fluids.toVanilla(SpaceFluids.CMILK),
                        Fluids.toVanilla(SpaceFluids.CREAM)),
                new int[] {TileEntityMachineMilkReformer.OUTPUT_ENRICHED,
                        TileEntityMachineMilkReformer.OUTPUT_CONDENSED, TileEntityMachineMilkReformer.OUTPUT_CREAM}));
    }

    @Override
    public void setRecipe(SlotLayout builder, Recipe recipe) {
        builder.addSlot(Role.INPUT, 48, 24).setBackground(slotFrame, -1, -1)
                .addFluidStack(recipe.input(), recipe.inputAmount());
        for (var i = 0; i < recipe.outputs().size(); i++) {
            builder.addSlot(Role.OUTPUT, 102 + i * 18, 24).setBackground(slotFrame, -1, -1)
                    .addFluidStack(recipe.outputs().get(i), recipe.outputAmounts()[i]);
        }
        builder.addSlot(Role.CRAFTING_STATION, 75, 31).addItemStack(icon());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
        machinePill.draw(graphics, 74, 14);
    }

    public record Recipe(Identifier id, Fluid input, int inputAmount, List<Fluid> outputs, int[] outputAmounts) {}
}
//?}
