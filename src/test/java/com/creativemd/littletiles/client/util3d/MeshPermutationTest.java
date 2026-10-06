package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.BLOCK;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCollide;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCollideEitherWay;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertEmpty;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertFit;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertFitsExactlyWhen;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertHasTiltedFaces;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertNotEmpty;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE_CONCAVE;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE_CONVEX;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.creativemd.littletiles.client.util3d.BlockSpace.Split;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/** Opt-in mesh permutation tests, see {@link Sweep}. Failures include the seed and generated inputs. */
public class MeshPermutationTest {

    /** Each trial places 768 bars, so fewer trials take about as long as the other sweeps. */
    private static final int ONE_PIXEL_BAR_TRIALS = 10_000;

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
}
