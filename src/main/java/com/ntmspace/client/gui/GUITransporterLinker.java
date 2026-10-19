package com.ntmspace.client.gui;

import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.item.ItemTransporterLinker.TransporterInfo;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code GUITransporterLinker}: {@code gui_transport_linker.png}, 218 x 201. The transporter it was opened on
 * at the top (its body's icon at (37, 14), name at (62, 13), "x: , z: " right-aligned to 188 at y 23, all green) with
 * the unlink button at (10, 13); a search field at (59, 46) (white, focused, its frame drawn from (0, 201) while
 * focused); five rows of saved transporters 26 apart from y 70 (icon, name, coordinates; the link button at (10, 69 +
 * 26n), lit from (218, 18) for the current link, hover (218, 0) or (236, 0)); the scrollbar knob (14 x 15 from (218,
 * 54)) at x 202 when there are more than five, dragged in (198, 64) 22 x 137 or moved by the wheel. Clicking a row links
 * the two transporters (or unlinks if it was the link) and closes the screen; the screen closes when the linker leaves
 * the hand. Transporters linked to someone else are not listed.
 */
public final class GUITransporterLinker extends CompatScreen {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/tool/gui_transport_linker.png");

    private final int xSize = 218;
    private final int ySize = 201;
    private int guiLeft;
    private int guiTop;

    private EditBox search;

    private int index = 0;

    private final TransporterInfo linkFromTransporter;
    private final @Nullable TransporterInfo linkToTransporter;
    private final List<TransporterInfo> transporters;

    private List<TransporterInfo> visibleTransporters = new ArrayList<>();

    private final Player player;
    private boolean dragging;

    public GUITransporterLinker(Player player, List<TransporterInfo> transporters, TransporterInfo linkFromTransporter) {
        super(Component.empty());
        this.player = player;
        this.linkFromTransporter = linkFromTransporter;
        this.linkToTransporter = linkFromTransporter.linkedTo;
        this.transporters = transporters;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(player.getMainHandItem().getItem() instanceof ItemTransporterLinker)) onClose();
    }

    @Override
    protected void init() {
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;

        search = new EditBox(font, guiLeft + 59, guiTop + 46, 86, 12, Component.empty());
        search.setTextColor(0xFFFFFFFF);
        search.setTextColorUneditable(0xFFFFFFFF);
        search.setBordered(false);
        search.setResponder(text -> updateSearch());
        addRenderableWidget(search);
        setFocused(search);
        search.setFocused(true);
        focusedEditBoxForInput = search;

        updateSearch();
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (dragging) mouseClickMove(mouseX, mouseY);

        int hideScroll = visibleTransporters.size() <= 5 ? 20 : 0;

        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0F, 0F, xSize - hideScroll, ySize, 256, 256);

        // Draw search box
        if (search.isFocused()) GuiDraw.blit(graphics, TEXTURE, guiLeft + 55, guiTop + 41, 0F, ySize, 107, 18, 256, 256);

        // Draw current transporter
        int x = guiLeft + 37;
        int textLeftX = guiLeft + 62;
        int textRightX = guiLeft + 188;
        GuiDraw.blit(graphics, linkFromTransporter.planet, x, guiTop + 14, 0F, 0F, 16, 16, 16, 16);

        String coordinates = "x: " + linkFromTransporter.x + ", z: " + linkFromTransporter.z;
        int width = font.width(coordinates);
        GuiDraw.text(graphics, font, linkFromTransporter.name, textLeftX, guiTop + 13, 0x00ff00, true);
        GuiDraw.text(graphics, font, coordinates, textRightX - width, guiTop + 23, 0x00ff00, true);

        // Draw linkable transporters
        for (int i = index; i < Math.min(index + 5, visibleTransporters.size()); i++) {
            TransporterInfo transporter = visibleTransporters.get(i);
            int y = guiTop + 70 + (i - index) * 26;

            GuiDraw.blit(graphics, transporter.planet, x, y, 0F, 0F, 16, 16, 16, 16);

            coordinates = "x: " + transporter.x + ", z: " + transporter.z;
            width = font.width(coordinates);
            GuiDraw.text(graphics, font, transporter.name, textLeftX, y - 1, 0x00ff00, true);
            GuiDraw.text(graphics, font, coordinates, textRightX - width, y + 9, 0x00ff00, true);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = guiLeft + 10;

        if (isInAABB(mouseX, mouseY, x, guiTop + 13, 18, 18)) {
            GuiDraw.blit(graphics, TEXTURE, x, guiTop + 13, xSize + 18, 0F, 18, 18, 256, 256);
        }

        for (int i = index; i < Math.min(index + 5, visibleTransporters.size()); i++) {
            TransporterInfo transporter = visibleTransporters.get(i);

            int y = guiTop + 69 + (i - index) * 26;
            if (linkToTransporter != null && linkToTransporter.equals(transporter)) {
                GuiDraw.blit(graphics, TEXTURE, x, y, xSize, 18F, 18, 18, 256, 256);

                if (isInAABB(mouseX, mouseY, x, y, 18, 18)) {
                    GuiDraw.blit(graphics, TEXTURE, x, y, xSize + 18, 0F, 18, 18, 256, 256);
                }
            } else {
                if (isInAABB(mouseX, mouseY, x, y, 18, 18)) {
                    GuiDraw.blit(graphics, TEXTURE, x, y, xSize, 0F, 18, 18, 256, 256);
                }
            }
        }

        // Draw scrollbar
        if (visibleTransporters.size() > 5) {
            x = guiLeft + 202;
            int y = guiTop + 68 + Mth.floor((float) index / (float) (visibleTransporters.size() - 5) * 114.0F);
            GuiDraw.blit(graphics, TEXTURE, x, y, xSize, 54F, 14, 15, 256, 256);
        }
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && index > 0) index--;
        if (scrollY < 0 && index < visibleTransporters.size() - 5) index++;
        return true;
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        dragging = true;
        double mouseX = event.x(), mouseY = event.y();
        int x = guiLeft + 10;

        if (isInAABB(mouseX, mouseY, x, guiTop + 13, 18, 18)) {
            if (selectTransporter(null)) {
                onClose();
                return true;
            }
        }

        for (int i = index; i < Math.min(index + 5, visibleTransporters.size()); i++) {
            int y = guiTop + 69 + (i - index) * 26;
            if (isInAABB(mouseX, mouseY, x, y, 18, 18)) {
                if (selectTransporter(visibleTransporters.get(i))) {
                    onClose();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean mouseReleasedCompat(MouseInput event) {
        dragging = false;
        return false;
    }

    /** 1.7 {@code mouseClickMove}: the scrollbar follows a drag inside its track. */
    private void mouseClickMove(int mouseX, int mouseY) {
        if (isInAABB(mouseX, mouseY, guiLeft + 198, guiTop + 64, 22, 137)) {
            index = Mth.floor((mouseY - (guiTop + 68)) / 114.0F * (float) (visibleTransporters.size() - 5));
            index = Mth.clamp(index, 0, Math.max(0, visibleTransporters.size() - 5));
        }
    }

    // Links commutatively
    private boolean selectTransporter(@Nullable TransporterInfo transporter) {
        if (linkFromTransporter.equals(transporter)) return false;

        if (linkToTransporter != null && linkToTransporter.equals(transporter)) {
            linkTransporters(linkFromTransporter, null);
        } else {
            linkTransporters(linkFromTransporter, transporter);
        }

        return true;
    }

    private static void linkTransporters(@Nullable TransporterInfo from, @Nullable TransporterInfo to) {
        if (from == null && to == null) return;

        if (to == null) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("unlink", true);
            send(from, data);
            return;
        } else if (from == null) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("unlink", true);
            send(to, data);
            return;
        }

        CompoundTag data = new CompoundTag();
        data.putString("dimension", to.dimension);
        data.putIntArray("linkedTo", new int[] {to.x, to.y, to.z});
        send(from, data);
    }

    private static void send(TransporterInfo at, CompoundTag data) {
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(at.pos(), data));
    }

    private void updateSearch() {
        visibleTransporters = new ArrayList<>();
        index = 0;

        String subs = search.getValue().toLowerCase(Locale.US);

        for (TransporterInfo transporter : transporters) {
            if (transporter.equals(linkFromTransporter)) continue;
            if (transporter.linkedTo != null && !transporter.linkedTo.equals(linkFromTransporter)) continue;
            if (transporter.name.toLowerCase(Locale.US).contains(subs)) {
                visibleTransporters.add(transporter);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInAABB(double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }

    /** Opens the screen (the linker's client-side use). */
    public static void open(TransporterInfo from) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.setScreen(new GUITransporterLinker(minecraft.player, ItemTransporterLinker.currentTransporters, from));
    }
}
