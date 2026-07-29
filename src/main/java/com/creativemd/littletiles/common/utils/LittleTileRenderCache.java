package com.creativemd.littletiles.common.utils;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Triangle3d;

/**
 * Client render geometry retained for a little tile.
 */
public class LittleTileRenderCache {

    private final LittleTile tile;
    private volatile Mesh3d simpleMesh;

    private volatile List<Triangle3d> visibleCutoutTriangles;
    private volatile List<Triangle3d> visibleBoxTriangles;

    /** Bit set of {@link ForgeDirection#ordinal()}: the sides drawn as triangles instead of as rectangles. */
    private int replacedBoxSides;

    public LittleTileRenderCache(LittleTile tile) {
        this.tile = tile;
    }

    public Mesh3d getSimpleMesh() {
        if (tile.getCutoutInfo() == null || tile.boundingBox == null) {
            return null;
        }
        if (simpleMesh == null) {
            simpleMesh = Mesh3dUtil.meshFromTile(tile.boundingBox, tile.getCutoutInfo());
        }
        return simpleMesh;
    }

    public boolean hasValidMesh() {
        Mesh3d mesh = getSimpleMesh();
        return mesh != null && !mesh.getTriangles().isEmpty();
    }

    /** The visible part of the cutout mesh from the last render, null when it has to be computed again. */
    public List<Triangle3d> getVisibleCutoutTriangles() {
        return visibleCutoutTriangles;
    }

    public void setVisibleCutoutTriangles(List<Triangle3d> triangles) {
        this.visibleCutoutTriangles = triangles;
    }

    /** The visible part of the box sides a mesh cuts into, null when it has to be computed again. */
    public List<Triangle3d> getVisibleBoxTriangles() {
        return visibleBoxTriangles;
    }

    public void setVisibleBoxTriangles(List<Triangle3d> triangles) {
        this.visibleBoxTriangles = triangles;
    }

    public int getReplacedBoxSides() {
        return replacedBoxSides;
    }

    public void addReplacedBoxSide(ForgeDirection side) {
        replacedBoxSides |= 1 << side.ordinal();
    }

    public void invalidateMesh() {
        simpleMesh = null;
        invalidateCuts();
    }

    public void invalidateCuts() {
        visibleCutoutTriangles = null;
        visibleBoxTriangles = null;
        replacedBoxSides = 0;
    }
}
