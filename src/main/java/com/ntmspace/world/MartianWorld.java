package com.ntmspace.world;

import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.ntmspace.NtmSpace;
import com.hbm.world.gen.nbt.NBTStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import com.ntmspace.orbit.SpaceTeleporter;
import com.ntmspace.world.structure.SpaceStructures;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.CompatSavedData;
import dev.golem.core.world.WorldTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * hbm-space's {@code WorldTypeTeleport.martian}, "The Martian": a world type whose players start on Duna. As in 1.7:
 * <ul>
 *   <li>Every player joining the world for the first time is sent to Duna at (0, 0), on the ground
 *       ({@code onPlayerJoin}: {@code CelestialTeleporter.teleport(player, duna, 0, 0, 0, true)}, once per player:
 *       1.7's {@code HbmPlayerProps.hasWarped}, here Golem Core's first-join flag from the player's statistics).</li>
 *   <li>Duna's chunk (0, 0) carries the {@code martian-base} structure, centred on the column (0, 0) one block below its
 *       height value, with working spotlights ({@code Spotlight.disableOnGeneration = false}). 1.7 built it while
 *       populating that chunk; it spans x -20..19 and z -21..20, more than a modern chunk's population may write, so it
 *       is built once when the server starts, before anyone arrives (saved as done in Duna's level).</li>
 *   <li>A player who dies on Duna respawns on Duna ({@code WorldProviderDuna.getRespawnDimension}), on the ground at
 *       the world spawn's column (1.7's {@code getSpawnPoint} there).</li>
 * </ul>
 * The overworld is vanilla's. The world type is Golem Core's {@link WorldTypes} {@code ntmspace:martian}: the preset
 * {@code data/ntmspace/worldgen/world_preset/martian.json}, its parameter list, the tag {@code minecraft:normal} and the
 * name {@code generator.ntmspace.martian} ("The Martian", hbm-space's {@code generator.martian}).
 */
public final class MartianWorld {

    public static final String NAME = "martian";

    /**
     * The real single-origin lifecycle build, also available to previews and metadata inspection.
     */
    public record BasePlan(ResourceKey<Level> dimension, BlockPos column, NBTStructure template) {
    }

    public static BasePlan basePlan() {
        return new BasePlan(SpaceDimensions.DUNA, BlockPos.ZERO, SpaceStructures.Files.MARTIAN_BASE);
    }

    private MartianWorld() {
    }

    /**
     * Dejson: the world type is listed on the create-world screen (tag {@code minecraft:normal}).
     */
    public static void declare() {
        dev.golem.core.dejson.Tags.member("worldgen/world_preset", "minecraft", "normal", NtmSpace.MOD_ID + ":" + NAME);
    }

    public static void init() {
        WorldTypes.declare(NtmSpace.MOD_ID, NAME);
        Lifecycle.onServerStarted(MartianWorld::buildBase);
        Lifecycle.onPlayerJoin((player, firstTime) -> {
            if (firstTime && is(player.level().getServer())) {
                SpaceTeleporter.teleport(player, SpaceDimensions.DUNA, 0, 0, 0, true);
            }
        });
    }

    /**
     * Whether this world is "The Martian".
     */
    public static boolean is(MinecraftServer server) {
        return server != null && WorldTypes.is(server, NtmSpace.MOD_ID, NAME);
    }

    /**
     * The martian base on Duna's (0, 0), once per world.
     */
    static void buildBase(MinecraftServer server) {
        if (!is(server)) return;
        var plan = basePlan();
        ServerLevel duna = server.getLevel(plan.dimension());
        if (duna == null) return;
        BaseData data = CompatSavedData.get(duna, BaseData.ID, BaseData::new, BaseData.CODEC);
        if (data.built) return;
        var template = plan.template();
        if (!template.isLoaded()) throw new IllegalStateException("The Martian base template did not load");
        var x = plan.column().getX();
        var z = plan.column().getZ();
        var minX = x - template.getSizeX() / 2;
        var minZ = z - template.getSizeZ() / 2;
        // Every chunk the actual centred template touches, generated in full first.
        for (var cx = minX >> 4; cx <= (minX + template.getSizeX() - 1) >> 4; cx++) {
            for (var cz = minZ >> 4; cz <= (minZ + template.getSizeZ() - 1) >> 4; cz++) duna.getChunk(cx, cz);
        }
        // 1.7 getHeightValue(origin) - 1: the top block of the column.
        var y = duna.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        LegacyStructurePalette.withSpotlightsWorking(() -> template.build(duna, x, y, z));
        data.built = true;
        data.setDirty();
        NtmSpace.LOG.info("Built the martian base on Duna at (0, {}, 0)", y);
    }

    /**
     * Duna's record that the base stands.
     */
    static final class BaseData extends CompatSavedData<BaseData> {
        static final String ID = "ntmspace:martian_base";
        static final com.mojang.serialization.Codec<BaseData> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        com.mojang.serialization.Codec.BOOL.optionalFieldOf("built", false)
                                                           .forGetter(data -> data.built)
                ).apply(instance, BaseData::new));

        boolean built;

        BaseData() {
            this(false);
        }

        BaseData(boolean built) {
            this.built = built;
        }

        @Override
        protected com.mojang.serialization.Codec<BaseData> codec() {
            return CODEC;
        }
    }
}
