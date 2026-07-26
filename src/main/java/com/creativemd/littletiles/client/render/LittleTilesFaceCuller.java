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

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.creativemd.creativecore.common.utils.RotationUtils;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.OrientationMapper;
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
    public static FaceClipper[] computeCoverage(IBlockAccess world, List<LittleTilesCubeObject> cubes, int x, int y,
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

    static boolean hasUncutSolidSide(LittleTilesCubeObject cube, ForgeDirection side) {
        if (cube.cutoutInfo == null) {
            return true;
        }
        return hasUncutMeshSide(cube, side, Mesh3dUtil.getSolidSides(cube.cutoutInfo.type));
    }

    /** Whether this side of the mesh is one of the shape's cross-sections, see {@link Mesh3dUtil#getPrismSides}. */
    private static boolean hasUncutPrismSide(LittleTilesCubeObject cube, ForgeDirection side) {
        if (cube.cutoutInfo == null) {
            return false;
        }
        return hasUncutMeshSide(cube, side, Mesh3dUtil.getPrismSides(cube.cutoutInfo.type));
    }

    /** Whether one of the given sides of the shape ends up on {@code side} and survived the clipping to the box. */
    private static boolean hasUncutMeshSide(LittleTilesCubeObject cube, ForgeDirection side, boolean[] baseSides) {
        for (ForgeDirection baseSide : ForgeDirection.VALID_DIRECTIONS) {
            if (baseSides[baseSide.ordinal()] && rotate(baseSide, cube.cutoutInfo.orientation) == side
                    && !isSideCut(cube, side)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the two meshes meet end to end with the very same cross-section, so that each hides the other's face on
     * {@code side} completely. That needs the same shape and orientation, both facing each other with an uncut
     * cross-section.
     */
    private static boolean isMatchingPrismSide(LittleTilesCubeObject cube, LittleTilesCubeObject occluder,
            ForgeDirection side) {
        if (cube.cutoutInfo == null || occluder.cutoutInfo == null
                || cube.cutoutInfo.type != occluder.cutoutInfo.type
                || cube.cutoutInfo.orientation != occluder.cutoutInfo.orientation) {
            return false;
        }
        if (!hasUncutPrismSide(cube, side) || !hasUncutPrismSide(occluder, side.getOpposite())) {
            return false;
        }
        return isAlignedOnAxis(cube, occluder, PLANE_X_AXIS[side.ordinal()])
                && isAlignedOnAxis(cube, occluder, PLANE_Y_AXIS[side.ordinal()]);
    }

    /** Whether both the boxes and the meshes inside them span the exact same range on the given axis. */
    private static boolean isAlignedOnAxis(LittleTilesCubeObject cube, LittleTilesCubeObject occluder, Axis axis) {
        return cube.gridMin(axis) == occluder.gridMin(axis) && cube.gridMax(axis) == occluder.gridMax(axis)
                && component(cube.cutoutInfo.pos, axis) == component(occluder.cutoutInfo.pos, axis)
                && component(cube.cutoutInfo.size, axis) == component(occluder.cutoutInfo.size, axis);
    }

    /**
     * Whether the shape's face on the given side did not survive the clipping to this little-tile box, so that the box
     * side is not covered by it. Mesh3dUtil places the mesh at the cutout offset relative to the box and then clips it
     * to the box, so the face is only left intact if the mesh ends exactly on the box side: sticking out means it got
     * cut off, ending short means the face is somewhere inside the box instead of on its side.
     */
    private static boolean isSideCut(LittleTilesCubeObject cube, ForgeDirection side) {
        Axis axis = Axis.getAxis(side);
        int meshMin = cube.gridMin(axis) + component(cube.cutoutInfo.pos, axis);
        int meshMax = meshMin + component(cube.cutoutInfo.size, axis);
        return RotationUtils.isNegative(side) ? meshMin != cube.gridMin(axis) : meshMax != cube.gridMax(axis);
    }

    private static int component(Vector3i vector, Axis axis) {
        return axis == AxisX ? vector.x : (axis == AxisY ? vector.y : vector.z);
    }

    private static ForgeDirection rotate(ForgeDirection side, int orientation) {
        Matrix3f matrix = OrientationMapper.fromId(orientation);
        Vector3f rotated = matrix.transform(new Vector3f(side.offsetX, side.offsetY, side.offsetZ));
        int x = Math.round(rotated.x);
        int y = Math.round(rotated.y);
        int z = Math.round(rotated.z);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (direction.offsetX == x && direction.offsetY == y && direction.offsetZ == z) {
                return direction;
            }
        }
        return ForgeDirection.UNKNOWN;
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
                        coverFlushSide(clippers[i], cube, occluder, side);
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
                if (!ignoreForCulling(cube) && touchesBorder(cube, border)
                        && (hasUncutSolidSide(cube, border) || hasUncutPrismSide(cube, border))) {
                    cubes.add(cube);
                }
            }
        }
        return cubes;
    }

    /**
     * Invalid blocks have nothing to cull. Clipping opaque blocks costs more CPU than the saved GPU work is worth.
     */
    private static boolean ignoreForCulling(LittleTilesCubeObject cube) {
        return cube.block == null || cube.meta == -1 || cube.block.isOpaqueCube();
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
            if (flush) {
                coverFlushSide(clipper, cube, occluder, side);
            }
        }
    }

    /**
     * Hides what the occluder sitting flush against the given side of {@code cube} covers there: either a rectangle of
     * the box face, or, for two meshes meeting end to end, the whole face.
     */
    private static void coverFlushSide(FaceClipper clipper, LittleTilesCubeObject cube, LittleTilesCubeObject occluder,
            ForgeDirection side) {
        if (hasUncutSolidSide(occluder, side.getOpposite())) {
            coverSide(clipper, cube, occluder, side);
        } else if (isMatchingPrismSide(cube, occluder, side)) {
            clipper.cullSide(side);
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
            clipper.cover(side, minPlaneX, maxPlaneX, minPlaneY, maxPlaneY);
        }
    }
}
