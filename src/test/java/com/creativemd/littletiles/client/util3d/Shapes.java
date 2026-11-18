package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.CENTER;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Builds and turns the info describing a tile's shape: its kind, the bounds it spans, in pixels, and its orientation,
 * one of the 48 rotations and mirrors of a cube. Bounds are given relative to the block minimum; {@link Tile} turns
 * them into what the game stores.
 */
public final class Shapes {

    /** Every shape made from a fixed mesh, so everything but pillars and deformed boxes. */
    public static final LittleTileShapeMode[] CUT_SHAPES = { LittleTileShapeMode.SLOPE,
            LittleTileShapeMode.SLOPE_CONCAVE, LittleTileShapeMode.SLOPE_CONVEX,
            LittleTileShapeMode.SLOPE_CONVEX_INNER_CORNER, LittleTileShapeMode.SLOPE_CONVEX_OUTER_CORNER,
            LittleTileShapeMode.SLOPE_TRIANGLE, LittleTileShapeMode.SLOPE_TRIANGLE_CORNER,
            LittleTileShapeMode.SLOPE_TRIANGLE_ALT, LittleTileShapeMode.SLOPE_OUTER_CORNER,
            LittleTileShapeMode.SLOPE_INNER_CORNER };

    public static final int ORIENTATIONS = 48;

    public static final int UNTURNED = 0;

    /**
     * The orientations whose slope fills the rest of the bounds of an unturned slope, for any sizes, and likewise for a
     * convex round slope and an unturned concave one. Orientations 6 and 25 look alike but can overlap.
     */
    public static final int[] COMPLEMENTS_OF_UNTURNED = { 10, 18, 34, 42 };

    private Shapes() {}

    public static LittleTileCutoutInfo shape(LittleTileShapeMode kind, Vector3i size, Vector3i pos, int orientation) {
        LittleTileCutoutInfo shape = new LittleTileCutoutInfo();
        shape.type = kind;
        shape.size = new Vector3i(size);
        shape.pos = new Vector3i(pos);
        shape.orientation = orientation;
        return shape;
    }

    /** A deformed box with the corners, relative to the shape's position. */
    public static LittleTileCutoutInfo deformedBox(Vector3i size, Vector3i pos, int orientation, Vector3i[] corners) {
        LittleTileCutoutInfo shape = shape(LittleTileShapeMode.DEFORMED_BOX, size, pos, orientation);
        shape.corners = LittleTileCutoutInfo.copyCorners(corners);
        return shape;
    }

    /** The same shape with its bounds moved by {@code pixels} along the axis. */
    public static LittleTileCutoutInfo movedAlong(LittleTileCutoutInfo shape, int axis, int pixels) {
        LittleTileCutoutInfo moved = new LittleTileCutoutInfo(shape);
        moved.pos = BlockSpace.movedAlong(shape.pos, axis, pixels);
        return moved;
    }

    /**
     * The four shapes of {@code kind} with the same bounds filling the rest of them, see
     * {@link #COMPLEMENTS_OF_UNTURNED}.
     */
    public static List<LittleTileCutoutInfo> complementsOf(LittleTileCutoutInfo shape, LittleTileShapeMode kind) {
        List<LittleTileCutoutInfo> complements = new ArrayList<>();
        for (int complementOfUnturned : COMPLEMENTS_OF_UNTURNED) {
            complements.add(shape(kind, shape.size, shape.pos, combined(shape.orientation, complementOfUnturned)));
        }
        return complements;
    }

    /** The shape with the block turned about its center. The bounds move along, and the shape turns with them. */
    public static LittleTileCutoutInfo turned(LittleTileCutoutInfo shape, int turn) {
        LittleTileBox bounds = turned(BlockSpace.bounds(shape), turn);
        LittleTileCutoutInfo turned = new LittleTileCutoutInfo(shape);
        turned.pos = BlockSpace.min(bounds);
        turned.size = BlockSpace.size(bounds);
        turned.orientation = combined(turn, shape.orientation);
        return turned;
    }

    /** The box with the block turned about its center. */
    public static LittleTileBox turned(LittleTileBox box, int turn) {
        Vector3i first = turnedPoint(BlockSpace.min(box), turn), second = turnedPoint(BlockSpace.max(box), turn);
        // On a flipped axis, the old maximum becomes the new minimum
        return BlockSpace.box(new Vector3i(first).min(second), new Vector3i(first).max(second));
    }

    public static String describe(LittleTileCutoutInfo shape) {
        return shape.type + " size=" + shape.size + " pos=" + shape.pos + " orientation=" + shape.orientation;
    }

    /** First {@code applied}, then {@code turn}. */
    private static int combined(int turn, int applied) {
        Matrix3f matrix = new Matrix3f(OrientationMapper.fromId(turn)).mul(OrientationMapper.fromId(applied));
        return OrientationMapper.toId(matrix);
    }

    private static Vector3i turnedPoint(Vector3i point, int turn) {
        Vector3f fromCenter = new Vector3f(point.x - CENTER, point.y - CENTER, point.z - CENTER);
        Vector3f turned = OrientationMapper.fromId(turn).transform(fromCenter);
        return new Vector3i(
                Math.round(CENTER + turned.x),
                Math.round(CENTER + turned.y),
                Math.round(CENTER + turned.z));
    }
}
