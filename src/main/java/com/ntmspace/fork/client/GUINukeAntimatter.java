package com.ntmspace.fork.client;

import com.hbm.packet.ControlEventPayload;
import com.ntmspace.fork.antimatter.ContainerNukeAntimatter;
import com.ntmspace.fork.antimatter.TileEntityAntimatter;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space {@code GUINukeAntimatter} ({@code BlombSchematic.png}, 176 x 222): the start button at (80,85) (its pressed
 * overlay from (176,26) while counting), a three-digit timer field at (80,108) whose value (seconds, 1..999) reaches the
 * tile as it is typed, and, while the core is in, the countdown in red at 0.6 scale.
 */
public class GUINukeAntimatter extends CompatContainerScreen<ContainerNukeAntimatter> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("ntmspace", "textures/gui/weapon/blombschematic.png");

    private EditBox timer;

    public GUINukeAntimatter(ContainerNukeAntimatter menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        String previous = timer == null ? null : timer.getValue();
        TileEntityAntimatter bomb = menu.getAntimatter();
        timer = new EditBox(this.font, leftPos + 80, topPos + 108, 29, 12, Component.empty());
        timer.setTextColor(0xFFFF0000);
        timer.setBordered(false);
        timer.setMaxLength(3);
        timer.setValue(previous != null ? previous : String.valueOf(bomb == null ? 900 : bomb.timer / 20));
        timer.setResponder(this::sendTimer);
        addRenderableWidget(timer);
    }

    private void sendTimer(String text) {
        TileEntityAntimatter bomb = menu.getAntimatter();
        if (bomb == null) return;
        double parsed;
        try {
            parsed = Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("timer", Mth.clamp((int) parsed, 1, 999));
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(bomb.getBlockPos(), data));
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityAntimatter bomb = menu.getAntimatter();
        if (bomb == null || bomb.started) return false;
        double x = event.x(), y = event.y();
        if (leftPos + 80 <= x && leftPos + 80 + 18 > x && topPos + 85 < y && topPos + 85 + 18 >= y) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("start", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(bomb.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        // a focused field consumes the inventory key too, as 1.7's keyTyped did
        if (event.key() != 256 && event.key() != 258 && timer != null && timer.isFocused()) {
            event.keyPressed(timer);
            return true;
        }
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntityAntimatter bomb = menu.getAntimatter();
        if (bomb != null && bomb.hasAmatCore()) {
            String text = bomb.getMinutes() + ":" + bomb.getSeconds();
            double scale = 0.60;
            GuiDraw.pushMatrix(graphics);
            GuiDraw.scale(graphics, (float) scale, (float) scale);
            // 1.7 halved the unscaled width, so the readout sits a little right of centre - kept
            GuiDraw.text(graphics, font, text, (int) ((95 - font.width(text) / 2) * (1 / scale)),
                    (int) (66 * (1 / scale)), 0xFFFF0000, false);
            GuiDraw.popMatrix(graphics);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityAntimatter bomb = menu.getAntimatter();
        if (bomb != null && bomb.started) GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 85, 176F, 26F, 18, 18, 256, 256);
    }
}
