package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

public class Mesh3dUtil {

    /** The template of each shape made from one, see {@link Mesh3dTemplateLoader#load}. */
    private static final Map<LittleTileShapeMode, Mesh3dTemplateLoader.Template> TEMPLATES = new EnumMap<>(
            LittleTileShapeMode.class);

    public static void initializeMeshes() {
        TEMPLATES.put(LittleTileShapeMode.SLOPE, Mesh3dTemplateLoader.load("slope"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_CONCAVE, Mesh3dTemplateLoader.load("slope_concave"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_CONVEX, Mesh3dTemplateLoader.load("slope_convex"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_CONVEX_INNER_CORNER, Mesh3dTemplateLoader.load("slope_convex_inner"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_CONVEX_OUTER_CORNER, Mesh3dTemplateLoader.load("slope_convex_outer"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_CONCAVE_INNER_CORNER, Mesh3dTemplateLoader.load("slope_concave_inner"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_CONCAVE_OUTER_CORNER, Mesh3dTemplateLoader.load("slope_concave_outer"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_TRIANGLE, Mesh3dTemplateLoader.load("slope_triangle"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_TRIANGLE_CORNER, Mesh3dTemplateLoader.load("slope_triangle_corner"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_TRIANGLE_ALT, Mesh3dTemplateLoader.load("slope_triangle_alternate"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_OUTER_CORNER, Mesh3dTemplateLoader.load("slope_outer"));
        TEMPLATES.put(LittleTileShapeMode.SLOPE_INNER_CORNER, Mesh3dTemplateLoader.load("slope_inner"));
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

    /** A box from the origin to (1, 1, 1). */
    private static List<Triangle3d> createBoxTriangles() {
        List<Triangle3d> triangles = new ArrayList<>();

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

        return triangles;
    }

    private static Mesh3d createWallMesh(LittleTileCutoutInfo cutoutInfo) {
        Mesh3d mesh = new Mesh3d(createBoxTriangles());

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

    /**
     * Overlap, in square grid units, below which two coplanar triangles only touch. Well above the slivers
     * {@link Triangle3d#split} drops, well below anything the grid can actually produce.
     */
    private static final double OVERLAP_AREA_EPSILON = 1.0E-3;

    /**
     * The 4 corners of every box face, in ring order, indexed as described by {@link #DEFORMED_BOX_CORNER_COUNT}. All
     * wound the same way round, so that the faces meet in edges walked once each way.
     */
    public static final int[][] DEFORMED_BOX_FACES = { { 0, 2, 6, 4 }, // west (x = min)
            { 1, 5, 7, 3 }, // east (x = max)
            { 0, 4, 5, 1 }, // down (y = min)
            { 2, 3, 7, 6 }, // up (y = max)
            { 0, 1, 3, 2 }, // north (z = min)
            { 4, 6, 7, 5 }, // south (z = max)
    };

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
     * The mesh is wound outwards as a whole, see {@link #createDeformedBoxTriangles}.
     * <p>
     * Deformed-box corners always remain in orientation-zero space. This method reconstructs and normalizes that
     * original space from the corners; {@link #createMesh} applies the saved orientation to the completed mesh
     * afterward. The stored cutout size may therefore describe the rotated tile bounds and must not be used to
     * normalize corners.
     */
    private static Mesh3d createDeformedBoxMesh(LittleTileCutoutInfo cutoutInfo) {
        if (cutoutInfo.corners == null) {
            return new Mesh3d(new ArrayList<>());
        }
        return new Mesh3d(createDeformedBoxTriangles(cutoutInfo.corners));
    }

    /** Size of the space the corners of a deformed box span, with orientation zero. */
    private static Vector3i originalSize(Vector3i[] cornerOffsets) {
        LittleTileBox cornerBounds = LittleTileBox.fromPoints(cornerOffsets);
        return new Vector3i(
                cornerBounds.maxX - cornerBounds.minX,
                cornerBounds.maxY - cornerBounds.minY,
                cornerBounds.maxZ - cornerBounds.minZ);
    }

    /**
     * The triangles of {@link #createDeformedBoxMesh}, in the cutout's local unit space. Each face is split along one
     * of its diagonals, see {@link #splitsAlongFirstDiagonal}. The mesh is closed whatever the corners: where some
     * coincide, the triangles left without area are left out, which keeps it closed.
     */
    private static List<Triangle3d> createDeformedBoxTriangles(Vector3i[] cornerOffsets) {
        LittleTileBox cornerBounds = LittleTileBox.fromPoints(cornerOffsets);
        Vector3i originalSize = originalSize(cornerOffsets);
        Vector3d[] corners = new Vector3d[DEFORMED_BOX_CORNER_COUNT];
        for (int i = 0; i < corners.length; i++) {
            // cutoutInfo.size follows the rotated tile bounds, while the corners remain in orientation-zero space.
            // Recover that space from the corners themselves so a non-cubic box is normalized before mesh rotation.
            Vector3i corner = new Vector3i(cornerOffsets[i]);
            corner.sub(cornerBounds.minX, cornerBounds.minY, cornerBounds.minZ);
            corners[i] = toLocal(corner, originalSize);
        }

        List<Triangle3d> triangles = new ArrayList<>();
        for (int[] face : DEFORMED_BOX_FACES) {
            Vector3d a = corners[face[0]], b = corners[face[1]], c = corners[face[2]], d = corners[face[3]];
            if (splitsAlongFirstDiagonal(
                    cornerOffsets[face[0]],
                    cornerOffsets[face[1]],
                    cornerOffsets[face[2]],
                    cornerOffsets[face[3]])) {
                addDeformedFaceTriangle(triangles, a, b, c);
                addDeformedFaceTriangle(triangles, a, c, d);
            } else {
                addDeformedFaceTriangle(triangles, b, c, d);
                addDeformedFaceTriangle(triangles, b, d, a);
            }
        }
        // The faces are listed wound inwards for an undragged box. Turned all at once, so they keep fitting together
        Mesh3d mesh = new Mesh3d(triangles);
        if (mesh.volume() < 0) mesh.flipWindingOrder();
        return triangles;
    }

    /**
     * Whether the mesh of a deformed box encloses volume everywhere. A box squashed completely flat, or only partly -
     * like dragging 3 of the 4 upper corners down, which leaves one triangle of the top face lying on the bottom face -
     * has surface triangles covering each other, so any two triangles overlapping in more than an edge rule it out.
     * Collapsed edges and faces, as in a wedge or pyramid, stay valid.
     *
     * @param cornerOffsets the corners as grid offsets, indexed as described by {@link #DEFORMED_BOX_CORNER_COUNT}
     */
    public static boolean enclosesVolume(Vector3i[] cornerOffsets) {
        Vector3i originalSize = originalSize(cornerOffsets);
        List<Triangle3d> triangles = createDeformedBoxTriangles(cornerOffsets);
        if (triangles.isEmpty()) {
            return false;
        }
        // Back to grid units, so the overlap tolerance does not shrink with the size of the box
        Vector3d scale = new Vector3d(originalSize.x, originalSize.y, originalSize.z);
        for (Triangle3d triangle : triangles) {
            triangle.scale(scale);
        }

        for (int i = 0; i < triangles.size(); i++) {
            Triangle3d triangle = triangles.get(i);
            for (int j = i + 1; j < triangles.size(); j++) {
                Triangle3d other = triangles.get(j);
                if (!triangle.boundsOverlap(other) || !triangle.isCoplanar(other)) continue;

                double uncovered = 0;
                for (Triangle3d piece : triangle.split(other)) {
                    uncovered += piece.getArea();
                }
                if (triangle.getArea() - uncovered > OVERLAP_AREA_EPSILON) {
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
     * Decided on the corners as grid offsets, exactly, rather than in the cutout's local unit space, which every box
     * stretches by its own size: boxes sharing a face share its corners, so they split it alike and fit together.
     * <p>
     * Public because the preview draws this diagonal as a line, and a line showing a different split than the mesh
     * actually uses would be worse than drawing none at all.
     */
    public static boolean splitsAlongFirstDiagonal(Vector3i a, Vector3i b, Vector3i c, Vector3i d) {
        return distanceSquared(a, c) <= distanceSquared(b, d);
    }

    private static long distanceSquared(Vector3i a, Vector3i b) {
        long x = a.x - b.x, y = a.y - b.y, z = a.z - b.z;
        return x * x + y * y + z * z;
    }

    private static void addDeformedFaceTriangle(List<Triangle3d> triangles, Vector3d a, Vector3d b, Vector3d c) {
        if (a.equals(b) || b.equals(c) || c.equals(a)) return;
        triangles.add(new Triangle3d(new Vector3d(a), new Vector3d(b), new Vector3d(c)));
    }

    /** A visibly warped full tile used to preview a deformed box, which has no fixed shape of its own. */
    public static Vector3i[] demoDeformedBoxCorners() {
        return new Vector3i[] { new Vector3i(2, 1, 2), new Vector3i(15, 0, 3), new Vector3i(0, 13, 1),
                new Vector3i(13, 16, 0), new Vector3i(1, 3, 14), new Vector3i(16, 2, 16), new Vector3i(3, 16, 15),
                new Vector3i(14, 13, 13), };
    }

    public static Mesh3d createBoxMesh() {
        return new Mesh3d(createBoxTriangles());
    }

    /**
     * The solid of the tile before it is cut to its box, as convex pieces that add up to it, in blocks. A shape made
     * from a template has the pieces the template lists, other shapes are split into {@link #tetrahedra}.
     *
     * @param cutoutInfo the cutout, or null for a plain box. Its solid is all of space, one piece without faces, as
     *                   tiles are cut to their boxes anyway
     */
    public static List<Mesh3d> convexPieces(LittleTileBox box, LittleTileCutoutInfo cutoutInfo) {
        if (cutoutInfo == null) return Collections.singletonList(new Mesh3d(new ArrayList<>()));
        Mesh3dTemplateLoader.Template template = TEMPLATES.get(cutoutInfo.type);
        List<Mesh3d> pieces = new ArrayList<>();
        if (template == null) {
            pieces.addAll(tetrahedra(shapeMesh(cutoutInfo)));
        } else {
            for (Mesh3d piece : template.pieces) {
                pieces.add(piece.copy());
            }
        }
        Vector3i boxMin = new Vector3i(box.minX, box.minY, box.minZ);
        for (Mesh3d piece : pieces) {
            place(piece, cutoutInfo, cutoutInfo.size, cutoutInfo.pos, boxMin, cutoutInfo.orientation);
        }
        return pieces;
    }

    /**
     * The tetrahedra from the first point of the closed mesh to each of its triangles. Those of triangles facing away
     * from that point are wound inside out, so that they count negatively, and all of them add up to the mesh.
     */
    public static List<Mesh3d> tetrahedra(Mesh3d mesh) {
        List<Mesh3d> tetrahedra = new ArrayList<>();
        if (mesh.getTriangles().isEmpty()) return tetrahedra;
        Vector3d apex = mesh.getTriangles().get(0).getP1();
        for (Triangle3d triangle : mesh.getTriangles()) {
            Vector3d a = triangle.getP1(), b = triangle.getP2(), c = triangle.getP3();
            if (MeshCutter.det(a, b, c, apex) == 0) continue;
            List<Triangle3d> faces = new ArrayList<>(4);
            faces.add(new Triangle3d(new Vector3d(a), new Vector3d(b), new Vector3d(c)));
            faces.add(new Triangle3d(new Vector3d(apex), new Vector3d(b), new Vector3d(a)));
            faces.add(new Triangle3d(new Vector3d(apex), new Vector3d(c), new Vector3d(b)));
            faces.add(new Triangle3d(new Vector3d(apex), new Vector3d(a), new Vector3d(c)));
            tetrahedra.add(new Mesh3d(faces));
        }
        return tetrahedra;
    }

    /** @param cutoutSize the size of the whole cutout, in tile pixels */
    public static Mesh3d createMesh(LittleTileCutoutInfo cutoutInfo, Vector3i cutoutSize, Vector3i posCutout,
            Vector3i posSubMin, Vector3i posSubMax, Block block, int meta, int orientation) {
        Mesh3d mesh = shapeMesh(cutoutInfo);
        place(mesh, cutoutInfo, cutoutSize, posCutout, posSubMin, orientation);

        // From the points, as a pillar thicker than its bounds reaches past them
        double[] min = new double[3], max = new double[3];
        pixelBounds(mesh, min, max);
        int[] subMin = { posSubMin.x, posSubMin.y, posSubMin.z }, subMax = { posSubMax.x, posSubMax.y, posSubMax.z };
        for (int axis = 0; axis < 3; axis++) {
            // Bounds that only touch the box share no volume with it
            if (min[axis] >= subMax[axis] || max[axis] <= subMin[axis]) return new Mesh3d(new ArrayList<>());
        }

        List<Plane3d> planes = new ArrayList<>(6);
        if (max[1] > subMax[1]) planes.add(Plane3d.UP.moveAlongNormal(-(16 - subMax[1]) / 16.0));
        if (min[1] < subMin[1]) planes.add(Plane3d.DOWN.moveAlongNormal(-subMin[1] / 16.0));
        if (min[0] < subMin[0]) planes.add(Plane3d.WEST.moveAlongNormal(-subMin[0] / 16.0));
        if (max[0] > subMax[0]) planes.add(Plane3d.EAST.moveAlongNormal(-(16 - subMax[0]) / 16.0));
        if (max[2] > subMax[2]) planes.add(Plane3d.SOUTH.moveAlongNormal(-(16 - subMax[2]) / 16.0));
        if (min[2] < subMin[2]) planes.add(Plane3d.NORTH.moveAlongNormal(-subMin[2] / 16.0));
        mesh = mesh.cutByPlanes(planes);
        if (block != null) {
            mesh.setTextures(block, meta);
        }

        return mesh;
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
     * How much of the box the cutout covers, from the volume they share. A big shape split across many blocks leaves
     * most of them either empty or completely filled, which need no mesh at all.
     */
    public static TileCoverage classifyTile(LittleTileBox box, LittleTileCutoutInfo cutoutInfo) {
        double shared = MeshCollision.sharedVolume(box, cutoutInfo, box, null);
        if (shared <= MeshCutter.VOLUME_EPSILON) return TileCoverage.EMPTY;
        return shared >= MeshCollision.volume(box) - MeshCutter.VOLUME_EPSILON ? TileCoverage.FULL
                : TileCoverage.PARTIAL;
    }

    /**
     * Shrinks {@code box} to the tile pixels {@code mesh} reaches and moves the cutout along, so both still describe
     * the same shape. A cut can leave a thin sliver of a big shape in a tile, which would otherwise keep the whole box.
     *
     * @param mesh the mesh {@link #meshFromTile} builds for the box and cutout, must not be empty
     */
    public static void fitToMesh(LittleTileBox box, LittleTileCutoutInfo cutoutInfo, Mesh3d mesh) {
        double[] min = new double[3], max = new double[3];
        pixelBounds(mesh, min, max);

        // rounded outwards, as points where the mesh was cut can sit between pixels
        int minX = Math.max(box.minX, (int) Math.floor(min[0]));
        int minY = Math.max(box.minY, (int) Math.floor(min[1]));
        int minZ = Math.max(box.minZ, (int) Math.floor(min[2]));
        int maxX = Math.min(box.maxX, (int) Math.ceil(max[0]));
        int maxY = Math.min(box.maxY, (int) Math.ceil(max[1]));
        int maxZ = Math.min(box.maxZ, (int) Math.ceil(max[2]));

        // the cutout is relative to the box's min corner
        cutoutInfo.pos.add(box.minX - minX, box.minY - minY, box.minZ - minZ);
        box.minX = minX;
        box.minY = minY;
        box.minZ = minZ;
        box.maxX = maxX;
        box.maxY = maxY;
        box.maxZ = maxZ;
    }

    /** The bounds of the points of the mesh, in pixels. */
    private static void pixelBounds(Mesh3d mesh, double[] min, double[] max) {
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (Triangle3d triangle : mesh.getTriangles()) {
            for (Vector3d point : new Vector3d[] { triangle.getP1(), triangle.getP2(), triangle.getP3() }) {
                double[] pixels = { point.x * 16, point.y * 16, point.z * 16 };
                for (int axis = 0; axis < 3; axis++) {
                    min[axis] = Math.min(min[axis], pixels[axis]);
                    max[axis] = Math.max(max[axis], pixels[axis]);
                }
            }
        }
    }

    /** The surface of the shape, before {@link #place}. */
    private static Mesh3d shapeMesh(LittleTileCutoutInfo cutoutInfo) {
        return switch (cutoutInfo.type) {
            case PILLAR -> createWallMesh(cutoutInfo);
            case DEFORMED_BOX -> createDeformedBoxMesh(cutoutInfo);
            case BOX -> throw new RuntimeException("Invalid cutout BOX");
            default -> {
                Mesh3dTemplateLoader.Template template = TEMPLATES.get(cutoutInfo.type);
                if (template == null) throw new RuntimeException("Unknown cutout: " + cutoutInfo.type);
                yield template.surface.copy();
            }
        };
    }

    /** Moves a mesh made for the shape, see {@link #createMesh}, to where the tile has it, in blocks. */
    private static void place(Mesh3d mesh, LittleTileCutoutInfo cutoutInfo, Vector3i cutoutSize, Vector3i posCutout,
            Vector3i posSubMin, int orientation) {
        // A pillar is made at its size, the other shapes in the unit cube
        if (cutoutInfo.type != LittleTileShapeMode.PILLAR) {
            mesh.rotate(orientation, new Vector3d(1, 1, 1));
            mesh.scale(cutoutSize);
        }
        // In one step, so the same tile in neighbouring boxes ends up with the very same points
        mesh.translate(
                new Vector3d(
                        (posCutout.x + posSubMin.x) / 16.0,
                        (posCutout.y + posSubMin.y) / 16.0,
                        (posCutout.z + posSubMin.z) / 16.0));
    }
}
