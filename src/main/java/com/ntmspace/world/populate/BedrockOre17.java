package com.ntmspace.world.populate;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import java.util.Random;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code BedrockOre} on a celestial body: a patch of HBM bedrock ore (centre always, the eight around it each
 * one time in two) at y 0 with depth rock above it. {@code generateAuto} sets the deposit's tier, and the acid it needs
 * unless the body names one, from the mean of the body's {@code CelestialBedrockOre} type densities
 * ({@code ItemBedrockOreBase.getOreLevel}, noise salted with the 1.7 dimension id). The spread and the shape come from
 * 1.7's unseeded {@code world.rand}.
 */
public final class BedrockOre17 {

    private BedrockOre17() {}

    /** {@code BedrockOre.generateAuto(world, x, z, targetBlock, acidOverride)}. */
    public static void generateAuto(Populate17.Body body, PopulateWorld world, long worldSeed, int x, int z, Block target) {
        int[] types = body.bedrockOreTypes();
        double total = 0;
        for (int index : types) total += Populate17.oreLevel(worldSeed, body.dimension, x, z, index);
        total /= types.length;

        FluidStack override = body.bedrockAcid();
        FluidStack acid = override != null ? override : boreFluid(total);
        int tier = tier(total);
        generate(world, x, z, new BedrockDeposit(new ItemStack(ModOreBlocks.bedrock_ore_base.get()), acid, 0xD78A16, tier), target);
    }

    /** {@code getBoreFluid}. */
    public static FluidStack boreFluid(double density) {
        if (density > 1.5) return new FluidStack(Fluids.SOLVENT, 2_000);
        if (density > 1) return new FluidStack(Fluids.SULFURIC_ACID, 1_000);
        if (density > 0.75) return new FluidStack(Fluids.WATER, 1_000);
        return null;
    }

    /** {@code getTier}. */
    public static int tier(double density) {
        if (density > 1.5) return 4;
        if (density > 1) return 3;
        if (density > 0.75) return 2;
        return 1;
    }

    /** {@code BedrockOre.generate(world, x, z, stack, acid, color, tier, stone_depth, targetBlock)}. */
    public static void generate(PopulateWorld world, int x, int z, BedrockDeposit deposit, Block target) {
        Random rand = world.worldRandom();
        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                if (world.is(ix, 0, iz, Blocks.BEDROCK)) {
                    if ((ix == x && iz == z) || rand.nextBoolean()) {
                        world.bedrockOre(ix, 0, iz, deposit, rand.nextInt(10));
                    }
                }
            }
        }

        BlockState depthRock = ModOreBlocks.stone_depth.get().defaultBlockState();
        for (int ix = x - 3; ix <= x + 3; ix++) {
            for (int iz = z - 3; iz <= z + 3; iz++) {
                for (int iy = 1; iy < 7; iy++) {
                    if (iy < 3 || world.is(ix, iy, iz, Blocks.BEDROCK)) {
                        BlockState b = world.get(ix, iy, iz);
                        if (b.is(target) || b.is(Blocks.BEDROCK)) {
                            world.set(ix, iy, iz, depthRock);
                        }
                    }
                }
            }
        }
    }
}
