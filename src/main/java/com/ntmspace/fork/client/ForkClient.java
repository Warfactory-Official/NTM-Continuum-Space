package com.ntmspace.fork.client;

import com.hbm.client.render.CondenserRenderer;
import com.hbm.client.render.DamagedView;
import com.hbm.client.render.RenderChungus;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.inventory.gui.GUISILEX;
import com.ntmspace.client.DuoDecAura;
import com.ntmspace.client.SatelliteTooltips;
import com.ntmspace.fork.ForkCapes;
import com.ntmspace.fork.ForkContent;
import com.ntmspace.fork.machine.DamagedMachines.Damageable;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.VanillaRenderers;
import dev.golem.core.Ids;
import dev.golem.core.client.Capes;
import net.minecraft.sounds.SoundSource;

/**
 * Client side of hbm-space's non-space fork content: the two capes hbm-space's {@code RenderAccessoryUtility} added,
 * for {@code DUODEC_} (Gwen, {@code CapeGwen}) and {@code MellowRPG8} ({@code CapeMellow}). Registered after HBM's
 * provider, which names neither player, so 1.7's order (every HBM cape first) holds.
 *
 * <p>Not reproduced: hbm-space also dropped HBM's "Player*" test cape; that only dresses development clients and HBM's
 * port keeps it (it also covers the {@code Dev} client name), so it stays.
 */
public final class ForkClient {

    private ForkClient() {
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        GUISILEX.waveCoefficient = 0.05F;
        EntityBomber.loopSoundSource = SoundSource.PLAYERS;
        Capes.register(ForkCapes::texture);
        DuoDecAura.init();
        SatelliteTooltips.init();
        // hbm-space RenderChungus / RenderCondenser: a damaged machine draws its wreck (chungus_destroyed with the
        // turbine's own placement, culled; condenser_damaged turned another 180 degrees, unculled)
        RenderChungus.DAMAGED = new DamagedView(
                be -> be instanceof Damageable d && d.isDamaged(),
                "ntmspace:models/machines/chungus_destroyed.obj",
                "ntmspace:textures/models/machines/chungus_destroyed.png", 0F, true);
        CondenserRenderer.DAMAGED = new DamagedView(
                be -> be instanceof Damageable d && d.isDamaged(),
                "ntmspace:models/machines/condenser_damaged.obj",
                "ntmspace:textures/models/machines/condenser_damaged.png", 180F, false);
        // hbm-space's flashbang / nitan overlays
        ForkHud.init();
        // part 3: wings fold while riding, the refueler's tank overlay, the compressor tube's gas line
        ForkBaseClient.init();
        ElectrolyserSpaceModel.init();
        // hbm-space's airliner and B2X bombers
        ForkBomberSkins.init();
        // hbm-space's bobbleheads: HBM's commemorative panel, HBM's figure
        com.ntmspace.fork.bobble.BlockSpaceBobble.OPEN_SCREEN = type -> net.minecraft.client.Minecraft.getInstance()
                                                                                                      .setScreen(
                                                                                                              new com.hbm.inventory.gui.GUIScreenBobble(
                                                                                                                      type.name,
                                                                                                                      type.contribution,
                                                                                                                      type.inscription));
        com.hbm.client.render.item.CustomItemRenderers.register("ntmspace:bobblehead", RenderSpaceBobble.Item::new);
        dev.golem.core.client.ClientRegistrations.renderers(
                renderers -> com.hbm.client.render.HbmBlockEntityRenderers.regGem(
                        com.ntmspace.client.render.machine.MachineRenderers.sink(renderers),
                        com.ntmspace.fork.ForkContent.BOBBLEHEAD_BE.get(), RenderSpaceBobble::new));
        // The blast is invisible, like the shared balefire logic entity, but still needs a renderer binding.
        ClientRegistrations.renderers(renderers -> renderers.entity(
                ForkContent.ANTIMATTER_BLAST.get(), VanillaRenderers.noop()));
        // hbm-space's antimatter bomb: GUINukeAntimatter + RenderAntimatter
        dev.golem.core.client.ClientRegistrations.screens(screens ->
                screens.register(com.ntmspace.fork.ForkContent.NUKE_ANTIMATTER_MENU.get(), GUINukeAntimatter::new));
        dev.golem.core.client.ClientRegistrations.renderers(
                renderers -> com.hbm.client.render.HbmBlockEntityRenderers.regGem(
                        com.ntmspace.client.render.machine.MachineRenderers.sink(renderers),
                        com.ntmspace.fork.ForkContent.NUKE_ANTIMATTER_BE.get(), RenderAntimatter::new));
    }
}
