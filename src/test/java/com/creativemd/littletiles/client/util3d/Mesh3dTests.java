package com.creativemd.littletiles.client.util3d;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joml.Vector3i;
import org.junit.BeforeClass;
import org.junit.Test;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/** Always-run regression tests for the meshes of tiles. */
public class Mesh3dTests {

    private static final LittleTileBox BLOCK = new LittleTileBox(0, 0, 0, 16, 16, 16);

    @BeforeClass
    public static void loadMeshes() {
        Mesh3dUtil.initializeMeshes();
    }

    private static LittleTileCutoutInfo cutout(LittleTileShapeMode shape, Vector3i size, Vector3i pos) {
        return cutout(shape, size, pos, 0);
    }

    private static LittleTileCutoutInfo cutout(LittleTileShapeMode shape, Vector3i size, Vector3i pos,
            int orientation) {
        LittleTileCutoutInfo cutout = new LittleTileCutoutInfo();
        cutout.type = shape;
        cutout.size = size;
        cutout.pos = pos;
        cutout.orientation = orientation;
        return cutout;
    }

    private static String describe(LittleTileCutoutInfo cutout) {
        return cutout.type + " size=" + cutout.size + " pos=" + cutout.pos + " orientation=" + cutout.orientation;
    }

    private static void assertEmpty(LittleTileCutoutInfo cutout) {
        Mesh3d mesh = Mesh3dUtil.meshFromTile(BLOCK, cutout);
        assertTrue(
                cutout.type + " size="
                        + cutout.size
                        + " pos="
                        + cutout.pos
                        + " triangles="
                        + mesh.getTriangles().size(),
                mesh.getTriangles().isEmpty());
    }

    // Tiles whose bounds only touch the box they are built for share no volume with it, so their mesh there is empty
    // rather than their face lying flat on the box

    /** Its end cap at x = max lies on the block's west face. */
    @Test
    public void slopeEndingWhereTheBoxStartsIsEmpty() {
        assertEmpty(cutout(LittleTileShapeMode.SLOPE, new Vector3i(5, 8, 8), new Vector3i(-5, 4, 4)));
    }

    /** Its bottom lies on the block's top face. */
    @Test
    public void roundSlopeStartingWhereTheBoxEndsIsEmpty() {
        assertEmpty(cutout(LittleTileShapeMode.SLOPE_CONCAVE, new Vector3i(8, 6, 8), new Vector3i(4, 16, 4)));
    }

    /** Its flat face at z = max lies on the block's north face. */
    @Test
    public void deformedBoxEndingWhereTheBoxStartsIsEmpty() {
        LittleTileCutoutInfo cutout = cutout(
                LittleTileShapeMode.DEFORMED_BOX,
                new Vector3i(8, 8, 7),
                new Vector3i(4, 4, -7));
        cutout.corners = new Vector3i[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
        for (int corner = 0; corner < cutout.corners.length; corner++) {
            cutout.corners[corner] = new Vector3i(
                    (corner & 1) != 0 ? 16 : 0,
                    (corner & 2) != 0 ? 16 : 0,
                    (corner & 4) != 0 ? 16 : 0);
        }
        // Dragged inwards on the far face, which keeps the bounds and leaves the face at z = max flat
        cutout.corners[3].set(12, 13, 0);
        assertEmpty(cutout);
    }

    // Cutting a tile down to its box leaves a closed mesh, also where the cuts pass close to its points

    private static void assertClosed(LittleTileBox box, LittleTileCutoutInfo cutout) {
        Mesh3d mesh = Mesh3dUtil.meshFromTile(box, cutout);
        assertFalse(describe(cutout) + " lost all volume", mesh.getTriangles().isEmpty());
        assertNull(describe(cutout), MeshGeometry.findOpenEdge(mesh));
    }

    @Test
    public void roundCornerCutToASmallBoxIsClosed() {
        assertClosed(
                new LittleTileBox(7, 0, 6, 14, 2, 11),
                cutout(
                        LittleTileShapeMode.SLOPE_CONVEX_OUTER_CORNER,
                        new Vector3i(5, 7, 19),
                        new Vector3i(-2, -5, -17),
                        27));
    }

    @Test
    public void largeRoundSlopeCutToTheBlockIsClosed() {
        assertClosed(
                BLOCK,
                cutout(LittleTileShapeMode.SLOPE_CONVEX, new Vector3i(37, 122, 154), new Vector3i(-33, -37, -66), 21));
    }

    @Test
    public void warpedDeformedBoxCutToTheBlockIsClosed() {
        LittleTileCutoutInfo cutout = cutout(
                LittleTileShapeMode.DEFORMED_BOX,
                new Vector3i(26, 18, 19),
                new Vector3i(-5, -1, -1),
                4);
        cutout.corners = new Vector3i[] { new Vector3i(2, 1, 0), new Vector3i(15, 1, -2), new Vector3i(-2, 17, -1),
                new Vector3i(15, 14, -1), new Vector3i(-1, 1, 17), new Vector3i(17, 2, 17), new Vector3i(1, 16, 18),
                new Vector3i(18, 18, 17) };
        assertClosed(BLOCK, cutout);
    }
}
