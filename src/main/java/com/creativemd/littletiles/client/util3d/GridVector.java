package com.creativemd.littletiles.client.util3d;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.creativemd.creativecore.lib.Vector3d;

/**
 * A point or offset in grid units, see {@link Grid3d}. A type of its own, so it cannot be mixed up with the plain
 * {@link Vector3i}s used for tile pixels.
 * <p>
 * Every grid vector must be one of these, never a plain {@link Vector3i}: {@link Vector3i#equals(Object)} compares
 * classes, so a plain one never equals a grid vector at the same position.
 */
public class GridVector extends Vector3i {

    public GridVector() {}

    public GridVector(int x, int y, int z) {
        super(x, y, z);
    }

    public GridVector(GridVector v) {
        super(v);
    }

    /** Converts a vector in tile pixels into grid units. */
    public static GridVector fromPixels(Vector3ic pixels) {
        return new GridVector(
                Grid3d.fromPixels(pixels.x()),
                Grid3d.fromPixels(pixels.y()),
                Grid3d.fromPixels(pixels.z()));
    }

    /** {@code (b - this) x (c - this)}, widened to longs before multiplying so it cannot overflow. */
    public GridNormal cross(GridVector b, GridVector c) {
        long abx = (long) b.x - x, aby = (long) b.y - y, abz = (long) b.z - z;
        long acx = (long) c.x - x, acy = (long) c.y - y, acz = (long) c.z - z;
        return new GridNormal(aby * acz - abz * acy, abz * acx - abx * acz, abx * acy - aby * acx);
    }

    /**
     * Whether this comes first of the two, in an arbitrary but fixed order. Every edge gets interpolated from the end
     * that comes first, whichever triangle or tile it is reached from. Without that the rounding of a cut could depend
     * on which way around an edge was walked.
     */
    public boolean isBefore(GridVector other) {
        if (x != other.x) {
            return x < other.x;
        }
        if (y != other.y) {
            return y < other.y;
        }
        return z <= other.z;
    }

    public Vector3d toVector3d() {
        return new Vector3d(Grid3d.toBlocks(x), Grid3d.toBlocks(y), Grid3d.toBlocks(z));
    }

    public Vector3f toVector3f() {
        return new Vector3f((float) Grid3d.toBlocks(x), (float) Grid3d.toBlocks(y), (float) Grid3d.toBlocks(z));
    }
}
