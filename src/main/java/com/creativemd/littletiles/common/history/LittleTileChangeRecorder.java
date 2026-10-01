package com.creativemd.littletiles.common.history;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;

/**
 * Records what really changed in a couple of blocks by comparing their tiles before and afterwards.
 */
public final class LittleTileChangeRecorder {

    private final World world;
    private final Map<ChunkCoordinates, Set<NBTTagCompound>> statesBefore = new LinkedHashMap<>();

    public LittleTileChangeRecorder(World world) {
        this.world = world;
    }

    /**
     * Has to be called before the block is modified. Watches the block itself and every other block the structures
     * inside it span. A structure goes away as a whole, so destroying one of its tiles changes blocks which are not
     * touched directly.
     */
    public void watch(ChunkCoordinates coord) {
        capture(coord);

        TileEntityLittleTiles tileEntity = getTileEntity(world, coord);
        if (tileEntity == null) return;
        for (LittleTile tile : new ArrayList<>(tileEntity.getTiles())) {
            if (!tile.isStructureBlock || !tile.isLoaded()) continue;
            for (LittleTile structureTile : tile.structure.getTiles()) {
                if (structureTile.te == null) continue;
                capture(structureTile.te.getCoord());
            }
        }
    }

    private void capture(ChunkCoordinates coord) {
        // Never overwrite an earlier snapshot, it might already reflect a modification.
        if (statesBefore.containsKey(coord)) return;
        statesBefore.put(coord, captureTiles(world, coord));
    }

    public LittleTilePlacementPlanResult finish() {
        LittleTilePlacementPlanResult result = new LittleTilePlacementPlanResult();
        for (Map.Entry<ChunkCoordinates, Set<NBTTagCompound>> entry : statesBefore.entrySet()) {
            result.addChangedTiles(entry.getKey(), entry.getValue(), captureTiles(world, entry.getKey()));
        }
        return result;
    }

    // A block cannot hold two identical tiles, so a set loses nothing and makes diffing cheap.
    private static Set<NBTTagCompound> captureTiles(World world, ChunkCoordinates coord) {
        HashSet<NBTTagCompound> snapshots = new HashSet<>();
        TileEntityLittleTiles tileEntity = getTileEntity(world, coord);
        if (tileEntity != null) {
            for (LittleTile tile : tileEntity.getTiles()) {
                NBTTagCompound nbt = new NBTTagCompound();
                tile.saveTile(nbt);
                snapshots.add(nbt);
            }
        }
        return snapshots;
    }

    static TileEntityLittleTiles getTileEntity(World world, ChunkCoordinates coord) {
        TileEntity tile = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
        if (tile instanceof TileEntityLittleTiles littleTile) return littleTile;
        return null;
    }
}
