package com.ntmspace.fluid.trait;

import com.google.gson.JsonObject;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * hbm-space's {@code FT_Rocket}: a rocket propellant, with its specific impulse and its thrust per bucket (the
 * transporter pads and station engines read them). In {@code fluid_property} files it is
 * {@code "ntmspace:rocket": {"isp": 380, "thrust": 700000}} (1.7's JSON names).
 */
public final class FT_Rocket implements com.hbm.api.fluid.TooltipTrait {

    public static final String NAME = "ntmspace:rocket";

    private final int isp;
    private final long thrust;

    public FT_Rocket(int isp, long thrust) {
        this.isp = isp;
        this.thrust = thrust;
    }

    public int getISP() {
        return isp;
    }

    public long getThrust() {
        return thrust;
    }

    /** 1.7 {@code addInfo}; HBM's fluid tooltip calls it after HBM's own traits ({@code TooltipTrait}). */
    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[Rocket Grade]").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (isp > 0) info.add(Component.literal("Provides ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(BobMathUtil.getShortNumber(isp) + " ISP ").withStyle(ChatFormatting.RED))
                .append(Component.literal("per bucket").withStyle(ChatFormatting.YELLOW)));
        info.add(Component.literal("[Thrust power]").withStyle(ChatFormatting.RED));
        if (thrust > 0) info.add(Component.literal("Provides ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(BobMathUtil.getShortNumber(thrust) + " N ").withStyle(ChatFormatting.RED))
                .append(Component.literal("of thrust per bucket").withStyle(ChatFormatting.YELLOW)));
    }

    static FT_Rocket read(JsonObject json) {
        return new FT_Rocket(json.get("isp").getAsInt(), json.get("thrust").getAsLong());
    }

    static JsonObject write(Object trait) {
        FT_Rocket rocket = (FT_Rocket) trait;
        JsonObject json = new JsonObject();
        json.addProperty("isp", rocket.isp);
        json.addProperty("thrust", rocket.thrust);
        return json;
    }
}
