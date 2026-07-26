package com.creativemd.littletiles.common.utils;

import com.creativemd.creativecore.common.utils.CubeObject;

public class LittleTilesCubeObject extends CubeObject {

    public LittleTileCutoutInfo cutoutInfo;

    /**
     * Bounds on the 1/16 grid. Kept alongside the double bounds so face occlusion can be computed with exact integer
     * math.
     */
    public int gridMinX, gridMinY, gridMinZ, gridMaxX, gridMaxY, gridMaxZ;

    public LittleTilesCubeObject(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(minX / 16D, minY / 16D, minZ / 16D, maxX / 16D, maxY / 16D, maxZ / 16D);
        this.gridMinX = minX;
        this.gridMinY = minY;
        this.gridMinZ = minZ;
        this.gridMaxX = maxX;
        this.gridMaxY = maxY;
        this.gridMaxZ = maxZ;
    }
}
