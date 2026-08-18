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
    private volatile BoxCullingResult boxCullingResult;

    public static final class BoxCullingResult {

        private final List<Triangle3d> triangles;
        /** Bit set of {@link ForgeDirection#ordinal()}: sides drawn as triangles instead of rectangles. */
        private final int replacedSides;

        public BoxCullingResult(List<Triangle3d> triangles, int replacedSides) {
            this.triangles = triangles;
            this.replacedSides = replacedSides;
        }

        public List<Triangle3d> getTriangles() {
            return triangles;
        }

        public int getReplacedSides() {
            return replacedSides;
        }
    }

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

    /** The visible box triangles and the rectangle sides they replace, or null when not yet computed. */
    public BoxCullingResult getBoxCullingResult() {
        return boxCullingResult;
    }

    public void setBoxCullingResult(BoxCullingResult result) {
        this.boxCullingResult = result;
    }

    public synchronized void invalidateMesh() {
        simpleMesh = null;
        invalidateCuts();
    }

    public void invalidateCuts() {
        visibleCutoutTriangles = null;
        boxCullingResult = null;
    }
}
