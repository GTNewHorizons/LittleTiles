package com.creativemd.littletiles.client.util3d;

/**
 * The integer grid all mesh geometry lives on, in {@link GridVector}s. Keeping every vertex on it makes the geometry
 * exact: neighbouring tiles that cut the same shape at the same plane end up with identical vertices, so their faces
 * can cull each other.
 * <p>
 * A grid unit is 1/{@link #BLOCK} of a block. Unit meshes (the shape OBJs, the box) span [0, {@link #PIXEL}] on each
 * axis instead of [0, 1], so scaling one to a size given in tile pixels is a plain integer multiply that lands exactly
 * on the grid.
 */
public final class Grid3d {

    /** Grid units per tile pixel, and the extent of a unit mesh. */
    public static final int PIXEL = 4096;
    /** Grid units per block. */
    public static final int BLOCK = 16 * PIXEL;

    private Grid3d() {}

    /** Converts a length in tile pixels into grid units. */
    public static int fromPixels(int pixels) {
        return pixels * PIXEL;
    }

    /** Rounds a length in blocks to the nearest grid unit. */
    public static int fromBlocks(double blocks) {
        return (int) Math.round(blocks * BLOCK);
    }

    public static double toBlocks(long grid) {
        return grid / (double) BLOCK;
    }

    /**
     * {@code numerator / denominator} rounded to the nearest integer, halves rounding up. Exact for any sign, unlike
     * {@code /}, which truncates towards zero and would round differently depending on direction.
     */
    public static long divRound(long numerator, long denominator) {
        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }
        return Math.floorDiv(2 * numerator + denominator, 2 * denominator);
    }
}
