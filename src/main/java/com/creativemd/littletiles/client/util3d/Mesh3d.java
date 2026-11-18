package com.creativemd.littletiles.client.util3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.render.LittleTilesBlockRenderHelper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;

/**
 * A triangle mesh. Triangles must not share point objects: transforms move the points in place, so a shared one would
 * be moved once per triangle.
 */
public class Mesh3d {

    /** Set in headless tests, where dumping a mesh would initialize FML. A failed cut throws instead. */
    private static final String DISABLE_MESH_DUMPS_PROPERTY = "littletiles.disableMeshDumps";

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
        return dumpMesh("DumpMesh", nextDumpIndex());
    }

    /** Reserves an index for {@link #dumpMesh(String, int)}, so related dumps can share it. */
    public static int nextDumpIndex() {
        return dumpCount.getAndIncrement();
    }

    /** Dumps into {@code logs/littleTiles<name><index>.obj}. */
    public File dumpMesh(String name, int index) {
        if (Boolean.getBoolean(DISABLE_MESH_DUMPS_PROPERTY)) return null;
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
        if (Boolean.getBoolean(DISABLE_MESH_DUMPS_PROPERTY)) return;
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

    /**
     * Cuts the mesh down to the side of every plane its normal points away from, closing each cut with a cap. See
     * {@link MeshCutter}.
     */
    public Mesh3d cutByPlanes(List<Plane3d> planes) {
        if (planes.isEmpty()) return this;
        MeshCutter cutter = new MeshCutter(triangles);
        for (Plane3d plane : planes) {
            if (!cutter.cut(plane)) {
                // Something went wrong trying to close the cut, log error and bail
                if (Boolean.getBoolean(DISABLE_MESH_DUMPS_PROPERTY)) {
                    throw new IllegalStateException("Could not close mesh cut");
                }
                dumpFailingMesh();
                return new Mesh3d(new ArrayList<>());
            }
        }
        return new Mesh3d(cutter.toTriangles());
    }

    public static double distanceSquared(Vector3d p1, Vector3d p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        double dz = p1.z - p2.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The volume the mesh encloses, negative if it is wound inwards. The mesh has to be closed. */
    public double volume() {
        if (triangles.isEmpty()) return 0;
        Vector3d origin = triangles.get(0).getP1();
        double volume = 0;
        for (Triangle3d triangle : triangles) {
            volume += MeshCutter.det(triangle.getP1(), triangle.getP2(), triangle.getP3(), origin);
        }
        return volume / 6;
    }

    public void flipWindingOrder() {
        for (Triangle3d triangle : triangles) {
            triangle.flipWindingOrder();
        }
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
                    writer.write(String.format(Locale.ROOT, "v %.6f %.6f %.6f%n", v.x, v.y, v.z));
                }
            }

            // Then, write faces
            for (int i = 0; i < triangles.size(); i++) {
                // Faces in OBJ reference vertices by their 1-based index
                writer.write(String.format(Locale.ROOT, "f %d %d %d%n", vertexIndex, vertexIndex + 1, vertexIndex + 2));
                vertexIndex += 3;
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Rotates the mesh within the box from the origin to {@code size}, so that afterwards it fills the rotated box from
     * the origin to {@code size} rotated as well.
     */
    public void rotate(int orientation, Vector3d size) {
        Vector3f rotatedSize = OrientationMapper.fromId(orientation).transform(size.toVector3f());
        rotatedSize.absolute();
        translate(new Vector3d(-size.x / 2, -size.y / 2, -size.z / 2));
        for (Triangle3d triangle : triangles) {
            triangle.rotate(orientation);
        }
        translate(new Vector3d(rotatedSize.x / 2, rotatedSize.y / 2, rotatedSize.z / 2));
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

    /** The corners of all triangles, three in a row per triangle. */
    public org.joml.Vector3d[] getVertices() {
        org.joml.Vector3d[] ret = new org.joml.Vector3d[triangles.size() * 3];
        for (int i = 0; i < triangles.size(); i++) {
            Triangle3d triangle = triangles.get(i);
            ret[i * 3] = toJoml(triangle.getP1());
            ret[i * 3 + 1] = toJoml(triangle.getP2());
            ret[i * 3 + 2] = toJoml(triangle.getP3());
        }
        return ret;
    }

    private static org.joml.Vector3d toJoml(Vector3d point) {
        return new org.joml.Vector3d(point.x, point.y, point.z);
    }
}
