package com.ntmspace.block.fluid;

import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.SlabType;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One neighbour's Space 1.7 {@code SchrabidicBlock.getReaction} chemistry. The Schrabidic block calls it on every
 * side after a fluid tick; the level and its random source belong to that server thread.
 */
public final class SchrabidicReactions {

    private static final Block SCHRABIDIC = byId("schrabidic_block");
    private static final Block SLAKED = byId("sellafield_slaked");
    private static final Block WASTE_LOG = byId("waste_log");
    private static final Block WASTE_PLANKS = byId("waste_planks");
    private static final Block REINFORCED_GLASS = byId("reinforced_glass");
    private static final Block OBSIDIAN_GRAVEL = byId("gravel_obsidian");

    private static final Block BRICK_OBSIDIAN = byId("brick_obsidian");
    private static final Block BRICK_OBSIDIAN_STAIRS = byId("brick_obsidian_stairs");
    private static final Block BLOCK_POLYMER = byId("block_polymer");
    private static final Set<Block> LEGACY_STONE_BRICKS = Set.of(Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS);
    private static final Set<Block> LEGACY_SANDSTONES = Set.of(Blocks.SANDSTONE,
            Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE);
    private static final Set<Block> LEGACY_STONE_SLABS = Set.of(Blocks.SMOOTH_STONE_SLAB,
            Blocks.SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB, Blocks.COBBLESTONE_SLAB,
            Blocks.BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB, Blocks.QUARTZ_SLAB);

    private static final Set<Block> NEW_STONE_FAMILIES = Set.of(
            Blocks.GRANITE, Blocks.GRANITE_STAIRS, Blocks.GRANITE_SLAB,
            Blocks.GRANITE_WALL, Blocks.DIORITE, Blocks.DIORITE_STAIRS,
            Blocks.DIORITE_SLAB, Blocks.DIORITE_WALL, Blocks.ANDESITE,
            Blocks.ANDESITE_STAIRS, Blocks.ANDESITE_SLAB, Blocks.ANDESITE_WALL,
            Blocks.POLISHED_GRANITE, Blocks.POLISHED_GRANITE_STAIRS, Blocks.POLISHED_GRANITE_SLAB,
            Blocks.POLISHED_DIORITE, Blocks.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_DIORITE_SLAB,
            Blocks.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE_SLAB,
            Blocks.PRISMARINE, Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE_SLAB,
            Blocks.PRISMARINE_WALL, Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICK_STAIRS,
            Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE, Blocks.DARK_PRISMARINE_STAIRS,
            Blocks.DARK_PRISMARINE_SLAB, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR,
            Blocks.PURPUR_STAIRS, Blocks.PURPUR_SLAB, Blocks.END_STONE_BRICKS,
            Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_WALL,
            Blocks.STONE_SLAB, Blocks.STONE_STAIRS, Blocks.SMOOTH_STONE,
            Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_WALL,
            Blocks.STONE_BRICK_WALL, Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_SLAB,
            Blocks.BRICK_WALL, Blocks.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICK_STAIRS,
            Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICK_WALL, Blocks.CHISELED_NETHER_BRICKS,
            Blocks.CRACKED_NETHER_BRICKS, Blocks.NETHER_BRICK_WALL, Blocks.SMOOTH_QUARTZ,
            Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.QUARTZ_BRICKS,
            Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT,
            Blocks.BLACKSTONE, Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE_SLAB,
            Blocks.BLACKSTONE_WALL, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_STAIRS,
            Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
            Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE_SLAB,
            Blocks.COBBLED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE_STAIRS,
            Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_WALL,
            Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB,
            Blocks.DEEPSLATE_TILE_WALL, Blocks.CHISELED_DEEPSLATE, Blocks.CRACKED_DEEPSLATE_BRICKS,
            Blocks.CRACKED_DEEPSLATE_TILES, Blocks.TUFF,
            Blocks.CALCITE,
            Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE);
    // The 1.21 tuff family, by id: 1.20.1 has none of them.
    private static final Set<Block> TUFF_FAMILY = Stream.of("tuff_stairs", "tuff_slab", "tuff_wall", "polished_tuff",
                    "polished_tuff_stairs", "polished_tuff_slab", "polished_tuff_wall", "tuff_bricks", "tuff_brick_stairs",
                    "tuff_brick_slab", "tuff_brick_wall", "chiseled_tuff", "chiseled_tuff_bricks")
            .flatMap(id -> BuiltInRegistries.BLOCK.getOptional(Ids.of("minecraft", id)).stream())
            .collect(Collectors.toUnmodifiableSet());

    private SchrabidicReactions() { }

    private static Block byId(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", id)).orElseThrow();
    }

    public static void react(ServerLevel level, BlockPos pos, RandomSource random) {
        // 26.1: a fluid tick at a chunk edge must not generate a neighbour merely to test its reaction.
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return;
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        // 1.7: the resistance floor also catches air, endlessly fizzing and rewriting the same air cell.
        if (state.isAir()) return;
        BlockState carrier = state;
        if (MultiblockSurface.isFoldedCell(state)) {
            // 1.7: makeDummy/makeExtra use their core's block; pooled cells recover that source identity.
            var core = MultiblockSurface.coreOfCell(level, pos);
            if (core == null) return;
            carrier = level.getBlockState(core);
        }
        double x = pos.getX() + random.nextFloat();
        double y = pos.getY() + random.nextFloat();
        double z = pos.getZ() + random.nextFloat();
        BlockState eroded = erosion(state);

        if (block != SCHRABIDIC && carrier.getFluidState().is(FluidTags.WATER)) {
            level.setBlockAndUpdate(pos, SLAKED.defaultBlockState());
        } else if ((state.is(BlockTags.LOGS) || state.is(BlockTags.BAMBOO_BLOCKS)) && random.nextInt(4) == 0) {
            change(level, pos, WASTE_LOG.defaultBlockState());
        } else if ((state.is(BlockTags.PLANKS) || block == Blocks.BAMBOO_MOSAIC) && random.nextInt(4) == 0) {
            change(level, pos, WASTE_PLANKS.defaultBlockState());
        } else if ((state.is(BlockTags.LEAVES) || glass(carrier) && block != REINFORCED_GLASS)
                && random.nextInt(4) == 0) {
            destroy(level, pos, x, y, z);
        } else if (eroded != null && random.nextInt(4) == 0) {
            change(level, pos, eroded);
        // 1.7: Material.iron. No 26.1 property separates metal from rock => every pickaxe block past the erosion
        // chain; chain ends, polymer, reinforced glass keep their 1.7 protection.
        } else if (eroded == null && carrier.is(BlockTags.MINEABLE_WITH_PICKAXE) && !resists(carrier.getBlock())
                && random.nextInt(4) == 0) {
            destroy(level, pos, x, y, z);
        } else if (carrier.getBlock().getExplosionResistance() < 1.2F && block != BLOCK_POLYMER
                && block != OBSIDIAN_GRAVEL && block != Blocks.SAND
                && block != Blocks.RED_SAND && block != Blocks.CLAY) {
            destroy(level, pos, x, y, z);
        }
    }

    private static @Nullable BlockState erosion(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.STONE || block == Blocks.STONE_BRICK_STAIRS || LEGACY_STONE_BRICKS.contains(block)
                || legacySingleSlab(state) || NEW_STONE_FAMILIES.contains(block)
                || TUFF_FAMILY.contains(block)) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (block == Blocks.COBBLESTONE) return Blocks.GRAVEL.defaultBlockState();
        if (block == Blocks.GRAVEL) return Blocks.CLAY.defaultBlockState();
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == BRICK_OBSIDIAN
                || block == BRICK_OBSIDIAN_STAIRS) {
            return OBSIDIAN_GRAVEL.defaultBlockState();
        }
        if (LEGACY_SANDSTONES.contains(block) || block == Blocks.RED_SANDSTONE
                || block == Blocks.CHISELED_RED_SANDSTONE || block == Blocks.CUT_RED_SANDSTONE
                || block == Blocks.END_STONE) {
            return Blocks.SAND.defaultBlockState();
        }
        return null;
    }

    // glowstone: 1.7 glass, 26.1 PLING
    private static boolean glass(BlockState state) {
        return state.instrument() == NoteBlockInstrument.HAT || state.is(Blocks.GLOWSTONE);
    }

    private static boolean resists(Block block) {
        return block == Blocks.NETHERITE_BLOCK || block == BLOCK_POLYMER || block == OBSIDIAN_GRAVEL
                || block == SLAKED || block == REINFORCED_GLASS;
    }

    private static boolean legacySingleSlab(BlockState state) {
        return LEGACY_STONE_SLABS.contains(state.getBlock())
                && state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE;
    }

    private static void change(ServerLevel level, BlockPos pos, BlockState result) {
        fizz(level, pos);
        level.setBlockAndUpdate(pos, result);
    }

    private static void destroy(ServerLevel level, BlockPos pos, double x, double y, double z) {
        fizz(level, pos);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0D, 0D, 0D, 0D);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void fizz(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1.0F);
    }
}
