package com.creativemd.littletiles.common.placement;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

/** Shared logic of plans which change the tiles of several blocks at once. */
abstract class BasePlan {

    /** Tiles can go into a block with a tile entity already, or into a replaceable block which is overwritten. */
    static boolean canHoldTiles(Block block, TileEntityLittleTiles tile) {
        return tile != null || (!(block instanceof BlockTile) && block.getMaterial().isReplaceable());
    }

    static TileEntityLittleTiles getTileEntity(World world, ChunkCoordinates coord) {
        TileEntity tile = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
        if (tile instanceof TileEntityLittleTiles littleTile) return littleTile;
        return null;
    }
}
