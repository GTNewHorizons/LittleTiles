package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

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

    /** The size of a unit mesh, see {@link Grid3d}. */
    private static final GridVector UNIT_SIZE = new GridVector(Grid3d.PIXEL, Grid3d.PIXEL, Grid3d.PIXEL);

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
                cutoutInfo,
                cutoutInfo.size,
                cutoutInfo.pos,
                new Vector3i(box.minX, box.minY, box.minZ),
                new Vector3i(box.maxX, box.maxY, box.maxZ),
                null,
                0,
                cutoutInfo.orientation);
    }

    /**
     * Turns {@code point} the quarter turn that takes axis {@code from} to axis {@code to}, around the origin. Taking
     * an axis to its opposite has no single quarter turn, which ends up mirroring the point through the origin instead.
     */
    private static void rotateBetween(Vector3ic from, Vector3ic to, GridVector point) {
        // a quarter turn around the unit axis k: v' = k x v + k (k . v)
        Vector3i k = new Vector3i(
                from.y() * to.z() - from.z() * to.y(),
                from.z() * to.x() - from.x() * to.z(),
                from.x() * to.y() - from.y() * to.x());
        if (k.x == 0 && k.y == 0 && k.z == 0) {
            if (!from.equals(to)) {
                point.negate();
            }
            return;
        }
        int kDotV = k.x * point.x + k.y * point.y + k.z * point.z;
        point.set(
                k.y * point.z - k.z * point.y + k.x * kDotV,
                k.z * point.x - k.x * point.z + k.y * kDotV,
                k.x * point.y - k.y * point.x + k.z * kDotV);
    }

    /** A box of the given size in grid units, from the origin. */
    private static List<Triangle3d> createBoxTriangles(int size) {
        int s = size;
        List<Triangle3d> triangles = new ArrayList<>();

        // Front face (z = 1)
        triangles.add(new Triangle3d(new GridVector(0, 0, s), new GridVector(s, 0, s), new GridVector(s, s, s)));
        triangles.add(new Triangle3d(new GridVector(0, 0, s), new GridVector(s, s, s), new GridVector(0, s, s)));

        // Back face (z = 0)
        triangles.add(new Triangle3d(new GridVector(0, 0, 0), new GridVector(s, s, 0), new GridVector(s, 0, 0)));
        triangles.add(new Triangle3d(new GridVector(0, 0, 0), new GridVector(0, s, 0), new GridVector(s, s, 0)));

        // Left face (x = 0)
        triangles.add(new Triangle3d(new GridVector(0, 0, 0), new GridVector(0, 0, s), new GridVector(0, s, s)));
        triangles.add(new Triangle3d(new GridVector(0, 0, 0), new GridVector(0, s, s), new GridVector(0, s, 0)));

        // Right face (x = 1)
        triangles.add(new Triangle3d(new GridVector(s, 0, 0), new GridVector(s, s, s), new GridVector(s, 0, s)));
        triangles.add(new Triangle3d(new GridVector(s, 0, 0), new GridVector(s, s, 0), new GridVector(s, s, s)));

        // Top face (y = 1)
        triangles.add(new Triangle3d(new GridVector(0, s, 0), new GridVector(0, s, s), new GridVector(s, s, s)));
        triangles.add(new Triangle3d(new GridVector(0, s, 0), new GridVector(s, s, s), new GridVector(s, s, 0)));

        // Bottom face (y = 0)
        triangles.add(new Triangle3d(new GridVector(0, 0, 0), new GridVector(s, 0, s), new GridVector(0, 0, s)));
        triangles.add(new Triangle3d(new GridVector(0, 0, 0), new GridVector(s, 0, 0), new GridVector(s, 0, s)));

        return triangles;
    }

    private static Mesh3d createWallMesh(LittleTileCutoutInfo cutoutInfo) {
        int thickness = Grid3d.fromPixels(cutoutInfo.thickness);
        Mesh3d mesh = new Mesh3d(createBoxTriangles(thickness));
        int middle = thickness / 2;

        GridVector moveNeg = new GridVector(
                cutoutInfo.negX ? Grid3d.fromPixels(cutoutInfo.size.x - cutoutInfo.thickness) : 0,
                cutoutInfo.negY ? Grid3d.fromPixels(cutoutInfo.size.y - cutoutInfo.thickness) : 0,
                cutoutInfo.negZ ? Grid3d.fromPixels(cutoutInfo.size.z - cutoutInfo.thickness) : 0);

        // Face start is what we looked at
        List<GridVector> endPoints = mesh.getPointsForSide(cutoutInfo.faceStart);
        GridVector move = new GridVector(
                Grid3d.fromPixels(cutoutInfo.size.x - cutoutInfo.thickness) * (cutoutInfo.negX ? -1 : 1),
                Grid3d.fromPixels(cutoutInfo.size.y - cutoutInfo.thickness) * (cutoutInfo.negY ? -1 : 1),
                Grid3d.fromPixels(cutoutInfo.size.z - cutoutInfo.thickness) * (cutoutInfo.negZ ? -1 : 1));

        if (cutoutInfo.faceStart != cutoutInfo.faceEnd.getOpposite()) {
            Plane3d planeStart = Plane3d.planes[cutoutInfo.faceStart.ordinal()];
            Plane3d planeEnd = Plane3d.planes[cutoutInfo.faceEnd.getOpposite().ordinal()];
            for (GridVector point : endPoints) {
                point.sub(middle, middle, middle);
                rotateBetween(planeStart.getNormal(), planeEnd.getNormal(), point);
                point.add(middle, middle, middle);
            }
        }
        for (GridVector point : endPoints) {
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

    /**
     * The 4 corners of every box face, in ring order, indexed as described by {@link #DEFORMED_BOX_CORNER_COUNT}. The
     * order of the faces themselves is the one {@link #nominalFaceNormal(int)} relies on.
     */
    public static final int[][] DEFORMED_BOX_FACES = { { 0, 2, 6, 4 }, // west (x = min)
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
    private static GridNormal nominalFaceNormal(int face) {
        long sign = face % 2 == 0 ? -1 : 1;
        return switch (face / 2) {
            case 0 -> new GridNormal(sign, 0, 0);
            case 1 -> new GridNormal(0, sign, 0);
            default -> new GridNormal(0, 0, sign);
        };
    }

    /**
     * Builds the mesh of a box whose 8 corners have been dragged out of their axis-aligned positions. Each face is a
     * general quad which is split along its shorter diagonal - for a warped (non-planar) face the two possible splits
     * give visibly different silhouettes, and the shorter diagonal is the one that keeps the surface closest to flat.
     * Windings are fixed up against the centroid so every face ends up pointing outwards, except where the box has been
     * flattened - see {@link #nominalFaceNormal(int)}.
     * <p>
     * Deformed-box corners always remain in orientation-zero space. The corners are stretched to the cutout size from
     * before rotation, then rotated, so that the result spans {@code cutoutSize}. The stored cutout size describes the
     * rotated tile bounds, so it has to be rotated back before the corners can be stretched to it.
     *
     * @param cutoutSize the size the mesh is to span once rotated, in tile pixels
     */
    private static Mesh3d createDeformedBoxMesh(LittleTileCutoutInfo cutoutInfo, Vector3ic cutoutSize,
            int orientation) {
        if (cutoutInfo.corners == null) {
            return new Mesh3d(new ArrayList<>());
        }
        Vector3i size = OrientationMapper.unrotateSize(orientation, cutoutSize);
        Mesh3d mesh = new Mesh3d(createDeformedBoxTriangles(cutoutInfo.corners, size));
        mesh.rotate(orientation, GridVector.fromPixels(size));
        return mesh;
    }

    /** Size of the space the corners of a deformed box span, with orientation zero. */
    public static Vector3i originalSize(Vector3i[] cornerOffsets) {
        LittleTileBox cornerBounds = LittleTileBox.fromPoints(cornerOffsets);
        return new Vector3i(
                cornerBounds.maxX - cornerBounds.minX,
                cornerBounds.maxY - cornerBounds.minY,
                cornerBounds.maxZ - cornerBounds.minZ);
    }

    /** Stretches one axis of a corner offset from the space the corners span to {@code size}, in grid units. */
    private static int stretch(int offset, int originalSize, int size) {
        return originalSize == 0 ? 0 : (int) Grid3d.divRound((long) offset * Grid3d.PIXEL * size, originalSize);
    }

    /**
     * The triangles of {@link #createDeformedBoxMesh}, with orientation zero.
     *
     * @param size the size the corners are stretched to, in tile pixels
     */
    private static List<Triangle3d> createDeformedBoxTriangles(Vector3i[] cornerOffsets, Vector3ic size) {
        LittleTileBox cornerBounds = LittleTileBox.fromPoints(cornerOffsets);
        Vector3i originalSize = originalSize(cornerOffsets);
        Vector3i[] offsets = new Vector3i[DEFORMED_BOX_CORNER_COUNT];
        GridVector[] corners = new GridVector[DEFORMED_BOX_CORNER_COUNT];
        long sumX = 0, sumY = 0, sumZ = 0;
        for (int i = 0; i < corners.length; i++) {
            // cutoutInfo.size follows the rotated tile bounds, while the corners remain in orientation-zero space.
            // Recover that space from the corners themselves, so a non-cubic box is stretched along the right axes.
            Vector3i corner = new Vector3i(cornerOffsets[i]);
            corner.sub(cornerBounds.minX, cornerBounds.minY, cornerBounds.minZ);
            offsets[i] = corner;
            corners[i] = new GridVector(
                    stretch(corner.x, originalSize.x, size.x()),
                    stretch(corner.y, originalSize.y, size.y()),
                    stretch(corner.z, originalSize.z, size.z()));
            sumX += corners[i].x;
            sumY += corners[i].y;
            sumZ += corners[i].z;
        }

        List<Triangle3d> triangles = new ArrayList<>();
        for (int f = 0; f < DEFORMED_BOX_FACES.length; f++) {
            int[] face = DEFORMED_BOX_FACES[f];
            GridVector a = corners[face[0]];
            GridVector b = corners[face[1]];
            GridVector c = corners[face[2]];
            GridVector d = corners[face[3]];

            // face center minus centroid, times 8 to stay whole: sum of the face's 4 corners / 4 - sum of all 8 / 8
            GridNormal outward = new GridNormal(
                    2L * ((long) a.x + b.x + c.x + d.x) - sumX,
                    2L * ((long) a.y + b.y + c.y + d.y) - sumY,
                    2L * ((long) a.z + b.z + c.z + d.z) - sumZ);
            // A box flattened onto a plane has both faces of the collapsed pair sitting on the centroid, which says
            // nothing about which way either of them points. Fall back to where the face pointed before any corner
            // was dragged
            if (outward.isZero()) {
                outward = nominalFaceNormal(f);
            }

            if (splitsAlongFirstDiagonal(
                    offsets[face[0]],
                    offsets[face[1]],
                    offsets[face[2]],
                    offsets[face[3]],
                    originalSize)) {
                addDeformedFaceTriangle(triangles, a, b, c, outward);
                addDeformedFaceTriangle(triangles, a, c, d, outward);
            } else {
                addDeformedFaceTriangle(triangles, b, c, d, outward);
                addDeformedFaceTriangle(triangles, b, d, a, outward);
            }
        }
        return triangles;
    }

    /**
     * Whether the mesh of a deformed box encloses volume everywhere. A box squashed completely flat, or only partly -
     * like dragging 3 of the 4 upper corners down, which leaves one triangle of the top face lying on the bottom face -
     * has surface triangles covering each other, so any two triangles overlapping in more than an edge rule it out.
     * Collapsed edges and faces, as in a wedge or pyramid, stay valid.
     *
     * @param cornerOffsets the corners as tile pixel offsets, indexed as described by
     *                      {@link #DEFORMED_BOX_CORNER_COUNT}
     */
    public static boolean enclosesVolume(Vector3i[] cornerOffsets) {
        // at their own size, where the corners need no rounding and the overlap test below stays exact
        List<Triangle3d> triangles = createDeformedBoxTriangles(cornerOffsets, originalSize(cornerOffsets));
        if (triangles.isEmpty()) {
            return false;
        }

        for (int i = 0; i < triangles.size(); i++) {
            Triangle3d triangle = triangles.get(i);
            for (int j = i + 1; j < triangles.size(); j++) {
                Triangle3d other = triangles.get(j);
                if (triangle.boundsOverlap(other) && triangle.isCoplanar(other) && triangle.overlaps(other)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Which of a face's two diagonals it gets split along: true for a-c, false for b-d. The shorter one wins, since on
     * a warped face the two splits give visibly different silhouettes and the shorter diagonal keeps the surface
     * closest to flat.
     * <p>
     * Public because the preview draws this diagonal as a line, and a line showing a different split than the mesh
     * actually uses would be worse than drawing none at all. The diagonals are compared in the cutout's local unit
     * space, every corner offset divided by the size per axis, not world space - on a non-cubic box the normalization
     * changes which diagonal is shorter.
     *
     * @param size the size of the space the corner offsets are normalized by, {@link #originalSize} for the corners of
     *             a deformed box
     */
    public static boolean splitsAlongFirstDiagonal(Vector3i a, Vector3i b, Vector3i c, Vector3i d, Vector3i size) {
        return localDistanceSquared(a, c, size) <= localDistanceSquared(b, d, size);
    }

    /**
     * The squared distance between two corner offsets in the cutout's local unit space. A zero size component
     * contributes nothing: all corners share that coordinate.
     */
    private static double localDistanceSquared(Vector3i p, Vector3i q, Vector3i size) {
        double dx = size.x == 0 ? 0 : ((double) p.x - q.x) / (double) size.x;
        double dy = size.y == 0 ? 0 : ((double) p.y - q.y) / (double) size.y;
        double dz = size.z == 0 ? 0 : ((double) p.z - q.z) / (double) size.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void addDeformedFaceTriangle(List<Triangle3d> triangles, GridVector a, GridVector b, GridVector c,
            GridNormal outward) {
        if (Triangle3d.isDegenerate(a, b, c)) {
            return;
        }
        Triangle3d triangle = new Triangle3d(new GridVector(a), new GridVector(b), new GridVector(c));
        triangle.ensureWindingOrder(outward);
        triangles.add(triangle);
    }

    /** A visibly warped full tile used to preview a deformed box, which has no fixed shape of its own. */
    public static Vector3i[] demoDeformedBoxCorners() {
        return new Vector3i[] { new Vector3i(2, 1, 2), new Vector3i(15, 0, 3), new Vector3i(0, 13, 1),
                new Vector3i(13, 16, 0), new Vector3i(1, 3, 14), new Vector3i(16, 2, 16), new Vector3i(3, 16, 15),
                new Vector3i(14, 13, 13), };
    }

    /** A full block. */
    public static Mesh3d createBoxMesh() {
        return new Mesh3d(createBoxTriangles(Grid3d.BLOCK));
    }

    /**
     * Builds the mesh of a cutout and cuts it down to the part inside the sub-box, in grid units relative to the block.
     *
     * @param cutoutSize the size of the whole cutout, in tile pixels
     * @param posCutout  where the cutout starts relative to the sub-box, in tile pixels
     * @param posSubMin  the sub-box within the block, in tile pixels
     */
    public static Mesh3d createMesh(LittleTileCutoutInfo cutoutInfo, Vector3ic cutoutSize, Vector3ic posCutout,
            Vector3ic posSubMin, Vector3ic posSubMax, Block block, int meta, int orientation) {
        Mesh3d mesh = buildMesh(cutoutInfo, cutoutSize, posCutout, posSubMin, orientation);

        int meshMinX = posCutout.x() + posSubMin.x();
        int meshMaxX = meshMinX + cutoutInfo.size.x;
        int meshMinY = posCutout.y() + posSubMin.y();
        int meshMaxY = meshMinY + cutoutInfo.size.y;
        int meshMinZ = posCutout.z() + posSubMin.z();
        int meshMaxZ = meshMinZ + cutoutInfo.size.z;
        Plane3d plane;

        if (meshMinY > posSubMax.y() || meshMaxY < posSubMin.y()
                || meshMinX > posSubMax.x()
                || meshMaxX < posSubMin.x()
                || meshMinZ > posSubMax.z()
                || meshMaxZ < posSubMin.z()) {
            // The anti mesh of a shape that misses the tile entirely is the whole tile
            return finishMesh(new Mesh3d(new ArrayList<>()), cutoutInfo, posSubMin, posSubMax, block, meta);
        }

        if (meshMaxY > posSubMax.y()) {
            plane = Plane3d.UP.moveAlongNormal(-Grid3d.fromPixels(16 - posSubMax.y()));
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMinY < posSubMin.y()) {
            plane = Plane3d.DOWN.moveAlongNormal(-Grid3d.fromPixels(posSubMin.y()));
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMinX < posSubMin.x()) {
            plane = Plane3d.WEST.moveAlongNormal(-Grid3d.fromPixels(posSubMin.x()));
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMaxX > posSubMax.x()) {
            plane = Plane3d.EAST.moveAlongNormal(-Grid3d.fromPixels(16 - posSubMax.x()));
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMaxZ > posSubMax.z()) {
            plane = Plane3d.SOUTH.moveAlongNormal(-Grid3d.fromPixels(16 - posSubMax.z()));
            mesh = mesh.cutByPlane(plane);
        }
        if (meshMinZ < posSubMin.z()) {
            plane = Plane3d.NORTH.moveAlongNormal(-Grid3d.fromPixels(posSubMin.z()));
            mesh = mesh.cutByPlane(plane);
        }

        return finishMesh(mesh, cutoutInfo, posSubMin, posSubMax, block, meta);
    }

    /** How much of its tile box a cutout covers, see {@link #classifyTile}. */
    public enum TileCoverage {
        /** Nothing of the shape is inside the box, so there is nothing to place. */
        EMPTY,
        /** The shape crosses the box and has to be cut to it. */
        PARTIAL,
        /** The shape fills the whole box, so a plain box tile looks the same. */
        FULL
    }

    /**
     * Works out how much of {@code box} the cutout covers, without cutting the mesh. A big shape split across many
     * blocks leaves most of them either empty or completely filled, and cutting those is wasted work. Whatever the
     * test cannot rule out counts as {@link TileCoverage#PARTIAL}, so the caller falls back to the cut mesh.
     */
    public static TileCoverage classifyTile(LittleTileBox box, LittleTileCutoutInfo cutoutInfo) {
        TileCoverage coverage = classifyShape(box, cutoutInfo);
        if (!cutoutInfo.inverted) {
            return coverage;
        }
        // the anti mesh covers exactly the rest of the box
        return switch (coverage) {
            case EMPTY -> TileCoverage.FULL;
            case FULL -> TileCoverage.EMPTY;
            default -> TileCoverage.PARTIAL;
        };
    }

    private static TileCoverage classifyShape(LittleTileBox box, LittleTileCutoutInfo cutoutInfo) {
        Vector3i posSubMin = new Vector3i(box.minX, box.minY, box.minZ);
        Mesh3d mesh = buildMesh(cutoutInfo, cutoutInfo.size, cutoutInfo.pos, posSubMin, cutoutInfo.orientation);
        if (mesh.getTriangles().isEmpty()) {
            return TileCoverage.EMPTY;
        }

        // the same bounds createMesh cuts with, in tile pixels
        int[] subMin = { box.minX, box.minY, box.minZ };
        int[] subMax = { box.maxX, box.maxY, box.maxZ };
        int[] meshMin = { cutoutInfo.pos.x + box.minX, cutoutInfo.pos.y + box.minY, cutoutInfo.pos.z + box.minZ };
        int[] meshMax = { meshMin[0] + cutoutInfo.size.x, meshMin[1] + cutoutInfo.size.y,
                meshMin[2] + cutoutInfo.size.z };

        // Only a cut can make the shape fill the box. Without one the mesh lies within the box as it is, which also
        // keeps thin shapes like a curved wall on the regular path.
        boolean cut = false;
        // The clip box: the part of the box the shape's bounds reach, in grid units
        int[] clipMin = new int[3];
        int[] clipMax = new int[3];
        boolean clipIsBox = true;
        for (int axis = 0; axis < 3; axis++) {
            cut |= meshMin[axis] < subMin[axis] || meshMax[axis] > subMax[axis];
            clipMin[axis] = Grid3d.fromPixels(Math.max(meshMin[axis], subMin[axis]));
            clipMax[axis] = Grid3d.fromPixels(Math.min(meshMax[axis], subMax[axis]));
            // Shapes that miss the box or only touch it
            if (clipMin[axis] >= clipMax[axis]) {
                return TileCoverage.EMPTY;
            }
            clipIsBox &= meshMin[axis] <= subMin[axis] && meshMax[axis] >= subMax[axis];
        }
        if (!cut) {
            return TileCoverage.PARTIAL;
        }

        // Relative to the clip box's min corner, so the floats the separating axis test runs on stay small and exact
        Vector3f extent = new Vector3f(
                (float) Grid3d.toBlocks(clipMax[0] - clipMin[0]) / 2,
                (float) Grid3d.toBlocks(clipMax[1] - clipMin[1]) / 2,
                (float) Grid3d.toBlocks(clipMax[2] - clipMin[2]) / 2);
        TriangleBoundingBoxIntersect.BoundingBox clip = new TriangleBoundingBoxIntersect.BoundingBox(
                new Vector3f(extent),
                extent);
        for (Triangle3d triangle : mesh.getTriangles()) {
            // Exact reject against the open interior of the clip box. This also drops every triangle lying on a face
            // of the cutout's bounds, as those faces are either faces of the clip box or outside of it.
            if (!reachesInterior(triangle, clipMin, clipMax)) {
                continue;
            }
            if (TriangleBoundingBoxIntersect.intersect(
                    clip,
                    relativeBlocks(triangle.getP1(), clipMin),
                    relativeBlocks(triangle.getP2(), clipMin),
                    relativeBlocks(triangle.getP3(), clipMin))) {
                return TileCoverage.PARTIAL;
            }
        }

        // The surface misses the clip box, so it is either completely inside the shape or completely outside. The
        // center is tested since corners can sit on faces flush with the shape.
        Vector3f center = new Vector3f(
                (float) Grid3d.toBlocks((long) clipMin[0] + clipMax[0]) / 2,
                (float) Grid3d.toBlocks((long) clipMin[1] + clipMax[1]) / 2,
                (float) Grid3d.toBlocks((long) clipMin[2] + clipMax[2]) / 2);
        if (!mesh.containsPoint(center)) {
            return TileCoverage.EMPTY;
        }
        // A filled clip box smaller than the box would need a smaller box tile, leave that to the cut
        return clipIsBox ? TileCoverage.FULL : TileCoverage.PARTIAL;
    }

    /** Whether the bounds of the triangle overlap the open interior of the clip box. Exact. */
    private static boolean reachesInterior(Triangle3d triangle, int[] clipMin, int[] clipMax) {
        for (int axis = 0; axis < 3; axis++) {
            int a = triangle.getP1().get(axis);
            int b = triangle.getP2().get(axis);
            int c = triangle.getP3().get(axis);
            if (max(a, b, c) <= clipMin[axis] || min(a, b, c) >= clipMax[axis]) {
                return false;
            }
        }
        return true;
    }

    private static Vector3f relativeBlocks(GridVector point, int[] origin) {
        return new Vector3f(
                (float) Grid3d.toBlocks((long) point.x - origin[0]),
                (float) Grid3d.toBlocks((long) point.y - origin[1]),
                (float) Grid3d.toBlocks((long) point.z - origin[2]));
    }

    /** The whole mesh of a cutout, uncut, placed relative to the block in grid units. */
    private static Mesh3d buildMesh(LittleTileCutoutInfo cutoutInfo, Vector3ic cutoutSize, Vector3ic posCutout,
            Vector3ic posSubMin, int orientation) {
        Mesh3d mesh = switch (cutoutInfo.type) {
            case SLOPE -> MESH_SLOPE.copy();
            case PILLAR -> createWallMesh(cutoutInfo);
            case SLOPE_CONCAVE -> MESH_SLOPE_CONCAVE.copy();
            case SLOPE_CONVEX -> MESH_SLOPE_CONVEX.copy();
            case SLOPE_CONVEX_INNER_CORNER -> MESH_SLOPE_CONVEX_INNER_CORNER.copy();
            case SLOPE_CONVEX_OUTER_CORNER -> MESH_SLOPE_CONVEX_OUTER_CORNER.copy();
            case SLOPE_TRIANGLE -> MESH_SLOPE_TRIANGLE.copy();
            case SLOPE_TRIANGLE_CORNER -> MESH_SLOPE_TRIANGLE_CORNER.copy();
            case SLOPE_TRIANGLE_ALT -> MESH_SLOPE_TRIANGLE_ALT.copy();
            case SLOPE_OUTER_CORNER -> MESH_SLOPE_OUTER_CORNER.copy();
            case SLOPE_INNER_CORNER -> MESH_SLOPE_INNER_CORNER.copy();
            case DEFORMED_BOX -> createDeformedBoxMesh(cutoutInfo, cutoutSize, orientation);
            case BOX -> throw new RuntimeException("Invalid cutout BOX");
            default -> throw new RuntimeException("Unknown cutout: " + cutoutInfo.type);
        };

        // the deformed box is built at its size and rotated already
        if (cutoutInfo.type != LittleTileShapeMode.PILLAR && cutoutInfo.type != LittleTileShapeMode.DEFORMED_BOX) {
            mesh.rotate(orientation, UNIT_SIZE);
            mesh.scale(cutoutSize);
        }

        mesh.translate(
                Grid3d.fromPixels(posCutout.x() + posSubMin.x()),
                Grid3d.fromPixels(posCutout.y() + posSubMin.y()),
                Grid3d.fromPixels(posCutout.z() + posSubMin.z()));
        return mesh;
    }

    /**
     * Turns a mesh already cut to its tile into the final one. Inverting has to wait until after the cuts, since the
     * anti mesh can fall apart into several pieces, which {@link Mesh3d#cutByPlane} cannot close.
     */
    private static Mesh3d finishMesh(Mesh3d mesh, LittleTileCutoutInfo cutoutInfo, Vector3ic posSubMin,
            Vector3ic posSubMax, Block block, int meta) {
        if (cutoutInfo.inverted) {
            mesh = mesh.invert(GridVector.fromPixels(posSubMin), GridVector.fromPixels(posSubMax));
        }
        if (block != null) {
            mesh.setTextures(block, meta);
        }

        return mesh;
    }

    static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    static int max(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }
}
