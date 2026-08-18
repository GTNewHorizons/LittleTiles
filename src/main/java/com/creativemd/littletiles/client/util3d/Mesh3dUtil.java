package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

public class Mesh3dUtil {

    private static Mesh3d MESH_SLOPE;
    private static Mesh3d MESH_SLOPE_CONCAVE;
    private static Mesh3d MESH_SLOPE_CONVEX;
    private static Mesh3d MESH_SLOPE_CONVEX_INNER_CORNER;
    private static Mesh3d MESH_SLOPE_CONVEX_OUTER_CORNER;
    private static Mesh3d MESH_SLOPE_TRIANGLE;
    private static Mesh3d MESH_SLOPE_TRIANGLE_CORNER;
    private static Mesh3d MESH_SLOPE_TRIANGLE_ALT;
    private static Mesh3d MESH_SLOPE_OUTER_CORNER;
    private static Mesh3d MESH_SLOPE_INNER_CORNER;

    public static void initializeMeshes() {
        MESH_SLOPE = Mesh3dObjLoader.load("slope");
        MESH_SLOPE_CONCAVE = Mesh3dObjLoader.load("slope_concave");
        MESH_SLOPE_CONVEX = Mesh3dObjLoader.load("slope_convex");
        MESH_SLOPE_CONVEX_INNER_CORNER = Mesh3dObjLoader.load("slope_convex_inner");
        MESH_SLOPE_CONVEX_OUTER_CORNER = Mesh3dObjLoader.load("slope_convex_outer");
        MESH_SLOPE_TRIANGLE = Mesh3dObjLoader.load("slope_triangle");
        MESH_SLOPE_TRIANGLE_CORNER = Mesh3dObjLoader.load("slope_triangle_corner");
        MESH_SLOPE_TRIANGLE_ALT = Mesh3dObjLoader.load("slope_triangle_alternate");
        MESH_SLOPE_OUTER_CORNER = Mesh3dObjLoader.load("slope_outer");
        MESH_SLOPE_INNER_CORNER = Mesh3dObjLoader.load("slope_inner");
    }

    public static Mesh3d meshFromTile(LittleTileBox box, LittleTileCutoutInfo cutoutInfo) {
        return Mesh3dUtil.createMesh(
                0,
                0,
                0,
                cutoutInfo,
                box.minX / 16.0,
                box.minY / 16.0,
                box.minZ / 16.0,
                box.maxX / 16.0,
                box.maxY / 16.0,
                box.maxZ / 16.0,
                null,
                0);
    }

    public static Mesh3d createMesh(int x, int y, int z, LittleTileCutoutInfo cutoutInfo, double minX, double minY,
            double minZ, double maxX, double maxY, double maxZ, Block block, int meta) {
        Vector3d pos = new Vector3d(x, y, z);
        Vector3d cutoutScale = new Vector3d(
                cutoutInfo.size.x / 16.0,
                cutoutInfo.size.y / 16.0,
                cutoutInfo.size.z / 16.0);
        Vector3i posSubMin = new Vector3i();
        posSubMin.x = (int) Math.round(minX * 16);
        posSubMin.y = (int) Math.round(minY * 16);
        posSubMin.z = (int) Math.round(minZ * 16);
        Vector3i posSubMax = new Vector3i();
        posSubMax.x = (int) Math.round(maxX * 16);
        posSubMax.y = (int) Math.round(maxY * 16);
        posSubMax.z = (int) Math.round(maxZ * 16);
        return Mesh3dUtil.createMesh(
                cutoutInfo,
                cutoutScale,
                pos,
                cutoutInfo.pos,
                posSubMin,
                posSubMax,
                block,
                meta,
                cutoutInfo.orientation);
    }

    public static Matrix3f rotationBetween(Vector3d v1, Vector3d v2) {
        Vector3d from = new Vector3d(v1);
        Vector3d to = new Vector3d(v2);
        from.normalize();
        to.normalize();

        double dot = from.dot(to);

        from.cross(from, to);
        float angle = (float) Math.acos(dot);

        return new Matrix3f().rotation(angle, (float) from.x, (float) from.y, (float) from.z);
    }

    private static Mesh3d createWallMesh(LittleTileCutoutInfo cutoutInfo) {
        ArrayList<Triangle3d> triangles = new ArrayList<>();

        // Front face (z = 1)
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 0, 1), new Vector3d(1, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 1, 1), new Vector3d(0, 1, 1)));

        // Back face (z = 0)
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 1, 0), new Vector3d(1, 0, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(0, 1, 0), new Vector3d(1, 1, 0)));

        // Left face (x = 0)
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(0, 0, 1), new Vector3d(0, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(0, 1, 1), new Vector3d(0, 1, 0)));

        // Right face (x = 1)
        triangles.add(new Triangle3d(new Vector3d(1, 0, 0), new Vector3d(1, 1, 1), new Vector3d(1, 0, 1)));
        triangles.add(new Triangle3d(new Vector3d(1, 0, 0), new Vector3d(1, 1, 0), new Vector3d(1, 1, 1)));

        // Top face (y = 1)
        triangles.add(new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(0, 1, 1), new Vector3d(1, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(1, 1, 1), new Vector3d(1, 1, 0)));

        // Bottom face (y = 0)
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 0, 1), new Vector3d(0, 0, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 0, 0), new Vector3d(1, 0, 1)));

        Mesh3d mesh = new Mesh3d(triangles);

        mesh.scale(cutoutInfo.thickness / 16f);
        float middle = cutoutInfo.thickness / 16f / 2;

        Vector3d moveNeg = new Vector3d(
                cutoutInfo.negX ? (cutoutInfo.size.x - cutoutInfo.thickness) / 16.0 : 0,
                cutoutInfo.negY ? (cutoutInfo.size.y - cutoutInfo.thickness) / 16.0 : 0,
                cutoutInfo.negZ ? (cutoutInfo.size.z - cutoutInfo.thickness) / 16.0 : 0);

        // Face start is what we looked at
        List<Vector3d> endPoints = mesh.getPointsForSide(cutoutInfo.faceStart);
        Vector3d move = new Vector3d(
                (cutoutInfo.size.x - cutoutInfo.thickness) / 16.0 * (cutoutInfo.negX ? -1 : 1),
                (cutoutInfo.size.y - cutoutInfo.thickness) / 16.0 * (cutoutInfo.negY ? -1 : 1),
                (cutoutInfo.size.z - cutoutInfo.thickness) / 16.0 * (cutoutInfo.negZ ? -1 : 1));

        if (cutoutInfo.faceStart != cutoutInfo.faceEnd.getOpposite()) {
            Plane3d planeStart = Plane3d.planes[cutoutInfo.faceStart.ordinal()];
            Plane3d planeEnd = Plane3d.planes[cutoutInfo.faceEnd.getOpposite().ordinal()];
            Matrix3f rotationMatrix = rotationBetween(planeStart.getNormal(), planeEnd.getNormal());
            for (Vector3d point : endPoints) {
                point.add(new Vector3d(-middle, -middle, -middle));
                Vector3f result = rotationMatrix
                        .transform(new Vector3f((float) point.x, (float) point.y, (float) point.z));
                point.x = result.x;
                point.y = result.y;
                point.z = result.z;
                point.add(new Vector3d(middle, middle, middle));
            }
        }
        for (Vector3d point : endPoints) {
            point.add(move);
        }
        mesh.translate(moveNeg);

        return mesh;
    }

    /**
     * Number of corners a {@link LittleTileShapeMode#DEFORMED_BOX} is made of. The index of a corner is a bitmask of
     * which axes it sits on the maximum side of: <code>(x ? 1 : 0) | (y ? 2 : 0) | (z ? 4 : 0)</code>. This is the same
     * convention modern LittleTiles' <code>BoxCorner</code> uses, so the corner math stays comparable.
     */
    public static final int DEFORMED_BOX_CORNER_COUNT = 8;

    /** Below this, a face's offset from the box centroid is too short to tell which way the face points. */
    private static final double OUTWARD_EPSILON_SQUARED = 1.0E-12;

    /**
     * The 4 corners of every box face, in ring order, indexed as described by {@link #DEFORMED_BOX_CORNER_COUNT}. The
     * order of the faces themselves is the one {@link #nominalFaceNormal(int)} relies on.
     */
    public static final int[][] DEFORMED_BOX_FACES = {
            { 0, 2, 6, 4 }, // west (x = min)
            { 1, 5, 7, 3 }, // east (x = max)
            { 0, 4, 5, 1 }, // down (y = min)
            { 2, 3, 7, 6 }, // up (y = max)
            { 0, 1, 3, 2 }, // north (z = min)
            { 4, 6, 7, 5 }, // south (z = max)
    };

    /**
     * The direction the face at the given index of {@link #DEFORMED_BOX_FACES} points in before any corner has been
     * dragged: the faces are listed as min/max pairs per axis, so the index encodes both.
     */
    private static Vector3d nominalFaceNormal(int face) {
        double sign = face % 2 == 0 ? -1 : 1;
        return switch (face / 2) {
            case 0 -> new Vector3d(sign, 0, 0);
            case 1 -> new Vector3d(0, sign, 0);
            default -> new Vector3d(0, 0, sign);
        };
    }

    /** Normalizes a corner offset into the cutout's own unit cube. A zero-thickness axis collapses to 0. */
    public static Vector3d toLocal(Vector3i corner, Vector3i size) {
        return new Vector3d(
                size.x == 0 ? 0 : corner.x / (double) size.x,
                size.y == 0 ? 0 : corner.y / (double) size.y,
                size.z == 0 ? 0 : corner.z / (double) size.z);
    }

    /**
     * Builds the mesh of a box whose 8 corners have been dragged out of their axis-aligned positions. Each face is a
     * general quad which is split along its shorter diagonal - for a warped (non-planar) face the two possible splits
     * give visibly different silhouettes, and the shorter diagonal is the one that keeps the surface closest to flat.
     * Windings are fixed up against the centroid so every face ends up pointing outwards, except where the box has
     * been flattened - see {@link #nominalFaceNormal(int)}.
     * <p>
     * Deformed-box corners always remain in orientation-zero space. This method reconstructs and normalizes that
     * original space from the corners; {@link #createMesh} applies the saved orientation to the completed mesh afterward.
     * The stored cutout size may therefore describe the rotated tile bounds and must not be used to normalize corners.
     */
    private static Mesh3d createDeformedBoxMesh(LittleTileCutoutInfo cutoutInfo) {
        if (cutoutInfo.corners == null) {
            return new Mesh3d(new ArrayList<>());
        }
        LittleTileBox cornerBounds = LittleTileBox.fromPoints(cutoutInfo.corners);
        Vector3i originalSize = new Vector3i(
                cornerBounds.maxX - cornerBounds.minX,
                cornerBounds.maxY - cornerBounds.minY,
                cornerBounds.maxZ - cornerBounds.minZ);
        Vector3d[] corners = new Vector3d[DEFORMED_BOX_CORNER_COUNT];
        Vector3d centroid = new Vector3d();
        for (int i = 0; i < corners.length; i++) {
            // cutoutInfo.size follows the rotated tile bounds, while the corners remain in orientation-zero space.
            // Recover that space from the corners themselves so a non-cubic box is normalized before mesh rotation.
            Vector3i corner = new Vector3i(cutoutInfo.corners[i]);
            corner.sub(cornerBounds.minX, cornerBounds.minY, cornerBounds.minZ);
            corners[i] = toLocal(corner, originalSize);
            centroid.add(corners[i]);
        }
        centroid.scale(1.0 / corners.length);

        List<Triangle3d> triangles = new ArrayList<>();
        for (int f = 0; f < DEFORMED_BOX_FACES.length; f++) {
            int[] face = DEFORMED_BOX_FACES[f];
            Vector3d a = corners[face[0]];
            Vector3d b = corners[face[1]];
            Vector3d c = corners[face[2]];
            Vector3d d = corners[face[3]];

            Vector3d faceCenter = new Vector3d(a);
            faceCenter.add(b);
            faceCenter.add(c);
            faceCenter.add(d);
            faceCenter.scale(0.25);

            Vector3d outward = new Vector3d(faceCenter);
            outward.sub(centroid);
            // A box flattened onto a plane has both faces of the collapsed pair sitting on the centroid, which says
            // nothing about which way either of them points. Fall back to where the face pointed before any corner
            // was dragged
            if (outward.lengthSquared() <= OUTWARD_EPSILON_SQUARED) {
                outward = nominalFaceNormal(f);
            }

            if (splitsAlongFirstDiagonal(a, b, c, d)) {
                addDeformedFaceTriangle(triangles, a, b, c, outward);
                addDeformedFaceTriangle(triangles, a, c, d, outward);
            } else {
                addDeformedFaceTriangle(triangles, b, c, d, outward);
                addDeformedFaceTriangle(triangles, b, d, a, outward);
            }
        }
        return new Mesh3d(triangles);
    }

    /**
     * Which of a face's two diagonals it gets split along: true for a-c, false for b-d. The shorter one wins, since on
     * a warped face the two splits give visibly different silhouettes and the shorter diagonal keeps the surface
     * closest to flat.
     * <p>
     * Public because the preview draws this diagonal as a line, and a line showing a different split than the mesh
     * actually uses would be worse than drawing none at all. The points must be in the cutout's local unit space
     * ({@link #toLocal}), not world space - on a non-cubic box the normalization changes which diagonal is shorter.
     */
    public static boolean splitsAlongFirstDiagonal(Vector3d a, Vector3d b, Vector3d c, Vector3d d) {
        return Mesh3d.distanceSquared(a, c) <= Mesh3d.distanceSquared(b, d);
    }

    private static void addDeformedFaceTriangle(List<Triangle3d> triangles, Vector3d a, Vector3d b, Vector3d c,
            Vector3d outward) {
        if (Mesh3d.isDegenerate(a, b, c)) {
            return;
        }
        Triangle3d triangle = new Triangle3d(new Vector3d(a), new Vector3d(b), new Vector3d(c));
        triangle.ensureWindingOrder(outward);
        triangles.add(triangle);
    }

    /** A visibly warped full tile used to preview a deformed box, which has no fixed shape of its own. */
    public static Vector3i[] demoDeformedBoxCorners() {
        return new Vector3i[] {
                new Vector3i(2, 1, 2),
                new Vector3i(15, 0, 3),
                new Vector3i(0, 13, 1),
                new Vector3i(13, 16, 0),
                new Vector3i(1, 3, 14),
                new Vector3i(16, 2, 16),
                new Vector3i(3, 16, 15),
                new Vector3i(14, 13, 13),
        };
    }

    public static Mesh3d createBoxMesh() {
        List<Triangle3d> triangles = new ArrayList<>();

        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 0, 1), new Vector3d(1, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 1, 1), new Vector3d(0, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 1, 0), new Vector3d(1, 0, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(0, 1, 0), new Vector3d(1, 1, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(0, 0, 1), new Vector3d(0, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(0, 1, 1), new Vector3d(0, 1, 0)));
        triangles.add(new Triangle3d(new Vector3d(1, 0, 0), new Vector3d(1, 1, 1), new Vector3d(1, 0, 1)));
        triangles.add(new Triangle3d(new Vector3d(1, 0, 0), new Vector3d(1, 1, 0), new Vector3d(1, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(0, 1, 1), new Vector3d(1, 1, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(1, 1, 1), new Vector3d(1, 1, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 0, 1), new Vector3d(0, 0, 1)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 0), new Vector3d(1, 0, 0), new Vector3d(1, 0, 1)));

        Mesh3d mesh = new Mesh3d(triangles);
        return mesh;
    }

    public static Mesh3d createMesh(LittleTileCutoutInfo cutoutInfo, Vector3d cutoutScale, Vector3d pos,
            Vector3i posCutout, Vector3i posSubMin, Vector3i posSubMax, Block block, int meta, int orientation) {
        Mesh3d mesh = switch (cutoutInfo.type) {
            case SLOPE -> MESH_SLOPE.copy();
            case PILLAR -> {
                cutoutScale = new Vector3d(1, 1, 1);
                yield createWallMesh(cutoutInfo);
            }
            case SLOPE_CONCAVE -> MESH_SLOPE_CONCAVE.copy();
            case SLOPE_CONVEX -> MESH_SLOPE_CONVEX.copy();
            case SLOPE_CONVEX_INNER_CORNER -> MESH_SLOPE_CONVEX_INNER_CORNER.copy();
            case SLOPE_CONVEX_OUTER_CORNER -> MESH_SLOPE_CONVEX_OUTER_CORNER.copy();
            case SLOPE_TRIANGLE -> MESH_SLOPE_TRIANGLE.copy();
            case SLOPE_TRIANGLE_CORNER -> MESH_SLOPE_TRIANGLE_CORNER.copy();
            case SLOPE_TRIANGLE_ALT -> MESH_SLOPE_TRIANGLE_ALT.copy();
            case SLOPE_OUTER_CORNER -> MESH_SLOPE_OUTER_CORNER.copy();
            case SLOPE_INNER_CORNER -> MESH_SLOPE_INNER_CORNER.copy();
            case DEFORMED_BOX -> createDeformedBoxMesh(cutoutInfo);
            case BOX -> throw new RuntimeException("Invalid cutout BOX");
            default -> throw new RuntimeException("Unknown cutout: " + cutoutInfo.type);
        };

        if (cutoutInfo.type != LittleTileShapeMode.PILLAR) {
            mesh.translate(new Vector3d(-0.5, -0.5, -0.5));
            mesh.rotate(orientation);
            mesh.translate(new Vector3d(0.5, 0.5, 0.5));
        }

        mesh.scale(cutoutScale);
        mesh.translate(posCutout);
        mesh.translate(new Vector3d(posSubMin.x / 16.0, posSubMin.y / 16.0, posSubMin.z / 16.0));

        int meshMinX = posCutout.x + posSubMin.x;
        int meshMaxX = meshMinX + cutoutInfo.size.x;
        int meshMinY = posCutout.y + posSubMin.y;
        int meshMaxY = meshMinY + cutoutInfo.size.y;
        int meshMinZ = posCutout.z + posSubMin.z;
        int meshMaxZ = meshMinZ + cutoutInfo.size.z;
        Plane3d plane;

        if (meshMinY > posSubMax.y || meshMaxY < posSubMin.y
                || meshMinX > posSubMax.x
                || meshMaxX < posSubMin.x
                || meshMinZ > posSubMax.z
                || meshMaxZ < posSubMin.z) {
            return new Mesh3d(new ArrayList<>());
        }

        if (meshMaxY > posSubMax.y) {
            plane = Plane3d.UP.moveAlongNormal(-(16 - posSubMax.y) / 16.0);
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMinY < posSubMin.y) {
            plane = Plane3d.DOWN.moveAlongNormal(-posSubMin.y / 16.0);
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMinX < posSubMin.x) {
            plane = Plane3d.WEST.moveAlongNormal(-posSubMin.x / 16.0);
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMaxX > posSubMax.x) {
            plane = Plane3d.EAST.moveAlongNormal(-(16 - posSubMax.x) / 16.0);
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMaxZ > posSubMax.z) {
            plane = Plane3d.SOUTH.moveAlongNormal(-(16 - posSubMax.z) / 16.0);
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMinZ < posSubMin.z) {
            plane = Plane3d.NORTH.moveAlongNormal(-posSubMin.z / 16.0);
            mesh = mesh.cutByPlane(plane);
        }

        if (block != null) {
            mesh.setTextures(block, meta);
        }
        mesh.translate(pos);

        return mesh;
    }
}
