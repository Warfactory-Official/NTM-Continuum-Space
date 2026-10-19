package com.ntmspace.client.gui;

import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.ContainerOrbitalStationComputer;
import com.ntmspace.orbit.OrbitalStation;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUIOrbitalStationComputer}: {@code gui_ship_computer.png} at 198 x 135, the station name in a
 * borderless white text field at (52, 26) that sends every edit, and the gravity switch at (36, 84) (29 x 5, its lit
 * overlay at u 198 while gravity is on). No slots, no player inventory, no labels.
 */
public final class GUIOrbitalStationComputer extends CompatContainerScreen<ContainerOrbitalStationComputer> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_ship_computer.png");

    private EditBox stationName;

    public GUIOrbitalStationComputer(ContainerOrbitalStationComputer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 198, 135);
    }

    @Override
    protected void init() {
        super.init();
        stationName = new EditBox(font, leftPos + 52, topPos + 26, 136, 12, Component.empty());
        stationName.setTextColor(0xFFFFFFFF);
        stationName.setTextColorUneditable(0xFFDDDDDD);
        stationName.setBordered(false);
        stationName.setValue(OrbitalStation.clientStation.name);
        stationName.setResponder(this::setName);
        addRenderableWidget(stationName);
        setFocused(stationName);
        stationName.setFocused(true);
        focusedEditBoxForInput = stationName;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (OrbitalStation.clientStation.gravityMultiplier > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 36, topPos + 84, imageWidth, 0F, 29, 5, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        // Toggle gravity
        if (event.x() >= leftPos + 36 && event.x() < leftPos + 36 + 29 && event.y() >= topPos + 84 && event.y() < topPos + 84 + 5) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            setGravity(OrbitalStation.clientStation.gravityMultiplier == 0);
            return true;
        }
        return false;
    }

    private void setName(String name) {
        CompoundTag data = new CompoundTag();
        data.putString("name", name);
        send(data);
    }

    private void setGravity(boolean enabled) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("gravity", enabled);
        send(data);
    }

    private void send(CompoundTag data) {
        if (menu.computer() == null) return;
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(menu.computer().getBlockPos(), data));
    }
}
