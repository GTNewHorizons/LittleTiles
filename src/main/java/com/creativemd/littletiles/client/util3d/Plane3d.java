package com.creativemd.littletiles.client.util3d;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector2d;

import com.creativemd.creativecore.lib.Vector3d;

public class Plane3d {

    public static final Plane3d WEST = new Plane3d(
            ForgeDirection.WEST,
            new Vector3d(0, 0, 1),
            new Vector3d(0, 1, 0),
            new Vector3d(0, 0, 1),
            new Vector3d(0, 1, 0),
            false,
            true,
            new Vector3d(0, 0, 0));

    public static final Plane3d EAST = new Plane3d(
            ForgeDirection.EAST,
            new Vector3d(0, 1, 0),
            new Vector3d(0, 0, 1),
            new Vector3d(0, 0, 1),
            new Vector3d(0, 1, 0),
            true,
            true,
            new Vector3d(1, 0, 0));

    public static final Plane3d SOUTH = new Plane3d(
            ForgeDirection.SOUTH,
            new Vector3d(1, 0, 0),
            new Vector3d(0, 1, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(0, 1, 0),
            false,
            true,
            new Vector3d(0, 0, 1));

    public static final Plane3d NORTH = new Plane3d(
            ForgeDirection.NORTH,
            new Vector3d(0, 1, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(0, 1, 0),
            true,
            true,
            new Vector3d(0, 0, 0));

    public static final Plane3d UP = new Plane3d(
            ForgeDirection.UP,
            new Vector3d(0, 0, 1),
            new Vector3d(1, 0, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(0, 0, 1),
            false,
            false,
            new Vector3d(0, 1, 0));

    public static final Plane3d DOWN = new Plane3d(
            ForgeDirection.DOWN,
            new Vector3d(1, 0, 0),
            new Vector3d(0, 0, 1),
            new Vector3d(1, 0, 0),
            new Vector3d(0, 0, 1),
            false,
            false,
            new Vector3d(0, 0, 0));

    public static Plane3d[] planes = { DOWN, UP, NORTH, SOUTH, WEST, EAST };

    private final double A, B, C, D;
    private final Vector3d normal;
    private final Vector3d vector1;
    private final Vector3d vector2;
    private final Vector3d origin;
    private final Vector3d uAxis;
    private final Vector3d vAxis;
    private final boolean flipU;
    private final boolean flipV;
    private final ForgeDirection direction;

    public Plane3d(ForgeDirection direction, Vector3d vector1, Vector3d vector2, Vector3d uAxis, Vector3d vAxis,
            boolean flipU, boolean flipV, Vector3d point) {
        this.direction = direction;
        this.origin = point;
        this.vector1 = vector1;
        this.vector2 = vector2;
        this.uAxis = uAxis;
        this.vAxis = vAxis;
        this.flipU = flipU;
        this.flipV = flipV;
        normal = new Vector3d();
        normal.cross(vector1, vector2);
        normal.normalize();
        this.A = normal.x;
        this.B = normal.y;
        this.C = normal.z;
        this.D = -(A * point.x + B * point.y + C * point.z);
    }

    public double getDistance(Vector3d point) {
        return A * point.x + B * point.y + C * point.z + D;
    }

    public boolean isPointAbove(Vector3d point) {
        double distance = getDistance(point);
        return distance > 0;
    }

    public Vector3d intersect(Vector3d p1, Vector3d p2) {
        double d1 = getDistance(p1);
        double d2 = getDistance(p2);

        double t = d1 / (d1 - d2); // Find the interpolation factor

        return new Vector3d(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y), p1.z + t * (p2.z - p1.z));
    }

    public Vector3d getNormal() {
        return normal;
    }

    public static Plane3d getPlaneForTriangle(Triangle3d triangle) {
        Vector3d normal = triangle.getNormal();
        Plane3d ret = null;
        double biggestDot = -1;
        for (Plane3d plane : planes) {
            double dot = plane.getNormal().dot(normal);
            if (dot > biggestDot) {
                biggestDot = dot;
                ret = plane;
            }
        }
        return ret;
    }

    public Vector2d mapTo2D(Vector3d point) {
        // project onto plane
        double distance = getDistance(point);
        Vector3d projected = new Vector3d(
                point.x - normal.x * distance,
                point.y - normal.y * distance,
                point.z - normal.z * distance);

        projected.sub(origin);

        // coordinates in plane
        double u = projected.dot(uAxis);
        double v = projected.dot(vAxis);

        return new Vector2d(u, v);
    }

    public ForgeDirection getDirection() {
        return direction;
    }

    public boolean isFlipU() {
        return flipU;
    }

    public boolean isFlipV() {
        return flipV;
    }

    public Plane3d moveAlongNormal(double distance) {
        Vector3d newOrigin = new Vector3d(
                origin.x + normal.x * distance,
                origin.y + normal.y * distance,
                origin.z + normal.z * distance);
        return new Plane3d(direction, vector1, vector2, uAxis, vAxis, flipU, flipV, newOrigin);
    }

}
