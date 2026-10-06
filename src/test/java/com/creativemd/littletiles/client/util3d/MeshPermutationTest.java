package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertFit;
import static com.creativemd.littletiles.client.util3d.MeshAssertions.assertNotEmpty;
import static com.creativemd.littletiles.common.utils.LittleTileShapeMode.SLOPE;

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
}
