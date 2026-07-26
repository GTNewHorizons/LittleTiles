package com.creativemd.littletiles.client.render;

import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisX;
import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisY;
import static com.creativemd.creativecore.common.utils.RotationUtils.Axis.AxisZ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.client.rendering.IFaceClipper;
import com.creativemd.creativecore.common.utils.RotationUtils;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Computes which parts of a tile entity's cube faces are covered by other cubes in the same or adjacent tile entities.
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
     * Computes the covered areas of every cube of the tile entity at the given position. Cubes that are never clipped
     * get a null entry, which renders them untouched.
     * <p>
     * Cubes reaching a block boundary are additionally clipped against the adjacent {@link TileEntityLittleTiles}, if
     * there is one.
     * <p>
     * Must be given all cubes, not just the ones of the current render pass: a tile drawn in pass 0 still hides the
     * faces of a tile drawn in pass 1.
     */
    public static IFaceClipper[] computeCoverage(IBlockAccess world, List<LittleTilesCubeObject> cubes, int x, int y,
            int z) {
        FaceClipper[] clippers = new FaceClipper[cubes.size()];
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
        coverNeighbours(world, cubes, clippers, x, y, z);
        return clippers;
    }

    /** Whether the cube is flush against the edge of its own block on the given side. */
    private static boolean touchesBorder(LittleTilesCubeObject cube, ForgeDirection side) {
        Axis axis = Axis.getAxis(side);
        return RotationUtils.isNegative(side) ? cube.gridMin(axis) == 0 : cube.gridMax(axis) == 16;
    }

    private static void coverNeighbours(IBlockAccess world, List<LittleTilesCubeObject> cubes, FaceClipper[] clippers,
            int x, int y, int z) {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            List<LittleTilesCubeObject> neighbourCubes = null;
            for (int i = 0; i < cubes.size(); i++) {
                LittleTilesCubeObject cube = cubes.get(i);
                if (clippers[i] == null || !touchesBorder(cube, side)) {
                    continue;
                }
                if (neighbourCubes == null) {
                    neighbourCubes = getNeighbourBorderCubes(
                            world,
                            x + side.offsetX,
                            y + side.offsetY,
                            z + side.offsetZ,
                            side.getOpposite());
                    if (neighbourCubes.isEmpty()) {
                        break;
                    }
                }
                for (LittleTilesCubeObject occluder : neighbourCubes) {
                    if (canOcclude(occluder, cube)) {
                        coverSide(clippers[i], cube, occluder, side);
                    }
                }
            }
        }
    }

    /**
     * The cubes of the tile entity at the given position that reach its {@code border} side, which is the side facing
     * back at us and therefore the only one that can occlude anything of ours.
     */
    private static List<LittleTilesCubeObject> getNeighbourBorderCubes(IBlockAccess world, int x, int y, int z,
            ForgeDirection border) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityLittleTiles)) {
            return Collections.emptyList();
        }

        TileEntityLittleTiles little = (TileEntityLittleTiles) tileEntity;
        List<LittleTile> tiles = little.getTiles();
        List<LittleTile> snapshot;
        synchronized (tiles) {
            snapshot = new ArrayList<>(tiles);
        }

        ArrayList<LittleTilesCubeObject> cubes = new ArrayList<>();
        for (LittleTile tile : snapshot) {
            for (LittleTilesCubeObject cube : tile.getRenderingCubes()) {
                if (!ignoreForCulling(cube) && touchesBorder(cube, border)) {
                    cubes.add(cube);
                }
            }
        }
        return cubes;
    }

    /**
     * Invalid blocks have nothing to cull and cutouts are meshes rather than boxes. Clipping opaque blocks costs more
     * CPU than the saved GPU work is worth.
     */
    private static boolean ignoreForCulling(LittleTilesCubeObject cube) {
        return cube.block == null || cube.meta == -1 || cube.cutoutInfo != null || cube.block.isOpaqueCube();
    }

    /*
     * We only care about occlusion between transparent/translucent blocks of the same type - there we need to cull.
     * Opaque blocks are ignored here.
     */
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

            coverSide(clipper, cube, occluder, side);
        }
    }

    private static void coverSide(FaceClipper clipper, LittleTilesCubeObject cube, LittleTilesCubeObject occluder,
            ForgeDirection side) {
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
