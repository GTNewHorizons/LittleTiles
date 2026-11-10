package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

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
        LittleTileBox common = new LittleTileBox(
                Math.max(boxA.minX, boxB.minX),
                Math.max(boxA.minY, boxB.minY),
                Math.max(boxA.minZ, boxB.minZ),
                Math.min(boxA.maxX, boxB.maxX),
                Math.min(boxA.maxY, boxB.maxY),
                Math.min(boxA.maxZ, boxB.maxZ));
        if (common.minX >= common.maxX || common.minY >= common.maxY || common.minZ >= common.maxZ) return false;
        if (cutoutA == null && cutoutB == null) return true;

        List<Piece> piecesA = pieces(boxA, cutoutA);
        List<Piece> piecesB = pieces(boxB, cutoutB);
        double sixTimesVolume = 0;
        for (Piece pieceA : piecesA) {
            MeshCutter withinA = MeshCutter.box(common);
            // A cut that doesn't close leaves too little volume, so rather than missing a collision count it as one
            if (!pieceA.cut(withinA)) return true;
            if (withinA.isEmpty()) continue;
            for (Piece pieceB : piecesB) {
                MeshCutter shared = withinA.copy();
                if (!pieceB.cut(shared)) return true;
                sixTimesVolume += pieceA.sign * pieceB.sign * shared.volumeTimesSix();
            }
        }
        return sixTimesVolume / 6 > MeshCutter.VOLUME_EPSILON;
    }

    /** The pieces of the tile, see {@link Mesh3dUtil#convexPieces}. */
    private static List<Piece> pieces(LittleTileBox box, LittleTileCutoutInfo cutout) {
        List<Piece> pieces = new ArrayList<>();
        for (Mesh3d mesh : Mesh3dUtil.convexPieces(box, cutout)) {
            pieces.add(new Piece(mesh));
        }
        return pieces;
    }

    /** Face planes and sign of a convex piece, prepared once for all its pairwise intersections. */
    private static final class Piece {

        final List<Plane3d> planes = new ArrayList<>();
        final int sign;

        Piece(Mesh3d mesh) {
            sign = mesh.volume() < 0 ? -1 : 1;
            // Turned outwards, so that its planes face out
            if (sign < 0) mesh.flipWindingOrder();
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
