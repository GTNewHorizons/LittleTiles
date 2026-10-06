package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertCollide;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertFit;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertNotEmpty;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE;

import java.util.Random;

import org.junit.Test;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;

/** Opt-in mesh permutation tests, see {@link Sweep}. Failures include the seed and generated inputs. */
public class MeshPermutationTest {

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
}
