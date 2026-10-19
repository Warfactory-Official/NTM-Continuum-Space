package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.machine.CombatDropPod;
import com.ntmspace.entity.missile.CombatDropPods;
import com.ntmspace.entity.missile.EntityCombatDropPod;
import com.ntmspace.tileentity.machine.storage.TileEntityCombatDropPod;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's combat drop pod (the alien invasion's glyphid pod): the falling entity {@code entity_combat_pod}, the
 * landed block {@code combat_drop} with its tile entity, and the two sounds hbm-space added for it.
 *
 * <ul>
 *   <li>Entity: 1.7 {@code addEntity(EntityCombatDropPod, "entity_combat_pod", 1000)}: tracking range 1000 blocks
 *       (63 chunks, as HBM's {@code entity_soyuz_capsule}), every-tick updates, {@code EntityThrowable}'s 0.25 size.</li>
 *   <li>Block: 1.7 {@code new CombatDropPod(Material.iron)}, hardness 5, resistance 10, missile tab, step sound stone
 *       (1.7's default), iron needs a tool; drops 16 {@code hbm:ingot_steel}. Drawn by its renderer; its item is the
 *       flat {@code block_steel} icon (1.7 drew a render-type -1 block's item as its block texture).</li>
 *   <li>Sounds: 1.7 {@code hbm:block.hatchOpen} / {@code block.hatchImpact} (files {@code misc/bomb_open},
 *       {@code misc/bombimpact}); a modern id is lower case, so {@code ntmspace:block.hatchopen} /
 *       {@code block.hatchimpact}.</li>
 * </ul>
 */
public final class DropPodContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.EntityTypes ENTITY_TYPES = DeferredRegister.createEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, ENTITY_TYPES, SOUNDS);

    public static final RegistryObject<SoundEvent> HATCH_OPEN = REG.sound("block.hatchopen");
    public static final RegistryObject<SoundEvent> HATCH_IMPACT = REG.sound("block.hatchimpact");

    public static final RegistryObject<CombatDropPod> COMBAT_DROP = REG.<CombatDropPod>block("combat_drop", CombatDropPod::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
                            .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false))
            .transform(Presets.rendered(Variants.simple())).loot(Loot.stacks("hbm:ingot_steel", 16)).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> COMBAT_DROP_ITEM = REG.blockItem(COMBAT_DROP)
            .transform(Presets.sprite("hbm:blocks/block_steel")).tab(HbmTab.MISSILES);

    public static final RegistryObject<BlockEntityType<TileEntityCombatDropPod>> COMBAT_DROP_BE = REG.blockEntity("combat_drop",
            () -> BlockEntities.ticking(TileEntityCombatDropPod::new, TileEntityCombatDropPod::clientTick,
                    TileEntityCombatDropPod::serverTick, COMBAT_DROP.get()));

    public static final Registrar.EntityHandle<EntityCombatDropPod> COMBAT_POD = REG.entity("entity_combat_pod", key -> Entities.build(
            Entities.noLoot(EntityType.Builder.<EntityCombatDropPod>of(EntityCombatDropPod::new, MobCategory.MISC))
                    .fireImmune().sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));

    private DropPodContent() {}

    public static void init() {
        CombatDropPods.init();
    }
}
