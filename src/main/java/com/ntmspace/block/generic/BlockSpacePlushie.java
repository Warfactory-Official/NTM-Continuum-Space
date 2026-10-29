package com.ntmspace.block.generic;

import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.tileentity.deco.TileEntityPressablePlushie;
import com.ntmspace.fork.ForkContent;
import com.ntmspace.registry.LeftoverContent;
import dev.golem.core.Ids;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The plushies hbm-space added to HBM's {@code BlockPlushie}: Kasane Teto, Hatsune Miku, Akita Neru and FAT TETO
 * ({@code PlushieType.TETO/MIKU/NERU/FATO}). HBM's {@link BlockPlushie.PlushieType} is a closed variant family,
 * so the additional types use {@code ntmspace:plushie}, on HBM's {@link BlockTrinket} figure (sixteen-way
 * facing, the type in the block entity saved by name, dropped by a player's break only) with one item each,
 * {@code ntmspace:plushie_teto}, {@code _miku}, {@code _neru}, {@code _fato}. Cloth, hardness 0, resistance 0.
 *
 * <p>Right-click (hbm-space {@code onBlockActivated}): a Teto fed butter ({@code ntmspace:butter}) becomes FAT TETO -
 * one butter used, squished, {@code hbm:player.groan} at volume 1, pitch 2; otherwise the client squishes it (11 ticks)
 * and the server plays the type's sound at volume 0.25 ({@code hbm:block.teto}, {@code block.miku},
 * {@code block.akita}; here {@code ntmspace:}), FAT TETO at pitch 0.5. FAT TETO is not in the creative list (1.7
 * {@code getSubBlocks} skipped it). A plushie above a press takes its authored compression and squeaky-pain stroke.
 */
public class BlockSpacePlushie extends BlockTrinket implements TickingBlock {

    /**
     * Space 1.7 BlockPlushie.onBlockActivated plays every original plushie at volume 0.25.
     */
    public static final class Base extends BlockPlushie {

        public Base(Properties properties) {
            super(properties);
        }

        @Override
        protected float squishVolume(PlushieType type) {
            return 0.25F;
        }

        @Override
        public boolean isPressable() {
            return true;
        }

        @Override
        public SoundEvent pressSound() {
            return ForkContent.SQUEAKY_PAIN.get();
        }
    }

    public enum Type implements BlockPlushie.Plushie {
        TETO("Kasane Teto", "please help I've been trapped in her basement for da-", "block.teto"),
        MIKU("Hatsune Miku", "In your wifi, and your heart.", "block.miku"),
        NERU("Akita Neru", "Careful, she might electrocute you.", "block.akita"),
        FATO("FAT TETO", "pls don't bully me", "block.teto");

        public static final Type[] VALUES = values();

        public final String label;
        public final String inscription;
        /**
         * The {@code ntmspace} sound event path.
         */
        public final String sound;

        Type(String label, String inscription, String sound) {
            this.label = label;
            this.inscription = inscription;
            this.sound = sound;
        }

        public static Type of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }

        /**
         * {@code plushie_teto} ...
         */
        public String id() {
            return "plushie_" + name().toLowerCase(java.util.Locale.ROOT);
        }

        @Override
        public float scaleX() {
            return this == FATO ? 1F : 0.5F;
        }

        @Override
        public float scaleY() {
            return 0.5F;
        }
    }

    public BlockSpacePlushie(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Plushie(LeftoverContent.PLUSHIE_BE.get(), pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (feed(level, pos, player, stack)) return InteractionResult.SUCCESS;
        // Butter is only for Teto: anyone else holding it passes, rather than squishing.
        if (isButter(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return squish(level, pos) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * The butter half of 1.7 {@code onBlockActivated}: a Teto fed butter becomes FAT TETO. Whether it happened.
     */
    public static boolean feed(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof Plushie plushie) || plushie.type() != Type.TETO || !isButter(stack))
            return false;
        plushie.squishTimer = 11;
        if (!level.isClientSide()) {
            plushie.setTypeOrdinal(Type.FATO.ordinal());
            if (!player.getAbilities().instabuild) stack.shrink(1);
            sound(level, pos, "hbm", "player.groan", 1F, 2F);
        }
        return true;
    }

    /**
     * The rest of 1.7 {@code onBlockActivated}: the client squishes, the server squeaks. Whether there was a plushie.
     */
    public static boolean squish(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Plushie plushie)) return false;
        if (level.isClientSide()) {
            plushie.squishTimer = 11;
        } else {
            Type type = plushie.type();
            sound(level, pos, "ntmspace", type.sound, 0.25F, type == Type.FATO ? 0.5F : 1F);
        }
        return true;
    }

    /**
     * hbm-space's {@code ModItems.butter}.
     */
    public static boolean isButter(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Ids.of("ntmspace", "butter"));
    }

    private static void sound(Level level, BlockPos pos, String namespace, String path, float volume, float pitch) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getOptional(Ids.of(namespace, path)).orElse(null);
        if (event != null) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, event, SoundSource.BLOCKS,
                    volume, pitch);
        }
    }

    /**
     * hbm-space {@code TileEntityPlushie}: the type (saved by name) and the client's squish timer.
     */
    public static class Plushie extends TileEntityPressablePlushie {

        public Plushie(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        protected Enum<?>[] typeValues() {
            return Type.VALUES;
        }

        public Type type() {
            return Type.of(typeOrdinal());
        }

        @Override
        public BlockPlushie.Plushie kind() {
            return type();
        }

        @Override
        public void setKind(BlockPlushie.Plushie kind) {
            setTypeOrdinal(((Type) kind).ordinal());
        }

        @Override
        public boolean isPressable() {
            return true;
        }

        @Override
        public SoundEvent pressSound() {
            return ForkContent.SQUEAKY_PAIN.get();
        }

    }
}
