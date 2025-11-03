package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.lib.Vector3d;

public class Edge3d {

    private Vector3d p1;
    private Vector3d p2;

    public Edge3d(Vector3d p1, Vector3d p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public static List<Vector3d> orderEdgesToVertexList(List<Edge3d> edges) {
        List<Vector3d> orderedLoop = new ArrayList<>();
        Edge3d firstEdge = edges.get(0);

        Vector3d prev = firstEdge.p1;
        Vector3d current = firstEdge.p2;
        orderedLoop.add(prev);
        orderedLoop.add(current);

        while (orderedLoop.size() < edges.size()) {
            int orderedLoopSize = orderedLoop.size();
            for (Edge3d edge : edges) {
                if (edge.p1.equals(current) && !edge.p2.equals(prev)) {
                    prev = current;
                    current = edge.p2;
                    orderedLoop.add(current);
                    break;
                } else if (edge.p2.equals(current) && !edge.p1.equals(prev)) {
                    prev = current;
                    current = edge.p1;
                    orderedLoop.add(current);
                    break;
                }
            }
            if (orderedLoopSize == orderedLoop.size()) {
                // Something went horribly wrong and we'd be stuck inside an endless loop
                return null;
            }
        }

        return orderedLoop;
    }
}
