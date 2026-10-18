package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;

import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * A block tile that connected textures can connect to, and the geometry rule for connections involving LittleTiles. A
 * source tile must have an unbroken volume of the same block and metadata between its bounds and the boundary towards
 * the target. A target tile must continue that volume from the opposite boundary; a whole target block already fills
 * its space.
 * <p>
 * Offsets point from the block that asks to the location it asks about, each component is -1, 0 or 1.
 */
public final class LittleTileFacade {

    /** The box of a real block, which always covers its whole block. */
    public static final LittleTileBox FULL_BLOCK = new LittleTileBox(
            LittleTile.minPos,
            LittleTile.minPos,
            LittleTile.minPos,
            LittleTile.maxPos,
            LittleTile.maxPos,
            LittleTile.maxPos);

    public final Block block;
    public final int meta;
    public final LittleTileBox box;

    public LittleTileFacade(Block block, int meta, LittleTileBox box) {
        this.block = block;
        this.meta = meta;
        this.box = box;
    }

    public boolean matches(Block block, int meta) {
        return this.block == block && this.meta == meta;
    }

    /**
     * @return the box stretched along every axis the offset points to, up to the boundary of its block
     */
    public static LittleTileBox stretch(LittleTileBox box, int dx, int dy, int dz) {
        return new LittleTileBox(
                dx < 0 ? LittleTile.minPos : box.minX,
                dy < 0 ? LittleTile.minPos : box.minY,
                dz < 0 ? LittleTile.minPos : box.minZ,
                dx > 0 ? LittleTile.maxPos : box.maxX,
                dy > 0 ? LittleTile.maxPos : box.maxY,
                dz > 0 ? LittleTile.maxPos : box.maxZ);
    }

    /** The source area extended to its boundary, or null if other materials leave a gap in it. */
    public static LittleTileBox reach(LittleTileFacade[] sourceTiles, LittleTileBox sourceBox, Block block, int meta,
            int dx, int dy, int dz) {
        LittleTileBox reach = stretch(sourceBox, dx, dy, dz);
        return fills(sourceTiles, block, meta, reach) ? reach : null;
    }

    /**
     * Whether tiles of the block and meta fill the whole box. The tiles of a block never overlap, so the volumes they
     * share with the box add up to the box's volume exactly when they fill it.
     */
    public static boolean fills(LittleTileFacade[] tiles, Block block, int meta, LittleTileBox box) {
        long filled = 0;
        for (LittleTileFacade tile : tiles) {
            if (tile.matches(block, meta)) filled += sharedVolume(tile.box, box);
        }
        return filled == sharedVolume(box, box);
    }

    private static long sharedVolume(LittleTileBox a, LittleTileBox b) {
        long x = Math.max(0, Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX));
        long y = Math.max(0, Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY));
        long z = Math.max(0, Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ));
        return x * y * z;
    }

    /** Whether the target has a tile of the same material touching the reached area from the opposite boundary. */
    public static boolean connectsToTiles(LittleTileBox reach, LittleTileFacade[] targetTiles, Block block, int meta,
            int dx, int dy, int dz) {
        for (LittleTileFacade tile : targetTiles) {
            if (tile.matches(block, meta) && continues(reach, tile.box, dx, dy, dz)) return true;
        }
        return false;
    }

    /**
     * Whether a box in the neighbouring block continues the asking box, which reaches the boundary towards it, so their
     * connected textures connect. Along every axis the offset points to, the box has to touch the facing boundary of
     * its block. Along the other axes they have to overlap, touching in a line or a point does not count.
     *
     * @param from the asking box, in its own block
     * @param box  the box to check, in the block at the offset
     */
    public static boolean continues(LittleTileBox from, LittleTileBox box, int dx, int dy, int dz) {
        return continues(from.minX, from.maxX, box.minX, box.maxX, dx)
                && continues(from.minY, from.maxY, box.minY, box.maxY, dy)
                && continues(from.minZ, from.maxZ, box.minZ, box.maxZ, dz);
    }

    private static boolean continues(int fromMin, int fromMax, int min, int max, int offset) {
        if (offset > 0) return min == LittleTile.minPos;
        if (offset < 0) return max == LittleTile.maxPos;
        return min < fromMax && fromMin < max;
    }
}
