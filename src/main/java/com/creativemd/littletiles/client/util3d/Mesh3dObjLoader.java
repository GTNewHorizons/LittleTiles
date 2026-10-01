package com.creativemd.littletiles.client.util3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.creativemd.littletiles.LittleTiles;

public class Mesh3dObjLoader {

    /**
     * Loads a unit mesh: the OBJ spans [0, 1] on each axis, which is mapped onto [0, {@link Grid3d#PIXEL}] and rounded
     * to the grid.
     */
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
        List<GridVector> vertices = new ArrayList<>();
        List<Triangle3d> triangles = new ArrayList<>();

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("v ")) {
                // vertex
                String[] tok = line.split("\\s+");
                int x = (int) Math.round(Double.parseDouble(tok[1]) * Grid3d.PIXEL);
                int y = (int) Math.round(Double.parseDouble(tok[2]) * Grid3d.PIXEL);
                int z = (int) Math.round(Double.parseDouble(tok[3]) * Grid3d.PIXEL);
                vertices.add(new GridVector(x, y, z));
            }

            else if (line.startsWith("f ")) {
                // face without slashes
                String[] tok = line.split("\\s+");

                int a = Integer.parseInt(tok[1]) - 1; // OBJ = 1-based
                int b = Integer.parseInt(tok[2]) - 1;
                int c = Integer.parseInt(tok[3]) - 1;

                // copies, as the triangles of a mesh must not share points
                triangles.add(
                        new Triangle3d(
                                new GridVector(vertices.get(a)),
                                new GridVector(vertices.get(b)),
                                new GridVector(vertices.get(c))));
            }
        }

        return new Mesh3d(triangles);
    }
}
