package com.creativemd.littletiles.client.util3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;

import org.joml.Vector3i;

import com.creativemd.creativecore.lib.Vector3d;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;

public class Mesh3d {

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

    private static int logCount;

    private void dumpFailingMesh() {
        File mcDir;

        if (FMLCommonHandler.instance().getSide().isClient()) {
            mcDir = Minecraft.getMinecraft().mcDataDir;
        } else {
            mcDir = new File("."); // server root
        }

        File logsFolder = new File(mcDir, "logs");
        File outFile = new File(logsFolder, "littleTilesErrorMesh" + logCount + ".obj");
        logCount++;
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
                newTriangles.add(triangle);
                if (on.size() == 2) {
                    existingEdges.add(new Edge3d(on.get(0), on.get(1)));
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
            if (Math.abs(plane.getDistance(point)) < 0.0001) {
                on.add(point);
            } else if (plane.isPointAbove(point)) {
                above.add(point);
            } else {
                below.add(point);
            }
        }
    }

    public static boolean isDegenerate(Vector3d p1, Vector3d p2, Vector3d p3) {
        if (p1.equals(p2) || p1.equals(p3) || p2.equals(p3)) {
            return true;
        }

        Vector3d edge1 = new Vector3d(p2);
        edge1.sub(p1);
        Vector3d edge2 = new Vector3d(p3);
        edge2.sub(p1);
        edge1.cross(edge1, edge2);
        return edge1.lengthSquared() == 0;
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
}
