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
                // face without slashes
                String[] tok = line.split("\\s+");

                int a = Integer.parseInt(tok[1]) - 1; // OBJ = 1-based
                int b = Integer.parseInt(tok[2]) - 1;
                int c = Integer.parseInt(tok[3]) - 1;

                triangles.add(new Triangle3d(vertices.get(a), vertices.get(b), vertices.get(c)));
            }
        }

        return new Mesh3d(triangles);
    }
}
