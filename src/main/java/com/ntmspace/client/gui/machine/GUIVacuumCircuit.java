package com.ntmspace.client.gui.machine;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.inventory.gui.IUpgradeInfoProviderSource;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.machine.ContainerVacuumCircuit;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code GUIVacuumCircuit}: {@code processing/gui_vac_cir_station.png} at 176 x 204; the power bar (16 x 52,
 * bottom y 70) at x 143 scaled to the machine's current buffer, the progress arrow (33 x 14) at (63, 28), the power light
 * at (146, 4) while the buffer covers one tick, the upgrade info star at (78, 76), the title centred 18 left of the middle.
 */
public class GUIVacuumCircuit extends CompatContainerScreen<ContainerVacuumCircuit> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/processing/gui_vac_cir_station.png");

    public GUIVacuumCircuit(ContainerVacuumCircuit menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2 - 18, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineVacuumCircuit te = menu.getTile();
        if (te == null) return;

        int p = (int) (te.power * 52 / Math.max(te.maxPower, 1));
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 143, topPos + 70 - p, 176F, (float) (52 - p), 16, p, 256, 256);

        int i = te.progress * 33 / Math.max(te.processTime, 1);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 63, topPos + 28, 192F, 0F, i, 14, 256, 256);

        if (te.power >= te.consumption) GuiDraw.blit(graphics, TEXTURE, leftPos + 146, topPos + 4, 176F, 52F, 9, 12, 256, 256);

        GuiUtil.drawInfoPanel(graphics, leftPos + 78, topPos + 76, 8);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineVacuumCircuit te = menu.getTile();
        if (te == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(te.getPower(), te.getMaxPower()), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 78, topPos + 76, 8, 8)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.upgradeInfo(te), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public @Nullable IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getTile();
    }
}
