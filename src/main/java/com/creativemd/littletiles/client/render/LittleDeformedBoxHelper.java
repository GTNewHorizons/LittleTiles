package com.creativemd.littletiles.client.render;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

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

    /** The 8 absolute grid cells containing the corner handles, or null while no box is being edited. */
    private static LittleTileBlockPos[] corners = null;
    /** Size of the grid cells represented by {@link #corners}. */
    private static int grid = 0;
    /** Index into {@link #corners} of the corner the player selected, or -1 if none is selected. */
    private static int markedCorner = -1;

    private LittleDeformedBoxHelper() {}

    public static boolean isEditing() {
        return corners != null;
    }

    public static void reset() {
        corners = null;
        grid = 0;
        markedCorner = -1;
    }

    public static boolean hasMarkedCorner() {
        return markedCorner >= 0;
    }

    /**
     * Selects the given corner, or deselects if it is already the selected one - so clicking a corner twice, or
     * clicking nothing at all (-1), leaves nothing selected and the next right click places the box.
     */
    public static void toggleMarkedCorner(int corner) {
        markedCorner = corner == markedCorner ? -1 : corner;
    }

    /** Moves the selected corner by a number of grid steps. */
    public static void nudgeMarked(ForgeDirection direction, int amount) {
        corners[markedCorner].moveInDirection(direction, amount);
    }

    /** Warps the selected corner to a position, which is how a right click moves it to where the player looks. */
    public static void moveMarkedTo(LittleTileBlockPos pos) {
        corners[markedCorner] = pos.copy();
    }

    /**
     * Whether no logical minimum corner has crossed its matching maximum corner. Collapsed edges are valid. Invalid
     * boxes may exist temporarily while editing so they can be shown in red, but they cannot be placed.
     */
    public static boolean hasValidGeometry() {
        for (int i = 0; i < corners.length; i++) {
            LittleTileBlockPos corner = geometryCorner(i);
            if ((i & 1) == 0 && geometryCorner(i | 1).subtract(corner).x < 0) return false;
            if ((i & 2) == 0 && geometryCorner(i | 2).subtract(corner).y < 0) return false;
            if ((i & 4) == 0 && geometryCorner(i | 4).subtract(corner).z < 0) return false;
        }
        return true;
    }

    /**
     * Materializes the 8 corners of the axis-aligned box the player just closed with two clicks. The two clicks name
     * two grid cells and the box covers both of them. The stored positions identify those cells; positive-side cells
     * are expanded by one grid step only when the actual mesh vertices are derived.
     */
    public static void beginBox(LittleTileBlockPos first, LittleTileBlockPos second, int align) {
        LittleTileBlockPos.Subtraction delta = second.subtract(first);
        int lowX = Math.min(0, delta.x), highX = Math.max(0, delta.x);
        int lowY = Math.min(0, delta.y), highY = Math.max(0, delta.y);
        int lowZ = Math.min(0, delta.z), highZ = Math.max(0, delta.z);

        LittleTileBlockPos[] box = new LittleTileBlockPos[Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT];
        for (int i = 0; i < box.length; i++) {
            LittleTileBlockPos corner = first.copy();
            corner.moveSubX((i & 1) != 0 ? highX : lowX);
            corner.moveSubY((i & 2) != 0 ? highY : lowY);
            corner.moveSubZ((i & 4) != 0 ? highZ : lowZ);
            box[i] = corner;
        }
        corners = box;
        grid = align;
    }

    /** The actual mesh vertex represented by a corner handle cell. */
    private static LittleTileBlockPos geometryCorner(int index) {
        LittleTileBlockPos corner = corners[index].copy();
        if ((index & 1) != 0) corner.moveSubX(grid);
        if ((index & 2) != 0) corner.moveSubY(grid);
        if ((index & 4) != 0) corner.moveSubZ(grid);
        return corner;
    }

    /**
     * The corners as plain grid offsets from the first corner. A {@link LittleTileBlockPos} carries a block position as
     * well as a sub position, so two corners in different blocks cannot be compared componentwise - flattening them
     * into one frame is what makes a bounding box over them possible. Anchoring that frame at the first corner, whose
     * world position is known, is also what lets {@link #placementAnchor()} convert back out of it.
     */
    private static Vector3i[] offsetsFromFirst() {
        Vector3i[] offsets = new Vector3i[corners.length];
        LittleTileBlockPos first = geometryCorner(0);
        for (int i = 0; i < corners.length; i++) {
            LittleTileBlockPos.Subtraction sub = geometryCorner(i).subtract(first);
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

    public static boolean isMarkedCorner(int index) {
        return index == markedCorner;
    }

    /** The world position of a corner, for drawing it. */
    public static Vec3 cornerHitVec(int index) {
        return geometryCorner(index).toHitVec();
    }

    /**
     * The pickable cube of a corner, in world coordinates: exactly the one grid cell the corner sits in
     */
    public static AxisAlignedBB getCornerBoxAABB(int index, int grid) {
        double size = grid / 16.0;
        Vec3 vec = corners[index].toHitVec();
        return AxisAlignedBB.getBoundingBox(
                vec.xCoord,
                vec.yCoord,
                vec.zCoord,
                vec.xCoord + size,
                vec.yCoord + size,
                vec.zCoord + size);
    }

    /** Raytraces the corner cubes and returns the index of the nearest one hit, or -1 if the ray misses all of them. */
    public static int pickCorner(Vec3 start, Vec3 end, int grid) {
        if (!isEditing()) {
            return -1;
        }
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < corners.length; i++) {
            MovingObjectPosition hit = getCornerBoxAABB(i, grid).calculateIntercept(start, end);
            if (hit == null) {
                continue;
            }
            double distance = start.squareDistanceTo(hit.hitVec);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    /** The cutout describing the box as it currently stands. */
    public static LittleTileCutoutInfo currentCutout() {
        Vector3i[] offsets = offsetsFromFirst();
        return LittleTileCutoutInfo.fromDeformedCorners(LittleTileBox.fromPoints(offsets), offsets);
    }
}
