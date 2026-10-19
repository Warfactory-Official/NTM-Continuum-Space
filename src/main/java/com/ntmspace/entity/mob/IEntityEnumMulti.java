package com.ntmspace.entity.mob;

/**
 * hbm-space {@code IEntityEnumMulti}: a mob drawn with one of several skins, one per constant of its enum
 * ({@code textures/entity/<enum>_<constant>.png}, see the client's {@code RenderObjMob}).
 */
public interface IEntityEnumMulti {

    Enum<?> getEnum();
}
