package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.cricketcraft.chisel.api.IFacade;

/** Chisel's block and world callbacks expressed in terms of the same LittleTiles connection rule. */
public final class ChiselConnections {

    private ChiselConnections() {}

    /** A whole block asking whether it connects to a LittleTiles block. */
    public static boolean fromBlockToTiles(IBlockAccess world, int x, int y, int z, int side, int fromX, int fromY,
            int fromZ, Block block, int meta) {
        TileEntity target = world.getTileEntity(x, y, z);
        if (!(target instanceof TileEntityLittleTiles)) return false;

        int dx = Integer.signum(x - fromX);
        int dy = Integer.signum(y - fromY);
        int dz = Integer.signum(z - fromZ);
        LittleTileFacades targetFacades = ((TileEntityLittleTiles) target).getFacades();
        return matchesTiles(LittleTileFacade.FULL_BLOCK, targetFacades, side, block, meta, dx, dy, dz);
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
        LittleTileFacades sourceFacades = ((TileEntityLittleTiles) source).getFacades();
        // Stretch the tile to the requested side(s) and check that matching tiles fill it.
        LittleTileBox reach = LittleTileFacade.reach(sourceFacades, sourceBox, block, meta, dx, dy, dz);
        if (reach == null) return false;

        TileEntity target = world.getTileEntity(x, y, z);
        if (target instanceof TileEntityLittleTiles) {
            LittleTileFacades targetFacades = ((TileEntityLittleTiles) target).getFacades();
            return matchesTiles(reach, targetFacades, side, block, meta, dx, dy, dz);
        }
        return matchesBlock(world, x, y, z, side, fromX, fromY, fromZ, block, meta);
    }

    /**
     * Chisel asks about a location beside the rendered face to connect to it, and about the location in front of that
     * neighbour to see whether the neighbour's face is hidden. Touching is enough to connect, but only covering the
     * neighbour's face hides it.
     */
    private static boolean matchesTiles(LittleTileBox reach, LittleTileFacades targetFacades, int side, Block block,
            int meta, int dx, int dy, int dz) {
        if (side >= 0 && side < 6) {
            ForgeDirection normal = ForgeDirection.VALID_DIRECTIONS[side];
            // The offset's step along the face normal: 0 beside the face, nonzero in front of it
            if (dx * normal.offsetX + dy * normal.offsetY + dz * normal.offsetZ != 0) {
                return LittleTileFacade.coversFace(reach, targetFacades, block, meta, dx, dy, dz, normal);
            }
        }
        return LittleTileFacade.connectsToTiles(reach, targetFacades, block, meta, dx, dy, dz);
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
