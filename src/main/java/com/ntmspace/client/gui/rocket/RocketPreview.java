package com.ntmspace.client.gui.rocket;

import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.RocketEntities;
import com.ntmspace.rocket.RocketStruct;
import dev.golem.core.gui.GuiEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * 1.7's in-screen {@code MissilePronter.prontRocket}: a throwaway rocket entity drawn through {@link GuiEntities} (the
 * 26.1 way to put a model in a screen), standing on {@code (originX, originY)} inside the scissor box, turning at one
 * degree per 10 ms about the vertical, {@code scale} pixels per block, raised by {@code offset} blocks.
 *
 * <p>1.7's {@code glScaled(-s, -s, -s)} after the spin is GuiEntities' {@code (s, s, -s)} frame with a half turn about
 * Z; the spin about -Y becomes a spin about +Y in that z-flipped frame; the offset becomes part of the translation.
 */
final class RocketPreview {

    private @Nullable EntityRideableRocket entity;
    private @Nullable RocketStruct shown;

    void draw(GuiGraphicsExtractor graphics, RocketStruct rocket, int x0, int y0, int x1, int y1, int originX, int originY,
              double scale, double offset) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (entity == null) {
            entity = new EntityRideableRocket(RocketEntities.RIDEABLE_ROCKET.get(), minecraft.level);
            entity.preview = true;
            if (minecraft.player != null) entity.setPos(minecraft.player.position());
        }
        if (!rocket.sameAs(shown)) {
            shown = rocket.copy();
            entity.setRocket(rocket);
        }

        float s = (float) scale;
        float spin = (float) Math.toRadians(System.currentTimeMillis() / 10 % 360);
        float dx = originX - (x0 + x1) / 2F;
        float dy = originY - (y0 + y1) / 2F;
        Vector3f translation = new Vector3f(dx / s, dy / s + (float) offset, 0F);
        Quaternionf rotation = new Quaternionf().rotateY(spin).rotateZ((float) Math.PI);
        GuiEntities.draw(graphics, entity, x0, y0, x1, y1, s, translation, rotation);
    }
}
