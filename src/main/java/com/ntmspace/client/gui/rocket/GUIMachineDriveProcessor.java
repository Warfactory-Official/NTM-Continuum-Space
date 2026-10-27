package com.ntmspace.client.gui.rocket;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.rocket.ContainerDriveProcessor;
import com.ntmspace.tileentity.rocket.TileEntityMachineDriveProcessor;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUIMachineDriveProcessor}: {@code processing/gui_drive_processor.png} at 176 x 207. The power bar
 * (134, 18, 16 x 52, from u 176), the lit process button (38, 61, from u 192 v 28) and the progress bar (59, 63, 44 x 14,
 * from u 192 v 14) while processing, and the status line in white at (41, 86). Buttons: process (38, 61, 18 x 18;
 * toggles), clone (49, 17, 18 x 18). No labels (1.7 drew none).
 */
public class GUIMachineDriveProcessor extends CompatContainerScreen<ContainerDriveProcessor> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/processing/gui_drive_processor.png");

    public GUIMachineDriveProcessor(ContainerDriveProcessor menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 207);
    }

    /** 1.7 {@code checkClick} / {@code drawCustomInfoStat} area test in screen coordinates. */
    private boolean inArea(double x, double y, int left, int top, int width, int height) {
        return leftPos + left <= x && leftPos + left + width > x && topPos + top < y && topPos + top + height >= y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineDriveProcessor machine = menu.getTile();
        if (machine == null) return;

        if (machine.isProcessing) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 38, topPos + 61, 192F, 28F, 18, 18, 256, 256);
        }

        int p = (int) (machine.power * 52 / machine.maxPower);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 134, topPos + 18 + 52 - p, imageWidth, 52 - p, 16, p, 256, 256);

        if (machine.isProcessing) {
            p = machine.progress * 44 / machine.maxProgress;
            if (p > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 59, topPos + 63, 192F, 14F, p, 14, 256, 256);
        }

        GuiDraw.text(graphics, font, machine.status.text(), leftPos + 41, topPos + 86, 0xFFFFFF, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineDriveProcessor machine = menu.getTile();
        if (machine == null) return;
        if (inArea(mouseX, mouseY, 134, 18, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(machine.power, machine.maxPower), mouseX, mouseY);
            return;
        }
        if (inArea(mouseX, mouseY, 49, 17, 18, 18)) {
            GuiUtil.tooltip(graphics, font, List.of(Component.literal("Clone drive")), mouseX, mouseY);
            return;
        }
        if (inArea(mouseX, mouseY, 38, 61, 18, 18)) {
            GuiUtil.tooltip(graphics, font, List.of(Component.literal("Start drive processing")), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        TileEntityMachineDriveProcessor machine = menu.getTile();
        if (machine == null) return handled;

        // Process drive
        if (inArea(event.x(), event.y(), 38, 61, 18, 18)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("process", !machine.isProcessing);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(machine.getBlockPos(), data));
            handled = true;
        }

        // Clone drive
        if (inArea(event.x(), event.y(), 49, 17, 18, 18)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("clone", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(machine.getBlockPos(), data));
            handled = true;
        }

        return handled;
    }
}
