package com.creativemd.littletiles.client.render;

import org.joml.Vector3i;

import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Handler for the DEFORMED_BOX shape
 *
 * The 8 corners are held as absolute grid positions while the player is editing - client side only, like the click
 * state in {@link PreviewRenderer}, since nothing but the finished tile ever reaches the server. Only once the tile is
 * placed do they get baked into a {@link LittleTileCutoutInfo} relative to the tile's own bounding box.
 */
@SideOnly(Side.CLIENT)
public final class LittleDeformedBoxHelper {

    /** The 8 absolute corner positions of the box being edited, or null while no box is being edited. */
    private static LittleTileBlockPos[] corners = null;

    private LittleDeformedBoxHelper() {}

    public static boolean isEditing() {
        return corners != null;
    }

    public static void reset() {
        corners = null;
    }

    /**
     * Materializes the 8 corners of the axis-aligned box the player just closed with two clicks. The two clicks name
     * two grid cells and the box covers both of them, so it spans from the lower cell's min corner to one grid step
     * past the upper cell - the corners are box <em>points</em>, not cells.
     */
    public static void beginBox(LittleTileBlockPos first, LittleTileBlockPos second, int align) {
        LittleTileBlockPos.Subtraction delta = second.subtract(first);
        int lowX = Math.min(0, delta.x), highX = Math.max(0, delta.x) + align;
        int lowY = Math.min(0, delta.y), highY = Math.max(0, delta.y) + align;
        int lowZ = Math.min(0, delta.z), highZ = Math.max(0, delta.z) + align;

        LittleTileBlockPos[] box = new LittleTileBlockPos[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
        for (int i = 0; i < box.length; i++) {
            LittleTileBlockPos corner = first.copy();
            corner.moveSubX((i & 1) != 0 ? highX : lowX);
            corner.moveSubY((i & 2) != 0 ? highY : lowY);
            corner.moveSubZ((i & 4) != 0 ? highZ : lowZ);
            box[i] = corner;
        }
        corners = box;
    }

    /**
     * The corners as plain grid offsets from the first corner. A {@link LittleTileBlockPos} carries a block position as
     * well as a sub position, so two corners in different blocks cannot be compared componentwise - flattening them
     * into one frame is what makes a bounding box over them possible. Anchoring that frame at the first corner, whose
     * world position is known, is also what lets {@link #placementAnchor()} convert back out of it.
     */
    private static Vector3i[] offsetsFromFirst() {
        Vector3i[] offsets = new Vector3i[corners.length];
        for (int i = 0; i < corners.length; i++) {
            LittleTileBlockPos.Subtraction sub = corners[i].subtract(corners[0]);
            offsets[i] = new Vector3i(sub.x, sub.y, sub.z);
        }
        return offsets;
    }

    /** The tile's bounding box. Only its size is used - which frame it is measured in does not matter for that. */
    public static LittleTileBox currentBox() {
        return LittleTileBox.fromPoints(offsetsFromFirst());
    }

    /**
     * The tile has to be rooted at the corner bounding box's own min corner, which can sit before the first corner
     * (e.g. after dragging a corner further west than the box started). Placing at the raw first corner instead would
     * mirror everything on its negative side onto the positive side.
     */
    public static LittleTileBlockPos placementAnchor() {
        LittleTileBox bounds = LittleTileBox.fromPoints(offsetsFromFirst());
        LittleTileBlockPos anchor = corners[0].copy();
        anchor.moveSubX(bounds.minX);
        anchor.moveSubY(bounds.minY);
        anchor.moveSubZ(bounds.minZ);
        return anchor;
    }

    /** The cutout describing the box as it currently stands. */
    public static LittleTileCutoutInfo currentCutout() {
        Vector3i[] offsets = offsetsFromFirst();
        return LittleTileCutoutInfo.fromDeformedCorners(LittleTileBox.fromPoints(offsets), offsets);
    }
}
