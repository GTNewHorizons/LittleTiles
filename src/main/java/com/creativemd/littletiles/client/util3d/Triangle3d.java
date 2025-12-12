package com.creativemd.littletiles.client.util3d;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Vector2d;
import org.joml.Vector3f;

import com.creativemd.creativecore.lib.Vector3d;

public class Triangle3d {

    private Vector3d p1, p2, p3;
    private Vector2d tex1, tex2, tex3;

    public Triangle3d(Vector3d p1, Vector3d p2, Vector3d p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public Vector3d getP1() {
        return p1;
    }

    public Vector3d getP2() {
        return p2;
    }

    public Vector3d getP3() {
        return p3;
    }

    public Vector2d getTex1() {
        return tex1;
    }

    public Vector2d getTex2() {
        return tex2;
    }

    public Vector2d getTex3() {
        return tex3;
    }

    public void scale(Vector3d vec) {
        scaleVector(p1, vec);
        scaleVector(p2, vec);
        scaleVector(p3, vec);
    }

    private void scaleVector(Vector3d vec, Vector3d scale) {
        vec.x *= scale.x;
        vec.y *= scale.y;
        vec.z *= scale.z;
    }

    public void translate(Vector3d vec) {
        p1.add(vec);
        p2.add(vec);
        p3.add(vec);
    }

    public Vector3d getNormal() {
        Vector3d edge1 = new Vector3d(p2);
        edge1.sub(p1);
        Vector3d edge2 = new Vector3d(p3);
        edge2.sub(p1);
        edge1.cross(edge1, edge2);
        edge1.normalize();
        return edge1;
    }

    private void flipWindingOrder() {
        Vector3d temp = p2;
        p2 = p3;
        p3 = temp;
    }

    public void ensureWindingOrder(Vector3d normal) {
        if (getNormal().dot(normal) < 0) {
            flipWindingOrder();
        }
    }

    private static Vector2d mapTexture(Plane3d plane, Vector3d point, IIcon icon) {
        Vector2d ret = plane.mapTo2D(point);
        if (plane.isFlipU()) {
            ret.x = 1 - ret.x;
        }
        if (plane.isFlipV()) {
            ret.y = 1 - ret.y;
        }
        ret.x = icon.getInterpolatedU(ret.x * 16);
        ret.y = icon.getInterpolatedV(ret.y * 16);
        return ret;
    }

    public void setTexture(Block block, int meta) {
        Plane3d plane = Plane3d.getPlaneForTriangle(this);
        ForgeDirection direction = plane.getDirection();
        IIcon icon = block.getIcon(direction.ordinal(), meta);
        tex1 = mapTexture(plane, p1, icon);
        tex2 = mapTexture(plane, p2, icon);
        tex3 = mapTexture(plane, p3, icon);
    }

    private static Vector3d rotateVector(Vector3d v, Matrix3f matrix) {
        Vector3f result = matrix.transform(new Vector3f((float) v.x, (float) v.y, (float) v.z));
        return new Vector3d(result.x, result.y, result.z);
    }

    public void rotate(int orientation) {
        Matrix3f matrix = OrientationMapper.fromId(orientation);
        p1 = rotateVector(p1, matrix);
        p2 = rotateVector(p2, matrix);
        p3 = rotateVector(p3, matrix);
    }

    public Triangle3d copy() {
        return new Triangle3d(new Vector3d(p1), new Vector3d(p2), new Vector3d(p3));
    }
}
