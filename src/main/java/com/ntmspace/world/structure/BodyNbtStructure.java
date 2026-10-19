package com.ntmspace.world.structure;

import com.hbm.config.StructureConfig;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.NtmSpace;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * {@code ntmspace:body_nbt}: one of a celestial body's hbm-space NBT structures (codec fields {@code spawn}, the 1.7
 * spawn name, and {@code body}). Every structure of a body's structure set ({@code data/ntmspace/worldgen/structure_set/
 * <body>_structures.json}, {@code hbm:config_spread} with the body's 1.7 salt) asks {@link BodyStructures#pick} for its
 * region and generates only where it is the winner, so a region holds exactly what 1.7 put there (or nothing, for a
 * null weight or a biome no spawn accepts). The start is HBM's 1.7 jigsaw assembler ({@code NBTStructure.Start}) drawing
 * from the pick's random.
 */
public class BodyNbtStructure extends Structure {

    public static final MapCodec<BodyNbtStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("spawn").forGetter(BodyNbtStructure::spawnName),
            Codec.STRING.fieldOf("body").forGetter(s -> s.body.id())
    ).apply(i, (settings, spawn, body) -> {
        BodyStructures.Body b = BodyStructures.Body.byId(body);
        if (b == null) throw new IllegalArgumentException("no celestial body with structures is called '" + body + "'");
        return new BodyNbtStructure(settings, spawn, b);
    }));

    private static final Set<String> MISSING = new HashSet<>();

    private final String spawnName;
    private final BodyStructures.Body body;

    public BodyNbtStructure(StructureSettings settings, String spawnName, BodyStructures.Body body) {
        super(settings);
        this.spawnName = spawnName;
        this.body = body;
    }

    public String spawnName() {
        return spawnName;
    }

    public BodyStructures.Body body() {
        return body;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (StructureConfig.enableStructures == 0) return Optional.empty();

        SpawnCondition spawn = NBTStructure.getStructure(spawnName);
        if (spawn == null) {
            synchronized (MISSING) {
                if (MISSING.add(spawnName)) NtmSpace.LOG.error("[structures] no structure registered as '{}'", spawnName);
            }
            return Optional.empty();
        }

        int chunkX = context.chunkPos().getMinBlockX() >> 4;
        int chunkZ = context.chunkPos().getMinBlockZ() >> 4;
        net.minecraft.util.RandomSource rand;
        if (NBTStructure.GenStructure.checksBiome(context)) {
            BodyStructures.Biome biome = BodyStructures.biomeAt(body, context.biomeSource(), chunkX, chunkZ);
            if (biome == null) return Optional.empty();
            BodyStructures.Pick pick = BodyStructures.pick(body, context.seed(), chunkX, chunkZ, biome);
            if (pick == null || !spawnName.equals(pick.member().spawn())) return Optional.empty();
            rand = pick.random();
        } else {
            rand = context.random(); // /place structure: here, whatever the region pick says
        }

        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        int surface = context.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        BlockPos center = new BlockPos(cx, surface, cz);
        return Optional.of(new GenerationStub(center, builder -> NBTStructure.Start.assemble(builder, context, spawn, rand)));
    }

    @Override
    public StructureType<?> type() {
        return SpaceStructureTypes.BODY_NBT.get();
    }
}
