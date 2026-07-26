package com.creativemd.littletiles.common.utils;

import com.creativemd.creativecore.common.utils.CubeObject;

public class LittleTilesCubeObject extends CubeObject {

    public LittleTilesCubeObject(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public LittleTileCutoutInfo cutoutInfo;

    /** Bit per vanilla side (== ForgeDirection.ordinal()); set faces are not rendered. */
    public int hiddenSides;

    /**
     * Bounds on the 1/16 grid. Kept alongside the double bounds so face occlusion can be
     * computed with exact integer math.
     */
    public int gridMinX, gridMinY, gridMinZ, gridMaxX, gridMaxY, gridMaxZ;

    public void setGridBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.gridMinX = minX;
        this.gridMinY = minY;
        this.gridMinZ = minZ;
        this.gridMaxX = maxX;
        this.gridMaxY = maxY;
        this.gridMaxZ = maxZ;
    }
}
