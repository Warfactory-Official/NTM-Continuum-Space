package com.ntmspace.client.gui.rocket;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.station.ContainerOrbitalStationLauncher;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketTarget;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationLauncher;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUIOrbitalStationLauncher}: {@code gui_orbital_launcher.png} at 192 x 224. The stage arrows
 * (17, 34) and (17, 98) page through the stages (hover-lit at u 192, v 36 / 44), the stage lamp (47, 39) shows the shown
 * stage, "COMMIT TO LAUNCH" (41, 12, 18 x 17) sends the launch control when the rocket is valid. With a rocket docked
 * only "Ready for launch!" (74, 11, green) is drawn. Otherwise the shown stage turns in the bay (scissor 65, 5, 90 x 106)
 * with its scale and offset easing towards the stage's (4 per second), the rocket's issue list at half size from
 * (65, 5) with shadow, the stage drives' body names at (162, 75) and (162, 108), and the launcher's own issues (tanks,
 * drive, travel) at half size stacked upwards from (65, 111). No labels (1.7 drew none).
 */
public class GUIOrbitalStationLauncher extends CompatContainerScreen<ContainerOrbitalStationLauncher> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_orbital_launcher.png");

    private final RocketPreview preview = new RocketPreview();

    private double currentOffset = 0;
    private double currentScale = 1;
    private long lastTime = 0;

    public GUIOrbitalStationLauncher(ContainerOrbitalStationLauncher menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 192, 224);
    }

    @Override
    protected void init() {
        super.init();
        lastTime = System.nanoTime();
    }

    private RocketStruct rocket() {
        TileEntityOrbitalStationLauncher machine = menu.getLauncher();
        return machine != null ? machine.rocket : new RocketStruct();
    }

    /** 1.7 {@code checkClick} in screen coordinates. */
    private boolean checkClick(double x, double y, int left, int top, int width, int height) {
        return leftPos + left <= x && leftPos + left + width > x && topPos + top < y && topPos + top + height >= y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityOrbitalStationLauncher machine = menu.getLauncher();
        RocketStruct rocket = rocket();

        int stage = Math.max(rocket.stages.size() - 1 - menu.getLayer(), -1);

        GuiDraw.blit(graphics, TEXTURE, leftPos + 47, topPos + 39, imageWidth + 18 + (stage + 1) * 6, 0F, 6, 8, 256, 256);

        stage = Math.max(stage, 0);

        double dt = (double) (System.nanoTime() - lastTime) / 1000000000;
        lastTime = System.nanoTime();

        if (machine != null && machine.hasDocked) {
            GuiDraw.text(graphics, font, Component.literal("Ready for launch!"), leftPos + 74, topPos + 11, 0x00FF00, false);
            return;
        }

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

        RocketTarget from = ItemVOTVdrive.getTarget(machine.getItem(TileEntityOrbitalStationLauncher.SLOT_DRIVES + menu.getLayer() * 2), machine.getLevel());
        RocketTarget to = ItemVOTVdrive.getTarget(machine.getItem(TileEntityOrbitalStationLauncher.SLOT_DRIVES + menu.getLayer() * 2 + 1), machine.getLevel());

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);

        List<Component> issues = rocket.findIssues(stage, from.body(), to.body(), from.inOrbit(), to.inOrbit());
        for (int i = 0; i < issues.size(); i++) {
            GuiDraw.text(graphics, font, issues.get(i), (leftPos + 65) * 2, (topPos + 5) * 2 + i * 8, 0xFFFFFF, true);
        }

        if (from.body() != null) GuiDraw.text(graphics, font, Component.translatable("body." + from.body().name), (leftPos + 162) * 2, (topPos + 75) * 2, 0x00FF00, false);
        if (to.body() != null) GuiDraw.text(graphics, font, Component.translatable("body." + to.body().name), (leftPos + 162) * 2, (topPos + 108) * 2, 0x00FF00, false);

        List<Component> launchIssues = machine.findIssues();
        for (int i = 0; i < launchIssues.size(); i++) {
            GuiDraw.text(graphics, font, launchIssues.get(i), (leftPos + 65) * 2, (topPos + 111) * 2 + i * 8 - launchIssues.size() * 8, 0xFFFFFF, false);
        }

        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (checkClick(mouseX, mouseY, 17, 34, 18, 8)) {
            GuiDraw.blit(graphics, TEXTURE, 17, 34, imageWidth, 36F, 18, 8, 256, 256);
        }

        if (checkClick(mouseX, mouseY, 17, 98, 18, 8)) {
            GuiDraw.blit(graphics, TEXTURE, 17, 98, imageWidth, 44F, 18, 8, 256, 256);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        double x = event.x(), y = event.y();
        boolean handled = false;

        // 1.7 ran the default click first and then tested every button
        if (minecraft.gameMode != null) {
            // Stage up
            if (checkClick(x, y, 17, 34, 18, 8)) {
                GuiUtil.playClickSound();
                if (menu.getLayer() > 0) setLayer(menu.getLayer() - 1);
                handled = true;
            }

            // Stage down
            if (checkClick(x, y, 17, 98, 18, 8)) {
                GuiUtil.playClickSound();
                if (menu.getLayer() < Math.min(rocket().stages.size(), RocketStruct.MAX_STAGES - 1)) setLayer(menu.getLayer() + 1);
                handled = true;
            }
        }

        // COMMIT TO LAUNCH
        TileEntityOrbitalStationLauncher machine = menu.getLauncher();
        if (machine != null && rocket().validate() && checkClick(x, y, 41, 12, 18, 17)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("launch", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(machine.getBlockPos(), data));
            handled = true;
        }

        return handled || super.mouseClicked(event, doubleClick);
    }

    private void setLayer(int layer) {
        menu.setLayer(layer);
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, layer);
    }
}
