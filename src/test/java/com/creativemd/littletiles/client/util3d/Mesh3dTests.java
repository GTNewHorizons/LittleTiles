package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.BLOCK;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.EXACT_VOLUME;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.ROUNDING_VOLUME;
import static com.creativemd.littletiles.client.util3d.Shapes.describe;
import static com.creativemd.littletiles.client.util3d.Shapes.shape;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Vector3i;
import org.junit.BeforeClass;
import org.junit.Test;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.MeshGeometry.FaceFilter;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

/** Always-run regression tests for the meshes of tiles. */
public class Mesh3dTests {

    /** At most 0.001 square blocks may remain visible, measured in square pixels. */
    private static final double MAJOR_CULLING_REMAINDER = 16 * 16 / 1000.0;

    @BeforeClass
    public static void loadMeshes() {
        Mesh3dUtil.initializeMeshes();
    }

    private static void assertEmpty(LittleTileCutoutInfo cutout) {
        assertEmpty(BLOCK, cutout);
    }

    private static void assertEmpty(LittleTileBox box, LittleTileCutoutInfo cutout) {
        Mesh3d mesh = Mesh3dUtil.meshFromTile(box, cutout);
        assertTrue(describe(cutout) + " triangles=" + mesh.getTriangles().size(), mesh.getTriangles().isEmpty());
    }

    // Tiles whose bounds only touch the box they are built for share no volume with it, so their mesh there is empty
    // rather than their face lying flat on the box

    /** Its end cap at x = max lies on the block's west face. */
    @Test
    public void slopeEndingWhereTheBoxStartsIsEmpty() {
        assertEmpty(shape(LittleTileShapeMode.SLOPE, new Vector3i(5, 8, 8), new Vector3i(-5, 4, 4), 0));
    }

    /** Its bottom lies on the block's top face. */
    @Test
    public void roundSlopeStartingWhereTheBoxEndsIsEmpty() {
        assertEmpty(shape(LittleTileShapeMode.SLOPE_CONCAVE, new Vector3i(8, 6, 8), new Vector3i(4, 16, 4), 0));
    }

    /** Its flat face at z = max lies on the block's north face. */
    @Test
    public void deformedBoxEndingWhereTheBoxStartsIsEmpty() {
        LittleTileCutoutInfo cutout = shape(
                LittleTileShapeMode.DEFORMED_BOX,
                new Vector3i(8, 8, 7),
                new Vector3i(4, 4, -7),
                0);
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
                shape(LittleTileShapeMode.SLOPE, new Vector3i(32, 14, 16), new Vector3i(-10, -9, -2), 35));
    }

    /** Rounded flat onto the box's planes, with faces that still have area. */
    @Test
    public void roundCornerBarelyReachingIntoTheBoxIsEmpty() {
        assertEmpty(
                new LittleTileBox(9, 2, 2, 12, 5, 8),
                shape(
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
                shape(LittleTileShapeMode.SLOPE_TRIANGLE, new Vector3i(31, 37, 19), new Vector3i(-1, -28, -13), 21));
    }

    // Cutting a tile down to its box leaves a closed mesh, also where the cuts pass close to its points

    private static void assertClosed(LittleTileBox box, LittleTileCutoutInfo cutout) {
        Mesh3d mesh = Mesh3dUtil.meshFromTile(box, cutout);
        assertFalse(describe(cutout) + " lost all volume", mesh.getTriangles().isEmpty());
        assertNull(String.valueOf(describe(cutout)), MeshGeometry.findOpenEdge(mesh));
    }

    @Test
    public void roundCornerCutToASmallBoxIsClosed() {
        assertClosed(
                new LittleTileBox(7, 0, 6, 14, 2, 11),
                shape(
                        LittleTileShapeMode.SLOPE_CONVEX_OUTER_CORNER,
                        new Vector3i(5, 7, 19),
                        new Vector3i(-2, -5, -17),
                        27));
    }

    @Test
    public void largeRoundSlopeCutToTheBlockIsClosed() {
        assertClosed(
                BLOCK,
                shape(LittleTileShapeMode.SLOPE_CONVEX, new Vector3i(37, 122, 154), new Vector3i(-33, -37, -66), 21));
    }

    @Test
    public void warpedDeformedBoxCutToTheBlockIsClosed() {
        LittleTileCutoutInfo cutout = shape(
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
        LittleTileCutoutInfo cutout = shape(
                LittleTileShapeMode.DEFORMED_BOX,
                new Vector3i(16, 16, 16),
                new Vector3i(0, 0, 0),
                0);
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
                shape(LittleTileShapeMode.SLOPE, size, pos, 10),
                BLOCK,
                shape(LittleTileShapeMode.SLOPE, size, pos, 0));
    }

    /** The slope face of the small one lies on that of the big one. */
    @Test
    public void differentlySizedCoplanarSlopesFit() {
        assertFits(
                BLOCK,
                shape(LittleTileShapeMode.SLOPE, new Vector3i(16, 10, 12), new Vector3i(2, 9, 0), 27),
                BLOCK,
                shape(LittleTileShapeMode.SLOPE, new Vector3i(8, 10, 6), new Vector3i(2, 9, 0), 35));
    }

    @Test
    public void onePixelBarNextToASlopeFits() {
        assertFits(
                BLOCK,
                shape(LittleTileShapeMode.SLOPE, new Vector3i(6, 18, 30), new Vector3i(10, 0, 10), 43),
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

    /**
     * A wedge whose upper north corners sit halfway up its north edges, so its west and east faces are triangles with a
     * corner in the middle of a side. Split along their shorter diagonals, each leaves a triangle of 3 distinct points
     * in a line, which has no normal to pick a texture direction from.
     */
    @Test
    public void deformedBoxWithACornerInTheMiddleOfAnEdgeHasTextureDirections() {
        Vector3i[] corners = { new Vector3i(0, 0, 0), new Vector3i(16, 0, 0), new Vector3i(0, 8, 0),
                new Vector3i(16, 8, 0), new Vector3i(0, 0, 16), new Vector3i(16, 0, 16), new Vector3i(0, 16, 0),
                new Vector3i(16, 16, 0) };
        assertTrue(Mesh3dUtil.enclosesVolume(corners));
        LittleTileBox box = LittleTileBox.fromPoints(corners);
        Mesh3d mesh = Mesh3dUtil.meshFromTile(box, LittleTileCutoutInfo.fromDeformedCorners(box, corners));
        for (Triangle3d triangle : mesh.getTriangles()) {
            assertNotNull(
                    triangle.getP1() + " " + triangle.getP2() + " " + triangle.getP3(),
                    triangle.getFaceDirection());
        }
    }

    // A face covered by that of another tile is culled, up to slivers

    /** How much of the tilted faces of the first stays visible next to the second. */
    private static double uncoveredTiltedArea(LittleTileCutoutInfo first, LittleTileCutoutInfo second) {
        return MeshGeometry.area(
                MeshGeometry.visiblePart(
                        FaceFilter.TILTED.of(Mesh3dUtil.meshFromTile(BLOCK, first)),
                        Mesh3dUtil.meshFromTile(BLOCK, second)));
    }

    private static void assertCovers(LittleTileCutoutInfo covered, LittleTileCutoutInfo covering) {
        double remainder = uncoveredTiltedArea(covered, covering);
        assertTrue(
                describe(covered) + " " + describe(covering) + " remainder=" + remainder,
                remainder <= MAJOR_CULLING_REMAINDER);
    }

    @Test
    public void complementarySlopeFacesCullEachOther() {
        Vector3i size = new Vector3i(16, 5, 26), pos = new Vector3i(0, 15, -7);
        LittleTileCutoutInfo first = shape(LittleTileShapeMode.SLOPE, size, pos, 15);
        LittleTileCutoutInfo second = shape(LittleTileShapeMode.SLOPE, size, pos, 35);
        assertCovers(first, second);
        assertCovers(second, first);
    }

    @Test
    public void complementaryRoundSlopeFacesCullEachOther() {
        Vector3i size = new Vector3i(13, 31, 20), pos = new Vector3i(-2, -15, -5);
        LittleTileCutoutInfo concave = shape(LittleTileShapeMode.SLOPE_CONCAVE, size, pos, 39);
        LittleTileCutoutInfo convex = shape(LittleTileShapeMode.SLOPE_CONVEX, size, pos, 31);
        assertCovers(concave, convex);
        assertCovers(convex, concave);
    }

    /** The slope face of the small one lies on that of the big one. */
    @Test
    public void smallerCoplanarSlopeFaceIsCulled() {
        assertCovers(
                shape(LittleTileShapeMode.SLOPE, new Vector3i(7, 1, 12), new Vector3i(2, 15, -2), 45),
                shape(LittleTileShapeMode.SLOPE, new Vector3i(14, 2, 12), new Vector3i(-5, 14, -2), 41));
    }

    /**
     * A sliver, 1.5e-5 blocks thin, lies against the west edge of a face turned towards it, sharing no area with it.
     * The face's other edges, cut along before the west one, run across it and leave pieces thinner than the split
     * tolerance.
     */
    @Test
    public void sliverOnlyTouchingAFaceStaysWhole() {
        double thickness = 1.5E-5;
        Triangle3d sliver = new Triangle3d(
                new Vector3d(0, -0.5, 0),
                new Vector3d(0, 1.5, 0),
                new Vector3d(-thickness, 0.5, 0));
        Triangle3d facingIt = new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 0));
        Mesh3d sliverMesh = new Mesh3d(Collections.singletonList(sliver));
        double area = MeshGeometry.area(sliverMesh);
        double visible = MeshGeometry
                .area(MeshGeometry.visiblePart(sliverMesh, new Mesh3d(Collections.singletonList(facingIt))));
        assertTrue("area=" + area + " visible=" + visible, Math.abs(visible - area) <= area * 1.0E-6);
    }

    /**
     * Only a face turned towards another hides it. One in the same plane facing the same way is the surface of a tile
     * right beside it, such as a sliver lying along it, and hides none of it where the two overlap.
     */
    @Test
    public void faceFacingTheSameWayCullsNothing() {
        Triangle3d face = new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0));
        Triangle3d alongIt = new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 0, 0), new Vector3d(0, 0.5, 0));
        Mesh3d faceMesh = new Mesh3d(Collections.singletonList(face));
        double area = MeshGeometry.area(faceMesh);
        double visible = MeshGeometry
                .area(MeshGeometry.visiblePart(faceMesh, new Mesh3d(Collections.singletonList(alongIt))));
        assertTrue("area=" + area + " visible=" + visible, Math.abs(visible - area) <= area * 1.0E-6);
    }

    /**
     * Two round slopes filling the block, the second turned a quarter about an axis, leave free what neither of them
     * covers. Anti fill against both has to fill exactly that: the tile filling against the first one used to be left
     * out where it ran into the second.
     */
    @Test
    public void antiFillAgainstTwoTurnedRoundSlopesFillsWhatBothLeaveFree() {
        Vector3i size = new Vector3i(16, 16, 16), pos = new Vector3i();
        LittleTileBlock first = placed(shape(LittleTileShapeMode.SLOPE_CONCAVE, size, pos, 0));
        float quarter = (float) Math.PI / 2;
        for (Matrix3f turn : new Matrix3f[] { new Matrix3f().rotateX(quarter), new Matrix3f().rotateY(quarter),
                new Matrix3f().rotateZ(quarter) }) {
            LittleTileBlock second = placed(
                    shape(LittleTileShapeMode.SLOPE_CONCAVE, size, pos, OrientationMapper.toId(turn)));
            String turned = describe(second.getCutoutInfo());
            double free = BlockSpace.volume(BLOCK) - Tile.of(first.getCutoutInfo()).volume()
                    - Tile.of(second.getCutoutInfo()).volume()
                    + MeshCollision.sharedVolume(BLOCK, first.getCutoutInfo(), BLOCK, second.getCutoutInfo())
                            * BlockSpace.PIXELS
                            * BlockSpace.PIXELS
                            * BlockSpace.PIXELS;
            assertTrue(turned + " leaves nothing free", free > 1);

            LittleTileBlock filling = new LittleTileBlock();
            filling.boundingBox = BLOCK.copy();
            filling.updateCorner();
            LittleTile againstFirst = filling.antiTileAgainst(first);
            assertNotNull(againstFirst);
            LittleTile againstBoth = againstFirst.antiTileAgainst(second);
            assertNotNull(turned, againstBoth);
            assertFalse(turned + " collides with the first", againstBoth.collidesWith(first));
            assertFalse(turned + " collides with the second", againstBoth.collidesWith(second));

            Tile anti = Tile.of(againstBoth.getCutoutInfo(), againstBoth.boundingBox);
            double volume = anti.volume(), meshVolume = MeshGeometry.volume(anti.mesh());
            assertTrue(turned + " volume=" + volume + " free=" + free, Math.abs(volume - free) <= EXACT_VOLUME);
            assertTrue(
                    turned + " mesh volume=" + meshVolume + " free=" + free,
                    Math.abs(meshVolume - free) <= ROUNDING_VOLUME);
            assertNull(turned, MeshGeometry.findOpenEdge(anti.mesh()));
        }
    }

    /**
     * An anti tile filling against two shapes, picked up and turned, still fills what the two shapes leave free when
     * they are turned the same way: its further shapes have to turn along with its own.
     */
    @Test
    public void antiTileAgainstTwoShapesTurnsWithThem() {
        Vector3i size = new Vector3i(16, 16, 16), pos = new Vector3i();
        LittleTileBlock first = placed(shape(LittleTileShapeMode.SLOPE_CONCAVE, size, pos, 0));
        Matrix3f quarter = new Matrix3f().rotateY((float) Math.PI / 2);
        LittleTileBlock second = placed(
                shape(LittleTileShapeMode.SLOPE_CONCAVE, size, pos, OrientationMapper.toId(quarter)));
        LittleTileBlock filling = new LittleTileBlock();
        filling.boundingBox = BLOCK.copy();
        filling.updateCorner();
        LittleTile anti = filling.antiTileAgainst(first).antiTileAgainst(second);
        double volume = Tile.of(anti.getCutoutInfo(), BLOCK).volume();

        for (ForgeDirection direction : new ForgeDirection[] { ForgeDirection.UP, ForgeDirection.NORTH,
                ForgeDirection.EAST }) {
            LittleTileCutoutInfo turnedAnti = turnedInBlock(anti.getCutoutInfo(), direction);
            String turned = "turned " + direction;
            LittleTileCutoutInfo turnedFirst = turnedInBlock(first.getCutoutInfo(), direction);
            LittleTileCutoutInfo turnedSecond = turnedInBlock(second.getCutoutInfo(), direction);
            assertFalse(
                    turned + " collides with the first",
                    MeshCollision.intersects(BLOCK, turnedAnti, BLOCK, turnedFirst));
            assertFalse(
                    turned + " collides with the second",
                    MeshCollision.intersects(BLOCK, turnedAnti, BLOCK, turnedSecond));
            double turnedVolume = Tile.of(turnedAnti, BLOCK).volume();
            assertTrue(
                    turned + " volume=" + turnedVolume + " expected=" + volume,
                    Math.abs(turnedVolume - volume) <= EXACT_VOLUME);
        }
    }

    /** The cutout of a tile filling the block, turned the way picking it up and rotating it turns it. */
    private static LittleTileCutoutInfo turnedInBlock(LittleTileCutoutInfo cutout, ForgeDirection direction) {
        NBTTagCompound nbt = new NBTTagCompound();
        cutout.writeToNBT(nbt);
        new LittleToolHandler(nbt).handleRotation(direction, new LittleTileSize(16, 16, 16));
        return LittleTileCutoutInfo.loadFromNBT(nbt);
    }

    private static LittleTileBlock placed(LittleTileCutoutInfo cutout) {
        LittleTileBlock tile = new LittleTileBlock();
        tile.boundingBox = BLOCK.copy();
        tile.setCutoutInfo(cutout);
        return tile;
    }
}
