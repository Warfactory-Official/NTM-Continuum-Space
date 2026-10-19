import dev.golem.harness.api.*;

/**
 * Bedrock ore fragments of materials without a colour band (gallium: light == dark) draw 1.7's greyscale fragment tinted
 * with the molten colour (ItemAutogen.getColorFromItemStack), next to a banded one (nickel) for comparison; screenshot of
 * the hotbar. The check that matters is the image: a flat single-colour silhouette is the old bug.
 */
public final class FragmentTint implements Scenario {
    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("clear @s");
        c.cmd("give @s ntmspace:bedrock_ore_fragment_gallium 1");
        c.cmd("give @s ntmspace:bedrock_ore_fragment_nickel 1");
        c.hotbar(1);
        c.waitTicks(10);
        c.markLog();
        c.waitTicks(5);
        Expect.that(c.logSinceMark("Missing textures.*fragment").isEmpty(), "no missing fragment textures");
        c.screenshot("fragments-hotbar");
    }
}
