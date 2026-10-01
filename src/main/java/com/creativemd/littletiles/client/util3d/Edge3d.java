package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

public class Edge3d {

    private static final class NextEdgeResult {

        private Edge3d edge;
        private GridVector nextPoint;
    }

    private final GridVector p1;
    private final GridVector p2;

    public Edge3d(GridVector p1, GridVector p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /** Points are on the grid, so edges connect only where their ends are identical. */
    private static boolean findNextBestEdge(GridVector current, List<Edge3d> edges, NextEdgeResult result) {
        result.edge = null;
        result.nextPoint = null;
        for (Edge3d edge : edges) {
            if (edge.p1.equals(current)) {
                result.edge = edge;
                result.nextPoint = edge.p2;
                return true;
            }

            if (edge.p2.equals(current)) {
                result.edge = edge;
                result.nextPoint = edge.p1;
                return true;
            }
        }
        return false;
    }

    public static List<GridVector> orderEdgesToVertexList(List<Edge3d> edges) {
        List<GridVector> orderedLoop = new ArrayList<>();
        NextEdgeResult nextEdge = new NextEdgeResult();
        Edge3d firstEdge = edges.get(0);

        orderedLoop.add(firstEdge.p1);
        orderedLoop.add(firstEdge.p2);
        GridVector current = firstEdge.p2;
        edges.remove(0);

        while (!edges.isEmpty()) {
            if (!findNextBestEdge(current, edges, nextEdge)) {
                // No edge continues from here — loop is broken
                return null;
            }
            edges.remove(nextEdge.edge);
            current = nextEdge.nextPoint;
            orderedLoop.add(current);
        }

        return orderedLoop;
    }
}
