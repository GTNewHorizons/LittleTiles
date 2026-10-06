package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;

import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/** Random inputs: numbers, sizes and positions in pixels relative to the block minimum, boxes and shapes. */
public final class Draw {

    /** Shapes span up to two and a half blocks, so they often reach out of the block. */
    public static final int MAX_SHAPE_SIZE = 40;

    /** Half of the slopes get this large, so the block mostly clips a long face. */
    public static final int MAX_LARGE_SLOPE_SIZE = 50 * PIXELS;

    /** The other half stay small, so the ends of the face often lie inside the block, as for most slopes in game. */
    public static final int MAX_SMALL_SLOPE_SIZE = 2 * PIXELS;

    /** Stepped slopes run down their face in this many equal steps, each up to this many pixels down and along. */
    public static final int MIN_STEPS = 2, MAX_STEPS = 4, MAX_STEP_SIZE = 8;

    private Draw() {}

    /**
     * Draws until {@code usable} accepts the draw. Inputs are redrawn rather than skipped, so every trial tests
     * something.
     */
    public static <T> T until(Supplier<T> draw, Predicate<T> usable) {
        while (true) {
            T candidate = draw.get();
            if (usable.test(candidate)) return candidate;
        }
    }

    /** From {@code min} to {@code max}, both included. */
    public static int between(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /** From 0 to {@code max}, both included. */
    public static int upTo(Random random, int max) {
        return between(random, 0, max);
    }

    public static <T> T oneOf(Random random, T[] options) {
        return options[random.nextInt(options.length)];
    }

    public static int axis(Random random) {
        return random.nextInt(BlockSpace.AXES);
    }

    public static int orientation(Random random) {
        return random.nextInt(Shapes.ORIENTATIONS);
    }

    /** A size from {@code min} to {@code max} pixels on every axis, drawn on its own per axis. */
    public static Vector3i size(Random random, int min, int max) {
        return BlockSpace.vector(axis -> between(random, min, max));
    }

    /** Half of the sizes up to {@link #MAX_LARGE_SLOPE_SIZE}, half up to {@link #MAX_SMALL_SLOPE_SIZE}. */
    public static Vector3i slopeSize(Random random) {
        return size(random, 1, random.nextBoolean() ? MAX_LARGE_SLOPE_SIZE : MAX_SMALL_SLOPE_SIZE);
    }

    /** Y or z: moving a slope's complement along x would keep it beside the slope. */
    public static int axisAcrossSlopeFace(Random random) {
        return random.nextBoolean() ? BlockSpace.Y : BlockSpace.Z;
    }

    /**
     * Where bounds of the size start so they reach into the block on every axis: before the block minimum by less than
     * the size, or anywhere inside the block, so short bounds can also lie inside it without touching its faces.
     */
    public static Vector3i posReachingIntoBlock(Random random, Vector3i size) {
        return BlockSpace.vector(axis -> between(random, 1 - size.get(axis), PIXELS - 1));
    }

    /** A box inside {@code bounds}, at least a pixel long on every axis. */
    public static LittleTileBox boxWithin(Random random, LittleTileBox bounds) {
        Vector3i boundsMin = BlockSpace.min(bounds), boundsMax = BlockSpace.max(bounds);
        Vector3i min = BlockSpace.vector(axis -> between(random, boundsMin.get(axis), boundsMax.get(axis) - 1));
        Vector3i max = BlockSpace.vector(axis -> between(random, min.get(axis) + 1, boundsMax.get(axis)));
        return BlockSpace.box(min, max);
    }

    /** Any cut shape in any orientation, spanning the bounds of the size at the position. */
    public static LittleTileCutoutInfo cutShape(Random random, Vector3i size, Vector3i pos) {
        return Shapes.shape(oneOf(random, Shapes.CUT_SHAPES), size, pos, orientation(random));
    }

    /** Any cut shape in any orientation, spanning exactly the box. */
    public static LittleTileCutoutInfo cutShapeFilling(Random random, LittleTileBox box) {
        return cutShape(random, BlockSpace.size(box), BlockSpace.min(box));
    }

    /** Any cut shape keeping some volume in the block. */
    public static Tile cutShapeInBlock(Random random) {
        return until(() -> {
            Vector3i size = size(random, 1, MAX_SHAPE_SIZE);
            return Tile.of(cutShape(random, size, posReachingIntoBlock(random, size)));
        }, tile -> !tile.isEmpty());
    }

    /** An unturned shape of the size, placed where it reaches into the block, see {@link #posReachingIntoBlock}. */
    public static LittleTileCutoutInfo unturnedReachingIntoBlock(Random random, LittleTileShapeMode kind,
            Vector3i size) {
        return Shapes.shape(kind, size, posReachingIntoBlock(random, size), Shapes.UNTURNED);
    }

    /** A slope of {@link #slopeSize} whose face crosses the block, turned into the orientation. */
    public static LittleTileCutoutInfo slopeAcrossBlock(Random random, int orientation) {
        LittleTileCutoutInfo unturned = until(
                () -> unturnedReachingIntoBlock(random, LittleTileShapeMode.SLOPE, slopeSize(random)),
                Slopes::faceCrossesBlock);
        return Shapes.turned(unturned, orientation);
    }

    /**
     * An unturned slope reaching into the block, whose face runs down y and along z in {@link #MIN_STEPS} to
     * {@link #MAX_STEPS} equal steps, so it passes through whole pixels between its ends.
     */
    public static LittleTileCutoutInfo steppedSlope(Random random) {
        int steps = between(random, MIN_STEPS, MAX_STEPS);
        Vector3i size = new Vector3i(
                between(random, 1, PIXELS),
                steps * between(random, 1, MAX_STEP_SIZE),
                steps * between(random, 1, MAX_STEP_SIZE));
        return unturnedReachingIntoBlock(random, LittleTileShapeMode.SLOPE, size);
    }

    /**
     * A slope whose bounds have a stretch of the unturned {@code big} slope's face as their diagonal, so both faces lie
     * in one plane, over part of the big slope's length along x. The stretch runs between whole pixels of the face,
     * never from one end to the other, so the big face must pass through whole pixels between its ends, as a
     * {@link #steppedSlope} does.
     *
     * @param orientation the orientation of the small slope: unturned it is a part of the big one, a complementing one
     *                    lies outside it
     */
    public static LittleTileCutoutInfo slopeOnStretchOf(Random random, LittleTileCutoutInfo big, int orientation) {
        // The face passes through whole pixels at every step of this many pixels down and along
        int steps = greatestCommonDivisor(big.size.y, big.size.z);
        int stepDown = big.size.y / steps, stepAlong = big.size.z / steps;
        int firstStep = upTo(random, steps - 1);
        // Starting at the first step, stop before the last, so the small bounds are never the whole big ones
        int endStep = between(random, firstStep + 1, firstStep == 0 ? steps - 1 : steps);
        int smallSteps = endStep - firstStep;
        int startX = upTo(random, big.size.x - 1);
        int endX = between(random, startX + 1, big.size.x);
        Vector3i size = new Vector3i(endX - startX, smallSteps * stepDown, smallSteps * stepAlong);
        // The face starts at the top at the minimum z, so the steps along z count from the top down
        Vector3i pos = new Vector3i(startX, (steps - endStep) * stepDown, firstStep * stepAlong).add(big.pos);
        return Shapes.shape(LittleTileShapeMode.SLOPE, size, pos, orientation);
    }

    private static int greatestCommonDivisor(int a, int b) {
        return b == 0 ? a : greatestCommonDivisor(b, a % b);
    }
}
