package com.creativemd.littletiles.client.util3d;

import java.util.function.IntUnaryOperator;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/** Coordinates inside one block, in pixels: its axes, vectors and boxes. */
public final class BlockSpace {

    public static final int PIXELS = 16;

    public static final int CENTER = PIXELS / 2;

    public static final LittleTileBox BLOCK = new LittleTileBox(0, 0, 0, PIXELS, PIXELS, PIXELS);

    public static final int X = 0, Y = 1, Z = 2;

    private BlockSpace() {}

    /** The vector with each component given by {@code component} of its axis. */
    public static Vector3i vector(IntUnaryOperator component) {
        return new Vector3i(component.applyAsInt(X), component.applyAsInt(Y), component.applyAsInt(Z));
    }

    public static LittleTileBox box(Vector3i min, Vector3i max) {
        return new LittleTileBox(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public static Vector3i min(LittleTileBox box) {
        return new Vector3i(box.minX, box.minY, box.minZ);
    }

    public static Vector3i max(LittleTileBox box) {
        return new Vector3i(box.maxX, box.maxY, box.maxZ);
    }

    public static Vector3i size(LittleTileBox box) {
        return max(box).sub(min(box));
    }

    /** The box the shape spans before anything clips it. */
    public static LittleTileBox bounds(LittleTileCutoutInfo shape) {
        return box(shape.pos, new Vector3i(shape.pos).add(shape.size));
    }

    /** The volume all boxes share, or null when they share none. */
    public static LittleTileBox intersection(LittleTileBox... boxes) {
        Vector3i min = min(boxes[0]), max = max(boxes[0]);
        for (LittleTileBox box : boxes) {
            min.max(min(box));
            max.min(max(box));
        }
        return min.x < max.x && min.y < max.y && min.z < max.z ? box(min, max) : null;
    }
}
