package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Vector2d;
import org.joml.Vector3f;

import com.creativemd.creativecore.lib.Vector3d;

public class Triangle3d {

    private static final double SPLIT_EPSILON = 1.0E-7;

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
        translate(vec.x, vec.y, vec.z);
    }

    public void translate(double x, double y, double z) {
        p1.add(x, y, z);
        p2.add(x, y, z);
        p3.add(x, y, z);
    }

    public Vector3d getNormal() {
        Vector3d normal = unnormalizedNormal();
        normal.normalize();
        return normal;
    }

    /**
     * Computes this triangle's signed solid angle relative to a point.
     *
     * The value is used by {@link Mesh3d#containsPoint(Vector3f)}: a closed mesh sums the signed solid angles from all
     * its triangles, producing a large absolute value for interior points and approximately zero for exterior points.
     * The sign depends on the triangle winding order.
     *
     * @param point The point to measure from.
     * @return The signed solid angle in radians.
     */
    public double signedSolidAngle(Vector3f point) {
        double ax = p1.x - point.x;
        double ay = p1.y - point.y;
        double az = p1.z - point.z;

        double bx = p2.x - point.x;
        double by = p2.y - point.y;
        double bz = p2.z - point.z;

        double cx = p3.x - point.x;
        double cy = p3.y - point.y;
        double cz = p3.z - point.z;

        double al = Math.sqrt(ax * ax + ay * ay + az * az);
        double bl = Math.sqrt(bx * bx + by * by + bz * bz);
        double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);

        double det = ax * (by * cz - bz * cy) - ay * (bx * cz - bz * cx) + az * (bx * cy - by * cx);

        double denom = al * bl * cl + (ax * bx + ay * by + az * bz) * cl
                + (bx * cx + by * cy + bz * cz) * al
                + (cx * ax + cy * ay + cz * az) * bl;

        return 2.0 * Math.atan2(det, denom);
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

    /**
     * Removes the area covered by another coplanar triangle.
     *
     * @param other triangle whose area is removed from this triangle
     * @return triangles covering the part of this triangle not covered by {@code other}
     */
    public List<Triangle3d> split(Triangle3d other) {
        List<Triangle3d> result = new ArrayList<>();
        Vector3d normal = unnormalizedNormal();
        double doubleArea = normal.length();

        if (doubleArea <= SPLIT_EPSILON || !other.isCoplanarWith(this, normal, doubleArea)) {
            result.add(copy());
            return result;
        }
        normal.scale(1 / doubleArea);

        // ordered counter-clockwise around the normal, so that a point is inside the cutter when it is left of all
        // three of its edges
        List<Vector3d> cutter = other.corners();
        double cutterWinding = signedArea(cutter.get(0), cutter.get(1), cutter.get(2), normal);
        if (Math.abs(cutterWinding) <= SPLIT_EPSILON) {
            result.add(copy());
            return result;
        }
        if (cutterWinding < 0) {
            Collections.reverse(cutter);
        }

        List<Vector3d> inside = corners();
        for (int i = 0; i < 3 && !inside.isEmpty(); i++) {
            List<Vector3d> outside = new ArrayList<>();
            inside = clip(inside, cutter.get(i), cutter.get((i + 1) % 3), normal, outside);
            addTriangulated(result, outside, normal);
        }

        return result;
    }

    private List<Vector3d> corners() {
        List<Vector3d> corners = new ArrayList<>(3);
        corners.add(new Vector3d(p1));
        corners.add(new Vector3d(p2));
        corners.add(new Vector3d(p3));
        return corners;
    }

    private Vector3d unnormalizedNormal() {
        Vector3d edge1 = new Vector3d(p2);
        edge1.sub(p1);
        Vector3d edge2 = new Vector3d(p3);
        edge2.sub(p1);
        edge1.cross(edge1, edge2);
        return edge1;
    }

    /** Cheap broad-phase test used before attempting the allocating polygon split. */
    public boolean boundsOverlap(Triangle3d other) {
        return min(p1.x, p2.x, p3.x) <= max(other.p1.x, other.p2.x, other.p3.x) + SPLIT_EPSILON
                && max(p1.x, p2.x, p3.x) + SPLIT_EPSILON >= min(other.p1.x, other.p2.x, other.p3.x)
                && min(p1.y, p2.y, p3.y) <= max(other.p1.y, other.p2.y, other.p3.y) + SPLIT_EPSILON
                && max(p1.y, p2.y, p3.y) + SPLIT_EPSILON >= min(other.p1.y, other.p2.y, other.p3.y)
                && min(p1.z, p2.z, p3.z) <= max(other.p1.z, other.p2.z, other.p3.z) + SPLIT_EPSILON
                && max(p1.z, p2.z, p3.z) + SPLIT_EPSILON >= min(other.p1.z, other.p2.z, other.p3.z);
    }

    /** Whether this triangle and another lie in the same plane. */
    public boolean isCoplanar(Triangle3d other) {
        Vector3d normal = unnormalizedNormal();
        double normalLength = normal.length();
        return normalLength > SPLIT_EPSILON && other.isCoplanarWith(this, normal, normalLength);
    }

    private static double min(double a, double b, double c) {
        return Math.min(a, Math.min(b, c));
    }

    private static double max(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    /** Whether all corners of this triangle lie in the plane of {@code plane}, whose (unnormalized) normal is given. */
    private boolean isCoplanarWith(Triangle3d plane, Vector3d normal, double normalLength) {
        return plane.distanceToPlane(p1, normal) <= SPLIT_EPSILON * normalLength
                && plane.distanceToPlane(p2, normal) <= SPLIT_EPSILON * normalLength
                && plane.distanceToPlane(p3, normal) <= SPLIT_EPSILON * normalLength;
    }

    private double distanceToPlane(Vector3d point, Vector3d normal) {
        Vector3d offset = new Vector3d(point);
        offset.sub(p1);
        return Math.abs(offset.dot(normal));
    }

    /** Twice the signed area of the triangle {@code a, b, point}, positive when the point is left of {@code a -> b}. */
    private static double signedArea(Vector3d a, Vector3d b, Vector3d point, Vector3d normal) {
        Vector3d edge = new Vector3d(b);
        edge.sub(a);
        Vector3d toPoint = new Vector3d(point);
        toPoint.sub(a);
        edge.cross(edge, toPoint);
        return edge.dot(normal);
    }

    /**
     * Splits the polygon along the line {@code edgeStart -> edgeEnd}. Corners on the line end up in both halves, so
     * that neither half leaves a gap.
     *
     * @param outside collects the part right of the line
     * @return the part left of the line
     */
    private static List<Vector3d> clip(List<Vector3d> polygon, Vector3d edgeStart, Vector3d edgeEnd, Vector3d normal,
            List<Vector3d> outside) {
        List<Vector3d> inside = new ArrayList<>();
        Vector3d previous = polygon.get(polygon.size() - 1);
        double previousSide = signedArea(edgeStart, edgeEnd, previous, normal);
        boolean previousInside = previousSide >= -SPLIT_EPSILON;
        boolean previousOutside = previousSide <= SPLIT_EPSILON;

        for (Vector3d current : polygon) {
            double currentSide = signedArea(edgeStart, edgeEnd, current, normal);
            boolean currentInside = currentSide >= -SPLIT_EPSILON;
            boolean currentOutside = currentSide <= SPLIT_EPSILON;

            if (currentInside != previousInside || currentOutside != previousOutside) {
                double denominator = previousSide - currentSide;
                double amount = Math.abs(denominator) <= SPLIT_EPSILON ? 0 : previousSide / denominator;
                amount = Math.max(0, Math.min(1, amount));
                Vector3d crossing = new Vector3d(previous);
                crossing.interpolate(current, amount);
                if (currentInside != previousInside) {
                    inside.add(crossing);
                }
                if (currentOutside != previousOutside) {
                    outside.add(new Vector3d(crossing));
                }
            }
            if (currentInside) {
                inside.add(new Vector3d(current));
            }
            if (currentOutside) {
                outside.add(new Vector3d(current));
            }

            previous = current;
            previousSide = currentSide;
            previousInside = currentInside;
            previousOutside = currentOutside;
        }
        return inside;
    }

    private static void addTriangulated(List<Triangle3d> triangles, List<Vector3d> polygon, Vector3d normal) {
        // the polygon is always convex, since it originates from a triangle cut by straight lines
        for (int i = 1; i < polygon.size() - 1; i++) {
            Triangle3d triangle = new Triangle3d(
                    new Vector3d(polygon.get(0)),
                    new Vector3d(polygon.get(i)),
                    new Vector3d(polygon.get(i + 1)));
            if (triangle.unnormalizedNormal().length() > SPLIT_EPSILON) {
                triangle.ensureWindingOrder(normal);
                triangles.add(triangle);
            }
        }
    }

    public Triangle3d copy() {
        return new Triangle3d(new Vector3d(p1), new Vector3d(p2), new Vector3d(p3));
    }
}
