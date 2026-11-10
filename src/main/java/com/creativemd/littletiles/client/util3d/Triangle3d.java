package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Vector2d;

import com.creativemd.creativecore.lib.Vector3d;

public class Triangle3d {

    /**
     * How far, in blocks, a point may lie off a line and still count as on it when splitting. Covers rounding the
     * points of a mesh to {@link MeshCutter#RENDER_GRID}, which moves them by up to sqrt(3) / 2 of it.
     */
    private static final double SPLIT_EPSILON = 1.0E-5;

    private Vector3d p1, p2, p3;
    private Vector2d tex1, tex2, tex3;
    /** See {@link #getFacePlane}, null until first needed. */
    private Plane3d plane;
    /**
     * Shared by the triangles a convex polygon of the mesh was split into, so that {@link MeshCutter} can put it back
     * together. Null for a triangle that is a face on its own.
     */
    private Object polygon;

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
        plane = null;
    }

    /**
     * The plane of the face this triangle is part of. Worked out from its own points on first use. A piece cut out of
     * another triangle takes that triangle's plane instead, since rounding tilts small pieces: whether triangles lie in
     * one plane, and which block plane textures them, is decided by the faces they come from.
     * <p>
     * Kept once known, so the points must not be moved afterwards other than by translating.
     */
    public Plane3d getFacePlane() {
        if (plane == null) {
            plane = planeThrough(p1, p2, p3);
        }
        return plane;
    }

    /** The block plane this triangle is textured from, the one its face is closest to. */
    public Plane3d getPlane() {
        return Plane3d.getPlaneForNormal(getFacePlane().getNormal());
    }

    /** Makes this a piece of {@code parent}, see {@link #getFacePlane}. */
    public void inheritPlane(Triangle3d parent) {
        plane = parent.getFacePlane();
    }

    public void setPlane(Plane3d plane) {
        this.plane = plane;
    }

    /** See {@link #polygon}. */
    public Object getPolygon() {
        return polygon;
    }

    public void setPolygon(Object polygon) {
        this.polygon = polygon;
    }

    /** The plane through the points, facing the way they turn counterclockwise around. */
    public static Plane3d planeThrough(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d ab = new Vector3d(b);
        ab.sub(a);
        Vector3d ac = new Vector3d(c);
        ac.sub(a);
        return new Plane3d(null, ab, ac, null, null, false, false, new Vector3d(a));
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
        if (plane != null) {
            plane = plane.moveAlongNormal(plane.getNormal().dot(new Vector3d(x, y, z)));
        }
    }

    public void inflate(double epsilon) {
        Vector3d n = getNormal();
        n.x *= epsilon;
        n.y *= epsilon;
        n.z *= epsilon;
        translate(n);
    }

    public Vector3d getNormal() {
        Vector3d normal = unnormalizedNormal();
        normal.normalize();
        return normal;
    }

    public double getArea() {
        return unnormalizedNormal().length() / 2;
    }

    public void flipWindingOrder() {
        Vector3d temp = p2;
        p2 = p3;
        p3 = temp;
        plane = null;
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

    /** In doubles, which is exact, as the orientations only swap and flip axes. */
    private static Vector3d rotateVector(Vector3d v, Matrix3f matrix) {
        return new Vector3d(
                matrix.m00() * v.x + matrix.m10() * v.y + matrix.m20() * v.z,
                matrix.m01() * v.x + matrix.m11() * v.y + matrix.m21() * v.z,
                matrix.m02() * v.x + matrix.m12() * v.y + matrix.m22() * v.z);
    }

    public void rotate(int orientation) {
        Matrix3f matrix = OrientationMapper.fromId(orientation);
        p1 = rotateVector(p1, matrix);
        p2 = rotateVector(p2, matrix);
        p3 = rotateVector(p3, matrix);
        // Mirrored orientations (used for flipped meshes) invert the winding order, so it has to be restored here to
        // keep the normal pointing outwards.
        if (matrix.determinant() < 0) {
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
        if (!isCoplanar(other)) {
            result.add(copy());
            return result;
        }
        // Split in the plane of the other's face, which this lies in
        Vector3d normal = other.getFacePlane().getNormal();

        // ordered counter-clockwise around the normal, so that a point is inside the cutter when it is left of all
        // three of its edges
        List<Vector3d> cutter = other.corners();
        double cutterWinding = signedArea(cutter.get(0), cutter.get(1), cutter.get(2), normal);
        if (cutterWinding == 0) {
            result.add(copy());
            return result;
        }
        if (cutterWinding < 0) {
            Collections.reverse(cutter);
        }
        // Touching the cutter is no reason to cut: re-triangulating would drop thin pieces of long slivers
        if (liesOutside(cutter, normal)) {
            result.add(copy());
            return result;
        }

        List<Vector3d> inside = corners();
        for (int i = 0; i < 3 && !inside.isEmpty(); i++) {
            List<Vector3d> outside = new ArrayList<>();
            inside = clip(inside, cutter.get(i), cutter.get((i + 1) % 3), normal, outside);
            addTriangulated(result, outside, this);
        }

        return result;
    }

    /**
     * Whether the triangle lies right of one of the cutter's edges, its corners on the edge within
     * {@link #SPLIT_EPSILON} at most, so that the two share no area.
     */
    private boolean liesOutside(List<Vector3d> cutter, Vector3d normal) {
        for (int i = 0; i < 3; i++) {
            Vector3d edgeStart = cutter.get(i), edgeEnd = cutter.get((i + 1) % 3);
            if (distanceLeftOf(edgeStart, edgeEnd, p1, normal) <= SPLIT_EPSILON
                    && distanceLeftOf(edgeStart, edgeEnd, p2, normal) <= SPLIT_EPSILON
                    && distanceLeftOf(edgeStart, edgeEnd, p3, normal) <= SPLIT_EPSILON) {
                return true;
            }
        }
        return false;
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

    /**
     * Whether this triangle lies in the plane of the other's face, facing either way, see {@link #getFacePlane}.
     * Decided by its corners, so that a sliver of another face rounded flat onto the plane lies in it too.
     */
    public boolean isCoplanar(Triangle3d other) {
        Plane3d plane = other.getFacePlane();
        return Math.abs(plane.getDistance(p1)) <= SPLIT_EPSILON && Math.abs(plane.getDistance(p2)) <= SPLIT_EPSILON
                && Math.abs(plane.getDistance(p3)) <= SPLIT_EPSILON;
    }

    private static double min(double a, double b, double c) {
        return Math.min(a, Math.min(b, c));
    }

    private static double max(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    /**
     * How far the point lies left of the line {@code a -> b}, looking against the normal, negative when right of it.
     */
    private static double distanceLeftOf(Vector3d a, Vector3d b, Vector3d point, Vector3d normal) {
        return signedArea(a, b, point, normal) / Math.sqrt(Mesh3d.distanceSquared(a, b));
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
     * Splits the polygon along the line {@code edgeStart -> edgeEnd}. Corners within {@link #SPLIT_EPSILON} of the line
     * count as on it and end up in both halves, so that neither half leaves a gap. A half without a corner off the line
     * on its own side is left empty, rather than becoming a sliver along the line.
     *
     * @param outside collects the part right of the line
     * @return the part left of the line
     */
    private static List<Vector3d> clip(List<Vector3d> polygon, Vector3d edgeStart, Vector3d edgeEnd, Vector3d normal,
            List<Vector3d> outside) {
        double[] sides = new double[polygon.size()];
        boolean anyLeft = false, anyRight = false;
        for (int i = 0; i < sides.length; i++) {
            double side = distanceLeftOf(edgeStart, edgeEnd, polygon.get(i), normal);
            sides[i] = Math.abs(side) <= SPLIT_EPSILON ? 0 : side;
            anyLeft |= sides[i] > 0;
            anyRight |= sides[i] < 0;
        }
        List<Vector3d> inside = new ArrayList<>();
        for (int i = 0; i < sides.length; i++) {
            int previous = (i + sides.length - 1) % sides.length;
            if (sides[previous] * sides[i] < 0) {
                Vector3d crossing = new Vector3d(polygon.get(previous));
                crossing.interpolate(polygon.get(i), sides[previous] / (sides[previous] - sides[i]));
                if (anyLeft) inside.add(crossing);
                if (anyRight) outside.add(new Vector3d(crossing));
            }
            if (sides[i] >= 0 && anyLeft) inside.add(new Vector3d(polygon.get(i)));
            if (sides[i] <= 0 && anyRight) outside.add(new Vector3d(polygon.get(i)));
        }
        return inside;
    }

    private static void addTriangulated(List<Triangle3d> triangles, List<Vector3d> polygon, Triangle3d parent) {
        // the polygon is always convex, since it originates from a triangle cut by straight lines
        for (int i = 1; i < polygon.size() - 1; i++) {
            Triangle3d triangle = new Triangle3d(
                    new Vector3d(polygon.get(0)),
                    new Vector3d(polygon.get(i)),
                    new Vector3d(polygon.get(i + 1)));
            // Pieces thinner than the tolerance are only along a line, however long
            if (!triangle.isThinnerThan(SPLIT_EPSILON)) {
                triangle.ensureWindingOrder(parent.getFacePlane().getNormal());
                triangle.inheritPlane(parent);
                triangles.add(triangle);
            }
        }
    }

    /** Whether the triangle's height across its longest side is below {@code height}. */
    private boolean isThinnerThan(double height) {
        double longest = Math.max(
                Mesh3d.distanceSquared(p1, p2),
                Math.max(Mesh3d.distanceSquared(p2, p3), Mesh3d.distanceSquared(p3, p1)));
        // twice the area over the side is the height on it
        return unnormalizedNormal().lengthSquared() <= height * height * longest;
    }

    public Triangle3d copy() {
        Triangle3d copy = new Triangle3d(new Vector3d(p1), new Vector3d(p2), new Vector3d(p3));
        copy.plane = plane;
        copy.polygon = polygon;
        return copy;
    }
}
