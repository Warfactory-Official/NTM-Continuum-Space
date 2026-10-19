package com.ntmspace.client.gui.machine;

import com.hbm.inventory.gui.GuiUtil;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.machine.ContainerMachineCryoDistill;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUIMachineCryoDistill}: {@code processing/gui_cryodistill.png} at 176 x 238, the power bar at x 26
 * (scaled to 54 against a 52-tall bar, as 1.7), the five 16 x 52 tanks at x 44/80/98/116/134 (bottom y 70), a white
 * centred title.
 */
public class GUIMachineCryoDistill extends CompatContainerScreen<ContainerMachineCryoDistill> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/processing/gui_cryodistill.png");
    private static final int[] TANK_X = {44, 80, 98, 116, 134};

    public GUIMachineCryoDistill(ContainerMachineCryoDistill menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xffffff, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineCryoDistill te = menu.getTile();
        if (te == null) return;

        int j = (int) (te.power * 54 / TileEntityMachineCryoDistill.maxPower);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 26, topPos + 70 - j, 176F, (float) (52 - j), 16, j, 256, 256);

        for (int i = 0; i < 5; i++) GuiUtil.tank(graphics, te.tanks[i], leftPos + TANK_X[i], topPos + 70, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCryoDistill te = menu.getTile();
        if (te == null) return;
        for (int i = 0; i < 5; i++) {
            if (GuiUtil.hover(mouseX, mouseY, leftPos + TANK_X[i], topPos + 70 - 52, 16, 52)) {
                GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(te.tanks[i]), mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 26, topPos + 70 - 52, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, TileEntityMachineCryoDistill.maxPower), mouseX, mouseY);
        }
    }
}
