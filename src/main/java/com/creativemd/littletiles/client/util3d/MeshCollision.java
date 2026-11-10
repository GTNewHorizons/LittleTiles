package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Whether tiles overlap, measured by the volume they share. Tiles that only touch share none, however their surfaces
 * are triangulated and rounded, so this needs no tolerances for touching faces, edges or corners.
 * <p>
 * Each tile is made of convex pieces, see {@link Mesh3dUtil#convexPieces}. What two convex pieces share within the box
 * both tiles have in common is that box cut by the face planes of both pieces, and the volumes of all pairs of pieces
 * add up to the volume the tiles share.
 */
public final class MeshCollision {

    private MeshCollision() {}

    /**
     * Whether two tiles share volume.
     *
     * @param cutoutA the cutout of the first tile, positioned relative to {@code boxA}, or null for a plain box
     * @param cutoutB the same for the second tile
     */
    public static boolean intersects(LittleTileBox boxA, LittleTileCutoutInfo cutoutA, LittleTileBox boxB,
            LittleTileCutoutInfo cutoutB) {
        return sharedVolume(boxA, cutoutA, boxB, cutoutB) > MeshCutter.VOLUME_EPSILON;
    }

    /** The volume two tiles share, in cubic blocks, see {@link #intersects}. */
    static double sharedVolume(LittleTileBox boxA, LittleTileCutoutInfo cutoutA, LittleTileBox boxB,
            LittleTileCutoutInfo cutoutB) {
        LittleTileBox common = new LittleTileBox(
                Math.max(boxA.minX, boxB.minX),
                Math.max(boxA.minY, boxB.minY),
                Math.max(boxA.minZ, boxB.minZ),
                Math.min(boxA.maxX, boxB.maxX),
                Math.min(boxA.maxY, boxB.maxY),
                Math.min(boxA.maxZ, boxB.maxZ));
        if (common.minX >= common.maxX || common.minY >= common.maxY || common.minZ >= common.maxZ) return 0;
        if (cutoutA == null && cutoutB == null) return volume(common);

        List<Piece> piecesA = pieces(boxA, cutoutA, common);
        if (piecesA == null) return Double.POSITIVE_INFINITY;
        if (piecesA.isEmpty()) return 0;
        List<Piece> piecesB = pieces(boxB, cutoutB, common);
        if (piecesB == null) return Double.POSITIVE_INFINITY;
        if (piecesB.isEmpty()) return 0;
        double sixTimesVolume = 0;
        for (Piece pieceA : piecesA) {
            MeshCutter withinA = MeshCutter.box(common);
            // A cut that doesn't close leaves too little volume, so rather than missing a collision count it as all
            if (!pieceA.cut(withinA)) return Double.POSITIVE_INFINITY;
            if (withinA.isEmpty()) continue;
            for (Piece pieceB : piecesB) {
                MeshCutter shared = withinA.copy();
                if (!pieceB.cut(shared)) return Double.POSITIVE_INFINITY;
                sixTimesVolume += pieceA.sign * pieceB.sign * shared.volumeTimesSix();
            }
        }
        return sixTimesVolume / 6;
    }

    /** The volume of the box, in cubic blocks. */
    static double volume(LittleTileBox box) {
        return (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ) / (16.0 * 16 * 16);
    }

    /**
     * The pieces of the tile, see {@link Mesh3dUtil#convexPieces}, less those sharing no volume with the box. An
     * inverted tile is its box less those of its shape. A tile with further shapes is made of its
     * {@link #disjointPieces}.
     *
     * @return null if a cut could not be closed
     */
    private static List<Piece> pieces(LittleTileBox box, LittleTileCutoutInfo cutout, LittleTileBox common) {
        List<Piece> pieces = new ArrayList<>();
        if (cutout != null && !cutout.furtherShapes.isEmpty()) {
            List<MeshCutter> disjoint = disjointPieces(box, cutout);
            if (disjoint == null) return null;
            for (MeshCutter piece : disjoint) {
                pieces.add(new Piece(piece.facePlanes()));
            }
            return pieces;
        }
        boolean inverted = cutout != null && cutout.inverted;
        if (inverted) pieces.add(new Piece(new Mesh3d(new ArrayList<>()), 1));
        for (Mesh3d mesh : Mesh3dUtil.convexPieces(box, cutout)) {
            if (reachesInto(mesh, common)) pieces.add(new Piece(mesh, inverted ? -1 : 1));
        }
        return pieces;
    }

    /**
     * The tile, its cutout having {@link LittleTileCutoutInfo#furtherShapes}, as convex pieces that don't overlap: its
     * box less its shape if it is inverted, else its shape within the box, less the further shapes either way, or less
     * all outside those that are inverted.
     *
     * @return null if a cut could not be closed
     */
    public static List<MeshCutter> disjointPieces(LittleTileBox box, LittleTileCutoutInfo cutout) {
        List<MeshCutter> parts = new ArrayList<>();
        parts.add(MeshCutter.box(box));
        List<LittleTileCutoutInfo> shapes = cutout.placedShapes();
        for (int i = 0; i < shapes.size() && parts != null; i++) {
            parts = split(parts, box, shapes.get(i), i == 0 ? !cutout.inverted : shapes.get(i).inverted);
        }
        return parts;
    }

    /**
     * The parts lying inside the shape, or those lying outside it. Each piece of the shape splits the parts it reaches
     * into: into what lies inside it, and what lies beyond each of its planes in turn. The pieces of a shape can count
     * negatively, see {@link Mesh3dUtil#tetrahedra}, so which parts lie inside is only known once all of them are
     * through, from the pieces they lie in adding up to the shape.
     *
     * @return null if a cut could not be closed
     */
    private static List<MeshCutter> split(List<MeshCutter> parts, LittleTileBox box, LittleTileCutoutInfo shape,
            boolean inside) {
        // How many pieces of the shape each part lies in, each counted by its sign
        IntArrayList depths = new IntArrayList(new int[parts.size()]);
        for (Mesh3d mesh : Mesh3dUtil.convexPieces(box, shape)) {
            Piece piece = new Piece(mesh, 1);
            List<MeshCutter> split = new ArrayList<>(parts.size());
            IntArrayList splitDepths = new IntArrayList(parts.size());
            for (int i = 0; i < parts.size(); i++) {
                MeshCutter part = parts.get(i), within = part.copy();
                List<MeshCutter> beyond = new ArrayList<>();
                for (Triangle3d face : piece.faces) {
                    MeshCutter beyondFace = within.copy();
                    // Through the same points the other way round, so both sides are cut at the very same plane
                    Plane3d inwards = Triangle3d.planeThrough(face.getP1(), face.getP3(), face.getP2());
                    if (!beyondFace.cut(inwards)) return null;
                    if (!isTiny(beyondFace)) beyond.add(beyondFace);
                    if (!within.cut(Triangle3d.planeThrough(face.getP1(), face.getP2(), face.getP3()))) return null;
                    if (isTiny(within)) break;
                }
                if (isTiny(within)) {
                    split.add(part);
                    splitDepths.add(depths.getInt(i));
                    continue;
                }
                for (MeshCutter outside : beyond) {
                    split.add(outside);
                    splitDepths.add(depths.getInt(i));
                }
                split.add(within);
                splitDepths.add(depths.getInt(i) + piece.sign);
            }
            parts = split;
            depths = splitDepths;
        }
        List<MeshCutter> kept = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            if ((depths.getInt(i) != 0) == inside) kept.add(parts.get(i));
        }
        return kept;
    }

    private static boolean isTiny(MeshCutter part) {
        return part.volumeTimesSix() <= 6 * MeshCutter.VOLUME_EPSILON;
    }

    /**
     * Whether the points of the piece reach into the box rather than only up to it, as otherwise they share no volume.
     * A piece without faces is all of space.
     */
    private static boolean reachesInto(Mesh3d piece, LittleTileBox box) {
        if (piece.getTriangles().isEmpty()) return true;
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (Triangle3d triangle : piece.getTriangles()) {
            for (Vector3d point : new Vector3d[] { triangle.getP1(), triangle.getP2(), triangle.getP3() }) {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                minZ = Math.min(minZ, point.z);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
                maxZ = Math.max(maxZ, point.z);
            }
        }
        return minX < box.maxX / 16.0 && maxX > box.minX / 16.0
                && minY < box.maxY / 16.0
                && maxY > box.minY / 16.0
                && minZ < box.maxZ / 16.0
                && maxZ > box.minZ / 16.0;
    }

    /** Face planes and sign of a convex piece, prepared once for all its pairwise intersections. */
    private static final class Piece {

        final List<Plane3d> planes = new ArrayList<>();
        /** A triangle of each face, in the order of {@link #planes}. */
        final List<Triangle3d> faces = new ArrayList<>();
        final int sign;

        /** A piece added, with the planes of its faces facing out. */
        Piece(List<Plane3d> planes) {
            this.planes.addAll(planes);
            sign = 1;
        }

        /** @param sign -1 if the piece is taken away rather than added */
        Piece(Mesh3d mesh, int sign) {
            boolean insideOut = mesh.volume() < 0;
            this.sign = insideOut ? -sign : sign;
            // Turned outwards, so that its planes face out
            if (insideOut) mesh.flipWindingOrder();
            Object polygon = null;
            for (Triangle3d triangle : mesh.getTriangles()) {
                // The triangles of a polygon come one after another, and one plane does for all of them
                if (polygon != null && triangle.getPolygon() == polygon) continue;
                polygon = triangle.getPolygon();
                planes.add(triangle.getFacePlane());
                faces.add(triangle);
            }
        }

        /** @return false if a cut could not be closed, see {@link MeshCutter#cut} */
        boolean cut(MeshCutter cutter) {
            for (Plane3d plane : planes) {
                if (cutter.isEmpty()) return true;
                if (!cutter.cut(plane)) return false;
            }
            return true;
        }
    }
}
