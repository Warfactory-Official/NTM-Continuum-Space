package com.ntmspace.block;

import dev.golem.core.world.CompatBlock;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * hbm-space's per-body ore ({@code BlockOre} and its fluid ores {@code BlockOreFluid}): one 1.7 block whose metadata was
 * the celestial body it was found on, drawn as that body's stone with the ore on top. Here one block with a
 * {@link #BODY} state (HBM's rule for 1.7 block families: one block, a named state) and one block item per state
 * ({@link BodyOreItem}); the 1.7 terrain maps its metadata to the state ({@code legacy/BodyOres17}). The model per body is
 * the body's stone under the ore overlay.
 *
 * <p>1.7 kept the body in the dropped item's damage ({@code damageDropped} returned the metadata for an ore that drops
 * itself, and a silk-touched block kept it too): the loot table names the block's own item (the Kerbin form) and
 * {@link BodyOre#drops} turns it into the broken state's item.
 */
public class BlockBodyOre extends CompatBlock implements BodyOre {

    /** {@code SolarSystem.Body} without {@code ORBIT}, in its order (1.7 metadata = ordinal + 1). */
    public enum Body implements StringRepresentable {
        KERBIN, MUN, MINMUS, DUNA, MOHO, DRES, EVE, IKE, LAYTHE, TEKTO;

        private final String name = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return name;
        }

        /** The body of 1.7 metadata {@code meta} ({@code SolarSystem.Body} ordinal); orbit (0) draws as Kerbin. */
        public static Body ofMeta(int meta) {
            return meta >= 2 && meta <= 10 ? values()[meta - 1] : KERBIN;
        }

        /**
         * The stone each body's ores are drawn on ({@code CelestialBody.withBlockTextures}, the first texture), as a
         * texture id.
         */
        public String stoneTexture() {
            return switch (this) {
                case KERBIN, LAYTHE -> "minecraft:block/stone";
                case MUN -> "ntmspace:blocks/moon_rock";
                case MINMUS -> "ntmspace:blocks/minmus_stone";
                case DUNA -> "ntmspace:blocks/duna_rock";
                case MOHO -> "ntmspace:blocks/moho_stone";
                case DRES -> "ntmspace:blocks/dresbase";
                case EVE -> "ntmspace:blocks/eve_stone_2";
                case IKE -> "ntmspace:blocks/ike_stone";
                case TEKTO -> "hbm:blocks/basalt";
            };
        }
    }

    public static final EnumProperty<Body> BODY = EnumProperty.create("body", Body.class);

    public BlockBodyOre(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BODY, Body.KERBIN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BODY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BodyOre.placement(this, context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return BodyOre.drops(this, state, super.getDrops(state, params));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return BodyOre.stack(this, state);
    }
}
