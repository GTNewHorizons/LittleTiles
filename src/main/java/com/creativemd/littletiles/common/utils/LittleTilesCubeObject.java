package com.creativemd.littletiles.common.utils;

import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisX;
import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisY;

import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;

public class LittleTilesCubeObject extends CubeObject {

    public LittleTileCutoutInfo cutoutInfo;
    public LittleTileGeometryCache geometryCache;

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

    public int gridMin(Axis axis) {
        return axis == AxisX ? gridMinX : (axis == AxisY ? gridMinY : gridMinZ);
    }

    public int gridMax(Axis axis) {
        return axis == AxisX ? gridMaxX : (axis == AxisY ? gridMaxY : gridMaxZ);
    }
}
