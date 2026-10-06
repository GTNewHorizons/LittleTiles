package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.AXES;
import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;

import java.util.Random;
import java.util.function.IntFunction;

import org.joml.Vector3i;

/**
 * Random corners of deformed boxes, in pixels. The eight corners of a box are indexed by one bit per axis, set where
 * the corner lies at the maximum along that axis.
 */
public final class DeformedBoxes {

    /** How far corners move from the block's corners when they only deform the box slightly. */
    public static final int MAX_CORNER_SHIFT = 2;

    private DeformedBoxes() {}

    /**
     * The block's corners sheared by up to {@link #MAX_CORNER_SHIFT} pixels per pair of axes, which keeps the faces
     * flat: the corners at the maximum along one axis all move by the same pixels along another.
     */
    public static Vector3i[] sheared(Random random) {
        int[][] shear = new int[AXES][AXES];
        for (int moved = 0; moved < AXES; moved++) {
            for (int by = 0; by < AXES; by++) {
                if (by != moved) shear[moved][by] = Draw.between(random, -MAX_CORNER_SHIFT, MAX_CORNER_SHIFT);
            }
        }
        return boxCorners(corner -> blockCorner(corner).add(BlockSpace.vector(moved -> {
            int offset = 0;
            for (int by = 0; by < AXES; by++) {
                if (by != moved && isAtMax(corner, by)) offset += shear[moved][by];
            }
            return offset;
        })));
    }

    public static boolean isAtMax(int corner, int axis) {
        return (corner & (1 << axis)) != 0;
    }

    public static Vector3i blockCorner(int corner) {
        return BlockSpace.vector(axis -> isAtMax(corner, axis) ? PIXELS : 0);
    }

    public static Vector3i[] boxCorners(IntFunction<Vector3i> corner) {
        Vector3i[] corners = new Vector3i[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
        for (int index = 0; index < corners.length; index++) corners[index] = corner.apply(index);
        return corners;
    }
}
