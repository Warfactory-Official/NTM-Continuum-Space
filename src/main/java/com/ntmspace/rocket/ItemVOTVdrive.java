package com.ntmspace.rocket;

import com.ntmspace.config.SpaceConfig;
import com.hbm.items.ItemEnumMulti;
import com.hbm.sound.ModSounds;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.orbit.NavDrives;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.world.CompatTooltipContext;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemVOTVdrive} ({@code hard_drive_full}): a navigation drive for one destination. 1.7 kept the
 * destination body in the item damage; here every {@link DriveBody} is its own item ({@code hard_drive_full_<body>},
 * HBM's rule for 1.7 metadata families), drawn as the {@code votv_f} drive with the body's overlay. The coordinates and
 * flags are the {@code ntmspace:drive} component ({@link DriveData}).
 *
 * <ul>
 *   <li>Used on a block of the destination body: sets the landing coordinates and processes the drive.</li>
 *   <li>An orbit drive used on a block of a surface: recalls a docked drop pod of that station to the block.</li>
 *   <li>Used in the air on the destination body (or in creative): processes the drive; while riding a rocket that is
 *       landed or awaiting launch, swaps the held drive with the rocket's.</li>
 * </ul>
 */
public class ItemVOTVdrive extends ItemEnumMulti {

    /**
     * The client's level for tooltips (set by the client start-up; null on a server).
     */
    public static volatile Supplier<@Nullable Level> clientLevel = () -> null;

    private final DriveBody body;

    public ItemVOTVdrive(Properties properties, DriveBody body) {
        super(DriveBody.class, body, properties);
        this.body = body;
    }

    public DriveBody body() {
        return body;
    }

    /**
     * The orbit agent's hooks: the drive's destination for the station computer and {@code /ntmstations}.
     */
    public static void registerHooks() {
        NavDrives.READERS.register(stack -> {
            if (!(stack.getItem() instanceof ItemVOTVdrive)) return null;
            Destination destination = getDestination(stack);
            return new NavDrives.Destination(
                    destination.body == DriveBody.ORBIT ? NavDrives.ORBIT : destination.body.name,
                    destination.x, destination.z);
        });
        // 1.7 /ntmstations fetch: a processed orbit drive for the station's grid square
        NavDrives.FACTORIES.register((x, z, name) -> {
            ItemStack drive = new ItemStack(RocketItems.drive(DriveBody.ORBIT));
            RocketItems.DRIVE.set(drive,
                    DriveData.EMPTY.withCoordinates(x, z).withProcessed(true).withStation(name, "", false));
            return drive;
        });
    }

    // ---------------------------------------------------------------------------------------- 1.7 statics

    public static DriveData data(ItemStack stack) {
        return RocketItems.DRIVE.getOr(stack, DriveData.EMPTY);
    }

    public static DriveBody getBody(ItemStack stack) {
        return stack.getItem() instanceof ItemVOTVdrive drive ? drive.body : DriveBody.ORBIT;
    }

    public static @Nullable Destination getDestination(@Nullable ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemVOTVdrive)) return null;
        DriveData data = data(stack);
        return new Destination(getBody(stack), data.x(), data.z());
    }

    /**
     * 1.7 {@code getTarget}: no drive is (null, false, false); an orbit drive asks the server for its station (creating
     * the station's record as 1.7 did, orbiting this level's body until launched); a body drive is (body, false, true).
     */
    public static RocketTarget getTarget(@Nullable ItemStack stack, @Nullable Level level) {
        if (stack == null || !(stack.getItem() instanceof ItemVOTVdrive)) {
            return new RocketTarget(null, false, false);
        }

        Destination destination = getDestination(stack);

        if (destination.body == DriveBody.ORBIT) {
            DriveData data = data(stack);
            if (level == null || level.isClientSide() || level.getServer() == null) {
                SolarSystem.Body body = data.stationBody().isEmpty() ? SolarSystem.body("kerbin") : SolarSystem.body(
                        data.stationBody());
                return new RocketTarget(body, true, data.stationHas());
            }

            OrbitalStation station = OrbitalStation.getStation(level.getServer(), destination.x, destination.z);
            if (!station.hasStation) station.orbiting = Orbit.bodyOf(level);

            return new RocketTarget(station.orbiting, true, station.hasStation);
        } else {
            return new RocketTarget(destination.body.getBody(), false, true);
        }
    }

    /**
     * Publishes the station snapshot while a server machine owns the installed drive.
     */
    public static void publishStation(ItemStack stack, Level level) {
        if (!(stack.getItem() instanceof ItemVOTVdrive) || getBody(stack) != DriveBody.ORBIT) return;
        var destination = getDestination(stack);
        var station = OrbitalStation.getStation(level.getServer(), destination.x, destination.z);
        if (!station.hasStation) station.orbiting = Orbit.bodyOf(level);
        var previous = data(stack);
        var updated = previous.withStation(station.name, station.orbiting.name, station.hasStation);
        if (!updated.equals(previous)) RocketItems.DRIVE.set(stack, updated);
    }

    public static void setApproximateDestination(ItemStack stack, int x, int z) {
        RocketItems.DRIVE.set(stack, data(stack).withApproximate(x, z));
    }

    public static void setCoordinates(ItemStack stack, int x, int z) {
        RocketItems.DRIVE.set(stack, data(stack).withCoordinates(x, z));
    }

    public static int getProcessingTier(ItemStack stack, SolarSystem.Body from) {
        return getBody(stack).getProcessingLevel(from);
    }

    public static boolean getProcessed(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return data(stack).processed();
    }

    public static void setProcessed(ItemStack stack, boolean processed) {
        RocketItems.DRIVE.set(stack, data(stack).withProcessed(processed));
    }

    /**
     * 1.7 {@code getApproximateDestination}: an area for the Stardar to draw so the player can pick a safe spot to
     * land, rolled once within {@code maxProbeDistance} of 0, 0.
     */
    public static Destination getApproximateDestination(ItemStack stack, RandomSource random) {
        DriveData data = data(stack);
        if (data.ax().isEmpty() || data.az().isEmpty()) {
            var range = SpaceConfig.maxProbeDistance;
            data = data.withApproximate(random.nextInt(range * 2) - range, random.nextInt(range * 2) - range);
            RocketItems.DRIVE.set(stack, data);
        }
        return new Destination(getBody(stack), data.ax().get(), data.az().get());
    }

    public static void markCopied(ItemStack stack) {
        RocketItems.DRIVE.set(stack, data(stack).withCopied(true));
    }

    public static boolean wasCopied(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return data(stack).copied();
    }

    // ---------------------------------------------------------------------------------------- use

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isProcessed = getProcessed(stack);
        boolean onDestination = level.dimension().equals(getBody(stack).level());

        // If we're on the body (or in creative), immediately process
        if (!isProcessed && (player.getAbilities().instabuild || player.isCreative() || onDestination)) {
            isProcessed = true;
            setProcessed(stack, true);
        }

        if (isProcessed && player.getVehicle() instanceof EntityRideableRocket rocket) {
            if (rocket.getRocket().stages.size() > 0 || Orbit.inOrbit(level) || rocket.isReusable()) {
                if (rocket.getState() == RocketState.LANDED || rocket.getState() == RocketState.AWAITING) {
                    // Replace our held stack with the rocket drive and place our held drive into the rocket
                    ItemStack newStack = rocket.navDrive != null ? rocket.navDrive : ItemStack.EMPTY;

                    rocket.navDrive = stack.copyWithCount(1);

                    if (!level.isClientSide()) {
                        rocket.setState(RocketState.AWAITING);
                    }

                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.UPGRADE_PLUG.get(),
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.setItemInHand(hand, newStack);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        int x = context.getClickedPos().getX();
        int z = context.getClickedPos().getZ();
        Destination destination = getDestination(stack);

        if (destination.body == DriveBody.ORBIT) {
            if (Orbit.inOrbit(level)) return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (!level.isClientSide() && level.getServer() != null) {
                OrbitalStation station = OrbitalStation.getStation(level.getServer(), destination.x, destination.z);

                if (station.recallPod(Orbit.bodyOf(level), x, z)) {
                    message(player, "Recalling drop pod to coordinates: " + x + ", " + z);
                } else {
                    message(player, "Could not recall drop pod from station!");
                }
            }

            return InteractionResult.SUCCESS;
        }

        boolean onDestination = level.dimension().equals(destination.body.level());
        if (!onDestination) return InteractionResult.TRY_WITH_EMPTY_HAND;

        setCoordinates(stack, x, z);
        setProcessed(stack, true);

        if (!level.isClientSide()) message(player, "Set landing coordinates to: " + x + ", " + z);

        return InteractionResult.SUCCESS;
    }

    private static void message(@Nullable Player player, String text) {
        if (player != null)
            player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }

    // ---------------------------------------------------------------------------------------- tooltip

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        Destination destination = getDestination(stack);
        Level level = clientLevel.get();

        if (destination.body == DriveBody.ORBIT) {
            DriveData data = data(stack);
            String identifier = data.stationName();

            if (identifier.isEmpty()) {
                OrbitalStation fake = new OrbitalStation(SolarSystem.body("kerbin"), destination.x, destination.z);
                identifier = fake.identifier();
            }

            list.add(Component.literal("Destination: ORBITAL STATION"));
            list.add(Component.literal("Station: " + identifier));

            if (level != null && !Orbit.inOrbit(level)) {
                for (String s : Component.translatable("item.hard_drive_full.orbit.desc").getString().split("\\$")) {
                    list.add(Component.literal(s).withStyle(ChatFormatting.GOLD));
                }
            }

            return;
        }

        SolarSystem.Body from = level != null ? Orbit.bodyOf(level) : SolarSystem.body("kerbin");
        int processingLevel = destination.body.getProcessingLevel(from);

        list.add(Component.literal("Destination: ").append(Component.translatable("body." + destination.body.name)
                                                                    .withStyle(ChatFormatting.AQUA)));

        if (destination.x == 0 && destination.z == 0) {
            list.add(Component.literal("Needs destination coordinates!").withStyle(ChatFormatting.GOLD));
        } else if (!getProcessed(stack)) {
            // Display processing level info if not processed
            list.add(Component.literal("Process requirement: Level " + processingLevel));
            list.add(Component.literal("Needs processing!").withStyle(ChatFormatting.GOLD));
            list.add(Component.literal("Target coordinates: " + destination.x + ", " + destination.z));
        } else {
            // Display destination info if processed
            list.add(Component.literal("Processed!").withStyle(ChatFormatting.GREEN));
            list.add(Component.literal("Target coordinates: " + destination.x + ", " + destination.z));
        }

        if (level != null && level.dimension().equals(destination.body.level())) {
            for (String s : Component.translatable("item.hard_drive_full.surface.desc").getString().split("\\$")) {
                list.add(Component.literal(s).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /**
     * 1.7 {@code getSubItems}: the creative drives are processed, {@code x = 1}, {@code ax = 1}.
     */
    public static ItemStack creativeStack(DriveBody body) {
        ItemStack stack = new ItemStack(RocketItems.drive(body));
        RocketItems.DRIVE.set(stack,
                new DriveData(1, 0, true, false, java.util.Optional.of(1), java.util.Optional.empty(), "", "", false));
        return stack;
    }

    /**
     * 1.7 {@code Destination}: a drive body and coordinates (grid square for the orbit).
     */
    public static final class Destination {
        public int x;
        public int z;
        public DriveBody body;

        public Destination(DriveBody body, int x, int z) {
            this.body = body;
            this.x = x;
            this.z = z;
        }

        /**
         * A landing location is the body and exact x/z; drive flags and owning stacks are separate data.
         */
        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Destination destination
                    && body == destination.body && x == destination.x && z == destination.z;
        }

        @Override
        public int hashCode() {
            var hash = body.hashCode();
            hash = 31 * hash + x;
            return 31 * hash + z;
        }
    }
}
