package com.creativemd.littletiles.client.util3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.LittleTiles;

public class Mesh3dObjLoader {

    /** The mesh in the file. Each face is a convex polygon, see {@link Triangle3d#getPolygon}. */
    public static Mesh3d load(String name) {
        String path = "assets/" + LittleTiles.modid + "/models/" + name + ".obj";
        try {
            InputStream res = LittleTiles.class.getClassLoader().getResourceAsStream(path);
            return loadFromStream(res);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Mesh3d loadFromStream(InputStream stream) throws IOException {
        List<Vector3d> vertices = new ArrayList<>();
        List<Triangle3d> triangles = new ArrayList<>();

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("v ")) {
                // vertex
                String[] tok = line.split("\\s+");
                double x = Double.parseDouble(tok[1]);
                double y = Double.parseDouble(tok[2]);
                double z = Double.parseDouble(tok[3]);
                vertices.add(new Vector3d(x, y, z));
            }

            else if (line.startsWith("f ")) {
                // a convex polygon, without slashes
                String[] tok = line.split("\\s+");
                Vector3d first = vertices.get(Integer.parseInt(tok[1]) - 1); // OBJ = 1-based
                Object polygon = new Object();
                for (int i = 2; i + 1 < tok.length; i++) {
                    // copies, as the triangles of a mesh must not share points
                    Triangle3d triangle = new Triangle3d(
                            new Vector3d(first),
                            new Vector3d(vertices.get(Integer.parseInt(tok[i]) - 1)),
                            new Vector3d(vertices.get(Integer.parseInt(tok[i + 1]) - 1)));
                    triangle.setPolygon(polygon);
                    triangles.add(triangle);
                }
            }
        }

        return new Mesh3d(triangles);
    }
}
