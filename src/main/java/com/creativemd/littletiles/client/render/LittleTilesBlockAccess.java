package com.creativemd.littletiles.client.render;

import com.creativemd.creativecore.client.block.IBlockAccessFake;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

/** The world a tile is rendered in, showing the tile's block at its position. */
public class LittleTilesBlockAccess extends IBlockAccessFake {

    public void setCube(LittleTilesCubeObject cube) {
        setBlock(cube.block, cube.meta);
    }
}
