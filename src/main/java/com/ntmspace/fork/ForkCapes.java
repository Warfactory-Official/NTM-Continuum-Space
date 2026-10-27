package com.ntmspace.fork;

import dev.golem.core.Ids;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** The two cape choices added by hbm-space, shared by native registration and common metadata queries. */
public final class ForkCapes {
    public static final String DUODEC_ = "85d54b8c-1862-4c86-b351-5ef1b998aa32";
    public static final String MELLOW_RPG8 = "99b9efa5-8c38-4b22-86c2-baf97789d2fb";

    private static final Identifier GWEN = Ids.of("ntmspace", "textures/models/capes/capegwen.png");
    private static final Identifier MELLOW = Ids.of("ntmspace", "textures/models/capes/capemellow.png");

    private ForkCapes() {}

    public static @Nullable Identifier texture(UUID id, String name) {
        var uuid = id.toString();
        if (uuid.equals(DUODEC_)) return GWEN;
        if (uuid.equals(MELLOW_RPG8)) return MELLOW;
        return null;
    }
}
