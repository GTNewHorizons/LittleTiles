package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector2d;
import org.joml.Vector3f;
import org.joml.Vector3ic;

/** A triangle on the integer grid of {@link Grid3d}. */
public class Triangle3d {

    private GridVector p1, p2, p3;
    private Vector2d tex1, tex2, tex3;
    /** See {@link #getPlane}, null until first needed. */
    private Plane3d plane;

    public Triangle3d(GridVector p1, GridVector p2, GridVector p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public GridVector getP1() {
        return p1;
    }

    public GridVector getP2() {
        return p2;
    }

    public GridVector getP3() {
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

    public void scale(Vector3ic factor) {
        p1.mul(factor);
        p2.mul(factor);
        p3.mul(factor);
        plane = null;
    }

    /**
     * The block plane this triangle is textured from. Worked out from its own normal on first use. A piece cut out of
     * another triangle takes that triangle's plane instead: the cut tilts the piece slightly, which could tip a face at
     * 45 degrees to the other of its two planes and break up its texture.
     * <p>
     * Kept once known, so the points must not be moved afterwards other than by translating.
     */
    public Plane3d getPlane() {
        if (plane == null) {
            plane = Plane3d.getPlaneForTriangle(this);
        }
        return plane;
    }

    /** Makes this a piece of {@code parent}, see {@link #getPlane}. */
    void inheritPlane(Triangle3d parent) {
        plane = parent.getPlane();
    }

    void setPlane(Plane3d plane) {
        this.plane = plane;
    }

    public void translate(int x, int y, int z) {
        p1.add(x, y, z);
        p2.add(x, y, z);
        p3.add(x, y, z);
    }

    /** Moves the triangle {@code distance} grid units along its normal, rounded to the grid. */
    public void inflate(int distance) {
        Vector3f n = getNormal();
        translate((int) Math.round(n.x * distance), (int) Math.round(n.y * distance), (int) Math.round(n.z * distance));
    }

    /** The direction the triangle faces, as a unit vector. Not exact, so only fit for lighting and texturing. */
    public Vector3f getNormal() {
        return unnormalizedNormal().toUnitVector();
    }

    /** The exact normal, with a length of twice the area in square grid units. Zero for a degenerate triangle. */
    public GridNormal unnormalizedNormal() {
        return p1.cross(p2, p3);
    }

    public boolean isDegenerate() {
        return isDegenerate(p1, p2, p3);
    }

    /** Whether the three points have no area between them, which covers coinciding points as well. Exact. */
    public static boolean isDegenerate(GridVector p1, GridVector p2, GridVector p3) {
        return p1.cross(p2, p3).isZero();
    }

    /**
     * Computes this triangle's signed solid angle relative to a point.
     *
     * The value is used by {@link Mesh3d#containsPoint(Vector3f)}: a closed mesh sums the signed solid angles from all
     * its triangles, producing a large absolute value for interior points and approximately zero for exterior points.
     * The sign depends on the triangle winding order.
     *
     * @param point The point to measure from, in blocks.
     * @return The signed solid angle in radians.
     */
    public double signedSolidAngle(Vector3f point) {
        double ax = p1.blockX() - point.x;
        double ay = p1.blockY() - point.y;
        double az = p1.blockZ() - point.z;

        double bx = p2.blockX() - point.x;
        double by = p2.blockY() - point.y;
        double bz = p2.blockZ() - point.z;

        double cx = p3.blockX() - point.x;
        double cy = p3.blockY() - point.y;
        double cz = p3.blockZ() - point.z;

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
        GridVector temp = p2;
        p2 = p3;
        p3 = temp;
    }

    public void ensureWindingOrder(Vector3ic normal) {
        ensureWindingOrder(normal.x(), normal.y(), normal.z());
    }

    public void ensureWindingOrder(GridNormal normal) {
        ensureWindingOrder(normal.x, normal.y, normal.z);
    }

    /**
     * Only the sign of the dot product matters and the normals compared are never close to perpendicular, so doubles
     * are exact enough here, and unlike longs they cannot overflow on two cross products multiplied.
     */
    private void ensureWindingOrder(double x, double y, double z) {
        GridNormal own = unnormalizedNormal();
        if ((double) own.x * x + (double) own.y * y + (double) own.z * z < 0) {
            flipWindingOrder();
        }
    }

    private static Vector2d mapTexture(Plane3d plane, GridVector point, IIcon icon) {
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

    /** The block face this triangle is textured from. */
    public ForgeDirection getFaceDirection() {
        return getPlane().getDirection();
    }

    public void setTexture(Block block, int meta) {
        Plane3d plane = getPlane();
        ForgeDirection direction = plane.getDirection();
        IIcon icon = block.getIcon(direction.ordinal(), meta);
        tex1 = mapTexture(plane, p1, icon);
        tex2 = mapTexture(plane, p2, icon);
        tex3 = mapTexture(plane, p3, icon);
    }

    public void rotate(int orientation) {
        p1.rotate(orientation);
        p2.rotate(orientation);
        p3.rotate(orientation);
        // Mirrored orientations (used for flipped meshes) invert the winding order, so it has to be restored here to
        // keep the normal pointing outwards.
        if (OrientationMapper.isMirrored(orientation)) {
            flipWindingOrder();
        }
        plane = null;
    }

    /**
     * Removes the area covered by another coplanar triangle.
     *
     * @param other triangle whose area is removed from this triangle
     * @return triangles covering the part of this triangle not covered by {@code other}
     */
    public List<Triangle3d> split(Triangle3d other) {
        List<Triangle3d> result = new ArrayList<>();
        GridNormal normal = unnormalizedNormal();

        if (normal.isZero() || !other.isCoplanarWith(this, normal)) {
            result.add(copy());
            return result;
        }
        int axis = normal.dominantAxis();

        // ordered counter-clockwise around the normal, so that a point is inside the cutter when it is left of all
        // three of its edges
        List<GridVector> cutter = other.corners();
        long cutterWinding = signedArea(cutter.get(0), cutter.get(1), cutter.get(2), normal, axis);
        if (cutterWinding == 0) {
            result.add(copy());
            return result;
        }
        if (cutterWinding < 0) {
            Collections.reverse(cutter);
        }

        List<GridVector> inside = corners();
        for (int i = 0; i < 3 && !inside.isEmpty(); i++) {
            List<GridVector> outside = new ArrayList<>();
            inside = clip(inside, cutter.get(i), cutter.get((i + 1) % 3), normal, axis, outside);
            addTriangulated(result, outside, normal, this);
        }

        return result;
    }

    private List<GridVector> corners() {
        List<GridVector> corners = new ArrayList<>(3);
        corners.add(new GridVector(p1));
        corners.add(new GridVector(p2));
        corners.add(new GridVector(p3));
        return corners;
    }

    /** Cheap broad-phase test used before attempting the allocating polygon split. Touching counts as overlapping. */
    public boolean boundsOverlap(Triangle3d other) {
        return min(p1.x, p2.x, p3.x) <= max(other.p1.x, other.p2.x, other.p3.x)
                && max(p1.x, p2.x, p3.x) >= min(other.p1.x, other.p2.x, other.p3.x)
                && min(p1.y, p2.y, p3.y) <= max(other.p1.y, other.p2.y, other.p3.y)
                && max(p1.y, p2.y, p3.y) >= min(other.p1.y, other.p2.y, other.p3.y)
                && min(p1.z, p2.z, p3.z) <= max(other.p1.z, other.p2.z, other.p3.z)
                && max(p1.z, p2.z, p3.z) >= min(other.p1.z, other.p2.z, other.p3.z);
    }

    /**
     * Whether this triangle and another one in the same plane cover some area together, rather than at most touching at
     * an edge or a corner. Exact: two triangles in a plane are apart exactly when one of their edges has all of the
     * other on its outside, the edge itself counting as outside.
     */
    public boolean overlaps(Triangle3d other) {
        GridNormal normal = unnormalizedNormal();
        if (normal.isZero() || other.isDegenerate()) {
            return false;
        }
        int axis = normal.dominantAxis();
        return !hasSeparatingEdge(this, other, normal, axis) && !hasSeparatingEdge(other, this, normal, axis);
    }

    /** Whether one of the edges of {@code triangle} has all corners of {@code other} on its outside or on it. */
    private static boolean hasSeparatingEdge(Triangle3d triangle, Triangle3d other, GridNormal normal, int axis) {
        // the inside of every edge is the side the triangle's remaining corner is on
        long winding = Long.signum(signedArea(triangle.p1, triangle.p2, triangle.p3, normal, axis));
        GridVector[] corners = { triangle.p1, triangle.p2, triangle.p3 };
        for (int i = 0; i < 3; i++) {
            GridVector start = corners[i];
            GridVector end = corners[(i + 1) % 3];
            if (winding * signedArea(start, end, other.p1, normal, axis) <= 0
                    && winding * signedArea(start, end, other.p2, normal, axis) <= 0
                    && winding * signedArea(start, end, other.p3, normal, axis) <= 0) {
                return true;
            }
        }
        return false;
    }

    /** Whether this triangle and another lie in the same plane. */
    public boolean isCoplanar(Triangle3d other) {
        GridNormal normal = unnormalizedNormal();
        return !normal.isZero() && other.isCoplanarWith(this, normal);
    }

    private static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    private static int max(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    /**
     * Whether all corners of this triangle lie in the plane of {@code plane}, whose (unnormalized) normal is given.
     * Allows a distance of one grid unit: a point where {@link #split} cut an edge was rounded onto the grid, which can
     * move it off a plane that is not axis aligned by up to that much.
     */
    private boolean isCoplanarWith(Triangle3d plane, GridNormal normal) {
        double tolerance = normal.length();
        return Math.abs(normal.dot(plane.p1, p1)) <= tolerance && Math.abs(normal.dot(plane.p1, p2)) <= tolerance
                && Math.abs(normal.dot(plane.p1, p3)) <= tolerance;
    }

    /**
     * Twice the signed area of the triangle {@code a, b, point}, positive when the point is left of {@code a -> b}
     * looking against the normal. Worked out after projecting along the dominant axis of the normal, which keeps the
     * sign while needing a single product of coordinates instead of two, so it stays exact in a long.
     */
    private static long signedArea(GridVector a, GridVector b, GridVector point, GridNormal normal, int axis) {
        long ab1, ab2, ap1, ap2, sign;
        switch (axis) {
            case 0 -> {
                ab1 = (long) b.y - a.y;
                ab2 = (long) b.z - a.z;
                ap1 = (long) point.y - a.y;
                ap2 = (long) point.z - a.z;
                sign = Long.signum(normal.x);
            }
            case 1 -> {
                ab1 = (long) b.z - a.z;
                ab2 = (long) b.x - a.x;
                ap1 = (long) point.z - a.z;
                ap2 = (long) point.x - a.x;
                sign = Long.signum(normal.y);
            }
            default -> {
                ab1 = (long) b.x - a.x;
                ab2 = (long) b.y - a.y;
                ap1 = (long) point.x - a.x;
                ap2 = (long) point.y - a.y;
                sign = Long.signum(normal.z);
            }
        }
        return sign * (ab1 * ap2 - ab2 * ap1);
    }

    /**
     * Where the edge {@code from -> to} crosses a line, given their signed areas from {@link #signedArea}, rounded to
     * the grid. Always interpolated from the same end of the edge, so the polygons on both sides of the line get the
     * same point.
     */
    private static GridVector crossing(GridVector from, long fromSide, GridVector to, long toSide) {
        if (!from.isBefore(to)) {
            GridVector temp = from;
            from = to;
            to = temp;
            long tempSide = fromSide;
            fromSide = toSide;
            toSide = tempSide;
        }
        long denominator = fromSide - toSide;
        return new GridVector(
                (int) (from.x + Grid3d.divRound(((long) to.x - from.x) * fromSide, denominator)),
                (int) (from.y + Grid3d.divRound(((long) to.y - from.y) * fromSide, denominator)),
                (int) (from.z + Grid3d.divRound(((long) to.z - from.z) * fromSide, denominator)));
    }

    /**
     * Splits the polygon along the line {@code edgeStart -> edgeEnd}. Corners on the line end up in both halves, so
     * that neither half leaves a gap.
     *
     * @param outside collects the part right of the line
     * @return the part left of the line
     */
    private static List<GridVector> clip(List<GridVector> polygon, GridVector edgeStart, GridVector edgeEnd,
            GridNormal normal, int axis, List<GridVector> outside) {
        List<GridVector> inside = new ArrayList<>();
        GridVector previous = polygon.get(polygon.size() - 1);
        long previousSide = signedArea(edgeStart, edgeEnd, previous, normal, axis);

        for (GridVector current : polygon) {
            long currentSide = signedArea(edgeStart, edgeEnd, current, normal, axis);

            // only an edge running from one side strictly to the other crosses the line between its ends
            if (previousSide > 0 && currentSide < 0 || previousSide < 0 && currentSide > 0) {
                GridVector crossing = crossing(previous, previousSide, current, currentSide);
                inside.add(crossing);
                outside.add(new GridVector(crossing));
            }
            if (currentSide >= 0) {
                inside.add(new GridVector(current));
            }
            if (currentSide <= 0) {
                outside.add(new GridVector(current));
            }

            previous = current;
            previousSide = currentSide;
        }
        return inside;
    }

    private static void addTriangulated(List<Triangle3d> triangles, List<GridVector> polygon, GridNormal normal,
            Triangle3d parent) {
        // the polygon is always convex, since it originates from a triangle cut by straight lines
        for (int i = 1; i < polygon.size() - 1; i++) {
            Triangle3d triangle = new Triangle3d(
                    new GridVector(polygon.get(0)),
                    new GridVector(polygon.get(i)),
                    new GridVector(polygon.get(i + 1)));
            if (!triangle.isSliver()) {
                triangle.ensureWindingOrder(normal);
                triangle.inheritPlane(parent);
                triangles.add(triangle);
            }
        }
    }

    /**
     * Whether the triangle is thinner than one grid unit, which covers degenerate ones as well. Rounding where
     * {@link #split} cuts an edge moves the point by up to half a unit, so the pieces of a triangle cut along the
     * shared edge of two occluders can keep a sliver that neither occluder covers. Nothing that thin can ever be seen.
     */
    boolean isSliver() {
        long longestEdgeSquared = Math
                .max(p1.distanceSquared(p2), Math.max(p2.distanceSquared(p3), p3.distanceSquared(p1)));
        // twice the area over the longest edge is the height on it
        return unnormalizedNormal().length() <= Math.sqrt(longestEdgeSquared);
    }

    public Triangle3d copy() {
        Triangle3d copy = new Triangle3d(new GridVector(p1), new GridVector(p2), new GridVector(p3));
        copy.plane = plane;
        return copy;
    }
}
