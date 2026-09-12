package com.creativemd.littletiles.common.history;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;

/** Immutable after construction; the NBT lists are handed over by the caller and stay untouched afterwards. */
public final class ChangeEntry {

    final ChunkCoordinates coord;
    final List<NBTTagCompound> placedTiles;
    final List<NBTTagCompound> removedTiles;

    ChangeEntry(ChunkCoordinates coord, List<NBTTagCompound> placedTiles, List<NBTTagCompound> removedTiles) {
        this.coord = new ChunkCoordinates(coord.posX, coord.posY, coord.posZ);
        this.placedTiles = placedTiles;
        this.removedTiles = removedTiles;
    }

    ChangeEntry invert() {
        return new ChangeEntry(coord, removedTiles, placedTiles);
    }

    boolean hasPlacedTiles() {
        return !placedTiles.isEmpty();
    }
}
