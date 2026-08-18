package com.creativemd.littletiles.common.utils;

import java.util.List;
import java.util.function.Supplier;

import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Client render geometry retained for a little tile.
 */
public class LittleTileGeometryCache {

    private final Supplier<LittleTileBox> boxGetter;
    private final Supplier<LittleTileCutoutInfo> cutoutGetter;
    private Mesh3d simpleMesh;

    private volatile List<Triangle3d> visibleCutoutTriangles;
    private volatile List<Triangle3d> visibleBoxTriangles;

    /** Bit set of {@link ForgeDirection#ordinal()}: the sides drawn as triangles instead of as rectangles. */
    private int replacedBoxSides;

    public LittleTileGeometryCache(Supplier<LittleTileBox> boxGetter, Supplier<LittleTileCutoutInfo> cutoutGetter) {
        this.boxGetter = boxGetter;
        this.cutoutGetter = cutoutGetter;
    }

    /** Returns the cached mesh, calculating and retaining it when necessary. */
    public synchronized Mesh3d getOrCreateSimpleMesh() {
        LittleTileBox box = boxGetter.get();
        LittleTileCutoutInfo cutoutInfo = cutoutGetter.get();
        if (cutoutInfo == null || box == null) {
            return null;
        }
        if (simpleMesh == null) {
            simpleMesh = Mesh3dUtil.meshFromTile(box, cutoutInfo);
        }
        return simpleMesh;
    }

    public boolean hasValidMesh() {
        Mesh3d mesh = getOrCreateSimpleMesh();
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

    public synchronized void invalidateMesh() {
        simpleMesh = null;
        invalidateCuts();
    }

    public void invalidateCuts() {
        visibleCutoutTriangles = null;
        visibleBoxTriangles = null;
        replacedBoxSides = 0;
    }
}
