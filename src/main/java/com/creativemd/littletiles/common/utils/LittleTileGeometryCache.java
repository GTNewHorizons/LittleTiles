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
    private volatile long meshGeneration;

    private CullingResult cullingResult;
    /**
     * Volatile so capturing it costs no lock. Every write and every decision made on it still happens under this
     * object's monitor; the plain read only ever hands out a snapshot that gets re-checked there before anything is
     * retained.
     */
    private volatile long cutsGeneration;

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

    /**
     * Returns the cached mesh, calculating and retaining it when necessary, or null when the tile currently has no box
     * or no cutout to build one from.
     * <p>
     * Resolve it once and work with what you get back. There is deliberately no "does it have a mesh" query: the answer
     * can stop being true before the caller acts on it, and a second call is not guaranteed to return what the first
     * one did.
     */
    public Mesh3d getOrCreateSimpleMesh() {
        // Captured before the inputs are read, so an invalidation racing this calculation is never undone by it.
        long generation = meshGeneration;

        LittleTileBox box = boxGetter.get();
        LittleTileCutoutInfo cutoutInfo = cutoutGetter.get();
        if (cutoutInfo == null || box == null) {
            return null;
        }

        synchronized (this) {
            if (meshGeneration == generation && simpleMesh != null) {
                return simpleMesh;
            }
        }

        // Triangulating outside the monitor is what keeps the client thread off it. Invalidation runs there for every
        // tile of a block and its six neighbours on every block update, and blocking it behind a chunk worker's
        // triangulation stalls the client while it holds the tile list lock the other workers need. The cost is that
        // two workers arriving together both calculate; the generation check below still publishes only one result.
        Mesh3d calculated = Mesh3dUtil.meshFromTile(box, cutoutInfo);

        synchronized (this) {
            if (meshGeneration == generation) {
                if (simpleMesh == null) {
                    simpleMesh = calculated;
                }
                return simpleMesh;
            }
        }
        return calculated;
    }

    /**
     * The visible triangles and any ordinary box sides they replace, calculating and retaining them when necessary.
     */
    public CullingResult getOrCreateCullingResult(long snapshotGeneration, Supplier<CullingResult> calculation) {
        synchronized (this) {
            if (cutsGeneration == snapshotGeneration && cullingResult != null) {
                return cullingResult;
            }
        }

        // Culling reads other tile caches, so calculate outside this monitor to avoid cross-tile deadlocks. The
        // generation captured with the cube prevents an old render snapshot from being published afterward.
        CullingResult calculated = calculation.get();
        synchronized (this) {
            if (cutsGeneration == snapshotGeneration) {
                if (cullingResult == null) {
                    cullingResult = calculated;
                }
                return cullingResult;
            }
        }
        return calculated;
    }

    /**
     * Captures the cut generation without taking the cache monitor. World rendering captures all generations under
     * the tile entity's list monitor along with membership, before reading any cube or occluder geometry.
     */
    public long captureCutsGeneration() {
        return cutsGeneration;
    }

    public synchronized void invalidateMesh() {
        meshGeneration++;
        simpleMesh = null;
        invalidateCuts();
    }

    public synchronized void invalidateCuts() {
        cutsGeneration++;
        cullingResult = null;
    }
}
