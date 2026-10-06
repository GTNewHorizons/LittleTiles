package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.BLOCK;
import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertClosedAndVolumesAddUp;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCollide;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCollideEitherWay;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCulled;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCulledByComplement;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertEmpty;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertFit;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertFitsExactlyWhen;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertHasTiltedFaces;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertHidden;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertNotEmpty;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertOnlySharedFacesHideEachOther;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertStayVisible;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertVisibleArea;
import static com.creativemd.littletiles.client.util3d.MeshGeometry.area;
import static com.creativemd.littletiles.client.util3d.Message.check;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE_CONCAVE;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE_CONVEX;

import java.util.List;
import java.util.Random;

import org.joml.Vector3i;
import org.junit.Test;

import com.creativemd.littletiles.client.util3d.BlockSpace.Split;
import com.creativemd.littletiles.client.util3d.MeshGeometry.FaceFilter;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/** Opt-in mesh permutation tests, see {@link Sweep}. Failures include the seed and generated inputs. */
public class MeshPermutationTest {

    /** Each trial places 768 bars, so fewer trials take about as long as the other sweeps. */
    private static final int ONE_PIXEL_BAR_TRIALS = 10_000;

    /** Large cuts span more than two blocks and up to sixteen, so the coordinates of the cuts get large. */
    private static final int MIN_LARGE_CUT_SIZE = 2 * PIXELS + 1, MAX_LARGE_CUT_SIZE = 16 * PIXELS;

    /** Checks slopes crossing the block against all four complements, in every orientation. */
    @Test
    public void complementarySlopesOnlyTouchInWholeBoxes() {
        Sweep.run("TEST_PERMUTATIONS_COMPLEMENTARY_SLOPES", "complementary slopes", trial -> {
            LittleTileCutoutInfo shape = Draw.slopeAcrossBlock(trial.random(), trial.orientationInTurn());
            Tile slope = trial.input("slope", Tile.of(shape));
            assertNotEmpty(slope);
            for (Tile complement : slope.complements(SLOPE)) {
                assertNotEmpty(complement);
                assertFit(slope, complement);
            }
        });
    }

    /**
     * Moves the four complements of a slope one pixel into it along y or z, before turning all of them into one of the
     * orientations. They then share a pixel-thin layer below the slope face inside the block, so they must collide,
     * catching collisions missed where the faces almost touch.
     */
    @Test
    public void complementarySlopesMovedIntoEachOtherCollide() {
        Sweep.run("TEST_PERMUTATIONS_OVERLAPPING_SLOPES", "overlapping slopes", trial -> {
            Random random = trial.random();
            int axis = trial.input("axis", Draw.axisAcrossSlopeFace(random));
            LittleTileCutoutInfo unturned = trial.input(
                    "unturned slope",
                    Draw.until(
                            () -> Draw.unturnedReachingIntoBlock(random, SLOPE, Draw.slopeSize(random)),
                            candidate -> Slopes.overlapsMovedComplementInBlock(candidate, axis)));
            int turn = trial.orientationInTurn();
            Tile slope = Tile.of(unturned).turned(turn);
            for (Tile moved : Tile.of(Shapes.movedAlong(unturned, axis, -1)).complements(SLOPE)) {
                assertCollide(slope, moved.turned(turn));
            }
        });
    }

    /**
     * Places a shape inside another, taking turns between the same shape twice, a part of a shape clipped to a smaller
     * box, any shape inside a box inside a slope, and a slope inside a bigger one with its face in the same plane.
     * Where faces only touch no surfaces cross, so the collision has to notice that one lies inside the other. They
     * must collide whichever of the two is placed first.
     */
    @Test
    public void nestedShapesCollide() {
        Sweep.run("TEST_PERMUTATIONS_NESTED_SHAPES", "nested shapes", trial -> {
            Random random = trial.random();
            Tile outer, inner;
            switch (trial.inTurn(4)) {
                case 0 -> {
                    // The same shape twice
                    outer = inner = Draw.cutShapeInBlock(random);
                }
                case 1 -> {
                    // A shape, and the part of it in a smaller box
                    Tile shape = Draw.cutShapeInBlock(random);
                    outer = shape;
                    inner = Draw.until(() -> shape.clippedTo(Draw.boxWithin(random, BLOCK)), part -> !part.isEmpty());
                }
                case 2 -> {
                    // A slope crossing the block, and any shape filling a box inside it, possibly touching its faces
                    LittleTileCutoutInfo slope;
                    LittleTileBox box;
                    do {
                        slope = Draw.unturnedReachingIntoBlock(random, SLOPE, Draw.slopeSize(random));
                        box = Draw.boxWithin(random, BlockSpace.intersection(BlockSpace.bounds(slope), BLOCK));
                    } while (!Slopes.contains(slope, box));
                    int turn = Draw.orientation(random);
                    outer = Tile.of(slope).turned(turn);
                    inner = Tile.of(Draw.cutShapeFilling(random, Shapes.turned(box, turn)));
                }
                default -> {
                    // A slope, and a smaller one on a stretch of its face, as a part of it
                    LittleTileCutoutInfo big = Draw.until(() -> Draw.steppedSlope(random), Slopes::faceCrossesBlock);
                    LittleTileCutoutInfo part = Draw.until(
                            () -> Draw.slopeOnStretchOf(random, big, Shapes.UNTURNED),
                            Slopes::faceCrossesBlock);
                    int turn = Draw.orientation(random);
                    outer = Tile.of(big).turned(turn);
                    inner = Tile.of(part).turned(turn);
                }
            }
            assertCollideEitherWay(trial.input("outer", outer), trial.input("inner", inner));
        });
    }

    /**
     * Places a box tile inside a slope, possibly touching its faces, and a shape inside a box tile, half of them
     * filling it exactly. Where faces only touch no surfaces cross, so the collision has to notice that one lies inside
     * the other. They must collide whichever of the two is placed first.
     */
    @Test
    public void shapesAndBoxesInsideEachOtherCollide() {
        Sweep.run("TEST_PERMUTATIONS_NESTED_BOXES", "nested boxes", trial -> {
            Random random = trial.random();
            Tile outer, inner;
            switch (trial.inTurn(2)) {
                case 0 -> {
                    // A slope crossing the block, and a plain box inside it, possibly touching its faces
                    LittleTileCutoutInfo slope;
                    LittleTileBox box;
                    do {
                        slope = Draw.unturnedReachingIntoBlock(random, SLOPE, Draw.slopeSize(random));
                        box = Draw.boxWithin(random, BlockSpace.intersection(BlockSpace.bounds(slope), BLOCK));
                    } while (!Slopes.contains(slope, box));
                    int turn = Draw.orientation(random);
                    outer = Tile.of(slope).turned(turn);
                    inner = Tile.plainBox(Shapes.turned(box, turn));
                }
                default -> {
                    // A plain box, and any shape filling it or a smaller box inside it
                    LittleTileBox box = Draw.boxWithin(random, BLOCK);
                    LittleTileBox shapeBox = random.nextBoolean() ? box : Draw.boxWithin(random, box);
                    outer = Tile.plainBox(box);
                    inner = Tile.of(Draw.cutShapeFilling(random, shapeBox), shapeBox);
                }
            }
            assertCollideEitherWay(trial.input("outer", outer), trial.input("inner", inner));
        });
    }

    /**
     * Splits the block of each slope crossing it at a random interior pixel plane, before turning both into one of the
     * orientations. Exactly the pieces sharing volume with the slope must keep some of it, catching pieces the cuts
     * lose. The two pieces must fit beside each other, and every piece keeping some of the slope beside each of the
     * four unsplit complements, catching false collisions from clipping the same surface in different boxes.
     */
    @Test
    public void complementarySlopesOnlyTouchAcrossSplitBoxes() {
        Sweep.run("TEST_PERMUTATIONS_SPLIT_SLOPES", "split slopes", trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo unturned = trial.input(
                    "unturned slope",
                    Draw.until(
                            () -> Draw.unturnedReachingIntoBlock(random, SLOPE, Draw.slopeSize(random)),
                            Slopes::faceCrossesBlock));
            Split split = trial.input("unturned split", Split.random(random, BLOCK));
            int turn = trial.orientationInTurn();
            Tile slope = trial.input("slope", Tile.of(unturned).turned(turn));
            LittleTileBox[] halves = split.halves();
            Tile[] pieces = new Tile[halves.length];
            for (int half = 0; half < halves.length; half++) {
                pieces[half] = slope.clippedTo(Shapes.turned(halves[half], turn));
                if (Slopes.overlaps(unturned, halves[half])) assertNotEmpty(pieces[half]);
                else assertEmpty(pieces[half]);
            }
            assertFit(pieces[0], pieces[1]);
            for (Tile piece : pieces) {
                // A piece the slope misses has no mesh, so it would fit beside anything
                if (piece.isEmpty()) continue;

                for (Tile complement : slope.complements(SLOPE)) assertFit(piece, complement);
            }
        });
    }

    /**
     * Places every one-pixel bar through the block along each axis next to a slope filling the block, with both turned
     * into a random orientation. Bars sharing volume with the slope must collide and the others must fit, catching
     * missed and false collisions of a mesh against a plain box, along the slope's ridge and across its face.
     */
    @Test
    public void onePixelBarsCollideExactlyWhenOverlapping() {
        List<LittleTileBox> bars = BlockSpace.onePixelBars();
        Sweep.run("TEST_PERMUTATIONS_MESH_BOX", "one-pixel bars", ONE_PIXEL_BAR_TRIALS, trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo unturned = trial.input(
                    "unturned slope",
                    Draw.until(
                            () -> Draw.unturnedReachingIntoBlock(random, SLOPE, Draw.smallSlopeSize(random)),
                            Slopes::faceCrossesBlock));
            int turn = trial.input("turn", Draw.orientation(random));
            Tile slope = Tile.of(unturned).turned(turn);
            for (LittleTileBox bar : bars) {
                assertFitsExactlyWhen(!Slopes.overlaps(unturned, bar), slope, Tile.plainBox(bar).turned(turn));
            }
        });
    }

    /**
     * Places a small slope in a random one of the complementing orientations on a stretch of a big slope's face, see
     * {@link Draw#slopeOnStretchOf}. Its face lies in the big slope's plane, so the two only touch, even though their
     * faces are triangulated and clipped differently.
     */
    @Test
    public void differentlySizedCoplanarSlopesOnlyTouch() {
        Sweep.run("TEST_PERMUTATIONS_COPLANAR_SLOPES", "coplanar slopes", trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo big = Draw.until(() -> Draw.steppedSlope(random), Slopes::faceCrossesBlock);
            int complement = Draw.oneOf(random, Shapes.COMPLEMENTS_OF_UNTURNED);
            LittleTileCutoutInfo small = Draw
                    .until(() -> Draw.slopeOnStretchOf(random, big, complement), Slopes::faceCrossesBlock);
            int turn = Draw.orientation(random);
            Tile bigSlope = trial.input("big", Tile.of(big).turned(turn));
            Tile smallSlope = trial.input("small", Tile.of(small).turned(turn));
            assertHasTiltedFaces(bigSlope);
            assertHasTiltedFaces(smallSlope);
            assertFit(bigSlope, smallSlope);
        });
    }

    /**
     * Places the four convex round slopes complementing a concave one with the same bounds, all turned into a random
     * orientation, with the curved face crossing the block. Their curved faces are the same facets facing opposite
     * ways, so the two only touch.
     */
    @Test
    public void roundSlopeComplementsOnlyTouch() {
        Sweep.run("TEST_PERMUTATIONS_ROUND_SLOPES", "round slopes", trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo unturned = Draw
                    .unturnedMeetingComplementsInBlock(random, SLOPE_CONCAVE, SLOPE_CONVEX, Draw::smallSlopeSize);
            Tile concave = trial.input("concave", Tile.of(unturned).turned(Draw.orientation(random)));
            for (Tile convex : concave.complements(SLOPE_CONVEX)) assertFit(concave, convex);
        });
    }

    /**
     * Moves the four convex round slopes complementing a concave one a pixel into it along y or z, before turning all
     * of them into one of the orientations. They then share a layer below the curved face inside the block, so they
     * must collide, catching collisions missed where the curved faces almost touch.
     */
    @Test
    public void roundSlopeComplementsMovedIntoEachOtherCollide() {
        Sweep.run("TEST_PERMUTATIONS_OVERLAPPING_ROUND_SLOPES", "overlapping round slopes", trial -> {
            Random random = trial.random();
            int axis = trial.input("axis", Draw.axisAcrossSlopeFace(random));
            LittleTileCutoutInfo unturned = trial.input(
                    "unturned concave",
                    Draw.until(
                            () -> Draw.unturnedReachingIntoBlock(random, SLOPE_CONCAVE, Draw.smallSlopeSize(random)),
                            candidate -> Slopes.concaveOverlapsMovedComplementInBlock(candidate, axis)));
            int turn = trial.orientationInTurn();
            Tile concave = Tile.of(unturned).turned(turn);
            for (Tile moved : Tile.of(Shapes.movedAlong(unturned, axis, -1)).complements(SLOPE_CONVEX)) {
                assertCollide(concave, moved.turned(turn));
            }
        });
    }

    /**
     * Culls the slope face of a slope against a complement with the same bounds, and the other way around. Both faces
     * cover each other exactly, so nothing but slivers may stay visible. The flat faces only lie next to each other, so
     * they must stay visible.
     */
    @Test
    public void matchingSlopeFacesLeaveNoLargeVisibleArea() {
        Sweep.run("TEST_PERMUTATIONS_CULLING_SLOPES", "culling slopes", trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo unturned = Draw
                    .unturnedMeetingComplementsInBlock(random, SLOPE, SLOPE, Draw::slopeSize);
            Tile slope = trial.input("slope", Tile.of(unturned).turned(Draw.orientation(random)));
            Tile complement = trial.input("complement", Draw.oneOf(random, slope.complements(SLOPE)));
            assertCulledByComplement(slope, complement);
            assertCulledByComplement(complement, slope);
        });
    }

    /**
     * Culls the curved face of a concave round slope against a convex one complementing it, and the other way
     * around. Each facet covers its counterpart exactly, so nothing but slivers may stay visible. The flat faces only
     * lie next to each other, so they must stay visible.
     */
    @Test
    public void matchingRoundFacesLeaveNoLargeVisibleArea() {
        Sweep.run("TEST_PERMUTATIONS_CULLING_ROUND_SLOPES", "culling round slopes", trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo unturned = Draw
                    .unturnedMeetingComplementsInBlock(random, SLOPE_CONCAVE, SLOPE_CONVEX, Draw::slopeSize);
            Tile concave = trial.input("concave", Tile.of(unturned).turned(Draw.orientation(random)));
            Tile convex = trial.input("convex", Draw.oneOf(random, concave.complements(SLOPE_CONVEX)));
            assertCulledByComplement(concave, convex);
            assertCulledByComplement(convex, concave);
        });
    }

    /**
     * Culls a small slope in a random one of the complementing orientations on a stretch of a big slope's face, see
     * {@link Draw#slopeOnStretchOf}, and the big slope against each other. The big face covers the small one entirely,
     * so nothing but slivers of the small face may stay visible, and exactly the small face's area of the big face may
     * go. The flat faces only lie next to each other, so they must stay visible.
     */
    @Test
    public void differentlySizedCoplanarFacesLeaveNoLargeVisibleArea() {
        Sweep.run("TEST_PERMUTATIONS_CULLING_COPLANAR_SLOPES", "culling coplanar slopes", trial -> {
            Random random = trial.random();
            LittleTileCutoutInfo big = Draw.until(() -> Draw.steppedSlope(random), Slopes::faceCrossesBlock);
            int complement = Draw.oneOf(random, Shapes.COMPLEMENTS_OF_UNTURNED);
            LittleTileCutoutInfo small = Draw
                    .until(() -> Draw.slopeOnStretchOf(random, big, complement), Slopes::faceCrossesBlock);
            int turn = Draw.orientation(random);
            Tile bigSlope = trial.input("big", Tile.of(big).turned(turn));
            Tile smallSlope = trial.input("small", Tile.of(small).turned(turn));
            assertHasTiltedFaces(bigSlope);
            assertHasTiltedFaces(smallSlope);
            assertCulledByComplement(smallSlope, bigSlope);
            assertCulled(bigSlope, smallSlope, bigSlope.tiltedFaceArea() - smallSlope.tiltedFaceArea());
        });
    }

    /**
     * Places a shape across the face between the block and its neighbour, as one tile in each, the neighbour built in
     * its own block and moved next to this one as rendering does. Both are cut at that face into caps of the same
     * cross-section, so each cap must hide the other up to slivers. The faces off that face only meet the other tile's
     * along the seam, so they must stay visible.
     */
    @Test
    public void adjacentCutCapsLeaveNoVisibleRemnants() {
        Sweep.run("TEST_PERMUTATIONS_CAP_REMNANTS", "cap remnants", trial -> {
            Random random = trial.random();
            int axis;
            FaceFilter blockFace;
            Tile here, neighbour;
            do {
                axis = Draw.axis(random);
                blockFace = FaceFilter.onPlane(axis, PIXELS);
                // Longer than a block, starting in this block and ending in the neighbour along the axis
                Vector3i size = Draw.size(random, PIXELS + 1, Draw.MAX_SHAPE_SIZE);
                Vector3i pos = Draw.posReachingIntoBlock(random, size);
                pos.setComponent(axis, Draw.startAcross(random, PIXELS, size.get(axis)));
                here = Tile.of(Draw.cutShape(random, size, pos));
                neighbour = Tile.of(Shapes.movedAlong(here.shapeInBlock(), axis, -PIXELS));
                // Shapes without volume at the block face have no caps there, and a sliver past it is no tile at all
            } while (area(blockFace.of(here.mesh())) == 0 || neighbour.isEmpty());
            trial.input("axis", axis);
            trial.input("here", here);
            trial.input("neighbour", neighbour);

            Mesh3d neighbourBesideHere = MeshGeometry.besideThisBlock(neighbour.mesh(), axis);
            assertOnlySharedFacesHideEachOther(here.mesh(), neighbourBesideHere, blockFace);
        });
    }

    /**
     * Splits the block at a random pixel plane into a cut shape and a plain box, with the box's faces as culling builds
     * them. The box face on the split plane covers the shape's cap there, so the cap must be hidden up to slivers, and
     * the face must stay visible exactly where the cap does not cover it. The box's other faces only meet the shape's
     * along the seam, so they must stay visible.
     */
    @Test
    public void boxFacesAndCutCapsCullEachOther() {
        Sweep.run("TEST_PERMUTATIONS_BOX_CAPS", "box caps", trial -> {
            Random random = trial.random();
            FaceFilter splitPlane;
            Tile shape, box;
            do {
                Split split = Split.random(random, BLOCK);
                splitPlane = FaceFilter.onPlane(split.axis, split.at);
                Vector3i size = Draw.size(random, 1, Draw.MAX_SHAPE_SIZE);
                // At least two pixels long across the plane, to reach a pixel past it on both sides
                size.setComponent(split.axis, Draw.between(random, 2, Draw.MAX_SHAPE_SIZE));
                Vector3i pos = Draw.posReachingIntoBlock(random, size);
                pos.setComponent(split.axis, Draw.startAcross(random, split.at, size.get(split.axis)));
                boolean shapeInLowHalf = random.nextBoolean();
                shape = Tile.of(Draw.cutShape(random, size, pos), shapeInLowHalf ? split.low : split.high);
                box = Tile.plainBox(shapeInLowHalf ? split.high : split.low);
                // Shapes without volume at the split plane have no cap there
            } while (area(splitPlane.of(shape.mesh())) == 0);
            trial.input("shape", shape);
            trial.input("box", box);

            Mesh3d cap = splitPlane.of(shape.mesh()), boxFace = splitPlane.of(box.mesh());
            assertHidden("cap", cap, boxFace);
            assertVisibleArea("box face", boxFace, shape.mesh(), area(boxFace) - area(cap));
            assertStayVisible("box faces off the split", splitPlane.notOf(box.mesh()), shape.mesh());
        });
    }

    /**
     * Clips random shapes, placed anywhere they overlap the box, to random sub-boxes of the block, and to both halves
     * of the sub-box split at a random pixel plane. Every resulting mesh must be closed and enclose positive volume, no
     * more than its box, and the volumes of the halves must add up to the whole, catching meshes turned inside out and
     * pieces the cuts lose or double. Shapes that only touch their box are left to their own sweep.
     */
    @Test
    public void randomlyClippedShapeMeshesClose() {
        Sweep.run("TEST_PERMUTATIONS_CLOSE_MESH", "closed meshes", trial -> {
            Random random = trial.random();
            Tile shape = trial.input("shape", Draw.cutShapeClippedToSubBox(random));
            assertClosedAndVolumesAddUp(random, shape);
        });
    }

    /**
     * Clips large random shapes to the block where their surface crosses it. Every resulting mesh must be closed, and
     * its volume must add up across a split, also where the coordinates of the cuts get large.
     */
    @Test
    public void largeRandomCutsClose() {
        Sweep.run("TEST_PERMUTATIONS_LARGE_CUTS", "large cuts", trial -> {
            Random random = trial.random();
            // The block can lie wholly inside the shape or outside it, where its surface does not cross the block
            Tile shape = trial.input("shape", Draw.until(() -> {
                Vector3i size = Draw.size(random, MIN_LARGE_CUT_SIZE, MAX_LARGE_CUT_SIZE);
                return Tile.of(Draw.cutShape(random, size, Draw.posReachingIntoBlock(random, size)));
            }, Tile::hasTiltedFaces));
            assertClosedAndVolumesAddUp(random, shape);
        });
    }

    /**
     * Places random shapes so their bounds meet a random sub-box of the block from outside in a face, an edge or a
     * corner, overlapping it anywhere on the other axes. They share no volume with the box, so nothing of them may be
     * left after clipping.
     */
    @Test
    public void meshesThatOnlyTouchTheSubboxAreEmpty() {
        Sweep.run("TEST_PERMUTATIONS_TOUCHING_SUBBOX", "touching sub-boxes", trial -> {
            Random random = trial.random();
            LittleTileBox box = Draw.boxWithin(random, BLOCK);
            LittleTileCutoutInfo shape = Draw.cutShapeOverlapping(random, box);
            // One bit per axis along which the bounds only touch the box: one for a face, two for an edge, three for a
            // corner
            int touching = Draw.between(random, 1, 7);
            for (int axis = 0; axis < BlockSpace.AXES; axis++) {
                if ((touching & (1 << axis)) == 0) continue;

                // Starting where the box ends, or ending where it starts
                int start = random.nextBoolean() ? BlockSpace.max(box).get(axis)
                        : BlockSpace.min(box).get(axis) - shape.size.get(axis);
                shape.pos.setComponent(axis, start);
            }
            assertEmpty(trial.input("shape", Tile.of(shape, box)));
        });
    }

    /**
     * Shears a cube by up to two pixels per pair of axes, which keeps its faces flat, and clips it to a random sub-box
     * as {@link #randomlyClippedShapeMeshesClose} does: the mesh must be closed and its volume add up.
     */
    @Test
    public void shearedDeformedBoxesClipClosed() {
        Sweep.run("TEST_PERMUTATIONS_DEFORMED_CAPS", "sheared deformed boxes", trial -> {
            Random random = trial.random();
            Vector3i[] corners = trial.input("corners", DeformedBoxes.sheared(random));
            Tile box = trial.input("box", Draw.deformedBoxClippedToSubBox(random, corners));
            assertClosedAndVolumesAddUp(random, box);
        });
    }

    /**
     * Moves the corners of a cube on their own, from slightly to collapsing edges and faces, see
     * {@link DeformedBoxes#warped}, and clips those enclosing volume without folding to a random sub-box as
     * {@link #randomlyClippedShapeMeshesClose} does: the mesh must be closed and its volume add up.
     */
    @Test
    public void warpedDeformedBoxesClipClosed() {
        Sweep.run("TEST_PERMUTATIONS_DEFORMED_CAPS", "warped deformed boxes", trial -> {
            Random random = trial.random();
            Vector3i[] corners = trial.input(
                    "corners",
                    Draw.until(
                            () -> DeformedBoxes.warped(random),
                            candidate -> Mesh3dUtil.enclosesVolume(candidate) && !DeformedBoxes.foldsOver(candidate)));
            Tile box = trial.input("box", Draw.deformedBoxClippedToSubBox(random, corners));
            assertClosedAndVolumesAddUp(random, box);
        });
    }

    /**
     * Splits the block near a random pixel plane into two deformed boxes sharing the face between them, see
     * {@link DeformedBoxes.Neighbours}. Both boxes must split a warped shared face along the same diagonal, or their
     * surfaces differ by a tetrahedron, which they then share or leave open. The boxes must fit beside each other, but
     * not with the high one moved a pixel into the low one. Each must hide the other's shared face up to slivers, and
     * keep its other faces, which only meet the other box's along the seam.
     */
    @Test
    public void neighbouringDeformedBoxesOnlyTouch() {
        Sweep.run("TEST_PERMUTATIONS_DEFORMED_NEIGHBOURS", "deformed neighbours", trial -> {
            DeformedBoxes.Neighbours boxes = trial.input("boxes", DeformedBoxes.neighbours(trial.random()));
            check(boxes.splitSharedFaceAlike(), "warped shared face split along different diagonals");
            assertFit(boxes.low, boxes.high);
            assertCollide(boxes.low, boxes.highMovedIntoLow());
            assertOnlySharedFacesHideEachOther(boxes.low.mesh(), boxes.high.mesh(), boxes.sharedFace());
        });
    }
}
