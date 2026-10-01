package com.creativemd.littletiles.common.history;

import java.util.ArrayList;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;

import com.creativemd.littletiles.common.items.LittleTilePlacementPlan;

/**
 * Result of applying a {@link LittleTilePlacementPlan}.
 */
public final class LittleTilePlacementPlanResult {

    private final ArrayList<ChangeEntry> changes = new ArrayList<>();

    public void addChangedTiles(ChunkCoordinates coord, Set<NBTTagCompound> before, Set<NBTTagCompound> after) {
        ArrayList<NBTTagCompound> placedTiles = new ArrayList<>();
        for (NBTTagCompound tile : after) {
            if (!before.contains(tile)) placedTiles.add(tile);
        }
        ArrayList<NBTTagCompound> removedTiles = new ArrayList<>();
        for (NBTTagCompound tile : before) {
            if (!after.contains(tile)) removedTiles.add(tile);
        }
        if (placedTiles.isEmpty() && removedTiles.isEmpty()) return;
        changes.add(new ChangeEntry(coord, placedTiles, removedTiles));
    }

    public boolean hasPlacedTiles() {
        for (ChangeEntry change : changes) {
            if (change.hasPlacedTiles()) return true;
        }
        return false;
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public LittleTileChangePlan createPlan() {
        return new LittleTileChangePlan(changes);
    }

}
