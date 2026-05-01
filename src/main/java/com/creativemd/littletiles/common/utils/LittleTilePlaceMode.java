package com.creativemd.littletiles.common.utils;

import net.minecraft.util.StatCollector;

public enum LittleTilePlaceMode {

    NORMAL("key.littletiles.mode_normal"),
    FILL("key.littletiles.mode_fill"),
    OVERWRITE("key.littletiles.mode_overwrite"),
    STENCIL("key.littletiles.mode_stencil");

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
