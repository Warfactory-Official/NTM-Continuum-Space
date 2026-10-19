package com.ntmspace.client.gui.machine;

import com.hbm.inventory.gui.GuiUtil;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.machine.ContainerHydroponic;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's {@code GUIHydroponic}: {@code machine/gui_hydrobay.png} at 176 x 186; CO2 and O2 tanks (16 x 52) at x 13
 * and 31 (bottom y 70), the power bar (16 x 34, bottom y 52) at x 147, the fertiliser gauge (6 x 32, bottom y 69) at
 * x 102, the lamp light at (151, 4) while powered (200 HE), the title centred on x 97. Hovering the empty fertiliser
 * slot with nothing carried lists the valid fertilisers, cycling one per second.
 */
public class GUIHydroponic extends CompatContainerScreen<ContainerHydroponic> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_hydrobay.png");

    public GUIHydroponic(ContainerHydroponic menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 97 - font.width(title) / 2, 5, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityHydroponic hydro = menu.getTile();
        if (hydro == null) return;

        int power = (int) (hydro.power * 34 / TileEntityHydroponic.maxPower);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 147, topPos + 52 - power, (float) imageWidth, (float) (34 - power), 16, power, 256, 256);

        int fertilizer = hydro.fertilizer * 32 / TileEntityHydroponic.maxFertilizer;
        if (fertilizer > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 102, topPos + 69 - fertilizer, (float) (imageWidth + 16), (float) (32 - fertilizer), 6, fertilizer, 256, 256);

        if (hydro.power >= 200) GuiDraw.blit(graphics, TEXTURE, leftPos + 151, topPos + 4, (float) imageWidth, 34F, 9, 12, 256, 256);

        GuiUtil.tank(graphics, hydro.tanks[0], leftPos + 13, topPos + 70, 16, 52);
        GuiUtil.tank(graphics, hydro.tanks[1], leftPos + 31, topPos + 70, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityHydroponic hydro = menu.getTile();
        if (hydro == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 13, topPos + 70 - 52, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(hydro.tanks[0]), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 31, topPos + 70 - 52, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(hydro.tanks[1]), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 147, topPos + 52 - 34, 16, 34)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(hydro.power, TileEntityHydroponic.maxPower), mouseX, mouseY);
            return;
        }

        Slot slot = menu.getSlot(1);
        if (menu.getCarried().isEmpty() && !slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            ItemStack[] validItems = hydro.getValidFertilizers();
            int cycle = (int) ((System.currentTimeMillis() % (1000L * validItems.length)) / 1000);
            ItemStack selected = validItems[cycle];
            // 1.7 marked the selected stack by setting its size to 0 (a red frame); a modern stack of 0 is empty and
            // draws nothing, so every stack is drawn and the name line names the selected one, as 1.7's did
            List<Object[]> lines = new ArrayList<>();
            lines.add(validItems);
            lines.add(new Object[] {selected.getHoverName().getString()});
            GuiUtil.stackText(graphics, font, width, height, lines, mouseX, mouseY);
        }
    }
}
