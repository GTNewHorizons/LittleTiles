package com.creativemd.littletiles.common.placement;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

/** Shared logic of plans which change the tiles of several blocks at once. */
abstract class BasePlan {

    static TileEntityLittleTiles getTileEntity(World world, ChunkCoordinates coord) {
        TileEntity tile = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
        if (tile instanceof TileEntityLittleTiles littleTile) return littleTile;
        return null;
    }
}
