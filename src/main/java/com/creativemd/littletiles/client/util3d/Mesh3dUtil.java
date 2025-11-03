package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;

import net.minecraft.block.Block;

import org.joml.Vector3i;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;

public class Mesh3dUtil {

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
        return Mesh3dUtil.createMesh(cutoutInfo, cutoutScale, pos, cutoutInfo.pos, posSubMin, posSubMax, block, meta);
    }

    private static Mesh3d createSlopeMesh() {
        ArrayList<Triangle3d> triangles = new ArrayList<>();
        /* Top */
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 0, 1), new Vector3d(1, 1, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 1, 0), new Vector3d(0, 1, 0)));
        /* Bottom */
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(0, 0, 0), new Vector3d(1, 0, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(1, 0, 0), new Vector3d(1, 0, 1)));
        /* Back */
        triangles.add(new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(1, 1, 0), new Vector3d(1, 0, 0)));
        triangles.add(new Triangle3d(new Vector3d(0, 1, 0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 0)));
        /* Right */
        triangles.add(new Triangle3d(new Vector3d(1, 0, 1), new Vector3d(1, 0, 0), new Vector3d(1, 1, 0)));
        /* Left */
        triangles.add(new Triangle3d(new Vector3d(0, 0, 1), new Vector3d(0, 1, 0), new Vector3d(0, 0, 0)));

        return new Mesh3d(triangles);
    }

    public static Mesh3d createMesh(LittleTileCutoutInfo cutoutInfo, Vector3d cutoutScale, Vector3d pos,
            Vector3i posCutout, Vector3i posSubMin, Vector3i posSubMax, Block block, int meta) {
        Mesh3d mesh;
        if (cutoutInfo.type == LittleTileShapeMode.SLOPE) {
            mesh = createSlopeMesh();
        } else {
            throw new RuntimeException("Unknown cutout: " + cutoutInfo.type);
        }

        mesh.scale(cutoutScale);
        mesh.translate(posCutout);
        mesh.translate(new Vector3d(posSubMin.x / 16.0, posSubMin.y / 16.0, posSubMin.z / 16.0));

        Plane3d plane;
        plane = Plane3d.UP.moveAlongNormal(-(16 - posSubMax.y) / 16.0);
        mesh = mesh.cutByPlane(plane);
        plane = Plane3d.DOWN.moveAlongNormal(-posSubMin.y / 16.0);
        mesh = mesh.cutByPlane(plane);
        plane = Plane3d.WEST.moveAlongNormal(-posSubMin.x / 16.0);
        mesh = mesh.cutByPlane(plane);
        plane = Plane3d.EAST.moveAlongNormal(-(16 - posSubMax.x) / 16.0);
        mesh = mesh.cutByPlane(plane);
        plane = Plane3d.SOUTH.moveAlongNormal(-(16 - posSubMax.z) / 16.0);
        mesh = mesh.cutByPlane(plane);
        plane = Plane3d.NORTH.moveAlongNormal(-posSubMin.z / 16.0);
        mesh = mesh.cutByPlane(plane);

        if (block != null) {
            mesh.setTextures(block, meta);
        }
        mesh.translate(pos);

        return mesh;
    }
}
