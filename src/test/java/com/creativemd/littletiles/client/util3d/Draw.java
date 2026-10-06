package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;

import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;

/** Random inputs: numbers, sizes and positions in pixels relative to the block minimum, boxes and shapes. */
public final class Draw {

    /** Half of the slopes get this large, so the block mostly clips a long face. */
    public static final int MAX_LARGE_SLOPE_SIZE = 50 * PIXELS;

    /** The other half stay small, so the ends of the face often lie inside the block, as for most slopes in game. */
    public static final int MAX_SMALL_SLOPE_SIZE = 2 * PIXELS;

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

    /** A size from {@code min} to {@code max} pixels on every axis, drawn on its own per axis. */
    public static Vector3i size(Random random, int min, int max) {
        return BlockSpace.vector(axis -> between(random, min, max));
    }

    /** Half of the sizes up to {@link #MAX_LARGE_SLOPE_SIZE}, half up to {@link #MAX_SMALL_SLOPE_SIZE}. */
    public static Vector3i slopeSize(Random random) {
        return size(random, 1, random.nextBoolean() ? MAX_LARGE_SLOPE_SIZE : MAX_SMALL_SLOPE_SIZE);
    }

    /**
     * Where bounds of the size start so they reach into the block on every axis: before the block minimum by less than
     * the size, or anywhere inside the block, so short bounds can also lie inside it without touching its faces.
     */
    public static Vector3i posReachingIntoBlock(Random random, Vector3i size) {
        return BlockSpace.vector(axis -> between(random, 1 - size.get(axis), PIXELS - 1));
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
}
