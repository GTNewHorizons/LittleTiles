package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.client.render.LittleTilesFaceCuller;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/** Builds, measures and culls meshes as rendering does. */
public final class MeshGeometry {

    private MeshGeometry() {}

    /** The faces of a plain box tile, as culling builds them. */
    public static Mesh3d boxFaces(LittleTileBox box) {
        List<Triangle3d> faces = new ArrayList<>();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            faces.addAll(LittleTilesFaceCuller.boxFaceTriangles(box.getCube(), side));
        }
        return new Mesh3d(faces);
    }
}
