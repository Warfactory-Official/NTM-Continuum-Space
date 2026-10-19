package com.ntmspace.client.gui.rocket;

import com.hbm.inventory.gui.GuiUtil;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.rocket.ContainerMachineRocketAssembly;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketTarget;
import com.ntmspace.tileentity.rocket.TileEntityMachineRocketAssembly;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUIMachineRocketAssembly}: {@code gui_rocket_assembly.png} at 192 x 224. The stage arrows
 * (17, 34) and (17, 98) page through the stages (hover-lit at u 192, v 36 / 44), the stage lamp (47, 39) shows the shown
 * stage, the construct button (41, 62) lights when the rocket is valid and the deconstruct frame (39, 52) when the
 * result slot holds a rocket and the rest is empty. The shown stage turns in the bay (scissor 65, 5, 90 x 106) with its
 * scale and offset easing towards the stage's (4 per second); the issue list is drawn at half size at (65, 5), the
 * drives' body names at (162, 75) and (162, 108).
 */
public class GUIMachineRocketAssembly extends CompatContainerScreen<ContainerMachineRocketAssembly> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_rocket_assembly.png");

    private final RocketPreview preview = new RocketPreview();

    private double currentOffset = 0;
    private double currentScale = 1;
    private long lastTime = 0;

    public GUIMachineRocketAssembly(ContainerMachineRocketAssembly menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 192, 224);
    }

    @Override
    protected void init() {
        super.init();
        lastTime = System.nanoTime();
    }

    private RocketStruct rocket() {
        TileEntityMachineRocketAssembly machine = menu.getAssembly();
        return machine != null ? machine.rocket : new RocketStruct();
    }

    /** 1.7 {@code checkClick} in screen coordinates. */
    private boolean checkClick(double x, double y, int left, int top, int width, int height) {
        return leftPos + left <= x && leftPos + left + width > x && topPos + top < y && topPos + top + height >= y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineRocketAssembly machine = menu.getAssembly();
        RocketStruct rocket = rocket();

        int stage = Math.max(rocket.stages.size() - 1 - menu.getLayer(), -1);

        GuiDraw.blit(graphics, TEXTURE, leftPos + 47, topPos + 39, imageWidth + 18 + (stage + 1) * 6, 0F, 6, 8, 256, 256);

        stage = Math.max(stage, 0);

        double dt = (double) (System.nanoTime() - lastTime) / 1000000000;
        lastTime = System.nanoTime();

        double size = 86;
        double height = rocket.getHeight(stage);
        double targetScale = size / Math.max(height, 6);
        currentScale = currentScale + (targetScale - currentScale) * dt * 4;

        double targetOffset = rocket.getOffset(stage);
        currentOffset = currentOffset + (targetOffset - currentOffset) * dt * 4;

        if (rocket.capsule != null || !rocket.stages.isEmpty()) {
            preview.draw(graphics, rocket, leftPos + 65, topPos + 5, leftPos + 155, topPos + 111, leftPos + 116, topPos + 103,
                    currentScale, currentOffset);
        }

        if (machine == null) return;

        RocketTarget from = ItemVOTVdrive.getTarget(machine.getItem(TileEntityMachineRocketAssembly.SLOT_DRIVES + menu.getLayer() * 2), machine.getLevel());
        RocketTarget to = ItemVOTVdrive.getTarget(machine.getItem(TileEntityMachineRocketAssembly.SLOT_DRIVES + menu.getLayer() * 2 + 1), machine.getLevel());

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);

        List<Component> issues = rocket.findIssues(stage, from.body(), to.body(), from.inOrbit(), to.inOrbit());
        for (int i = 0; i < issues.size(); i++) {
            GuiDraw.text(graphics, font, issues.get(i), (leftPos + 65) * 2, (topPos + 5) * 2 + i * 8, 0xFFFFFF, true);
        }

        if (from.body() != null) GuiDraw.text(graphics, font, Component.translatable("body." + from.body().name), (leftPos + 162) * 2, (topPos + 75) * 2, 0x00FF00, false);
        if (to.body() != null) GuiDraw.text(graphics, font, Component.translatable("body." + to.body().name), (leftPos + 162) * 2, (topPos + 108) * 2, 0x00FF00, false);

        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRocketAssembly machine = menu.getAssembly();

        if (checkClick(mouseX, mouseY, 17, 34, 18, 8)) {
            GuiDraw.blit(graphics, TEXTURE, 17, 34, imageWidth, 36F, 18, 8, 256, 256);
        }

        if (checkClick(mouseX, mouseY, 17, 98, 18, 8)) {
            GuiDraw.blit(graphics, TEXTURE, 17, 98, imageWidth, 44F, 18, 8, 256, 256);
        }

        if (rocket().validate()) {
            GuiDraw.blit(graphics, TEXTURE, 41, 62, imageWidth + 18, 8F, 18, 18, 256, 256);
        }

        if (machine != null && machine.canDeconstruct()) {
            GuiDraw.blit(graphics, TEXTURE, 39, 52, imageWidth + 36, 8F, 20, 38, 256, 256);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        double x = event.x(), y = event.y();

        if (minecraft.gameMode != null) {
            // Stage up
            if (checkClick(x, y, 17, 34, 18, 8)) {
                GuiUtil.playClickSound();
                if (menu.getLayer() > 0) setLayer(menu.getLayer() - 1);
                return true;
            }

            // Stage down
            if (checkClick(x, y, 17, 98, 18, 8)) {
                GuiUtil.playClickSound();
                if (menu.getLayer() < Math.min(rocket().stages.size(), RocketStruct.MAX_STAGES - 1)) setLayer(menu.getLayer() + 1);
                return true;
            }

            // Construct rocket
            if (checkClick(x, y, 41, 62, 18, 18)) {
                TileEntityMachineRocketAssembly machine = menu.getAssembly();
                if (rocket().validate()) {
                    GuiUtil.playClickSound();
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerMachineRocketAssembly.BUTTON_CONSTRUCT);
                    return true;
                } else if (machine != null && machine.canDeconstruct()) {
                    GuiUtil.playClickSound();
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerMachineRocketAssembly.BUTTON_DECONSTRUCT);
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void setLayer(int layer) {
        menu.setLayer(layer);
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, layer);
    }
}
