package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Cuts a closed mesh by planes, closing each cut with a cap.
 * <p>
 * While cutting, faces refer to their corners by index into one shared list of points. A plane crossing an edge makes
 * one new point, which both faces along that edge use, and caps are joined from edges by index. So which faces fit
 * together never depends on comparing positions, and a closed mesh stays closed.
 * <p>
 * Faces are convex polygons rather than triangles: the polygons of the mesh are put back together from the triangles
 * they were split into, see {@link Triangle3d#getPolygon}, and stay polygons until all cuts are done. A plane then only
 * ever crosses the edges of the polygons and of earlier caps, never the diagonals of a triangulation, and fewer points
 * make fewer triangles to draw and cull.
 */
public final class MeshCutter {

    /**
     * The grid the points of the cut mesh are rounded to, in blocks, far finer than anything visible. Points cuts make
     * right next to each other become one rather than making slivers, and points on the plane of a pixel stay on it.
     */
    public static final double RENDER_GRID = 0x1p-17;

    /**
     * Volume up to which, in cubic blocks, a mesh or what two meshes share counts as none: a millionth of a cubic
     * pixel. Far above rounding, far below anything a player could see or aim at.
     */
    public static final double VOLUME_EPSILON = 1.0E-6 / (16 * 16 * 16);

    private final List<Vector3d> points = new ArrayList<>();
    /** Convex polygons wound counterclockwise around their outward normal. */
    private List<Face> faces = new ArrayList<>();

    private static final class Face {

        /** Indices into the cutter's shared list of points. */
        final int[] corners;
        /** The plane of the uncut face, see {@link Triangle3d#getFacePlane}. */
        final Plane3d plane;

        Face(int[] corners, Plane3d plane) {
            this.corners = corners;
            this.plane = plane;
        }
    }

    private MeshCutter() {}

    /** @param mesh a closed mesh */
    public MeshCutter(List<Triangle3d> mesh) {
        // Corners at the same position are the same point: the triangles of a mesh get them from the same vertex
        Object2IntOpenHashMap<Vector3d> indices = new Object2IntOpenHashMap<>();
        indices.defaultReturnValue(-1);
        // The edges around each polygon: those of its triangles, less the diagonals between them, walked both ways
        Map<Object, LongArrayList> outlines = new LinkedHashMap<>();
        List<Plane3d> planes = new ArrayList<>();
        for (Triangle3d triangle : mesh) {
            int a = index(indices, triangle.getP1()), b = index(indices, triangle.getP2()),
                    c = index(indices, triangle.getP3());
            // A triangle without a side has its other two cancel out
            if (a == b || b == c || c == a) continue;
            Object polygon = triangle.getPolygon() != null ? triangle.getPolygon() : triangle;
            LongArrayList outline = outlines.get(polygon);
            if (outline == null) {
                outlines.put(polygon, outline = new LongArrayList());
                planes.add(triangle.getFacePlane());
            }
            walk(outline, a, b);
            walk(outline, b, c);
            walk(outline, c, a);
        }
        int polygon = 0;
        for (LongArrayList outline : outlines.values()) {
            for (int[] face : loops(outline)) {
                faces.add(new Face(face, planes.get(polygon)));
            }
            polygon++;
        }
    }

    private int index(Object2IntOpenHashMap<Vector3d> indices, Vector3d point) {
        // + 0.0 turns -0.0 into 0.0, which equals it but hashes differently
        Vector3d key = new Vector3d(point.x + 0.0, point.y + 0.0, point.z + 0.0);
        int index = indices.getInt(key);
        if (index < 0) {
            index = points.size();
            points.add(key);
            indices.put(key, index);
        }
        return index;
    }

    /** The box, in blocks. */
    public static MeshCutter box(LittleTileBox box) {
        MeshCutter cutter = new MeshCutter();
        for (int corner = 0; corner < Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT; corner++) {
            cutter.points.add(
                    new Vector3d(
                            ((corner & 1) == 0 ? box.minX : box.maxX) / 16.0,
                            ((corner & 2) == 0 ? box.minY : box.maxY) / 16.0,
                            ((corner & 4) == 0 ? box.minZ : box.maxZ) / 16.0));
        }
        for (int[] inwards : Mesh3dUtil.DEFORMED_BOX_FACES) {
            int[] face = { inwards[3], inwards[2], inwards[1], inwards[0] };
            cutter.faces.add(
                    new Face(
                            face,
                            Triangle3d.planeThrough(
                                    cutter.points.get(face[0]),
                                    cutter.points.get(face[1]),
                                    cutter.points.get(face[2]))));
        }
        return cutter;
    }

    /** A copy to cut on its own. */
    public MeshCutter copy() {
        MeshCutter copy = new MeshCutter();
        copy.points.addAll(points);
        copy.faces.addAll(faces);
        return copy;
    }

    /** The planes of the faces, each once, see {@link Triangle3d#getFacePlane}. */
    public List<Plane3d> facePlanes() {
        List<Plane3d> planes = new ArrayList<>();
        for (Face face : faces) {
            if (!planes.contains(face.plane)) planes.add(face.plane);
        }
        return planes;
    }

    /** Whether nothing is left of the mesh. */
    public boolean isEmpty() {
        return faces.isEmpty();
    }

    /**
     * Cuts the mesh down to the side of the plane its normal points away from.
     *
     * @return false if the cut could not be closed, which only happens to meshes that weren't closed
     */
    public boolean cut(Plane3d plane) {
        int corners = 0;
        for (Face polygon : faces) {
            corners += polygon.corners.length;
        }
        // With room for the points the cut makes, which all lie in the plane
        double[] distances = new double[points.size() + corners];
        // Worked out once per point, so the faces sharing it agree on its side whatever the rounding
        for (int i = 0; i < points.size(); i++) {
            distances[i] = plane.getDistance(points.get(i));
        }

        List<Face> keptFaces = new ArrayList<>(faces.size());
        // The edges crossing the plane, each making the point after the points of the uncut mesh at its position
        int firstCrossing = points.size();
        LongArrayList crossed = new LongArrayList();
        // The edges in the plane the kept faces walk, less those walked both ways
        LongArrayList edgesInPlane = new LongArrayList();
        for (Face polygon : faces) {
            int[] face = polygon.corners;
            if (isBelow(face, distances)) {
                keptFaces.add(polygon);
                continue;
            }
            // Sutherland-Hodgman: the corners on the kept side, and where the edges cross the plane
            int[] kept = new int[face.length * 2];
            int size = 0;
            boolean inPlane = true;
            for (int i = 0; i < face.length; i++) {
                int from = face[i], to = face[(i + 1) % face.length];
                if (distances[from] <= 0) kept[size++] = from;
                if (distances[from] < 0 && distances[to] > 0 || distances[from] > 0 && distances[to] < 0) {
                    kept[size++] = crossing(crossed, firstCrossing, plane, from, to);
                }
                inPlane &= distances[from] == 0;
            }
            // A face lying in the plane is left to the cap
            if (size < 3 || inPlane) continue;

            keptFaces.add(new Face(size == kept.length ? kept : Arrays.copyOf(kept, size), polygon.plane));
            for (int i = 0; i < size; i++) {
                int from = kept[i], to = kept[(i + 1) % size];
                if (distances[from] == 0 && distances[to] == 0) {
                    // The cap walks them the other way round
                    walk(edgesInPlane, to, from);
                }
            }
        }
        faces = keptFaces;
        // Closes the cut with caps, joined from the open edges in the plane
        List<int[]> loops = loops(edgesInPlane);
        if (loops == null) return false;
        for (int[] loop : bridgeHoles(loops, plane.getNormal())) {
            addCap(plane, loop);
        }
        return true;
    }

    /** Whether all corners of the face lie below the plane, rather than in or above it. */
    private static boolean isBelow(int[] face, double[] distances) {
        for (int corner : face) {
            if (distances[corner] >= 0) return false;
        }
        return true;
    }

    private static long key(int from, int to) {
        return (long) from << 32 | to;
    }

    /** Adds the edge, unless it cancels out against the same edge walked the other way. */
    private static void walk(LongArrayList edges, int from, int to) {
        int reverse = edges.indexOf(key(to, from));
        if (reverse < 0) {
            edges.add(key(from, to));
        } else {
            edges.removeLong(reverse);
        }
    }

    /** The point where the plane crosses the edge, made once per edge. */
    private int crossing(LongArrayList crossed, int firstCrossing, Plane3d plane, int from, int to) {
        // The same way round from both faces along the edge
        long key = from < to ? key(from, to) : key(to, from);
        int index = crossed.indexOf(key);
        if (index < 0) {
            index = crossed.size();
            crossed.add(key);
            points.add(plane.intersect(points.get(Math.min(from, to)), points.get(Math.max(from, to))));
        }
        return firstCrossing + index;
    }

    /** Joins the edges into loops, using them up, or null if they don't close. */
    private static List<int[]> loops(LongArrayList edges) {
        List<int[]> loops = new ArrayList<>();
        while (!edges.isEmpty()) {
            IntArrayList loop = new IntArrayList();
            int start = (int) (edges.getLong(0) >>> 32), current = start;
            do {
                int edge = 0;
                while (edge < edges.size() && (int) (edges.getLong(edge) >>> 32) != current) edge++;
                if (edge == edges.size()) return null;
                loop.add(current);
                current = (int) edges.removeLong(edge);
            } while (current != start);
            loops.add(loop.toIntArray());
        }
        return loops;
    }

    /**
     * The loops of a cap, each hole joined into the loop around it by a bridge there and back, so that every part of
     * the cap is a single loop. Loops wound counterclockwise around the normal bound the cap, clockwise ones are holes
     * in it, like where a solid turned inside out is cut through.
     */
    private List<int[]> bridgeHoles(List<int[]> loops, Vector3d normal) {
        if (loops.size() < 2) return loops;
        List<IntArrayList> outlines = new ArrayList<>(), holes = new ArrayList<>();
        for (int[] loop : loops) {
            IntArrayList list = new IntArrayList(loop);
            (area(list, normal) >= 0 ? outlines : holes).add(list);
        }
        List<int[]> caps = new ArrayList<>(loops.size());
        while (!holes.isEmpty()) {
            IntArrayList hole = holes.remove(holes.size() - 1), around = null;
            // The smallest outline around it, or one touching it, which can only be from inside
            for (IntArrayList outline : outlines) {
                if (sharedPoint(outline, hole) >= 0) {
                    around = outline;
                    break;
                }
                if (encloses(outline, hole, normal) && (around == null || area(outline, normal) < area(around, normal)))
                    around = outline;
            }
            if (around == null) {
                // Only for meshes folded over themselves: capped on its own, which still keeps the mesh closed
                caps.add(hole.toIntArray());
            } else {
                bridge(around, hole, outlines, holes, normal);
            }
        }
        for (IntArrayList outline : outlines) {
            caps.add(outline.toIntArray());
        }
        return caps;
    }

    /** Twice the area of the loop, negative if it is wound clockwise around the normal. */
    private double area(IntArrayList loop, Vector3d normal) {
        double area = 0;
        for (int i = 1; i + 1 < loop.size(); i++) {
            area += cross(loop.getInt(0), loop.getInt(i), loop.getInt(i + 1), normal);
        }
        return area;
    }

    /**
     * Whether the outline goes around the hole, by the winding number around the points of the hole. A point can lie
     * right on the outline where the hole touches it, so one point inside is enough, as loops don't cross.
     */
    private boolean encloses(IntArrayList outline, IntArrayList hole, Vector3d normal) {
        // Counts the edges crossing the line through the point along up, any direction in the plane
        Vector3d up = new Vector3d(Math.abs(normal.x) < 0.5 ? 1 : 0, Math.abs(normal.x) < 0.5 ? 0 : 1, 0);
        up.cross(normal, up);
        for (int j = 0; j < hole.size(); j++) {
            int point = hole.getInt(j);
            double height = up.dot(points.get(point));
            int winding = 0;
            for (int i = 0; i < outline.size(); i++) {
                int a = outline.getInt(i), b = outline.getInt((i + 1) % outline.size());
                boolean aBelow = up.dot(points.get(a)) <= height, bBelow = up.dot(points.get(b)) <= height;
                if (aBelow && !bBelow && cross(a, b, point, normal) > 0) winding++;
                if (!aBelow && bBelow && cross(a, b, point, normal) < 0) winding--;
            }
            if (winding != 0) return true;
        }
        return false;
    }

    /**
     * Joins the hole into the loop around it, by the shortest bridge between them that crosses no edge of any loop: the
     * loop runs over to the hole, around it and back. Where they touch, the bridge has no length.
     */
    private void bridge(IntArrayList around, IntArrayList hole, List<IntArrayList> outlines, List<IntArrayList> holes,
            Vector3d normal) {
        List<IntArrayList> blocking = new ArrayList<>(outlines);
        blocking.addAll(holes);
        blocking.add(hole);
        // Only for caps rounding has tangled up, no bridge is free: any still keeps the cap closed
        int from = 0, to = 0;
        double shortest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < around.size(); i++) {
            for (int j = 0; j < hole.size(); j++) {
                double length = Mesh3d.distanceSquared(points.get(around.getInt(i)), points.get(hole.getInt(j)));
                if (length < shortest && !crossesAny(around.getInt(i), hole.getInt(j), blocking, normal)) {
                    shortest = length;
                    from = i;
                    to = j;
                }
            }
        }
        // A shared point is passed once on the way there and once on the way back
        int same = around.getInt(from) == hole.getInt(to) ? 1 : 0;
        IntArrayList bridged = new IntArrayList(around.size() + hole.size() + 2);
        bridged.addElements(0, around.elements(), 0, from + 1);
        for (int j = same; j <= hole.size(); j++) {
            bridged.add(hole.getInt((to + j) % hole.size()));
        }
        bridged.addElements(bridged.size(), around.elements(), from + same, around.size() - from - same);
        around.clear();
        around.addAll(bridged);
    }

    /** A point both loops run through, or -1. */
    private static int sharedPoint(IntArrayList a, IntArrayList b) {
        for (int i = 0; i < b.size(); i++) {
            if (a.contains(b.getInt(i))) return b.getInt(i);
        }
        return -1;
    }

    /** Whether the segment between the points crosses an edge of the loops, other than at its ends. */
    private boolean crossesAny(int a, int b, List<IntArrayList> loops, Vector3d normal) {
        for (IntArrayList loop : loops) {
            for (int i = 0; i < loop.size(); i++) {
                int c = loop.getInt(i), d = loop.getInt((i + 1) % loop.size());
                if (c == a || c == b || d == a || d == b) continue;
                if (cross(a, b, c, normal) * cross(a, b, d, normal) < 0
                        && cross(c, d, a, normal) * cross(c, d, b, normal) < 0)
                    return true;
            }
        }
        return false;
    }

    /**
     * Adds the cap as convex faces: clips ears off it until what is left is convex. Keeps every point of the loop, so
     * that the cap meets the faces around it in the very same points: earcut would leave out points lying in line with
     * their neighbours. Clips the ear with the shortest diagonal first, which keeps the triangles from getting thin.
     */
    private void addCap(Plane3d plane, int[] loop) {
        Vector3d normal = plane.getNormal();
        IntArrayList remaining = new IntArrayList(loop);
        while (remaining.size() > 3) {
            int size = remaining.size();
            double[] turns = new double[size];
            boolean convex = true;
            for (int i = 0; i < size; i++) {
                turns[i] = cross(
                        remaining.getInt((i + size - 1) % size),
                        remaining.getInt(i),
                        remaining.getInt((i + 1) % size),
                        normal);
                convex &= turns[i] >= 0;
            }
            if (convex) break;
            int ear = -1, mostConvex = 0;
            double shortest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < size; i++) {
                if (turns[i] > turns[mostConvex]) mostConvex = i;
                double diagonal = Mesh3d.distanceSquared(
                        points.get(remaining.getInt((i + size - 1) % size)),
                        points.get(remaining.getInt((i + 1) % size)));
                if (turns[i] >= 0 && diagonal < shortest && isEar(remaining, turns, i, normal)) {
                    shortest = diagonal;
                    ear = i;
                }
            }
            // Only for caps rounding has tangled up: clipping the most convex corner still keeps the cap closed
            if (ear < 0) ear = mostConvex;
            faces.add(
                    new Face(
                            new int[] { remaining.getInt((ear + size - 1) % size), remaining.getInt(ear),
                                    remaining.getInt((ear + 1) % size) },
                            plane));
            remaining.removeInt(ear);
        }
        faces.add(new Face(remaining.toIntArray(), plane));
    }

    /** Twice the signed area of the triangle, positive when it turns counterclockwise around the normal. */
    private double cross(int a, int b, int c, Vector3d normal) {
        Vector3d pa = points.get(a), pb = points.get(b), pc = points.get(c);
        double bx = pb.x - pa.x, by = pb.y - pa.y, bz = pb.z - pa.z;
        double cx = pc.x - pa.x, cy = pc.y - pa.y, cz = pc.z - pa.z;
        return normal.x * (by * cz - bz * cy) + normal.y * (bz * cx - bx * cz) + normal.z * (bx * cy - by * cx);
    }

    /**
     * Whether no other point of the loop lies in the triangle the corner makes with its neighbours. Only corners
     * turning right or going straight on can: the loop can only come into the triangle and leave it again through the
     * same side by turning right in it, or by running along the side from one corner to the other.
     */
    private boolean isEar(IntArrayList loop, double[] turns, int corner, Vector3d normal) {
        int size = loop.size();
        int a = loop.getInt((corner + size - 1) % size), b = loop.getInt(corner), c = loop.getInt((corner + 1) % size);
        for (int i = 0; i < size; i++) {
            int point = loop.getInt(i);
            if (turns[i] > 0 || point == a || point == b || point == c) continue;
            if (cross(a, b, point, normal) >= 0 && cross(b, c, point, normal) >= 0 && cross(c, a, point, normal) >= 0)
                return false;
        }
        return true;
    }

    /**
     * Six times the enclosed volume. Taken from a corner of the faces, so that it is exactly 0 when their corners, on
     * the grid, lie in one plane across an axis or along one line.
     */
    public double volumeTimesSix() {
        if (faces.isEmpty()) return 0;
        Vector3d origin = points.get(faces.get(0).corners[0]);
        double volume = 0;
        for (Face polygon : faces) {
            int[] face = polygon.corners;
            Vector3d a = points.get(face[0]);
            for (int i = 1; i < face.length - 1; i++) {
                volume += det(a, points.get(face[i]), points.get(face[i + 1]), origin);
            }
        }
        return volume;
    }

    /** Six times the signed volume of the tetrahedron from {@code origin} to the triangle. */
    public static double det(Vector3d a, Vector3d b, Vector3d c, Vector3d origin) {
        double ax = a.x - origin.x, ay = a.y - origin.y, az = a.z - origin.z;
        double bx = b.x - origin.x, by = b.y - origin.y, bz = b.z - origin.z;
        double cx = c.x - origin.x, cy = c.y - origin.y, cz = c.z - origin.z;
        return ax * (by * cz - bz * cy) - ay * (bx * cz - bz * cx) + az * (bx * cy - by * cx);
    }

    /** The faces as triangles, with the points rounded to {@link #RENDER_GRID}. */
    public List<Triangle3d> toTriangles() {
        // Points rounded to the same position become one
        int[] merged = new int[points.size()];
        Object2IntOpenHashMap<Vector3d> indices = new Object2IntOpenHashMap<>();
        indices.defaultReturnValue(-1);
        for (int i = 0; i < merged.length; i++) {
            points.set(i, round(points.get(i)));
            int first = indices.getInt(points.get(i));
            if (first < 0) indices.put(points.get(i), first = i);
            merged[i] = first;
        }

        // Cuts where the mesh barely reaches past a plane leave a sliver too thin to see or aim at, or, through a
        // corner, edge or face, nothing but faces rounded flat onto the plane
        List<Triangle3d> triangles = new ArrayList<>(faces.size() * 2);
        if (volumeTimesSix() <= 6 * VOLUME_EPSILON) return triangles;
        for (Face polygon : faces) {
            int[] face = polygon.corners;
            for (int i = 1; i < face.length - 1; i++) {
                int a = merged[face[0]], b = merged[face[i]], c = merged[face[i + 1]];
                // A triangle losing a side leaves its other two sides to cancel out against each other
                if (a == b || b == c || c == a) continue;
                // new Vectors, as the triangles of a mesh must not share points
                Triangle3d triangle = new Triangle3d(
                        new Vector3d(points.get(a)),
                        new Vector3d(points.get(b)),
                        new Vector3d(points.get(c)));
                triangle.setPlane(polygon.plane);
                triangles.add(triangle);
            }
        }
        return triangles;
    }

    private static Vector3d round(Vector3d point) {
        // + 0.0 turns -0.0 into 0.0, which equals it but hashes differently
        return new Vector3d(
                Math.rint(point.x / RENDER_GRID) * RENDER_GRID + 0.0,
                Math.rint(point.y / RENDER_GRID) * RENDER_GRID + 0.0,
                Math.rint(point.z / RENDER_GRID) * RENDER_GRID + 0.0);
    }
}
