package com.ntmspace.fluid.trait;

import com.google.gson.JsonObject;
import com.hbm.api.fluid.TooltipTrait;
import com.hbm.inventory.fluid.FluidProperties;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The fluid traits hbm-space adds to 1.7's {@code FluidTrait} registry ({@code rocket}, {@code terraformer}, {@code uk},
 * {@code explosive}), added to HBM's {@code fluid_property} format in the {@code ntmspace} namespace. Only
 * {@link FT_Rocket} is on any of hbm-space's fluids; the other three were registered for its trait config file and are
 * kept for datapacks that set them.
 */
public final class SpaceFluidTraits {

    private SpaceFluidTraits() {}

    private static boolean registered;

    /** Adds the traits to the file format. Mod construction, before any fluid property file is read. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        FluidProperties.registerTrait(FT_Rocket.NAME, FT_Rocket.class, FT_Rocket::read, FT_Rocket::write);
        FluidProperties.registerTrait(FT_Terraformer.NAME, FT_Terraformer.class, FT_Terraformer::read, FT_Terraformer::write);
        FluidProperties.registerFlag("ntmspace:uk", ULTRAKILL);
        FluidProperties.registerFlag("ntmspace:explosive", EXPLOSIVE);
    }

    /** hbm-space {@code FluidTraitSimple.FT_ULTRAKILL}: a hidden dark-red {@code [ULTRAKILL]} line, nothing else. */
    public static final class FT_ULTRAKILL implements TooltipTrait {
        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[ULTRAKILL]").withStyle(ChatFormatting.DARK_RED));
        }
    }

    /** hbm-space {@code FluidTraitSimple.FT_EXPLOSIVE}: a hidden red {@code [Explosive]} line, nothing else. */
    public static final class FT_EXPLOSIVE implements TooltipTrait {
        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Explosive]").withStyle(ChatFormatting.RED));
        }
    }

    /** hbm-space {@code Fluids.ULTRAKILL}. */
    public static final FT_ULTRAKILL ULTRAKILL = new FT_ULTRAKILL();
    /** hbm-space {@code Fluids.EXPLOSIVE}. */
    public static final FT_EXPLOSIVE EXPLOSIVE = new FT_EXPLOSIVE();

    /**
     * hbm-space's {@code FT_Terraformer}: a fluid the chemical thrower turns ground into a body's soil with
     * ({@code EntityChemical}; Duna sand, Laythe silt, otherwise stone). File form {@code {"block": "DUNA"}}, the 1.7
     * {@code SolarSystem.Body} name.
     */
    public static final class FT_Terraformer implements TooltipTrait {

        public static final String NAME = "ntmspace:terraformer";

        private final String body;

        public FT_Terraformer(String body) {
            this.body = body;
        }

        /** The 1.7 {@code SolarSystem.Body} name ({@code DUNA}, {@code LAYTHE}, ...). */
        public String getBody() {
            return body;
        }

        /** hbm-space {@code FT_Terraformer.addInfo}. */
        @Override
        public void addInfo(List<Component> info) {
            info.add(Component.literal("[Terraforming]").withStyle(ChatFormatting.AQUA));
        }

        static FT_Terraformer read(JsonObject json) {
            return new FT_Terraformer(json.get("block").getAsString());
        }

        static JsonObject write(Object trait) {
            JsonObject json = new JsonObject();
            json.addProperty("block", ((FT_Terraformer) trait).body);
            return json;
        }
    }
}
