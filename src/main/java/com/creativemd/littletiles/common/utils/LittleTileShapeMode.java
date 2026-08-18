package com.creativemd.littletiles.common.utils;

import net.minecraft.util.StatCollector;

public enum LittleTileShapeMode {

    // Only add new enums at the end, otherwise existing shapes will break.
    BOX("key.littletiles.box"),
    SLOPE("key.littletiles.slope"),
    PILLAR("key.littletiles.pillar"),
    SLOPE_CONCAVE("key.littletiles.slope_concave"),
    SLOPE_CONVEX("key.littletiles.slope_convex"),
    SLOPE_TRIANGLE("key.littletiles.slope_triangle"),
    SLOPE_TRIANGLE_CORNER("key.littletiles.slope_triangle_corner"),
    SLOPE_OUTER_CORNER("key.littletiles.slope_outer_corner"),
    SLOPE_INNER_CORNER("key.littletiles.slope_inner_corner"),
    SLOPE_TRIANGLE_ALT("key.littletiles.slope_triangle_alt"),
    SLOPE_CONVEX_INNER_CORNER("key.littletiles.slope_convex_inner_corner"),
    SLOPE_CONVEX_OUTER_CORNER("key.littletiles.slope_convex_outer_corner"),
    DEFORMED_BOX("key.littletiles.deformed_box");

    private final String name;

    LittleTileShapeMode(String name) {
        this.name = name;
    }

    public String getName() {
        return StatCollector.translateToLocal(name);
    }
}
