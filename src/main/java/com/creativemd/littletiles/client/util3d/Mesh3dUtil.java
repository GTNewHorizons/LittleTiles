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

public class Mesh3dUtil {

    private static Mesh3d MESH_SLOPE;

    public static void initializeMeshes() {
        MESH_SLOPE = Mesh3dObjLoader.load("slope");
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

    public static Mesh3d createMesh(LittleTileCutoutInfo cutoutInfo, Vector3d cutoutScale, Vector3d pos,
            Vector3i posCutout, Vector3i posSubMin, Vector3i posSubMax, Block block, int meta, int orientation) {
        Mesh3d mesh = switch (cutoutInfo.type) {
            case SLOPE -> MESH_SLOPE.copy();
            case PILLAR -> {
                cutoutScale = new Vector3d(1, 1, 1);
                yield createWallMesh(cutoutInfo);
            }
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
