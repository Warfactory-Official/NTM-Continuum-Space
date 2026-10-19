package com.ntmspace.client.gui;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.ContainerTransporterRocket;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
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
 * hbm-space's {@code GUITransporterRocket}: {@code gui_transporter.png} at 230 x 236. The pad's name in a borderless
 * green text field at (8, 12), 122 x 12 (every edit is sent); the threshold readout "x2^(n-1)" right-aligned at 167, 12;
 * the threshold slider (drag inside (98, 120) 74 x 20, 0..10, its 4 x 15 knob from u 230 at 100 + n * 6.78, 122); the
 * four input tanks (8 + 18i, 34 tall, bottom at 104), the four output tanks (98 + 18i) and the hydrogen and oxygen tanks
 * (188 and 206, 70 tall, bottom at 88). No labels.
 *
 * <p>1.7 drew the tanks over the finished screen in {@code drawScreen}; here they are drawn on the background.
 * 1.7's slider reacted to {@code mouseClickMove} (a drag); the port follows the held mouse each frame and sends a new
 * value when it changes.
 */
public final class GUITransporterRocket extends CompatContainerScreen<ContainerTransporterRocket> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_transporter.png");

    private EditBox transporterName;
    private boolean dragging;
    private int lastMouseX = Integer.MIN_VALUE, lastMouseY = Integer.MIN_VALUE;

    public GUITransporterRocket(ContainerTransporterRocket menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 236);
    }

    @Override
    protected void init() {
        super.init();
        TileEntityTransporterRocket transporter = menu.transporter();
        transporterName = new EditBox(font, leftPos + 8, topPos + 12, 122, 12, Component.empty());
        transporterName.setTextColor(0xFF00FF00);
        transporterName.setTextColorUneditable(0xFF00FF00);
        transporterName.setBordered(false);
        transporterName.setValue(transporter != null ? transporter.getTransporterName() : "");
        transporterName.setResponder(this::setName);
        addRenderableWidget(transporterName);
        focusedEditBoxForInput = transporterName;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TileEntityTransporterRocket transporter = menu.transporter();
        if (dragging && (mouseX != lastMouseX || mouseY != lastMouseY)) mouseClickMove(mouseX, mouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (transporter == null) return;

        GuiDraw.blit(graphics, TEXTURE, leftPos + 100 + (int) (transporter.threshold * 6.78), topPos + 122, imageWidth, 0F, 4, 15, 256, 256);

        int threshold = transporter.getThreshold();
        int width = font.width("x" + threshold);
        GuiDraw.text(graphics, font, "x" + threshold, leftPos + 167 - width, topPos + 12, -1, true);

        for (int i = 0; i < 4; i++) {
            GuiUtil.tank(graphics, transporter.tanks[i], leftPos + 8 + i * 18, topPos + 70 + 34, 16, 34);
        }
        for (int i = 0; i < 4; i++) {
            GuiUtil.tank(graphics, transporter.tanks[i + 4], leftPos + 98 + i * 18, topPos + 70 + 34, 16, 34);
        }
        for (int i = 0; i < 2; i++) {
            GuiUtil.tank(graphics, transporter.tanks[i + 8], leftPos + 188 + i * 18, topPos + 18 + 70, 16, 70);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityTransporterRocket transporter = menu.transporter();
        if (transporter == null) return;
        for (int i = 0; i < 4; i++) {
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 8 + i * 18, topPos + 70, 16, 34)) {
                GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(transporter.tanks[i]), mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 98 + i * 18, topPos + 70, 16, 34)) {
                GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(transporter.tanks[i + 4]), mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (GuiUtil.hover(mouseX, mouseY, leftPos + 188 + i * 18, topPos + 18, 16, 70)) {
                GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(transporter.tanks[i + 8]), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        dragging = true;
        lastMouseX = (int) event.x();
        lastMouseY = (int) event.y();
        return false;
    }

    @Override
    protected boolean mouseReleased(MouseInput event) {
        dragging = false;
        return false;
    }

    /** 1.7 {@code mouseClickMove}: the slider follows a drag inside its box. */
    private void mouseClickMove(int mouseX, int mouseY) {
        TileEntityTransporterRocket transporter = menu.transporter();
        if (transporter == null) return;

        if (isInAABB(mouseX, mouseY, leftPos + 98, topPos + 120, 74, 20)) {
            int slidPos = (int) ((mouseX - (leftPos + 98)) / 6.78);
            slidPos = Mth.clamp(slidPos, 0, 10); // 2^0 - 2^9 | 0 - 512

            CompoundTag data = new CompoundTag();
            data.putDouble("threshold", slidPos);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(transporter.getBlockPos(), data));
        }
    }

    private void setName(String name) {
        TileEntityTransporterRocket transporter = menu.transporter();
        if (transporter != null) transporter.setTransporterName(name);
    }

    private static boolean isInAABB(int mouseX, int mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }
}
