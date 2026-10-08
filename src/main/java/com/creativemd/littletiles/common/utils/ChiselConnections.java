package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

/** Chisel's block and world callbacks expressed in terms of the same LittleTiles connection rule. */
public final class ChiselConnections {

    private ChiselConnections() {}

    /** A whole block asking whether it connects to a LittleTiles block. */
    public static boolean fromBlockToTiles(IBlockAccess world, int x, int y, int z, int fromX, int fromY, int fromZ,
            Block block, int meta) {
        TileEntity target = world.getTileEntity(x, y, z);
        if (!(target instanceof TileEntityLittleTiles)) return false;

        int dx = Integer.signum(x - fromX);
        int dy = Integer.signum(y - fromY);
        int dz = Integer.signum(z - fromZ);
        LittleTileFacade[] targetFacades = ((TileEntityLittleTiles) target).getFacades();
        return LittleTileFacade.connectsToTiles(LittleTileFacade.FULL_BLOCK, targetFacades, block, meta, dx, dy, dz);
    }
}
