import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * GitHub issue 157, point 1: strawberries and mint looked wrong in the world. The crops and the wild flowers are
 * cutout cross models; a client that draws them on the solid layer shows their clear texels, which are black, as a
 * black plate. Every crop, unripe and ripe, and both wild flowers stand in front of a white wall, and the frame must
 * hold almost no black pixels (the plants' darkest texels are not black). The same missing client install left the
 * tinted Space items grey, so a Space spawn egg in the hotbar must show its cyan.
 */
public class Gh157CropTextures implements Scenario {

    private static final int Y = 100, G = 101;
    private static final int MAX_BLACK = 400;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 8) + " 10 minecraft:air");
        c.cmd("fill -6 " + G + " 4 6 " + (G + 4) + " 4 minecraft:white_concrete");
        c.cmd("time set 6000");
        String[] crops = {"strawberry", "mint", "coffee", "tea", "paraffin"};
        for (int i = 0; i < crops.length; i++) {
            String soil = crops[i].equals("paraffin") ? "ntmspace:rubber_farmland" : "minecraft:farmland";
            Fixtures.setBlock(c, -5 + i * 2, Y, 2, soil);
            Fixtures.setBlock(c, -5 + i * 2, G, 2, "ntmspace:crop_" + crops[i] + "[age=7]");
            Fixtures.setBlock(c, -5 + i * 2, Y, 3, soil);
            Fixtures.setBlock(c, -5 + i * 2, G, 3, "ntmspace:crop_" + crops[i] + "[age=0]");
        }
        Fixtures.setBlock(c, 5, Y, 2, "minecraft:grass_block");
        Fixtures.setBlock(c, 5, Y, 3, "minecraft:grass_block");
        Fixtures.setBlock(c, 5, G, 2, "ntmspace:plant_flower_strawberry");
        Fixtures.setBlock(c, 5, G, 3, "ntmspace:plant_flower_mint");
        c.waitTicks(10);

        c.teleport(0.5, G + 1, -3.5);
        c.lookAt(0.5, G + 0.3, 3.0);
        c.waitTicks(30);
        BufferedImage frame = ImageIO.read(c.screenshot("01-crops-and-wild-flowers").toFile());
        c.teleport(-4.5, G, 0.5);
        c.lookAt(-4.5, G + 0.4, 2.5);
        c.waitTicks(15);
        BufferedImage close = ImageIO.read(c.screenshot("02-strawberry-close").toFile());
        c.teleport(-2.5, G, 0.5);
        c.lookAt(-2.5, G + 0.4, 2.5);
        c.waitTicks(15);
        BufferedImage mint = ImageIO.read(c.screenshot("03-mint-close").toFile());

        c.cmd("clear");
        c.cmd("item replace entity @s hotbar.0 with ntmspace:entity_depthsquid_spawn_egg");
        c.cmd("item replace entity @s hotbar.1 with ntmspace:bedrock_ore_base_dres_heavy");
        c.hotbar(3);
        c.lookAt(-2.5, G + 3, 6.0);
        c.waitTicks(10);
        BufferedImage hotbar = ImageIO.read(c.screenshot("04-tinted-space-items").toFile());
        int cyan = cyan(hotbar);

        int black = black(frame), blackClose = black(close), blackMint = black(mint);
        System.out.println("[Gh157] black pixels: overview " + black + ", strawberry " + blackClose + ", mint " + blackMint);
        Expect.that(black <= MAX_BLACK, "overview: " + black + " black pixels (clear texels drawn solid)");
        Expect.that(blackClose <= MAX_BLACK, "strawberry close-up: " + blackClose + " black pixels");
        Expect.that(blackMint <= MAX_BLACK, "mint close-up: " + blackMint + " black pixels");
        System.out.println("[Gh157] cyan pixels of the depth squid egg in the hotbar: " + cyan);
        Expect.that(cyan >= 20, "the NTM: Space spawn egg carries its 1.7 colour: " + cyan + " cyan pixels");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(ntmspace|hbm|golem)");
        System.out.println("[Gh157] PASS");
    }

    /** Pixels of the depth squid egg's cyan (0x00B4DF) in the bottom eighth of the screen, where the hotbar is. */
    private static int cyan(BufferedImage image) {
        int count = 0;
        int w = image.getWidth(), h = image.getHeight();
        for (int y = h * 7 / 8; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (r < 60 && g > 100 && b > 140) count++;
            }
        }
        return count;
    }

    /** Pixels in the central area darker than any plant texel; the hotbar and hand are outside it. */
    private static int black(BufferedImage image) {
        int count = 0;
        int w = image.getWidth(), h = image.getHeight();
        for (int y = h / 8; y < h * 3 / 4; y++) {
            for (int x = w / 8; x < w * 7 / 8; x++) {
                int rgb = image.getRGB(x, y);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (r < 12 && g < 12 && b < 12) count++;
            }
        }
        return count;
    }
}
