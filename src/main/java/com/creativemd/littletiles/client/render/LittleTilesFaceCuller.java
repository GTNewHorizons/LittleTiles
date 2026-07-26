package com.creativemd.littletiles.client.render;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Decides which faces of a tile entity's cubes are hidden by other cubes of the same tile entity.
 * <p>
 * Two touching translucent faces would otherwise blend against each other and darken the seam, and every hidden face
 * still costs a quad. The result is written to {@link LittleTilesCubeObject#hiddenSides} and consumed by
 * ExtendedRenderBlocks, which skips the matching renderFace* call.
 */
@SideOnly(Side.CLIENT)
public class LittleTilesFaceCuller {

    private LittleTilesFaceCuller() {}

    /**
     * Computes {@link LittleTilesCubeObject#hiddenSides} for every cube of one tile entity.
     * <p>
     * Must be given all cubes, not just the ones of the current render pass: an opaque tile drawn in pass 0 still hides
     * the faces of a translucent tile drawn in pass 1.
     */
    public static void computeHiddenSides(List<LittleTilesCubeObject> cubes,
            ArrayList<ArrayList<LittleTilesCubeObject>> neighbourCubes) {
        for (int i = 0; i < cubes.size(); i++) {
            final LittleTilesCubeObject cube = cubes.get(i);
            cube.hiddenSides = 0;
            if (ignoreForCulling(cube) || cube.block.isOpaqueCube()) {
                continue;
            }
            // Process the primary cube for internal hidden faces
            for (int j = 0; j < cubes.size(); j++) {
                if (j == i) {
                    continue;
                }
                final LittleTilesCubeObject occluder = cubes.get(j);
                if (canOcclude(occluder, cube)) {
                    cube.hiddenSides |= coveredSides(cube, occluder);
                }
            }
            // Check the neighboring cubes.
            for (int j = 0; j < 6; j++) {
                final List<LittleTilesCubeObject> neighbourCubeObjects = neighbourCubes.get(j);
                for (int k = 0; k < neighbourCubeObjects.size(); k++) {
                    final LittleTilesCubeObject occluder = neighbourCubeObjects.get(k);
                    if (canOcclude(occluder, cube)) {
                        cube.hiddenSides |= coveredNeighbourSides(cube, occluder, j);
                    }
                }
            }
        }
    }

    /** Invalid blocks have nothing to cull, cutouts are meshes rather than boxes. */
    private static boolean ignoreForCulling(LittleTilesCubeObject cube) {
        return cube.block == null || cube.meta == -1 || cube.cutoutInfo != null;
    }

    private static boolean canOcclude(LittleTilesCubeObject occluder, LittleTilesCubeObject cube) {
        if (ignoreForCulling(occluder)) {
            return false;
        }
        if (occluder.block.isOpaqueCube()) {
            return true;
        }
        // A translucent tile only hides an identical neighbour. Glass against stained glass is
        // intentional layering and has to keep both faces.
        return occluder.block == cube.block && occluder.meta == cube.meta && occluder.color == cube.color;
    }

    /**
     * Bit mask of the faces of {@code cube} that {@code occluder} sits flush against and fully covers on its own. Faces
     * that are only partially covered, or covered by several occluders together, are not detected.
     */
    private static int coveredSides(LittleTilesCubeObject cube, LittleTilesCubeObject occluder) {
        int hidden = 0;

        if (occluder.gridMinX <= cube.gridMinX && occluder.gridMaxX >= cube.gridMaxX
                && occluder.gridMinZ <= cube.gridMinZ
                && occluder.gridMaxZ >= cube.gridMaxZ) {
            if (occluder.gridMaxY == cube.gridMinY) hidden |= DOWN.flag;
            if (occluder.gridMinY == cube.gridMaxY) hidden |= UP.flag;
        }

        if (occluder.gridMinX <= cube.gridMinX && occluder.gridMaxX >= cube.gridMaxX
                && occluder.gridMinY <= cube.gridMinY
                && occluder.gridMaxY >= cube.gridMaxY) {
            if (occluder.gridMaxZ == cube.gridMinZ) hidden |= NORTH.flag;
            if (occluder.gridMinZ == cube.gridMaxZ) hidden |= SOUTH.flag;
        }

        if (occluder.gridMinY <= cube.gridMinY && occluder.gridMaxY >= cube.gridMaxY
                && occluder.gridMinZ <= cube.gridMinZ
                && occluder.gridMaxZ >= cube.gridMaxZ) {
            if (occluder.gridMaxX == cube.gridMinX) hidden |= WEST.flag;
            if (occluder.gridMinX == cube.gridMaxX) hidden |= EAST.flag;
        }

        return hidden;
    }

    /**
     * Bit mask of the faces of {@code cube}'s neighbouring blocks that {@code occluder} sits flush against and fully
     * covers on its own. Faces that are only partially covered, or covered by several occluders together, are not
     * detected. Essentially the same as {@code coveredSides}, but only checking one face of the cube.
     */
    private static int coveredNeighbourSides(LittleTilesCubeObject cube, LittleTilesCubeObject occluder, int side) {
        int hidden = 0;

        ForgeDirection direction = ForgeDirection.getOrientation(side);
        switch (direction) {
            case DOWN: {
                if (occluder.gridMinX <= cube.gridMinX && occluder.gridMaxX >= cube.gridMaxX
                        && occluder.gridMinZ <= cube.gridMinZ
                        && occluder.gridMaxZ >= cube.gridMaxZ
                        && occluder.gridMaxY == 16
                        && cube.gridMinY == 0)
                    hidden |= DOWN.flag;
                break;
            }
            case UP: {
                if (occluder.gridMinX <= cube.gridMinX && occluder.gridMaxX >= cube.gridMaxX
                        && occluder.gridMinZ <= cube.gridMinZ
                        && occluder.gridMaxZ >= cube.gridMaxZ
                        && occluder.gridMinY == 0
                        && cube.gridMaxY == 16)
                    hidden |= UP.flag;
                break;
            }
            case NORTH: {
                if (occluder.gridMinX <= cube.gridMinX && occluder.gridMaxX >= cube.gridMaxX
                        && occluder.gridMinY <= cube.gridMinY
                        && occluder.gridMaxY >= cube.gridMaxY
                        && occluder.gridMaxZ == 16
                        && cube.gridMinZ == 0)
                    hidden |= NORTH.flag;
                break;
            }
            case SOUTH: {
                if (occluder.gridMinX <= cube.gridMinX && occluder.gridMaxX >= cube.gridMaxX
                        && occluder.gridMinY <= cube.gridMinY
                        && occluder.gridMaxY >= cube.gridMaxY
                        && occluder.gridMinZ == 0
                        && cube.gridMaxZ == 16)
                    hidden |= SOUTH.flag;
                break;
            }
            case WEST: {
                if (occluder.gridMinY <= cube.gridMinY && occluder.gridMaxY >= cube.gridMaxY
                        && occluder.gridMinZ <= cube.gridMinZ
                        && occluder.gridMaxZ >= cube.gridMaxZ
                        && occluder.gridMaxX == 16
                        && cube.gridMinX == 0)
                    hidden |= WEST.flag;
                break;
            }
            case EAST: {
                if (occluder.gridMinY <= cube.gridMinY && occluder.gridMaxY >= cube.gridMaxY
                        && occluder.gridMinZ <= cube.gridMinZ
                        && occluder.gridMaxZ >= cube.gridMaxZ
                        && occluder.gridMinX == 0
                        && cube.gridMaxX == 16)
                    hidden |= EAST.flag;
                break;
            }
            default:
                break;
        }

        return hidden;
    }
}
