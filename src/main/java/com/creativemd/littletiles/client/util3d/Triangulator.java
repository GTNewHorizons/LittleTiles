package com.creativemd.littletiles.client.util3d;

import java.util.List;

import org.joml.Vector2d;

import com.creativemd.creativecore.lib.Vector3d;

import earcut4j.Earcut;

public class Triangulator {

    public static void triangulate(List<Triangle3d> triangles, Plane3d plane, List<Vector3d> points) {
        double[] flatVertices = new double[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            Vector2d mapped = plane.mapTo2D(points.get(i));
            flatVertices[i * 2] = mapped.x;
            flatVertices[i * 2 + 1] = mapped.y;
        }

        List<Integer> indices = Earcut.earcut(flatVertices);

        for (int i = 0; i < indices.size() / 3; i++) {
            Vector3d firstVertex = points.get(indices.get(i * 3));
            Vector3d secondVertex = points.get(indices.get(i * 3 + 1));
            Vector3d thirdVertex = points.get(indices.get(i * 3 + 2));
            if (Mesh3d.isDegenerate(firstVertex, secondVertex, thirdVertex)) {
                continue;
            }
            // new Vectors to have different objects for later translation
            Triangle3d triangle = new Triangle3d(
                    new Vector3d(firstVertex),
                    new Vector3d(secondVertex),
                    new Vector3d(thirdVertex));
            triangle.ensureWindingOrder(plane.getNormal());
            triangles.add(triangle);
        }
    }
}
