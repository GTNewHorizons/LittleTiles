package com.creativemd.littletiles.common.utils;

import net.minecraft.util.StatCollector;

public enum LittleTileShapeMode {

    BOX("key.littletiles.box"),
    SLOPE("key.littletiles.slope"),
    PILLAR("key.littletiles.pillar"),
    SLOPE_CONCAVE("key.littletiles.slope_concave"),
    SLOPE_CONVEX("key.littletiles.slope_convex"),
    SLOPE_TRIANGLE("key.littletiles.slope_triangle"),
    SLOPE_TRIANGLE_CORNER("key.littletiles.slope_triangle_corner"),
    SLOPE_OUTER_CORNER("key.littletiles.slope_outer_corner"),
    SLOPE_INNER_CORNER("key.littletiles.slope_inner_corner");

    private final String name;

    LittleTileShapeMode(String name) {
        this.name = name;
    }

    public String getName() {
        return StatCollector.translateToLocal(name);
    }
}
