package com.creativemd.littletiles.common.utils;

import java.util.List;

import org.joml.Vector3i;

import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Shared math for the TRIANGLE shape's 4-click flow, used by both the item placement code and the live preview so they
 * always agree on where the tile actually needs to be rooted.
 */
public final class TriangleClickHelper {

    private TriangleClickHelper() {}

    public static final class Vertices {

        public final Vector3i v1 = new Vector3i();
        public final Vector3i v2;
        public final Vector3i v3;
        public final Vector3i v4;
        public final LittleTileBox box;

        private Vertices(Vector3i v2, Vector3i v3, Vector3i v4) {
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.box = LittleTileCutoutInfo.boxFromTriangleVertices(v1, v2, v3, v4);
        }
    }

    public static LittleTileBlockPos anchor(LittleTileBlockPos current) {
        List<LittleTileBlockPos> hits = PreviewRenderer.triangleHits;
        return hits.isEmpty() ? current : hits.get(0);
    }

    /**
     * v4 is always the live/tentative point (the next click) - while placing vertex 2 that also makes v2 live, and
     * since v3 then synthesizes off v2, all of v2/v3/v4 track the cursor together as a flat right triangle. Once v2 is
     * real (placing vertex 3), v3 still synthesizes but is now fixed, so only the live v4 adds height, showing a
     * flat-bottomed shape. Once v3 is real too (placing vertex 4), all but the live v4 are fixed, completing the shape
     * as it is finalized.
     */
    public static Vertices computeVertices(LittleTileBlockPos current) {
        List<LittleTileBlockPos> hits = PreviewRenderer.triangleHits;
        LittleTileBlockPos anchor = anchor(current);

        Vector3i v2 = hits.size() >= 2 ? offset(hits.get(1), anchor) : offset(current, anchor);
        Vector3i v3 = hits.size() >= 3 ? offset(hits.get(2), anchor) : synthesizeRightAngleCorner(v2);
        Vector3i v4 = offset(current, anchor);
        return new Vertices(v2, v3, v4);
    }

    /**
     * The tile's own bounding box (and the cutout's local frame) starts at the MINIMUM of the 4 vertex offsets, not
     * necessarily at the anchor vertex itself (e.g. a 2nd vertex west of the anchor pulls the box's min corner west of
     * it too). Placing the tile at the raw anchor position instead of this shifted one mirrors anything on the negative
     * side of the anchor onto the positive side.
     */
    public static LittleTileBlockPos placementAnchor(LittleTileBlockPos current) {
        LittleTileBlockPos anchor = anchor(current);
        LittleTileBox box = computeVertices(current).box;

        LittleTileBlockPos shifted = new LittleTileBlockPos(
                anchor.getPosX(),
                anchor.getPosY(),
                anchor.getPosZ(),
                anchor.getSubX(),
                anchor.getSubY(),
                anchor.getSubZ(),
                anchor.getSide());
        shifted.moveSubX(box.minX);
        shifted.moveSubY(box.minY);
        shifted.moveSubZ(box.minZ);
        return shifted;
    }

    private static Vector3i offset(LittleTileBlockPos pos, LittleTileBlockPos anchor) {
        LittleTileBlockPos.Subtraction sub = pos.subtract(anchor);
        return new Vector3i(sub.x, sub.y, sub.z);
    }

    /**
     * Synthesizes the missing 3rd corner of a flat, axis-legged right triangle from just 2 vertices, for the 2-hit
     * placeholder preview. The axis with the smallest delta is treated as the (near-)constant face-normal axis.
     */
    private static Vector3i synthesizeRightAngleCorner(Vector3i v2) {
        int ax = Math.abs(v2.x), ay = Math.abs(v2.y), az = Math.abs(v2.z);
        if (ax <= ay && ax <= az) {
            return new Vector3i(v2.x, 0, v2.z);
        }
        return new Vector3i(0, v2.y, v2.z);
    }
}
