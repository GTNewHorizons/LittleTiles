package com.creativemd.littletiles.client.render;

import net.minecraft.block.Block;

import com.creativemd.creativecore.client.block.IBlockAccessFake;
import com.creativemd.littletiles.common.utils.ChiselConnections;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.cricketcraft.chisel.api.IConnectionAccess;

import cpw.mods.fml.common.Optional;

/**
 * The world a tile is rendered in. Besides showing the tile's block at its position, it decides connected textures for
 * the tile.
 * <p>
 * A tile connects towards a neighbour only if tiles of its block and meta fill the way from the tile to the boundary of
 * its block there, and the neighbour continues them beyond the boundary.
 */
@Optional.Interface(iface = "com.cricketcraft.chisel.api.IConnectionAccess", modid = "chisel")
public class LittleTilesBlockAccess extends IBlockAccessFake implements IConnectionAccess {

    private LittleTilesCubeObject cube;
    private LittleTileBox box;

    public void setCube(LittleTilesCubeObject cube) {
        setBlock(cube.block, cube.meta);
        this.cube = cube;
        this.box = new LittleTileBox(
                cube.gridMinX,
                cube.gridMinY,
                cube.gridMinZ,
                cube.gridMaxX,
                cube.gridMaxY,
                cube.gridMaxZ);
    }

    @Override
    public void reset() {
        super.reset();
        this.cube = null;
        this.box = null;
    }

    @Override
    @Optional.Method(modid = "chisel")
    public boolean matches(int x, int y, int z, int side, int fromX, int fromY, int fromZ, Block block, int meta) {
        if (cube == null || !isTilePos(fromX, fromY, fromZ)) {
            return ChiselConnections.matchesBlock(this, x, y, z, side, fromX, fromY, fromZ, block, meta);
        }
        // cut shapes don't connect for now, their bounding box isn't what they show
        if (cube.cutoutInfo != null) return false;
        return ChiselConnections.fromTile(world, box, posX, posY, posZ, x, y, z, side, block, meta);
    }
}
