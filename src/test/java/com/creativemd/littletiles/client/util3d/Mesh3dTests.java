package com.creativemd.littletiles.client.util3d;

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
        LittleTileCutoutInfo cutout = new LittleTileCutoutInfo();
        cutout.type = shape;
        cutout.size = size;
        cutout.pos = pos;
        cutout.orientation = 0;
        return cutout;
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
}
