package com.ntmspace.registry;

import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.war.WarController;
import com.ntmspace.entity.mob.siege.EntitySiegeCraft;
import com.ntmspace.inventory.war.ContainerMachineWarController;
import com.ntmspace.tileentity.war.TileEntityMachineWarController;
import dev.golem.core.dejson.Tints;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.LivingAttributes;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Tabs;
import dev.golem.core.world.SpawnEggs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's war between bodies and its invasions, what they register:
 *
 * <ul>
 *   <li>{@code war_controller} ({@code ModBlocks.war_controller}: {@code WarController}, iron, hardness 10, resistance
 *       10, metal sound, {@code block_steel} texture, no creative tab - hidden in NEI too), its tile entity
 *       ({@code tileentity_war}) and menu.</li>
 *   <li>{@code entity_micro_ufo}, the siege craft ({@code addMob(EntitySiegeCraft, "entity_micro_ufo", 0x5B963E,
 *       0xC0B286)}): 7 x 1, fireproof; its spawn egg.</li>
 *   <li>The three sounds hbm-space added for it: {@code alarm.ping} (the invasion warning), {@code misc.impact} (a war
 *       projectile arriving) and {@code misc.fireflash} (the railgun, only on dead 1.7 paths).</li>
 * </ul>
 */
public final class WarContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.MenuTypes MENUS = DeferredRegister.createMenuTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.EntityTypes ENTITY_TYPES = DeferredRegister.createEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS, ENTITY_TYPES, SOUNDS);

    public static final RegistryObject<SoundEvent> ALARM_PING = REG.sound("alarm.ping");
    public static final RegistryObject<SoundEvent> MISC_IMPACT = REG.sound("misc.impact");
    public static final RegistryObject<SoundEvent> MISC_FIREFLASH = REG.sound("misc.fireflash");

    public static final RegistryObject<WarController> WAR_CONTROLLER = REG.<WarController>block("war_controller", WarController::new,
                    p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll("hbm:blocks/block_steel")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** 1.7 {@code setCreativeTab(null)}: on no tab. */
    public static final RegistryObject<BlockItem> WAR_CONTROLLER_ITEM = REG.blockItem(WAR_CONTROLLER);

    public static final RegistryObject<BlockEntityType<TileEntityMachineWarController>> WAR_CONTROLLER_BE = REG.blockEntity(
            "war_controller", () -> BlockEntities.serverTicking(TileEntityMachineWarController::new,
                    TileEntityMachineWarController::serverTick, WAR_CONTROLLER.get()));

    public static final RegistryObject<MenuType<ContainerMachineWarController>> WAR_CONTROLLER_MENU =
            REG.menu("war_controller", () -> Menus.typeWithPos(ContainerMachineWarController::new));

    /** 1.7 {@code EntityUFOBase} is {@code ISuffocationImmune} in hbm-space, so the craft is too. */
    public static final Registrar.EntityHandle<EntitySiegeCraft> SIEGE_CRAFT = REG.entity("entity_micro_ufo", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntitySiegeCraft>of(EntitySiegeCraft::new, MobCategory.MONSTER))
                            .sized(7.0F, 1.0F).fireImmune().clientTrackingRange(5).updateInterval(3), key))
            .tag(SpaceMobs.SUFFOCATION_IMMUNE);

    public static final RegistryObject<Item> SIEGE_CRAFT_EGG = REG.<Item>item("entity_micro_ufo_spawn_egg",
                    p -> SpawnEggs.create(() -> SIEGE_CRAFT.get(), 0x5B963E, 0xC0B286, p))
            .transform(Presets.sprite("hbm:items/spawn_egg", "hbm:items/spawn_egg_overlay")
                    .tints(Tints.constant(0xFF000000 | 0x5B963E), Tints.constant(0xFF000000 | 0xC0B286)));

    private WarContent() {}

    public static void init() {
        LivingAttributes.register(SIEGE_CRAFT, EntitySiegeCraft::createAttributes);
        Tabs.modifyEntries(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                dev.golem.core.Ids.of("minecraft", "spawn_eggs")), entries -> entries.accept(new ItemStack(SIEGE_CRAFT_EGG.get())));
        com.ntmspace.war.Invasions.init();
        com.ntmspace.war.WarSatellites.init();
        com.ntmspace.war.ThatmoStrikes.init();
        com.ntmspace.command.WarCommands.init();
    }
}
