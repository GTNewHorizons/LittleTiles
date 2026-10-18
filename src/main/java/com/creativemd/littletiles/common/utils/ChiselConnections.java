package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.cricketcraft.chisel.api.IFacade;

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

    /** A rendered tile asking whether its material continues at another location. */
    public static boolean fromTile(IBlockAccess world, LittleTileBox sourceBox, int fromX, int fromY, int fromZ, int x,
            int y, int z, int side, Block block, int meta) {
        // The fake render world hides this tile entity, so use the underlying world.
        TileEntity source = world.getTileEntity(fromX, fromY, fromZ);
        if (!(source instanceof TileEntityLittleTiles)) return false;

        int dx = Integer.signum(x - fromX);
        int dy = Integer.signum(y - fromY);
        int dz = Integer.signum(z - fromZ);
        LittleTileFacade[] sourceFacades = ((TileEntityLittleTiles) source).getFacades();
        // Stretch the tile to the requested side(s) and check that matching tiles fill it.
        LittleTileBox reach = LittleTileFacade.reach(sourceFacades, sourceBox, block, meta, dx, dy, dz);
        if (reach == null) return false;

        TileEntity target = world.getTileEntity(x, y, z);
        if (target instanceof TileEntityLittleTiles) {
            LittleTileFacade[] targetFacades = ((TileEntityLittleTiles) target).getFacades();
            return LittleTileFacade.connectsToTiles(reach, targetFacades, block, meta, dx, dy, dz);
        }
        return matchesBlock(world, x, y, z, side, fromX, fromY, fromZ, block, meta);
    }

    /** Chisel's normal comparison, for queries that do not originate at the rendered tile or end at a whole block. */
    public static boolean matchesBlock(IBlockAccess world, int x, int y, int z, int side, int fromX, int fromY,
            int fromZ, Block block, int meta) {
        Block target = world.getBlock(x, y, z);
        if (target instanceof IFacade) {
            IFacade facade = (IFacade) target;
            return facade.getFacade(world, x, y, z, side, fromX, fromY, fromZ, block, meta) == block
                    && facade.getFacadeMetadata(world, x, y, z, side, fromX, fromY, fromZ, block, meta) == meta;
        }
        return target == block && world.getBlockMetadata(x, y, z) == meta;
    }
}
