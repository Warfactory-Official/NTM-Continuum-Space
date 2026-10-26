package com.ntmspace.jei;

import com.ntmspace.rocket.DriveBody;
//? if jei {
import com.hbm.handler.jei.HbmRecipeCategory;
import com.hbm.handler.jei.JEIUniversalHandler;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Each body's declared atmosphere, with real gas ingredients and its partial pressures. */
public final class AtmosphereExtractionCategory extends HbmRecipeCategory<AtmosphereExtractionCategory.Recipe> {
    private final Drawable slotFrame = Drawable.texture(tex("gui_nei.png"), 5, 87, 18, 18);

    public AtmosphereExtractionCategory() {
        super(SpaceJeiCommon.ATMOSPHERE_EXTRACTION, AtmosphereMachines.ATMO_VENT.get().getName(),
                new ItemStack(AtmosphereMachines.ATMO_VENT.get()), "gui_nei.png");
    }

    public Identifier id() {
        return Ids.parse(type().id());
    }

    public List<Recipe> rows() {
        var rows = new ArrayList<Recipe>();
        // The navigation-body catalogue keeps the authored 1.7 declaration order.
        for (var destination : DriveBody.values()) {
            var body = destination.getBody();
            if (body == null) continue;
            var name = body.name;
            var atmosphere = CelestialTraits.getDefaultTrait(name, CBT_Atmosphere.class);
            if (atmosphere == null) continue;
            var fluids = new ArrayList<Fluid>();
            var pressures = new ArrayList<Double>();
            for (var entry : atmosphere.fluids) {
                fluids.add(Fluids.toVanilla(entry.fluid));
                pressures.add(entry.pressure);
            }
            rows.add(new Recipe(Ids.of(NtmSpace.MOD_ID, "atmosphere_extraction/" + name), body,
                    List.copyOf(fluids), List.copyOf(pressures)));
        }
        return rows;
    }

    @Override
    public void setRecipe(SlotLayout builder, Recipe recipe) {
        var coordinates = JEIUniversalHandler.getOutputCoords(recipe.fluids().size());
        for (var i = 0; i < coordinates.length; i++) {
            var pressure = Math.round(recipe.pressures().get(i) * 1000D) / 1000D + "atm";
            builder.addSlot(Role.OUTPUT, coordinates[i][0], coordinates[i][1]).setBackground(slotFrame, -1, -1)
                    .addFluidStack(recipe.fluids().get(i), 1000)
                    .addTooltipCallback((shown, lines) -> lines.add(Component.literal(pressure)));
        }
        builder.addSlot(Role.CRAFTING_STATION, 75, 31).addItemStack(icon());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
        float scroll = (System.currentTimeMillis() % 4000) / 4000F;
        GuiDraw.blitUv(graphics, Ids.of(NtmSpace.MOD_ID, recipe.body().texture()), 32, 12, 32, 32,
                scroll, 0F, 1F + scroll, 1F, 256, 256);
        var name = Component.translatable("body." + recipe.body().name).getString();
        text(graphics, name, 48 - textWidth(name) / 2, 50, 0xFF000000);
    }

    public record Recipe(Identifier id, SolarSystem.Body body, List<Fluid> fluids, List<Double> pressures) {}
}
//?}
