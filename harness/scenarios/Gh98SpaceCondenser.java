import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/**
 * HBM GitHub issue 98: with NTM: Space installed the powered condenser and the Leviathan drew no item icon below 26.1
 * (Space points both at a block_state select). Shows intact and damaged stacks of each in the hotbar and open inventory.
 */
public final class Gh98SpaceCondenser implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        if (c.screen().get("open").getAsBoolean()) c.key("escape");
        c.setOption("gui_scale", "3");
        c.cmd("gamemode creative");
        c.cmd("clear @s");
        boolean legacy = c.node().startsWith("1.20.1");
        String damaged = legacy ? "{BlockEntityTag:{damaged:1b}}" : "[block_state={damaged:\"true\"}]";
        c.cmd("item replace entity @s hotbar.0 with hbm:machine_condenser_powered");
        c.cmd("item replace entity @s hotbar.1 with hbm:machine_condenser_powered" + damaged);
        c.cmd("item replace entity @s hotbar.2 with hbm:machine_chungus");
        c.cmd("item replace entity @s hotbar.3 with hbm:machine_chungus" + damaged);
        c.cmd("item replace entity @s hotbar.4 with minecraft:stone");
        c.hotbar(4);
        c.waitTicks(20);
        c.screenshot("98-hotbar");
        c.key("e");
        c.waitTicks(10);
        c.mouseMove(2, 2);
        c.screenshot("98-creative");
        c.key("escape");
    }
}
