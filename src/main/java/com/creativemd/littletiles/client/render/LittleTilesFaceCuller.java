package com.creativemd.littletiles.client.render;

import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisX;
import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisY;
import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisZ;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.client.rendering.IFaceClipper;
import com.creativemd.creativecore.common.utils.RotationUtils;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Computes which parts of a tile entity's cube faces are covered by other cubes of the same tile entity.
 * <p>
 * Covered faces are split by {@link FaceClipper}; fully covered faces produce no pieces and therefore emit no quad.
 */
@SideOnly(Side.CLIENT)
public final class LittleTilesFaceCuller {

    /**
     * The two axes spanning a side's plane, used as the local 2D axes for clipping rectangles on that face: DOWN/UP use
     * X/Z, NORTH/SOUTH use X/Y, and WEST/EAST use Z/Y. The axis a side is perpendicular to comes from
     * {@link Axis#getAxis(ForgeDirection)}.
     * <p>
     * Indexed by {@link ForgeDirection#ordinal()}: DOWN, UP, NORTH, SOUTH, WEST, EAST.
     * <p>
     * That in-plane orientation is a convention, not something Forge supplies. Swapping a face to Z/X or Y/Z would also
     * be valid if FaceClipper and ExtendedRenderBlocks used the same convention.
     */
    private static final Axis[] PLANE_X_AXIS = { AxisX, AxisX, AxisX, AxisX, AxisZ, AxisZ };
    private static final Axis[] PLANE_Y_AXIS = { AxisZ, AxisZ, AxisY, AxisY, AxisY, AxisY };

    private LittleTilesFaceCuller() {}

    /**
     * Computes the covered areas of every cube of one tile entity, indexed like {@code cubes}.
     * <p>
     * Must be given all cubes, not just the ones of the current render pass: a tile drawn in pass 0 still hides the
     * faces of a tile drawn in pass 1.
     */
    public static IFaceClipper[] computeCoverage(List<LittleTilesCubeObject> cubes) {
        IFaceClipper[] clippers = new IFaceClipper[cubes.size()];
        for (int i = 0; i < cubes.size(); i++) {
            LittleTilesCubeObject cube = cubes.get(i);
            if (ignoreForCulling(cube)) {
                continue;
            }
            FaceClipper clipper = new FaceClipper(cube);
            clippers[i] = clipper;
            for (int j = 0; j < cubes.size(); j++) {
                LittleTilesCubeObject occluder = cubes.get(j);
                if (j != i && canOcclude(occluder, cube)) {
                    cover(clipper, cube, occluder);
                }
            }
        }
        return clippers;
    }

    /**
     * Invalid blocks have nothing to cull and cutouts are meshes rather than boxes. Clipping opaque blocks costs more
     * CPU than the saved GPU work is worth.
     */
    private static boolean ignoreForCulling(LittleTilesCubeObject cube) {
        return cube.block == null || cube.meta == -1 || cube.cutoutInfo != null || cube.block.isOpaqueCube();
    }

    private static boolean canOcclude(LittleTilesCubeObject occluder, LittleTilesCubeObject cube) {
        if (ignoreForCulling(occluder)) {
            return false;
        }
        // A translucent tile only hides an identical neighbour. Glass against stained glass is
        // intentional layering and has to keep both faces.
        return occluder.block == cube.block && occluder.meta == cube.meta && occluder.color == cube.color;
    }

    /** Marks the areas the given occluder covers on the faces of {@code cube} it sits flush against. */
    private static void cover(FaceClipper clipper, LittleTilesCubeObject cube, LittleTilesCubeObject occluder) {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            Axis axis = Axis.getAxis(side);
            boolean flush = RotationUtils.isNegative(side) ? occluder.gridMax(axis) == cube.gridMin(axis)
                    : occluder.gridMin(axis) == cube.gridMax(axis);
            if (!flush) {
                continue;
            }

            Axis planeX = PLANE_X_AXIS[side.ordinal()];
            Axis planeY = PLANE_Y_AXIS[side.ordinal()];
            int minPlaneX = Math.max(cube.gridMin(planeX), occluder.gridMin(planeX));
            int maxPlaneX = Math.min(cube.gridMax(planeX), occluder.gridMax(planeX));
            int minPlaneY = Math.max(cube.gridMin(planeY), occluder.gridMin(planeY));
            int maxPlaneY = Math.min(cube.gridMax(planeY), occluder.gridMax(planeY));
            if (minPlaneX < maxPlaneX && minPlaneY < maxPlaneY) {
                clipper.cover(side, minPlaneX, minPlaneY, maxPlaneX, maxPlaneY);
            }
        }
    }
}
