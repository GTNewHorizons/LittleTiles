package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.BLOCK;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Facts about unturned slopes, to check the meshes against.
 * <p>
 * A slope's face runs along x, from the top of its bounds at their minimum z down to their bottom at their maximum z.
 * The slope fills its bounds below the face, every complement above it. These facts are exact, in whole pixels.
 * <p>
 * Scaled to its unit box, a concave round slope's curved face runs along x on the circle of radius 1 around the top of
 * the box at its maximum z: the concave slope fills its bounds outside the circle, the convex complements inside it.
 * The curve is 16 chords between points on the circle, so points close to the circle count on neither side.
 */
public final class Slopes {

    /** Beyond this distance from the curve's center, a point lies in the concave slope however the curve is rounded. */
    public static final double CONCAVE_RADIUS = 1.001;

    /** Within this distance, below the cos(pi / 64) of the curve's 16 chords, a point lies in the convex complement. */
    public static final double CONVEX_RADIUS = 0.997;

    private Slopes() {}

    /** Whether the slope's face crosses the block: the slope and its complements each keep volume in it. */
    public static boolean faceCrossesBlock(LittleTileCutoutInfo slope) {
        LittleTileBox inBlock = BlockSpace.intersection(BlockSpace.bounds(slope), BLOCK);
        return inBlock != null && lowestLevel(slope, inBlock) < faceLevel(slope)
                && highestLevel(slope, inBlock) > faceLevel(slope);
    }

    /** Whether the slope shares volume with the box. */
    public static boolean overlaps(LittleTileCutoutInfo slope, LittleTileBox box) {
        LittleTileBox shared = BlockSpace.intersection(BlockSpace.bounds(slope), box);
        return shared != null && lowestLevel(slope, shared) < faceLevel(slope);
    }

    /** Whether the box lies inside the slope, possibly touching its faces. */
    public static boolean contains(LittleTileCutoutInfo slope, LittleTileBox box) {
        return box.equals(BlockSpace.intersection(BlockSpace.bounds(slope), box))
                && highestLevel(slope, box) <= faceLevel(slope);
    }

    /**
     * Whether the slope shares volume inside the block with its complements moved one pixel towards the minimum along
     * {@code axis}, y or z: they share the pixel-thin layer right below the slope's face.
     */
    public static boolean overlapsMovedComplementInBlock(LittleTileCutoutInfo slope, int axis) {
        LittleTileCutoutInfo moved = Shapes.movedAlong(slope, axis, -1);
        LittleTileBox shared = BlockSpace.intersection(BlockSpace.bounds(slope), BlockSpace.bounds(moved), BLOCK);
        if (shared == null) return false;

        // The moved complement holds the points that lie above the face once moved back by the pixel
        Vector3i highestMovedBack = BlockSpace.movedAlong(BlockSpace.max(shared), axis, 1);
        return lowestLevel(slope, shared) < faceLevel(slope)
                && level(slope, highestMovedBack.y, highestMovedBack.z) > faceLevel(slope);
    }

    /**
     * Whether the concave slope surely shares volume inside the block with its convex complements moved one pixel
     * towards the minimum along {@code axis}, y or z: they share a layer right below the curved face.
     */
    public static boolean concaveOverlapsMovedComplementInBlock(LittleTileCutoutInfo concave, int axis) {
        LittleTileCutoutInfo moved = Shapes.movedAlong(concave, axis, -1);
        LittleTileBox shared = BlockSpace.intersection(BlockSpace.bounds(concave), BlockSpace.bounds(moved), BLOCK);
        if (shared == null) return false;

        // The concave slope holds every point below one of its points, the convex one every point above. So the layer
        // passes through the shared bounds when their lowest corner lies in the concave slope, and their highest
        // corner, moved back by the pixel, in the convex one.
        Vector3i highestMovedBack = BlockSpace.movedAlong(BlockSpace.max(shared), axis, 1);
        return distanceFromCenter(concave, shared.minY, shared.minZ) > CONCAVE_RADIUS
                && distanceFromCenter(concave, highestMovedBack.y, highestMovedBack.z) < CONVEX_RADIUS;
    }

    /**
     * How far a point lies across the slope's face: (y - pos.y) / size.y + (z - pos.z) / size.z, times size.y * size.z
     * to keep it an exact integer. Points below {@link #faceLevel} lie in the slope, points above in its complements.
     */
    private static long level(LittleTileCutoutInfo slope, int y, int z) {
        return (long) (y - slope.pos.y) * slope.size.z + (long) (z - slope.pos.z) * slope.size.y;
    }

    /** The {@link #level} of the face. */
    private static long faceLevel(LittleTileCutoutInfo slope) {
        return (long) slope.size.y * slope.size.z;
    }

    private static long lowestLevel(LittleTileCutoutInfo slope, LittleTileBox box) {
        return level(slope, box.minY, box.minZ);
    }

    private static long highestLevel(LittleTileCutoutInfo slope, LittleTileBox box) {
        return level(slope, box.maxY, box.maxZ);
    }

    /** The distance of a point from the center of the curve, in the slope's unit box. */
    private static double distanceFromCenter(LittleTileCutoutInfo concave, int y, int z) {
        double fromTop = 1 - (y - concave.pos.y) / (double) concave.size.y;
        double fromFarEnd = 1 - (z - concave.pos.z) / (double) concave.size.z;
        return Math.hypot(fromTop, fromFarEnd);
    }
}
