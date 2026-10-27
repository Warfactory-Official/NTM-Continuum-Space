package com.ntmspace.block.terrain;

import com.google.gson.JsonParser;
import dev.golem.core.data.DataFiles;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import net.minecraft.world.level.material.MapColor;

/** Registration-time palette samples of a shipped static texture, tinted before linear-light averaging. */
public final class TextureMapColors {
    private TextureMapColors() {}

    public static MapColor[] sample(Class<?> owner, String resource, int... tints) {
        var metadata = DataFiles.bundled(owner, resource + ".mcmeta");
        try {
            if (metadata != null) {
                try (var in = new InputStreamReader(metadata.openStream(), StandardCharsets.UTF_8)) {
                    if (JsonParser.parseReader(in).getAsJsonObject().has("animation")) {
                        throw new IllegalArgumentException("Static map-color sampling cannot read an animated texture: " + resource);
                    }
                }
            }
            try (var in = DataFiles.bundled(owner, resource).openStream()) {
                var image = ImageIO.read(in);
                var result = new MapColor[tints.length];
                for (var index = 0; index < tints.length; index++) {
                    var tint = tints[index];
                    double r = 0, g = 0, b = 0, weight = 0;
                    for (var y = 0; y < image.getHeight(); y++) {
                        for (var x = 0; x < image.getWidth(); x++) {
                            var pixel = image.getRGB(x, y);
                            var alpha = (pixel >>> 24) / 255D;
                            r += linear((pixel >>> 16 & 255) / 255D * (tint >>> 16 & 255) / 255D) * alpha;
                            g += linear((pixel >>> 8 & 255) / 255D * (tint >>> 8 & 255) / 255D) * alpha;
                            b += linear((pixel & 255) / 255D * (tint & 255) / 255D) * alpha;
                            weight += alpha;
                        }
                    }
                    if (weight == 0) throw new IllegalStateException("Map-color source has no nontransparent pixels: " + resource);
                    result[index] = closest(r / weight, g / weight, b / weight);
                }
                return result;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Reading map-color texture " + resource, error);
        }
    }

    private static double linear(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static MapColor closest(double r, double g, double b) {
        var target = oklab(r, g, b);
        var distance = Double.POSITIVE_INFINITY;
        var chosen = MapColor.NONE;
        for (var id = 1; id <= MapColor.GLOW_LICHEN.id; id++) {
            var candidate = MapColor.byId(id);
            // WATER enables map depth shading; an opaque surface cannot borrow that palette entry.
            if (candidate == MapColor.WATER) continue;
            var rgb = candidate.col;
            var sample = oklab(linear((rgb >>> 16 & 255) / 255D), linear((rgb >>> 8 & 255) / 255D), linear((rgb & 255) / 255D));
            double delta = 0;
            for (var channel = 0; channel < 3; channel++) delta += (target[channel] - sample[channel]) * (target[channel] - sample[channel]);
            if (delta < distance) { distance = delta; chosen = candidate; }
        }
        return chosen;
    }

    // Linear sRGB to OKLab: https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
    private static double[] oklab(double r, double g, double b) {
        var l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        var m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        var s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return new double[]{0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s};
    }
}
