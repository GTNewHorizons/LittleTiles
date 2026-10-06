package com.creativemd.littletiles.client.util3d;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joml.Vector3i;
import org.junit.BeforeClass;
import org.junit.Test;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
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
        assertEmpty(BLOCK, cutout);
    }

    private static void assertEmpty(LittleTileBox box, LittleTileCutoutInfo cutout) {
        Mesh3d mesh = Mesh3dUtil.meshFromTile(box, cutout);
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

    // Tiles reaching into their box by less than the rounding grid are rounded flat onto its planes, and those reaching
    // in by far less than anything visible leave a sliver: neither is a tile there

    /** Its slope face only touches the box along an edge, which is left as triangles without area. */
    @Test
    public void slopeTouchingTheBoxAlongAnEdgeIsEmpty() {
        assertEmpty(
                new LittleTileBox(1, 9, 2, 2, 13, 5),
                cutout(LittleTileShapeMode.SLOPE, new Vector3i(32, 14, 16), new Vector3i(-10, -9, -2), 35));
    }

    /** Rounded flat onto the box's planes, with faces that still have area. */
    @Test
    public void roundCornerBarelyReachingIntoTheBoxIsEmpty() {
        assertEmpty(
                new LittleTileBox(9, 2, 2, 12, 5, 8),
                cutout(
                        LittleTileShapeMode.SLOPE_CONVEX_OUTER_CORNER,
                        new Vector3i(11, 178, 37),
                        new Vector3i(-2, -78, 0),
                        38));
    }

    /** Leaves a sliver of less than a millionth of a cubic pixel in the box. */
    @Test
    public void triangleSlopeLeavingASliverInTheBoxIsEmpty() {
        assertEmpty(
                new LittleTileBox(11, 12, 12, 14, 14, 14),
                cutout(LittleTileShapeMode.SLOPE_TRIANGLE, new Vector3i(31, 37, 19), new Vector3i(-1, -28, -13), 21));
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

    // Tiles that only touch can be placed next to each other

    private static void assertFits(LittleTileBox existingBox, LittleTileCutoutInfo existing, LittleTileBox candidateBox,
            LittleTileCutoutInfo candidate) {
        LittleTileBlock tile = new LittleTileBlock();
        tile.boundingBox = existingBox.copy();
        tile.setCutoutInfo(existing);
        TileEntityLittleTiles tiles = new TileEntityLittleTiles();
        tiles.getTiles().add(tile);
        assertTrue(
                describe(existing) + " " + (candidate == null ? candidateBox : describe(candidate)),
                tiles.isSpaceForLittleTile(candidateBox.copy(), candidate));
    }

    private static void assertDoesntFit(LittleTileBox existingBox, LittleTileCutoutInfo existing,
            LittleTileBox candidateBox, LittleTileCutoutInfo candidate) {
        LittleTileBlock tile = new LittleTileBlock();
        tile.boundingBox = existingBox.copy();
        tile.setCutoutInfo(existing);
        TileEntityLittleTiles tiles = new TileEntityLittleTiles();
        tiles.getTiles().add(tile);
        assertFalse(
                describe(existing) + " " + (candidate == null ? candidateBox : describe(candidate)),
                tiles.isSpaceForLittleTile(candidateBox.copy(), candidate));
    }

    /** Its mesh doesn't close when cut, which must not let a copy of it in. */
    @Test
    public void deformedBoxWithItsCornerDraggedInDoesntFitOntoItself() {
        LittleTileCutoutInfo cutout = cutout(
                LittleTileShapeMode.DEFORMED_BOX,
                new Vector3i(16, 16, 16),
                new Vector3i(0, 0, 0));
        cutout.corners = new Vector3i[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
        for (int corner = 0; corner < cutout.corners.length; corner++) {
            cutout.corners[corner] = new Vector3i(
                    (corner & 1) != 0 ? 16 : 0,
                    (corner & 2) != 0 ? 16 : 0,
                    (corner & 4) != 0 ? 16 : 0);
        }
        cutout.corners[0].set(13, 13, 13);
        assertDoesntFit(BLOCK, cutout, BLOCK, cutout);
    }

    @Test
    public void largeComplementarySlopesFit() {
        Vector3i size = new Vector3i(781, 156, 78), pos = new Vector3i(-154, -112, -5);
        assertFits(
                BLOCK,
                cutout(LittleTileShapeMode.SLOPE, size, pos, 10),
                BLOCK,
                cutout(LittleTileShapeMode.SLOPE, size, pos, 0));
    }

    /** The slope face of the small one lies on that of the big one. */
    @Test
    public void differentlySizedCoplanarSlopesFit() {
        assertFits(
                BLOCK,
                cutout(LittleTileShapeMode.SLOPE, new Vector3i(16, 10, 12), new Vector3i(2, 9, 0), 27),
                BLOCK,
                cutout(LittleTileShapeMode.SLOPE, new Vector3i(8, 10, 6), new Vector3i(2, 9, 0), 35));
    }

    @Test
    public void onePixelBarNextToASlopeFits() {
        assertFits(
                BLOCK,
                cutout(LittleTileShapeMode.SLOPE, new Vector3i(6, 18, 30), new Vector3i(10, 0, 10), 43),
                new LittleTileBox(12, 8, 0, 13, 9, 16),
                null);
    }

    // Deformed boxes sharing a face can be placed next to each other

    private static void assertNeighboursFit(Vector3i[] low, Vector3i[] high) {
        LittleTileBox lowBox = LittleTileBox.fromPoints(low), highBox = LittleTileBox.fromPoints(high);
        assertFits(
                lowBox,
                LittleTileCutoutInfo.fromDeformedCorners(lowBox, low),
                highBox,
                LittleTileCutoutInfo.fromDeformedCorners(highBox, high));
    }

    @Test
    public void deformedNeighboursAlongZFit() {
        assertNeighboursFit(
                new Vector3i[] { new Vector3i(1, 0, 1), new Vector3i(15, 2, 1), new Vector3i(1, 14, 0),
                        new Vector3i(15, 14, 2), new Vector3i(2, 2, 14), new Vector3i(14, 0, 13),
                        new Vector3i(1, 14, 12), new Vector3i(16, 16, 14) },
                new Vector3i[] { new Vector3i(2, 2, 14), new Vector3i(14, 0, 13), new Vector3i(1, 14, 12),
                        new Vector3i(16, 16, 14), new Vector3i(1, 1, 16), new Vector3i(15, 0, 15),
                        new Vector3i(0, 15, 15), new Vector3i(15, 16, 14) });
    }

    @Test
    public void deformedNeighboursSharingASlantedFaceAlongZFit() {
        assertNeighboursFit(
                new Vector3i[] { new Vector3i(0, 0, 1), new Vector3i(14, 1, 1), new Vector3i(1, 15, 0),
                        new Vector3i(15, 15, 0), new Vector3i(1, 1, 10), new Vector3i(16, 0, 13),
                        new Vector3i(1, 16, 14), new Vector3i(14, 16, 12) },
                new Vector3i[] { new Vector3i(1, 1, 10), new Vector3i(16, 0, 13), new Vector3i(1, 16, 14),
                        new Vector3i(14, 16, 12), new Vector3i(0, 1, 15), new Vector3i(14, 2, 15),
                        new Vector3i(2, 16, 14), new Vector3i(15, 16, 14) });
    }

    @Test
    public void deformedNeighboursAlongYFit() {
        assertNeighboursFit(
                new Vector3i[] { new Vector3i(0, 2, 2), new Vector3i(16, 2, 2), new Vector3i(0, 10, 2),
                        new Vector3i(16, 14, 1), new Vector3i(2, 1, 15), new Vector3i(14, 1, 14),
                        new Vector3i(2, 11, 16), new Vector3i(15, 12, 14) },
                new Vector3i[] { new Vector3i(0, 10, 2), new Vector3i(16, 14, 1), new Vector3i(1, 14, 2),
                        new Vector3i(15, 14, 0), new Vector3i(2, 11, 16), new Vector3i(15, 12, 14),
                        new Vector3i(1, 15, 15), new Vector3i(14, 16, 15) });
    }

    /**
     * Their shared face is warped, and its diagonals are close in length: the low box is 6 high and the high one 14, so
     * measured in each box's own unit cube they would split it along different diagonals and overlap between them.
     */
    @Test
    public void deformedNeighboursOfDifferentHeightsSplitTheirSharedFaceAlike() {
        assertNeighboursFit(
                new Vector3i[] { new Vector3i(0, 0, 0), new Vector3i(16, 0, 0), new Vector3i(0, 16, 0),
                        new Vector3i(16, 16, 0), new Vector3i(0, 0, 6), new Vector3i(16, 0, 6), new Vector3i(0, 15, 2),
                        new Vector3i(16, 16, 5) },
                new Vector3i[] { new Vector3i(0, 0, 6), new Vector3i(16, 0, 6), new Vector3i(0, 15, 2),
                        new Vector3i(16, 16, 5), new Vector3i(0, 0, 16), new Vector3i(16, 0, 16),
                        new Vector3i(0, 16, 16), new Vector3i(16, 16, 16) });
    }
}
