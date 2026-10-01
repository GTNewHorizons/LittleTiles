package com.creativemd.littletiles.common.history;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;

/** One change of a {@link LittleTileChangePlan}, with everything it needs already looked up and checked. */
final class ResolvedChange {

    private final ChangeEntry change;
    private final TileEntityLittleTiles tileEntity;
    private final List<LittleTile> tilesToRemove;

    ResolvedChange(ChangeEntry change, TileEntityLittleTiles tileEntity, List<LittleTile> tilesToRemove) {
        this.change = change;
        this.tileEntity = tileEntity;
        this.tilesToRemove = tilesToRemove;
    }

    void apply(World world) {
        TileEntityLittleTiles tile = tileEntity;
        if (tile == null) {
            if (change.placedTiles.isEmpty()) return;
            ChunkCoordinates coord = change.coord;
            world.setBlock(coord.posX, coord.posY, coord.posZ, LittleTiles.blockTile, 0, 3);
            tile = LittleTileChangeRecorder.getTileEntity(world, coord);
            if (tile == null) return;
        }

        for (LittleTile removed : tilesToRemove) {
            tile.removeTile(removed, false);
        }
        for (NBTTagCompound placedNbt : change.placedTiles) {
            LittleTileChangePlan.createTile(tile, world, placedNbt).place();
        }
        tile.updateTiles();
    }
}
