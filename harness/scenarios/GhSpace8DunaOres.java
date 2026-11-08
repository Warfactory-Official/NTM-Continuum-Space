import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * NTM: Space issue 8, point 3: per-body ores are cutout models; drawn solid, the overlay's clear texels hid the stone as
 * blue or black squares. A floor of every ore in its Duna state, seen from above, must show neither.
 */
public class GhSpace8DunaOres implements Scenario {

    private static final int Y = 100;
    /** One block fills about 10,000 pixels here; a few ores have real black or blue texels. */
    private static final int MAX_PIXELS = 3000;
    private static final String[] ORES = {"iron", "gold", "redstone", "quartz", "diamond", "lapis", "emerald", "plutonium",
            "thorium", "titanium", "sulfur", "glowstone", "niter", "copper", "nickel", "mineral", "zinc", "lithium", "niobium",
            "cadmium", "palladium", "arsenic", "silicon", "iodine", "tungsten", "aluminium", "fluorite", "lead", "beryllium",
            "lignite", "shale", "australium", "rare", "cobalt", "cinnebar", "coltan"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + (Y + 1) + " -10 10 " + (Y + 14) + " 10 minecraft:air");
        c.cmd("time set 6000");
        for (int i = 0; i < ORES.length; i++) {
            Fixtures.setBlock(c, -3 + i % 6, Y, -3 + i / 6, "ntmspace:ore_" + ORES[i] + "[body=duna]");
        }
        c.waitTicks(10);
        Expect.block(c, -3, Y, -3, "ntmspace:ore_iron");

        c.teleport(0.0, Y + 11, -0.5);
        c.cmd("clear");
        c.lookAt(0.0, Y, 0.0);
        c.waitTicks(30);
        BufferedImage frame = ImageIO.read(c.screenshot("01-duna-ores").toFile());
        int blue = 0, black = 0;
        for (int y = frame.getHeight() / 8; y < frame.getHeight() * 3 / 4; y++) {
            for (int x = frame.getWidth() / 8; x < frame.getWidth() * 7 / 8; x++) {
                int rgb = frame.getRGB(x, y);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (r < 60 && g > 120 && b > 200) blue++;
                if (r < 12 && g < 12 && b < 12) black++;
            }
        }
        System.out.println("[GhSpace8] bright blue pixels " + blue + ", black pixels " + black);
        Expect.that(blue <= MAX_PIXELS, blue + " bright blue pixels (clear overlay texels drawn solid)");
        Expect.that(black <= MAX_PIXELS, black + " black pixels (clear overlay texels drawn solid)");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(ntmspace|hbm|golem)");
        System.out.println("[GhSpace8] PASS");
    }
}
