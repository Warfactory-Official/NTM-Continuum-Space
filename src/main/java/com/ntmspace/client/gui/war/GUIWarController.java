package com.ntmspace.client.gui.war;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.items.ISatChip;
import com.hbm.packet.ControlEventPayload;
import com.ntmspace.NtmSpace;
import com.ntmspace.inventory.war.ContainerMachineWarController;
import com.ntmspace.tileentity.war.TileEntityMachineWarController;
import dev.golem.core.gui.CharInput;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.net.GolemNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's {@code GUIWarController}: {@code gui/machine/gui_controlpanel.png} at 176 x 204 with a green terminal. What
 * is typed shows at half size at (78, 57) as {@code > text_} (the cursor blinks on the world clock); Enter runs it and
 * the last six lines of history show above it from (78, 15), 6 px apart, in green ({@code 0x00FF00}; lines that carry a
 * colour code keep it). The 1.7 text field itself sat off-screen at (-74, 28) and was never drawn; it is kept as an
 * unrendered, always-focused edit box (100 characters) that takes every key but Escape and Enter.
 *
 * <p>Commands, 1.7's: {@code setpos <x> <z>} (writes the coordinates onto the drive in slot 1 through the control
 * packet), {@code health}, {@code getsat}, {@code fire} (only answers: its packet was commented out in 1.7) - with 1.7's
 * replies, including "Wrong satellite" followed by a red code that colours nothing, and {@code fire} not echoing the
 * command. History keeps the last twelve lines. Hovering (132, 18, 16 x 52) shows "0/0HE" (1.7
 * {@code drawElectricityInfo} of a machine without a buffer).
 */
public class GUIWarController extends CompatContainerScreen<ContainerMachineWarController> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_controlpanel.png");
    private static final int MAX_HISTORY = 6; // Number of lines to keep visible (1.7)

    private List<String> commandHistory = new ArrayList<>();
    private EditBox textField;

    public GUIWarController(ContainerMachineWarController menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
    }

    @Override
    protected void init() {
        super.init();
        String previous = textField == null ? "" : textField.getValue();
        textField = new EditBox(this.font, leftPos - 74, topPos + 28, 100, 20, Component.empty());
        textField.setMaxLength(100);
        textField.setBordered(false);
        textField.setValue(previous);
        textField.setFocused(true);
    }

    /** What is typed (harness view). */
    public String typed() {
        return textField == null ? "" : textField.getValue();
    }

    /** The command history (harness view). */
    public List<String> history() {
        return List.copyOf(commandHistory);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int yOffset = 15;
        float scale = 0.5F;
        for (int x = Math.max(0, commandHistory.size() - MAX_HISTORY); x < commandHistory.size(); x++) {
            GuiDraw.pushMatrix(graphics);
            GuiDraw.scale(graphics, scale, scale);
            GuiDraw.text(graphics, font, commandHistory.get(x), (int) (78 / scale), (int) (yOffset / scale), 0x00FF00, false);
            GuiDraw.popMatrix(graphics);
            yOffset += (int) (12 * scale);
        }

        Minecraft mc = Minecraft.getInstance();
        boolean blink = mc.level != null && mc.level.getGameTime() % 20 < 10;
        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, scale, scale);
        GuiDraw.text(graphics, font, "> " + typed() + (textField != null && textField.isFocused() && blink ? "_" : ""),
                (int) (78 / scale), (int) (57 / scale), 0x00FF00, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 drawElectricityInfo(132, 18, 16, 52, getPower() = 0, getMaxPower() = 0)
        if (leftPos + 132 <= mouseX && leftPos + 132 + 16 > mouseX && topPos + 18 < mouseY && topPos + 18 + 52 >= mouseY) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(0, 0), mouseX, mouseY);
        }
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        int key = event.key();
        if (key == 256) return false; // Escape: the screen closes (1.7 super.keyTyped)
        if (key == 257 || key == 335) {
            processCommand(typed().trim());
            textField.setValue(""); // Clear after processing
            return true;
        }
        // 1.7: the field took the key first; anything it did not take (the inventory key included) was typed into it
        event.keyPressed(textField);
        return true;
    }

    @Override
    protected boolean charTyped(CharInput event) {
        event.charTyped(textField);
        return true;
    }

    /** Runs one terminal command (1.7 {@code processCommand}); public for the harness. */
    public void processCommand(String command) {
        if (command.isEmpty()) return;

        TileEntityMachineWarController sucker = menu.getTile();
        ItemStack chip = menu.getSlot(2).getItem();
        ItemStack drive = menu.getSlot(1).getItem();
        int satId = ISatChip.getFreqS(chip);
        boolean sat = sucker != null && sucker.satPresent && sucker.id == satId;
        String[] parts = command.split(" ");

        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "setpos" -> {
                if (parts.length < 3) {
                    addCommandHistory("> " + command);
                    addCommandHistory("Error, invalid args", ChatFormatting.RED);
                    return;
                }
                String xValueStr = parts[1];
                String zValueStr = parts[2];
                if (drive.isEmpty()) {
                    addCommandHistory("> " + command);
                    addCommandHistory("No drive.", ChatFormatting.RED);
                    return;
                }
                if (!xValueStr.matches("-?\\d+") || !zValueStr.matches("-?\\d+")) {
                    addCommandHistory("> " + command);
                    addCommandHistory("Invalid number format.", ChatFormatting.RED);
                    return;
                }
                int xValue;
                int zValue;
                try {
                    xValue = Integer.parseInt(xValueStr);
                    zValue = Integer.parseInt(zValueStr);
                } catch (NumberFormatException e) {
                    // 1.7 threw here for numbers beyond an int; the port reports them as malformed
                    addCommandHistory("> " + command);
                    addCommandHistory("Invalid number format.", ChatFormatting.RED);
                    return;
                }
                addCommandHistory("> " + command);
                addCommandHistory("Set to: X=" + xValue + ", Z=" + zValue);

                if (sucker != null) {
                    CompoundTag data = new CompoundTag();
                    data.putInt("xcoord", xValue);
                    data.putInt("zcoord", zValue);
                    GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(sucker.getBlockPos(), data));
                }
            }
            case "health" -> {
                addCommandHistory("> " + command);
                addCommandHistory("Requesting " + cmd + "...");
                if (!sat) {
                    addCommandHistory("Satellite not in orbit!", ChatFormatting.RED);
                } else {
                    addCommandHistory("health: " + sucker.satHealth);
                }
            }
            case "fire" -> {
                if (!sat) {
                    addCommandHistory("Satellite not in orbit!", ChatFormatting.RED);
                } else if (isWar(sucker.satName)) {
                    addCommandHistory("Firing!");
                    // PacketDispatcher.wrapper.sendToServer(new SatActivatePacket(satId)); (commented out in 1.7)
                } else {
                    addCommandHistory("Wrong satellite" + ChatFormatting.RED);
                }
            }
            case "getsat" -> {
                addCommandHistory("> " + command);
                if (chip.isEmpty()) {
                    addCommandHistory("No satellite chip in slot 2.", ChatFormatting.RED);
                } else {
                    addCommandHistory("Requesting Satellite ID: " + satId);
                    if (!sat) {
                        addCommandHistory("Satellite not in orbit!", ChatFormatting.RED);
                    } else {
                        addCommandHistory("Satellite: " + sucker.satName);
                    }
                }
            }
            default -> {
                addCommandHistory("> " + command);
                addCommandHistory("Unknown command.", ChatFormatting.RED);
            }
        }
    }

    /** 1.7 {@code sat instanceof SatelliteWar}: the war satellites' class names. */
    private static boolean isWar(String simpleName) {
        return "SatelliteWar".equals(simpleName) || "SatelliteRailgun".equals(simpleName);
    }

    private void addCommandHistory(String text) {
        addToHistory(text, null);
    }

    private void addCommandHistory(String text, ChatFormatting color) {
        addToHistory(text, color);
    }

    /** 1.7: a coloured line is {@code ChatComponentText.getFormattedText()}, i.e. the colour code, the text and a reset. */
    private void addToHistory(String text, ChatFormatting color) {
        if (color != null) {
            commandHistory.add(color + text + ChatFormatting.RESET);
        } else {
            commandHistory.add(text);
        }

        if (commandHistory.size() > MAX_HISTORY * 2) {
            commandHistory = new ArrayList<>(commandHistory.subList(commandHistory.size() - MAX_HISTORY * 2, commandHistory.size()));
        }
    }
}
