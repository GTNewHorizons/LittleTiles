package com.creativemd.littletiles.common.utils;

import net.minecraft.util.StatCollector;

public enum LittleTileShapeMode {

    BOX("key.littletiles.box"),
    SLOPE("key.littletiles.slope");

    private final String name;

    LittleTileShapeMode(String name) {
        this.name = name;
    }

    public String getName() {
        return StatCollector.translateToLocal(name);
    }
}
