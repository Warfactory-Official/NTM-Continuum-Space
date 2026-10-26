package com.ntmspace.client.gui.machine;

import com.hbm.inventory.gui.GuiUtil;
import com.hbm.packet.ControlEventPayload;
import com.mojang.math.Axis;
import com.ntmspace.NtmSpace;
import com.ntmspace.client.StardarLayers;
import com.ntmspace.inventory.machine.ContainerStardar;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SolarSystem.Body;
import com.ntmspace.world.StardarMath;
import dev.golem.core.Ids;
import dev.golem.core.client.DynamicTextures;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.render.GuiCanvas;
import dev.golem.core.render.Sky;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code GUIMachineStardar} (176 x 256, {@code machine/gui_stardar.png}): the solar-system map (an isometric
 * map of the system on {@code starmap3.png}: orbits, bodies turning, moons in front of and behind their planet, rings,
 * labels; drag to pan, wheel to zoom, click a body to focus it, click it again to land, right click to step back) and
 * the landing view (the target's 256 x 256 height map, 2 px per block, green by height mod 16; drag to move, click a
 * flat 5 x 5 spot to set the drive's landing coordinates). Three buttons: focus the current body, program a new orbital
 * station, program the current body.
 *
 * <p>All math (orbits, iso projection, zoom bounds, focus animation, crescent phase and moon eclipses) is 1.7's. The
 * system map is drawn on a Golem {@link GuiCanvas} in 1.7's order, as 1.7 tesselated it: sub-pixel quads, textures
 * repeating (a turning body's offset is one quad), {@code glColor} tints, orbit lines as thin quads 2 framebuffer pixels
 * wide (1.7 {@code glLineWidth(2)}), and each body's layers ({@link StardarLayers}): the atmosphere glow band, the disc,
 * then the 1.7 GLSL layers through Golem's shader quads - atmosphere ({@code atmosphere.frag}), crescent
 * ({@code crescent.frag}), emissive air and city lights ({@code atmosphere_emissive.frag}, {@code nightlights.frag}),
 * lightning ({@code lightning.frag}), the impact lava fading in, nuke flashes and rings ({@code nuke.frag}). The body
 * labels are GUI text over it; the landing view is GUI blits.
 *
 * <p>Artificial satellites (the current body's satellites with orbit lines and tooltips) are left out: HBM's satellites
 * carry no client copy or orbital elements. 1.7's fling on release in the landing view never ran (the release cleared
 * the drag flag first), so it is not here either.
 */
public class GUIMachineStardar extends CompatContainerScreen<ContainerStardar> {

    private static final Identifier TEXTURE = Ids.of(NtmSpace.MOD_ID, "textures/gui/machine/gui_stardar.png");
    private static final Identifier STARMAP = Ids.of(NtmSpace.MOD_ID, "textures/gui/starmap3.png");
    private static final Sky.Texture STARMAP_CANVAS = Sky.Texture.of(NtmSpace.MOD_ID, "textures/gui/starmap3.png");
    private static final Sky.Texture RINGS = Sky.Texture.of(NtmSpace.MOD_ID, "textures/misc/space/rings.png");
    private static final Identifier GROUND_MAP = Ids.of(NtmSpace.MOD_ID, "dynamic/stardar_ground_map");

    private static final int MAP_X = 9;
    private static final int MAP_Y = 9;
    private static final int MAP_W = 158;
    private static final int MAP_H = 108;

    private static final float MAP_TEX_W = 1024F;
    private static final float MAP_TEX_H = 1024F;
    private static final float STARMAP_BG_TEX_W = 1024F;
    private static final float STARMAP_BG_TEX_H = 1024F;
    private static final float SYSTEM_LAYOUT_REF_TEX = 256F;

    private static final float MIN_ZOOM = 0.5F;
    private static final float MAX_ZOOM = 180.0F;
    private static final float ZOOM_STEP = 1.1F;

    private static final float ISO_X = 0.70F;
    private static final float ISO_Y = 0.35F;
    private static final float STARDAR_BODY_SCALE = 0.45F;
    private static final float STARDAR_MOON_VISUAL_SCALE = 0.82F;
    private static final float STARDAR_SYSTEM_RADIUS_FRACTION = 0.50F;

    /** Destination units per blit under the pose scale, and the virtual texture size for UV precision. */
    private static final int Q = 256;
    private static final int UV = 4096;

    /** 1.7 {@code hasTransparentPixels} per texture, shared by every screen. */
    private static final Map<Identifier, Boolean> TEXTURE_ALPHA = new HashMap<>();

    private final @Nullable TileEntityMachineStardar star;
    private int @Nullable [] uploadedHeightmap;

    private final List<BodyRenderInfo> renderedBodies = new ArrayList<>();
    private final List<BodyLabelRenderInfo> renderedBodyLabels = new ArrayList<>();
    private final BodyPosition trackedBodyPosition = new BodyPosition();

    private final Body currentBody;
    private @Nullable Body focusedBody;
    private @Nullable Body focusAnimationBody;
    private @Nullable BodyRenderInfo hoveredBody;
    private @Nullable Body landingBody;

    private boolean landingMode;
    private boolean hadFullDrive;
    private @Nullable Item lastFullDrive;

    private boolean leftDown;
    private boolean draggingMap;
    private int dragLastX;
    private int dragLastY;

    private int mX;
    private int mY;
    private int lX;
    private int lY;
    private int sX;
    private int sY;
    private boolean draggingSurface;

    private float surfaceX;
    private float surfaceY;
    private float velocityX;
    private float velocityY;

    private float mapU = 0F;
    private float mapV = 0F;
    private float mapZoom = 1.0F;

    private boolean focusAnimationActive = false;
    private boolean focusAnimationToBody = false;
    private float focusAnimStartCenterU = 0F;
    private float focusAnimStartCenterV = 0F;
    private float focusAnimStartZoom = 1.0F;
    private float focusAnimTargetCenterU = 0F;
    private float focusAnimTargetCenterV = 0F;
    private float focusAnimTargetZoom = 1.0F;
    private boolean focusAnimTrackingInitialized = false;
    private float focusAnimTrackingOffsetU = 0F;
    private float focusAnimTrackingOffsetV = 0F;
    private long focusAnimStartTimeMs = 0L;
    private int focusAnimDurationMs = 500;

    private int lastMouseX;
    private int lastMouseY;

    public GUIMachineStardar(ContainerStardar menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        this.star = menu.getTile();
        Minecraft mc = Minecraft.getInstance();
        this.currentBody = mc.level != null ? Orbit.bodyOf(mc.level) : SolarSystem.body("kerbin");
        this.focusedBody = null;

        setViewFromCenter(MAP_TEX_W * 0.5F, MAP_TEX_H * 0.5F, mapZoom);
    }

    // ------------------------------------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        updateLandingModeFromDrive();

        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        graphics.enableScissor(leftPos + MAP_X, topPos + MAP_Y, leftPos + MAP_X + MAP_W, topPos + MAP_Y + MAP_H);
        if (landingMode) {
            drawSurfaceMap(graphics);
            drawLandingMarker(graphics);
        } else {
            GuiCanvas.draw(graphics, leftPos + MAP_X, topPos + MAP_Y, leftPos + MAP_X + MAP_W, topPos + MAP_Y + MAP_H,
                    canvas -> drawSystemMap(canvas, partialTick));
            drawQueuedBodyLabels(graphics);
            hoveredBody = findBodyUnderCursor(lastMouseX, lastMouseY);
        }
        graphics.disableScissor();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 drew no title or inventory label, only the two status lines
        if (landingMode) {
            drawLandingForeground(graphics);
        } else {
            drawMapForeground(graphics);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        tooltip(graphics, mouseX, mouseY, 129, 124, "gui.stardar.tooltip.focus_current_body");
        tooltip(graphics, mouseX, mouseY, 129, 143, "gui.stardar.tooltip.program_new_station");
        tooltip(graphics, mouseX, mouseY, 149, 143, "gui.stardar.tooltip.program_current_body");
    }

    private void tooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, String key) {
        if (GuiUtil.hover(mouseX, mouseY, leftPos + x, topPos + y, 18, 18)) {
            GuiUtil.tooltip(graphics, font, List.of(Component.translatable(key)), mouseX, mouseY);
        }
    }

    private void label(GuiGraphicsExtractor graphics, String text, int y, int color) {
        GuiDraw.text(graphics, font, text, 10, y, color, false);
    }

    private void drawMapForeground(GuiGraphicsExtractor graphics) {
        Body activeFocus = getRenderFocusBody();
        Body displayBody = hoveredBody != null ? hoveredBody.body : activeFocus;

        if (displayBody == null) {
            label(graphics, I18n.get("gui.stardar.label.drag_map"), 128, 0x00FF00);
            label(graphics, I18n.get("gui.stardar.label.click_body_focus"), 148, 0x00FF00);
            return;
        }

        label(graphics, I18n.get("gui.stardar.label.focus_body", getBodyDisplayName(displayBody)), 128, 0x00FF00);
        if (activeFocus != null && displayBody == activeFocus) {
            if (!canLand(displayBody)) {
                label(graphics, I18n.get("gui.stardar.label.cant_land"), 148, 0xFF4040);
            } else {
                ItemStack slotStack = menu.getSlot(0).getItem();

                if (slotStack.isEmpty()) {
                    label(graphics, I18n.get("gui.stardar.label.insert_drive"), 148, 0x00FF00);
                } else if (slotStack.is(RocketItems.HARD_DRIVE.get())) {
                    label(graphics, I18n.get("gui.stardar.label.click_again_land"), 148, 0x00FF00);
                } else {
                    label(graphics, I18n.get("gui.stardar.label.insert_drive"), 148, 0x00FF00);
                }
            }
        } else {
            label(graphics, I18n.get("gui.stardar.label.click_body_focus"), 148, 0x00FF00);
        }
    }

    private void drawLandingForeground(GuiGraphicsExtractor graphics) {
        if (star == null || star.heightmap == null) {
            label(graphics, I18n.get("gui.stardar.label.loading_surface_map"), 148, 0x00FF00);
            return;
        }

        if (!isMouseInsideMap(lastMouseX, lastMouseY)) {
            label(graphics, I18n.get("gui.stardar.label.select_landing_zone"), 128, 0x00FF00);
            return;
        }

        int hx = (lastMouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
        int hz = (lastMouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
        String info = landingInfo(hx, hz);
        int altitude = altitude(hx, hz);
        boolean canLand = info == null;

        label(graphics, canLand ? I18n.get("gui.stardar.label.valid_location") : info, 128, canLand ? 0x00FF00 : 0xFF0000);
        if (altitude > 0) label(graphics, I18n.get("gui.stardar.label.target_altitude", altitude), 148, 0x00FF00);
    }

    /** The 14 x 14 cursor marker of the landing view (1.7 drew it in the foreground, inside the map's scissor). */
    private void drawLandingMarker(GuiGraphicsExtractor graphics) {
        if (star == null || star.heightmap == null || !isMouseInsideMap(lastMouseX, lastMouseY)) return;

        // 1.7 Java remainder: snaps to the 2 px grid the map is drawn on
        int sx = lastMouseX - ((lastMouseX + (int) surfaceX) % 2);
        int sy = lastMouseY - ((lastMouseY + (int) surfaceY) % 2);
        int hx = (lastMouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
        int hz = (lastMouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
        boolean canLand = landingInfo(hx, hz) == null;

        GuiDraw.blit(graphics, TEXTURE, sx - 6, sy - 6, imageWidth + (canLand ? 14 : 0), 28, 14, 14, 256, 256);
    }

    private void drawSystemMap(GuiCanvas canvas, float partialTicks) {
        updateDragPanning(lastMouseX, lastMouseY);

        float bgSrcW = MAP_W;
        float bgSrcH = MAP_H;
        float bgU = Mth.clamp(mapU + (MAP_W / mapZoom) * 0.5F - bgSrcW * 0.5F, 0F, MAP_TEX_W - bgSrcW);
        float bgV = Mth.clamp(mapV + (MAP_H / mapZoom) * 0.5F - bgSrcH * 0.5F, 0F, MAP_TEX_H - bgSrcH);
        float bgScaleU = STARMAP_BG_TEX_W / MAP_TEX_W;
        float bgScaleV = STARMAP_BG_TEX_H / MAP_TEX_H;
        float bgUTex = bgU * bgScaleU;
        float bgVTex = bgV * bgScaleV;
        float bgSrcWTex = bgSrcW * bgScaleU;
        float bgSrcHTex = bgSrcH * bgScaleV;

        canvas.texturedQuads(STARMAP_CANVAS, Sky.Blend.ALPHA, 1F, 1F, 1F, 1F, StardarMath.partialTex(leftPos + MAP_X, topPos + MAP_Y,
                MAP_W, MAP_H, bgUTex / STARMAP_BG_TEX_W, bgVTex / STARMAP_BG_TEX_H,
                (bgUTex + bgSrcWTex) / STARMAP_BG_TEX_W, (bgVTex + bgSrcHTex) / STARMAP_BG_TEX_H));

        renderedBodies.clear();
        renderedBodyLabels.clear();

        Body root = SolarSystem.KERBOL;

        Minecraft mc = Minecraft.getInstance();
        double dayTicks = mc.level != null ? (double) mc.level.getGameTime() + (double) partialTicks : 0D;
        double worldTicks = dayTicks * (double) SolarSystem.CLIENT_TIME_MULTIPLIER;
        float centerU = MAP_TEX_W * 0.5F;
        float centerV = MAP_TEX_H * 0.5F;
        float systemOrbitScale = getOrbitScalePxPerKm(root);

        updateFocusView(root, centerU, centerV, systemOrbitScale, worldTicks);
        drawBodyTree(canvas, root, centerU, centerV, systemOrbitScale, worldTicks, dayTicks, Float.NaN, Float.NaN);
    }

    private void drawSurfaceMap(GuiGraphicsExtractor graphics) {
        updateSurfaceDrag(lastMouseX, lastMouseY);

        if (!leftDown) {
            velocityX *= 0.85F;
            velocityY *= 0.85F;
            surfaceX += velocityX;
            surfaceY += velocityY;
            surfaceX = Mth.clamp(surfaceX, -256 + MAP_W, 256);
            surfaceY = Mth.clamp(surfaceY, -256 + MAP_H, 256);
        }
        clampSurface();

        quad(graphics, STARMAP, leftPos + MAP_X, topPos + MAP_Y, MAP_W, MAP_H,
                0F, 0F, MAP_W / STARMAP_BG_TEX_W, MAP_H / STARMAP_BG_TEX_H);

        if (star == null || star.heightmap == null) return;

        if (star.updateHeightmap || star.heightmap != uploadedHeightmap) {
            int[] map = star.heightmap;
            int[] colors = new int[256 * 256];
            for (int i = 0; i < map.length && i < colors.length; i++) {
                int h = map[i] % 16 * 16;
                colors[i] = 255 << 24 | h << 8;
            }
            DynamicTextures.register(GROUND_MAP, 256, 256, colors);
            uploadedHeightmap = map;
            star.updateHeightmap = false;
        }

        quad(graphics, GROUND_MAP, leftPos + MAP_X + (int) surfaceX - 256, topPos + MAP_Y + (int) surfaceY - 256, 512, 512,
                0F, 0F, 1F, 1F);
    }

    private void drawBodyTree(GuiCanvas canvas, Body body, float bodyMapU, float bodyMapV, float systemOrbitScalePxPerKm,
                              double worldTicks, double dayTicks, float parentMapU, float parentMapV) {
        if (body == null) return;

        List<ChildRenderState> childStates = new ArrayList<>();
        float parentScreenY = mapToScreenY(bodyMapU, bodyMapV);

        for (Body child : body.satellites) {
            float childScale = getChildOrbitScalePxPerKm(body, systemOrbitScalePxPerKm);
            double meanAnomaly = calculateMeanAnomaly(child, worldTicks);
            float[] offset = calculateOrbitOffsetPx(body, child, meanAnomaly, childScale);

            boolean moonChild = body.parent != null;
            float childMapU = bodyMapU + offset[0];
            float childMapV = bodyMapV + offset[1];
            boolean drawInFrontOfParent = !moonChild || mapToScreenY(childMapU, childMapV) > parentScreenY;

            childStates.add(new ChildRenderState(child, childScale, offset[0], offset[1], moonChild, drawInFrontOfParent));
        }

        for (ChildRenderState childState : childStates) {
            if (!shouldDrawChildOrbit(body, childState.childBody)) continue;

            if (childState.moonChild) {
                drawOrbitHalf(canvas, bodyMapU, bodyMapV, body, childState.childBody, childState.orbitScalePxPerKm, false);
            } else {
                drawOrbit(canvas, bodyMapU, bodyMapV, body, childState.childBody, childState.orbitScalePxPerKm);
            }
        }

        for (ChildRenderState childState : childStates) {
            if (!childState.moonChild || childState.drawInFrontOfParent) continue;
            drawBodyTree(canvas, childState.childBody, bodyMapU + childState.orbitOffsetU, bodyMapV + childState.orbitOffsetV,
                    systemOrbitScalePxPerKm, worldTicks, dayTicks, bodyMapU, bodyMapV);
        }

        drawBody(canvas, body, bodyMapU, bodyMapV, dayTicks, parentMapU, parentMapV);

        for (ChildRenderState childState : childStates) {
            if (!childState.moonChild || !shouldDrawChildOrbit(body, childState.childBody)) continue;
            drawOrbitHalf(canvas, bodyMapU, bodyMapV, body, childState.childBody, childState.orbitScalePxPerKm, true);
        }

        for (ChildRenderState childState : childStates) {
            if (childState.moonChild && !childState.drawInFrontOfParent) continue;
            drawBodyTree(canvas, childState.childBody, bodyMapU + childState.orbitOffsetU, bodyMapV + childState.orbitOffsetV,
                    systemOrbitScalePxPerKm, worldTicks, dayTicks, bodyMapU, bodyMapV);
        }
    }

    private boolean shouldDrawChildOrbit(Body parent, Body child) {
        if (parent == null || child == null) return false;
        if (parent.parent == null) return true;
        if (!isMoon(child)) return true;

        // Moon orbit lines are visible only when the parent body (or one of its moons) is focused.
        Body activeFocus = getRenderFocusBody();
        return activeFocus != null && (activeFocus == parent || activeFocus.parent == parent);
    }

    private int orbitColor(Body body, float alpha) {
        float[] color = body.color != null && body.color.length >= 3 ? body.color : null;
        float r = color != null ? color[0] : 0.8F;
        float g = color != null ? color[1] : 0.8F;
        float b = color != null ? color[2] : 0.8F;
        return argb(r, g, b, alpha);
    }

    private void drawOrbit(GuiCanvas canvas, float parentMapU, float parentMapV, Body parent, Body body, float orbitScalePxPerKm) {
        if (body == null || body.semiMajorAxisKm <= 0F) return;
        int color = orbitColor(body, 0.4F);
        float width = lineWidth(2F);

        float prevX = 0F, prevY = 0F;
        for (int i = 0; i <= 96; i++) {
            double meanAnomaly = 2D * Math.PI * ((double) i / 96D);
            float[] offset = calculateOrbitOffsetPx(parent, body, meanAnomaly, orbitScalePxPerKm);
            float orbitMapU = parentMapU + offset[0];
            float orbitMapV = parentMapV + offset[1];
            float x = mapToScreenX(orbitMapU, orbitMapV);
            float y = mapToScreenY(orbitMapU, orbitMapV);
            if (i > 0) line(canvas, prevX, prevY, x, y, width, color);
            prevX = x;
            prevY = y;
        }
    }

    private void drawOrbitHalf(GuiCanvas canvas, float parentMapU, float parentMapV, Body parent, Body body,
                               float orbitScalePxPerKm, boolean frontHalf) {
        if (body == null || body.semiMajorAxisKm <= 0F) return;
        int color = orbitColor(body, 0.4F);
        float width = lineWidth(2F);

        float crossY = mapToScreenY(parentMapU, parentMapV);
        boolean hasPrevPoint = false;
        float prevScreenX = 0F;
        float prevScreenY = 0F;
        boolean prevInFrontHalf = false;

        for (int i = 0; i <= 96; i++) {
            double meanAnomaly = 2D * Math.PI * ((double) i / 96D);
            float[] offset = calculateOrbitOffsetPx(parent, body, meanAnomaly, orbitScalePxPerKm);
            float orbitMapU = parentMapU + offset[0];
            float orbitMapV = parentMapV + offset[1];
            float currScreenX = mapToScreenX(orbitMapU, orbitMapV);
            float currScreenY = mapToScreenY(orbitMapU, orbitMapV);
            boolean currInFrontHalf = currScreenY >= crossY;

            if (!hasPrevPoint) {
                prevScreenX = currScreenX;
                prevScreenY = currScreenY;
                prevInFrontHalf = currInFrontHalf;
                hasPrevPoint = true;
                continue;
            }

            boolean prevInSelectedHalf = prevInFrontHalf == frontHalf;
            boolean currInSelectedHalf = currInFrontHalf == frontHalf;

            if (prevInSelectedHalf && currInSelectedHalf) {
                line(canvas, prevScreenX, prevScreenY, currScreenX, currScreenY, width, color);
            } else if (prevInSelectedHalf != currInSelectedHalf) {
                // the segment crossing the parent's screen height is cut there
                float dy = currScreenY - prevScreenY;
                float tCross = dy == 0F ? 0.5F : (crossY - prevScreenY) / dy;
                tCross = Mth.clamp(tCross, 0F, 1F);
                float crossX = prevScreenX + (currScreenX - prevScreenX) * tCross;

                if (prevInSelectedHalf) {
                    line(canvas, prevScreenX, prevScreenY, crossX, crossY, width, color);
                } else {
                    line(canvas, crossX, crossY, currScreenX, currScreenY, width, color);
                }
            }

            prevScreenX = currScreenX;
            prevScreenY = currScreenY;
            prevInFrontHalf = currInFrontHalf;
        }
    }

    private void drawBody(GuiCanvas canvas, Body body, float bodyMapU, float bodyMapV, double dayTicks,
                          float parentMapU, float parentMapV) {
        if (body == null) return;

        float drawSize = getBodySizePxAtZoom(body);
        float bodyScreenX = mapToScreenX(bodyMapU, bodyMapV);
        float bodyScreenY = mapToScreenY(bodyMapU, bodyMapV);
        float half = drawSize * 0.5F;
        float ringHalfWidth = 0F;
        float ringHalfHeight = 0F;

        float minX = bodyScreenX - half;
        float maxX = bodyScreenX + half;
        float minY = bodyScreenY - half;
        float maxY = bodyScreenY + half;

        boolean hasRings = body.rings != null;
        if (hasRings) {
            ringHalfWidth = drawSize * 0.5F * Math.max(1F, ringSize(body));
            float ringTiltSin = Math.abs(Mth.sin((float) Math.toRadians(ringTilt(body))));
            ringTiltSin = Math.max(0.08F, ringTiltSin);
            ringHalfHeight = Math.max(0.5F, ringHalfWidth * ringTiltSin);

            minX = Math.min(minX, bodyScreenX - ringHalfWidth);
            maxX = Math.max(maxX, bodyScreenX + ringHalfWidth);
            minY = Math.min(minY, bodyScreenY - ringHalfHeight);
            maxY = Math.max(maxY, bodyScreenY + ringHalfHeight);
        }

        if (maxX < leftPos + MAP_X || minX > leftPos + MAP_X + MAP_W || maxY < topPos + MAP_Y || minY > topPos + MAP_Y + MAP_H) {
            return;
        }

        if (hasRings) drawBodyRingHalf(canvas, body, bodyScreenX, bodyScreenY, ringHalfWidth, ringHalfHeight, false);

        Identifier texture = Ids.of(NtmSpace.MOD_ID, body.texture());
        Sky.Texture surface = Sky.Texture.of(NtmSpace.MOD_ID, body.texture());
        if (body.parent == null) {
            drawTexturedQuad(canvas, surface, bodyScreenX, bodyScreenY, drawSize, 0F);
        } else {
            StardarLayers.glow(canvas, body, bodyScreenX, bodyScreenY, drawSize);

            float phase = getBodyRotationPhase(body, dayTicks);
            float bodyRotationAngle = phase * 360F;
            boolean rotateBody = hasTransparentPixels(texture);
            float textureUOffset = 0F;
            if (rotateBody) {
                drawTexturedQuadRotating(canvas, surface, bodyScreenX, bodyScreenY, drawSize, bodyRotationAngle);
            } else {
                drawTexturedQuad(canvas, surface, bodyScreenX, bodyScreenY, drawSize, phase);
                textureUOffset = phase;
            }
            drawBodyOverlays(canvas, body, bodyScreenX, bodyScreenY, drawSize, bodyMapU, bodyMapV, parentMapU, parentMapV,
                    rotateBody, bodyRotationAngle, dayTicks, textureUOffset);
        }

        if (hasRings) drawBodyRingHalf(canvas, body, bodyScreenX, bodyScreenY, ringHalfWidth, ringHalfHeight, true);

        float labelAnchorExtentX = half;
        float labelAnchorExtentY = half;
        if (body.parent == null) {
            float starLabelHalfExtent = (8F * mapZoom) * 0.5F;
            labelAnchorExtentX = Math.min(labelAnchorExtentX, starLabelHalfExtent);
            labelAnchorExtentY = Math.min(labelAnchorExtentY, starLabelHalfExtent);
        }
        renderedBodyLabels.add(new BodyLabelRenderInfo(body, bodyScreenX + labelAnchorExtentX, bodyScreenY - labelAnchorExtentY));

        renderedBodies.add(new BodyRenderInfo(body, bodyMapU, bodyMapV, drawSize));
    }

    /** 1.7 {@code withRings(tilt, size, color...)}: here {@code rings = {tilt, size, r, g, b}}. */
    private static float ringTilt(Body body) {
        return body.rings[0];
    }

    private static float ringSize(Body body) {
        return body.rings[1];
    }

    private void drawQueuedBodyLabels(GuiGraphicsExtractor graphics) {
        if (renderedBodyLabels.isEmpty()) return;

        List<BodyLabelRenderInfo> sortedLabels = new ArrayList<>(renderedBodyLabels);
        sortedLabels.sort((a, b) -> Integer.compare(getBodyHierarchyDepth(b.body), getBodyHierarchyDepth(a.body)));
        for (BodyLabelRenderInfo labelInfo : sortedLabels) {
            drawBodyLabel(graphics, labelInfo.body, labelInfo.topRightAnchorScreenX, labelInfo.topRightAnchorScreenY);
        }
    }

    private void drawBodyLabel(GuiGraphicsExtractor graphics, Body body, float anchorScreenX, float anchorScreenY) {
        if (!shouldShowBodyLabel(body)) return;

        String label = getBodyDisplayName(body);
        if (label.isEmpty()) return;

        float labelX = anchorScreenX + 1.5F;
        float labelY = anchorScreenY - 1.5F - font.lineHeight * 0.6F;
        int labelColor = colorArrayToRgb(body.color);
        int labelDrawX = Math.round(labelX / 0.6F);
        int labelDrawY = Math.round(labelY / 0.6F);

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.6F, 0.6F);
        GuiDraw.text(graphics, font, label, labelDrawX, labelDrawY, labelColor, true);
        GuiDraw.popMatrix(graphics);
    }

    private boolean shouldShowBodyLabel(Body body) {
        if (body == null || body.parent == null) return false;
        if (!isMoon(body)) return true;

        Body focusedTopLevelPlanet = getTopLevelPlanetForBody(getRenderFocusBody());
        if (focusedTopLevelPlanet == null) return false;
        return getTopLevelPlanetForBody(body) == focusedTopLevelPlanet;
    }

    private void drawBodyRingHalf(GuiCanvas canvas, Body body, float bodyScreenX, float bodyScreenY, float ringHalfWidth,
                                  float drawH, boolean frontHalf) {
        if (ringHalfWidth <= 0F || drawH <= 0F) return;

        float r = body.rings.length >= 5 ? body.rings[2] : 0.5F;
        float g = body.rings.length >= 5 ? body.rings[3] : 0.5F;
        float b = body.rings.length >= 5 ? body.rings[4] : 0.5F;
        float a = body.rings.length >= 6 ? body.rings[5] : 1F;

        float drawX = bodyScreenX - ringHalfWidth;
        float drawY = frontHalf ? bodyScreenY : bodyScreenY - drawH;
        float drawW = ringHalfWidth * 2F;
        float v1 = frontHalf ? 0.5F : 0F;
        float v2 = frontHalf ? 1F : 0.5F;

        // glColor4f(r, g, b, a) over the ring texture's half
        canvas.texturedQuads(RINGS, Sky.Blend.ALPHA, r, g, b, a, StardarMath.partialTex(drawX, drawY, drawW, drawH, 0F, v1, 1F, v2));
    }

    /** {@code drawBodyOverlays}: the crescent phase, then every layer ({@link StardarLayers#overlays}). */
    private void drawBodyOverlays(GuiCanvas canvas, Body body, float bodyScreenX, float bodyScreenY, float drawSize,
                                  float bodyMapU, float bodyMapV, float parentMapU, float parentMapV, boolean rotateBody,
                                  float bodyRotationAngle, double dayTicks, float textureUOffset) {
        if (body == null || body.parent == null) return;

        float phase = calculateHorizontalCrescentPhase(body, bodyMapU, bodyMapV, parentMapU, parentMapV);
        StardarLayers.overlays(canvas, body, bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, dayTicks,
                textureUOffset, phase);
    }

    // 1.7: "yeah no, radius checks and new trait just don't work. drop them transparency checks, planets won't have it
    // anyway, how bad it can be!"
    private static boolean hasTransparentPixels(Identifier texture) {
        Boolean cached = TEXTURE_ALPHA.get(texture);
        if (cached != null) return cached;

        boolean hasAlpha = false;
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    int[] size = new int[2];
                    int[] pixels = DynamicTextures.decodePng(stream.readAllBytes(), size);
                    for (int pixel : pixels) {
                        if ((pixel >>> 24) < 255) {
                            hasAlpha = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 1.7 ignored unreadable textures too
        }

        TEXTURE_ALPHA.put(texture, hasAlpha);
        return hasAlpha;
    }

    private float calculateHorizontalCrescentPhase(Body body, float bodyMapU, float bodyMapV, float parentMapU, float parentMapV) {
        float sunMapU = MAP_TEX_W * 0.5F;
        float sunMapV = MAP_TEX_H * 0.5F;
        float bodyProjX = projectMapToIsoX(bodyMapU, bodyMapV);
        float bodyProjY = projectMapToIsoY(bodyMapU, bodyMapV);
        float sunProjX = projectMapToIsoX(sunMapU, sunMapV);
        float sunProjY = projectMapToIsoY(sunMapU, sunMapV);

        float dx = bodyProjX - sunProjX;
        float dy = bodyProjY - sunProjY;
        float lengthSq = dx * dx + dy * dy;
        if (lengthSq <= 0.0001F) return 0F;

        float length = Mth.sqrt(lengthSq);
        float verticalFactor = dy / length;
        float phaseMagnitude = Mth.clamp((verticalFactor + 1F) * 0.5F, 0F, 1F);

        phaseMagnitude = applyMoonEclipseDarkening(body, bodyMapU, bodyMapV, parentMapU, parentMapV, sunMapU, sunMapV, phaseMagnitude);

        float phaseSign = dx <= 0F ? 1F : -1F;
        return Mth.clamp(phaseMagnitude * phaseSign, -1F, 1F);
    }

    private float applyMoonEclipseDarkening(Body body, float moonMapU, float moonMapV, float parentMapU, float parentMapV,
                                            float sunMapU, float sunMapV, float phaseMagnitude) {
        if (!isMoon(body) || body.parent == null) return phaseMagnitude;
        if (Float.isNaN(parentMapU) || Float.isNaN(parentMapV)) return phaseMagnitude;

        float moonProjX = projectMapToIsoX(moonMapU, moonMapV);
        float moonProjY = projectMapToIsoY(moonMapU, moonMapV);
        float parentProjX = projectMapToIsoX(parentMapU, parentMapV);
        float parentProjY = projectMapToIsoY(parentMapU, parentMapV);
        float sunProjX = projectMapToIsoX(sunMapU, sunMapV);
        float sunProjY = projectMapToIsoY(sunMapU, sunMapV);

        float sunToParentX = parentProjX - sunProjX;
        float sunToParentY = parentProjY - sunProjY;
        float parentToMoonX = moonProjX - parentProjX;
        float parentToMoonY = moonProjY - parentProjY;

        float sunParentLenSq = sunToParentX * sunToParentX + sunToParentY * sunToParentY;
        if (sunParentLenSq <= 0.0001F) return phaseMagnitude;

        float sunParentLen = Mth.sqrt(sunParentLenSq);
        float alongShadowAxis = (sunToParentX * parentToMoonX + sunToParentY * parentToMoonY) / sunParentLen;
        float lineDistance = Math.abs(sunToParentX * parentToMoonY - sunToParentY * parentToMoonX) / sunParentLen;
        float eclipseRadius = Math.max(1.5F, getBodySizePxAt1x(body.parent) * 0.45F);
        float penumbraRadius = eclipseRadius * 2F;
        float behindFade = Math.max(0.15F, eclipseRadius * 0.12F);

        if (lineDistance >= penumbraRadius || alongShadowAxis <= -behindFade) return phaseMagnitude;

        float behindFactor = alongShadowAxis >= 0F ? 1F : smoothstep(alongShadowAxis, -behindFade, 0F);
        if (lineDistance <= eclipseRadius) {
            return Mth.clamp(phaseMagnitude + (1F - phaseMagnitude) * behindFactor, 0F, 1F);
        }

        float penumbraFactor = 1F - smoothstep(lineDistance, eclipseRadius, penumbraRadius);
        float eclipseFactor = behindFactor * penumbraFactor;
        return Mth.clamp(phaseMagnitude + (1F - phaseMagnitude) * eclipseFactor, 0F, 1F);
    }

    /** 1.7 {@code Library.smoothstep(x, edge0, edge1)}. */
    static float smoothstep(float x, float edge0, float edge1) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    // ------------------------------------------------------------------------------------------------ primitives

    /** A textured quad with any float position, size and UV rectangle (1.7 {@code drawPartialTex}). */
    private static void quad(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float w, float h,
                             float u0, float v0, float u1, float v1) {
        if (w <= 0F || h <= 0F) return;
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, x, y);
        GuiDraw.scale(graphics, w / Q, h / Q);
        GuiDraw.blitUv(graphics, texture, 0, 0, Q, Q, u0, v0, u1, v1, UV, UV);
        GuiDraw.popMatrix(graphics);
    }

    /** 1.7 {@code drawTexturedQuad}: a square of {@code size} centred on x, y, u from {@code uOffset} (the texture repeats). */
    private static void drawTexturedQuad(GuiCanvas canvas, Sky.Texture texture, float x, float y, float size, float uOffset) {
        canvas.texturedQuads(texture, Sky.Blend.ALPHA, 1F, 1F, 1F, 1F, StardarMath.texturedQuad(x, y, size, uOffset));
    }

    /** 1.7 {@code drawTexturedQuadRotating}: {@code glTranslatef(x, y)}, {@code glRotatef(angle, 0, 0, 1)}, the square. */
    private static void drawTexturedQuadRotating(GuiCanvas canvas, Sky.Texture texture, float x, float y, float size, float angle) {
        canvas.pose().pushPose();
        canvas.pose().translate(x, y, 0F);
        canvas.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        drawTexturedQuad(canvas, texture, 0F, 0F, size, 0F);
        canvas.pose().popPose();
    }

    /** A line segment as a thin quad, {@code width} across (1.7 {@code GL_LINES} at {@code glLineWidth(2)}). */
    private static void line(GuiCanvas canvas, float x0, float y0, float x1, float y1, float width, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 0F) return;
        float nx = -dy / length * width * 0.5F, ny = dx / length * width * 0.5F;
        canvas.quads(Sky.Blend.ALPHA, ((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F,
                ((color >>> 24) & 0xFF) / 255F,
                x0 - nx, y0 - ny, 0, x1 - nx, y1 - ny, 0, x1 + nx, y1 + ny, 0, x0 + nx, y0 + ny, 0);
    }

    /** {@code pixels} framebuffer pixels in GUI units (1.7 {@code glLineWidth} counts framebuffer pixels). */
    private static float lineWidth(float pixels) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        return scale > 0 ? (float) (pixels / scale) : pixels;
    }

    private static int argb(float r, float g, float b, float a) {
        return (Mth.clamp((int) (a * 255F), 0, 255) << 24) | (Mth.clamp((int) (r * 255F), 0, 255) << 16)
                | (Mth.clamp((int) (g * 255F), 0, 255) << 8) | Mth.clamp((int) (b * 255F), 0, 255);
    }

    // ------------------------------------------------------------------------------------------------ projection

    private float projectMapToIsoX(float mapU, float mapV) {
        return (mapU - mapV) * ISO_X;
    }

    private float projectMapToIsoY(float mapU, float mapV) {
        return (mapU + mapV) * ISO_Y;
    }

    private float mapToScreenX(float mapCoordU, float mapCoordV) {
        float centerU = mapU + (MAP_W / mapZoom) * 0.5F;
        float centerV = mapV + (MAP_H / mapZoom) * 0.5F;
        float relU = mapCoordU - centerU;
        float relV = mapCoordV - centerV;
        return leftPos + MAP_X + MAP_W * 0.5F + (relU - relV) * mapZoom * ISO_X;
    }

    private float mapToScreenY(float mapCoordU, float mapCoordV) {
        float centerU = mapU + (MAP_W / mapZoom) * 0.5F;
        float centerV = mapV + (MAP_H / mapZoom) * 0.5F;
        float relU = mapCoordU - centerU;
        float relV = mapCoordV - centerV;
        return topPos + MAP_Y + MAP_H * 0.5F + (relU + relV) * mapZoom * ISO_Y;
    }

    private @Nullable BodyRenderInfo findBodyUnderCursor(int mouseX, int mouseY) {
        BodyRenderInfo nearest = null;
        float nearestDistSq = Float.MAX_VALUE;

        for (int i = renderedBodies.size() - 1; i >= 0; i--) {
            BodyRenderInfo bodyInfo = renderedBodies.get(i);
            float bodyX = mapToScreenX(bodyInfo.mapU, bodyInfo.mapV);
            float bodyY = mapToScreenY(bodyInfo.mapU, bodyInfo.mapV);
            float clickableRadius = Math.max(5F, bodyInfo.drawSize * 0.5F);

            float dx = mouseX - bodyX;
            float dy = mouseY - bodyY;
            float distanceSq = dx * dx + dy * dy;

            if (distanceSq <= clickableRadius * clickableRadius && distanceSq < nearestDistSq) {
                nearestDistSq = distanceSq;
                nearest = bodyInfo;
            }
        }

        return nearest;
    }

    // ------------------------------------------------------------------------------------------------ input

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (landingMode || scrollY == 0) return false;

        int mx = (int) mouseX, my = (int) mouseY;
        if (!isMouseInsideMap(mx, my) || focusAnimationActive) return false;
        if (isMoon(focusedBody)) return false;

        float oldZoom = mapZoom;
        float minZoom = MIN_ZOOM;
        float maxZoom = MAX_ZOOM;

        if (focusedBody != null) {
            float[] bounds = new float[2];
            if (calculateFocusedChildZoomBounds(focusedBody, bounds)) {
                minZoom = bounds[0];
                maxZoom = Math.max(bounds[1], getBodyDetailZoomCap(focusedBody));
            }
        }

        float newZoom = oldZoom;
        if (scrollY > 0) newZoom = Math.min(maxZoom, oldZoom * ZOOM_STEP);
        if (scrollY < 0) newZoom = Math.max(minZoom, oldZoom / ZOOM_STEP);

        if (newZoom != oldZoom) {
            if (isFocusContextActive()) {
                mapZoom = newZoom;
            } else {
                zoomAtMouse(mx, my, oldZoom, newZoom);
            }
        }
        return true;
    }

    /** 1.7 {@code handleLandingMouseInput}'s clamp (run on every 1.7 mouse event; every frame here). */
    private void clampSurface() {
        if (surfaceX > 256) {
            velocityX = 0;
            surfaceX = 256;
        }
        if (surfaceX < -256 + MAP_W) {
            velocityX = 0;
            surfaceX = -256 + MAP_W;
        }
        if (surfaceY > 256) {
            velocityY = 0;
            surfaceY = 256;
        }
        if (surfaceY < -256 + MAP_H) {
            velocityY = 0;
            surfaceY = -256 + MAP_H;
        }
    }

    /** 1.7 {@code mouseClickMove} for the landing view, driven by the frame's mouse position. */
    private void updateSurfaceDrag(int mouseX, int mouseY) {
        if (!draggingSurface || !leftDown) return;
        if (mouseX == mX && mouseY == mY) return;

        int deltaX = mouseX - mX;
        int deltaY = mouseY - mY;
        surfaceX += deltaX;
        surfaceY += deltaY;

        lX = mX;
        lY = mY;
        mX = mouseX;
        mY = mouseY;
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int mouseX = (int) event.x(), mouseY = (int) event.y();
        int button = event.button();
        if (button == 0) leftDown = true;

        if (checkClick(mouseX, mouseY, 129, 143)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putString("pid", DriveBody.ORBIT.key());
            send(data);
            return true;
        }

        if (checkClick(mouseX, mouseY, 129, 123)) {
            GuiUtil.playClickSound();
            Body body = currentBody;
            if (body != null && body.getStar() != null) {
                if (landingMode) exitLandingMode();
                configureFocusAnimation(body, body, true, 420, 0F, 0F, getFocusZoomForBody(body));
            }
            return true;
        }

        if (checkClick(mouseX, mouseY, 149, 143)) {
            GuiUtil.playClickSound();
            String here = currentLevelTarget();
            if (here != null && star != null) {
                CompoundTag data = new CompoundTag();
                data.putString("pid", here);
                data.putInt("ix", star.getBlockPos().getX());
                data.putInt("iz", star.getBlockPos().getZ());
                send(data);
            }
            return true;
        }

        if (!isMouseInsideMap(mouseX, mouseY)) {
            draggingMap = false;
            draggingSurface = false;
            return false;
        }

        if (landingMode) {
            if (button == 1) {
                exitLandingMode();
                return true;
            }
            if (button == 0) {
                draggingSurface = true;
                sX = mX = lX = mouseX;
                sY = mY = lY = mouseY;
            }
            return true;
        }

        if (button == 1) {
            beginUnfocusAnimation();
            draggingMap = false;
            return true;
        }

        if (button != 0) return true;

        BodyRenderInfo clickedBody = findBodyUnderCursor(mouseX, mouseY);
        if (clickedBody != null) {
            Body activeFocusBody = getRenderFocusBody();
            if (activeFocusBody != null && clickedBody.body == activeFocusBody) {
                handleFocusedBodySecondClick(clickedBody.body);
                draggingMap = false;
                return true;
            }

            if (clickedBody.body.parent != null) {
                startFocusAnimation(clickedBody);
                draggingMap = false;
                return true;
            }
        }

        if (isFocusContextActive()) {
            draggingMap = false;
            return true;
        }

        draggingMap = true;
        dragLastX = mouseX;
        dragLastY = mouseY;
        return true;
    }

    private boolean checkClick(int mouseX, int mouseY, int x, int y) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + 18 && mouseY >= topPos + y && mouseY < topPos + y + 18;
    }

    private void handleFocusedBodySecondClick(Body body) {
        if (body == null || !canLand(body)) return;

        ItemStack slotStack = menu.getSlot(0).getItem();
        if (slotStack.is(RocketItems.HARD_DRIVE.get())) enterLandingMode(body);
    }

    @Override
    protected boolean mouseReleased(MouseInput event) {
        int mouseX = (int) event.x(), mouseY = (int) event.y();
        int which = event.button();
        if (which == 0) {
            leftDown = false;
            draggingMap = false;
            // 1.7 cleared this before handleLandingMouseInput looked at it, so its fling ((mX - lX) * 0.8) never ran
            draggingSurface = false;
        }

        if (!landingMode || which == 1) return false;

        if (Math.abs(sX - mouseX) > 2 || Math.abs(sY - mouseY) > 2) return false;
        if (!isMouseInsideMap(mouseX, mouseY)) return false;
        if (star == null || star.heightmap == null) return false;

        int hx = (mouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
        int hz = (mouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
        if (landingInfo(hx, hz) != null) return false;

        GuiUtil.playClickSound();
        CompoundTag data = new CompoundTag();
        data.putInt("px", hx);
        data.putInt("pz", hz);
        send(data);
        return true;
    }

    private void send(CompoundTag data) {
        if (star == null) return;
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(star.getBlockPos(), data));
    }

    /**
     * 1.7 sent {@code provider.dimensionId} for "program current body": the orbit's id on a station, a body's id on a
     * body, and anything else (the Nether...) was refused by the server. Here: {@code orbit}, the drive key, or null.
     */
    private static @Nullable String currentLevelTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (Orbit.inOrbit(mc.level)) return DriveBody.ORBIT.key();
        for (DriveBody body : DriveBody.values()) {
            if (body != DriveBody.ORBIT && body.level().equals(mc.level.dimension())) return body.key();
        }
        return null;
    }

    private void updateDragPanning(int mouseX, int mouseY) {
        if (!draggingMap) return;

        if (isFocusContextActive() || !leftDown) {
            draggingMap = false;
            return;
        }

        int dx = mouseX - dragLastX;
        int dy = mouseY - dragLastY;
        if (dx != 0 || dy != 0) {
            panByScreenDelta(dx, dy);
            clampMapUV();
        }

        dragLastX = mouseX;
        dragLastY = mouseY;
    }

    private void panByScreenDelta(int dx, int dy) {
        float zoom = Math.max(0.0001F, mapZoom);
        float normalizedZoom = (Mth.clamp(mapZoom, MIN_ZOOM, 5.0F) - MIN_ZOOM) / (5.0F - MIN_ZOOM);
        float speed = (0.35F + (1.20F - 0.35F) * normalizedZoom) * 0.75F;

        float a = -dx / (zoom * ISO_X);
        float b = -dy / (zoom * ISO_Y);
        float deltaU = 0.5F * (a + b);
        float deltaV = 0.5F * (b - a);

        mapU += deltaU * speed;
        mapV += deltaV * speed;
    }

    // ------------------------------------------------------------------------------------------------ focus

    private void startFocusAnimation(BodyRenderInfo bodyInfo) {
        if (bodyInfo == null || bodyInfo.body == null || bodyInfo.body.parent == null) return;
        if (bodyInfo.body == focusedBody) return;
        if (focusAnimationActive && focusAnimationToBody && bodyInfo.body == focusAnimationBody) return;

        GuiUtil.playClickSound();
        configureFocusAnimation(bodyInfo.body, bodyInfo.body, true, 500, bodyInfo.mapU, bodyInfo.mapV, getFocusZoomForBody(bodyInfo.body));
    }

    private void beginUnfocusAnimation() {
        if (!hasBodyFocusContext()) return;

        Body activeFocus = focusedBody != null ? focusedBody : focusAnimationBody;
        Body parentFocus = activeFocus != null ? activeFocus.parent : null;
        if (parentFocus != null && parentFocus.parent != null) {
            configureFocusAnimation(parentFocus, parentFocus, true, 420, 0F, 0F, getFocusZoomForBody(parentFocus));
            return;
        }

        configureFocusAnimation(null, null, false, 420, MAP_TEX_W * 0.5F, MAP_TEX_H * 0.5F, 1.0F);
    }

    private void configureFocusAnimation(@Nullable Body newFocusedBody, @Nullable Body animationBody, boolean animationToBody,
                                         int durationMs, float targetCenterU, float targetCenterV, float targetZoom) {
        focusedBody = newFocusedBody;
        focusAnimationBody = animationBody;
        focusAnimationToBody = animationToBody;
        focusAnimationActive = true;
        focusAnimDurationMs = durationMs;
        focusAnimStartTimeMs = System.currentTimeMillis();
        focusAnimStartCenterU = mapU + (MAP_W / mapZoom) * 0.5F;
        focusAnimStartCenterV = mapV + (MAP_H / mapZoom) * 0.5F;
        focusAnimStartZoom = mapZoom;
        focusAnimTargetCenterU = targetCenterU;
        focusAnimTargetCenterV = targetCenterV;
        focusAnimTargetZoom = targetZoom;
        focusAnimTrackingInitialized = false;
        focusAnimTrackingOffsetU = 0F;
        focusAnimTrackingOffsetV = 0F;
    }

    private void updateFocusView(Body starBody, float starCenterU, float starCenterV, float systemOrbitScalePxPerKm, double worldTicks) {
        if (!hasBodyFocusContext()) return;

        Body trackingBody = focusedBody != null ? focusedBody : focusAnimationBody;
        boolean hasTrackingBodyPosition = false;
        if (trackingBody != null) {
            hasTrackingBodyPosition = findBodyMapPosition(starBody, starCenterU, starCenterV, systemOrbitScalePxPerKm, worldTicks,
                    trackingBody, trackedBodyPosition);
        }

        if (focusAnimationActive) {
            long now = System.currentTimeMillis();
            double duration = Math.max(1D, focusAnimDurationMs);
            float t = Mth.clamp((float) ((now - focusAnimStartTimeMs) / duration), 0F, 1F);
            float eased = t * t * (3F - 2F * t);

            float centerU;
            float centerV;
            if (focusAnimationToBody && hasTrackingBodyPosition) {
                if (!focusAnimTrackingInitialized) {
                    focusAnimTrackingOffsetU = focusAnimStartCenterU - trackedBodyPosition.mapU;
                    focusAnimTrackingOffsetV = focusAnimStartCenterV - trackedBodyPosition.mapV;
                    focusAnimTrackingInitialized = true;
                }
                float remaining = 1F - eased;
                centerU = trackedBodyPosition.mapU + focusAnimTrackingOffsetU * remaining;
                centerV = trackedBodyPosition.mapV + focusAnimTrackingOffsetV * remaining;
            } else {
                centerU = focusAnimStartCenterU + (focusAnimTargetCenterU - focusAnimStartCenterU) * eased;
                centerV = focusAnimStartCenterV + (focusAnimTargetCenterV - focusAnimStartCenterV) * eased;
            }

            float zoom = focusAnimStartZoom + (focusAnimTargetZoom - focusAnimStartZoom) * eased;
            setViewFromCenter(centerU, centerV, zoom);

            if (t >= 1F) {
                focusAnimationActive = false;
                focusAnimationBody = null;
                focusAnimTrackingInitialized = false;
            }
            return;
        }

        if (focusedBody != null && hasTrackingBodyPosition) {
            setViewFromCenter(trackedBodyPosition.mapU, trackedBodyPosition.mapV, mapZoom);
            return;
        }

        if (focusedBody != null) cancelFocusState();
    }

    private boolean findBodyMapPosition(Body body, float bodyMapU, float bodyMapV, float systemOrbitScalePxPerKm, double worldTicks,
                                        Body targetBody, BodyPosition outPosition) {
        if (body == targetBody) {
            outPosition.mapU = bodyMapU;
            outPosition.mapV = bodyMapV;
            return true;
        }

        float childOrbitScale = getChildOrbitScalePxPerKm(body, systemOrbitScalePxPerKm);
        for (Body childBody : body.satellites) {
            double meanAnomaly = calculateMeanAnomaly(childBody, worldTicks);
            float[] orbitOffset = calculateOrbitOffsetPx(body, childBody, meanAnomaly, childOrbitScale);
            if (findBodyMapPosition(childBody, bodyMapU + orbitOffset[0], bodyMapV + orbitOffset[1], systemOrbitScalePxPerKm,
                    worldTicks, targetBody, outPosition)) {
                return true;
            }
        }

        return false;
    }

    private void zoomAtMouse(int mouseX, int mouseY, float oldZoom, float newZoom) {
        float minZoom = MIN_ZOOM;
        if (oldZoom <= 0F || newZoom <= 0F) {
            mapZoom = Mth.clamp(newZoom, minZoom, MAX_ZOOM);
            clampMapUV();
            return;
        }

        float mapCenterScreenX = leftPos + MAP_X + MAP_W * 0.5F;
        float mapCenterScreenY = topPos + MAP_Y + MAP_H * 0.5F;

        float oldCenterU = mapU + (MAP_W / oldZoom) * 0.5F;
        float oldCenterV = mapV + (MAP_H / oldZoom) * 0.5F;

        float oldA = (mouseX - mapCenterScreenX) / (oldZoom * ISO_X);
        float oldB = (mouseY - mapCenterScreenY) / (oldZoom * ISO_Y);
        float targetMapU = oldCenterU + 0.5F * (oldA + oldB);
        float targetMapV = oldCenterV + 0.5F * (oldB - oldA);

        float newA = (mouseX - mapCenterScreenX) / (newZoom * ISO_X);
        float newB = (mouseY - mapCenterScreenY) / (newZoom * ISO_Y);
        float newCenterU = targetMapU - 0.5F * (newA + newB);
        float newCenterV = targetMapV - 0.5F * (newB - newA);

        mapZoom = Mth.clamp(newZoom, minZoom, MAX_ZOOM);
        float viewW = MAP_W / mapZoom;
        float viewH = MAP_H / mapZoom;
        mapU = newCenterU - viewW * 0.5F;
        mapV = newCenterV - viewH * 0.5F;
        clampMapUV();
    }

    private @Nullable Body getRenderFocusBody() {
        if (focusedBody != null) return focusedBody;
        if (focusAnimationActive && focusAnimationToBody) return focusAnimationBody;
        return null;
    }

    private @Nullable Body getLocalOrbitParent() {
        Body activeFocus = getRenderFocusBody();
        if (activeFocus == null) return null;
        if (isTopLevelPlanet(activeFocus)) return activeFocus;
        return activeFocus.parent;
    }

    private @Nullable Body getTopLevelPlanetForBody(@Nullable Body body) {
        if (body == null) return null;

        Body current = body;
        int steps = 0;
        while (current.parent != null && current.parent.parent != null && steps <= 64) {
            current = current.parent;
            steps++;
        }
        return isTopLevelPlanet(current) ? current : null;
    }

    private static boolean isTopLevelPlanet(@Nullable Body body) {
        return body != null && body.parent != null && body.parent.parent == null;
    }

    private static boolean isMoon(@Nullable Body body) {
        return body != null && body.parent != null && body.parent.parent != null;
    }

    /** 1.7 {@code canLand}: the body has a dimension and a solid surface (the drive bodies). */
    private static boolean canLand(Body body) {
        return DriveBody.of(body) != null;
    }

    private void cancelFocusState() {
        focusedBody = null;
        focusAnimationBody = null;
        focusAnimationActive = false;
        focusAnimationToBody = false;
        focusAnimTrackingInitialized = false;
    }

    private boolean isMouseInsideMap(int mouseX, int mouseY) {
        return mouseX >= leftPos + MAP_X && mouseX < leftPos + MAP_X + MAP_W && mouseY >= topPos + MAP_Y && mouseY < topPos + MAP_Y + MAP_H;
    }

    private boolean hasBodyFocusContext() {
        return focusedBody != null || focusAnimationActive;
    }

    private boolean isFocusContextActive() {
        return hasBodyFocusContext();
    }

    private void setViewFromCenter(float centerU, float centerV, float zoom) {
        mapZoom = Mth.clamp(zoom, MIN_ZOOM, MAX_ZOOM);
        float viewW = MAP_W / mapZoom;
        float viewH = MAP_H / mapZoom;
        mapU = centerU - viewW * 0.5F;
        mapV = centerV - viewH * 0.5F;
        clampMapUV();
    }

    private float getFocusZoomForBody(@Nullable Body body) {
        // If the focused body has satellites, start zoom so all of them are visible.
        if (body != null) {
            float[] focusZoomBounds = new float[2];
            if (calculateFocusedChildZoomBounds(body, focusZoomBounds)) return focusZoomBounds[0];
        }

        float bodySizeAt1x = isMoon(body) ? getMoonSizePxAt1x(body) : getBodySizePxAt1x(body);
        if (bodySizeAt1x <= 0F) return Mth.clamp(mapZoom, MIN_ZOOM, MAX_ZOOM);

        float targetBodySizePx = Math.min(MAP_W, MAP_H) * 0.88F;
        float targetZoom = targetBodySizePx / bodySizeAt1x;
        targetZoom /= 3.0F;
        targetZoom = Math.max(targetZoom, 4.0F);
        return Mth.clamp(targetZoom, MIN_ZOOM, MAX_ZOOM);
    }

    private boolean calculateFocusedChildZoomBounds(@Nullable Body focusBody, float[] outBounds) {
        if (outBounds == null || outBounds.length < 2 || focusBody == null || focusBody.satellites.isEmpty()) return false;

        Body starBody = focusBody.getStar();
        if (starBody == null) return false;

        float systemOrbitScale = getOrbitScalePxPerKm(starBody);
        if (systemOrbitScale <= 0F) return false;

        float childOrbitScale = getChildOrbitScalePxPerKm(focusBody, systemOrbitScale);
        if (childOrbitScale <= 0F) return false;

        float nearestProjectedDist = Float.MAX_VALUE;
        float furthestProjectedDist = 0F;
        for (Body childBody : focusBody.satellites) {
            if (childBody.semiMajorAxisKm <= 0F) continue;
            for (int i = 0; i <= 96; i++) {
                double meanAnomaly = 2D * Math.PI * ((double) i / 96D);
                float[] orbitOffset = calculateOrbitOffsetPx(focusBody, childBody, meanAnomaly, childOrbitScale);
                float projected = getProjectedOffsetDistanceAt1x(orbitOffset[0], orbitOffset[1]);
                if (projected <= 0F || Float.isNaN(projected) || Float.isInfinite(projected)) continue;
                nearestProjectedDist = Math.min(nearestProjectedDist, projected);
                furthestProjectedDist = Math.max(furthestProjectedDist, projected);
            }
        }

        if (nearestProjectedDist == Float.MAX_VALUE || furthestProjectedDist <= 0F) return false;

        float availableRadiusPx = Math.max(1F, Math.min(MAP_W, MAP_H) * 0.5F - 10F);
        float minZoom = availableRadiusPx / furthestProjectedDist;
        float maxZoom = availableRadiusPx / nearestProjectedDist;

        minZoom = Mth.clamp(minZoom, MIN_ZOOM, MAX_ZOOM);
        maxZoom = Mth.clamp(maxZoom, MIN_ZOOM, MAX_ZOOM);
        if (maxZoom < minZoom) {
            float swap = minZoom;
            minZoom = maxZoom;
            maxZoom = swap;
        }

        outBounds[0] = minZoom;
        outBounds[1] = maxZoom;
        return true;
    }

    private static float getProjectedOffsetDistanceAt1x(float offsetU, float offsetV) {
        float screenOffsetX = (offsetU - offsetV) * ISO_X;
        float screenOffsetY = (offsetU + offsetV) * ISO_Y;
        return Mth.sqrt(screenOffsetX * screenOffsetX + screenOffsetY * screenOffsetY);
    }

    private float getBodyDetailZoomCap(@Nullable Body body) {
        if (body == null) return MIN_ZOOM;

        float bodySizeAt1x = isMoon(body) ? getMoonSizePxAt1x(body) : getBodySizePxAt1x(body);
        if (bodySizeAt1x <= 0F || Float.isNaN(bodySizeAt1x) || Float.isInfinite(bodySizeAt1x)) return MIN_ZOOM;

        float targetBodySizePx = Math.min(MAP_W, MAP_H) * 0.88F;
        float detailZoom = targetBodySizePx / bodySizeAt1x;
        return Mth.clamp(detailZoom, MIN_ZOOM, MAX_ZOOM);
    }

    private void clampMapUV() {
        float viewW = MAP_W / mapZoom;
        float viewH = MAP_H / mapZoom;
        float maxU = Math.max(0F, MAP_TEX_W - viewW);
        float maxV = Math.max(0F, MAP_TEX_H - viewH);
        mapU = Mth.clamp(mapU, 0F, maxU);
        mapV = Mth.clamp(mapV, 0F, maxV);
    }

    // ------------------------------------------------------------------------------------------------ orbit math (1.7)

    static float getOrbitScalePxPerKm(Body starBody) {
        float maxDistanceKm = getSystemMaxDistanceKm(starBody);
        if (maxDistanceKm <= 0F) return 0F;

        float starRadiusPxAt1x = getBodySizePxAt1x(starBody) * 0.5F;
        float availableRadiusPx = Math.max(0F, (SYSTEM_LAYOUT_REF_TEX * STARDAR_SYSTEM_RADIUS_FRACTION) - starRadiusPxAt1x);
        if (availableRadiusPx <= 0F) return 0F;

        return availableRadiusPx / maxDistanceKm;
    }

    static float getSystemMaxDistanceKm(Body starBody) {
        if (starBody == null) return 0F;

        float maxDistance = 0F;
        for (Body child : starBody.satellites) {
            float apoapsisKm = child.semiMajorAxisKm * (1F + child.eccentricity);
            maxDistance = Math.max(maxDistance, Math.max(0F, apoapsisKm));
        }

        return maxDistance;
    }

    static float getChildOrbitScalePxPerKm(Body parent, float systemOrbitScalePxPerKm) {
        if (parent != null && parent.parent != null) {
            float moonOrbitScale = getMoonOrbitScalePxPerKm(parent, systemOrbitScalePxPerKm);
            if (moonOrbitScale > 0F) return moonOrbitScale;
        }
        return systemOrbitScalePxPerKm;
    }

    static float getMoonOrbitScalePxPerKm(Body parent, float systemOrbitScalePxPerKm) {
        if (parent == null || parent.satellites.isEmpty()) return 0F;

        float maxMoonDistanceKm = 0F;
        for (Body moon : parent.satellites) {
            float apoapsisKm = moon.semiMajorAxisKm * (1F + moon.eccentricity);
            maxMoonDistanceKm = Math.max(maxMoonDistanceKm, Math.max(0F, apoapsisKm));
        }

        if (maxMoonDistanceKm <= 0F) return 0F;

        float parentRadiusPxAt1x = getBodySizePxAt1x(parent) * 0.5F;
        float moonOrbitRadiusPxAt1x = Math.max(10.0F, parentRadiusPxAt1x * 14F);

        float nearestSiblingGapKm = getNearestSiblingOrbitGapKm(parent);
        if (systemOrbitScalePxPerKm > 0F && nearestSiblingGapKm > 0F) {
            float siblingGapPxAt1x = nearestSiblingGapKm * systemOrbitScalePxPerKm;
            float maxAllowedMoonOrbitRadiusPxAt1x = siblingGapPxAt1x * 0.45F;
            if (maxAllowedMoonOrbitRadiusPxAt1x > 0F) {
                moonOrbitRadiusPxAt1x = Math.min(moonOrbitRadiusPxAt1x, maxAllowedMoonOrbitRadiusPxAt1x);
            }
        }

        return moonOrbitRadiusPxAt1x / maxMoonDistanceKm;
    }

    static float getNearestSiblingOrbitGapKm(Body body) {
        if (body == null || body.parent == null || body.semiMajorAxisKm <= 0F) return 0F;

        float nearestGapKm = Float.MAX_VALUE;
        for (Body sibling : body.parent.satellites) {
            if (sibling == body || sibling.semiMajorAxisKm <= 0F) continue;
            float gapKm = Math.abs(sibling.semiMajorAxisKm - body.semiMajorAxisKm);
            if (gapKm > 0F) nearestGapKm = Math.min(nearestGapKm, gapKm);
        }

        return nearestGapKm == Float.MAX_VALUE ? 0F : nearestGapKm;
    }

    static float[] calculateOrbitOffsetPx(Body parent, Body body, double meanAnomaly, float orbitScalePxPerKm) {
        double eccentricAnomaly = calculateEccentricAnomaly(meanAnomaly, body.eccentricity);
        double semiMinorAxisFactor = body.semiMinorAxisFactor > 0 ? body.semiMinorAxisFactor : Math.sqrt(1D - (body.eccentricity * body.eccentricity));

        double x = body.semiMajorAxisKm * (Math.cos(eccentricAnomaly) - body.eccentricity);
        double y = body.semiMajorAxisKm * semiMinorAxisFactor * Math.sin(eccentricAnomaly);
        double z;

        double px = x;
        x = Math.cos(body.argumentPeriapsis) * px - Math.sin(body.argumentPeriapsis) * y;
        y = Math.sin(body.argumentPeriapsis) * px + Math.cos(body.argumentPeriapsis) * y;

        z = Math.sin(body.inclination) * y;
        y = Math.cos(body.inclination) * y;

        px = x;
        x = Math.cos(body.ascendingNode) * px - Math.sin(body.ascendingNode) * y;
        y = Math.sin(body.ascendingNode) * px + Math.cos(body.ascendingNode) * y;

        y -= z * 0.35D;
        y *= 0.8F;

        float mapX = (float) (x * orbitScalePxPerKm);
        float mapY = (float) (y * orbitScalePxPerKm);

        if (parent != null) {
            float parentVisualRadiusPx = getBodySizePxAt1x(parent) * 0.5F;
            float distanceFromParentPx = Mth.sqrt(mapX * mapX + mapY * mapY);
            if (distanceFromParentPx > 0F) {
                float radialScale = (distanceFromParentPx + parentVisualRadiusPx) / distanceFromParentPx;
                mapX *= radialScale;
                mapY *= radialScale;
            } else {
                mapX = parentVisualRadiusPx;
                mapY = 0F;
            }
        }

        return new float[] {mapX, mapY};
    }

    static double calculateMeanAnomaly(Body body, double worldTicks) {
        if (body == null) return 0D;

        double orbitPeriodTicks = getOrbitalPeriodTicks(body);
        if (orbitPeriodTicks <= 0D || Double.isNaN(orbitPeriodTicks) || Double.isInfinite(orbitPeriodTicks)) return 0D;

        return 2D * Math.PI * (worldTicks / orbitPeriodTicks);
    }

    static double getOrbitalPeriodTicks(Body body) {
        if (body == null || body.parent == null || body.semiMajorAxisKm <= 0F) return 0D;

        double orbitPeriodDays = body.getOrbitalPeriod();
        if (orbitPeriodDays <= 0D || Double.isNaN(orbitPeriodDays) || Double.isInfinite(orbitPeriodDays)) return 0D;

        return orbitPeriodDays * (double) SolarSystem.TICKS_IN_DAY;
    }

    static double calculateEccentricAnomaly(double meanAnomaly, float eccentricity) {
        double eccentricAnomaly = meanAnomaly;
        for (int i = 0; i < 4; i++) {
            eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(eccentricAnomaly);
        }
        return eccentricAnomaly;
    }

    static float getBodyRotationPhase(Body body, double dayTicks) {
        if (body == null) return 0F;

        double period = body.getClientRotationalPeriod();
        if (period <= 0D || Double.isNaN(period) || Double.isInfinite(period)) return 0F;

        return (float) ((dayTicks % period) / period);
    }

    private float getBodySizePxAtZoom(Body body) {
        float bodySize = getBodySizePxAt1x(body) * mapZoom;
        Body renderFocusBody = getRenderFocusBody();
        if (renderFocusBody != null) {
            Body localOrbitParent = getLocalOrbitParent();
            boolean isLocalMoon = localOrbitParent != null && body.parent == localOrbitParent;
            if (isLocalMoon) {
                float moonSizeAt1x = getMoonSizePxAt1x(body);
                bodySize = moonSizeAt1x * mapZoom;
            }
        }
        return bodySize;
    }

    static float getBodySizePxAt1x(@Nullable Body body) {
        if (body == null) return 1F;
        if (body.parent == null) return 36F * STARDAR_BODY_SCALE;
        if (isMoon(body)) return getMoonSizePxAt1x(body);
        float bodySizeAt1x = body.radiusKm * (36F / 261_600F) * 2.6F;
        bodySizeAt1x = Mth.clamp(bodySizeAt1x, 0.8F, 2.0F);
        return bodySizeAt1x * STARDAR_BODY_SCALE;
    }

    static float getMoonSizePxAt1x(@Nullable Body moon) {
        if (moon == null) return 0.2F * STARDAR_BODY_SCALE * STARDAR_MOON_VISUAL_SCALE;
        float moonRadiusKm = Math.max(0F, moon.radiusKm);
        float radiusRangeKm = 500F - 65F;
        float t = (moonRadiusKm - 65F) / radiusRangeKm;
        t = Mth.clamp(t, 0F, 1F);
        return (0.2F + (0.5F - 0.2F) * t) * STARDAR_BODY_SCALE * STARDAR_MOON_VISUAL_SCALE;
    }

    // ------------------------------------------------------------------------------------------------ landing

    private void enterLandingMode(Body body) {
        landingMode = true;
        landingBody = body;
        surfaceX = 0;
        surfaceY = 0;
        velocityX = 0;
        velocityY = 0;

        GuiUtil.playClickSound();

        DriveBody drive = DriveBody.of(body);
        if (drive == null || star == null) return;

        CompoundTag data = new CompoundTag();
        data.putString("pid", drive.key());

        Minecraft mc = Minecraft.getInstance();
        if (body == currentBody && mc.level != null && !Orbit.inOrbit(mc.level)) {
            data.putInt("ix", star.getBlockPos().getX());
            data.putInt("iz", star.getBlockPos().getZ());
        }

        send(data);
    }

    private void exitLandingMode() {
        landingMode = false;
        landingBody = null;
        draggingSurface = false;
    }

    /** 1.7 {@code updateLandingModeFromDrive} (1.7 ran it every screen tick; here every frame, it only reacts to changes). */
    private void updateLandingModeFromDrive() {
        ItemStack slotStack = menu.getSlot(0).getItem();

        if (slotStack.isEmpty()) {
            hadFullDrive = false;
            lastFullDrive = null;
            if (landingMode) exitLandingMode();
            return;
        }

        if (!(slotStack.getItem() instanceof ItemVOTVdrive)) {
            hadFullDrive = false;
            lastFullDrive = null;
            if (landingMode && !slotStack.is(RocketItems.HARD_DRIVE.get())) exitLandingMode();
            return;
        }

        Item drive = slotStack.getItem();
        if (hadFullDrive && drive == lastFullDrive) return;

        hadFullDrive = true;
        lastFullDrive = drive;

        Body body = ItemVOTVdrive.getBody(slotStack).getBody();
        if (landingMode && landingBody == body) return;

        if (body != null && canLand(body)) {
            enterLandingMode(body);
        } else if (landingMode) {
            exitLandingMode();
        }
    }

    /** 1.7 {@code landingInfo}: null where a 5 x 5 area of the height map is flat and inside the map. */
    private @Nullable String landingInfo(int x, int z) {
        if (star == null || star.heightmap == null) return I18n.get("gui.stardar.landing.no_heightmap");
        return landingProblem(star.heightmap, x, z);
    }

    /** The 1.7 landing rule on a 256 x 256 height map; the translated reason, or null. */
    public static @Nullable String landingProblem(int[] heightmap, int x, int z) {
        var issue = TileEntityMachineStardar.landingIssue(heightmap, x, z);
        return issue == null ? null : I18n.get(issue);
    }

    private int altitude(int x, int z) {
        if (star == null || star.heightmap == null) return -1;
        if (x < 0 || x > 255 || z < 0 || z > 255) return -1;
        return star.heightmap[256 * z + x];
    }

    private static String getBodyDisplayName(@Nullable Body body) {
        if (body == null || body.name == null) return I18n.get("gui.stardar.body.unknown");
        String key = "body." + body.name;
        if (I18n.exists(key)) return I18n.get(key);
        if (body.name.length() <= 1) return body.name.toUpperCase(Locale.ROOT);
        return body.name.substring(0, 1).toUpperCase(Locale.ROOT) + body.name.substring(1);
    }

    private static int colorArrayToRgb(float @Nullable [] color) {
        if (color == null || color.length < 3) return 0xFFFFFFFF;
        int r = Mth.clamp((int) (color[0] * 255F), 0, 255);
        int g = Mth.clamp((int) (color[1] * 255F), 0, 255);
        int b = Mth.clamp((int) (color[2] * 255F), 0, 255);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int getBodyHierarchyDepth(@Nullable Body body) {
        if (body == null) return 0;

        int depth = 0;
        Body current = body;
        while (current.parent != null && depth < 64) {
            depth++;
            current = current.parent;
        }
        return depth;
    }

    private static final class BodyPosition {
        float mapU;
        float mapV;
    }

    private record ChildRenderState(Body childBody, float orbitScalePxPerKm, float orbitOffsetU, float orbitOffsetV,
                                    boolean moonChild, boolean drawInFrontOfParent) {}

    private record BodyRenderInfo(Body body, float mapU, float mapV, float drawSize) {}

    private record BodyLabelRenderInfo(Body body, float topRightAnchorScreenX, float topRightAnchorScreenY) {}
}
