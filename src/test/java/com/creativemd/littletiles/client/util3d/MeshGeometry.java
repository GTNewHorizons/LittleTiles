package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.BlockSpace.PIXELS;

import java.util.ArrayList;
import java.util.List;
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

    /** The normal of the triangle, as long as twice its area. */
    public static Vector3d normal(Vector3d a, Vector3d b, Vector3d c) {
        return new Vector3d(b).sub(a).cross(new Vector3d(c).sub(a));
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

        /** The faces this filter picks, copied into a mesh of their own. */
        public Mesh3d of(Mesh3d mesh) {
            Vector3d[] vertices = mesh.getVertices();
            List<Triangle3d> faces = new ArrayList<>();
            for (int face = 0; face < mesh.getTriangles().size(); face++) {
                Vector3d[] corners = { vertices[face * 3], vertices[face * 3 + 1], vertices[face * 3 + 2] };
                if (accepts.test(corners)) faces.add(mesh.getTriangles().get(face).copy());
            }
            return new Mesh3d(faces);
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
