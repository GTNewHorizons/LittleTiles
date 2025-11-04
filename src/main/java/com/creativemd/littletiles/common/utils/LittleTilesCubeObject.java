package com.creativemd.littletiles.common.utils;

import com.creativemd.creativecore.common.utils.CubeObject;

public class LittleTilesCubeObject extends CubeObject {

    public LittleTilesCubeObject(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public LittleTileCutoutInfo cutoutInfo;
}
