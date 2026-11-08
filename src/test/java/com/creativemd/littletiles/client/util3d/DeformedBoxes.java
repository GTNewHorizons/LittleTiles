package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.AXES;
import static com.creativemd.littletiles.client.util3d.BlockSpace.CENTER;
import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

import org.joml.Vector3i;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.MeshGeometry.FaceFilter;

/**
 * Random corners of deformed boxes, in pixels. The eight corners of a box are indexed by one bit per axis, set where
 * the corner lies at the maximum along that axis. The four corners of a box face across an axis are indexed by the bits
 * of the two other axes, in the order x, y, z, x, y: the first as bit 0, the second as bit 1.
 */
public final class DeformedBoxes {

    /** How far corners move from the block's corners when they only deform the box slightly. */
    public static final int MAX_CORNER_SHIFT = 2;

    /** How often a strongly moved corner snaps onto a neighbouring corner, collapsing an edge: once in this many. */
    private static final int SNAP_ONE_IN = 4;

    private static final int FACE_CORNERS = 4;

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

    /**
     * The block's corners, each moved on its own. Half of the time by up to {@link #MAX_CORNER_SHIFT} pixels, which
     * warps the faces slightly, else anywhere in the block's half on the corner's side of every axis, sometimes snapped
     * onto a neighbouring corner, which collapses edges and faces as in wedges and pyramids. They may squash the box
     * flat, see {@link Mesh3dUtil#enclosesVolume}, or fold it, see {@link #foldsOver}.
     */
    public static Vector3i[] warped(Random random) {
        if (random.nextBoolean()) {
            return boxCorners(
                    corner -> blockCorner(corner)
                            .add(BlockSpace.vector(axis -> Draw.between(random, -MAX_CORNER_SHIFT, MAX_CORNER_SHIFT))));
        }
        Vector3i[] corners = boxCorners(
                corner -> BlockSpace.vector(
                        axis -> isAtMax(corner, axis) ? Draw.between(random, CENTER, PIXELS)
                                : Draw.between(random, 0, CENTER)));
        for (int corner = 0; corner < corners.length; corner++) {
            if (random.nextInt(SNAP_ONE_IN) == 0) {
                int neighbour = corner ^ (1 << Draw.axis(random));
                corners[corner].set(corners[neighbour]);
            }
        }
        return corners;
    }

    /** Two deformed boxes sharing a face, see {@link Neighbours}. */
    public static Neighbours neighbours(Random random) {
        return Draw.until(() -> Neighbours.draw(random), Neighbours::onlyTouch);
    }

    public static boolean isAtMax(int corner, int axis) {
        return (corner & (1 << axis)) != 0;
    }

    /** Which corner of the box's faces across the axis the box corner lies on. */
    public static int faceCorner(int corner, int axis) {
        int first = isAtMax(corner, (axis + 1) % AXES) ? 1 : 0;
        int second = isAtMax(corner, (axis + 2) % AXES) ? 1 : 0;
        return first | (second << 1);
    }

    /** The box corner lying on the face corner, on the face across the axis at its minimum. */
    public static int boxCornerAtMin(int faceCorner, int axis) {
        int first = faceCorner & 1, second = (faceCorner >> 1) & 1;
        return (first << ((axis + 1) % AXES)) | (second << ((axis + 2) % AXES));
    }

    public static Vector3i blockCorner(int corner) {
        return BlockSpace.vector(axis -> isAtMax(corner, axis) ? PIXELS : 0);
    }

    public static Vector3i[] boxCorners(IntFunction<Vector3i> corner) {
        Vector3i[] corners = new Vector3i[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
        for (int index = 0; index < corners.length; index++) corners[index] = corner.apply(index);
        return corners;
    }

    /**
     * A corner at {@code along} on the axis, near the block edge along that axis through the face corner, moved inwards
     * by up to {@link #MAX_CORNER_SHIFT} pixels on the other two axes.
     */
    public static Vector3i nearBlockEdge(Random random, int axis, int faceCorner, int along) {
        Vector3i edgeCorner = blockCorner(boxCornerAtMin(faceCorner, axis));
        return BlockSpace.vector(other -> {
            if (other == axis) return along;
            int inwards = Draw.upTo(random, MAX_CORNER_SHIFT);
            return edgeCorner.get(other) == PIXELS ? PIXELS - inwards : inwards;
        });
    }

    /** Whether the four corners lie in one plane. */
    public static boolean isPlanar(Vector3i[] corners) {
        return sideOfPlane(normal(corners[0], corners[1], corners[2]), corners[0], corners[3]) == 0;
    }

    /** The plane's normal through the corners, as exact integers. */
    public static long[] normal(Vector3i a, Vector3i b, Vector3i c) {
        Vector3i u = new Vector3i(b).sub(a), v = new Vector3i(c).sub(a);
        return new long[] { (long) u.y * v.z - (long) u.z * v.y, (long) u.z * v.x - (long) u.x * v.z,
                (long) u.x * v.y - (long) u.y * v.x };
    }

    /** On which side of the plane through {@code origin} the point lies: 1, -1, or 0 on it. */
    public static int sideOfPlane(long[] normal, Vector3i origin, Vector3i point) {
        Vector3i offset = new Vector3i(point).sub(origin);
        return Long.signum(normal[0] * offset.x + normal[1] * offset.y + normal[2] * offset.z);
    }

    /**
     * Whether the surface of the deformed box, inside the block, folds through itself, which turns it inside out in
     * places. Exact, on the corners. Faces overlapping in a plane are left to {@link Mesh3dUtil#enclosesVolume}.
     * <p>
     * A surface folds where it crosses itself, which shows as an edge passing right through a triangle, or where more
     * than two triangles meet in an edge and two of them following each other around it run along it the same way: the
     * wedge between them then lies inside, or outside, twice. Corners touching faces or edges elsewhere fold nothing.
     */
    public static boolean foldsOver(Vector3i[] corners) {
        List<Vector3i[]> triangles = new ArrayList<>();
        for (Triangle3d triangle : Tile.deformedBox(corners).mesh().getTriangles()) {
            triangles.add(
                    new Vector3i[] { pixels(triangle.getP1()), pixels(triangle.getP2()), pixels(triangle.getP3()) });
        }
        for (Vector3i[] triangle : triangles) {
            for (int i = 0; i < 3; i++) {
                Vector3i from = triangle[i], to = triangle[(i + 1) % 3];
                if (!alternateAround(from, to, triangles)) return true;
                for (Vector3i[] other : triangles) {
                    if (passesThrough(from, to, other)) return true;
                    for (int j = 0; j < 3; j++) {
                        if (passesThroughEdge(from, to, other, j, triangles)) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether the edge crosses the triangle's edge {@code j} inside both, and goes from inside the surface along that
     * edge to outside it, so that the creases of the surface along the two edges pass through each other. Creases only
     * touching keep the edge on one side.
     */
    private static boolean passesThroughEdge(Vector3i from, Vector3i to, Vector3i[] triangle, int j,
            List<Vector3i[]> triangles) {
        Vector3i start = triangle[j], end = triangle[(j + 1) % 3];
        if (!edgesCross(from, to, start, end)) return false;
        // The triangle running along the edge the other way
        Vector3i[] beside = null;
        for (Vector3i[] other : triangles) {
            for (int i = 0; i < 3; i++) {
                if (other[i].equals(end) && other[(i + 1) % 3].equals(start)) beside = other;
            }
        }
        if (beside == null) return false;
        return sideOfSurface(from, triangle, beside, triangle[(j + 2) % 3])
                * sideOfSurface(to, triangle, beside, triangle[(j + 2) % 3]) < 0;
    }

    /** Whether the two edges, sharing no end, lie in one plane and cross each other inside both. */
    private static boolean edgesCross(Vector3i from, Vector3i to, Vector3i start, Vector3i end) {
        if (from.equals(start) || from.equals(end) || to.equals(start) || to.equals(end)) return false;
        long[] up = normal(from, to, start);
        if (up[0] == 0 && up[1] == 0 && up[2] == 0 || sideOfPlane(up, from, end) != 0) return false;
        return turn(from, to, start, up) * turn(from, to, end, up) < 0
                && turn(start, end, from, up) * turn(start, end, to, up) < 0;
    }

    /** Which way the line from {@code a} through {@code b} turns to reach {@code c}, looking down {@code up}. */
    private static int turn(Vector3i a, Vector3i b, Vector3i c, long[] up) {
        long[] normal = normal(a, b, c);
        return Long.signum(normal[0] * up[0] + normal[1] * up[1] + normal[2] * up[2]);
    }

    /**
     * On which side of the surface along the edge shared by the two triangles, wound outwards, the point lies, as seen
     * from right next to the edge: -1 inside, 1 outside, 0 on it or undecided. {@code apex} is the first triangle's
     * corner off the edge.
     */
    private static int sideOfSurface(Vector3i point, Vector3i[] first, Vector3i[] second, Vector3i apex) {
        long[] firstNormal = normal(first[0], first[1], first[2]);
        long[] secondNormal = normal(second[0], second[1], second[2]);
        int firstSide = sideOfPlane(firstNormal, first[0], point);
        int secondSide = sideOfPlane(secondNormal, second[0], point);
        // Along a convex edge, the inside lies below both planes, along a concave one below either
        boolean convex = sideOfPlane(secondNormal, second[0], apex) <= 0;
        if (convex) {
            if (firstSide > 0 || secondSide > 0) return 1;
            return firstSide < 0 && secondSide < 0 ? -1 : 0;
        }
        if (firstSide < 0 || secondSide < 0) return -1;
        return firstSide > 0 && secondSide > 0 ? 1 : 0;
    }

    private static Vector3i pixels(Vector3d point) {
        return new Vector3i(
                (int) Math.round(point.x * PIXELS),
                (int) Math.round(point.y * PIXELS),
                (int) Math.round(point.z * PIXELS));
    }

    /** Whether the edge passes through the inside of the triangle, its ends on either side of the triangle's plane. */
    private static boolean passesThrough(Vector3i from, Vector3i to, Vector3i[] triangle) {
        long[] normal = normal(triangle[0], triangle[1], triangle[2]);
        if (sideOfPlane(normal, triangle[0], from) * sideOfPlane(normal, triangle[0], to) >= 0) return false;
        // On the same side of the edge's line, looking along it, as each side of the triangle
        int first = sideOfPlane(normal(from, to, triangle[0]), from, triangle[1]);
        int second = sideOfPlane(normal(from, to, triangle[1]), from, triangle[2]);
        int third = sideOfPlane(normal(from, to, triangle[2]), from, triangle[0]);
        return first != 0 && first == second && second == third;
    }

    /**
     * Whether the triangles meeting in the edge run along it in turns one way and the other, going round it. Always so
     * for two: those of a closed surface run along their shared edge both ways.
     */
    private static boolean alternateAround(Vector3i from, Vector3i to, List<Vector3i[]> triangles) {
        // The corner of each triangle off the edge, and whether it runs from the edge's start to its end
        List<Vector3i> spokes = new ArrayList<>();
        List<Boolean> forwards = new ArrayList<>();
        for (Vector3i[] triangle : triangles) {
            for (int i = 0; i < 3; i++) {
                Vector3i start = triangle[i], end = triangle[(i + 1) % 3];
                if (start.equals(from) && end.equals(to) || start.equals(to) && end.equals(from)) {
                    spokes.add(triangle[(i + 2) % 3]);
                    forwards.add(start.equals(from));
                }
            }
        }
        if (spokes.size() <= 2) return true;

        Integer[] around = new Integer[spokes.size()];
        for (int i = 0; i < around.length; i++) around[i] = i;
        Vector3i reference = spokes.get(0);
        Arrays.sort(around, (first, second) -> {
            int firstHalf = halfAround(from, to, reference, spokes.get(first));
            int secondHalf = halfAround(from, to, reference, spokes.get(second));
            if (firstHalf != secondHalf) return firstHalf - secondHalf;
            // Turning right-handed about the edge from the first spoke to the second
            return -sideOfPlane(normal(from, spokes.get(first), spokes.get(second)), from, to);
        });
        for (int i = 0; i < around.length; i++) {
            if (forwards.get(around[i]).equals(forwards.get(around[(i + 1) % around.length]))) return false;
        }
        return true;
    }

    /**
     * Which half turn about the edge, right-handed from the reference spoke, the spoke lies in: 0 for the first, from
     * the reference on, 1 for the second.
     */
    private static int halfAround(Vector3i from, Vector3i to, Vector3i reference, Vector3i spoke) {
        int side = sideOfPlane(normal(from, reference, spoke), from, to);
        if (side != 0) return side > 0 ? 0 : 1;
        // In the reference's half plane, or the opposite one: by the spoke's direction across the edge
        Vector3i axis = new Vector3i(to).sub(from);
        Vector3i towardsReference = new Vector3i(reference).sub(from), towardsSpoke = new Vector3i(spoke).sub(from);
        long across = dot(towardsReference, towardsSpoke) * dot(axis, axis)
                - dot(towardsReference, axis) * dot(towardsSpoke, axis);
        return across > 0 ? 0 : 1;
    }

    private static long dot(Vector3i a, Vector3i b) {
        return (long) a.x * b.x + (long) a.y * b.y + (long) a.z * b.z;
    }

    /**
     * The block split near a pixel plane into two deformed boxes sharing the face between them, with every corner moved
     * by up to {@link #MAX_CORNER_SHIFT} pixels. The low box lies towards the minimum along the axis. Both boxes
     * enclose volume and lie on opposite sides of the shared face, however it is split.
     */
    public static final class Neighbours {

        /**
         * The two ways to split the shared face into triangles, by their face corners. Both triangles of a split are
         * wound the same way round.
         */
        private static final int[][] SPLIT_ALONG_FIRST_DIAGONAL = { { 0, 1, 3 }, { 0, 3, 2 } },
                SPLIT_ALONG_SECOND_DIAGONAL = { { 0, 1, 2 }, { 1, 3, 2 } };

        public final int axis;
        /** The corners of the shared face, indexed as face corners. */
        private final Vector3i[] sharedCorners;
        private final Vector3i[] lowCorners, highCorners;
        public final Tile low, high;

        private Neighbours(int axis, Vector3i[] sharedCorners, Vector3i[] lowCorners, Vector3i[] highCorners) {
            this.axis = axis;
            this.sharedCorners = sharedCorners;
            this.lowCorners = lowCorners;
            this.highCorners = highCorners;
            low = Tile.deformedBox(lowCorners);
            high = Tile.deformedBox(highCorners);
        }

        private static Neighbours draw(Random random) {
            int axis = Draw.axis(random);
            // Far enough inside that the shared face's corners stay clear of the outer faces' corners
            int at = Draw.between(random, 2 * MAX_CORNER_SHIFT, PIXELS - 2 * MAX_CORNER_SHIFT);
            Vector3i[] shared = new Vector3i[FACE_CORNERS];
            for (int faceCorner = 0; faceCorner < FACE_CORNERS; faceCorner++) {
                int along = at + Draw.between(random, -MAX_CORNER_SHIFT, MAX_CORNER_SHIFT);
                shared[faceCorner] = nearBlockEdge(random, axis, faceCorner, along);
            }
            Vector3i[] low = boxCorners(corner -> {
                int faceCorner = faceCorner(corner, axis);
                return isAtMax(corner, axis) ? new Vector3i(shared[faceCorner])
                        : nearBlockEdge(random, axis, faceCorner, Draw.upTo(random, MAX_CORNER_SHIFT));
            });
            Vector3i[] high = boxCorners(corner -> {
                int faceCorner = faceCorner(corner, axis);
                return isAtMax(corner, axis)
                        ? nearBlockEdge(random, axis, faceCorner, PIXELS - Draw.upTo(random, MAX_CORNER_SHIFT))
                        : new Vector3i(shared[faceCorner]);
            });
            return new Neighbours(axis, shared, low, high);
        }

        /**
         * Whether both boxes enclose volume, and lie strictly on opposite sides of the shared face split either way. A
         * corner moved past the shared face would fold its box over into the neighbour.
         */
        private boolean onlyTouch() {
            return Mesh3dUtil.enclosesVolume(lowCorners) && Mesh3dUtil.enclosesVolume(highCorners)
                    && onOppositeSides(SPLIT_ALONG_FIRST_DIAGONAL)
                    && onOppositeSides(SPLIT_ALONG_SECOND_DIAGONAL);
        }

        private boolean onOppositeSides(int[][] split) {
            for (int[] triangle : split) {
                Vector3i origin = sharedCorners[triangle[0]];
                long[] normal = normal(origin, sharedCorners[triangle[1]], sharedCorners[triangle[2]]);
                int lowSide = sideOfCornersOffSharedFace(lowCorners, false, normal, origin);
                int highSide = sideOfCornersOffSharedFace(highCorners, true, normal, origin);
                if (lowSide == 0 || highSide == 0 || lowSide == highSide) return false;
            }
            return true;
        }

        /**
         * The side of the plane the box's corners off the shared face lie on, 1 or -1, or 0 if they don't all lie
         * strictly on one side.
         *
         * @param offAtMax whether the corners off the shared face lie at the maximum along the axis
         */
        private int sideOfCornersOffSharedFace(Vector3i[] corners, boolean offAtMax, long[] normal, Vector3i origin) {
            int side = 0;
            for (int corner = 0; corner < corners.length; corner++) {
                if (isAtMax(corner, axis) != offAtMax) continue;

                int cornerSide = sideOfPlane(normal, origin, corners[corner]);
                if (cornerSide == 0 || side != 0 && cornerSide != side) return 0;
                side = cornerSide;
            }
            return side;
        }

        /** Whether both boxes split the shared face along the same diagonal, which only matters when it is warped. */
        public boolean splitSharedFaceAlike() {
            if (isPlanar(sharedCorners)) return true;
            Vector3i first = sharedCorners[0], last = sharedCorners[3];
            return MeshGeometry.hasEdge(low.mesh(), first, last) == MeshGeometry.hasEdge(high.mesh(), first, last);
        }

        /** The high box moved a pixel into the low one. */
        public Tile highMovedIntoLow() {
            return Tile.deformedBox(
                    Arrays.stream(highCorners).map(corner -> BlockSpace.movedAlong(corner, axis, -1))
                            .toArray(Vector3i[]::new));
        }

        /** The faces lying in the shared face. */
        public FaceFilter sharedFace() {
            return FaceFilter.between(sharedCorners);
        }

        @Override
        public String toString() {
            return "low=" + Arrays.toString(lowCorners) + " high=" + Arrays.toString(highCorners);
        }
    }
}
