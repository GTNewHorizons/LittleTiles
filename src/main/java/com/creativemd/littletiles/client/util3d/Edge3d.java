package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

public class Edge3d {

    private final GridVector p1;
    private final GridVector p2;

    public Edge3d(GridVector p1, GridVector p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * Adds the edge from {@code p1} to {@code p2}, unless it is already in the list in either direction, in which case
     * that one is removed instead. In a closed mesh every edge belongs to two triangles: if both of them are kept, the
     * plane only touches the mesh along the edge, so it is no border of the cap and would leave a spur in the loop.
     */
    public static void addOrCancel(List<Edge3d> edges, GridVector p1, GridVector p2) {
        for (int i = 0; i < edges.size(); i++) {
            Edge3d edge = edges.get(i);
            if ((edge.p1.equals(p1) && edge.p2.equals(p2)) || (edge.p1.equals(p2) && edge.p2.equals(p1))) {
                edges.remove(i);
                return;
            }
        }
        edges.add(new Edge3d(p1, p2));
    }

    /**
     * Chains the edges into a loop. Points are on the grid, so edges connect only where their ends are identical.
     *
     * @return the points along the loop, or null when the edges do not connect
     */
    public static List<GridVector> orderEdgesToVertexList(List<Edge3d> edges) {
        List<GridVector> orderedLoop = new ArrayList<>();
        Edge3d firstEdge = edges.remove(0);

        orderedLoop.add(firstEdge.p1);
        orderedLoop.add(firstEdge.p2);
        GridVector current = firstEdge.p2;

        while (!edges.isEmpty()) {
            current = removeEdgeFrom(current, edges);
            if (current == null) {
                // No edge continues from here — loop is broken
                return null;
            }
            orderedLoop.add(current);
        }

        return orderedLoop;
    }

    /** Removes an edge starting or ending at {@code point} and returns its other end, null when there is none. */
    private static GridVector removeEdgeFrom(GridVector point, List<Edge3d> edges) {
        for (int i = 0; i < edges.size(); i++) {
            Edge3d edge = edges.get(i);
            if (edge.p1.equals(point)) {
                edges.remove(i);
                return edge.p2;
            }
            if (edge.p2.equals(point)) {
                edges.remove(i);
                return edge.p1;
            }
        }
        return null;
    }
}
