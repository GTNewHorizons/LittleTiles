package com.creativemd.littletiles.common.placement;

import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

/** The part of a plan that changes the tiles of one block. */
abstract class BaseEntry {

    final ChunkCoordinates coord;
    private TileEntityLittleTiles tileEntity;
    /** The block {@link #prepare} replaced to place tiles where there were none, null if it replaced nothing. */
    private Block replacedBlock;
    private int replacedMeta;

    BaseEntry(ChunkCoordinates coord) {
        this.coord = coord;
    }

    /**
     * Creates the block the tiles are placed into if there is none yet. This is the only part of an entry that can
     * still fail after the plan was checked, so it happens for every entry before any tile is touched.
     *
     * @return false if the block has no tile entity afterwards, {@link #unprepare} puts back what was replaced
     */
    final boolean prepare(World world) {
        replacedBlock = null;
        tileEntity = BasePlan.getTileEntity(world, coord);
        if (tileEntity != null) return true;

        replacedBlock = world.getBlock(coord.posX, coord.posY, coord.posZ);
        replacedMeta = world.getBlockMetadata(coord.posX, coord.posY, coord.posZ);
        world.setBlock(coord.posX, coord.posY, coord.posZ, LittleTiles.blockTile, 0, 3);
        tileEntity = BasePlan.getTileEntity(world, coord);
        return tileEntity != null;
    }

    /** Puts back the block {@link #prepare} replaced. */
    final void unprepare(World world) {
        if (replacedBlock == null) return;

        world.setBlock(coord.posX, coord.posY, coord.posZ, replacedBlock, replacedMeta, 3);
        replacedBlock = null;
        tileEntity = null;
    }

    /** Only valid after a successful {@link #prepare}. */
    final TileEntityLittleTiles getTileEntity() {
        return tileEntity;
    }
}
