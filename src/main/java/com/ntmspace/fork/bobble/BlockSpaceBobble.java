package com.ntmspace.fork.bobble;

import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.tileentity.deco.TileEntityTrinket;
import com.ntmspace.fork.ForkContent;
import dev.golem.core.registry.TickingBlock;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The bobbleheads hbm-space added to HBM's {@code BlockBobble} ({@code BobbleType.GWEN, JUICE, JAMESH_2, MRKIMKIMORA}).
 * HBM's types are a closed variant family (inventory E-094: an addon registers its own blocks), so they are
 * {@code ntmspace:bobblehead} on HBM's {@link BlockTrinket} (sixteen-way facing, the type in the block entity saved by
 * name, dropped by a player's break only) with one item each, {@code ntmspace:bobblehead_gwen}, {@code _juice},
 * {@code _jamesh_2}, {@code _mrkimkimora}. As 1.7: iron, hardness 0, a 5x10x5-pixel box, right-click opens the
 * commemorative panel ({@code GUIScreenBobble}), drawn by HBM's figure ({@code RenderBobble.renderFigure}).
 */
public class BlockSpaceBobble extends BlockTrinket implements TickingBlock {

    /** hbm-space's four new {@code BobbleType}s, values verbatim (tooltip name, socket label, contribution, inscription). */
    public enum Type {
        GWEN("Gwen", "Gwen", "Numero Uno Homie",
                "HELP ME I'M TRAPPED IN THIS FUCKING POLYRESIN PRISON YOU NEED TO LET ME OUT PLEASE SMASH IT OPEN DO IT NOW CRACK IT",
                true, "gwen"),
        JUICE("Juicy_Lad", "Juicy_Lad", "The Mojave Testing for this funny fork",
                "\"What should the Inscription be?\",$ \"Uhh.. I'll think about it.\"", true, "juicy_lad"),
        JAMESH_2("JamesH_2", "JamesH_2", "The fork itself", "COME ON AND SLAM", true, "jamesh_2"),
        /** Drawn as the {@code MrKimkimora} part of hbm-space's bobble model, not as the player figure. */
        MRKIMKIMORA("MrKimkimora", "MrKimkimora", "Part of NTM:Space models,$textures and russian localization",
                "I know your IP. Your IP is 127.0.0.1.", false, "mrkimkimora");

        public static final Type[] VALUES = values();

        public final String name;
        public final String label;
        public final String contribution;
        public final String inscription;
        public final boolean skinLayers;
        /** {@code ntmspace:textures/models/trinkets/<texture>.png}. */
        public final String texture;

        Type(String name, String label, String contribution, String inscription, boolean skinLayers, String texture) {
            this.name = name;
            this.label = label;
            this.contribution = contribution;
            this.inscription = inscription;
            this.skinLayers = skinLayers;
            this.texture = texture;
        }

        public static Type of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }

        /** {@code bobblehead_gwen} ... */
        public String id() {
            return "bobblehead_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Client hook: opens HBM's {@code GUIScreenBobble} with the figure's text. Installed by the client start-up. */
    public static Consumer<Type> OPEN_SCREEN = type -> {};

    private static final VoxelShape SHAPE = Block.box(5.5D, 0.0D, 5.5D, 10.5D, 10.0D, 10.5D);

    public BlockSpaceBobble(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Bobble(ForkContent.BOBBLEHEAD_BE.get(), pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof Bobble bobble) OPEN_SCREEN.accept(bobble.type());
        return InteractionResult.SUCCESS;
    }

    /** hbm-space {@code TileEntityBobble}: the type, saved by name. */
    public static class Bobble extends TileEntityTrinket {

        public Bobble(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        protected Enum<?>[] typeValues() {
            return Type.VALUES;
        }

        public Type type() {
            return Type.of(typeOrdinal());
        }
    }
}
