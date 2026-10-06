package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3d;

import com.creativemd.littletiles.client.render.LittleTilesFaceCuller;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Measures meshes in pixels, picks some of their faces, and culls them as rendering does. Reads the points only through
 * {@link Mesh3d#getVertices}: three in a row per triangle, in blocks.
 */
public final class MeshGeometry {

    /**
     * Distance in blocks within which two points are the same, or a point lies on a plane or line. Far below a pixel,
     * far above the rounding of the cuts.
     */
    public static final double EPSILON = 5.0E-6;

    private static final int TRIANGLE_CORNERS = 3;

    private MeshGeometry() {}

    /** Total area of the faces, in square pixels. */
    public static double area(Mesh3d mesh) {
        Vector3d[] vertices = mesh.getVertices();
        double area = 0;
        for (int first = 0; first < vertices.length; first += TRIANGLE_CORNERS) {
            area += normal(vertices[first], vertices[first + 1], vertices[first + 2]).length() / 2;
        }
        return area * PIXELS * PIXELS;
    }

    /** The volume the faces enclose, in cubic pixels, negative when they face inwards. The mesh must be closed. */
    public static double volume(Mesh3d mesh) {
        Vector3d[] vertices = mesh.getVertices();
        double sixTimesVolume = 0;
        for (int first = 0; first < vertices.length; first += TRIANGLE_CORNERS) {
            sixTimesVolume += vertices[first].dot(new Vector3d(vertices[first + 1]).cross(vertices[first + 2]));
        }
        return sixTimesVolume / 6 * PIXELS * PIXELS * PIXELS;
    }

    /** The normal of the triangle, as long as twice its area. */
    public static Vector3d normal(Vector3d a, Vector3d b, Vector3d c) {
        return new Vector3d(b).sub(a).cross(new Vector3d(c).sub(a));
    }

    /**
     * Looks for an edge of the mesh that leaves it open: every edge of a closed mesh is walked once in each direction,
     * by the two triangles meeting there. A cap that does not fit its outline, or a piece left out, leaves an edge
     * without its counterpart.
     * <p>
     * A point of the mesh lying inside an edge splits it first. The triangles on one side can have a point there that
     * the other side runs past in a single edge, as triangulating leaves out points in line with their neighbours. That
     * leaves no gap, so it counts as closed.
     *
     * @return a description of an open edge, in pixels, or null when the mesh is closed
     */
    public static String findOpenEdge(Mesh3d mesh) {
        List<Vector3d> points = new ArrayList<>();
        int[] cornerPoints = weldCorners(mesh.getVertices(), points);
        Map<List<Integer>, Integer> timesWalked = new HashMap<>();
        for (int first = 0; first < cornerPoints.length; first += TRIANGLE_CORNERS) {
            for (int corner = 0; corner < TRIANGLE_CORNERS; corner++) {
                int from = cornerPoints[first + corner];
                int to = cornerPoints[first + (corner + 1) % TRIANGLE_CORNERS];
                if (from == to) return "zero-length edge at " + toPixels(points.get(from));

                walkEdge(from, to, points, timesWalked);
            }
        }
        return describeUnmatchedEdge(timesWalked, points);
    }

    /** Merges the corners lying at the same point, returning the index in {@code points} of each corner's point. */
    private static int[] weldCorners(Vector3d[] corners, List<Vector3d> points) {
        int[] cornerPoints = new int[corners.length];
        for (int corner = 0; corner < corners.length; corner++) {
            cornerPoints[corner] = pointIndex(points, corners[corner]);
        }
        return cornerPoints;
    }

    /** The index of the point in {@code points}, added if it is not there yet. */
    private static int pointIndex(List<Vector3d> points, Vector3d point) {
        for (int index = 0; index < points.size(); index++) {
            if (points.get(index).distance(point) <= EPSILON) return index;
        }
        points.add(point);
        return points.size() - 1;
    }

    /** Counts the edge as walked once, piece by piece between the points lying inside it. */
    private static void walkEdge(int from, int to, List<Vector3d> points, Map<List<Integer>, Integer> timesWalked) {
        Vector3d start = points.get(from), end = points.get(to);
        List<Integer> inside = new ArrayList<>();
        for (int point = 0; point < points.size(); point++) {
            if (point != from && point != to && liesInside(start, end, points.get(point))) inside.add(point);
        }
        inside.sort((a, b) -> Double.compare(along(start, end, points.get(a)), along(start, end, points.get(b))));
        int previous = from;
        for (int point : inside) {
            timesWalked.merge(Arrays.asList(previous, point), 1, Integer::sum);
            previous = point;
        }
        timesWalked.merge(Arrays.asList(previous, to), 1, Integer::sum);
    }

    /** An edge walked a different number of times one way than back, or null if there is none. */
    private static String describeUnmatchedEdge(Map<List<Integer>, Integer> timesWalked, List<Vector3d> points) {
        for (Map.Entry<List<Integer>, Integer> edge : timesWalked.entrySet()) {
            int from = edge.getKey().get(0), to = edge.getKey().get(1);
            int back = timesWalked.getOrDefault(Arrays.asList(to, from), 0);
            if (back != edge.getValue()) {
                return "edge " + toPixels(points.get(from))
                        + " -> "
                        + toPixels(points.get(to))
                        + " walked "
                        + edge.getValue()
                        + " times, back "
                        + back
                        + " times";
            }
        }
        return null;
    }

    /** The point's projected distance along the edge, multiplied by the edge length. */
    private static double along(Vector3d start, Vector3d end, Vector3d point) {
        return new Vector3d(point).sub(start).dot(new Vector3d(end).sub(start));
    }

    /** Whether the point lies on the edge between its ends. */
    private static boolean liesInside(Vector3d start, Vector3d end, Vector3d point) {
        Vector3d edge = new Vector3d(end).sub(start);
        Vector3d offset = new Vector3d(point).sub(start);
        double lengthSquared = edge.lengthSquared();
        double along = offset.dot(edge);
        if (along <= 0 || along >= lengthSquared) return false;

        return edge.cross(offset).lengthSquared() <= EPSILON * EPSILON * lengthSquared;
    }

    private static String toPixels(Vector3d point) {
        return "(" + point.x * PIXELS + ", " + point.y * PIXELS + ", " + point.z * PIXELS + ")";
    }

    /** What stays visible of the faces once the faces of {@code occluder} lying in the same planes are culled away. */
    public static Mesh3d visiblePart(Mesh3d faces, Mesh3d occluder) {
        List<Triangle3d> visible = new ArrayList<>();
        for (Triangle3d triangle : faces.getTriangles()) {
            visible.addAll(LittleTilesFaceCuller.cutTriangle(triangle, occluder.getTriangles()));
        }
        return new Mesh3d(visible);
    }

    /** How much of the faces, in square pixels, culling them against {@code occluder} takes away. */
    public static double culledArea(Mesh3d faces, Mesh3d occluder) {
        return area(faces) - area(visiblePart(faces, occluder));
    }

    /**
     * A copy of the mesh of a tile in the neighbouring block along the axis, towards the maximum, moved next to this
     * block as rendering moves the neighbour's faces.
     */
    public static Mesh3d besideThisBlock(Mesh3d neighbourMesh, int axis) {
        Mesh3d moved = neighbourMesh.copy();
        moved.translate(BlockSpace.unit(axis).mul(PIXELS));
        return moved;
    }

    /** The faces of a plain box tile, as culling builds them. */
    public static Mesh3d boxFaces(LittleTileBox box) {
        List<Triangle3d> faces = new ArrayList<>();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            faces.addAll(LittleTilesFaceCuller.boxFaceTriangles(box.getCube(), side));
        }
        return new Mesh3d(faces);
    }

    /** Picks some faces of a mesh, by where its three corners lie. */
    public static final class FaceFilter {

        /**
         * Below this, a component of a face's unit normal does not tilt the face towards that axis. Far below the tilt
         * of the steepest facet of a round slope stretched 800 to 1, about 6e-5, far above the rounding of flat faces.
         */
        private static final double TILT_EPSILON = 1.0E-7;

        /**
         * The faces parallel to a block face. Unlike how far the corners spread, the normal stays exact however thin a
         * face is, so a long sliver of a barely tilted face still counts as tilted.
         */
        public static final FaceFilter FLAT = new FaceFilter(FaceFilter::isFlat);

        /** The faces not parallel to any block face, see {@link #FLAT}. */
        public static final FaceFilter TILTED = new FaceFilter(corners -> !isFlat(corners));

        private final Predicate<Vector3d[]> accepts;

        private FaceFilter(Predicate<Vector3d[]> accepts) {
            this.accepts = accepts;
        }

        /**
         * The faces lying in the plane {@code pixel} pixels along the axis from the block minimum, facing either way.
         */
        public static FaceFilter onPlane(int axis, int pixel) {
            double plane = BlockSpace.toBlocks(pixel);
            return new FaceFilter(
                    corners -> allMatch(corners, corner -> Math.abs(corner.get(axis) - plane) <= EPSILON));
        }

        /** The faces this filter picks, copied into a mesh of their own. */
        public Mesh3d of(Mesh3d mesh) {
            return select(mesh, true);
        }

        /** The faces this filter leaves out, copied into a mesh of their own. */
        public Mesh3d notOf(Mesh3d mesh) {
            return select(mesh, false);
        }

        private Mesh3d select(Mesh3d mesh, boolean picked) {
            Vector3d[] vertices = mesh.getVertices();
            List<Triangle3d> faces = new ArrayList<>();
            for (int face = 0; face < mesh.getTriangles().size(); face++) {
                Vector3d[] corners = { vertices[face * 3], vertices[face * 3 + 1], vertices[face * 3 + 2] };
                if (accepts.test(corners) == picked) faces.add(mesh.getTriangles().get(face).copy());
            }
            return new Mesh3d(faces);
        }

        private static boolean allMatch(Vector3d[] corners, Predicate<Vector3d> condition) {
            for (Vector3d corner : corners) {
                if (!condition.test(corner)) return false;
            }
            return true;
        }

        /** Whether the face's normal points along an axis: it leans towards at most one. */
        private static boolean isFlat(Vector3d[] corners) {
            Vector3d normal = normal(corners[0], corners[1], corners[2]).normalize();
            int axesLeanedTowards = 0;
            for (int axis = 0; axis < BlockSpace.AXES; axis++) {
                if (Math.abs(normal.get(axis)) > TILT_EPSILON) axesLeanedTowards++;
            }
            return axesLeanedTowards <= 1;
        }
    }
}
