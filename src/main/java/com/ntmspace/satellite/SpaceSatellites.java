package com.ntmspace.satellite;

import com.hbm.api.environment.Environment;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatChip;
import com.hbm.registration.Presets;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.hbm.tileentity.machine.TileEntityMachineSatLinker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's satellites across the solar system, on HBM's satellite system:
 *
 * <ul>
 *   <li><b>Which level's satellites</b> ({@code Environment.SATELLITES}; 1.7 {@code SatelliteSavedData.getData(world, x,
 *       z)} and {@code XSatelliteRegistry.getTargetDimensionId}): satellite data is per level, so a satellite launched on
 *       a body orbits that body and is listed there only. On an orbital station it is the body the station orbits. A new
 *       relay ({@code sat_foeq}'s satellite) goes to Duna, a lunar miner to the Mun and an asteroid miner to Dres,
 *       wherever it was launched.</li>
 *   <li><b>Orbit settings</b> (1.7 {@code SatelliteBase}: inclination, altitude, phase, blinking, owner, colour): carried
 *       by a satellite item in {@link #ORBIT} (defaults with the satellite's registered colour where it has none), copied
 *       onto the satellite when it reaches orbit ({@code XSatelliteRegistry.ORBITED}, into {@code Satellite.extra}) and
 *       from chip to chip by the satellite linker ({@code TileEntityMachineSatLinker.CHIP_COPY}, both satellite items).
 *       The settings screen and the sky drawing of satellites are client work left to the sky port.</li>
 *   <li><b>The three new satellites</b>: {@link SatelliteDysonRelay} ({@code sat_dyson_relay}), the war base
 *       {@link SatelliteWar} and {@link SatelliteRailgun} ({@code sat_war}), saved by name ({@code ntmspace:dyson_relay},
 *       {@code ntmspace:railgun}); which item launches which is the {@code hbm:satellite} item data map (this mod's
 *       copy adds the two; hbm-space's other {@code sat_*} items are HBM's {@code satellite_*}, as the rocket port maps
 *       them, so HBM's legacy chips stay launch-nothing as in 1.7 HBM).</li>
 *   <li>{@code /ntmsatellites orbit <frequency>}: hbm-space's frequency override, a branch added to HBM's command.</li>
 * </ul>
 */
public final class SpaceSatellites {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ITEMS);

    /** 1.7 {@code sat_dyson_relay} ({@code ItemSatellite(32_000)}), on the missile tab. */
    public static final RegistryObject<ItemSatChip> SAT_DYSON_RELAY =
            REG.<ItemSatChip>item("sat_dyson_relay", ItemDysonRelay::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite("ntmspace:items/sat_dyson_relay")).tab(HbmTab.MISSILES);
    /** 1.7 {@code sat_war} ({@code ItemSatellite(128_000)}, Gerald's sprite), on no tab. */
    public static final RegistryObject<ItemSatChip> SAT_WAR =
            REG.item("sat_war", ItemSatChip::new, p -> p.stacksTo(1)).transform(Presets.sprite("hbm:items/sat_gerald"));

    /** A satellite item's orbit settings ({@code ntmspace:sat_orbit}). */
    public static final GolemComponent<SatelliteOrbit> ORBIT =
            GolemComponent.of(NtmSpace.MOD_ID, "sat_orbit", () -> SatelliteOrbit.CODEC);

    public static SatelliteType DYSON_RELAY;
    public static SatelliteType RAILGUN;

    /** 1.7 {@code XSatelliteRegistry.registerSatellite} colours, by satellite type name. */
    private static final Map<String, float[]> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("mapper", new float[] {0.538F, 1.0F, 0.523F});
        COLORS.put("scanner", new float[] {0.544F, 0.680F, 1.0F});
        COLORS.put("radar", new float[] {0.134F, 1.0F, 0.134F});
        COLORS.put("laser", new float[] {0.221F, 0.663F, 1.0F});
        COLORS.put("resonator", new float[] {1.0F, 0.646F, 0.181F});
        COLORS.put("relay", new float[] {1.0F, 0.15F, 0.15F});
        COLORS.put("miner", new float[] {0.46F, 0.56F, 0.68F});
        COLORS.put("lunar_miner", new float[] {0.42F, 0.54F, 0.82F});
        COLORS.put("ntmspace:dyson_relay", new float[] {1.0F, 0.9F, 0.8F});
        COLORS.put("horizons", new float[] {0.0F, 0.0F, 0.0F});
        COLORS.put("ntmspace:railgun", new float[] {0.0F, 0.0F, 0.0F});
        COLORS.put("precision_laser", new float[] {0.221F, 1.0F, 0.663F});
        COLORS.put("detector", new float[] {0.1F, 0.2F, 0.3F});
        COLORS.put("ray_scan", new float[] {0.1F, 0.2F, 0.3F});
    }

    private static boolean installed;

    private SpaceSatellites() {}

    /** At mod construction. */
    public static synchronized void init() {
        if (installed) return;
        installed = true;
        DYSON_RELAY = SatelliteType.register("ntmspace:dyson_relay", SatelliteDysonRelay::new, SatelliteDysonRelay.CODEC, null);
        RAILGUN = SatelliteType.register("ntmspace:railgun", SatelliteRailgun::new, SatelliteRailgun.CODEC, null);

        Environment.SATELLITES.register(SpaceSatellites::satelliteLevel);
        XSatelliteRegistry.ORBITED.register((level, stack, freq, sat) -> orbit(stack, sat.type()).write(sat.extra));
        TileEntityMachineSatLinker.CHIP_COPY.register(SpaceSatellites::copyItemData);
        CompatCommands.server(SpaceSatellites::registerCommand);
    }

    /**
     * 1.7 {@code SatelliteSavedData.getData(world, x, z)} and {@code getTargetDimensionId}: the level whose satellites
     * serve {@code level} at {@code pos}, or null for {@code level} itself.
     */
    public static @Nullable ServerLevel satelliteLevel(ServerLevel level, @Nullable BlockPos pos, ItemStack launched) {
        ResourceKey<Level> key = satelliteLevelKey(level.getServer(), level.dimension(), pos, launched);
        if (key == null || key.equals(level.dimension())) return null;
        return level.getServer().getLevel(key);
    }

    /**
     * The level key {@link #satelliteLevel} answers, or null for the level itself: the redirected body's level for a
     * relay or a miner being launched, else, in orbit, the level of the body the station at {@code pos} orbits.
     */
    public static @Nullable ResourceKey<Level> satelliteLevelKey(MinecraftServer server, ResourceKey<Level> level,
                                                                 @Nullable BlockPos pos, ItemStack launched) {
        String redirect = targetBody(XSatelliteRegistry.typeFor(launched));
        if (redirect != null) return Orbit.levelOf(SolarSystem.body(redirect));
        if (pos != null && level.equals(Orbit.ORBIT)) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(server, pos.getX(), pos.getZ());
            return Orbit.levelOf(station.orbiting);
        }
        return null;
    }

    /** 1.7 {@code getTargetDimensionId(Class)}: relay to Duna, lunar miner to the Mun, asteroid miner to Dres. */
    public static @Nullable String targetBody(@Nullable SatelliteType type) {
        if (type == null) return null;
        if (type == SatelliteType.RELAY) return "duna";
        if (type == SatelliteType.LUNAR_MINER) return "mun";
        if (type == SatelliteType.MINER) return "dres";
        return null;
    }

    /** 1.7 {@code XSatelliteRegistry.getRegisteredColor}: a satellite type's colour (white for one hbm-space lacked). */
    public static float[] color(@Nullable SatelliteType type) {
        float[] color = type == null ? null : COLORS.get(type.name());
        return color == null ? new float[] {1F, 1F, 1F} : color.clone();
    }

    /** 1.7 {@code SatelliteBase.getItemData}: a satellite item's settings, the defaults where it has none. */
    public static SatelliteOrbit orbit(ItemStack stack) {
        return orbit(stack, XSatelliteRegistry.typeFor(stack));
    }

    private static SatelliteOrbit orbit(ItemStack stack, @Nullable SatelliteType type) {
        return ORBIT.get(stack).orElseGet(() -> SatelliteOrbit.defaults(color(type)));
    }

    /** The orbit settings of a satellite in orbit (1.7 {@code SatelliteBase} fields). */
    public static SatelliteOrbit orbit(Satellite sat) {
        return SatelliteOrbit.read(sat.extra, color(sat.type()));
    }

    /** 1.7 {@code SatelliteBase.copyItemData}, which the linker ran when both chips were satellite items. */
    public static boolean copyItemData(ItemStack source, ItemStack copy) {
        if (XSatelliteRegistry.typeFor(source) == null || XSatelliteRegistry.typeFor(copy) == null) return false;
        SatelliteOrbit settings = orbit(source);
        if (settings.equals(ORBIT.get(copy).orElse(null))) return false;
        ORBIT.set(copy, settings);
        return true;
    }

    /** hbm-space {@code CommandSatellites orbit [frequency]}: launch the held satellite on another frequency. */
    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmsatellites")
                .requires(dev.golem.core.command.Permissions.level(4))
                .then(Commands.literal("orbit")
                        .then(Commands.argument("frequency", IntegerArgumentType.integer())
                                .executes(ctx -> orbit(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "frequency"))))));
    }

    private static int orbit(CommandSourceStack source, int freq) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("commands.satellite.should_be_run_as_player").withStyle(ChatFormatting.RED));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        // as HBM's own orbit: any chip but the blank ID chip; one that launches nothing is still consumed (1.7)
        if (held.getItem() instanceof ISatChip && held.getItem() != ModSatelliteItems.sat_chip.get()) {
            Satellite.orbit((ServerLevel) player.level(), held, freq, player.getX(), player.getY(), player.getZ());
            held.shrink(1);
            source.sendSuccess(() -> Component.translatable("commands.satellite.satellite_orbited")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.satellite.not_a_satellite").withStyle(ChatFormatting.RED));
        return 0;
    }
}
