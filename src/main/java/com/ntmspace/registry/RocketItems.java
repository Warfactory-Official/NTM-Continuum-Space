package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemFamily;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.DriveData;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketFuels;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The rocket family's items, hbm-space's {@code ModItems} values: the 18 rocket parts hbm-space added (ids are the 1.7
 * unlocalized names, so {@code rp_fuselage_20_12} is {@code rp_f_20_12}), the assembled rocket {@code rocket_custom},
 * the empty drive {@code hard_drive} and the navigation drives {@code hard_drive_full_<body>} (1.7's one metadata item).
 *
 * <p>The parts are HBM {@link ItemCustomMissilePart}s built with HBM's missile builders, so HBM's missile assembly takes
 * them as 1.7 did (HBM's builders weigh in tonnes and litres: hbm-space's kilograms are divided by 1000); their rocket
 * stats are {@link RocketParts}. hbm-space let fuselages and thrusters stack (rocket clusters): HBM's own are opened
 * with {@link ModCustomMissileItems#PART_PROPERTIES} in {@link #init()}.
 */
public final class RocketItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ITEMS);

    /** {@code rocket_custom}'s design (1.7 NBT: capsule, stages, freq), by part id. */
    public static final GolemComponent<RocketStruct.Design> ROCKET =
            GolemComponent.of(NtmSpace.MOD_ID, "rocket", () -> RocketStruct.Design.CODEC);
    /** 1.7 {@code hasFuel}: the rocket came off a pad or station fully fuelled. */
    public static final GolemComponent<Boolean> ROCKET_FUELLED =
            GolemComponent.of(NtmSpace.MOD_ID, "rocket_fuelled", () -> com.mojang.serialization.Codec.BOOL);
    /** A navigation drive's coordinates and flags. */
    public static final GolemComponent<DriveData> DRIVE = GolemComponent.of(NtmSpace.MOD_ID, "drive", () -> DriveData.CODEC);

    // ---------------------------------------------------------------------------------------- parts

    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_METHALOX = part("mp_thruster_20_methalox", 64,
            p -> p.makeThruster(RocketFuels.METHALOX, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_METHALOX_DUAL = part("mp_thruster_20_methalox_dual", 64,
            p -> p.makeThruster(RocketFuels.METHALOX, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_METHALOX_TRIPLE = part("mp_thruster_20_methalox_triple", 64,
            p -> p.makeThruster(RocketFuels.METHALOX, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_HYDROGEN = part("mp_thruster_20_hydrogen", 64,
            p -> p.makeThruster(FuelType.HYDROGEN, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_HYDROGEN_DUAL = part("mp_thruster_20_hydrogen_dual", 64,
            p -> p.makeThruster(FuelType.HYDROGEN, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_HYDROGEN_TRIPLE = part("mp_thruster_20_hydrogen_triple", 64,
            p -> p.makeThruster(FuelType.HYDROGEN, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_20_HYDRAZINE = part("mp_thruster_20_hydrazine", 64,
            p -> p.makeThruster(RocketFuels.HYDRAZINE, 1F, 100F, PartSize.SIZE_20).setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> MP_THRUSTER_10_HYDRAZINE = part("mp_thruster_10_hydrazine", 64,
            p -> p.makeThruster(RocketFuels.HYDRAZINE, 1F, 1.5F, PartSize.SIZE_10).setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> MP_FUSELAGE_10_HYDRAZINE = part("mp_fuselage_10_hydrazine", 64,
            p -> p.makeFuselage(RocketFuels.HYDRAZINE, 2000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F));
    /** 1.7 field {@code rp_fuselage_20_12_hydrazine}. */
    public static final RegistryObject<ItemCustomMissilePart> MP_FUSELAGE_20_HYDRAZINE = part("mp_fuselage_20_hydrazine", 64,
            p -> p.makeFuselage(RocketFuels.HYDRAZINE, 12500F, PartSize.SIZE_20, PartSize.SIZE_20).setHealth(25F));
    public static final RegistryObject<ItemCustomMissilePart> RP_F_20_12 = part("rp_f_20_12", 64,
            p -> p.makeFuselage(RocketFuels.ANY, 64_000F, PartSize.SIZE_20, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_F_20_6 = part("rp_f_20_6", 64,
            p -> p.makeFuselage(RocketFuels.ANY, 32_000F, PartSize.SIZE_20, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_F_20_3 = part("rp_f_20_3", 64,
            p -> p.makeFuselage(RocketFuels.ANY, 16_000F, PartSize.SIZE_20, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_F_20_1 = part("rp_f_20_1", 64,
            p -> p.makeFuselage(RocketFuels.ANY, 6_000F, PartSize.SIZE_20, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_L_20 = part("rp_l_20", 1,
            p -> p.makeStability(0F, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_C_20 = part("rp_c_20", 1,
            p -> p.makeWarhead(RocketFuels.APOLLO, 15F, 8F, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_SC_20 = part("rp_sc_20", 1,
            p -> p.makeWarhead(RocketFuels.SATELLITE, 15F, 64F, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> RP_POD_20 = part("rp_pod_20", 1,
            p -> p.makeWarhead(RocketFuels.APOLLO, 15F, 4F, PartSize.SIZE_20));

    // ---------------------------------------------------------------------------------------- rocket and drives

    /** 1.7: stack size 1, no creative tab, the {@code missile_custom} sprite. */
    public static final RegistryObject<ItemCustomRocket> ROCKET_CUSTOM = REG.item("rocket_custom", ItemCustomRocket::new, p -> p.stacksTo(1))
            .transform(Presets.sprite("hbm:items/missile_custom"));

    /** 1.7 {@code hard_drive}: an empty drive (sprite {@code votv_e}), parts tab. */
    public static final RegistryObject<Item> HARD_DRIVE = REG.simpleItem("hard_drive", p -> p)
            .transform(Presets.sprite("ntmspace:items/votv_e")).tab(HbmTab.MATERIALS);

    /** 1.7 {@code hard_drive_full}, one item per destination: the drive and the body's overlay, stack size 1, control tab. */
    public static final ItemFamily<DriveBody, ItemVOTVdrive> DRIVES = ItemFamily.of(DriveBody.class, body ->
            REG.item("hard_drive_full_" + body.key(), p -> new ItemVOTVdrive(p, body), p -> p.stacksTo(1))
                    .transform(Presets.sprite("ntmspace:items/votv_f", "ntmspace:items/votv." + body.key())));

    static {
        // 1.7 getSubItems: processed drives with the placeholder coordinates
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for (DriveBody body : DriveBody.values()) stacks.add(ItemVOTVdrive.creativeStack(body));
            return stacks;
        });
    }

    private RocketItems() {}

    public static Item drive(DriveBody body) {
        return DRIVES.get(body);
    }

    /**
     * A part: HBM's builder, then its assembly row (height, OBJ, skin) from {@link RocketParts} so HBM's missile renderer
     * draws it; missile tab.
     */
    private static Registrar.Handle<ItemCustomMissilePart> part(String id, int stack, UnaryOperator<ItemCustomMissilePart> builder) {
        return REG.item(id, props -> {
            ItemCustomMissilePart part = builder.apply(new ItemCustomMissilePart(props));
            RocketPart row = RocketParts.get(NtmSpace.MOD_ID + ":" + id);
            if (row != null) part.setAssembly((float) row.height(), row.mesh(), row.skin());
            return part;
        }, p -> p.stacksTo(stack)).tab(HbmTab.MISSILES);
    }

    public static void init() {
        RocketFuels.init();
        // 1.7's copy() used a fresh uncapped constructor; only base warheads, fins and chips kept their explicit cap.
        ModCustomMissileItems.PART_PROPERTIES.register((id, properties) -> switch (id) {
            case "mp_warhead_15_nuclear_shark", "mp_warhead_15_nuclear_mimi" -> properties.stacksTo(64);
            default -> id.startsWith("mp_fuselage") || id.startsWith("mp_thruster") ? properties.stacksTo(64) : properties;
        });
    }
}
