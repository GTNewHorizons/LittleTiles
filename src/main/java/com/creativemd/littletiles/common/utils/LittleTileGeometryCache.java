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

    private CullingResult cullingResult;
    private long cutsGeneration;

    public static final class CullingResult {

        private final List<Triangle3d> triangles;
        /** Bit set of {@link ForgeDirection#ordinal()}: sides drawn as triangles instead of rectangles. */
        private final int replacedSides;

        public CullingResult(List<Triangle3d> triangles, int replacedSides) {
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

    /** The visible triangles and any ordinary box sides they replace, or null when not yet computed. */
    public synchronized CullingResult getCullingResult() {
        return cullingResult;
    }

    public CullingResult getOrCreateCullingResult(Supplier<CullingResult> calculation) {
        long generation;
        synchronized (this) {
            if (cullingResult != null) {
                return cullingResult;
            }
            generation = cutsGeneration;
        }

        // Culling reads other tile caches, so calculate outside this monitor to avoid cross-tile deadlocks. The
        // generation check prevents an invalidated calculation from being published afterward.
        CullingResult calculated = calculation.get();
        synchronized (this) {
            if (cutsGeneration == generation) {
                if (cullingResult == null) {
                    cullingResult = calculated;
                }
                return cullingResult;
            }
        }
        return calculated;
    }

    public synchronized void invalidateMesh() {
        simpleMesh = null;
        invalidateCuts();
    }

    public synchronized void invalidateCuts() {
        cutsGeneration++;
        cullingResult = null;
    }
}
