package com.creativemd.littletiles.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.rendering.RenderHelper3D;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleDeformedBoxPreviewRenderer {

    /** Opacity of the fill of the corner the player is aiming at. */
    private static final double CORNER_MARKER_HOVER_FILL_ALPHA = 0.25;
    /**
     * How far the deformed box overlay is pulled off the grid planes to prevent z-fighting, in blocks.
     */
    private static final double Z_FIGHT_EPSILON = 0.002;

    /** The 12 edges of the box, as pairs of corner indices - the two corners of an edge differ in exactly one axis. */
    private static final int[][] BOX_EDGES = { { 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 }, // along x
            { 0, 2 }, { 1, 3 }, { 4, 6 }, { 5, 7 }, // along y
            { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 }, // along z
    };

    /**
     * Draws the deformed box being edited as a wireframe of its 12 edges. Faces are never filled, so the player can see
     * the tiles behind the box while shaping it.
     */
    public static void renderBoxEdges(int grid) {
        boolean valid = LittleDeformedBoxHelper.hasValidGeometry();
        double width = cornerMarkerEdgeWidth(grid);
        GL11.glColor4d(valid ? 0.2 : 1, valid ? 0.8 : 0.1, valid ? 1 : 0.1, 0.9);
        GL11.glBegin(GL11.GL_QUADS);
        for (int[] edge : BOX_EDGES) {
            renderBeam(cornerFromCamera(edge[0]), cornerFromCamera(edge[1]), width);
        }
        GL11.glEnd();

        renderFaceDiagonals(width);
    }

    /** A corner relative to the camera, which sits at the origin while rendering. */
    private static Vector3d cornerFromCamera(int index) {
        Vec3 vec = LittleDeformedBoxHelper.cornerHitVec(index);
        double x = vec.xCoord - TileEntityRendererDispatcher.staticPlayerX;
        double y = vec.yCoord - TileEntityRendererDispatcher.staticPlayerY;
        double z = vec.zCoord - TileEntityRendererDispatcher.staticPlayerZ;

        // A deformed box cannot simply be grown like an axis-aligned one. Pulling each corner towards the camera lifts
        // the edges off any face from every angle.
        double scale = Math.max(0, 1 - Z_FIGHT_EPSILON / Math.sqrt(x * x + y * y + z * z));
        return new Vector3d(x * scale, y * scale, z * scale);
    }

    /**
     * Draws a line as a square beam of the given width, in blocks. Like the corner markers it is real world-space
     * geometry rather than a GL line, so it does not get thinner on high resolution displays. Both ends are extended by
     * half the width, so beams meeting at a corner close the joint. Must be called between
     * <code>glBegin(GL_QUADS)</code> and <code>glEnd()</code>.
     */
    private static void renderBeam(Vector3d from, Vector3d to, double width) {
        Vector3d along = new Vector3d(to);
        along.sub(from);
        double length = along.length();
        // A collapsed edge has no direction to build a beam around, and the corner marker already covers it
        if (length < 1.0E-9) {
            return;
        }
        along.scale(width / 2 / length);

        // Any axis the beam is not parallel to spans its cross section - the one it deviates from most is the safest
        double ax = Math.abs(along.x), ay = Math.abs(along.y), az = Math.abs(along.z);
        Vector3d axis = ax <= ay && ax <= az ? new Vector3d(1, 0, 0)
                : ay <= az ? new Vector3d(0, 1, 0) : new Vector3d(0, 0, 1);
        Vector3d side = new Vector3d();
        side.cross(along, axis);
        side.normalize();
        side.scale(width / 2);
        Vector3d up = new Vector3d();
        up.cross(along, side);
        up.normalize();
        up.scale(width / 2);

        Vector3d start = new Vector3d(from);
        start.sub(along);
        Vector3d end = new Vector3d(to);
        end.add(along);

        // The corners of the cross section, in ring order
        Vector3d[] ring = new Vector3d[4];
        for (int i = 0; i < ring.length; i++) {
            ring[i] = new Vector3d(side);
            ring[i].scale(i == 0 || i == 3 ? 1 : -1);
            ring[i].scaleAdd(i < 2 ? 1 : -1, up, ring[i]);
        }

        for (int i = 0; i < ring.length; i++) {
            Vector3d next = ring[(i + 1) % ring.length];
            beamVertex(start, ring[i]);
            beamVertex(start, next);
            beamVertex(end, next);
            beamVertex(end, ring[i]);
        }
        for (int i = ring.length - 1; i >= 0; i--) {
            beamVertex(start, ring[i]);
        }
        for (Vector3d offset : ring) {
            beamVertex(end, offset);
        }
    }

    private static void beamVertex(Vector3d base, Vector3d offset) {
        GL11.glVertex3d(base.x + offset.x, base.y + offset.y, base.z + offset.z);
    }

    /**
     * Draws the split diagonal of every face whose 4 corners are no longer coplanar, showing where the surface actually
     * bends. The diagonal comes from {@link Mesh3dUtil#splitsAlongFirstDiagonal}, the same call the mesh itself is
     * built from, so the line can never disagree with the geometry it is describing.
     */
    private static void renderFaceDiagonals(double width) {
        LittleTileCutoutInfo cutout = LittleDeformedBoxHelper.currentCutout();
        Vector3d[] local = new Vector3d[cutout.corners.length];
        for (int i = 0; i < local.length; i++) {
            local[i] = Mesh3dUtil.toLocal(cutout.corners[i], cutout.size);
        }

        boolean valid = LittleDeformedBoxHelper.hasValidGeometry();
        GL11.glColor4d(valid ? 0.2 : 1, valid ? 0.8 : 0.1, valid ? 1 : 0.1, 0.45);
        GL11.glBegin(GL11.GL_QUADS);
        for (int[] face : Mesh3dUtil.DEFORMED_BOX_FACES) {
            if (isFacePlanar(face, cutout.corners)) {
                continue;
            }
            boolean first = Mesh3dUtil
                    .splitsAlongFirstDiagonal(local[face[0]], local[face[1]], local[face[2]], local[face[3]]);
            renderBeam(cornerFromCamera(first ? face[0] : face[1]), cornerFromCamera(first ? face[2] : face[3]), width);
        }
        GL11.glEnd();
    }

    /**
     * Whether a face's 4 corners still lie in one plane - that is, whether the face is merely tilted or has actually
     * been folded. A flat face is drawn by two coplanar triangles, so its diagonal is invisible on the surface and
     * drawing it would suggest a bend that is not there; only a folded face has a fold worth showing.
     * <p>
     * Worked out as a scalar triple product in whole grid units, which makes the test exact: corner offsets are
     * integers, so the product either is zero or it is not, and there is no epsilon to tune. Longs because a corner
     * dragged a long way makes the intermediate cross product outgrow an int.
     * <p>
     * A face with three collinear corners counts as planar, correctly: some plane always contains that line and the
     * fourth corner, and the mesh gets nothing but a degenerate triangle out of it.
     */
    private static boolean isFacePlanar(int[] face, Vector3i[] corners) {
        Vector3i a = corners[face[0]];
        long abx = corners[face[1]].x - a.x, aby = corners[face[1]].y - a.y, abz = corners[face[1]].z - a.z;
        long acx = corners[face[2]].x - a.x, acy = corners[face[2]].y - a.y, acz = corners[face[2]].z - a.z;
        long adx = corners[face[3]].x - a.x, ady = corners[face[3]].y - a.y, adz = corners[face[3]].z - a.z;

        long nx = aby * acz - abz * acy;
        long ny = abz * acx - abx * acz;
        long nz = abx * acy - aby * acx;
        return nx * adx + ny * ady + nz * adz == 0;
    }

    private static void renderCornerMarker(AxisAlignedBB box, double width, boolean selected, boolean hovered,
            boolean valid) {
        double minX = box.minX - TileEntityRendererDispatcher.staticPlayerX;
        double minY = box.minY - TileEntityRendererDispatcher.staticPlayerY;
        double minZ = box.minZ - TileEntityRendererDispatcher.staticPlayerZ;
        double maxX = box.maxX - TileEntityRendererDispatcher.staticPlayerX;
        double maxY = box.maxY - TileEntityRendererDispatcher.staticPlayerY;
        double maxZ = box.maxZ - TileEntityRendererDispatcher.staticPlayerZ;

        double red = valid ? (selected ? 1 : 0.2) : 1;
        double green = valid ? 0.6 : 0.1;
        double blue = valid ? (selected ? 0 : 1) : 0.1;
        double alpha = selected ? 0.9 : 0.5;

        // The corner a left click would select is filled in faintly, like vanilla highlights the block under the
        // cursor. A fill rather than a different colour, since colour already tells selected and invalid apart. It is
        // grown slightly so a side resting on a block face sinks behind it instead of z-fighting with it.
        if (hovered) {
            RenderHelper3D.renderBlock(
                    (minX + maxX) / 2,
                    (minY + maxY) / 2,
                    (minZ + maxZ) / 2,
                    maxX - minX + 2 * Z_FIGHT_EPSILON,
                    maxY - minY + 2 * Z_FIGHT_EPSILON,
                    maxZ - minZ + 2 * Z_FIGHT_EPSILON,
                    0,
                    0,
                    0,
                    red,
                    green,
                    blue,
                    CORNER_MARKER_HOVER_FILL_ALPHA);
        }

        // These are real world-space cuboids rather than GL lines. Their thickness therefore stays uniform on every
        // face and does not depend on the display's pixel scale or the driver's supported line widths.
        for (int sideA = 0; sideA < 2; sideA++) {
            for (int sideB = 0; sideB < 2; sideB++) {
                double x = sideA == 0 ? minX : maxX;
                double y = sideA == 0 ? minY : maxY;
                double z = sideB == 0 ? minZ : maxZ;

                renderMarkerEdge((minX + maxX) / 2, y, z, maxX - minX + width, width, width, red, green, blue, alpha);
                renderMarkerEdge(x, (minY + maxY) / 2, z, width, maxY - minY + width, width, red, green, blue, alpha);

                y = sideB == 0 ? minY : maxY;
                renderMarkerEdge(x, y, (minZ + maxZ) / 2, width, width, maxZ - minZ + width, red, green, blue, alpha);
            }
        }
    }

    /**
     * Physical width of the corner edges on a grid, in blocks. The corner cubes halve with every grid step, but edges
     * shrinking just as fast get lost on the small ones - so they only halve every second step.
     */
    private static double cornerMarkerEdgeWidth(int grid) {
        return switch (grid) {
            case 16, 8 -> 1D / 64D;
            case 4, 2 -> 1D / 128D;
            default -> 1D / 256D;
        };
    }

    private static void renderMarkerEdge(double x, double y, double z, double sizeX, double sizeY, double sizeZ,
            double red, double green, double blue, double alpha) {
        RenderHelper3D.renderBlock(x, y, z, sizeX, sizeY, sizeZ, 0, 0, 0, red, green, blue, alpha);
    }

    /**
     * Draws a small wireframe cube on each of the 8 corners of the deformed box being edited, so the player can see
     * what there is to grab. The selected corner is drawn in a different colour, the one aimed at is filled in. These
     * are the very same cubes {@link LittleDeformedBoxHelper#pickLookedAtCorner} raytraces against, so what is clicked
     * is what is shown.
     */
    public static void renderCornerMarkers(int grid) {
        boolean valid = LittleDeformedBoxHelper.hasValidGeometry();
        int hovered = LittleDeformedBoxHelper.pickLookedAtCorner(Minecraft.getMinecraft().thePlayer, grid);
        double width = cornerMarkerEdgeWidth(grid);
        for (int i = 0; i < Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT; i++) {
            AxisAlignedBB box = LittleDeformedBoxHelper.getCornerBoxAABB(i, grid);
            boolean selected = LittleDeformedBoxHelper.isMarkedCorner(i);
            renderCornerMarker(box, width, selected, i == hovered, valid);
        }
    }
}
