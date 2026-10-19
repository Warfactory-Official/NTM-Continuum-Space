package com.ntmspace.client.gui.rocket;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.rocket.ContainerLaunchPadRocket;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * hbm-space's {@code GUILaunchPadRocket}: {@code gui_launchpad_rocket.png} at 188 x 236. The power bar (167, 36, 16 x 52,
 * from u 188 v 8), the launch lamp (59, 43: lit at u 200 when the pad can launch), the power lamp (172, 23), the rocket
 * turning in the bay (scissor 97, 18, 50 x 106, standing at (122, 119), scale 86 / height) and the issue list at half
 * size from (6, 66). "COMMIT TO LAUNCH" (56, 20, 18 x 17) sends the launch control. No labels (1.7 drew none).
 */
public class GUILaunchPadRocket extends CompatContainerScreen<ContainerLaunchPadRocket> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_launchpad_rocket.png");

    private final RocketPreview preview = new RocketPreview();

    public GUILaunchPadRocket(ContainerLaunchPadRocket menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 188, 236);
    }

    private boolean checkClick(double x, double y, int left, int top, int width, int height) {
        return leftPos + left <= x && leftPos + left + width > x && topPos + top < y && topPos + top + height >= y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityLaunchPadRocket machine = menu.getPad();
        if (machine == null) return;

        int p = (int) (machine.power * 52 / machine.maxPower);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 167, topPos + 36 + 52 - p, imageWidth, 8 + 52 - p, 16, p, 256, 256);

        if (machine.rocket != null) {
            int ox = machine.canLaunch() ? 12 : 0;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 59, topPos + 43, imageWidth + ox, 0F, 12, 8, 256, 256);

            ox = machine.power > machine.maxPower * 0.75 ? 0 : 6;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 172, topPos + 23, imageWidth + 16 + ox, 8F, 6, 8, 256, 256);

            double size = 86;
            double height = machine.rocket.getHeight();
            double targetScale = size / Math.max(height, 6);
            preview.draw(graphics, machine.rocket, leftPos + 97, topPos + 18, leftPos + 147, topPos + 124, leftPos + 122, topPos + 119,
                    targetScale, 0);

            GuiDraw.pushMatrix(graphics);
            GuiDraw.scale(graphics, 0.5F, 0.5F);
            List<Component> issues = machine.findIssues();
            for (int i = 0; i < issues.size(); i++) {
                GuiDraw.text(graphics, font, issues.get(i), (leftPos + 6) * 2, (topPos + 66) * 2 + i * 8, 0xFFFFFF, false);
            }
            GuiDraw.popMatrix(graphics);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityLaunchPadRocket machine = menu.getPad();
        if (machine == null) return;
        if (isHovering(167, 36, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(machine.power, machine.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(56, 20, 18, 17, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("COMMIT TO LAUNCH")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityLaunchPadRocket machine = menu.getPad();
        // COMMIT TO LAUNCH
        if (machine != null && machine.rocket != null && machine.rocket.validate() && checkClick(event.x(), event.y(), 56, 20, 18, 17)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("launch", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(machine.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
