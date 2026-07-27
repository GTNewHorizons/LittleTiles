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
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Computes which parts of little-tile boxes and meshes are hidden by adjacent geometry.
 * <p>
 * A box has rectangular faces, so {@link #computeCoverage} subtracts covered rectangles with integer math and hands the
 * result to {@link FaceClipper}; fully covered faces produce no pieces and therefore emit no quad. A cutout is an
 * arbitrary mesh instead, so {@link #visibleTriangles} subtracts triangles from its faces directly. Both halves share
 * the same grid box tests to find out what touches what.
 * <p>
 * Boxes and meshes can hide each other. Box faces are converted to triangles when rectangular clipping cannot represent
 * the visible area.
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
            for (LittleTilesCubeObject occluder : cubes) {
                if (occluder.cutoutInfo == null && canOcclude(occluder, cube)) {
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
                    if (occluder.cutoutInfo == null && canOcclude(occluder, cube)) {
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
        if (!(tileEntity instanceof TileEntityLittleTiles little)) {
            return Collections.emptyList();
        }

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
        if (occluder == cube || ignoreForCulling(occluder)) {
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

    // ================Triangle culling================

    /** Geometry shared by all triangle culling done for one tile entity. */
    public static final class CullingContext {

        private final List<LittleTilesCubeObject> localCubes;
        private final List<Neighbour> neighbours;

        private CullingContext(List<LittleTilesCubeObject> localCubes, List<Neighbour> neighbours) {
            this.localCubes = localCubes;
            this.neighbours = neighbours;
        }
    }

    /** A cube of an adjacent tile entity, with the geometry facing us already moved into this block's space. */
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
     * Takes a snapshot of each neighbouring tile entity and moves its border geometry into this block's space. The
     * result can be reused for every cube in the current render.
     */
    public static CullingContext prepareCulling(IBlockAccess world, List<LittleTilesCubeObject> cubes, int x, int y,
            int z) {
        List<Neighbour> neighbours = new ArrayList<>();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (!anyTouchesBorder(cubes, side)) {
                continue;
            }
            ForgeDirection facingUs = side.getOpposite();
            List<LittleTilesCubeObject> neighbourCubes = getNeighbourBorderCubes(
                    world,
                    x + side.offsetX,
                    y + side.offsetY,
                    z + side.offsetZ,
                    facingUs);
            for (LittleTilesCubeObject neighbour : neighbourCubes) {
                // copies, since these get moved and the mesh is the one cached on the neighbouring tile
                List<Triangle3d> triangles;
                if (neighbour.cutoutInfo != null) {
                    triangles = neighbour.renderCache.getSimpleMesh().copy().getTriangles();
                } else {
                    triangles = boxFaceTriangles(neighbour, facingUs);
                }
                for (Triangle3d triangle : triangles) {
                    triangle.translate(side.offsetX, side.offsetY, side.offsetZ);
                }
                neighbours.add(new Neighbour(neighbour, side, triangles));
            }
        }
        return new CullingContext(cubes, neighbours);
    }

    private static boolean anyTouchesBorder(List<LittleTilesCubeObject> cubes, ForgeDirection side) {
        for (LittleTilesCubeObject cube : cubes) {
            if (touchesBorder(cube, side)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The triangles of a cutout's mesh that are still visible, after removing what the meshes around it hide - both the
     * ones in the same tile entity and the ones in the six neighbours.
     */
    public static List<Triangle3d> visibleTriangles(CullingContext culling, LittleTilesCubeObject cube) {
        List<Triangle3d> occludingTriangles = getOccludingTriangles(culling, cube, false);
        List<Triangle3d> visible = new ArrayList<>();
        for (Triangle3d triangle : cube.renderCache.getSimpleMesh().getTriangles()) {
            visible.addAll(cutTriangle(triangle, occludingTriangles));
        }
        return visible;
    }

    /**
     * The counterpart of {@link #visibleTriangles}: the faces of a box that remain visible after culling against
     * meshes and other boxes.
     */
    public static List<Triangle3d> visibleBoxTriangles(CullingContext culling, LittleTilesCubeObject cube,
            FaceClipper clipper) {
        List<Triangle3d> meshOccludingTriangles = getOccludingTriangles(culling, cube, true);
        if (meshOccludingTriangles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Triangle3d> allOccludingTriangles = getOccludingTriangles(culling, cube, false);
        List<Triangle3d> visible = new ArrayList<>();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            List<Triangle3d> face = boxFaceTriangles(cube, side);
            if (!overlapsAny(face, meshOccludingTriangles)) {
                continue; // no mesh in this plane, the rectangle clipping of the box renderer covers this side
            }
            coverSide(clipper, cube, cube, side);
            for (Triangle3d triangle : face) {
                visible.addAll(cutTriangle(triangle, allOccludingTriangles));
            }
        }
        return visible;
    }

    /** Whether an occluding triangle shares a plane with one of the triangles and could therefore hide part of it. */
    private static boolean overlapsAny(List<Triangle3d> triangles, List<Triangle3d> occludingTriangles) {
        for (Triangle3d triangle : triangles) {
            for (Triangle3d occludingTriangle : occludingTriangles) {
                if (triangle.boundsOverlap(occludingTriangle) && triangle.isCoplanar(occludingTriangle)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Every triangle that could hide something of the cube, in this block's space: the meshes around it, plus (if
     * {@code meshOccludersOnly} is false) the faces of the boxes sitting flush against it.
     */
    private static List<Triangle3d> getOccludingTriangles(CullingContext culling, LittleTilesCubeObject cube,
            boolean meshOccludersOnly) {
        List<Triangle3d> occludingTriangles = new ArrayList<>();

        for (LittleTilesCubeObject occluder : culling.localCubes) {
            if (!canOcclude(occluder, cube)) {
                continue;
            }
            if (occluder.cutoutInfo != null) {
                occludingTriangles.addAll(occluder.renderCache.getSimpleMesh().getTriangles());
            } else if (!meshOccludersOnly) {
                for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                    if (isFlush(cube, occluder, side)) {
                        occludingTriangles.addAll(boxFaceTriangles(occluder, side.getOpposite()));
                    }
                }
            }
        }
        for (Neighbour neighbour : culling.neighbours) {
            if (meshOccludersOnly && neighbour.cube.cutoutInfo == null) {
                continue;
            }
            if (touchesBorder(cube, neighbour.side) && canOcclude(neighbour.cube, cube)) {
                occludingTriangles.addAll(neighbour.triangles);
            }
        }
        return occludingTriangles;
    }

    /** Builds the two outward-facing triangles of one box face. */
    private static List<Triangle3d> boxFaceTriangles(LittleTilesCubeObject cube, ForgeDirection side) {
        double minX = cube.gridMinX / 16.0;
        double minY = cube.gridMinY / 16.0;
        double minZ = cube.gridMinZ / 16.0;
        double maxX = cube.gridMaxX / 16.0;
        double maxY = cube.gridMaxY / 16.0;
        double maxZ = cube.gridMaxZ / 16.0;
        return switch (side) {
            case DOWN -> quad(
                new Vector3d(minX, minY, minZ),
                new Vector3d(maxX, minY, minZ),
                new Vector3d(maxX, minY, maxZ),
                new Vector3d(minX, minY, maxZ));
            case UP -> quad(
                new Vector3d(minX, maxY, minZ),
                new Vector3d(minX, maxY, maxZ),
                new Vector3d(maxX, maxY, maxZ),
                new Vector3d(maxX, maxY, minZ));
            case NORTH -> quad(
                new Vector3d(minX, minY, minZ),
                new Vector3d(minX, maxY, minZ),
                new Vector3d(maxX, maxY, minZ),
                new Vector3d(maxX, minY, minZ));
            case SOUTH -> quad(
                new Vector3d(minX, minY, maxZ),
                new Vector3d(maxX, minY, maxZ),
                new Vector3d(maxX, maxY, maxZ),
                new Vector3d(minX, maxY, maxZ));
            case WEST -> quad(
                new Vector3d(minX, minY, minZ),
                new Vector3d(minX, minY, maxZ),
                new Vector3d(minX, maxY, maxZ),
                new Vector3d(minX, maxY, minZ));
            case EAST -> quad(
                new Vector3d(maxX, minY, minZ),
                new Vector3d(maxX, maxY, minZ),
                new Vector3d(maxX, maxY, maxZ),
                new Vector3d(maxX, minY, maxZ));
            default -> Collections.emptyList();
        };
    }

    private static List<Triangle3d> quad(Vector3d p1, Vector3d p2, Vector3d p3, Vector3d p4) {
        List<Triangle3d> triangles = new ArrayList<>(2);
        triangles.add(new Triangle3d(p1, p2, p3));
        triangles.add(new Triangle3d(new Vector3d(p1), new Vector3d(p3), p4));
        return triangles;
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
