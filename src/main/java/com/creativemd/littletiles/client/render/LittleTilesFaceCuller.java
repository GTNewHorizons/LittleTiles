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
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * <p>
 * A box has rectangular faces, so {@link #computeCoverage} subtracts covered rectangles with integer math and hands the
 * result to {@link FaceClipper}; fully covered faces produce no pieces and therefore emit no quad. A cutout is an
 * arbitrary mesh instead, so {@link #visibleTriangles} subtracts triangles from its faces directly. Both halves share
 * the same grid box tests to find out what touches what.
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
     * Invalid blocks have nothing to cull. Clipping opaque blocks costs more CPU than the saved GPU work is worth.
     */
    private static boolean ignoreForCulling(LittleTilesCubeObject cube) {
        return cube.block == null || cube.meta == -1 || cube.block.isOpaqueCube();
    }

    /**
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

    /** Whether the occluder sits directly against the given side of the cube, without overlapping it. */
    private static boolean isFlush(LittleTilesCubeObject cube, LittleTilesCubeObject occluder, ForgeDirection side) {
        Axis axis = Axis.getAxis(side);
        return RotationUtils.isNegative(side) ? occluder.gridMax(axis) == cube.gridMin(axis)
                : occluder.gridMin(axis) == cube.gridMax(axis);
    }

    /** Marks the areas the given occluder covers on the faces of {@code cube} it sits flush against. */
    private static void cover(FaceClipper clipper, LittleTilesCubeObject cube, LittleTilesCubeObject occluder) {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (isFlush(cube, occluder, side)) {
                coverSide(clipper, cube, occluder, side);
            }
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

    // ================Cutouts================

    /** Geometry shared by all cutouts rendered for one tile entity. */
    public static final class CutoutCulling {

        private final List<LittleTilesCubeObject> cubes;
        private final List<Neighbour> neighbours;

        private CutoutCulling(List<LittleTilesCubeObject> cubes, List<Neighbour> neighbours) {
            this.cubes = cubes;
            this.neighbours = neighbours;
        }
    }

    private static final class Neighbour {

        private final LittleTilesCubeObject cube;
        private final ForgeDirection side;
        private final List<Triangle3d> triangles;

        private Neighbour(LittleTilesCubeObject cube, ForgeDirection side, List<Triangle3d> triangles) {
            this.cube = cube;
            this.side = side;
            this.triangles = triangles;
        }
    }

    /**
     * Takes a snapshot of each neighbouring tile entity and moves its border meshes into this block's space. The result
     * can be reused for every cutout in the current render.
     */
    public static CutoutCulling prepareCutoutCulling(IBlockAccess world, List<LittleTilesCubeObject> cubes, int x,
            int y, int z) {
        List<Neighbour> neighbours = new ArrayList<>();
        boolean hasCutout = false;
        for (LittleTilesCubeObject cube : cubes) {
            if (cube.renderCache.hasValidMesh()) {
                hasCutout = true;
                break;
            }
        }
        if (!hasCutout) {
            return new CutoutCulling(cubes, neighbours);
        }

        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            List<LittleTilesCubeObject> neighbors = getNeighbourBorderCubes(
                    world,
                    x + side.offsetX,
                    y + side.offsetY,
                    z + side.offsetZ,
                    side.getOpposite());
            for (LittleTilesCubeObject neighbor : neighbors) {
                if (!neighbor.renderCache.hasValidMesh()) {
                    continue;
                }
                Mesh3d moved = neighbor.renderCache.getSimpleMesh().copy();
                moved.translate(new Vector3d(side.offsetX, side.offsetY, side.offsetZ));
                neighbours.add(new Neighbour(neighbor, side, moved.getTriangles()));
            }
        }
        return new CutoutCulling(cubes, neighbours);
    }

    /**
     * The triangles of a cutout's mesh that are still visible, after removing what the meshes around it hide - both the
     * ones in the same tile entity and the ones in the six neighbours.
     */
    public static List<Triangle3d> visibleTriangles(CutoutCulling culling, LittleTilesCubeObject cube) {
        List<Triangle3d> occludingTriangles = getOccludingTriangles(culling, cube);
        List<Triangle3d> visible = new ArrayList<>();
        for (Triangle3d triangle : cube.renderCache.getSimpleMesh().getTriangles()) {
            visible.addAll(cutTriangle(triangle, occludingTriangles));
        }
        return visible;
    }

    /** Every triangle that could hide something of the cube, in this block's space. */
    private static List<Triangle3d> getOccludingTriangles(CutoutCulling culling, LittleTilesCubeObject cube) {
        List<Triangle3d> occludingTriangles = new ArrayList<>();

        for (LittleTilesCubeObject occluder : culling.cubes) {
            if (occluder != cube && occluder.renderCache.hasValidMesh() && canOcclude(occluder, cube)) {
                occludingTriangles.addAll(occluder.renderCache.getSimpleMesh().getTriangles());
            }
        }
        for (Neighbour neighbour : culling.neighbours) {
            if (touchesBorder(cube, neighbour.side) && canOcclude(neighbour.cube, cube)) {
                occludingTriangles.addAll(neighbour.triangles);
            }
        }
        return occludingTriangles;
    }

    /**
     * Cuts a triangle and returns whatever is left of the triangle once all occluding triangles are subtracted from it,
     * empty when it is covered entirely. The triangle counterpart of {@link #coverSide}.
     */
    private static List<Triangle3d> cutTriangle(Triangle3d triangle, List<Triangle3d> occludingTriangles) {
        List<Triangle3d> remaining = Collections.singletonList(triangle.copy());
        for (Triangle3d occludingTriangle : occludingTriangles) {
            // Quick check to avoid unnecessary work. We're O(n2) already...
            if (!triangle.boundsOverlap(occludingTriangle) || !triangle.isCoplanar(occludingTriangle)) {
                continue;
            }
            List<Triangle3d> next = new ArrayList<>();
            for (Triangle3d piece : remaining) {
                if (piece.boundsOverlap(occludingTriangle)) {
                    next.addAll(piece.split(occludingTriangle));
                } else {
                    next.add(piece);
                }
            }
            remaining = next;
            if (remaining.isEmpty()) {
                break; // fully hidden, the rest of the occluding triangles cannot change that
            }
        }
        return remaining;
    }
}
