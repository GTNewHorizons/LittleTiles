package com.creativemd.littletiles.client.util3d;

import org.joml.Vector3L;

import com.creativemd.creativecore.lib.Vector3d;

/**
 * An unnormalized normal of grid geometry, as {@link GridVector#cross} produces it. Its components are products of two
 * grid coordinates, which outgrow an int.
 */
public class GridNormal extends Vector3L {

    public GridNormal(long x, long y, long z) {
        // Vector3L has no constructor taking longs
        set(x, y, z);
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    /** {@code this . (point - origin)}. */
    public long dot(GridVector origin, GridVector point) {
        return x * ((long) point.x - origin.x) + y * ((long) point.y - origin.y) + z * ((long) point.z - origin.z);
    }

    /**
     * Worked out in doubles. The inherited one squares in longs, which overflows for the size of a cross product, and
     * so does {@link #lengthSquared()}.
     */
    @Override
    public double length() {
        double dx = x, dy = y, dz = z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** The direction as a unit vector. Not exact, so only fit for lighting and picking texture planes. */
    public Vector3d toUnitVector() {
        Vector3d ret = new Vector3d(x, y, z);
        ret.normalize();
        return ret;
    }
}
