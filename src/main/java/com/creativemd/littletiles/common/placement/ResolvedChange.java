package com.creativemd.littletiles.common.placement;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;

/** One change of a {@link LittleTileChangePlan}, with everything it needs already looked up and checked. */
final class ResolvedChange extends BaseEntry {

    private final ChangeEntry change;
    private final List<LittleTile> tilesToRemove;

    ResolvedChange(ChangeEntry change, List<LittleTile> tilesToRemove) {
        super(change.coord);
        this.change = change;
        this.tilesToRemove = tilesToRemove;
    }

    /** Cannot fail once every change of the plan was prepared. */
    void apply(World world) {
        TileEntityLittleTiles tile = getTileEntity();
        for (LittleTile removed : tilesToRemove) {
            tile.removeTile(removed, false);
        }
        for (NBTTagCompound placedNbt : change.placedTiles) {
            LittleTileChangePlan.createTile(tile, world, placedNbt).place();
        }
        tile.updateTiles();
    }
}
