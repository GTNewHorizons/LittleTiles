package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.creativemd.creativecore.lib.Vector3d;

/** Loads the templates shapes are made from, split into the convex pieces collision works with. */
public final class Mesh3dTemplateLoader {

    /**
     * How far a corner may lie in front of a face plane and still count as behind it, in the template's unit cube: the
     * rounding of doubles, as the corners of each face in the OBJ files lie in one plane.
     */
    private static final double EPSILON = 1.0E-9;

    private Mesh3dTemplateLoader() {}

    /** A template: its surface, and the convex pieces its solid is made of. */
    public static final class Template {

        public final Mesh3d surface;
        /** Just the surface if the solid is convex. */
        public final List<Mesh3d> pieces;

        public Template(Mesh3d surface, List<Mesh3d> pieces) {
            this.surface = surface;
            this.pieces = pieces;
        }
    }

    /**
     * The template, with the convex pieces its solid splits into, unless it is convex: the pyramids from a corner it is
     * star-shaped from to each of its triangles, merged while they stay convex. Of the corners it is star-shaped from,
     * the one leaving the fewest pieces is taken, as collision works through every pair of pieces. A solid star-shaped
     * from none of its corners is split into {@link Mesh3dUtil#tetrahedra}.
     */
    public static Template load(String name) {
        Mesh3d surface = Mesh3dObjLoader.load(name);
        List<Triangle3d> triangles = surface.getTriangles();
        if (isConvex(triangles)) return new Template(surface, Collections.singletonList(surface));

        Vector3d bestApex = null;
        List<List<Triangle3d>> bestBases = null;
        Set<Vector3d> corners = new LinkedHashSet<>();
        for (Triangle3d triangle : triangles) {
            Collections.addAll(corners, triangle.getP1(), triangle.getP2(), triangle.getP3());
        }
        for (Vector3d apex : corners) {
            if (!liesBehind(triangles, apex)) continue;
            List<List<Triangle3d>> bases = pyramidBases(triangles, apex);
            if (bestBases == null || bases.size() < bestBases.size()) {
                bestApex = apex;
                bestBases = bases;
            }
        }
        if (bestBases == null) return new Template(surface, Mesh3dUtil.tetrahedra(surface));
        List<Mesh3d> pieces = new ArrayList<>();
        for (List<Triangle3d> base : bestBases) {
            pieces.add(new Mesh3d(groupByPlane(pyramid(bestApex, base))));
        }
        return new Template(surface, pieces);
    }

    /** The triangles each convex piece is the pyramid over, merged greedily from one pyramid per triangle. */
    private static List<List<Triangle3d>> pyramidBases(List<Triangle3d> triangles, Vector3d apex) {
        List<List<Triangle3d>> bases = new ArrayList<>();
        for (Triangle3d triangle : triangles) {
            // A triangle in a plane through the apex makes no pyramid
            if (Math.abs(MeshCutter.det(triangle.getP1(), triangle.getP2(), triangle.getP3(), apex)) > EPSILON)
                bases.add(new ArrayList<>(Collections.singletonList(triangle)));
        }
        for (int i = 0; i < bases.size(); i++) {
            for (int j = i + 1; j < bases.size(); j++) {
                List<Triangle3d> merged = new ArrayList<>(bases.get(i));
                merged.addAll(bases.get(j));
                if (isConvex(pyramid(apex, merged))) {
                    bases.set(i, merged);
                    bases.remove(j);
                    // What didn't fit before may fit the larger piece
                    j = i;
                }
            }
        }
        return bases;
    }

    /**
     * The closed pyramid from the apex over the triangles: copies of them, and a side to the apex from each edge around
     * them.
     */
    private static List<Triangle3d> pyramid(Vector3d apex, List<Triangle3d> base) {
        List<Triangle3d> faces = new ArrayList<>();
        List<Vector3d[]> edges = new ArrayList<>();
        for (Triangle3d triangle : base) {
            faces.add(triangle.copy());
            Vector3d[] corners = { triangle.getP1(), triangle.getP2(), triangle.getP3() };
            for (int i = 0; i < 3; i++) {
                Vector3d from = corners[i], to = corners[(i + 1) % 3];
                // An edge between two triangles of the base is walked both ways and has no side
                if (!edges.removeIf(edge -> Objects.equals(edge[0], to) && Objects.equals(edge[1], from))) {
                    edges.add(new Vector3d[] { from, to });
                }
            }
        }
        for (Vector3d[] edge : edges) {
            faces.add(new Triangle3d(new Vector3d(apex), new Vector3d(edge[1]), new Vector3d(edge[0])));
        }
        return faces;
    }

    /**
     * The faces with those lying in one plane next to each other and sharing a polygon token, so that collision takes
     * each plane of a piece once, see {@link Triangle3d#getPolygon}.
     */
    private static List<Triangle3d> groupByPlane(List<Triangle3d> faces) {
        List<Triangle3d> grouped = new ArrayList<>(faces.size());
        boolean[] done = new boolean[faces.size()];
        for (int i = 0; i < faces.size(); i++) {
            if (done[i]) continue;
            Plane3d plane = faces.get(i).getFacePlane();
            Object polygon = new Object();
            for (int j = i; j < faces.size(); j++) {
                Triangle3d face = faces.get(j);
                if (done[j] || !liesIn(plane, face)) continue;
                face.setPolygon(polygon);
                grouped.add(face);
                done[j] = true;
            }
        }
        return grouped;
    }

    private static boolean liesIn(Plane3d plane, Triangle3d face) {
        return Math.abs(plane.getDistance(face.getP1())) <= EPSILON
                && Math.abs(plane.getDistance(face.getP2())) <= EPSILON
                && Math.abs(plane.getDistance(face.getP3())) <= EPSILON;
    }

    /** Whether the closed mesh is convex: no corner lies in front of the plane of a face. */
    public static boolean isConvex(List<Triangle3d> faces) {
        for (Triangle3d face : faces) {
            if (!liesBehind(faces, face.getP1()) || !liesBehind(faces, face.getP2())
                    || !liesBehind(faces, face.getP3())) {
                return false;
            }
        }
        return true;
    }

    /** Whether the point lies behind or in the plane of every face. */
    private static boolean liesBehind(List<Triangle3d> faces, Vector3d point) {
        for (Triangle3d face : faces) {
            if (face.getFacePlane().getDistance(point) > EPSILON) return false;
        }
        return true;
    }
}
