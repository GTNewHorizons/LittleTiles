package com.creativemd.littletiles.common.placement;

import java.util.ArrayList;

import net.minecraft.util.ChunkCoordinates;

import com.creativemd.littletiles.utils.PreviewTile;

/** The tiles a {@link LittleTilePlacementPlan} places into one block. */
final class PlacementEntry extends BaseEntry {

    final ArrayList<PreviewTile> placeTiles;

    PlacementEntry(ChunkCoordinates coord, ArrayList<PreviewTile> placeTiles) {
        super(coord);
        this.placeTiles = placeTiles;
    }
}
