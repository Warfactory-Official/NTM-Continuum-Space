package com.ntmspace.client.gui.machine;

import com.hbm.inventory.gui.GuiUtil;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.machine.ContainerMachineMilkReformer;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUIMilkReformer}: {@code processing/gui_milk_reformer.png} at 176 x 238, a horizontal power bar
 * (63 wide at (98, 13), from u 176 v 6), the milk tank drawn 5 x 30 at (50, 62) and the three products 3 x 10 at
 * x 101/128/155 (bottom y 63). Its hover areas are 1.7's, which do not match the drawn bars. No title, only the
 * inventory label.
 */
public class GUIMilkReformer extends CompatContainerScreen<ContainerMachineMilkReformer> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/processing/gui_milk_reformer.png");

    public GUIMilkReformer(ContainerMachineMilkReformer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineMilkReformer te = menu.getTile();
        if (te == null) return;

        int j = (int) (te.power * 63 / TileEntityMachineMilkReformer.maxPower);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 98, topPos + 13, 176F, 6F, j, 6, 256, 256);

        GuiUtil.tank(graphics, te.tanks[0], leftPos + 50, topPos + 62, 5, 30);
        GuiUtil.tank(graphics, te.tanks[1], leftPos + 101, topPos + 63, 3, 10);
        GuiUtil.tank(graphics, te.tanks[2], leftPos + 128, topPos + 63, 3, 10);
        GuiUtil.tank(graphics, te.tanks[3], leftPos + 155, topPos + 63, 3, 10);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineMilkReformer te = menu.getTile();
        if (te == null) return;
        // 1.7 renderTankInfo areas
        int[][] areas = {{45, 90 - 52, 16, 29}, {90, 70 - 15, 21, 10}, {117, 70 - 15, 21, 10}, {143, 70 - 15, 21, 10}};
        for (int i = 0; i < 4; i++) {
            if (GuiUtil.hover(mouseX, mouseY, leftPos + areas[i][0], topPos + areas[i][1], areas[i][2], areas[i][3])) {
                GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(te.tanks[i]), mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 98, topPos + 60 - 52, 70, 20)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, TileEntityMachineMilkReformer.maxPower), mouseX, mouseY);
        }
    }
}
