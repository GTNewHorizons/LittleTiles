package com.creativemd.littletiles.common.utils;

import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;

/**
 * Client render geometry retained for a little tile.
 */
public class LittleTileRenderCache {

    private final LittleTile tile;
    private volatile Mesh3d simpleMesh;

    public LittleTileRenderCache(LittleTile tile) {
        this.tile = tile;
    }

    public Mesh3d getSimpleMesh() {
        if (tile.getCutoutInfo() == null || tile.boundingBox == null) {
            return null;
        }
        if (simpleMesh == null) {
            simpleMesh = Mesh3dUtil.meshFromTile(tile.boundingBox, tile.getCutoutInfo());
        }
        return simpleMesh;
    }

    public void invalidateMesh() {
        simpleMesh = null;
    }
}
