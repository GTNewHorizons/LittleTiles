package com.creativemd.littletiles.client.util3d;

import org.joml.Vector3L;
import org.joml.Vector3f;

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
     * The axis the normal points along the most, 0 to 2 for x to z. A coplanar polygon can be projected along it
     * without folding, and stepping along it against the normal always leads to the inner side.
     */
    public int dominantAxis() {
        long ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        return ax >= ay && ax >= az ? 0 : ay >= az ? 1 : 2;
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
    public Vector3f toUnitVector() {
        double length = length();
        return new Vector3f((float) (x / length), (float) (y / length), (float) (z / length));
    }
}
