package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.lib.Vector3d;

public class Edge3d {

    private static final double EPSILON = 0.0001;
    private static final double EPSILON_SQUARED = EPSILON * EPSILON;

    private static final class NextEdgeResult {

        private Edge3d edge;
        private Vector3d nextPoint;
    }

    private final Vector3d p1;
    private final Vector3d p2;

    public Edge3d(Vector3d p1, Vector3d p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    private static double distanceSquared(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean findNextBestEdge(Vector3d current, List<Edge3d> edges, NextEdgeResult result) {
        double bestDistance = Double.POSITIVE_INFINITY;
        result.edge = null;
        result.nextPoint = null;
        for (Edge3d edge : edges) {
            double distance = distanceSquared(edge.p1, current);
            if (distance < bestDistance) {
                bestDistance = distance;
                result.edge = edge;
                result.nextPoint = edge.p2;
            }

            distance = distanceSquared(edge.p2, current);
            if (distance < bestDistance) {
                bestDistance = distance;
                result.edge = edge;
                result.nextPoint = edge.p1;
            }
        }
        return bestDistance <= EPSILON_SQUARED;
    }

    public static List<Vector3d> orderEdgesToVertexList(List<Edge3d> edges) {
        List<Vector3d> orderedLoop = new ArrayList<>();
        NextEdgeResult nextEdge = new NextEdgeResult();
        Edge3d firstEdge = edges.get(0);

        orderedLoop.add(firstEdge.p1);
        orderedLoop.add(firstEdge.p2);
        Vector3d current = firstEdge.p2;
        edges.remove(0);

        while (!edges.isEmpty()) {
            if (!findNextBestEdge(current, edges, nextEdge)) {
                // Closest endpoint is farther than epsilon — loop is broken
                return null;
            }
            edges.remove(nextEdge.edge);
            current = nextEdge.nextPoint;
            orderedLoop.add(current);
        }

        return orderedLoop;
    }
}
