package com.creativemd.littletiles.client.util3d;

import java.util.Random;
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

    public static final int AXES = 3;

    private BlockSpace() {}

    /** The vector with each component given by {@code component} of its axis. */
    public static Vector3i vector(IntUnaryOperator component) {
        return new Vector3i(component.applyAsInt(X), component.applyAsInt(Y), component.applyAsInt(Z));
    }

    /** One pixel along the axis. */
    public static Vector3i unit(int axis) {
        return vector(other -> other == axis ? 1 : 0);
    }

    /** A copy of the vector moved by {@code pixels} along the axis. */
    public static Vector3i movedAlong(Vector3i vector, int axis, int pixels) {
        return new Vector3i(vector).add(unit(axis).mul(pixels));
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

    /** A box cut in two at a pixel plane across one axis. */
    public static final class Split {

        public final int axis;
        /** The plane's position along the axis, in pixels from the block minimum. */
        public final int at;
        public final LittleTileBox low, high;

        private Split(LittleTileBox box, int axis, int at) {
            this.axis = axis;
            this.at = at;
            low = box(min(box), max(box).setComponent(axis, at));
            high = box(min(box).setComponent(axis, at), max(box));
        }

        /** At a random pixel plane inside the box, which must be longer than a pixel along some axis. */
        public static Split random(Random random, LittleTileBox box) {
            Vector3i size = size(box);
            int axis = Draw.until(() -> Draw.axis(random), candidate -> size.get(candidate) > 1);
            return new Split(box, axis, min(box).get(axis) + Draw.between(random, 1, size.get(axis) - 1));
        }

        public LittleTileBox[] halves() {
            return new LittleTileBox[] { low, high };
        }

        @Override
        public String toString() {
            return "split at " + at + " along axis " + axis;
        }
    }
}
