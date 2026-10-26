package com.ntmspace.client;

import com.ntmspace.satellite.SatelliteSettings;
import com.ntmspace.satellite.SatelliteSettingsMenu;
import com.ntmspace.satellite.SpaceSatellites;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Edits the held satellite's orbit and sends those edits through its server-authorized menu. */
public final class SatelliteSettingsScreen extends CompatContainerScreen<SatelliteSettingsMenu> {

    private static final String PREFIX = "gui.ntmspace.satellite_settings.";
    private static final String[] LABELS = {"owner", "altitude", "inclination", "phase", "period", "red", "green", "blue"};
    private final EditBox[] values = new EditBox[LABELS.length];
    private boolean blinking;
    private Component error;

    public SatelliteSettingsScreen(SatelliteSettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 292, 230);
    }

    @Override
    protected void init() {
        super.init();
        var orbit = SpaceSatellites.orbit(menu.stack());
        var initial = new String[] {orbit.owner(), Float.toString(orbit.altitude()), Float.toString(orbit.inclination()),
                Float.toString(orbit.phaseOffset()), Float.toString(orbit.blinkPeriod()),
                Integer.toString(Math.round(orbit.colorR() * 255)), Integer.toString(Math.round(orbit.colorG() * 255)),
                Integer.toString(Math.round(orbit.colorB() * 255))};
        for (var i = 0; i < values.length; i++) {
            var box = new EditBox(font, leftPos + 118, topPos + 25 + i * 18, 162, 16, Component.translatable(PREFIX + LABELS[i]));
            box.setMaxLength(i == 0 ? 64 : 20);
            box.setValue(initial[i]);
            values[i] = addRenderableWidget(box);
        }
        blinking = orbit.blinking();
        addRenderableWidget(Button.builder(blinkLabel(), button -> {
            blinking = !blinking;
            button.setMessage(blinkLabel());
        }).bounds(leftPos + 8, topPos + 174, 272, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(leftPos + 8, topPos + 195, 132, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> discardChanges())
                .bounds(leftPos + 148, topPos + 195, 132, 18).build());
    }

    private Component blinkLabel() {
        return Component.translatable(PREFIX + "blinking").append(": ").append(Component.translatable(blinking ? "options.on" : "options.off"));
    }

    private void discardChanges() {
        super.onClose();
    }

    @Override
    public void onClose() {
        try {
            var altitude = Float.parseFloat(values[1].getValue());
            var inclination = Float.parseFloat(values[2].getValue());
            var phase = Float.parseFloat(values[3].getValue());
            var period = Float.parseFloat(values[4].getValue());
            if (!Float.isFinite(altitude) || !Float.isFinite(inclination) || !Float.isFinite(phase) || !Float.isFinite(period)) {
                throw new NumberFormatException();
            }
            GolemNetwork.sendToServer(SatelliteSettings.CODEC, new SatelliteSettings.Edit(menu.containerId,
                    values[0].getValue(), altitude, inclination, phase, blinking, period,
                    Integer.parseInt(values[5].getValue()), Integer.parseInt(values[6].getValue()), Integer.parseInt(values[7].getValue())));
            super.onClose();
        } catch (NumberFormatException invalid) {
            error = Component.translatable(PREFIX + "invalid");
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF222A32);
        for (var i = 0; i < LABELS.length; i++) {
            GuiDraw.text(graphics, font, Component.translatable(PREFIX + LABELS[i]), leftPos + 8, topPos + 29 + i * 18, 0xFFFFFFFF, false);
        }
        if (error != null) GuiDraw.text(graphics, font, error, leftPos + 8, topPos + 216, 0xFFFF7777, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 8, 8, 0xFFFFFFFF, false);
    }
}
