package com.creativemd.littletiles.common.utils;

import net.minecraft.util.StatCollector;

public enum LittleTilePlaceMode {

    NORMAL("key.littletiles.mode_normal"),
    FILL("key.littletiles.mode_fill"),
    OVERWRITE("key.littletiles.mode_overwrite"),
    STENCIL("key.littletiles.mode_stencil"),
    /**
     * Like {@link #FILL}, but where the selection runs into an existing shape it fills against that shape with its anti
     * mesh, instead of leaving the shape's whole box empty.
     */
    ANTI_FILL("key.littletiles.mode_anti_fill");

    private final String name;

    LittleTilePlaceMode(String name) {
        this.name = name;
    }

    public String getName() {
        return StatCollector.translateToLocal(name);
    }

    public String getInfo() {
        return StatCollector.translateToLocal(name + "_info");
    }

    public static LittleTilePlaceMode fromOrdinal(int ordinal) {
        LittleTilePlaceMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return NORMAL;
        }
        return values[ordinal];
    }
}
