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
 */
public final class Slopes {

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
}
