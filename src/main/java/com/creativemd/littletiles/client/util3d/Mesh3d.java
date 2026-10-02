package com.creativemd.littletiles.client.util3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3f;
import org.joml.Vector3ic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.littletiles.client.render.LittleTilesBlockRenderHelper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;

/**
 * A triangle mesh on the integer grid of {@link Grid3d}. Triangles must not share point objects: transforms move the
 * points in place, so a shared one would be moved once per triangle.
 */
public class Mesh3d {

    private final List<Triangle3d> triangles;

    public Mesh3d(List<Triangle3d> triangles) {
        this.triangles = triangles;
    }

    public void scale(Vector3ic factor) {
        for (Triangle3d triangle : triangles) {
            triangle.scale(factor);
        }
    }

    public void translate(GridVector vec) {
        translate(vec.x, vec.y, vec.z);
    }

    public void translate(int x, int y, int z) {
        for (Triangle3d triangle : triangles) {
            triangle.translate(x, y, z);
        }
    }

    private static final AtomicInteger logCount = new AtomicInteger();
    private static final AtomicInteger dumpCount = new AtomicInteger();

    public File dumpMesh() {
        return dumpMesh("DumpMesh", nextDumpIndex());
    }

    /** Reserves an index for {@link #dumpMesh(String, int)}, so related dumps can share it. */
    public static int nextDumpIndex() {
        return dumpCount.getAndIncrement();
    }

    /** Dumps into {@code logs/littleTiles<name><index>.obj}. */
    public File dumpMesh(String name, int index) {
        File mcDir;
        if (FMLCommonHandler.instance().getSide().isClient()) {
            mcDir = Minecraft.getMinecraft().mcDataDir;
        } else {
            mcDir = new File(".");
        }
        File logsFolder = new File(mcDir, "logs");
        File outFile = new File(logsFolder, "littleTiles" + name + index + ".obj");
        exportObj(outFile);
        FMLLog.getLogger().info("Dumped mesh into " + outFile.getAbsolutePath());
        return outFile;
    }

    private void dumpFailingMesh() {
        File mcDir;

        if (FMLCommonHandler.instance().getSide().isClient()) {
            mcDir = Minecraft.getMinecraft().mcDataDir;
        } else {
            mcDir = new File("."); // server root
        }

        File logsFolder = new File(mcDir, "logs");
        File outFile = new File(logsFolder, "littleTilesErrorMesh" + logCount.getAndIncrement() + ".obj");
        exportObj(outFile);
        FMLLog.getLogger().error("Failed to process mesh, dumped into " + outFile.getAbsolutePath());
    }

    public Mesh3d cutByPlane(Plane3d plane) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        ArrayList<Edge3d> addedEdges = new ArrayList<>();
        ArrayList<Edge3d> existingEdges = new ArrayList<>();

        for (Triangle3d triangle : triangles) {
            GridVector p1 = triangle.getP1();
            GridVector p2 = triangle.getP2();
            GridVector p3 = triangle.getP3();

            int side1 = plane.getDistance(p1);
            int side2 = plane.getDistance(p2);
            int side3 = plane.getDistance(p3);
            int aboveCount = (side1 > 0 ? 1 : 0) + (side2 > 0 ? 1 : 0) + (side3 > 0 ? 1 : 0);
            int belowCount = (side1 < 0 ? 1 : 0) + (side2 < 0 ? 1 : 0) + (side3 < 0 ? 1 : 0);

            if (aboveCount == 0) {
                // No vertex is above the plane, so keep the triangle
                if (!triangle.isDegenerate()) {
                    newTriangles.add(triangle);
                    if (belowCount == 1) {
                        GridVector firstOn = side1 == 0 ? p1 : p2;
                        GridVector secondOn = side3 == 0 ? p3 : p2;
                        existingEdges.add(new Edge3d(firstOn, secondOn));
                    }
                }
                continue;
            }
            if (belowCount == 0) {
                // No vertex is below the plane, so discard the triangle
                continue;
            }

            ArrayList<Triangle3d> addTriangles;
            if (aboveCount == 1 && belowCount == 2) {
                GridVector firstBelow = side1 < 0 ? p1 : p2;
                GridVector secondBelow = side3 < 0 ? p3 : p2;
                GridVector above = side1 > 0 ? p1 : side2 > 0 ? p2 : p3;
                addTriangles = clipTriangle(firstBelow, secondBelow, above, plane, addedEdges);
            } else if (aboveCount == 2 && belowCount == 1) {
                GridVector below = side1 < 0 ? p1 : side2 < 0 ? p2 : p3;
                GridVector firstAbove = side1 > 0 ? p1 : p2;
                GridVector secondAbove = side3 > 0 ? p3 : p2;
                addTriangles = clipTriangleTwoAbove(below, firstAbove, secondAbove, plane, addedEdges);
            } else {
                // One vertex lies on the plane, with one vertex on each side
                GridVector on = side1 == 0 ? p1 : side2 == 0 ? p2 : p3;
                GridVector below = side1 < 0 ? p1 : side2 < 0 ? p2 : p3;
                GridVector above = side1 > 0 ? p1 : side2 > 0 ? p2 : p3;
                addTriangles = clipTriangleOneOnOneBelow(on, below, above, plane, addedEdges);
            }

            if (!addTriangles.isEmpty()) {
                GridNormal normal = triangle.unnormalizedNormal();
                for (Triangle3d t : addTriangles) {
                    t.ensureWindingOrder(normal);
                    t.inheritPlane(triangle);
                }
            }

            newTriangles.addAll(addTriangles);
        }

        /* Close gaps */
        if (!addedEdges.isEmpty()) {
            addedEdges.addAll(existingEdges); // Might have existing edges we need to take into account
            List<GridVector> addVertices = Edge3d.orderEdgesToVertexList(addedEdges);
            if (addVertices == null) {
                // Something went wrong trying to close the edges, log error and bail
                dumpFailingMesh();
                return new Mesh3d(new ArrayList<>());
            }
            Triangulator.triangulate(newTriangles, plane, removeCollinearPoints(addVertices));
        }

        return new Mesh3d(newTriangles);
    }

    /**
     * Drops the points of a closed loop that lie on the straight line between their neighbours, as well as the closing
     * point that repeats the first one. Cutting long triangles leaves many of those along the sides of a cap, and
     * triangulating them would only produce fans of long, thin triangles. Exact, as the points are on the grid.
     */
    private static List<GridVector> removeCollinearPoints(List<GridVector> loop) {
        List<GridVector> points = new ArrayList<>(loop);
        if (points.size() > 1 && points.get(0).equals(points.get(points.size() - 1))) {
            points.remove(points.size() - 1);
        }
        boolean removed = true;
        while (removed && points.size() > 3) {
            removed = false;
            for (int i = 0; i < points.size() && points.size() > 3;) {
                GridVector previous = points.get((i + points.size() - 1) % points.size());
                GridVector next = points.get((i + 1) % points.size());
                if (previous.cross(points.get(i), next).isZero()) {
                    points.remove(i);
                    removed = true;
                } else {
                    i++;
                }
            }
        }
        return points;
    }

    // Handles case when 2 vertices are below the plane, 1 is above
    private ArrayList<Triangle3d> clipTriangle(GridVector pBelow1, GridVector pBelow2, GridVector pAbove, Plane3d plane,
            ArrayList<Edge3d> addedEdges) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        GridVector i1 = plane.intersect(pBelow1, pAbove);
        GridVector i2 = plane.intersect(pBelow2, pAbove);

        if (!Triangle3d.isDegenerate(pBelow1, pBelow2, i1)) {
            newTriangles.add(new Triangle3d(pBelow1, pBelow2, i1));
        }

        if (!Triangle3d.isDegenerate(pBelow2, i1, i2)) {
            addedEdges.add(new Edge3d(i1, i2));
            // new Vectors to have different objects for later translation
            newTriangles.add(new Triangle3d(new GridVector(pBelow2), new GridVector(i1), i2));
        }

        return newTriangles;
    }

    private ArrayList<Triangle3d> clipTriangleOneOnOneBelow(GridVector pOn, GridVector pBelow, GridVector pAbove,
            Plane3d plane, ArrayList<Edge3d> addedEdges) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        GridVector i1 = plane.intersect(pBelow, pAbove);

        if (!Triangle3d.isDegenerate(pBelow, pOn, i1)) {
            newTriangles.add(new Triangle3d(pBelow, pOn, i1));
            addedEdges.add(new Edge3d(i1, pOn));
        }

        return newTriangles;
    }

    // Handles case when 1 vertex is below the plane, 2 are above
    private ArrayList<Triangle3d> clipTriangleTwoAbove(GridVector pBelow, GridVector pAbove1, GridVector pAbove2,
            Plane3d plane, ArrayList<Edge3d> addedEdges) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        GridVector i1 = plane.intersect(pBelow, pAbove1);
        GridVector i2 = plane.intersect(pBelow, pAbove2);

        if (!Triangle3d.isDegenerate(pBelow, i1, i2)) {
            addedEdges.add(new Edge3d(i1, i2));
            newTriangles.add(new Triangle3d(pBelow, i1, i2));
        }

        return newTriangles;
    }

    public List<Triangle3d> getTriangles() {
        return triangles;
    }

    public void setTextures(Block block, int meta) {
        for (Triangle3d triangle : triangles) {
            triangle.setTexture(block, meta);
        }
    }

    /**
     * Draws every triangle of this mesh with its normal and texture coordinates, viewed through the standard isometric
     * icon transform, textured from the block atlas and lit with standard GUI item lighting.
     *
     * <p>
     * Binds the block texture and enables blending, rescale-normal and lighting for the duration of the draw; all of it
     * is restored to its previous state on return. The caller is responsible for the surrounding
     * {@code glPushMatrix}/{@code glPopMatrix} pair, positioning, color, and any additional state of its own (for
     * example alpha testing).
     * </p>
     */
    public void renderIcon() {
        renderIcon(ColorUtils.WHITE, false);
    }

    /**
     * Draws this mesh as an icon, tinted with {@code color}. With {@code topTintOnly} the tint is limited to the top
     * face and the remaining faces stay white, as grass needs - see
     * {@code LittleTilesBlockRenderHelper.tintsTopFaceOnly}.
     */
    public void renderIcon(int color, boolean topTintOnly) {
        boolean lightingWasEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean rescaleWasEnabled = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        GL11.glTranslatef(1.0F, 0.5F, 1.0F);
        GL11.glScalef(1.0F, 1.0F, -1.0F);
        GL11.glRotatef(210.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);

        LittleTilesBlockRenderHelper.setGlColor(color, topTintOnly);

        GL11.glBegin(GL11.GL_TRIANGLES);
        for (Triangle3d triangle : triangles) {
            if (topTintOnly)
                LittleTilesBlockRenderHelper.setGlColor(color, triangle.getFaceDirection() != ForgeDirection.UP);
            Vector3f normal = triangle.getNormal();
            GL11.glNormal3d(normal.x, normal.y, normal.z);
            GL11.glTexCoord2d(triangle.getTex1().x, triangle.getTex1().y);
            glVertex(triangle.getP1());
            GL11.glTexCoord2d(triangle.getTex2().x, triangle.getTex2().y);
            glVertex(triangle.getP2());
            GL11.glTexCoord2d(triangle.getTex3().x, triangle.getTex3().y);
            glVertex(triangle.getP3());
        }
        GL11.glEnd();

        if (!blendWasEnabled) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (!rescaleWasEnabled) {
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        }
        if (!lightingWasEnabled) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }
    }

    private static void glVertex(GridVector point) {
        GL11.glVertex3d(point.blockX(), point.blockY(), point.blockZ());
    }

    /** Writes the mesh in blocks. */
    public void exportObj(File file) {
        try {
            FileWriter writer = new FileWriter(file);
            int vertexIndex = 1; // OBJ indices start at 1

            // First, write all vertices
            for (Triangle3d tri : triangles) {
                GridVector[] verts = { tri.getP1(), tri.getP2(), tri.getP3() };
                for (GridVector v : verts) {
                    writer.write(String.format("v %.6f %.6f %.6f%n", v.blockX(), v.blockY(), v.blockZ()));
                }
            }

            // Then, write faces
            for (int i = 0; i < triangles.size(); i++) {
                // Faces in OBJ reference vertices by their 1-based index
                writer.write(String.format("f %d %d %d%n", vertexIndex, vertexIndex + 1, vertexIndex + 2));
                vertexIndex += 3;
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Rotates the mesh within the box from the origin to {@code size}, so that afterwards it fills the rotated box from
     * the origin to {@code size} rotated as well. Every component of {@code size} has to be even, so the center of the
     * box is on the grid.
     */
    public void rotate(int orientation, GridVector size) {
        GridVector rotatedSize = new GridVector(size).rotate(orientation);
        rotatedSize.absolute();
        translate(-size.x / 2, -size.y / 2, -size.z / 2);
        for (Triangle3d triangle : triangles) {
            triangle.rotate(orientation);
        }
        translate(rotatedSize.x / 2, rotatedSize.y / 2, rotatedSize.z / 2);
    }

    public Mesh3d copy() {
        List<Triangle3d> trianglesNew = new ArrayList<>();
        for (Triangle3d triangle : triangles) {
            trianglesNew.add(triangle.copy());
        }
        return new Mesh3d(trianglesNew);
    }

    public List<Triangle3d> getTrianglesForSide(ForgeDirection direction) {
        Plane3d plane = Plane3d.planes[direction.ordinal()];
        List<Triangle3d> ret = new ArrayList<>();
        for (Triangle3d triangle : triangles) {
            if (Plane3d.getPlaneForTriangle(triangle) == plane) {
                ret.add(triangle);
            }
        }

        return ret;
    }

    public static List<GridVector> getPointsForTriangles(List<Triangle3d> triangles) {
        List<GridVector> allPoints = new ArrayList<>();
        for (Triangle3d triangle : triangles) {
            allPoints.add(triangle.getP1());
            allPoints.add(triangle.getP2());
            allPoints.add(triangle.getP3());
        }
        return allPoints;
    }

    public List<GridVector> getPointsForSide(ForgeDirection faceStart) {
        List<GridVector> ret = new ArrayList<>();
        List<GridVector> allPoints = getPointsForTriangles(triangles);
        List<Triangle3d> sidedTriangles = getTrianglesForSide(faceStart);
        List<GridVector> trianglePoints = getPointsForTriangles(sidedTriangles);
        for (GridVector point : allPoints) {
            if (trianglePoints.contains(point)) {
                ret.add(point);
            }
        }
        return ret;
    }

    /**
     * Returns a point expected to be inside this closed mesh.
     *
     * The point is based on the first triangle's centroid and nudged slightly opposite that triangle's outward normal.
     * This avoids using an exact surface vertex or face point when a later containment test needs an interior sample.
     *
     * @return A point just inside the mesh surface, in blocks.
     */
    public Vector3f getInteriorSamplePoint() {
        Triangle3d t = triangles.get(0);
        GridVector p1 = t.getP1();
        GridVector p2 = t.getP2();
        GridVector p3 = t.getP3();
        GridVector point = new GridVector(
                (int) Grid3d.divRound((long) p1.x + p2.x + p3.x, 3),
                (int) Grid3d.divRound((long) p1.y + p2.y + p3.y, 3),
                (int) Grid3d.divRound((long) p1.z + p2.z + p3.z, 3));
        // Along the axis the normal points along the most, so the step always leads to the inner side. It leaves the
        // plane by at least INTERIOR_STEP / sqrt(3), well clear of the half unit the centroid was rounded by.
        GridNormal normal = t.unnormalizedNormal();
        int axis = normal.dominantAxis();
        int step = normal.get(axis) > 0 ? -INTERIOR_STEP : INTERIOR_STEP;
        point.setComponent(axis, point.get(axis) + step);
        return point.toVector3f();
    }

    /** How far {@link #getInteriorSamplePoint} moves into the mesh, in grid units. About 1e-4 blocks. */
    private static final int INTERIOR_STEP = 8;

    /**
     * Tests whether a point is inside this closed mesh using the summed signed solid angle of all triangles.
     *
     * Points inside a consistently wound closed mesh produce a total solid angle near +/-4*pi. Points outside produce a
     * value near 0. The threshold intentionally only accepts clear interior points; callers should avoid points that
     * lie exactly on a mesh boundary.
     *
     * @param point The point to test in mesh coordinates.
     * @return True when the point is inside the mesh volume.
     */
    public boolean containsPoint(Vector3f point) {
        double solidAngle = 0.0;
        for (Triangle3d triangle : triangles) {
            solidAngle += triangle.signedSolidAngle(point);
        }
        return Math.abs(solidAngle) > 2.0 * Math.PI;
    }

    /** All corners of all triangles, in blocks. */
    public Vector3f[] getVertices() {
        Vector3f[] ret = new Vector3f[triangles.size() * 3];
        for (int i = 0; i < triangles.size(); i++) {
            Triangle3d triangle = triangles.get(i);
            ret[i * 3] = triangle.getP1().toVector3f();
            ret[i * 3 + 1] = triangle.getP2().toVector3f();
            ret[i * 3 + 2] = triangle.getP3().toVector3f();
        }
        return ret;
    }
}
