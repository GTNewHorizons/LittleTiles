package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraftforge.common.util.ForgeDirection;

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

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LittleTileFacade)) return false;
        LittleTileFacade other = (LittleTileFacade) object;
        return matches(other.block, other.meta) && box.equals(other.box);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * System.identityHashCode(block) + meta) + box.hashCode();
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
    public static LittleTileBox reach(LittleTileFacades sourceTiles, LittleTileBox sourceBox, Block block, int meta,
            int dx, int dy, int dz) {
        LittleTileBox reach = stretch(sourceBox, dx, dy, dz);
        return sourceTiles.fills(new LittleTileFacade(block, meta, reach)) ? reach : null;
    }

    /**
     * Whether tiles of the block and meta fill the whole box. The tiles of a block never overlap, so the volumes they
     * share with the box add up to the box's volume exactly when they fill it.
     */
    static boolean fills(LittleTileFacade[] tiles, Block block, int meta, LittleTileBox box) {
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
    public static boolean connectsToTiles(LittleTileBox reach, LittleTileFacades targetTiles, Block block, int meta,
            int dx, int dy, int dz) {
        for (LittleTileFacade tile : targetTiles.tiles) {
            if (tile.matches(block, meta) && continues(reach, tile.box, dx, dy, dz)) return true;
        }
        return false;
    }

    /**
     * Whether tiles of the block and meta in the target cover the face that the target lies in front of: the face of
     * the neighbour beside the asking area, as far as that area reaches it. Along the axes the asker steps sideways the
     * whole face counts, along the others only the reached part of it.
     *
     * @param normal the side being rendered; the offset steps along it once, in front of the neighbour
     */
    public static boolean coversFace(LittleTileBox reach, LittleTileFacades targetTiles, Block block, int meta, int dx,
            int dy, int dz, ForgeDirection normal) {
        LittleTileBox face = new LittleTileBox(
                faceMin(reach.minX, dx, normal.offsetX),
                faceMin(reach.minY, dy, normal.offsetY),
                faceMin(reach.minZ, dz, normal.offsetZ),
                faceMax(reach.maxX, dx, normal.offsetX),
                faceMax(reach.maxY, dy, normal.offsetY),
                faceMax(reach.maxZ, dz, normal.offsetZ));
        return targetTiles.fills(new LittleTileFacade(block, meta, face));
    }

    /** The covered face is one grid unit deep at the target's boundary facing back, against the normal. */
    private static int faceMin(int reachMin, int offset, int normal) {
        if (normal > 0) return LittleTile.minPos;
        if (normal < 0) return LittleTile.maxPos - 1;
        return offset != 0 ? LittleTile.minPos : reachMin;
    }

    private static int faceMax(int reachMax, int offset, int normal) {
        if (normal > 0) return LittleTile.minPos + 1;
        if (normal < 0) return LittleTile.maxPos;
        return offset != 0 ? LittleTile.maxPos : reachMax;
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
