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
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.creativemd.creativecore.lib.Vector3d;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;

public class Mesh3d {

    private static final double DEGENERATE_EPSILON = 1.0E-8;
    private static final double DEGENERATE_EPSILON_SQUARED = DEGENERATE_EPSILON * DEGENERATE_EPSILON;

    private final List<Triangle3d> triangles;

    public Mesh3d(List<Triangle3d> triangles) {
        this.triangles = triangles;
    }

    public void scale(Vector3d vec) {
        for (Triangle3d triangle : triangles) {
            triangle.scale(vec);
        }
    }

    public void scale(double factor) {
        scale(new Vector3d(factor, factor, factor));
    }

    public void translate(Vector3d vec) {
        for (Triangle3d triangle : triangles) {
            triangle.translate(vec);
        }
    }

    public void translate(Vector3i vec) {
        translate(new Vector3d(vec.x / 16.0, vec.y / 16.0, vec.z / 16.0));
    }

    public void scale(Vector3i vec) {
        scale(new Vector3d(vec.x / 16.0, vec.y / 16.0, vec.z / 16.0));
    }

    private static final AtomicInteger logCount = new AtomicInteger();
    private static final AtomicInteger dumpCount = new AtomicInteger();

    public File dumpMesh() {
        File mcDir;
        if (FMLCommonHandler.instance().getSide().isClient()) {
            mcDir = Minecraft.getMinecraft().mcDataDir;
        } else {
            mcDir = new File(".");
        }
        File logsFolder = new File(mcDir, "logs");
        File outFile = new File(logsFolder, "littleTilesDumpMesh" + dumpCount.getAndIncrement() + ".obj");
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
            ArrayList<Vector3d> below = new ArrayList<>();
            ArrayList<Vector3d> above = new ArrayList<>();
            ArrayList<Vector3d> on = new ArrayList<>();
            ArrayList<Triangle3d> addTriangles = new ArrayList<>();

            classifyVertices(triangle, plane, below, above, on);

            if (above.isEmpty()) {
                // Entire triangle is below the plane, keep it
                if (!isDegenerate(triangle.getP1(), triangle.getP2(), triangle.getP3())) {
                    newTriangles.add(triangle);
                    if (on.size() == 2) {
                        existingEdges.add(new Edge3d(on.get(0), on.get(1)));
                    }
                }
            } else if (below.isEmpty()) {
                // SKIP
            } else if (above.size() == 1 && below.size() == 2) {
                // One vertex above, two below → Split into 2 triangles
                addTriangles.addAll(clipTriangle(below.get(0), below.get(1), above.get(0), plane, addedEdges));
            } else if (above.size() == 2 && below.size() == 1) {
                // Two vertices above, one below → Correct handling of this case
                addTriangles.addAll(clipTriangleTwoAbove(below.get(0), above.get(0), above.get(1), plane, addedEdges));
            } else if (on.size() == 1 && above.size() == 1) {
                // One vertex above, one below, one on → Correct handling of this case
                addTriangles
                        .addAll(clipTriangleOneOnOneBelow(on.get(0), below.get(0), above.get(0), plane, addedEdges));
            } else {
                throw new RuntimeException();
            }

            for (Triangle3d t : addTriangles) {
                t.ensureWindingOrder(triangle.getNormal());
            }

            newTriangles.addAll(addTriangles);
            // If all vertices are above, we discard the triangle
        }

        /* Close gaps */
        if (!addedEdges.isEmpty()) {
            addedEdges.addAll(existingEdges); // Might have existing edges we need to take into account
            List<Vector3d> addVertices = Edge3d.orderEdgesToVertexList(addedEdges);
            if (addVertices == null) {
                // Something went wrong trying to close the edges, log error and bail
                dumpFailingMesh();
                return new Mesh3d(new ArrayList<>());
            }
            Triangulator.triangulate(newTriangles, plane, addVertices);
        }

        return new Mesh3d(newTriangles);
    }

    private void classifyVertices(Triangle3d triangle, Plane3d plane, ArrayList<Vector3d> below,
            ArrayList<Vector3d> above, ArrayList<Vector3d> on) {
        Vector3d[] points = { triangle.getP1(), triangle.getP2(), triangle.getP3() };

        for (Vector3d point : points) {
            double d = plane.getDistance(point);
            if (Math.abs(d) < 0.0001) {
                on.add(point);
            } else if (d > 0) {
                above.add(point);
            } else {
                below.add(point);
            }
        }
    }

    public static boolean isDegenerate(Vector3d p1, Vector3d p2, Vector3d p3) {
        if (distanceSquared(p1, p2) <= DEGENERATE_EPSILON_SQUARED
                || distanceSquared(p1, p3) <= DEGENERATE_EPSILON_SQUARED
                || distanceSquared(p2, p3) <= DEGENERATE_EPSILON_SQUARED) {
            return true;
        }

        Vector3d edge1 = new Vector3d(p2);
        edge1.sub(p1);
        Vector3d edge2 = new Vector3d(p3);
        edge2.sub(p1);
        edge1.cross(edge1, edge2);
        return edge1.lengthSquared() <= DEGENERATE_EPSILON_SQUARED;
    }

    private static double distanceSquared(Vector3d p1, Vector3d p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        double dz = p1.z - p2.z;
        return dx * dx + dy * dy + dz * dz;
    }

    // Handles case when 2 vertices are below the plane, 1 is above
    private ArrayList<Triangle3d> clipTriangle(Vector3d pBelow1, Vector3d pBelow2, Vector3d pAbove, Plane3d plane,
            ArrayList<Edge3d> addedEdges) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        Vector3d i1 = plane.intersect(pBelow1, pAbove);
        Vector3d i2 = plane.intersect(pBelow2, pAbove);

        if (!isDegenerate(pBelow1, pBelow2, i1)) {
            newTriangles.add(new Triangle3d(pBelow1, pBelow2, i1));
        }

        if (!isDegenerate(pBelow2, i1, i2)) {
            addedEdges.add(new Edge3d(i1, i2));
            // new Vectors to have different objects for later translation
            newTriangles.add(new Triangle3d(new Vector3d(pBelow2), new Vector3d(i1), i2));
        }

        return newTriangles;
    }

    private ArrayList<Triangle3d> clipTriangleOneOnOneBelow(Vector3d pOn, Vector3d pBelow, Vector3d pAbove,
            Plane3d plane, ArrayList<Edge3d> addedEdges) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        Vector3d i1 = plane.intersect(pBelow, pAbove);

        if (!isDegenerate(pBelow, pOn, i1)) {
            newTriangles.add(new Triangle3d(pBelow, pOn, i1));
            addedEdges.add(new Edge3d(i1, pOn));
        }

        return newTriangles;
    }

    // Handles case when 1 vertex is below the plane, 2 are above
    private ArrayList<Triangle3d> clipTriangleTwoAbove(Vector3d pBelow, Vector3d pAbove1, Vector3d pAbove2,
            Plane3d plane, ArrayList<Edge3d> addedEdges) {
        ArrayList<Triangle3d> newTriangles = new ArrayList<>();

        Vector3d i1 = plane.intersect(pBelow, pAbove1);
        Vector3d i2 = plane.intersect(pBelow, pAbove2);

        if (!isDegenerate(pBelow, i1, i2)) {
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

        GL11.glBegin(GL11.GL_TRIANGLES);
        for (Triangle3d triangle : triangles) {
            Vector3d normal = triangle.getNormal();
            GL11.glNormal3d(normal.x, normal.y, normal.z);
            GL11.glTexCoord2d(triangle.getTex1().x, triangle.getTex1().y);
            GL11.glVertex3d(triangle.getP1().x, triangle.getP1().y, triangle.getP1().z);
            GL11.glTexCoord2d(triangle.getTex2().x, triangle.getTex2().y);
            GL11.glVertex3d(triangle.getP2().x, triangle.getP2().y, triangle.getP2().z);
            GL11.glTexCoord2d(triangle.getTex3().x, triangle.getTex3().y);
            GL11.glVertex3d(triangle.getP3().x, triangle.getP3().y, triangle.getP3().z);
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

    public void exportObj(File file) {
        try {
            FileWriter writer = new FileWriter(file);
            int vertexIndex = 1; // OBJ indices start at 1

            // First, write all vertices
            for (Triangle3d tri : triangles) {
                Vector3d[] verts = { tri.getP1(), tri.getP2(), tri.getP3() };
                for (Vector3d v : verts) {
                    writer.write(String.format("v %.6f %.6f %.6f%n", v.x, v.y, v.z));
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

    public void rotate(int orientation) {
        for (Triangle3d triangle : triangles) {
            triangle.rotate(orientation);
        }
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

    public static List<Vector3d> getPointsForTriangles(List<Triangle3d> triangles) {
        List<Vector3d> allPoints = new ArrayList<>();
        for (Triangle3d triangle : triangles) {
            allPoints.add(triangle.getP1());
            allPoints.add(triangle.getP2());
            allPoints.add(triangle.getP3());
        }
        return allPoints;
    }

    public List<Vector3d> getPointsForSide(ForgeDirection faceStart) {
        List<Vector3d> ret = new ArrayList<>();
        List<Vector3d> allPoints = getPointsForTriangles(triangles);
        List<Triangle3d> sidedTriangles = getTrianglesForSide(faceStart);
        List<Vector3d> trianglePoints = getPointsForTriangles(sidedTriangles);
        for (Vector3d point : allPoints) {
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
     * @return A point just inside the mesh surface.
     */
    public Vector3f getInteriorSamplePoint() {
        Triangle3d t = triangles.get(0);
        Vector3d p1 = t.getP1();
        Vector3d p2 = t.getP2();
        Vector3d p3 = t.getP3();
        Vector3d normal = t.getNormal();
        double eps = 1e-4;
        return new Vector3f(
                (float) ((p1.x + p2.x + p3.x) / 3.0 - normal.x * eps),
                (float) ((p1.y + p2.y + p3.y) / 3.0 - normal.y * eps),
                (float) ((p1.z + p2.z + p3.z) / 3.0 - normal.z * eps));
    }

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
