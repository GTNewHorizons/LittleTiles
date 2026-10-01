package com.creativemd.littletiles.client.util3d;

import java.util.List;

import org.joml.Vector2d;

import earcut4j.Earcut;

public class Triangulator {

    public static void triangulate(List<Triangle3d> triangles, Plane3d plane, List<GridVector> points) {
        double[] flatVertices = new double[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            Vector2d mapped = plane.mapTo2D(points.get(i));
            flatVertices[i * 2] = mapped.x;
            flatVertices[i * 2 + 1] = mapped.y;
        }

        List<Integer> indices = Earcut.earcut(flatVertices);

        for (int i = 0; i < indices.size() / 3; i++) {
            // new Vectors to have different objects for later translation
            Triangle3d triangle = new Triangle3d(
                    new GridVector(points.get(indices.get(i * 3))),
                    new GridVector(points.get(indices.get(i * 3 + 1))),
                    new GridVector(points.get(indices.get(i * 3 + 2))));
            if (triangle.isDegenerate()) {
                continue;
            }
            triangle.ensureWindingOrder(plane.getNormal());
            // the cap lies in the cutting plane, so that is its plane, whatever rounding did to its normal
            triangle.setPlane(Plane3d.planes[plane.getDirection().ordinal()]);
            triangles.add(triangle);
        }
    }
}
