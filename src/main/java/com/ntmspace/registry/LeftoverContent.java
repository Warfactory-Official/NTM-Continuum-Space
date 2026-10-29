package com.ntmspace.registry;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.items.ItemFamily;
import dev.golem.core.dejson.Shape;
import net.minecraft.world.level.block.LadderBlock;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.generic.BlockAlgaeFilm;
import com.ntmspace.block.generic.BlockFenceGate;
import com.ntmspace.block.generic.BlockOrrery;
import com.ntmspace.block.generic.BlockSpacePlushie;
import com.ntmspace.block.generic.BlockWildFlower;
import com.ntmspace.block.generic.BrownBarrel;
import com.ntmspace.item.MachineDescItem;
import com.ntmspace.tileentity.machine.TileEntityAlgaeFilm;
import dev.golem.core.dejson.Model;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.BlockSets;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;

/**
 * The hbm-space blocks other families left over (M3 leftovers): the {@code orrery}, the chain-link {@code fence_gate},
 * the {@code oil_barrel}, the orbital {@code algae_film}, the vocaloid plushies ({@code plushie_teto}, {@code _miku},
 * {@code _neru}, {@code _fato}) and the two wild flowers hbm-space added to HBM's {@code plant_flower}
 * ({@code plant_flower_strawberry}, {@code plant_flower_mint}). Values, tabs and textures are hbm-space's
 * {@code ModBlocks}/{@code ModItems}; the classes say what each one does. Item models of the renderer-drawn blocks:
 * {@link LeftoverItemDeclarations}.
 */
public final class LeftoverContent {

    private static final String NS = NtmSpace.MOD_ID;
    private static final String CUTOUT = "minecraft:cutout";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NS);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NS);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NS);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, SOUNDS);

    // ---------------------------------------------------------------- sounds (hbm-space sounds.json, 1.7 names)

    /** {@code hbm:block.fenceGate}, file {@code block/fence_gate}. */
    public static final RegistryObject<SoundEvent> FENCE_GATE_SOUND = REG.sound("block.fencegate");
    /** {@code hbm:block.teto}, file {@code block/teto}. */
    public static final RegistryObject<SoundEvent> TETO_SOUND = REG.sound("block.teto");
    /** {@code hbm:block.miku}, file {@code block/miku}. */
    public static final RegistryObject<SoundEvent> MIKU_SOUND = REG.sound("block.miku");
    /** {@code hbm:block.akita}, file {@code block/akita}. */
    public static final RegistryObject<SoundEvent> AKITA_SOUND = REG.sound("block.akita");

    /**
     * The fence gate's block set: metal steps, {@code block.fencegate} on opening and closing (1.7 {@code getSFX} for
     * both). A holder: it reads sound events, which exist only in a running game, and this class also initialises in
     * DejsonMain's plain JVM.
     */
    public static final class Sets {
        private Sets() {}

        public static final BlockSetType FENCE_GATE = BlockSets.type("ntmspace_fence_gate", SoundType.METAL,
                FENCE_GATE_SOUND.get(), FENCE_GATE_SOUND.get(), FENCE_GATE_SOUND.get(), FENCE_GATE_SOUND.get());
    }

    // ---------------------------------------------------------------- blocks

    private static UnaryOperator<BlockBehaviour.Properties> rendered(UnaryOperator<BlockBehaviour.Properties> base) {
        return p -> base.apply(p).noOcclusion().isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);
    }

    /** 1.7: {@code BlockOrrery(Material.iron)}, hardness 10, resistance 12 (1.7's 20 in 26.x units, x3/5),
     * machine tab, particles {@code block_steel_machine}. */
    public static final RegistryObject<BlockOrrery> ORRERY = REG.block("orrery", BlockOrrery::new,
                    rendered(p -> p.strength(10.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(Presets.rendered(Variants.simple())).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> ORRERY_ITEM = REG.blockItem(ORRERY).tab(HbmTab.MACHINES);

    /**
     * 1.7: {@code BlockFenceGate(Material.iron)}, hardness 5, resistance 1, no tab (its item {@code fence_gate},
     * {@code ItemModDoor}, texture {@code fence_metal}, is in the blocks tab). Blockstate and models are authored
     * ({@code blockstates/fence_gate.json}, {@code models/block/fence_gate_*}); the lower half drops the item.
     */
    public static final RegistryObject<BlockFenceGate> FENCE_GATE = REG.<BlockFenceGate>block("fence_gate",
                    p -> new BlockFenceGate(Sets.FENCE_GATE, p),
                    p -> p.strength(5.0F, 0.6F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                            .pushReaction(PushReaction.DESTROY))
            .loot(Loot.selfWhen("half", "lower")).tag(BlockTag.PICKAXE)
            // hbm-space BlockMetalFence.canConnectFenceTo: HBM's chain-link fence joins this gate
            .tag(BlockTag.of("hbm:metal_fence_connects"));
    public static final RegistryObject<BlockItem> FENCE_GATE_ITEM = REG.blockItem(FENCE_GATE)
            .transform(Presets.sprite(NS + ":items/fence_metal")).tab(HbmTab.BLOCKS);

    /** 1.7: {@code BrownBarrel(Material.iron)}, hardness 0.5, resistance 2.5, nuke tab, texture {@code barrel_oily}. */
    public static final RegistryObject<BrownBarrel> OIL_BARREL = REG.block("oil_barrel", BrownBarrel::new,
                    rendered(p -> p.strength(0.5F, 2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(Presets.rendered(Variants.simple())).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> OIL_BARREL_ITEM = REG.blockItem(OIL_BARREL).tab(HbmTab.BOMBS);

    /** 1.7: {@code BlockAlgaeFilm(Material.iron)}, hardness 5, resistance 10, machine tab, {@code addStandardInfo}. */
    public static final RegistryObject<BlockAlgaeFilm> ALGAE_FILM = REG.block("algae_film", BlockAlgaeFilm::new,
                    rendered(p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()))
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDescItem> ALGAE_FILM_ITEM = REG.<BlockAlgaeFilm, MachineDescItem>blockItem(
            ALGAE_FILM, MachineDescItem::new, Compat::blockDescription).tab(HbmTab.MACHINES);

    /** 1.7: {@code BlockPlushie}, cloth, hardness 0, resistance 0 (the 500,000 was overwritten); drawn by its renderer. */
    public static final RegistryObject<BlockSpacePlushie> PLUSHIE = REG.block("plushie", BlockSpacePlushie::new,
                    rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.WOOL)))
            .transform(Presets.rendered(Variants.uniformRange("rotation", 0, 15))).loot(Loot.nothing());
    /** One item per type, index = type ordinal; the block's own item is the first (Teto, registered last). */
    public static final List<RegistryObject<ItemBlockDecoVariant>> PLUSHIE_ITEMS = plushieItems();
    public static final ItemFamily<BlockSpacePlushie.Type, ItemBlockDecoVariant> PLUSHIE_FAMILY =
            ItemFamily.of(BlockSpacePlushie.Type.class, type -> PLUSHIE_ITEMS.get(type.ordinal()))
                    .joins(ModDecoCrateBlocks.plushie_family);

    /** 1.7 {@code plant_flower} STRAWBERRY: "Wild Strawberries", plants, blocks tab; drops a strawberry. */
    public static final RegistryObject<BlockWildFlower> PLANT_FLOWER_STRAWBERRY = flower("plant_flower_strawberry", "ntmspace:strawberry");
    public static final RegistryObject<BlockItem> PLANT_FLOWER_STRAWBERRY_ITEM = REG.blockItem(PLANT_FLOWER_STRAWBERRY).tab(HbmTab.BLOCKS);
    /** 1.7 {@code plant_flower} MINT: "Wild Mint"; drops mint leaves. */
    public static final RegistryObject<BlockWildFlower> PLANT_FLOWER_MINT = flower("plant_flower_mint", "ntmspace:mint_leaves");
    public static final RegistryObject<BlockItem> PLANT_FLOWER_MINT_ITEM = REG.blockItem(PLANT_FLOWER_MINT).tab(HbmTab.BLOCKS);

    /**
     * 1.7: the martian/mohobase aluminum ladders ({@code BlockLadder}), stone sound, blocks tab. Vanilla's ladder
     * shape (one plane at the wall) with this block's texture; vanilla climbing needs no seam.
     */
    public static final RegistryObject<LadderBlock> LADDER_ALUMINIUM =
            REG.block("ladder_aluminium", LadderBlock::new,
                            p -> p.strength(0.25F, 1.2F).sound(SoundType.STONE).noOcclusion()
                                    .pushReaction(PushReaction.DESTROY))
                    .model(d -> d.models(Models.renderType(CUTOUT, ladder("ntmspace:blocks/ladder_aluminium")))
                            .states(Variants.facing())
                            .item(Models.blockItem("ntmspace:block/ladder_aluminium")))
                    .loot(Loot.self()).tag(BlockTag.PICKAXE).tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockItem> LADDER_ALUMINIUM_ITEM = REG.blockItem(LADDER_ALUMINIUM).tab(HbmTab.BLOCKS);

    // ---------------------------------------------------------------- block entities (1.7 TileMappings ids in the comments)

    /** 1.7 {@code tileentity_orrery}. */
    public static final RegistryObject<BlockEntityType<BlockOrrery.TileEntityOrrery>> ORRERY_BE = REG.blockEntity("orrery",
            () -> BlockEntities.type((pos, state) -> new BlockOrrery.TileEntityOrrery(LeftoverContent.ORRERY_BE.get(), pos, state),
                    ORRERY.get()));
    /** The oil barrel's renderer anchor and tick (1.7 bound {@code TileEntityBrownBarrel} but never made one). */
    public static final RegistryObject<BlockEntityType<BrownBarrel.Barrel>> OIL_BARREL_BE = REG.blockEntity("oil_barrel",
            () -> BlockEntities.ticking((pos, state) -> new BrownBarrel.Barrel(LeftoverContent.OIL_BARREL_BE.get(), pos, state),
                    BrownBarrel.Barrel::clientTick, BrownBarrel.Barrel::serverTick, OIL_BARREL.get()));
    /** 1.7 {@code tileentity_algae_film}. */
    public static final RegistryObject<BlockEntityType<TileEntityAlgaeFilm>> ALGAE_FILM_BE = REG.blockEntity("algae_film",
            () -> BlockEntities.ticking(TileEntityAlgaeFilm::new, TileEntityAlgaeFilm::clientTick,
                    TileEntityAlgaeFilm::serverTick, ALGAE_FILM.get()));
    /** 1.7 {@code tileentity_ntm_plushie} (HBM's id is HBM's; this is the addon's plushie block). */
    public static final RegistryObject<BlockEntityType<BlockSpacePlushie.Plushie>> PLUSHIE_BE = REG.blockEntity("plushie",
            () -> BlockEntities.clientTicking((pos, state) -> new BlockSpacePlushie.Plushie(LeftoverContent.PLUSHIE_BE.get(), pos, state),
                    BlockSpacePlushie.Plushie::clientTick, PLUSHIE.get()));

    private LeftoverContent() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        // the StarDar's OpenComputers component ntm_stardar, served to CC: Tweaked through HBM's ComputerComponents
        com.ntmspace.tileentity.machine.StardarComputer.register();
        // the wild flowers' biome modifier for Fabric (NeoForge/Forge read the emitted files)
        LeftoverDeclarations.declareWorldgen();
        dev.golem.core.world.BiomeFeatures.install();
        // creative list: Teto, Miku, Neru (1.7 getSubBlocks skipped FAT TETO)
        com.hbm.creativetabs.ModTabs.addStacksTo(HbmTab.BLOCKS, () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for (BlockSpacePlushie.Type type : BlockSpacePlushie.Type.VALUES) {
                if (type != BlockSpacePlushie.Type.FATO) stacks.add(new ItemStack(PLUSHIE_ITEMS.get(type.ordinal()).get()));
            }
            return stacks;
        });
    }

    // ---------------------------------------------------------------- helpers

    /** HBM's {@code ModDecoCrateBlocks.variants}: index = type ordinal, registered 1..n-1 then 0, so 0 is the block's item. */
    private static List<RegistryObject<ItemBlockDecoVariant>> plushieItems() {
        BlockSpacePlushie.Type[] types = BlockSpacePlushie.Type.VALUES;
        @SuppressWarnings("unchecked")
        RegistryObject<ItemBlockDecoVariant>[] items = new RegistryObject[types.length];
        for (int k = 1; k <= types.length; k++) {
            int variant = k % types.length;
            BlockSpacePlushie.Type type = types[variant];
            items[variant] = REG.item(type.id(), p -> new ItemBlockDecoVariant(PLUSHIE.get(), p, variant,
                            i -> Component.translatable("block.ntmspace.plushie", BlockSpacePlushie.Type.of(i).label),
                            i -> List.of(Component.literal(BlockSpacePlushie.Type.of(i).inscription))),
                    Compat::blockDescription);
        }
        List<RegistryObject<ItemBlockDecoVariant>> members = List.of(items);
        ItemBlockDecoVariant.Families.record(PLUSHIE, members);
        return members;
    }

    /** HBM's flower registration ({@code ModTerrainBlocks}): grass sound, hardness 0, no collision, a cutout cross. */
    private static RegistryObject<BlockWildFlower> flower(String id, String drop) {
        String texture = NS + ":blocks/plant_flower." + id.substring("plant_flower_".length());
        return REG.block(id, BlockWildFlower::new,
                        p -> p.strength(0.0F).sound(SoundType.GRASS).noOcclusion().noCollision())
                .model(d -> d.models(Models.renderType(CUTOUT, Models.cross(texture))).states(Variants.simple())
                        .item(Models.generated(texture)))
                .loot(Loot.only(drop));
    }

    /** Vanilla's ladder plane (one textured quad at the wall), rotated per facing by {@link Variants#facing()}. */
    /** Vanilla's ladder plane (one textured quad at the wall), rotated per facing by {@link Variants#facing()}. */
    private static Shape ladder(String texture) {
        return Model.of()
                .textures(Slots.of("texture", texture, "particle", texture))
                .ambientOcclusion(false)
                .elements(Model.Element.box(0, 0, 15.2F, 16, 16, 15.2F).shade(false)
                        .faces(Model.Face.of("north", "#texture").uv(0, 0, 16, 16),
                                Model.Face.of("south", "#texture").uv(0, 0, 16, 16)));
    }
}
