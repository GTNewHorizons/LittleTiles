package com.creativemd.littletiles.common.utils;

import java.util.Collections;
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

    /**
     * A culling result and the box sides it replaces, kept together so one reference read always observes a coherent
     * pair. One result serves a tile's cutout culling or its box culling, never both: a tile renders as exactly one
     * cube, so only one of the two paths can ever populate this cache.
     */
    public static final class CullingResult {

        private final List<Triangle3d> triangles;
        /** Bit set of {@link ForgeDirection#ordinal()}: sides drawn as triangles instead of rectangles. */
        private final int replacedSides;

        public CullingResult(List<Triangle3d> triangles, int replacedSides) {
            // One instance is shared by every thread that renders this tile, so the list must not stay writable.
            // Callers hand over a freshly built list and drop it, which is why wrapping is enough and no copy is made.
            this.triangles = Collections.unmodifiableList(triangles);
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
        // Unlike culling, mesh creation reads no other tile caches, so it can stay under this monitor. Invalidation
        // then runs either before calculation or after publication and no separate mesh generation is needed.
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
