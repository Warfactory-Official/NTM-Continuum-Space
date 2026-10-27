package com.ntmspace.tileentity.machine;

import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** A menu viewer's current complete surface map; ordinary block sync contains no map. */
public final class StardarHeightmap {
    private StardarHeightmap() {}
    public record Payload(BlockPos pos, int @Nullable [] heights) implements GolemPayload {
        public Payload {
            if (heights != null && heights.length != TileEntityMachineStardar.MAP_SIZE * TileEntityMachineStardar.MAP_SIZE)
                throw new IllegalArgumentException("A StarDar map must contain 256 by 256 heights");
        }
        @Override public Identifier id() { return CODEC.id(); }
    }
    public static final GolemCodec<Payload> CODEC = GolemCodec.of(Payload.class,
            Ids.of(NtmSpace.MOD_ID, "stardar_heightmap"),
            (payload, buffer) -> {
                buffer.writeBlockPos(payload.pos());
                buffer.writeBoolean(payload.heights() != null);
                if (payload.heights() != null) for (var height : payload.heights()) buffer.writeVarInt(height);
            },
            buffer -> {
                var pos = buffer.readBlockPos();
                int[] heights = null;
                if (buffer.readBoolean()) {
                    heights = new int[TileEntityMachineStardar.MAP_SIZE * TileEntityMachineStardar.MAP_SIZE];
                    for (var i = 0; i < heights.length; i++) heights[i] = buffer.readVarInt();
                }
                return new Payload(pos, heights);
            });
    public static void init() { GolemNetwork.declarePlayToClient(CODEC); }
    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> receive(context.level(), payload));
    }
    public static void receive(Level level, Payload payload) {
        if (level.getBlockEntity(payload.pos()) instanceof TileEntityMachineStardar stardar)
            stardar.receiveHeightmap(payload.heights());
    }
    public static void send(ServerPlayer viewer, BlockPos pos, int @Nullable [] heights) {
        GolemNetwork.sendToPlayer(viewer, CODEC, new Payload(pos, heights));
    }
}
