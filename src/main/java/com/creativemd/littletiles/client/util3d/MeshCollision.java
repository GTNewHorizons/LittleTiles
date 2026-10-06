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
 * The first tile is cut down to the box both tiles have in common. The second is split into signed tetrahedra, see
 * {@link Mesh3dUtil#tetrahedra}: what the first shares with each of them is the first cut by its face planes, and these
 * volumes add up to the volume the tiles share.
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
        if (cutoutA == null) return intersects(boxB, cutoutB, boxA, null);

        MeshCutter withinA = new MeshCutter(Mesh3dUtil.solid(boxA, cutoutA).getTriangles());
        for (Plane3d plane : facePlanes(common)) {
            withinA.cut(plane);
        }
        if (cutoutB == null) return withinA.volumeTimesSix() / 6 > MeshCutter.VOLUME_EPSILON;

        double sixTimesVolume = 0;
        for (Mesh3d tetrahedron : Mesh3dUtil.tetrahedra(Mesh3dUtil.solid(boxB, cutoutB))) {
            if (withinA.isEmpty()) break;
            List<Triangle3d> faces = tetrahedron.getTriangles();
            Triangle3d base = faces.get(0);
            Vector3d apex = faces.get(1).getP1();
            // Inside out, it counts negatively, and its faces are turned outwards for their planes
            boolean insideOut = MeshCutter.det(base.getP1(), base.getP2(), base.getP3(), apex) < 0;
            MeshCutter shared = withinA.copy();
            for (Triangle3d face : faces) {
                shared.cut(
                        insideOut ? Triangle3d.planeThrough(face.getP1(), face.getP3(), face.getP2())
                                : Triangle3d.planeThrough(face.getP1(), face.getP2(), face.getP3()));
            }
            sixTimesVolume += insideOut ? -shared.volumeTimesSix() : shared.volumeTimesSix();
        }
        return sixTimesVolume / 6 > MeshCutter.VOLUME_EPSILON;
    }

    /** The planes of the box's faces, facing out of it. */
    private static List<Plane3d> facePlanes(LittleTileBox box) {
        List<Plane3d> planes = new ArrayList<>(6);
        planes.add(Plane3d.UP.moveAlongNormal(-(16 - box.maxY) / 16.0));
        planes.add(Plane3d.DOWN.moveAlongNormal(-box.minY / 16.0));
        planes.add(Plane3d.WEST.moveAlongNormal(-box.minX / 16.0));
        planes.add(Plane3d.EAST.moveAlongNormal(-(16 - box.maxX) / 16.0));
        planes.add(Plane3d.SOUTH.moveAlongNormal(-(16 - box.maxZ) / 16.0));
        planes.add(Plane3d.NORTH.moveAlongNormal(-box.minZ / 16.0));
        return planes;
    }
}
