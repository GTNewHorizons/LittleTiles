package com.creativemd.littletiles.common.history;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;

public final class LittleTileChangePlan {

    private final ArrayList<ChangeEntry> changes;

    public LittleTileChangePlan(List<ChangeEntry> changes) {
        // Entries are immutable, so a new list is all this plan needs to own its content.
        this.changes = new ArrayList<>(changes);
    }

    public LittleTileChangePlan invert() {
        ArrayList<ChangeEntry> inverted = new ArrayList<>(changes.size());
        for (ChangeEntry change : changes) {
            inverted.add(change.invert());
        }
        return new LittleTileChangePlan(inverted);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public boolean apply(World world) {
        List<ResolvedChange> resolved = resolve(world);
        if (resolved == null) return false;

        for (ResolvedChange change : resolved) {
            change.apply(world);
        }
        return true;
    }

    /** Looks up everything the changes need and makes sure they still fit, without touching the world. */
    private List<ResolvedChange> resolve(World world) {
        if (world == null || isEmpty()) return null;

        List<ResolvedChange> resolved = new ArrayList<>(changes.size());
        for (ChangeEntry change : changes) {
            ChunkCoordinates coord = change.coord;
            TileEntityLittleTiles tile = LittleTileChangeRecorder.getTileEntity(world, coord);
            Block block = world.getBlock(coord.posX, coord.posY, coord.posZ);

            if (tile == null && (block instanceof BlockTile || !block.getMaterial().isReplaceable())) return null;

            List<LittleTile> remainingTiles = tile == null ? new ArrayList<LittleTile>()
                    : new ArrayList<>(tile.getTiles());
            List<LittleTile> tilesToRemove = new ArrayList<>(change.removedTiles.size());
            for (NBTTagCompound removedNbt : change.removedTiles) {
                LittleTile removed = findMatchingTile(remainingTiles, removedNbt);
                if (removed == null) return null;
                remainingTiles.remove(removed);
                tilesToRemove.add(removed);
            }

            if (!isSpaceForTiles(world, coord, remainingTiles, change.placedTiles)) return null;

            resolved.add(new ResolvedChange(change, tile, tilesToRemove));
        }
        return resolved;
    }

    private static boolean isSpaceForTiles(World world, ChunkCoordinates coord, List<LittleTile> remainingTiles,
            List<NBTTagCompound> placedTiles) {
        TileEntityLittleTiles collisionTile = createTemporaryTileEntity(world, coord, remainingTiles);
        for (NBTTagCompound placedNbt : placedTiles) {
            LittleTile placed = createTile(collisionTile, world, placedNbt);
            if (placed == null || placed.boundingBox == null
                    || !collisionTile.isSpaceForLittleTile(placed.boundingBox, placed.getCutoutInfo()))
                return false;
            collisionTile.getTiles().add(placed);
        }
        return true;
    }

    /** The stored nbt is copied, so a loaded tile can never change the plan. */
    static LittleTile createTile(TileEntityLittleTiles te, World world, NBTTagCompound nbt) {
        return LittleTile.CreateandLoadTile(te, world, (NBTTagCompound) nbt.copy());
    }

    private static LittleTile findMatchingTile(List<LittleTile> tiles, NBTTagCompound expectedNbt) {
        for (LittleTile tile : tiles) {
            NBTTagCompound currentNbt = new NBTTagCompound();
            tile.saveTile(currentNbt);
            if (currentNbt.equals(expectedNbt)) return tile;
        }
        return null;
    }

    private static TileEntityLittleTiles createTemporaryTileEntity(World world, ChunkCoordinates coord,
            List<LittleTile> tiles) {
        TileEntityLittleTiles tile = new TileEntityLittleTiles();
        tile.setWorldObj(world);
        tile.xCoord = coord.posX;
        tile.yCoord = coord.posY;
        tile.zCoord = coord.posZ;
        tile.setTiles(tiles);
        return tile;
    }
}
