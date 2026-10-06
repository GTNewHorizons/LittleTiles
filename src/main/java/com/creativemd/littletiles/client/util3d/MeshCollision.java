package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

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
        if (piecesA.isEmpty()) return 0;
        List<Piece> piecesB = pieces(boxB, cutoutB, common);
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
     * inverted tile is its box less those of its shape.
     */
    private static List<Piece> pieces(LittleTileBox box, LittleTileCutoutInfo cutout, LittleTileBox common) {
        List<Piece> pieces = new ArrayList<>();
        boolean inverted = cutout != null && cutout.inverted;
        if (inverted) pieces.add(new Piece(new Mesh3d(new ArrayList<>()), 1));
        for (Mesh3d mesh : Mesh3dUtil.convexPieces(box, cutout)) {
            if (reachesInto(mesh, common)) pieces.add(new Piece(mesh, inverted ? -1 : 1));
        }
        return pieces;
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
        final int sign;

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
