package com.creativemd.littletiles.client.util3d;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector2d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public class Plane3d {

    public static final Plane3d WEST = new Plane3d(
            ForgeDirection.WEST,
            new Vector3i(0, 0, 1),
            new Vector3i(0, 1, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(0, 1, 0),
            false,
            true,
            new GridVector(0, 0, 0));

    public static final Plane3d EAST = new Plane3d(
            ForgeDirection.EAST,
            new Vector3i(0, 1, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(0, 0, 1),
            new Vector3i(0, 1, 0),
            true,
            true,
            new GridVector(Grid3d.BLOCK, 0, 0));

    public static final Plane3d SOUTH = new Plane3d(
            ForgeDirection.SOUTH,
            new Vector3i(1, 0, 0),
            new Vector3i(0, 1, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 1, 0),
            false,
            true,
            new GridVector(0, 0, Grid3d.BLOCK));

    public static final Plane3d NORTH = new Plane3d(
            ForgeDirection.NORTH,
            new Vector3i(0, 1, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 1, 0),
            true,
            true,
            new GridVector(0, 0, 0));

    public static final Plane3d UP = new Plane3d(
            ForgeDirection.UP,
            new Vector3i(0, 0, 1),
            new Vector3i(1, 0, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 0, 1),
            false,
            false,
            new GridVector(0, Grid3d.BLOCK, 0));

    public static final Plane3d DOWN = new Plane3d(
            ForgeDirection.DOWN,
            new Vector3i(1, 0, 0),
            new Vector3i(0, 0, 1),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 0, 1),
            false,
            false,
            new GridVector(0, 0, 0));

    public static Plane3d[] planes = { DOWN, UP, NORTH, SOUTH, WEST, EAST };

    /** A unit axis, so distances to the plane come out in grid units. */
    private final Vector3i normal;
    private final Vector3i vector1;
    private final Vector3i vector2;
    private final GridVector origin;
    private final Vector3i uAxis;
    private final Vector3i vAxis;
    private final boolean flipU;
    private final boolean flipV;
    private final ForgeDirection direction;

    public Plane3d(ForgeDirection direction, Vector3i vector1, Vector3i vector2, Vector3i uAxis, Vector3i vAxis,
            boolean flipU, boolean flipV, GridVector point) {
        this.direction = direction;
        this.origin = point;
        this.vector1 = vector1;
        this.vector2 = vector2;
        this.uAxis = uAxis;
        this.vAxis = vAxis;
        this.flipU = flipU;
        this.flipV = flipV;
        normal = new Vector3i(
                vector1.y * vector2.z - vector1.z * vector2.y,
                vector1.z * vector2.x - vector1.x * vector2.z,
                vector1.x * vector2.y - vector1.y * vector2.x);
    }

    /** Signed distance in grid units, positive on the side the normal points to. Exact. */
    public long getDistance(GridVector point) {
        return (long) normal.x * (point.x - origin.x) + (long) normal.y * (point.y - origin.y)
                + (long) normal.z * (point.z - origin.z);
    }

    /**
     * Where the edge between two points on opposite sides crosses the plane, rounded to the grid. The coordinate along
     * the normal lands exactly on the plane, as the plane sits on the grid itself. The edge is always interpolated from
     * the same end, so every triangle sharing it, and every tile cutting the same shape there, gets the same point.
     */
    public GridVector intersect(GridVector p1, GridVector p2) {
        if (!p1.isBefore(p2)) {
            GridVector temp = p1;
            p1 = p2;
            p2 = temp;
        }
        long d1 = getDistance(p1);
        long denominator = d1 - getDistance(p2);
        return new GridVector(
                (int) (p1.x + Grid3d.divRound(((long) p2.x - p1.x) * d1, denominator)),
                (int) (p1.y + Grid3d.divRound(((long) p2.y - p1.y) * d1, denominator)),
                (int) (p1.z + Grid3d.divRound(((long) p2.z - p1.z) * d1, denominator)));
    }

    public Vector3ic getNormal() {
        return normal;
    }

    /**
     * The plane that fits the triangle best, worked out from its own normal. Prefer {@link Triangle3d#getPlane}, which
     * pieces cut out of a triangle inherit, as their own normals are tilted by the rounding of the cut.
     */
    public static Plane3d getPlaneForTriangle(Triangle3d triangle) {
        return getPlaneForNormal(triangle.unnormalizedNormal());
    }

    /**
     * A face at 45 degrees to two axes fits both of their planes equally well. Which one is picked does not matter, but
     * it has to be the same one for every triangle of that face, otherwise their textures do not line up. The normal is
     * exact, so such a face ties exactly, and only a strictly better plane replaces the first one found.
     */
    public static Plane3d getPlaneForNormal(GridNormal normal) {
        Plane3d ret = null;
        long biggestDot = Long.MIN_VALUE;
        for (Plane3d plane : planes) {
            long dot = normal.x * plane.normal.x + normal.y * plane.normal.y + normal.z * plane.normal.z;
            if (dot > biggestDot) {
                biggestDot = dot;
                ret = plane;
            }
        }
        return ret;
    }

    /** The texture coordinates of a point on this plane, in blocks. Points off the plane are projected onto it. */
    public Vector2d mapTo2D(GridVector point) {
        long u = (long) uAxis.x * (point.x - origin.x) + (long) uAxis.y * (point.y - origin.y)
                + (long) uAxis.z * (point.z - origin.z);
        long v = (long) vAxis.x * (point.x - origin.x) + (long) vAxis.y * (point.y - origin.y)
                + (long) vAxis.z * (point.z - origin.z);
        return new Vector2d(Grid3d.toBlocks(u), Grid3d.toBlocks(v));
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

    /** This plane shifted by {@code distance} grid units along its normal. */
    public Plane3d moveAlongNormal(int distance) {
        GridVector newOrigin = new GridVector(
                origin.x + normal.x * distance,
                origin.y + normal.y * distance,
                origin.z + normal.z * distance);
        return new Plane3d(direction, vector1, vector2, uAxis, vAxis, flipU, flipV, newOrigin);
    }

}
