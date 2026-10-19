package com.ntmspace.item;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.transporter.TileEntityTransporterBase;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.Ids;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemTransporterLinker} ("Transporter Pad Linker"): sneak-use on a transporter saves it on the
 * linker; plain use opens the linking screen ({@code GUITransporterLinker}) for that transporter. While the linker is
 * in the main hand the server sends the saved transporters (with their live names and links) every second tick
 * (1.7 {@code TransporterLinkerPacket}).
 *
 * <p>Saved by name: 1.7 kept an int array of dimension ids and per-dimension coordinate arrays in the stack NBT; here the
 * stack holds a list of {level id, x, y, z} ({@link #SAVED}), in the order they were added (1.7: a {@code HashSet}, so
 * the order on screen followed the hash of the coordinates).
 */
public class ItemTransporterLinker extends CompatItem {

    /** One saved transporter: the level by id ({@code minecraft:overworld}), never a numeric dimension. */
    public record Saved(String dimension, int x, int y, int z) {
        public static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Saved::dimension),
                Codec.INT.fieldOf("x").forGetter(Saved::x),
                Codec.INT.fieldOf("y").forGetter(Saved::y),
                Codec.INT.fieldOf("z").forGetter(Saved::z)).apply(i, Saved::new));
    }

    /** The transporters saved on a linker. */
    public static final GolemComponent<List<Saved>> SAVED = GolemComponent.of(NtmSpace.MOD_ID, "transporter_linker",
            () -> Saved.CODEC.listOf());

    /** The client's list (1.7 {@code currentTransporters}), as last sent by the server. */
    public static volatile List<TransporterInfo> currentTransporters = List.of();

    /** Opens the linking screen for the clicked transporter; installed by the client (no screen class in common code). */
    private static Consumer<TransporterInfo> screenOpener = info -> { };

    public static void setScreenOpener(Consumer<TransporterInfo> opener) {
        screenOpener = opener;
    }

    public ItemTransporterLinker(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Sneak-click to save transporter"));
        tooltip.add(Component.literal("Use on transporter to link to a saved transporter"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        // 1.7 CompatExternal.getCoreFromPos
        BlockEntity tile = MultiblockSurface.coreEntity(level, context.getClickedPos());
        if (!(tile instanceof TileEntityTransporterBase transporter) || player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                addTransporter(context.getItemInHand(), level, transporter);
                player.sendSystemMessage(Component.literal("Added transporter to linker"));
            }
        } else if (level.isClientSide()) {
            screenOpener.accept(TransporterInfo.from(dimensionOf(level), transporter));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot, boolean held) {
        if (!(owner instanceof ServerPlayer player) || player.getMainHandItem() != stack) return;
        if (owner.tickCount % 2 == 0) {
            List<TransporterInfo> transporters = getTransporters(level.getServer(), stack);
            if (GolemNetwork.canSendToPlayer(player, CODEC)) GolemNetwork.sendToPlayer(player, CODEC, new Payload(transporters));
        }
    }

    // ---------------------------------------------------------------------------------------------- saved list

    /** 1.7 {@code addTransporter}: the saved set plus this one (the set drops transporters that are gone). */
    public static void addTransporter(ItemStack stack, Level level, TileEntityTransporterBase transporter) {
        if (level.getServer() == null) return;
        Set<TransporterInfo> transporters = loadTransporters(level.getServer(), stack);
        transporters.add(TransporterInfo.from(dimensionOf(level), transporter));
        saveTransporters(stack, transporters);
    }

    /** 1.7 {@code getTransporters}: the saved transporters that still exist, with their current names and links. */
    public static List<TransporterInfo> getTransporters(MinecraftServer server, ItemStack stack) {
        return new ArrayList<>(loadTransporters(server, stack));
    }

    private static Set<TransporterInfo> loadTransporters(MinecraftServer server, ItemStack stack) {
        Set<TransporterInfo> transporters = new LinkedHashSet<>();
        for (Saved saved : SAVED.getOr(stack, List.of())) {
            ServerLevel world = server.getLevel(levelKey(saved.dimension()));
            if (world == null) continue;
            BlockPos pos = new BlockPos(saved.x(), saved.y(), saved.z());
            // 1.7 read the tile entity of an unloaded chunk by loading it; only loaded chunks are read here (a placed
            // transporter keeps its own chunk loaded)
            if (!world.isLoaded(pos)) continue;
            if (world.getBlockEntity(pos) instanceof TileEntityTransporterBase transporter) {
                transporters.add(TransporterInfo.from(saved.dimension(), transporter));
            }
        }
        return transporters;
    }

    private static void saveTransporters(ItemStack stack, Set<TransporterInfo> transporters) {
        List<Saved> list = new ArrayList<>();
        for (TransporterInfo info : transporters) list.add(new Saved(info.dimension, info.x, info.y, info.z));
        SAVED.set(stack, List.copyOf(list));
    }

    // ---------------------------------------------------------------------------------------------- levels

    /** The id of a level, the form a transporter's level is saved and sent in. */
    public static String dimensionOf(Level level) {
        return Ids.keyId(level.dimension()).toString();
    }

    public static ResourceKey<Level> levelKey(String dimension) {
        return ResourceKey.create(Registries.DIMENSION, Ids.parse(dimension));
    }

    /** 1.7 {@code CelestialBody.getBody(dimensionId)}: the body whose surface the level is (Kerbin for any other level). */
    public static SolarSystem.Body bodyOf(String dimension) {
        ResourceKey<Level> key = levelKey(dimension);
        for (String name : Orbit.landableNames()) {
            SolarSystem.Body body = SolarSystem.body(name);
            if (Orbit.levelOf(body).equals(key)) return body;
        }
        return SolarSystem.body("kerbin");
    }

    // ---------------------------------------------------------------------------------------------- info

    /** 1.7 {@code TransporterInfo}: trivially comparable (level and position), for sets and the client list. */
    public static final class TransporterInfo {

        public final String name;
        /** 1.7 {@code planet}: the texture of the body the level belongs to, {@code ntmspace:textures/misc/space/<body>.png}. */
        public final Identifier planet;
        public @Nullable TransporterInfo linkedTo;

        public final String dimension;
        public final int x, y, z;

        public TransporterInfo(String name, String dimension, int x, int y, int z) {
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.planet = Ids.of(NtmSpace.MOD_ID, bodyOf(dimension).texture());
        }

        public static TransporterInfo from(String dimension, TileEntityTransporterBase transporter) {
            BlockPos pos = transporter.getBlockPos();
            TransporterInfo info = new TransporterInfo(transporter.getTransporterName(), dimension, pos.getX(), pos.getY(), pos.getZ());
            info.linkedTo = transporter.getLinkedTransporter();
            return info;
        }

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }

        void write(RegistryFriendlyByteBuf buf, boolean recurse) {
            buf.writeUtf(name);
            buf.writeUtf(dimension);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            boolean linked = recurse && linkedTo != null;
            buf.writeBoolean(linked);
            if (linked) linkedTo.write(buf, false);
        }

        static TransporterInfo read(RegistryFriendlyByteBuf buf) {
            TransporterInfo info = new TransporterInfo(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt());
            if (buf.readBoolean()) info.linkedTo = read(buf);
            return info;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, x, y, z);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            TransporterInfo info = (TransporterInfo) other;
            return dimension.equals(info.dimension) && x == info.x && y == info.y && z == info.z;
        }
    }

    // ---------------------------------------------------------------------------------------------- sync

    /** 1.7 {@code TransporterLinkerPacket}: the held linker's transporters, to its holder. */
    public record Payload(List<TransporterInfo> transporters) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.ofRegistry(Payload.class, Ids.of(NtmSpace.MOD_ID, "transporter_linker"),
            (payload, buf) -> {
                buf.writeVarInt(payload.transporters().size());
                for (TransporterInfo info : payload.transporters()) info.write(buf, true);
            },
            buf -> {
                int count = buf.readVarInt();
                List<TransporterInfo> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) list.add(TransporterInfo.read(buf));
                return new Payload(list);
            });

    public static void declarePayload() {
        GolemNetwork.declarePlayToClient(CODEC);
    }

    public static void registerClientPayload() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> currentTransporters = List.copyOf(payload.transporters()));
    }
}
