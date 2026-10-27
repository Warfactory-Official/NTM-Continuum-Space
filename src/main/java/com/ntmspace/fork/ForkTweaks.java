package com.ntmspace.fork;

import com.hbm.blocks.machine.BlockRefueler;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.handler.ArmorModEvents;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.ntmspace.registry.SpaceItems;
import com.ntmspace.registry.SpaceSpecialItems;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
//? if forge {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
*///?}
//? if neoforge {
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
//?}
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.logic.EntityPlaneBase;
import com.hbm.explosion.ExplosionTom;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.food.ItemLemon;
import com.hbm.items.food.ItemPancake;
import com.hbm.items.special.ItemEnergy;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.tileentity.machine.storage.TileEntityFileCabinet;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.registry.Substitutions;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatConsumables;
import dev.golem.core.world.Remainders;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Small changes hbm-space made to base HBM classes through hooks, substitutions or tunables, so they hold only while NTM:
 * Space is installed (fork-content triage, part 2 item 7):
 * <ul>
 *   <li>{@code ExplosionTom}: the lava sheet under the old terrain only within 500 blocks of the impact, like the
 *       clearing above it ({@code ExplosionTom.lavaRadius}, V-174);</li>
 *   <li>{@code EntityPlaneBase}: a shot-down plane's crash also composes the large explosion effect
 *       ({@code ExplosionCreator.composeEffectLarge}; {@code EntityPlaneBase.CRASH}, H-73);</li>
 *   <li>{@code TileEntityFileCabinet}: a cabinet holding a Soyuz ({@code missile_soyuz}, any of its three forms) explodes
 *       every tick it does - {@code ExplosionVNT} 15 with a 48-ray allocator, no drops and slag debris, the cross entity
 *       processor (7.5, range x3), the standard player processor and the standard effect
 *       ({@code TileEntityFileCabinet.TICK}, H-74);</li>
 *   <li>{@code ItemEnergy}: coffee leaves its mug ({@code ntmspace:cmug_empty}; {@code ItemEnergy.EMPTY}, H-75).</li>
 * </ul>
 */
public final class ForkTweaks {

    private ForkTweaks() {}

    /** Space 1.7 ModItems food values, applied before construction while retaining each item's class and other properties. */
    public static void substituteFoods() {
        substituteLemon("bio_wafer", 4, 0.6F);
        substituteLemon("fooditem", 2, 0.6F);
        substituteLemon("pudding", 6, 0.6F);
        // ItemPancake's 1.7 constructor calls setAlwaysEdible in addition to its authored nutrition and saturation.
        Substitutions.item(NtmSpace.MOD_ID, "hbm:pancake", properties -> new ItemPancake(
                CompatConsumables.foodAlwaysEdible(properties, 20, 1.0F, List.of())));
    }

    private static void substituteLemon(String id, int nutrition, float saturation) {
        Substitutions.item(NtmSpace.MOD_ID, "hbm:" + id, properties -> new ItemLemon(
                CompatConsumables.food(properties, nutrition, saturation, List.of()), id));
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        // Keep Host's late death-cancellation and mob-loot gates; the causing player includes indirect player kills.
        ArmorModEvents.KILLED_BY_PLAYER.register((entity, killer) -> {
            if (entity instanceof Villager && killer instanceof ServerPlayer) {
                Compat.spawnAtLocation(entity, (ServerLevel) entity.level(),
                        new ItemStack(SpaceItems.FLESH.get(), 5), 0F);
            }
        });
        ILookOverlay.PROVIDERS.register(ForkTweaks::refueler);
        SpaceItemEvents.init();
        ExplosionTom.lavaRadius = 500;
        EntityPlaneBase.CRASH.register((plane, x, y, z) -> ExplosionCreator.composeEffectLarge(plane.level(), x, y, z));
        TileEntityFileCabinet.TICK.register(ForkTweaks::soyuz);
        // 1.7 Space EntityMaskMan.dropFewItems:119 drops the V1 where NTM drops the bottled cloud
        // (M65, coin and skull stay). A loader event swaps the drop; nothing here touches base classes.
        //? if neoforge {
        NeoForge.EVENT_BUS.addListener(
                (LivingDropsEvent event) -> maskManDrops(event.getEntity(),
                        event.getDrops()));
        //?} elif forge {
        /*MinecraftForge.EVENT_BUS.addListener(
                (LivingDropsEvent event) -> maskManDrops(event.getEntity(),
                        event.getDrops()));
        *///?}
        ItemEnergy.EMPTY.register(id -> id.equals("coffee")
                ? BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", "cmug_empty")).orElse(null) : null);
        // hbm-space ItemEnergy: setContainerItem(cmug_empty), the mug machines and recipes hand back.
        Remainders.add(ModConsumableTailItems.coffee, SpaceItems.CMUG_EMPTY);
    }

    /** hbm-space {@code TileEntityFileCabinet.updateEntity}'s soyuz check. */
    static void soyuz(TileEntityFileCabinet cabinet) {
        Level level = cabinet.getLevel();
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < cabinet.getContainerSize(); i++) {
            ItemStack slot = cabinet.getItem(i);
            if (slot.isEmpty() || !BuiltInRegistries.ITEM.getKey(slot.getItem()).toString().startsWith("hbm:missile_soyuz")) continue;
            BlockPos pos = cabinet.getBlockPos();
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 15);
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                    .withBlockEffect(new BlockMutatorDebris(() -> ModBlocks.block_slag.get().defaultBlockState())));
            xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(3));
            xnt.setPlayerProcessor(new PlayerProcessorStandard());
            xnt.explode();
            // 1.7 composeEffect(15, 5, 1, 45, 10, 16, 50, 1, 3, -2, 200) = composeEffectStandard
            ExplosionCreator.composeEffectStandard(level, x, y, z);
            if (cabinet.isRemoved()) return;
        }
    }

    /** The mask man's bottled cloud becomes the V1 (drops are captured, so the swap holds). */
    static void maskManDrops(LivingEntity entity,
                             Collection<ItemEntity> drops) {
        if (!(entity instanceof EntityMaskMan)) return;
        drops.removeIf(drop -> drop.getItem().is(ModArmorModItems.bottled_cloud.get()));
        var at = entity.position();
        drops.add(new ItemEntity(entity.level(), at.x, at.y, at.z,
                new ItemStack(SpaceSpecialItems.V1.get())));
    }

    /** hbm-space {@code BlockRefueler.printHook}; the readout contains no client-only state. */
    static ILookOverlay.Data refueler(Level level, BlockPos pos,
                                                       Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof BlockRefueler block)
                || !(level.getBlockEntity(pos) instanceof TileEntityRefueler refueler)) return null;
        var type = refueler.tank.getTankType();
        return new ILookOverlay.Data(block.getName().getString(), 0xffff00, 0x404000,
                List.of("&[" + type.getColor() + "&]" + type.getDisplayNameString()));
    }
}
